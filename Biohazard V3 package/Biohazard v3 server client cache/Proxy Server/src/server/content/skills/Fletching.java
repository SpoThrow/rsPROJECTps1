package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.Player;
import core.util.Misc;


public class Fletching {

	public static boolean fletching;
	
	public enum Bolts {
		BRONZEBOLT(819, 314, 877, 5, 9),
		IRONBOLT(820, 314, 9140, 15, 39),
		STEELBOLT(821, 314, 9141, 35, 46),
		MITHRILBOLT(822, 314, 9142, 50, 54),
		ADAMANTBOLT(823, 314, 9143, 70, 61),
		RUNITEBOLT(824, 314, 9144, 100, 69);
		
		public int item1, item2, outcome, xp, levelReq;
		private Bolts(int item1, int item2, int outcome, int xp, int levelReq) {
			this.item1 = item1;
			this.item2 = item2;
			this.outcome = outcome;
			this.xp = xp;
			this.levelReq = levelReq;
		}
		public int getItem1() {
			return item1;
		}

		public int getItem2() {
			return item2;
		}

		public int getOutcome() {
			return outcome;
		}

		public int getXp() {
			return xp;
		}

		public int getLevelReq() {
			return levelReq;
		}
	}
	
	/**
	 * Finds a bolt recipe by its unfinished bolt id.
	 *
	 * <p>This used to match {@code getItem2()}, which is {@code 314} (feathers) for every entry in
	 * the table. So it returned {@code BRONZEBOLT} whenever it was asked about feathers and
	 * {@code null} for every actual bolt, which is why iron through runite bolts could not be
	 * made. Matching the bolt column is what makes the lookup mean what its name says.
	 */
	public static Bolts forBolts(int id) {
		for (Bolts bolts : Bolts.values()) {
			if (bolts.getItem1() == id) {
				return bolts;
			}
		}
		return null;
	}

	public enum Arrows {
		HEADLESS(52, 314, 53, 15, 1),
		BRONZE(53, 39, 882, 40, 1),
		IRON(53, 40, 884, 58, 15),
		STEEL(53, 41, 886, 95, 30),
		MITHRIL(53, 42, 888, 132, 45),
		ADAMANT(53, 43, 890, 170, 60),
		RUNE(53, 44, 892, 207, 75);

		public int item1;
		public int item2;
		public int outcome;
		public int xp;
		public int levelReq;
		private Arrows(int item1, int item2, int outcome, int xp, int levelReq) {
			this.item1 = item1;
			this.item2 = item2;
			this.outcome = outcome;
			this.xp = xp;
			this.levelReq = levelReq;
		}
		public int getItem1() {
			return item1;
		}

		public int getItem2() {
			return item2;
		}

		public int getOutcome() {
			return outcome;
		}

		public int getXp() {
			return xp;
		}

		public int getLevelReq() {
			return levelReq;
		}
	}

	public static Arrows forArrow(int id) {
		for (Arrows ar : Arrows.values()) {
			if (ar.getItem2() == id) {
				return ar;
			}
		}
		return null;
	}

	public static int getPrimary(int item1, int item2) {
		return item1 == 52 || item1 == 53 ? item2 : item1;
	}

	/**
	 * Makes arrows: fifteen at a time, from fifteen of each supply.
	 *
	 * <p>Fifteen is the OSRS batch size — a click makes fifteen arrows, not one and not a whole
	 * inventory — so this stays one action per click. What changes is that the action now runs
	 * on the game tick instead of completing inside the click, so it is interruptible and the
	 * supply check is re-done when it actually executes.
	 */
	public static void makeArrows(Client c, int item1, int item2) {
		Arrows arr = forArrow(getPrimary(item1, item2));
		if (arr == null) {
			return;
		}
		fletchBatch(c, arr.getOutcome(), ARROWS_PER_ACTION, arr.getXp(), arr.getLevelReq(),
				arr.getItem1(), ARROWS_PER_ACTION, arr.getItem2(), ARROWS_PER_ACTION,
				"arrows", null);
	}

