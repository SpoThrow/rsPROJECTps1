package server.content.skills;

import server.Config;
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
	
	public static Bolts forBolts(int id) {
		for (Bolts bolts : Bolts.values()) {
			if (bolts.getItem2() == id) {
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

	public static void makeArrows(Client c, int item1, int item2) {
		Arrows arr = forArrow(getPrimary(item1, item2));
		if (arr != null) {
			if (c.playerLevel[Player.playerFletching] >= arr.getLevelReq()) {
				System.out.println(arr.getItem1()+", "+ c.getItems().getItemCount(arr.getItem1()));
				System.out.println(arr.getItem2()+", "+ c.getItems().getItemCount(arr.getItem2()));
				if (c.getItems().getItemCount(arr.getItem1()) >= 15 && c.getItems().getItemCount(arr.getItem2()) >= 15) {
					c.getItems().deleteItem2(arr.getItem1(), 15); 
					c.getItems().deleteItem2(arr.getItem2(), 15);
					c.getItems().addItem(arr.getOutcome(), 15);
					
					c.getPA().addSkillXP(arr.getXp()*Config.FLETCHING_EXPERIENCE, Player.playerFletching);
					
				} else {
					//System.out.println("1");
					c.sendMessage("You must have at least 15 of each supply to make arrows!");
				}
			} else {
				//System.out.println("2");
				c.sendMessage("You need a fletching level of "+arr.getLevelReq()+" to fletch this.");
			}
		}
	}
	
	public static void makeBolts(Client c, int item1, int item2) {
		Bolts bolts = forBolts(item1);
		if (bolts != null) {
			if (c.playerLevel[Player.playerFletching] >= bolts.getLevelReq()) {
				System.out.println(bolts.getItem1()+", "+ c.getItems().getItemCount(bolts.getItem1()));
				System.out.println(bolts.getItem2()+", "+ c.getItems().getItemCount(bolts.getItem2()));
				if (c.getItems().getItemCount(bolts.getItem1()) >= 10 && c.getItems().getItemCount(bolts.getItem2()) >= 10) {
					c.getItems().deleteItem2(bolts.getItem1(), 10); 
					c.getItems().deleteItem2(bolts.getItem2(), 10);
					c.getItems().addItem(bolts.getOutcome(), 10);
					
					c.getPA().addSkillXP(bolts.getXp()*Config.FLETCHING_EXPERIENCE, Player.playerFletching);
					
				} else {
					//System.out.println("1");
					c.sendMessage("You must have at least 10 of each supply to make bolts!");
				}
			} else {
				//System.out.println("2");
				c.sendMessage("You need a fletching level of "+bolts.getLevelReq()+" to fletch this.");
			}
		}
	}
	
	private enum Fletch {

		ARROWSHAFTS(1511, 52, 5, 15),

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

	private static Fletch forBow(int id) {
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

	public static void fletchBow(Client c, int id, int amount) {
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
					if (c.playerLevel[Player.playerFletching] >= fle.getLevelReq()) {
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
	
	private static final int AMOUNT = 10;
	private static final int DELETE = 1;

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

	public static void handleBoltTipCrafting(Client c, int itemUsed, int useWith) {
		for (int i = 0; i < boltTips.length; i++) {
			if ((itemUsed == boltTips[i][0] || itemUsed == 1755)
					&& (useWith == boltTips[i][0] || useWith == 1755)) {
				if (System.currentTimeMillis() - c.alchDelay > 1400) {
					if (c.getItems().playerHasItem(boltTips[i][0], DELETE)) {
						c.getItems().deleteItem(boltTips[i][0], DELETE);
						c.getItems().addItem(boltTips[i][1], AMOUNT);
						c.startAnimation(boltTips[i][2]);
						c.sendMessage("You carefully craft the "
								+ ItemAssistant.getItemName(boltTips[i][0])
								+ " into bolt tips.");
						c.alchDelay = System.currentTimeMillis();
					} else {
						c.sendMessage("You need at least "
								+ DELETE
								+ " "
								+ Misc.formatPlayerName(ItemAssistant
										.getItemName(boltTips[i][0])) + " gem.");
					}
				}
			}
		}
	}

	/**
	 * Method to craft bolts with bolt tips
	 * 
	 * @param c
	 * @param itemUsed
	 * @param useWith
	 */

	public static void handleBoltTipping(Client c, int itemUsed, int useWith) {
		for (int i = 0; i < craftingVariables.length; i++) {
			if ((itemUsed == craftingVariables[i][0] || itemUsed == craftingVariables[i][1])
					&& (useWith == craftingVariables[i][0] || useWith == craftingVariables[i][1])) {
				if (c.playerLevel[Player.playerFletching] >= craftingVariables[i][3]) {
					if (System.currentTimeMillis() - c.alchDelay > 1200) {
						if (c.getItems().playerHasItem(craftingVariables[i][0],
								AMOUNT)
								&& c.getItems().playerHasItem(
										craftingVariables[i][1], AMOUNT)) {
							c.getItems().deleteItem(
									craftingVariables[i][0],
									c.getItems().getItemSlot(
											craftingVariables[i][0]), AMOUNT);
							c.getItems().deleteItem(
									craftingVariables[i][1],
									c.getItems().getItemSlot(
											craftingVariables[i][1]), AMOUNT);
							c.getItems().addItem(craftingVariables[i][2],
									AMOUNT);
							
							c.getPA().addSkillXP(
									craftingVariables[i][4]
											* Config.FLETCHING_EXPERIENCE,
									Player.playerFletching);
							
							c.sendMessage("You carefully craft some gem tipped bolts.");
							c.alchDelay = System.currentTimeMillis();
						} else {
							c.sendMessage("You need at least "
									+ AMOUNT
									+ " "
									+ Misc.formatPlayerName(ItemAssistant
											.getItemName(craftingVariables[i][0]))
									+ " & "
									+ Misc.formatPlayerName(ItemAssistant
											.getItemName(craftingVariables[i][1]))
									+ ".");
						}
					}
				} else {
					c.sendMessage("You need at least "
							+ craftingVariables[i][3]
							+ " fletching to make this.");
				}
			}
		}
	}
}