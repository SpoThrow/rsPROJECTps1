package server.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

/**
 * A small, dependency-free JDBC connection pool.
 *
 * <p><b>Why this exists.</b> The hiscores layer used to keep its connection in a
 * {@code public static Connection} field with a matching {@code public static Statement}, opened
 * a brand-new connection on <em>every</em> save, and closed that shared connection as a side
 * effect of <em>any</em> failed statement — so one bad query took the connection out from under
 * whatever else was reading it. Nothing was ever pooled, and a {@code ResultSet} was never
 * closed. This class replaces all of that: callers borrow a validated connection, return it, and
 * never touch a shared static.
 *
 * <p><b>Behaviour when the database is unreachable is deliberate: {@link #borrow()} returns
 * {@code null} rather than throwing.</b> The server must boot and run with no MySQL at all (the
 * vote system already degrades that way), so a hiscore write simply cannot happen and the caller
 * reports failure. A missing driver is the same story: JDBC 4 self-registers the driver from the
 * jar's {@code META-INF/services}, and if that jar is absent the connect fails and is reported
 * once.
 *
 * <p><b>A database that is down is remembered, not retried.</b> After a failed connect the pool
 * stops attempting one for {@link #FAILURE_BACKOFF_MS} and answers {@link #borrow()} with
 * {@code null} immediately. This matters because the hiscore write happens on the game thread on
 * every logout: without the backoff, a dead MySQL turned each logout into a full connect timeout,
 * and the shutdown hook's save into a wait times the number of players online.</p>
 *
 * <p><b>Failure logging is throttled</b> to one line per {@link #FAILURE_LOG_INTERVAL_MS}, because
 * a save is attempted on every logout and a down database would otherwise flood the console.
 *
 * <p>The pool is thread-safe. Callers must return what they borrow, exactly once, via
 * {@link #release(Connection)} (it worked) or {@link #discard(Connection)} (it did not) — a
 * borrowed connection that is never returned permanently reduces the pool's capacity.
 */
public final class ConnectionPool {

	/** Connections kept open at once when nothing is borrowed. */
	public static final int DEFAULT_MAX_SIZE = 4;

	/**
	 * Bound on the TCP connect, in milliseconds. A save runs on the game thread, so a database
	 * that is up but unreachable must fail fast instead of stalling a tick.
	 */
	private static final int CONNECT_TIMEOUT_MS = 3_000;

	/** Bound on a single statement's socket read, in milliseconds. */
	private static final int SOCKET_TIMEOUT_MS = 30_000;

	/** How long {@code Connection.isValid} may wait when checking a borrowed connection. */
	private static final int VALIDATION_TIMEOUT_SECONDS = 2;

	/** Minimum gap between failure log lines, in milliseconds. */
	private static final long FAILURE_LOG_INTERVAL_MS = 60_000;

	/**
	 * How long the pool refuses to try the database again after a failed connect, in milliseconds.
	 *
	 * <p>Without this, a down database cost a full connect timeout on <em>every</em> save — the
	 * hiscore write happens on every logout, so a dead MySQL turned each logout into a multi-second
	 * stall and the shutdown save into a wait times the number of players online. With it, the first
	 * failure pays the timeout and everything after it fails instantly until the window expires.
	 * It doubles as the recovery delay: a database that comes back is picked up on the next attempt
	 * after the window, which is why the value matches the log throttle rather than being longer.
	 */
	static final long FAILURE_BACKOFF_MS = 60_000;

	/** The driver the shipped {@code deps/mysql.jar} registers; used only for the error message. */
	private static final String DRIVER_CLASS = "com.mysql.jdbc.Driver";

	private final String url;
	private final String user;
	private final String password;
	private final int maxSize;

	private final Deque<Connection> idle = new ArrayDeque<>();
	/** Connections currently open: those sitting in {@link #idle} plus those on loan. */
	private int open;
	private long lastFailureLog;
	/** Wall-clock time before which connect attempts are skipped; 0 when the pool is not backing off. */
	private long retryAfter;

	/**
	 * @param url      JDBC URL, e.g. {@code jdbc:mysql://localhost/hiscores}
	 * @param user     database user, or {@code null} to omit it
	 * @param password database password, or {@code null} to omit it
	 * @param maxSize  most connections to hold open; clamped up to at least 1
	 */
	public ConnectionPool(String url, String user, String password, int maxSize) {
		if (url == null) {
			throw new IllegalArgumentException("url must not be null");
		}
		this.url = url;
		this.user = user;
		this.password = password;
		this.maxSize = Math.max(1, maxSize);
	}

