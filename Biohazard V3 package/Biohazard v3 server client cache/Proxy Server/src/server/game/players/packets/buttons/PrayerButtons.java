package server.game.players.packets.buttons;

import server.game.players.Client;

/**
 * The prayer-book toggle buttons, migrated out of the switch in
 * {@code ClickingButtons}.
 *
 * <p>Positional: {@code IDS[prayerIndex]} is the button that activates that prayer,
 * and the indices are the contiguous 0..25 {@code activatePrayer} expects. The names
 * are carried over from the switch, because knowing which index is "piety" is not
 * otherwise recoverable from this file.
 */
public final class PrayerButtons {

	private static final int[] IDS = {
			21233, // 0  thick skin
			21234, // 1  burst of strength
			21235, // 2  charity of thought
			70080, // 3  range
			70082, // 4  mage
			21236, // 5  rockskin
			21237, // 6  super human
			21238, // 7  improved reflexes
			21239, // 8  hawk eye
			21240, // 9
			21241, // 10 protect item
			70084, // 11 range
			70086, // 12 mage
			21242, // 13 steel skin
			21243, // 14 ultimate strength
			21244, // 15 incredible reflexes
			21245, // 16 protect from magic
			21246, // 17 protect from range
			21247, // 18 protect from melee
			70088, // 19 range
			70090, // 20 mystic
			2171,  // 21 retribution
			2172,  // 22 redemption
			2173,  // 23 smite
			70092, // 24 chivalry
			70094, // 25 piety
	};

	private PrayerButtons() {
	}

	static void register() {
		for (int prayer = 0; prayer < IDS.length; prayer++) {
			final int index = prayer;
			ButtonHandler.register(IDS[prayer], (c, actionButtonId) -> c.getCombat().activatePrayer(index));
		}
	}

	// Package-private for PrayerButtonsTest, which pins the transcription.
	static int[] ids() {
		return IDS.clone();
	}
}
