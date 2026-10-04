package server.game.players;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import server.Configuration;
import server.util.ConnectionPool;

/**
 * Writes a player's public hiscores row (the {@code hs} table) and reads nothing back.
 *
 * <p><b>What this replaced, and why.</b> The original class held its connection in
 * {@code public static Connection con} with a {@code public static Statement stmt}, opened a
 * fresh connection on every save, closed that shared connection as a side effect of any failed
 * statement, and never closed its {@code ResultSet}. It also interpolated
 * {@code player.playerName} straight into SQL and issued <em>24 separate</em> {@code UPDATE}
 * round trips per save (one per skill plus the totals). This version borrows a validated
 * connection from a {@link ConnectionPool}, closes every statement and result set, binds the name
 * as a parameter, and writes all 23 skills and both totals in a single {@code UPDATE}.
 *
 * <p><b>Two behaviour changes are deliberate.</b> (1) The old code inserted the username-only row
 * on the first save and returned early, so a character's levels only reached the table on the
 * <em>second</em> logout; this version inserts and fills in the same call. (2) {@link
 * #saveHighScore(Client)} used to return {@code false} after successfully updating an existing
 * row and {@code true} only for the insert path — the opposite of its name. It now returns
 * {@code true} whenever the write succeeded. No caller reads the value today (the one live call
 * site, {@code Client.destruct}, ignores it), so neither change is observable in game.
 *
 * <p>Credentials come from {@link Configuration} under the same keys and with the same fallbacks
 * as before, so a checkout with no {@code Data/server.properties} connects exactly as it used to.
 */
public final class HiscoresHandler {

	/** Skills written to the table: {@code lvl_1}..{@code lvl_23} and {@code xp_1}..{@code xp_23}. */
	private static final int SKILL_COUNT = 23;

	/** Cap written to the table, so a boosted or virtual level never reaches the website. */
	private static final int MAX_LEVEL = 99;

	/** Simultaneously-open connections kept for hiscore writes. */
	private static final int POOL_SIZE = ConnectionPool.DEFAULT_MAX_SIZE;

	private static final String UPDATE_SQL = buildUpdateSql();

	private static final Object POOL_LOCK = new Object();
	private static ConnectionPool pool;

	private HiscoresHandler() {
	}

	/** The shared pool, built on first use so a boot with no database is unaffected. */
	private static ConnectionPool pool() {
		synchronized (POOL_LOCK) {
			if (pool == null) {
				String host = Configuration.get().getString("hiscores.host", "localhost");
				String database = Configuration.get().getString("hiscores.database", "hiscores");
				String user = Configuration.get().getString("hiscores.user", "root");
				String password = Configuration.get().getString("hiscores.password", "------");
				pool = new ConnectionPool("jdbc:mysql://" + host + "/" + database, user, password, POOL_SIZE);
			}
			return pool;
		}
	}

	/**
	 * Saves the player's hiscores row.
	 *
	 * @return {@code true} when the row was written; {@code false} when the player is unusable or
	 *         the database is unavailable, in which case nothing is thrown at the caller.
	 */
	public static boolean saveHighScore(Client player) {
		return saveHighScore(player, pool());
	}

	/**
	 * Points the shared pool somewhere else, or back at lazy construction when passed {@code null}.
	 *
	 * <p>Exists so a test can drive the <em>whole</em> logout path — {@code Client.destruct},
	 * which calls {@link #saveHighScore(Client)} with no pool argument — at a refused loopback
	 * port, instead of at whatever MySQL the machine running the tests happens to have. Without
	 * it, those tests would either block on a connect timeout or silently write real rows.
	 */
	static void usePool(ConnectionPool replacement) {
		synchronized (POOL_LOCK) {
			pool = replacement;
		}
	}

	/** Saves through a given pool. Package-private so tests can drive a pool with no server behind it. */
	static boolean saveHighScore(Client player, ConnectionPool pool) {
		if (player == null || player.playerName == null) {
			return false;
		}
		Connection connection = pool.borrow();
		if (connection == null) {
			return false; // database unavailable — the caller has nothing to act on
		}
		boolean saved = false;
		try {
			write(player, connection);
			saved = true;
		} catch (SQLException e) {
			System.out.println("Hiscores Error, could not save highscores for " + player.playerName + ".");
		} finally {
			// A failed write may have broken the connection, so only a clean one goes back.
			if (saved) {
				pool.release(connection);
			} else {
				pool.discard(connection);
			}
		}
		return saved;
	}

	private static void write(Client player, Connection connection) throws SQLException {
		if (!exists(player, connection)) {
			try (PreparedStatement insert = connection.prepareStatement("INSERT INTO `hs` (`username`) VALUES (?)")) {
				insert.setString(1, player.playerName);
				insert.executeUpdate();
			}
		}
		int[] overall = getOverall(player);
		try (PreparedStatement update = connection.prepareStatement(UPDATE_SQL)) {
			int column = 1;
			for (int i = 0; i < SKILL_COUNT; i++) {
				int level = player.getLevelForXP(player.skills.playerXP[i]);
				if (level > MAX_LEVEL) {
					level = MAX_LEVEL;
				}
				update.setInt(column++, level);
				update.setInt(column++, player.skills.playerXP[i]);
			}
			update.setInt(column++, overall[1]); // total_exp
			update.setInt(column++, overall[0]); // total_lvl
			update.setString(column, player.playerName);
			update.executeUpdate();
		}
		System.out.println("Highscores have been updated for " + player.playerName);
	}

	private static boolean exists(Client player, Connection connection) throws SQLException {
		try (PreparedStatement select = connection.prepareStatement("SELECT 1 FROM `hs` WHERE `username` = ? LIMIT 1")) {
			select.setString(1, player.playerName);
			try (ResultSet rs = select.executeQuery()) {
				return rs.next();
			}
		}
	}

	/** @return {@code {totalLevel, totalXp}} over the same 23 skills the table holds. */
	public static int[] getOverall(Client player) {
		int totalLevel = 0;
		int totalXp = 0;
		for (int i = 0; i < SKILL_COUNT; i++) {
			totalLevel += player.getLevelForXP(player.skills.playerXP[i]);
		}
		for (int i = 0; i < SKILL_COUNT; i++) {
			totalXp += player.skills.playerXP[i];
		}
		return new int[] { totalLevel, totalXp };
	}

	/**
	 * One {@code UPDATE} naming every column, so a save is a single round trip instead of 24. The
	 * placeholder order matches the binding order in {@link #write}: for each skill a level then
	 * its xp, then {@code total_exp}, {@code total_lvl}, and the username for the {@code WHERE}.
	 *
	 * <p>Package-private so a test can assert its placeholder count still matches what
	 * {@link #write} binds — a mismatch would only ever fail against a live database.
	 */
	static String buildUpdateSql() {
		StringBuilder sql = new StringBuilder("UPDATE `hs` SET ");
		for (int i = 1; i <= SKILL_COUNT; i++) {
			sql.append('`').append("lvl_").append(i).append("`=?, ");
			sql.append('`').append("xp_").append(i).append("`=?, ");
		}
		sql.append("`total_exp`=?, `total_lvl`=? WHERE `username`=?");
		return sql.toString();
	}
}