	/**
	 * Takes a connection from the pool, opening one if there is room, or returns {@code null} if
	 * the database is unavailable, the driver is missing, or every slot is already on loan.
	 *
	 * <p>An idle connection is validated before it is handed out, so one the server has reaped
	 * (MySQL's {@code wait_timeout}, a restart) is discarded and replaced rather than returned.
	 */
	public synchronized Connection borrow() {
		Connection reused;
		while ((reused = idle.pollFirst()) != null) {
			if (healthy(reused)) {
				return reused;
			}
			closeQuietly(reused);
			open--;
		}
		if (open >= maxSize) {
			return null;
		}
		long now = System.currentTimeMillis();
		if (now < retryAfter) {
			// Known down: skip the connect so a logout cannot stall on the connect timeout.
			return null;
		}
		try {
			Connection created = openConnection();
			open++;
			lastFailureLog = 0; // a success clears the throttle, so the next failure is reported at once
			retryAfter = 0;
			return created;
		} catch (Exception e) {
			retryAfter = now + FAILURE_BACKOFF_MS;
			logFailure("connect to " + url + " failed", e);
			return null;
		}
	}

	/**
	 * Milliseconds until the pool will next attempt a connection, or {@code 0} when it is not
	 * backing off.
	 *
	 * <p>A non-zero value means the last connect failed and further {@link #borrow()} calls are
	 * being answered {@code null} without touching the network. Exposed so the backoff is
	 * observable rather than only inferable from timing.
	 */
	public synchronized long retryDelayMillis() {
		return Math.max(0, retryAfter - System.currentTimeMillis());
	}

	/** Returns a connection that was used successfully to the pool. */
	public synchronized void release(Connection connection) {
		if (connection == null) {
			return;
		}
		if (idle.size() < maxSize && healthy(connection)) {
			idle.addFirst(connection);
		} else {
			closeQuietly(connection);
			open--;
		}
	}

	/**
	 * Disposes of a connection that was not used successfully — its socket may be broken, so it
	 * must not go back into the pool.
	 */
	public synchronized void discard(Connection connection) {
		if (connection == null) {
			return;
		}
		closeQuietly(connection);
		open--;
	}

	/**
	 * Closes every idle connection. Connections currently on loan are the borrower's to return.
	 * Safe to call more than once.
	 */
	public synchronized void close() {
		Connection connection;
		while ((connection = idle.pollFirst()) != null) {
			closeQuietly(connection);
			open--;
		}
	}

	/** Connections held open but not currently on loan. */
	public synchronized int idleCount() {
		return idle.size();
	}

	/** Connections currently open, whether idle or on loan. */
	public synchronized int openCount() {
		return open;
	}

	/** The configured cap on simultaneous connections, after clamping. */
	public int maxSize() {
		return maxSize;
	}

	/** The JDBC URL this pool connects to. */
	public String url() {
		return url;
	}

	private Connection openConnection() throws SQLException {
		Properties properties = new Properties();
		if (user != null) {
			properties.setProperty("user", user);
		}
		if (password != null) {
			properties.setProperty("password", password);
		}
		properties.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_MS));
		properties.setProperty("socketTimeout", String.valueOf(SOCKET_TIMEOUT_MS));
		return DriverManager.getConnection(url, properties);
	}

	/**
	 * True when the connection is open and, where the driver can check, still talking to the
	 * server. A driver that does not implement {@code isValid} is trusted once open, because the
	 * alternative would be to throw away working connections.
	 */
	private static boolean healthy(Connection connection) {
		if (connection == null) {
			return false;
		}
		try {
			if (connection.isClosed()) {
				return false;
			}
		} catch (SQLException e) {
			return false;
		}
		try {
			return connection.isValid(VALIDATION_TIMEOUT_SECONDS);
		} catch (SQLFeatureNotSupportedException e) {
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	private static void closeQuietly(Connection connection) {
		try {
			connection.close();
		} catch (SQLException e) {
			// Closing is best-effort: the socket is going away either way.
		}
	}

	/** Reports a failure at most once per {@link #FAILURE_LOG_INTERVAL_MS}, plus the first one. */
	private void logFailure(String what, Throwable cause) {
		long now = System.currentTimeMillis();
		if (lastFailureLog != 0 && now - lastFailureLog < FAILURE_LOG_INTERVAL_MS) {
			return;
		}
		lastFailureLog = now;
		String message = cause == null ? null : cause.getMessage();
		if (message != null && message.contains("No suitable driver")) {
			message += " (is deps/mysql.jar on the classpath? it registers " + DRIVER_CLASS + ")";
		}
		System.out.println("[Hiscores] Database unavailable — " + what
				+ (message == null || message.isEmpty() ? "" : ": " + message));
	}
}
