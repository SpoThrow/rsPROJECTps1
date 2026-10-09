package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import server.Config;

/**
 * {@code BotProvisioning} — {@code BOT_ACCOUNTS.md} §4.1, the step that makes a spawned bot viable.
 *
 * <p><b>The failure this exists to prevent.</b> Account creation used to grant no kit at all, so a
 * config-spawned woodcutter owned no axe and {@code Gather} failed on its first click. The headline test
 * here is therefore the plain one: a created woodcutter has its axe.
 *
 * <p>These drive a bare {@link BotPlayer} rather than a real account file, because provisioning is pure
 * state mutation — {@code BotProfileSpawnTest} covers the on-disk path, and the two together pin both
 * halves without either duplicating the other.
 */
class BotProvisioningTest {

	private static final int BRONZE_AXE = 1351;
	private static final int BRONZE_PICKAXE = 1265;
	private static final int SMALL_FISHING_NET = 303;
	private static final int FLY_FISHING_ROD = 309;
	private static final int FEATHER = 314;
	private static final int TINDERBOX = 590;

	private static BotPlayer freshBot() {
		return new BotPlayer(0);
	}

	private static boolean has(BotPlayer bot, int itemId) {
		return bot.getItems().playerHasItem(itemId);
	}

	// ---- the point of the whole thing ---------------------------------------------------------

	@Test
	void aCreatedWoodcutterOwnsTheAxeItsScriptNeeds() {
		BotPlayer bot = freshBot();

		BotProvisioning.provision(bot, BotProfiles.WOODCUTTER);

		assertTrue(has(bot, BRONZE_AXE),
				"without an axe a spawned woodcutter cannot chop, which is the entire bug this fixes");
	}

	@Test
	void theDefaultKitCanGatherEveryResourceTheWorldSupports() {
		// LocationKind offers exactly three gatherable kinds — TREE, ROCK, FISHING — and a row that names
		// no profile is the case where the operator did not say which one the script wants. So the default
		// has to cover all three or the obvious config line silently fails.
		BotPlayer bot = freshBot();

		BotProvisioning.provision(bot, BotProfiles.DEFAULT);

		assertTrue(has(bot, BRONZE_AXE), "trees");
		assertTrue(has(bot, BRONZE_PICKAXE), "rocks");
		assertTrue(has(bot, SMALL_FISHING_NET), "fishing");
	}

	@Test
	void aFisherIsGivenTheToolsBothFishingMethodsNeed() {
		// Fishing is the one resource with two techniques, so the kit carries both rather than guessing
		// which water the bot ends up at.
		BotPlayer bot = freshBot();

		BotProvisioning.provision(bot, BotProfiles.FISHER);

		assertTrue(has(bot, SMALL_FISHING_NET), "a net for the shallows");
		assertTrue(has(bot, FLY_FISHING_ROD), "a rod for the river");
		assertTrue(has(bot, FEATHER), "and bait for the rod");
	}

	@Test
	void everyProfileActuallyGrantsTheItemsItNames() {
		for (BotProfiles.Profile profile : BotProfiles.all()) {
			BotPlayer bot = freshBot();
			BotProvisioning.provision(bot, profile);

			assertTrue(profile.itemCount() > 0, profile.name() + " grants nothing at all");
			for (int i = 0; i < profile.itemCount(); i++) {
				assertTrue(has(bot, profile.itemId(i)), profile.name() + " is missing item "
						+ profile.itemId(i) + " — addItem grants nothing for an id item.cfg does not have");
			}
		}
	}

	@Test
	void theSpecialisedKitsStayLean() {
		// The adventurer starter grants 2,000,000 coins and a suit of armour. A bot meant to look like an
		// ordinary newcomer must not, and a gatherer needs none of it — this pins that the kits did not
		// quietly inherit the starter table.
		BotPlayer bot = freshBot();

		BotProvisioning.provision(bot, BotProfiles.MINER);

		assertTrue(has(bot, BRONZE_PICKAXE));
		assertFalse(has(bot, 995), "no coin stack; a gathering bot does not need starting cash");
		assertFalse(has(bot, 1323), "no iron scimitar; the starter armour table is not reused");
	}

	// ---- the hitpoints trap -------------------------------------------------------------------

