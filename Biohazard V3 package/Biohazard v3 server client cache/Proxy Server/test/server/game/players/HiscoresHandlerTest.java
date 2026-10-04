package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import server.util.ConnectionPool;

/**
 * Pins the hiscores layer after it moved off its {@code public static Connection}/{@code Statement}
 * pair onto a {@link ConnectionPool}.
 *
 * <p>Without a MySQL server the only things worth asserting are the ones that must hold when the
 * database is <em>absent</em>: a write reports failure instead of throwing on the game thread, an
 * unusable player is rejected, and — the point of the refactor — the class no longer exposes a
 * shared, mutable connection that anything could close out from under a reader.
 */
class HiscoresHandlerTest {

	/** Nothing listens on port 1, so a connect is refused at once and no MySQL is needed. */
	private static final String DEAD_URL = "jdbc:mysql://127.0.0.1:1/nonexistent";

	@Test
	void aWriteWithNoDatabaseReportsFailureInsteadOfThrowing() {
		final ConnectionPool pool = new ConnectionPool(DEAD_URL, "user", "password", 1);
		try {
			final Client player = new Client(null, 1);
			player.playerName = "Tester";
			assertFalse(HiscoresHandler.saveHighScore(player, pool),
					"an unreachable database must read as a failed save, not an exception");
		} finally {
			pool.close();
		}
	}

	@Test
	void anUnusablePlayerIsRejectedWithoutTouchingTheDatabase() {
		final ConnectionPool pool = new ConnectionPool(DEAD_URL, "user", "password", 1);
		try {
			assertFalse(HiscoresHandler.saveHighScore((Client) null, pool), "a null player");
			final Client nameless = new Client(null, 1);
			nameless.playerName = null;
			assertFalse(HiscoresHandler.saveHighScore(nameless, pool), "a player with no name");
		} finally {
			pool.close();
		}
	}

	@Test
	void theHandlerNoLongerSharesAConnectionThroughPublicStatics() {
		// The regression this guards: `public static Connection con` / `public static Statement stmt`
		// let any caller reach in and close the connection another caller was reading from.
		for (Field field : HiscoresHandler.class.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())) {
				continue;
			}
			assertFalse(Connection.class.isAssignableFrom(field.getType()),
					"public static JDBC Connection field: " + field.getName());
			assertFalse(Statement.class.isAssignableFrom(field.getType()),
					"public static JDBC Statement field: " + field.getName());
		}
	}

	@Test
	void overallSumsExactlyTheTwentyThreeSkillsTheTableHolds() {
		final Client player = new Client(null, 1);
		final int[] before = HiscoresHandler.getOverall(player);

		player.skills.playerXP[0] += 1_000;   // first written skill
		player.skills.playerXP[22] += 500;    // last written skill (lvl_23 / xp_23)
		player.skills.playerXP[23] += 7_000;  // the 24th slot, which the table has no column for

		final int[] after = HiscoresHandler.getOverall(player);

		assertEquals(2, after.length, "returns {totalLevel, totalXp}");
		assertEquals(before[1] + 1_500, after[1],
				"totalXp covers skills 0..22 and must ignore slot 23");
	}

	@Test
	void theSingleUpdateBindsExactlyAsManyValuesAsItHasPlaceholders() {
		// write() binds a level and an xp for each of the 23 skills, then total_exp, total_lvl and
		// the username. A typo in either loop would only ever fail against a live database, so the
		// count is pinned here instead.
		final String sql = HiscoresHandler.buildUpdateSql();
		int placeholders = 0;
		for (int i = 0; i < sql.length(); i++) {
			if (sql.charAt(i) == '?') {
				placeholders++;
			}
		}
		assertEquals(23 * 2 + 3, placeholders, "46 skill values plus total_exp, total_lvl and the username");
	}
}