	/**
	 * Makes bolts: ten at a time, from ten unfinished bolts and ten feathers.
	 *
	 * <p><b>The bolt is looked up in whichever argument holds it.</b> It used to be looked up in
	 * the first argument only, and {@code forBolts} matched the feather column, so this method
	 * had two failures at once: called as (bolts, feathers) the lookup found nothing and the
	 * click silently did nothing, and called as (feathers, bolts) it always resolved to bronze,
	 * so iron through runite bolts could not be made at all.
	 */
	public static void makeBolts(Client c, int item1, int item2) {
		Bolts bolts = forBolts(item1);
		if (bolts == null) {
			bolts = forBolts(item2);
		}
		if (bolts == null) {
			return;
		}
		fletchBatch(c, bolts.getOutcome(), BOLTS_PER_ACTION, bolts.getXp(), bolts.getLevelReq(),
				bolts.getItem1(), BOLTS_PER_ACTION, bolts.getItem2(), BOLTS_PER_ACTION,
				"bolts", null);
	}

	/**
	 * One ticked fletching action that consumes fixed quantities of two materials and produces a
	 * fixed quantity of product.
	 *
	 * <p>Shared by arrow making, bolt making and bolt tipping, which differ only in their numbers
	 * and their wording. It is one action, not a loop: all three are batch recipes in OSRS — a
	 * click makes fifteen arrows or ten bolts and then stops — so what this buys is tick
	 * pacing and a re-checked supply count, not repetition.
	 *
	 * @param successMessage sent on completion, or {@code null} to stay silent as arrow and bolt
	 *                       making always have been
	 */
	private static void fletchBatch(final Client c, final int product, final int productCount,
			final int xp, int levelReq, final int mat1, final int mat1Cost, final int mat2,
			final int mat2Cost, final String what, final String successMessage) {
		if (c.playerFletch) {
			return;
		}
		if (c.skills.playerLevel[Player.playerFletching] < levelReq) {
			c.sendMessage("You need a fletching level of at least " + levelReq + " to fletch this.");
			return;
		}
		if (!hasSupplies(c, mat1, mat1Cost, mat2, mat2Cost)) {
			c.sendMessage("You must have at least " + mat1Cost + " of each supply to make " + what + ".");
			return;
		}

		c.playerFletch = true;
		c.startAnimation(FLETCH_ANIMATION);

		CycleEventHandler.addEvent(FLETCH_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				// Re-check rather than trust the click: two ticks have passed, and the player can
				// have dropped or banked a supply in them.
				if (!c.playerFletch || !hasSupplies(c, mat1, mat1Cost, mat2, mat2Cost)) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(mat1, mat1Cost);
				if (mat2Cost > 0) {
					c.getItems().deleteItem2(mat2, mat2Cost);
				}
				c.getItems().addItem(product, productCount);
				if (xp > 0) {
					c.getPA().addSkillXP(xp * Config.FLETCHING_EXPERIENCE, Player.playerFletching);
				}
				if (successMessage != null) {
					c.sendMessage(successMessage);
				}
				container.stop();
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, 2);
	}

	/** True when the player holds the required quantity of every material in the recipe. */
	private static boolean hasSupplies(Client c, int mat1, int mat1Cost, int mat2, int mat2Cost) {
		return c.getItems().playerHasItem(mat1, mat1Cost)
				&& (mat2Cost <= 0 || c.getItems().playerHasItem(mat2, mat2Cost));
	}
	
	/**
	 * The fletching table: which log makes which product, for how much xp and at what level.
	 *
	 * <p>Public like {@link Bolts} and {@link Arrows} so the tables can be read from outside the
	 * package — {@code FletchingTest} pins the rows and the QOL validator checks every id in them.
	 * A wrong product id here is a bow that silently does not exist rather than a compile error,
	 * so these rows are worth being able to look at from a test.
	 */
	public enum Fletch {