	@Test
	void hitpointsIsNotResetToOneByTheSkillReset() {
		// The trap this test exists for: Player's constructor seeds every skill to 1 EXCEPT hitpoints,
		// which starts at 10 with the XP for 10. A "set all skills to 1" loop would leave the account at a
		// single hitpoint — a character that is dead on arrival, and one nothing else here would catch.
		BotPlayer bot = freshBot();

		BotProvisioning.provision(bot, BotProfiles.DEFAULT);

		assertEquals(10, bot.skills.playerLevel[Config.HITPOINTS], "hitpoints stays at its starting level");
		assertEquals(1300, bot.skills.playerXP[Config.HITPOINTS], "and at the XP its constructor uses");
	}

	@Test
	void theOtherSkillsStartAtOne() {
		BotPlayer bot = freshBot();
		bot.skills.playerLevel[Config.WOODCUTTING] = 99; // as if the account had been trained

		BotProvisioning.provision(bot, BotProfiles.DEFAULT);

		assertEquals(1, bot.skills.playerLevel[Config.WOODCUTTING],
				"provisioning starts the character clean rather than inheriting the client's state");
		assertEquals(1, bot.skills.playerLevel[Config.MINING]);
		assertEquals(0, bot.skills.playerXP[Config.WOODCUTTING]);
	}

	// ---- the rest of the §4.1 checklist -------------------------------------------------------

	@Test
	void theTieFlagMatchesTheKitAndSurvivesAsTheOnlyOne() {
		BotPlayer bot = freshBot();

		BotProvisioning.provision(bot, BotProfiles.WOODCUTTER);

		assertTrue(bot.skillerPid, "a skiller kit sets the skiller flag");
		assertFalse(bot.adventurerPid);
		assertFalse(bot.pkerPid);

		BotProvisioning.clear(bot);
		assertFalse(bot.skillerPid, "clearing takes the tie flags back off");
		assertFalse(bot.adventurerPid);
		assertFalse(bot.pkerPid);
	}

	@Test
	void theCharacterIsPlacedWhereTheProfileSaysAndNotLeftFrozenInTheTutorial() {
		BotPlayer bot = freshBot();

		BotProvisioning.provision(bot, BotProfiles.MINER);

		assertEquals(BotProfiles.MINER.startX(), bot.position.absX);
		assertEquals(BotProfiles.MINER.startY(), bot.position.absY);
		assertEquals(BotProfiles.MINER.plane(), bot.position.heightLevel);
		assertEquals(BotProfiles.MINER.startX(), bot.position.teleportToX,
				"the teleport destination too, or the first tick walks the character back");

		// Client.initialize() freezes movement when addStarter is set (canWalk = false, plus a forced
		// dialogue). A frozen bot never ticks, so this is not cosmetic.
		assertFalse(bot.addStarter, "never the IP-gated starter path");
		assertTrue(bot.canWalk, "a frozen bot never acts");
	}

	@Test
	void theCharacterIsMarkedToBeSaved() {
		BotPlayer bot = freshBot();

		BotProvisioning.provision(bot, BotProfiles.DEFAULT);

		assertTrue(bot.saveFile);
		assertTrue(bot.saveCharacter);
		assertFalse(bot.newPlayer, "PlayerSave treats a 'new' client as one it must not write");
	}

	// ---- clear and reprovision -----------------------------------------------------------------

	@Test
	void clearEmptiesTheInventory() {
		BotPlayer bot = freshBot();
		BotProvisioning.provision(bot, BotProfiles.WOODCUTTER);
		assertTrue(has(bot, BRONZE_AXE));

		BotProvisioning.clear(bot);

		assertFalse(has(bot, BRONZE_AXE));
		assertEquals(28, bot.getItems().freeSlots(), "the whole inventory is free again");
	}

	@Test
	void reprovisioningReplacesTheKitRatherThanAddingToIt() {
		BotPlayer bot = freshBot();
		BotProvisioning.provision(bot, BotProfiles.WOODCUTTER);

		BotProvisioning.reprovision(bot, BotProfiles.MINER);

		assertTrue(has(bot, BRONZE_PICKAXE), "the new kit is there");
		assertFalse(has(bot, BRONZE_AXE), "and the old one is gone, not merely joined");
	}

