package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.Item;
import server.game.minigames.randomevents.RiverTroll;
import server.game.players.Client;
import server.game.players.Player;
import core.util.Misc;

public class Fishing {

	private enum Spot {
		LURE(309, new int[]{335, 331}, 309, 314, new int[]{20, 30}, false, new int[]{50, 70}, 623),
		CAGE(312, new int[]{377}, 301, -1, new int[]{40}, false, new int[]{90}, 619),
		BIGNET(313, new int[]{353, 341, 363}, 305, -1, new int[]{16, 23, 46}, false, new int[]{20, 45, 100}, 620),
		SMALLNET(316, new int[]{317, 321}, 303, -1, new int[]{1, 15}, false, new int[]{10, 40}, 621),
		MONKNET2(14939, new int[]{7944}, 303, -1, new int[]{68}, false, new int[]{120}, 621),
		MONKNET(326, new int[]{7944}, 303, -1, new int[]{68}, false, new int[]{120}, 621),
		LURE2(309, new int[]{349}, 307, 313, new int[]{25}, true, new int[]{60}, 623),
		HARPOON(312, new int[]{359, 371}, 311, -1, new int[]{35, 50}, true, new int[]{80, 100}, 618),
		HARPOON2(313, new int[]{383}, 311, -1, new int[]{76}, true, new int[]{110}, 618),
		BAIT(316, new int[]{327, 345}, 307, 313, new int[]{5, 10}, true, new int[]{20, 30}, 623),
		BAIT2(326, new int[]{327, 345}, 307, 313, new int[]{5, 10}, true, new int[]{20, 30}, 623);

		int npcId, equipment, bait, anim;
		int[] rawFish, fishingReqs, xp;
		boolean second;
		private Spot(int npcId, int[] rawFish, int equipment, int bait, int[] fishingReqs, boolean second, int[] xp, int anim) {
			this.npcId = npcId;
			this.rawFish = rawFish;
			this.equipment = equipment;
			this.bait = bait;
			this.fishingReqs = fishingReqs;
			this.second = second;
			this.xp = xp;
			this.anim = anim;
		}

		public int getNPCId() {
			return npcId;
		}

		public int[] getRawFish() {
			return rawFish;
		}

		public int getEquipment() {
			return equipment;
		}

		public int getBait() {
			return bait;
		}

		public int[] getLevelReq() {
			return fishingReqs;
		}

		public boolean getSecond() {
			return second;
		}

		public int[] getXp() {
			return xp;
		}

		public int getAnim() {
			return anim;
		}
	}

	public static Spot forSpot(int npcId, boolean secondClick) {
		for (Spot s : Spot.values()) {
			if (secondClick) {
				if (s.getSecond()) {
					if (s.getNPCId() == npcId) {
						if (s != null)
							return s;
					}
				}
			} else {
				if (s.getNPCId() == npcId) {
					if (s != null)
						return s;
				}
			}
		}
		return null;
	}

	public static void setupFishing(Client c, Spot s) {
		if (c.playerLevel[Player.playerFishing] >= s.getLevelReq()[0]) {
			if (c.getItems().playerHasItem(s.getEquipment())) {
				if (s.getBait() != -1) {
					if (c.getItems().playerHasItem(s.getBait(), 1)) {
						startFishing(c, s);
					} else {
						c.sendMessage("You don't have enough bait to fish here.");
						c.startAnimation(65535);
					}
				} else {
					startFishing(c, s);
				}
			} else {
				c.sendMessage("You need a "+Item.getItemName(s.getEquipment()).toLowerCase()+" to fish here.");
			}
		} else {
			c.sendMessage("You need a fishing level of at least "+s.getLevelReq()[0]+" to fish here.");
		}
	}

	public static void startFishing(final Client c, final Spot s) {
		final int wat = Misc.random(100) >= 70 ? getMax(c, s.fishingReqs) : (getMax(c, s.fishingReqs) != 0 ? getMax(c, s.fishingReqs) - 1 : 0);
		c.startAnimation(s.getAnim());
		CycleEventHandler.addEvent(1, c, new CycleEvent() {
			int cycle = 0;
			@Override
			public void execute(CycleEventContainer container) {
				cycle++;
				c.startAnimation(s.getAnim());
				if(c.getItems().freeSlots() < 1) {
					c.sendMessage("Your inventory is full!");
					container.stop();
				}
				if (cycle == 2) {
					if (s.getBait() != -1) {
						c.getItems().deleteItem(s.getBait(), c.getItems().getItemSlot(s.getBait()), 1);
					}
					if(Misc.random(250) == 0) {
						RiverTroll.spawnRiverTroll(c);
						container.stop();
					}
					c.getItems().addItem(s.getRawFish()[wat], 1);
					c.sendMessage("You catch a "+Item.getItemName(s.getRawFish()[wat]).toLowerCase().replace("_", " ")+".");
					c.getPA().addSkillXP(s.getXp()[wat]*Config.FISHING_EXPERIENCE, Player.playerFishing);
					setupFishing(c, s);
					this.stop();
				}
			}
			@Override
			public void stop() {
				// Whatever you want it to do when the event stops
			}
		}, 2);
	}
	
	public static void resetFishing(Client c) {
		c.startAnimation(65535);
		CycleEventHandler.stopEvents(c, 1);
	}

	public static int getMax(Client c, int[] reqs) {
		int tempInt = -1;
		for (int i : reqs) {
			if (c.playerLevel[Player.playerFishing] >= i) {
				tempInt++;
			}
		}
		return tempInt;
	}
}