		// Arrow shafts are level 1 in OSRS, and the old batch code never checked their level at
		// all because the shafts branch had no guard. The 15 here would have been enforced for
		// the first time by the ticked rewrite and would have taken shafts away from anyone
		// below 15, so it is corrected to the real requirement rather than kept.
		ARROWSHAFTS(1511, 52, 5, 1),

		SHORTBOW(1511, 841, 5, 5),
		LONGBOW(1511, 839, 10, 10),

		OAKSBOW(1521, 843, 17, 20),
		OAKLBOW(1521, 845, 25, 25),

		WILLOWSBOW(1519, 849, 34, 35),
		WILLOWLBOW(1519, 847, 42, 40),

		MAPLESBOW(1517, 853, 50, 50),
		MAPLELBOW(1517, 851, 59, 55),

		YEWSBOW(1515, 857, 68, 65),
		YEWLBOW(1515, 855, 75, 70),

		MAGICSBOW(1513, 861, 84, 80),
		MAGICLBOW(1513, 859, 92, 87);

		public int logID, unstrungBow, xp, levelReq;

		private Fletch(int logID, int unstrungBow, int xp, int levelReq) {
			this.logID = logID;
			this.unstrungBow = unstrungBow;
			this.xp = xp;
			this.levelReq = levelReq;
		}

		public int getLogID() {
			return logID;
		}

		public int getBowID() {
			return unstrungBow;
		}

		public int getXp() {
			return xp;
		}

		public int getLevelReq() {
			return levelReq;
		}
	}

	static Fletch forBow(int id) {
		for (Fletch fl : Fletch.values()) {
			if (fl.getBowID() == id) {
				return fl;
			}
		}
		return null;
	}
	public static void handleLog(Client c, int item1, int item2) {
		openFletching(c, (item1 == 946) ? item2 : item1);
	}

	public static void resetFletching(Client c) {
		c.playerIsFletching = false;
		c.log = -1;
		cancel(c);
	}

	/**
	 * Stops a running fletching action, if one is running.
	 *
	 * <p>Keyed on its own event id rather than on the player: {@code stopEvents(c)} would stop
	 * <em>every</em> event the player owns, and they own other skills' events too — a walk
	 * while fletching next to a fire would put the fire out.
	 *
	 * <p>Called from {@link #resetFletching}, which {@code PlayerAssistant.resetVariables}
	 * reaches on every walk, so walking away ends the action the way it ends cooking and
	 * mining.
	 */
	public static void cancel(Client c) {
		if (c.playerFletch) {
			c.playerFletch = false;
			CycleEventHandler.stopEvents(c, FLETCH_EVENT);
			c.startAnimation(65535);
		}
	}