	@Test
	void provisionAddsToTheInventoryWhileReprovisionReplaces() {
		// The contract is split, and this pins the half that is easy to assume wrongly: skills, tie and
		// position are idempotent, but items are additive — a bare second provision grants the kit again.
		// That is safe because createAccount provisions a brand-new empty character and reprovision is the
		// replace path (see the test above). Asserted so nobody "fixes" provision to clear, which would
		// wipe the inventory of any character it was ever called on twice.
		BotPlayer bot = freshBot();
		BotProvisioning.provision(bot, BotProfiles.WOODCUTTER);
		int afterOne = bot.getItems().freeSlots();

		BotProvisioning.provision(bot, BotProfiles.WOODCUTTER);

		assertTrue(bot.getItems().freeSlots() <= afterOne,
				"a second provision never reclaims slots; it only ever adds");
		assertTrue(has(bot, BRONZE_AXE));
	}

	@Test
	void aNullProfileOrBotIsIgnoredRatherThanThrowing() {
		BotProvisioning.provision(null, BotProfiles.DEFAULT);
		BotProvisioning.provision(freshBot(), null);
		BotProvisioning.clear(null);
		BotProvisioning.reprovision(null, BotProfiles.MINER);
	}

	// ---- the table -----------------------------------------------------------------------------

	@Test
	void profileNamesResolveCaseInsensitivelyAndUnknownNamesAreNull() {
		assertEquals(BotProfiles.WOODCUTTER, BotProfiles.named("WOODCUTTER"));
		assertEquals(BotProfiles.MINER, BotProfiles.named("  miner  "));
		assertNull(BotProfiles.named("no_such_profile"));
		assertNull(BotProfiles.named(null));
	}

	@Test
	void theDefaultProfileIsOneOfTheTableAndNamedAsSuch() {
		assertTrue(BotProfiles.names().contains(BotProfiles.DEFAULT.name()));
		assertEquals(BotProfiles.all().size(), BotProfiles.names().size());
		assertEquals(BotProfiles.DEFAULT, BotProfiles.named(BotProfiles.DEFAULT.name()));
	}

	/**
	 * The guard against a typo'd item id, which is the quietest way this table can break: {@code addItem}
	 * grants nothing for an id {@code item.cfg} does not define, so the bot would simply never gather and
	 * nothing would log.
	 *
	 * <p>Read from the real {@code Data/cfg/item.cfg} via the {@code workshopDataRoot} property, because
	 * the test task runs from {@code build/testwork} and cannot see {@code ./Data}.
	 */
	@Test
	void everyItemIdInTheTableIsReal() throws IOException {
		String dataRoot = System.getProperty("workshopDataRoot");
		assertTrue(dataRoot != null && !dataRoot.isBlank(),
				"the test task must set workshopDataRoot to Data/; see build.gradle");

		Set<Integer> defined = definedItemIds(Path.of(dataRoot, "cfg", "item.cfg"));

		for (BotProfiles.Profile profile : BotProfiles.all()) {
			for (int i = 0; i < profile.itemCount(); i++) {
				int id = profile.itemId(i);
				assertTrue(defined.contains(id), profile.name() + " grants item " + id
						+ ", which Data/cfg/item.cfg does not define — addItem would silently grant nothing");
			}
		}
	}

	/** Every id {@code item.cfg} defines. Its rows look like {@code item = 1351\tBronze_axe\t...}. */
	private static Set<Integer> definedItemIds(Path itemCfg) throws IOException {
		// Latin-1, not UTF-8: item.cfg is a legacy config with a few non-UTF-8 bytes in its descriptions,
		// and reading it as UTF-8 throws MalformedInputException. Every byte maps to a character here, so
		// this cannot fail, and the field that matters (the id) is ASCII.
		List<String> lines = Files.readAllLines(itemCfg, StandardCharsets.ISO_8859_1);
		Set<Integer> ids = new HashSet<Integer>();
		for (String raw : lines) {
			String line = raw.trim();
			if (!line.startsWith("item")) {
				continue;
			}
			int equals = line.indexOf('=');
			if (equals < 0) {
				continue;
			}
			String[] parts = line.substring(equals + 1).trim().split("\\s+");
			if (parts.length == 0 || parts[0].isEmpty()) {
				continue;
			}
			try {
				ids.add(Integer.parseInt(parts[0]));
			} catch (NumberFormatException e) {
				// A malformed row is item.cfg's problem, not this test's; skip it.
			}
		}
		assertFalse(ids.isEmpty(), "no ids parsed out of " + itemCfg + " — the format must have changed");
		return ids;
	}
}
