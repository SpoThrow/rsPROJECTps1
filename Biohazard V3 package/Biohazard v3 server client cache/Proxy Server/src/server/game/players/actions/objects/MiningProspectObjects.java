package server.game.players.actions.objects;

import server.content.skills.Mining;

/**
 * The "prospect" second-click actions on rocks, lifted out of {@code ActionHandler}.
 *
 * <p>{@code ORE_IDS} and {@code ORE_NAMES} are parallel: row {@code i} of the ids is
 * the rocks that report ore {@code i}. The names carry the original wording, including
 * {@code "coal"} having no "ore" suffix. {@code MiningProspectObjectsTest} pins the
 * alignment, because a slip here prospects the wrong rock.
 */
public final class MiningProspectObjects {

	private static final int[][] ORE_IDS = {
			{ 2090, 2091, 3042 }, // copper
			{ 2094, 2095, 3043 }, // tin
			{ 2110 },             // blurite
			{ 2092, 2093 },       // iron
			{ 2100, 2101 },       // silver
			{ 2098, 2099 },       // gold
			{ 2096, 2097 },       // coal
			{ 2102, 2103 },       // mithril
			{ 2104, 2105 },       // adamantite
			{ 2106, 2107 },       // runite
	};

	private static final String[] ORE_NAMES = {
			"copper ore",
			"tin ore",
			"blurite ore",
			"iron ore",
			"silver ore",
			"gold ore",
			"coal",
			"mithril ore",
			"adamantite ore",
			"runite ore",
	};

	// Rocks that have nothing worth reporting when prospected.
	private static final int[] NOTHING = { 450, 451 };

	private MiningProspectObjects() {
	}

	static void register() {
		for (int ore = 0; ore < ORE_IDS.length; ore++) {
			final String name = ORE_NAMES[ore];
			for (int id : ORE_IDS[ore]) {
				ObjectHandler.register(id, ObjectClick.SECOND,
						(c, objectType, objectX, objectY) -> Mining.prospectRock(c, name));
			}
		}
		for (int id : NOTHING) {
			ObjectHandler.register(id, ObjectClick.SECOND,
					(c, objectType, objectX, objectY) -> Mining.prospectNothing(c));
		}
	}

	// Package-private for MiningProspectObjectsTest.
	static int[][] oreIds() {
		int[][] copy = new int[ORE_IDS.length][];
		for (int i = 0; i < ORE_IDS.length; i++) {
			copy[i] = ORE_IDS[i].clone();
		}
		return copy;
	}

	static String[] oreNames() {
		return ORE_NAMES.clone();
	}

	static int[] nothing() {
		return NOTHING.clone();
	}
}
