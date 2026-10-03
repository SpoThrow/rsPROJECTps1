package server.game.players.actions.dialogues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DialogueRegistryTest {

	private static final int[] MIGRATED = {
			1, 2, 3, 5, 6, 11, 12, 13, 14, 15, 16, 17, 18, 19, 28, 29, 58, 59, 60, 61,
			62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 75, 76, 77, 78, 79, 80, 81, 82, 83,
			84, 85, 86, 87, 88, 89, 150, 300, 400, 401, 402, 404, 408, 409, 411, 412, 413, 414, 415, 416,
			417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 431, 432, 433, 434, 435, 436,
			437, 438, 439, 440, 441, 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455, 456,
			458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 470, 472, 474, 476, 477, 478, 479, 480, 481,
			482, 483, 484, 485, 486, 487, 488, 490, 500, 501, 502, 504, 505, 506, 507, 512, 513, 515, 516, 519,
			520, 521, 522, 549, 550, 551, 552, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566,
			567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586,
			587, 588, 589, 590, 591, 592, 593, 594, 595, 596, 597, 598, 600, 601, 602, 603, 604, 605, 606, 607,
			608, 610, 611, 612, 613, 614, 615, 619, 620, 621, 622, 624, 690, 750, 751, 752, 996, 997, 998, 999,
			1000, 1001, 1002, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1099, 1100, 1103, 1104, 1105, 1106, 1109, 1149,
			1150, 1151, 1152, 1155, 1156, 1157, 1173, 1176, 1181, 1192, 1193, 1194, 2000, 2001, 8801, 9001,
	};

	@Test
	void registersEveryMigratedDialogue() {
		for (int id : MIGRATED) {
			assertTrue(DialogueRegistry.isRegistered(id), "dialogue " + id);
		}
		assertEquals(MIGRATED.length, DialogueRegistry.size());
	}

	@Test
	void migratedIdsAreUnique() {
		final Set<Integer> seen = new HashSet<>();
		for (int id : MIGRATED) assertTrue(seen.add(id), "duplicate dialogue id " + id);
	}

	@Test
	void unknownDialogueIsNotHandled() {
		assertFalse(DialogueRegistry.dispatch(null, null, Integer.MIN_VALUE));
	}

	@Test
	void rejectsDuplicateRegistration() {
		assertThrows(IllegalStateException.class, () -> DialogueRegistry.register(1, (dh, c, id) -> { }));
	}
}