	public static void handleFletchingClick(Client c, int abutton) {
		switch (abutton) {
		case 34185:
			switch (c.log) {
			case 1511: //Normal log
				fletchBow(c, 841, 1);
				break;
			case 1521: //Oak log
				fletchBow(c, 843, 1);
				break;
			case 1519: //Willow log
				fletchBow(c, 849, 1);
				break;
			case 1517: //Maple log
				fletchBow(c, 853, 1);
				break;
			case 1515: //Yew log
				fletchBow(c, 857, 1);
				break;
			case 1513: //Magic logs
				fletchBow(c, 861, 1);
				break;
			}
			break;
		case 34184:
			switch (c.log) {
			case 1511: //Normal log
				fletchBow(c, 841, 5);
				break;
			case 1521: //Oak log
				fletchBow(c, 843, 5);
				break;
			case 1519: //Willow log
				fletchBow(c, 849, 5);
				break;
			case 1517: //Maple log
				fletchBow(c, 853, 5);
				break;
			case 1515: //Yew log
				fletchBow(c, 857, 5);
				break;
			case 1513: //Magic logs
				fletchBow(c, 861, 5);
				break;
			}
			break;
		case 34183:
			switch (c.log) {
			case 1511: //Normal log
				fletchBow(c, 841, 10);
				break;
			case 1521: //Oak log
				fletchBow(c, 843, 10);
				break;
			case 1519: //Willow log
				fletchBow(c, 849, 10);
				break;
			case 1517: //Maple log
				fletchBow(c, 853, 10);
				break;
			case 1515: //Yew log
				fletchBow(c, 857, 10);
				break;
			case 1513: //Magic logs
				fletchBow(c, 861, 10);
				break;
			}
			break;
		case 34182:
			switch (c.log) {
			case 1511: //Normal log
				fletchBow(c, 841, 28);
				break;
			case 1521: //Oak log
				fletchBow(c, 843, 28);
				break;
			case 1519: //Willow log
				fletchBow(c, 849, 28);
				break;
			case 1517: //Maple log
				fletchBow(c, 853, 28);
				break;
			case 1515: //Yew log
				fletchBow(c, 857, 28);
				break;
			case 1513: //Magic logs
				fletchBow(c, 861, 28);
				break;
			}
			break;
		case 34189:
			switch (c.log) {
			case 1511: //Normal log
				fletchBow(c, 839, 1);
				break;
			case 1521: //Oak log
				fletchBow(c, 845, 1);
				break;
			case 1519: //Willow log
				fletchBow(c, 847, 1);
				break;
			case 1517: //Maple log
				fletchBow(c, 851, 1);
				break;
			case 1515: //Yew log
				fletchBow(c, 855, 1);
				break;
			case 1513: //Magic logs
				fletchBow(c, 859, 1);
				break;
			}
			break;
		case 34188:
			switch (c.log) {
			case 1511: //Normal log
				fletchBow(c, 839, 5);
				break;
			case 1521: //Oak log
				fletchBow(c, 845, 5);
				break;
			case 1519: //Willow log
				fletchBow(c, 847, 5);
				break;
			case 1517: //Maple log
				fletchBow(c, 851, 5);
				break;
			case 1515: //Yew log
				fletchBow(c, 855, 5);
				break;
			case 1513: //Magic logs
				fletchBow(c, 859, 5);
				break;
			}
			break;
		case 34187:
			switch (c.log) {
			case 1511: //Normal log
				fletchBow(c, 839, 10);
				break;
			case 1521: //Oak log
				fletchBow(c, 845, 10);
				break;
			case 1519: //Willow log
				fletchBow(c, 847, 10);
				break;
			case 1517: //Maple log
				fletchBow(c, 851, 10);
				break;
			case 1515: //Yew log
				fletchBow(c, 855, 10);
				break;
			case 1513: //Magic logs
				fletchBow(c, 859, 10);
				break;
			}
			break;
		case 34186:
			switch (c.log) {
			case 1511: //Normal log
				fletchBow(c, 839, 28);
				break;
			case 1521: //Oak log
				fletchBow(c, 845, 28);
				break;
			case 1519: //Willow log
				fletchBow(c, 847, 28);
				break;
			case 1517: //Maple log
				fletchBow(c, 851, 28);
				break;
			case 1515: //Yew log
				fletchBow(c, 855, 28);
				break;
			case 1513: //Magic logs
				fletchBow(c, 859, 28);
				break;
			}
			break;
		case 34193: //Arrow shafts
			fletchBow(c, 52, 1);
			break;
		case 34192: //Arrow shafts
			fletchBow(c, 52, 5);
			break;
		case 34191: //Arrow shafts
			fletchBow(c, 52, 10);
			break;
		case 34190: //Arrow shafts
			fletchBow(c, 52, 28);
			break;
		}
	}

