package server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link ConnectionPool}'s contract when there is no database to talk to, which is the
 * state the server is expected to run in (the vote system degrades the same way).
 *
 * <p>The tests point at {@code 127.0.0.1:1} — a privileged loopback port nothing listens on — so
 * a connect is refused immediately and no MySQL server is required. They pin the property that
 * matters most: a database that is down must surface as {@code null} from {@link
 * ConnectionPool#borrow()}, never as an exception on the game thread.
 */
class ConnectionPoolTest {

	/** Nothing listens on port 1, so a connect is refused at once. */
	private static final String DEAD_URL = "jdbc:mysql://127.0.0.1:1/nonexistent";

	private static ConnectionPool deadPool() {
		return new ConnectionPool(DEAD_URL, "user", "password", 2);
	}

	@Test
	void borrowingFromAnUnreachableServerReturnsNullInsteadOfThrowing() {
		final ConnectionPool pool = deadPool();
		try {
			assertNull(pool.borrow(), "a refused connection must surface as null");
			assertNull(pool.borrow(), "a second attempt must also degrade rather than throw");
			assertEquals(0, pool.openCount(), "a failed connect must not count as an open connection");
			assertEquals(0, pool.idleCount(), "a failed connect must leave nothing idle");
		} finally {
			pool.close();
		}
	}

	@Test
	void returningNullsIsHarmless() {
		final ConnectionPool pool = deadPool();
		try {
			pool.release(null);
			pool.discard(null);
			assertEquals(0, pool.openCount());
		} finally {
			pool.close();
		}
	}

	@Test
	void aFailedConnectMakesThePoolBackOffRatherThanRetryEveryCall() {
		// The hiscore write runs on the game thread on every logout, so paying a connect timeout
		// on each one would stall the tick — and the shutdown sweep would pay it once per player
		// online. After the first failure the pool must stop attempting connects until the window
		// expires, which also doubles as the recovery delay.
		final ConnectionPool pool = deadPool();
		try {
			assertEquals(0, pool.retryDelayMillis(), "a fresh pool is not backing off");

			assertNull(pool.borrow(), "the first borrow really does try the database");

			assertTrue(pool.retryDelayMillis() > 0,
					"a failed connect must put the pool into backoff");
			assertTrue(pool.retryDelayMillis() <= ConnectionPool.FAILURE_BACKOFF_MS,
					"the backoff must expire, so it cannot exceed the configured window");

			assertNull(pool.borrow(), "while backing off, borrow still degrades to null");
			assertEquals(0, pool.openCount(), "a skipped attempt opens nothing");
		} finally {
			pool.close();
		}
	}

	@Test
	void closingIsSafeAndIdempotentWhenNothingWasBorrowed() {
		final ConnectionPool pool = deadPool();
		pool.close();
		pool.close();
		assertEquals(0, pool.openCount());
		assertEquals(0, pool.idleCount());
	}

	@Test
	void thePoolCapIsClampedToAtLeastOne() {
		// A cap of 0 or below would make borrow() always fail; clamp it rather than allow that.
		assertTrue(deadPoolWithCap(0).maxSize() >= 1);
		assertTrue(deadPoolWithCap(-5).maxSize() >= 1);
	}

	@Test
	void aNullUrlIsRejectedAtConstruction() {
		assertThrows(IllegalArgumentException.class,
				() -> new ConnectionPool(null, "user", "password", 2));
	}

	@Test
	void theMysqlDriverIsRegisteredSoAReachableServerCouldActuallyBeUsed() throws Exception {
		// The pool deliberately does not call Class.forName: it relies on JDBC 4 auto-registration
		// from deps/mysql.jar's META-INF/services. If that jar ever leaves the classpath this fails,
		// which distinguishes "database is down" from "driver was never loaded" — otherwise both
		// look like a null borrow in production.
		assertNotNull(java.sql.DriverManager.getDriver("jdbc:mysql://localhost/hiscores"),
				"the MySQL driver must self-register from deps/mysql.jar");
	}

	private static ConnectionPool deadPoolWithCap(int cap) {
		return new ConnectionPool(DEAD_URL, "user", "password", cap);
	}
}
