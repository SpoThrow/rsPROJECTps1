package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.minigames.randomevents.SpiritTree;
import server.game.objects.Object;
import server.game.players.Client;
import server.game.players.PlayerHandler;
import core.util.Misc;

public class Woodcutting {
	
	public final static int[][] Axe_Settings = {
		{1351, 1, 1, 879}, //Bronze
		{1349, 1, 2, 877}, //Iron
		{1353, 6, 3, 875}, //Steel
		{1361, 6, 4, 873}, //Black
		{1355, 21, 5, 871}, //Mithril
		{1357, 31, 6, 869}, //Addy
		{1359, 41, 7, 867}, //Rune
		{6739, 61, 8, 2846}, //Dragon
		{13661, 41, 8, 10251} //Adze
	};

	public final static int[][] Tree_Settings = {
		{1276, 1342, 1, 25, 1511, 45, 100}, //Tree
		{1278, 1342, 1, 25, 1511, 45, 100}, //Tree
		{1286, 1342, 1, 25, 1511, 45, 100}, //Dead Tree
		{1281, 1356, 15, 38, 1521, 11, 20}, //Oak
		{1308, 7399, 30, 68, 1519, 11, 8}, //Willow
		{5552, 7399, 30, 68, 1519, 11, 8}, //Willow
		{1307, 1343, 45, 100, 1517, 48, 8}, //Maple
		{1309, 7402, 60, 175, 1515, 79, 5}, //Yew
		{1306, 7401, 75, 250, 1513, 150, 3}, //Magic
		{5551, 7399, 30, 68, 1519, 11, 8}, //Willow
		{5553, 7399, 30, 68, 1519, 11, 8} //Willow
	};

	static int a = -1;
	
	public static void startWoodcutting(final Client c, final int j, final int x, final int y, final int type) {
		if (c.isWc)
			return;
		if (c.wcing)
			return;
		int wcLevel = c.playerLevel[8];
		a = -1;
		c.turnPlayerTo(x, y);
		if (Tree_Settings[j][2] > wcLevel) {
			c.sendMessage("You need a Woodcutting level of " + Tree_Settings[j][2] + " to cut this tree.");
			return;
		}
		for (int i = 0; i < Axe_Settings.length; i++) {
			if (c.getItems().playerHasItem(Axe_Settings[i][0]) || c.playerEquipment[c.playerWeapon] == Axe_Settings[i][0]) {
				if (Axe_Settings[i][1] <= wcLevel) {
					a = i;
				}
			}
		}
		if (a == -1) {
			c.sendMessage("You need an axe to cut this tree.");
			return;
		}
		if (c.getItems().freeSlots() < 1) {
			c.sendMessage("You do not have enough inventory slots to do that.");
			return;
		}
		c.startAnimation(Axe_Settings[a][3]);
		c.isWc = true;
		c.treeX = x;
		c.treeY = y;
		c.wcing = true;
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.isWc) {
					container.stop();
					return;
				}
				if (c.isWc)
					c.startAnimation(Axe_Settings[a][3]);
				if (c.getItems().freeSlots() < 1) {
					c.sendMessage("You have ran out of inventory slots.");
					container.stop();
				}
				int xp = Tree_Settings[j][3] * Config.WOODCUTTING_EXPERIENCE;
				if (c.getItems().playerHasEquipped(10933)) {
					xp = (int)(xp * 1.002);
				}
				if (c.getItems().playerHasEquipped(10939)) {
					xp = (int)(xp * 1.008);
				}
				if (c.getItems().playerHasEquipped(10940)) {
					xp = (int)(xp * 1.006);
				}
				if (c.getItems().playerHasEquipped(10941)) {
					xp = (int)(xp * 1.004);
				}
				if (c.isWc) {
					c.getItems().addItem(Tree_Settings[j][4], 1);
					c.getPA().addSkillXP(xp, 8);
				}
				if(Misc.random(250) == 0) {
					SpiritTree.spawnSpiritTree(c);
					container.stop();
				}
				if (c.getItems().freeSlots() < 1) {
					c.sendMessage("You have ran out of inventory slots.");
					container.stop();
				}
				if (c.isWc)
					birdNests(c);
				if (c.getItems().freeSlots() < 1) {
					c.sendMessage("You have ran out of inventory slots.");
					container.stop();
				}
				if (Misc.random(100) <= Tree_Settings[j][6]) {
					cutDownTree(Tree_Settings[j][5], x, y, type, Tree_Settings[j][1], Tree_Settings[j][0]);
					container.stop();
				}
			}
			@Override
			public void stop() {
				c.startAnimation(65535);
				c.isWc = false;
				c.treeX = 0;
				c.treeY = 0;
				c.wcing = false;
				return;
			}
		}, getTimer(j, a, wcLevel));
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.isWc) {
					container.stop();
					return;
				}
				if (c.isWc) {
					c.startAnimation(Axe_Settings[a][3]);
				}
			}
			@Override
			public void stop() {

			}
		}, 4);
	}

	public static int getTimer(int b, int c, int level) {
		double timer = (int)((Tree_Settings[b][2]  * 2) + 20 + Misc.random(20))-((Axe_Settings[c][2] * (Axe_Settings[c][2] * 0.75)) + level);
		if (timer < 3.0) {
			return 3;
		} else {
			return (int)timer;
		}
	}
	
	public static void birdNests(Client c) {
		if (Misc.random(100) < 5) {
			c.getItems().addItem(5070, 1);
		}
	}
	
	public static void cutDownTree(int respawnTime, int x, int y, int type, int i, int j) {
		//new Object(i, x, y, 0, type, 10, j, respawnTime);
		new Object(j, x, y, 0, 0, 10, i, respawnTime);
		for (int t = 0; t < PlayerHandler.players.length; t++) {
			if (PlayerHandler.players[t] != null) {
				if (PlayerHandler.players[t].treeX == x && PlayerHandler.players[t].treeY == y) {
					PlayerHandler.players[t].isWc = false;
					PlayerHandler.players[t].startAnimation(65535);
					PlayerHandler.players[t].treeX = 0;
					PlayerHandler.players[t].treeY = 0;
				}
			}
		}
	}
}