	/** Cutting animation, shared by every fletching action. */
	static final int FLETCH_ANIMATION = 1248;
	/** One log makes this many arrow shafts. */
	static final int ARROW_SHAFTS_PER_LOG = 15;
	/**
	 * Event id for the ticked fletching action.
	 *
	 * <p>Non-zero on purpose: the plain {@code addEvent} overload leaves the id at 0, so a
	 * dedicated id is what lets {@link #cancel} stop this action and nothing else the player
	 * happens to be running.
	 */
	private static final int FLETCH_EVENT = 4614;

	/**
	 * Fletches bows or arrow shafts, one log per game tick.
	 *
	 * <p><b>The log comes from the product</b> ({@code forBow(product)}) and not from
	 * {@code c.log}. {@code c.log} is only what the make-X interface was opened with, and the
	 * two can disagree on a reachable path: the arrow-shaft buttons stay live in the interface
	 * whatever log opened it, so with an oak log held, pressing one used to fall into the shaft
	 * table's own log id and consume normal logs instead. Deriving from the product removes the
	 * question.
	 *
	 * <p>Set {@code Config.FLETCHING_ONE_BY_ONE_ENABLED} to {@code false} for the previous
	 * behaviour, which did the whole amount in one call — see {@link #fletchBowInstant}.
	 */
	public static void fletchBow(Client c, int id, int amount) {
		if (!Config.FLETCHING_ONE_BY_ONE_ENABLED) {
			fletchBowInstant(c, id, amount);
			return;
		}
		Fletch fle = forBow(id);
		if (fle == null || c.playerFletch) {
			return;
		}
		final int log = fle.getLogID();
		final boolean shafts = id == 52; // the shaft product id; forBow matched it above

		if (!c.getItems().playerHasItem(946)) {
			c.sendMessage("You need a knife to fletch this log.");
			c.getPA().removeAllWindows();
			resetFletching(c);
			return;
		}
		if (c.skills.playerLevel[Player.playerFletching] < fle.getLevelReq()) {
			c.sendMessage("You need a fletching level of at least " + fle.getLevelReq() + " to cut this log.");
			c.getPA().removeAllWindows();
			resetFletching(c);
			return;
		}
		int held = c.getItems().getItemAmount(log);
		if (held < 1) {
			c.sendMessage("You have no " + ItemAssistant.getItemName(log).toLowerCase() + " to fletch.");
			c.getPA().removeAllWindows();
			resetFletching(c);
			return;
		}
		if (amount > held) {
			amount = held;
		}

		// Close the make-X interface before the action starts: removeAllWindows does not call
		// resetVariables (closeAllWindows does), so the interface state is cleared explicitly.
		c.getPA().removeAllWindows();
		resetFletching(c);

		c.playerFletch = true;
		c.doAmount = amount;
		c.startAnimation(FLETCH_ANIMATION);

		CycleEventHandler.addEvent(FLETCH_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerFletch || c.doAmount <= 0 || !c.getItems().playerHasItem(log)) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(log, 1);
				c.getItems().addItem(fle.getBowID(), shafts ? ARROW_SHAFTS_PER_LOG : 1);
				c.getPA().addSkillXP(fle.getXp() * Config.FLETCHING_EXPERIENCE, Player.playerFletching);
				c.startAnimation(FLETCH_ANIMATION);
				c.doAmount--;
				if (c.doAmount <= 0) {
					container.stop();
				}
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, 2);
	}

	/**
	 * The pre-Phase-1 behaviour: deletes the whole amount and adds the whole product in one
	 * call, with arrow shafts multiplied by fifteen.
	 *
	 * <p>Kept verbatim behind {@code Config.FLETCHING_ONE_BY_ONE_ENABLED} so the change can be
	 * turned off without a revert. It is the instant path the flag's javadoc refers to, and it
	 * is not used while the flag is on.
	 */
	private static void fletchBowInstant(Client c, int id, int amount) {
		Fletch fle = forBow(id);
		if (fle != null) {
			int amount2 = c.getItems().getItemAmount(fle.getLogID());
			if(c.getItems().playerHasItem(fle.getLogID(), amount)) {
				amount2 = amount;
			}
			if (id == 52) {
				int[] logArray = {1511, 1521, 1519, 1517, 1515, 1513};
				for (int i = 0; i < logArray.length; i++)
					if (c.getItems().playerHasItem(logArray[i])) {
						c.getItems().deleteItem2(logArray[i], amount2);
						c.getItems().addItem(fle.getBowID(), 15*amount2);

						c.getPA().addSkillXP(fle.getXp()*amount2*Config.FLETCHING_EXPERIENCE, Player.playerFletching);

						c.getPA().closeAllWindows();
						return;
					}
			} else {
				if (c.getItems().playerHasItem(fle.getLogID())) {
					if (c.skills.playerLevel[Player.playerFletching] >= fle.getLevelReq()) {
						c.getItems().deleteItem2(fle.getLogID(), amount2);
						c.getItems().addItem(fle.getBowID(), amount2);

						c.getPA().addSkillXP(fle.getXp()*amount2*Config.FLETCHING_EXPERIENCE, Player.playerFletching);

						c.startAnimation(1248);
						c.getPA().closeAllWindows();
					} else {
						c.sendMessage("You need a fletching level of at least" +fle.getLevelReq()+" to cut this log.");
						c.getPA().closeAllWindows();
					}
				}
			}
			resetFletching(c);
			c.getPA().removeAllWindows();
		}
	}
	
	public static int[][] ifItems = {
			{1511, 839, 841},
			{1521, 845, 843},
			{1519, 847, 849},
			{1517, 851, 853},
			{1515, 855, 857},
			{1513, 859, 861}
	};

	static public void openFletching(Client c, int item) {
		for (int i = 0; i < ifItems.length; i++) {
			if (ifItems[i][0] == item) {
				c.getPA().sendFrame164(8880);
				c.getPA().sendFrame126("What would you like to make?", 8879);
				c.getPA().sendFrame246(8884, 250, ifItems[i][1]); // middle
				c.getPA().sendFrame246(8883, 250, ifItems[i][2]); // left picture
				//c.getPA().sendFrame246(8885, 250, 52); // right pic
				c.getPA().sendFrame126("Shortbow", 8889);
				c.getPA().sendFrame126("Longbow", 8893);
				if(ifItems[i][0] == 1511) {
					c.getPA().sendFrame246(8885, 250, 52); // right pic
					c.getPA().sendFrame126("Arrow Shafts", 8897);
				} else {
					c.getPA().sendFrame246(8885, 250, -1); // right pic
					c.getPA().sendFrame126(" ", 8897);
				}
			}
		}
		c.log = item;
		c.playerIsFletching = true;
	}
	
	/** Arrows are made fifteen at a time, bolts ten — the OSRS batch sizes. */
	private static final int ARROWS_PER_ACTION = 15;
	private static final int BOLTS_PER_ACTION = 10;
	/** One gem yields this many bolt tips. */
	private static final int BOLT_TIPS_PER_GEM = 10;
	/** The chisel used to cut gems into bolt tips. */
	private static final int CHISEL = 1755;

	/**
	 * Data for making bolt tips
	 */

	public static final int[][] boltTips = {
	/** Uncut Gem ID || Bolt Tips ID || Animation ID */
	{ 1611, 9187, 891 }, // jade bolt tips
			{ 1613, 9188, 892 }, // topaz bolt tips
			{ 1607, 9189, 888 }, // sapphire bolt tips
			{ 1605, 9190, 889 }, // emerald bolt tips
			{ 1603, 9191, 887 }, // ruby bolt tips
			{ 1601, 9192, 886 }, // diamond bolt tips
			{ 1615, 9193, 885 }, // dragon bolt tips
			{ 6573, 9194, 886 }, // onyx bolt tips
	};

	/**
	 * Data for making bolts
	 */

	public static int[][] craftingVariables = {
	/** Unf Bolt ID || Bolt Tips ID || Tipped Bolts || Level Req || Exp Gained */
	{ 9141, 9188, 9336, 48, 39 }, // topaz tipped bolts
			{ 9142, 9187, 9335, 26, 24 }, // jade tipped bolts
			{ 9142, 9189, 9337, 56, 47 }, // sapphire tipped bolts
			{ 9142, 9190, 9338, 58, 55 }, // emerald tipped bolts
			{ 9143, 9191, 9339, 63, 63 }, // ruby tipped bolts
			{ 9143, 9192, 9340, 65, 70 }, // diamond tipped bolts
			{ 9144, 9193, 9341, 71, 82 }, // dragonstone tipped bolts
			{ 9144, 9194, 9342, 73, 94 }, // onyx tipped bolts
	};

	/**
	 * Method to cut gems into bolt tips
	 * 
	 * @param c
	 * @param itemUsed
	 * @param useWith
	 */

	/**
	 * Cuts a gem into bolt tips with a chisel.
	 *
	 * <p>Was a wall-clock throttle: a {@code System.currentTimeMillis()} gate in front of an
	 * instant action, which lets a fast clicker through on lag and lets a slow one do nothing.
	 * It is a ticked action now, with the same guard the other fletching actions use.
	 *
	 * <p>The per-gem animation is preserved — it is the one piece of data here that has always
	 * been per-recipe, and the generic batch helper has no place for it.
	 */
	public static void handleBoltTipCrafting(Client c, int itemUsed, int useWith) {
		for (int i = 0; i < boltTips.length; i++) {
			if ((itemUsed == boltTips[i][0] || itemUsed == CHISEL)
					&& (useWith == boltTips[i][0] || useWith == CHISEL)) {
				craftBoltTips(c, boltTips[i][0], boltTips[i][1], boltTips[i][2]);
				return;
			}
		}
	}

	private static void craftBoltTips(final Client c, final int gem, final int product, final int animation) {
		if (c.playerFletch) {
			return;
		}
		if (!c.getItems().playerHasItem(CHISEL, 1)) {
			c.sendMessage("You need a chisel to cut the gem into bolt tips.");
			return;
		}
		if (!c.getItems().playerHasItem(gem, 1)) {
			c.sendMessage("You need at least 1 "
					+ Misc.formatPlayerName(ItemAssistant.getItemName(gem)) + " gem.");
			return;
		}

		c.playerFletch = true;
		c.startAnimation(animation);

		CycleEventHandler.addEvent(FLETCH_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerFletch || !c.getItems().playerHasItem(gem, 1)) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(gem, 1);
				c.getItems().addItem(product, BOLT_TIPS_PER_GEM);
				c.startAnimation(animation);
				c.sendMessage("You carefully craft the " + ItemAssistant.getItemName(gem)
						+ " into bolt tips.");
				container.stop();
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, 2);
	}

	/**
	 * Attaches gem bolt tips to unfinished bolts, ten at a time.
	 *
	 * <p>Same recipe numbers as before, on the tick instead of behind a wall-clock throttle. The
	 * level check is unchanged, and the recipe is chosen by the pair of items used rather than by
	 * argument order, so either order works.
	 */
	public static void handleBoltTipping(Client c, int itemUsed, int useWith) {
		for (int i = 0; i < craftingVariables.length; i++) {
			if ((itemUsed == craftingVariables[i][0] || itemUsed == craftingVariables[i][1])
					&& (useWith == craftingVariables[i][0] || useWith == craftingVariables[i][1])) {
				fletchBatch(c, craftingVariables[i][2], BOLTS_PER_ACTION, craftingVariables[i][4],
						craftingVariables[i][3], craftingVariables[i][0], BOLTS_PER_ACTION,
						craftingVariables[i][1], BOLTS_PER_ACTION, "gem tipped bolts",
						"You carefully craft some gem tipped bolts.");
				return;
			}
		}
	}
}
