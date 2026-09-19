package server.game.players;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import server.Config;
import server.Connection;
import server.Server;
import server.clip.region.Region;
import server.content.skills.Cooking;
import server.content.skills.CraftingData;
import server.content.skills.Fishing;
import server.content.skills.Fletching;
import server.content.skills.Herblore;
import server.content.skills.Magic;
import server.content.skills.Mining;
import server.content.skills.Smelting;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.GameItem;
import server.game.items.ItemAssistant;
import server.game.minigames.bountyhunter.BountyHunter;
import server.game.minigames.castlewars.CastleWars;
import server.game.npcs.NPCHandler;
import server.game.objects.Objects;
import server.world.Clan;
import core.util.Misc;

public class PlayerAssistant {

	private Client c;

	public PlayerAssistant(Client Client) {
		this.c = Client;
	}
	
	public boolean checkSkilling() {
		for(int i = 0; i <= 5; i++)
			if(c.checkSkilling[i])
				return true;
		return false;
	}

	/**
	 * MulitCombat icon
	 * 
	 * @param i1
	 *            0 = off 1 = on
	 */
	public void multiWay(int i1) {
		// synchronized(c) {
		c.outStream.createFrame(61);
		c.outStream.writeByte(i1);
		c.updateRequired = true;
		c.setAppearanceUpdateRequired(true);

	}
	
	public int getBhSkull() {
		int wealth = c.getItems().getTotalNetPlayer(c);
		if(wealth >= 2500000)
			return 10;
		else if(wealth >= 1100000)
			return 9;
		else if(wealth >= 500000)
			return 8;
		else if(wealth >= 100000)
			return 7;
		else
			return 6;
	}
	
	public Clan getClan() {
		if (Server.clanManager.clanExists(c.playerName)) {
			return Server.clanManager.getClan(c.playerName);
		}
		return null;
	}
	
	public void sendClan(String name, String message, String clan, int rights) {
		if(rights >= 3)
			rights--;
		c.outStream.createFrameVarSizeWord(217);
		c.outStream.writeString(name);
		c.outStream.writeString(Misc.formatPlayerName(message));
		c.outStream.writeString(clan);
		c.outStream.writeWord(rights);
		c.outStream.endFrameVarSize();
	}
	
	public void clearClanChat() {
		c.clanId = -1;
		c.getPA().sendFrame126("Talking in: ", 50139);
		c.getPA().sendFrame126("Owner: ", 50140);
		for (int j = 50144; j < 50244; j++) {
			c.getPA().sendFrame126("", j);
		}
	}
	
	public void setClanData() {
		boolean exists = Server.clanManager.clanExists(c.playerName);
		if (!exists || c.clan == null) {
			sendFrame126("Join chat", 50135);
			sendFrame126("Talking in: Not in chat", 50139);
			sendFrame126("Owner: None", 50140);
		}
		if (!exists) {
			sendFrame126("Chat Disabled", 50306);
			String title = "";
			for (int id = 50307; id < 50317; id += 3) {
				if (id == 50307) {
					title = "Anyone";
				} else if (id == 50310) {
					title = "Anyone";
				} else if (id == 50313) {
					title = "General+";
				} else if (id == 50316) {
					title = "Only Me";
				}
				sendFrame126(title, id + 2);
			}
			for (int index = 0; index < 100; index++) {
				sendFrame126("", 50323 + index);
			}
			for (int index = 0; index < 100; index++) {
				sendFrame126("", 50424 + index);
			}
			return;
		}
		Clan clan = Server.clanManager.getClan(c.playerName);
		sendFrame126(clan.getTitle(), 50306);
		String title = "";
		for (int id = 50307; id < 50317; id += 3) {
			if (id == 50307) {
				title = clan.getRankTitle(clan.whoCanJoin)
						+ (clan.whoCanJoin > Clan.Rank.ANYONE
								&& clan.whoCanJoin < Clan.Rank.OWNER ? "+" : "");
			} else if (id == 50310) {
				title = clan.getRankTitle(clan.whoCanTalk)
						+ (clan.whoCanTalk > Clan.Rank.ANYONE
								&& clan.whoCanTalk < Clan.Rank.OWNER ? "+" : "");
			} else if (id == 50313) {
				title = clan.getRankTitle(clan.whoCanKick)
						+ (clan.whoCanKick > Clan.Rank.ANYONE
								&& clan.whoCanKick < Clan.Rank.OWNER ? "+" : "");
			} else if (id == 50316) {
				title = clan.getRankTitle(clan.whoCanBan)
						+ (clan.whoCanBan > Clan.Rank.ANYONE
								&& clan.whoCanBan < Clan.Rank.OWNER ? "+" : "");
			}
			sendFrame126(title, id + 2);
		}
		if (clan.rankedMembers != null) {
			for (int index = 0; index < 100; index++) {
				if (index < clan.rankedMembers.size()) {
					sendFrame126("<clan=" + clan.ranks.get(index) + ">"
							+ clan.rankedMembers.get(index), 50323 + index);
				} else {
					sendFrame126("", 50323 + index);
				}
			}
		}
		if (clan.bannedMembers != null) {
			for (int index = 0; index < 100; index++) {
				if (index < clan.bannedMembers.size()) {
					sendFrame126(clan.bannedMembers.get(index), 50424 + index);
				} else {
					sendFrame126("", 50424 + index);
				}
			}
		}
	}

	public void showOption(int i, int l, String s) {
		if (c.getOutStream() != null) {
			if (!optionType.equalsIgnoreCase(s)) {
				optionType = s;
				c.getOutStream().createFrameVarSize(104);
				c.getOutStream().writeByteC(i);
				c.getOutStream().writeByteA(l);
				c.getOutStream().writeString(s);
				c.getOutStream().endFrameVarSize();
				c.flushOutStream();
			}
		}
	}
	
	public void sendFrame34a(int frame, int item, int slot, int amount) {
		c.outStream.createFrameVarSizeWord(34);
		c.outStream.writeWord(frame);
		c.outStream.writeByte(slot);
		c.outStream.writeWord(item + 1);
		c.outStream.writeByte(255);
		c.outStream.writeDWord(amount);
		c.outStream.endFrameVarSizeWord();
	}
	
	public void sendFrame177(int x, int y, int height, int speed, int angle) {
		c.getOutStream().createFrame(177);
		c.getOutStream().writeByte(x / 64); // X coord within your loaded map area
		c.getOutStream().writeByte(y / 64); // Y coord within your loaded map area
		c.getOutStream().writeWord(height); // HeightLevel
		c.getOutStream().writeByte(speed); //Camera Speed
		c.getOutStream().writeByte(angle); //Angle
	}
	public void sendFrame35(int i1, int i2, int i3, int i4)
	{
		c.getOutStream().createFrame(35);
		c.getOutStream().writeByte(i1);
		c.getOutStream().writeByte(i2);
		c.getOutStream().writeByte(i3);
		c.getOutStream().writeByte(i4);
		c.updateRequired = true;
		c.appearanceUpdateRequired = true;
	}
	
	public void moveThroughDoor(final Client c, final int obI, boolean n) {
		final int[] coords = new int[2];
		final int[] obFace = new int[2];
		if(n) {
			if(c.absX > c.objectX) {
				coords[0] = -1;
			} else {
				coords[0] = 1;
			}
			coords[1] = 0;
		} else {
			if(c.absY > c.objectY || c.absY == c.objectY) {
				coords[1] = -1;
			} else {
				coords[1] = 1;
			}
			coords[0] = 0;
		}
		obFace[0] = n ? -1 : -4;
		obFace[1] = n ? -2 : -1;
		object(obI, c.objectX, c.objectY, obFace[0],0);
		walkTo(coords[0], coords[1]);
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				object(obI, c.objectX, c.objectY, obFace[1],0);
				container.stop();
			}
			@Override
			public void stop() {

			}
		}, 1);
	}
	
	public ArrayList<GameItem> randomFish(int fish) {
		Random r = new Random();
		ArrayList<GameItem> toReturn = new ArrayList<GameItem>();
		boolean turtles = true;
		boolean mantas = true;
		boolean lobsters = true;
		boolean swordfish = true;
		int turt = 0;
		int manta = 0;
		int lobs = 0;
		int swordFish = 0;
		int junk = 0;
		int done = 0;
		while (done != fish) {
			done++;
			int random = r.nextInt(100);
			if (random >= 85 - Server.trawler.chanceByLevel(c, 381)) {
				if (mantas) {
					manta++;
				}
			} else if (random >= 70 - Server.trawler.chanceByLevel(c,
					381)) {
				if (turtles) {
					turt++;
				}
			} else if (random >= 40) {
				if (swordfish) {
					swordFish++;
				}
			} else if (random >= 5) {
				if (lobsters) {
					lobs++;
				}
			} else {
				junk++;
			}
		}
		int xpToAdd = 0;
		if (manta > 0) {
			toReturn.add(new GameItem(389, manta));
			if (c.playerLevel[Player.playerFishing] >= 81) {
				xpToAdd += (manta * 46 * Config.FISHING_EXPERIENCE);
			}
		}
		if (turt > 0) {
			toReturn.add(new GameItem(395, turt));
			if (c.playerLevel[Player.playerFishing] >= 79) {
				xpToAdd += (manta * 38 * Config.FISHING_EXPERIENCE);
			}
		}
		if (lobs > 0) {
			toReturn.add(new GameItem(377, lobs));
			if (c.playerLevel[Player.playerFishing] >= 40) {
				xpToAdd += (manta * 90 * Config.FISHING_EXPERIENCE);
			}
		}
		if (swordFish > 0) {
			toReturn.add(new GameItem(371, swordFish));
			if (c.playerLevel[Player.playerFishing] >= 50) {
				xpToAdd += (manta * 100 * Config.FISHING_EXPERIENCE);
			}
		}
		if (junk > 0)
			toReturn.add(new GameItem(685, junk));
		
		c.getPA().addSkillXP(xpToAdd, Player.playerFishing);
		
		return toReturn;
	}
	
	public void removeFishingTrawlerRewardItem(int slot, boolean all) {
		try {
			if (!all) {
				if (c.getItems().freeSlots() != 0) {
					if (c.fishingTrawlerReward.get(slot).amount >= 1) {
						c.getItems().addItem(
								c.fishingTrawlerReward.get(slot).id, 1);
						c.fishingTrawlerReward.get(slot).amount--;
						if (c.fishingTrawlerReward.get(slot).amount <= 0) {
							c.fishingTrawlerReward.remove(slot);
							Server.trawler.showReward(c);
						} else {
							Server.trawler.updateRewardSlot(c, slot);
						}
					}
				} else {
					c.sendMessage("You don't have enough inventory space to withdraw that");
				}
			} else {
				int loop = c.fishingTrawlerReward.get(slot).amount;
				for (int j = 0; j < loop; j++) {
					if (c.getItems().freeSlots() == 0) {
						c.sendMessage("You don't have enough inventory space to withdraw that");
						Server.trawler.updateRewardSlot(c, slot);
						return;
					}
					c.getItems()
							.addItem(c.fishingTrawlerReward.get(slot).id, 1);
					c.fishingTrawlerReward.get(slot).amount--;
					if (c.fishingTrawlerReward.get(slot).amount <= 0) {
						c.fishingTrawlerReward.remove(slot);
						Server.trawler.showReward(c);
						return;
					}
				}
			}
		} catch (Exception e) {

		}
	}
	
	public void removeAllSidebars() {
		c.setSidebarInterface(1, -1);
		c.setSidebarInterface(2, -1);
		c.setSidebarInterface(3, -1);
		c.setSidebarInterface(4, -1);
		c.setSidebarInterface(5, -1);
		c.setSidebarInterface(6, -1); // modern
		c.setSidebarInterface(7, -1);
		c.setSidebarInterface(8, -1);
		c.setSidebarInterface(9, -1);
		c.setSidebarInterface(10, -1);
		c.setSidebarInterface(11, -1); // wrench tab
		c.setSidebarInterface(12, -1); // run tab
		c.setSidebarInterface(13, -1);
		c.setSidebarInterface(0, -1);
	}
	
	public void sendMapState(int state) {
		//c.getOutStream().createFrame(99);
		//c.getOutStream().writeByte(state);
	}
	
	public void resetAnimationsToPrevious() {
		c.playerRunIndex = c.prevPrevPlayerRunIndex;
		c.playerStandIndex = c.prevPlayerStandIndex;
		c.playerWalkIndex = c.prevplayerWalkIndex;
		c.playerTurnIndex = c.prevPlayerTurnIndex;
		c.playerTurn90CWIndex = c.prevPlayerTurn90CWIndex;
		c.playerTurn90CCWIndex = c.prevPlayerTurn90CCWIndex;
		c.playerTurn180Index = c.prevPlayerTurn180Index;
		requestUpdates();
	}
	
	public void setSidebarInterfaces(Client c, boolean i) {
		if (i) {
			c.getPA().handleWeaponStyle();
			c.setSidebarInterface(1, 3917);
			c.setSidebarInterface(2, 638);
			c.setSidebarInterface(3, 3213);
			c.setSidebarInterface(4, 1644);
			c.setSidebarInterface(5, 5608);
			if(c.playerMagicBook == 0)
				c.setSidebarInterface(6, 1151); //modern
			else if (c.playerMagicBook == 1)
				c.setSidebarInterface(6, 12855); // ancient
			else if (c.playerMagicBook == 2)
				c.setSidebarInterface(6, 29999);
			c.setSidebarInterface(7, 50128);
			c.setSidebarInterface(8, 5065);
			c.setSidebarInterface(9, 5715);
			c.setSidebarInterface(10, 2449);
			// setSidebarInterface(11, 4445); // wrench tab
			c.setSidebarInterface(11, 904); // wrench tab
			c.setSidebarInterface(12, 147); // run tab
			c.setSidebarInterface(13, 962);
			c.setSidebarInterface(0, 2423);
		} else {
			c.setSidebarInterface(1, -1);
			c.setSidebarInterface(2, -1);
			c.setSidebarInterface(3, 6014);//
			c.setSidebarInterface(4, -1);
			c.setSidebarInterface(5, -1);
			if (c.playerMagicBook == 0) {
				c.setSidebarInterface(6, -1); // modern
			} else {
				c.setSidebarInterface(6, -1); // ancient
			}
			c.setSidebarInterface(7, 50128);
			c.setSidebarInterface(8, 5065);
			c.setSidebarInterface(9, 5715);
			c.setSidebarInterface(10, -1);
			// setSidebarInterface(11, 4445); // wrench tab
			c.setSidebarInterface(11, -1); // wrench tab
			c.setSidebarInterface(12, -1); // run tab
			c.setSidebarInterface(13, -1);
			c.setSidebarInterface(14, -1);
			c.setSidebarInterface(0, -1);
		}
	}
	
	public void starterSideBars(Client c) {
		c.setSidebarInterface(1, -1);
		c.setSidebarInterface(2, -1);
		c.setSidebarInterface(3, -1);//
		c.setSidebarInterface(4, -1);
		c.setSidebarInterface(5, -1);
		if (c.playerMagicBook == 0) {
			c.setSidebarInterface(6, -1); // modern
		} else {
			c.setSidebarInterface(6, -1); // ancient
		}
		c.setSidebarInterface(7, -1);
		c.setSidebarInterface(8, -1);
		c.setSidebarInterface(9, -1);
		c.setSidebarInterface(10, -1);
		// setSidebarInterface(11, 4445); // wrench tab
		c.setSidebarInterface(11, -1); // wrench tab
		c.setSidebarInterface(12, -1); // run tab
		c.setSidebarInterface(13, -1);
		c.setSidebarInterface(14, -1);
		c.setSidebarInterface(0, -1);
	}
	
	public void sendFrame20(int id, int state) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(36);
			c.getOutStream().writeWordBigEndian(id);
			c.getOutStream().writeByte(state);
			c.flushOutStream();
			
		}
}
	
	public void itemOnInterface(int interfaceChild, int zoom, int itemId) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(246);
			c.getOutStream().writeWordBigEndian(interfaceChild);
			c.getOutStream().writeWord(zoom);
			c.getOutStream().writeWord(itemId);
			c.flushOutStream();
		}
	}
	
public void sendFrame34P2(int item, int slot, int frame, int amount) {
		c.outStream.createFrameVarSizeWord(34);
		c.outStream.writeWord(frame);
		c.outStream.writeByte(slot);
		c.outStream.writeWord(item + 1);
		c.outStream.writeByte(255);
		c.outStream.writeDWord(amount);
		c.outStream.endFrameVarSizeWord();
	}
	public void setSidebarInterfaces(Client c) {
		int[] inter = { 2434, // Attack
				3917, // Skill
				638, // Quest
				3213, // Inventory
				1644, // Equip
				5608, // Prayer 5608, 22500
				-1, // Magic
				-1, // Clan chat
				5065, // Friends
				5715, // Ignores
				2449, // Logout
				904, // Settings
				147, // Animation
				29500, // Music
		};
		for (int i = 0; i < 14; i++) {
			c.setSidebarInterface(i, inter[i]);
		}
		if (c.playerMagicBook == 1) {
			c.setSidebarInterface(6, 12855);
		} else if (c.playerMagicBook == 2) {
			c.setSidebarInterface(6, 16640);
		} else {
			c.setSidebarInterface(6, 1151);
		}
		c.getItems().sendWeapon(c.playerEquipment[c.playerWeapon],
				ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]));
	}

	public void writeEnergy() {
		sendFrame126(c.playerEnergy + "%", 149);
	}

	public int totalLevel() {
		int total = 0;
		for (int i = 0; i <= 22; i++) {
			total += getLevelForXP(c.playerXP[i]);
		}
		return total;
	}

	public int xpTotal() {
		int xp = 0;
		for (int i = 0; i <= 20; i++) {
			xp += c.playerXP[i];
		}
		return xp;
	}

	public void resting() {
		c.isResting = true;
		c.startAnimation(4853);
	}

	public int raiseTimer() {
		if (!c.isResting)
			if (c.playerLevel[16] >= 2 && c.playerLevel[16] < 10)
				return 6500;
		if (c.playerLevel[16] >= 10 && c.playerLevel[16] < 25)
			return 6000;
		if (c.playerLevel[16] >= 25 && c.playerLevel[16] < 40)
			return 5500;
		if (c.playerLevel[16] >= 40 && c.playerLevel[16] < 55)
			return 5000;
		if (c.playerLevel[16] >= 55 && c.playerLevel[16] < 70)
			return 4500;
		if (c.playerLevel[16] >= 70 && c.playerLevel[16] < 85)
			return 4000;
		if (c.playerLevel[16] >= 85 && c.playerLevel[16] < 99)
			return 3500;
		if (c.playerLevel[16] == 99)
			return 3000;
		return 7000;
	}

	public int raiseTimer2() {
		if (c.isResting)
			if (c.playerLevel[16] >= 2 && c.playerLevel[16] < 10)
				return 2250;
		if (c.playerLevel[16] >= 10 && c.playerLevel[16] < 25)
			return 2000;
		if (c.playerLevel[16] >= 25 && c.playerLevel[16] < 40)
			return 1750;
		if (c.playerLevel[16] >= 40 && c.playerLevel[16] < 55)
			return 1500;
		if (c.playerLevel[16] >= 55 && c.playerLevel[16] < 70)
			return 1250;
		if (c.playerLevel[16] >= 70 && c.playerLevel[16] < 85)
			return 1000;
		if (c.playerLevel[16] >= 85 && c.playerLevel[16] < 99)
			return 750;
		if (c.playerLevel[16] == 99)
			return 500;
		return 2500;
	}

	public void playerWalk(int x, int y) {
		PathFinder.getPathFinder().findRoute(c, x, y, true, 1, 1);
	}

	int tmpNWCX[] = new int[50];
	int tmpNWCY[] = new int[50];

	/*
	 * public void resetAutocast() { c.autocastId = 0; c.autocasting = false;
	 * c.getPA().sendFrame36(108, 0); }
	 */

	public void resetAutocast() {
		c.autocastId = -1;
		c.autocasting = false;
		c.setSidebarInterface(0, 328);
		c.getPA().sendFrame36(108, 0);
		c.getItems().sendWeapon(c.playerEquipment[c.playerWeapon],
				ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]));
	}

	public int getItemSlot(int itemID) {
		for (int i = 0; i < c.playerItems.length; i++) {
			if ((c.playerItems[i] - 1) == itemID) {
				return i;
			}
		}
		return -1;
	}

	public boolean isItemInBag(int itemID) {
		for (int i = 0; i < c.playerItems.length; i++) {
			if ((c.playerItems[i] - 1) == itemID) {
				return true;
			}
		}
		return false;
	}

	public int freeSlots() {
		int freeS = 0;
		for (int i = 0; i < c.playerItems.length; i++) {
			if (c.playerItems[i] <= 0) {
				freeS++;
			}
		}
		return freeS;
	}

	public void turnTo(int pointX, int pointY) {
		c.focusPointX = 2 * pointX + 1;
		c.focusPointY = 2 * pointY + 1;
		c.updateRequired = true;
	}

	public void movePlayer(int x, int y, int h) {
		//castlewars
		if(c.heightLevel != h)
			c.updateRegion = true;
		c.resetWalkingQueue();
		c.teleportToX = x;
		c.teleportToY = y;
		c.heightLevel = h;
		requestUpdates();
	}

	public int getX() {
		return absX;
	}

	public int getY() {
		return absY;
	}

	public int absX, absY;
	public int heightLevel;

	public static void QuestReward(Client c, String questName, int PointsGain,
			String Line1, String Line2, String Line3, String Line4,
			String Line5, String Line6, int itemID) {
		c.getPA().sendFrame126("You have completed " + questName + "!", 12144);
		sendQuest(c, "" + (c.questPoints), 12147);
		// c.QuestPoints += PointsGain;
		sendQuest(c, Line1, 12150);
		sendQuest(c, Line2, 12151);
		sendQuest(c, Line3, 12152);
		sendQuest(c, Line4, 12153);
		sendQuest(c, Line5, 12154);
		sendQuest(c, Line6, 12155);
		c.getPlayerAssistant().sendFrame246(12145, 250, itemID);
		showInterface(c, 12140);
		Server.getStillGraphicsManager().stillGraphics(c, c.getX(), c.getY(),
				c.getHeightLevel(), 199, 0);
	}

	public static void showInterface(Client client, int i) {
		client.getOutStream().createFrame(97);
		client.getOutStream().writeWord(i);
		client.flushOutStream();
	}

	public static void sendQuest(Client client, String s, int i) {
		client.getOutStream().createFrameVarSizeWord(126);
		client.getOutStream().writeString(s);
		client.getOutStream().writeWordA(i);
		client.getOutStream().endFrameVarSizeWord();
		client.flushOutStream();
	}

	public void sendStillGraphics(int id, int heightS, int y, int x, int timeBCS) {
		c.getOutStream().createFrame(85);
		c.getOutStream().writeByteC(y - (c.mapRegionY * 8));
		c.getOutStream().writeByteC(x - (c.mapRegionX * 8));
		c.getOutStream().createFrame(4);
		c.getOutStream().writeByte(0);// Tiles away (X >> 4 + Y & 7)
										// //Tiles away from
		// absX and absY.
		c.getOutStream().writeWord(id); // Graphic ID.
		c.getOutStream().writeByte(heightS); // Height of the graphic when
												// cast.
		c.getOutStream().writeWord(timeBCS); // Time before the graphic
												// plays.
		c.flushOutStream();
	}

	public void createArrow(int type, int id) {
		if (c != null) {
			c.getOutStream().createFrame(254); // The packet ID
			c.getOutStream().writeByte(type); // 1=NPC, 10=Player
			c.getOutStream().writeWord(id); // NPC/Player ID
			c.getOutStream().write3Byte(0); // Junk
		}
	}
	

	public void createArrow(int x, int y, int height, int pos) {
		if (c != null) {
			c.getOutStream().createFrame(254); // The packet ID
			c.getOutStream().writeByte(pos); // Position on Square(2 = middle, 3
												// = west, 4 = east, 5 = south,
												// 6 = north)
			c.getOutStream().writeWord(x); // X-Coord of Object
			c.getOutStream().writeWord(y); // Y-Coord of Object
			c.getOutStream().writeByte(height); // Height off Ground
		}
	}

	public void sendQuest(String s, int i) {
		c.getOutStream().createFrameVarSizeWord(126);
		c.getOutStream().writeString(s);
		c.getOutStream().writeWordA(i);
		c.getOutStream().endFrameVarSizeWord();
		c.flushOutStream();
	}

	/**
	 * Quest tab information
	 **/
	public void loadQuests() {
		// c.getAA2().sendQuestTab();
				sendFrame126("Biohazard", 640);
				sendFrame126("", 13136);
				sendFrame126("Welcome to Biohazard", 663);
				sendFrame126("", 673);
				sendFrame126("@red@[@or1@Assault@red@] Points: @or2@"+c.assaultPoints, 7332);
				sendFrame126("@red@[@or1@Donated@red@] Amount: @or2@"+c.donated, 7333);
				sendFrame126("@red@[@or1@PC@red@] Points: @or2@"+c.pcPoints, 7334);
			    sendFrame126("@red@[@or1@FM@red@] Points: @or2@"+c.fmPoints, 7336);
				sendFrame126("@red@[@or1@Slayer@red@] Points: @or2@"+c.slayerPoints, 7383);
				sendFrame126("@red@[@or1@Vote@red@] Points: @or2@"+c.votePoints, 7339);
				sendFrame126("@red@[@or1@BH@red@] Bounty Kills: @or2@"+c.bountyKills, 7338);
				sendFrame126("@red@[@or1@BH@red@] Rogue Kills: @or2@"+c.rogueKills, 7340);
				sendFrame126("@red@[@or1@BH@red@] Bounty Kills Left: @or2@"+((c.killsMultiplier *10) - c.bountyKills), 7346);
				/*if(c.cookAss == 0) {
		        	sendFrame126("Cook's Assistant", 7338);
				} else if(c.cookAss == 3) {
					sendFrame126("@gre@Cook's Assistant", 7338);
				} else {
					sendFrame126("@yel@Cook's Assistant", 7338);
				}
				if(c.RuneMysteries == 0) {
		        	sendFrame126("Rune Mysteries", 7340);
				} else if(c.RuneMysteries == 4) {
					sendFrame126("@gre@Rune Mysteries", 7340);
				} else {
					sendFrame126("@yel@Rune Mysteries", 7340);
				}*/
				sendFrame126("@red@[@or1@CO@red@] XP: @or1@"+Misc.format(c.playerXP[21]), 7341);
				sendFrame126("@red@[@or1@CO@red@] Left: @or1@"+Misc.format((getXPForLevel(getLevelForXP(c.playerXP[21]) + 1)-c.playerXP[21])), 7342);
				if((getXPForLevel(getLevelForXP(c.playerXP[21]) + 1)-c.playerXP[21]) < 0)
					sendFrame126("@red@[@or1@CO@red@]Left: @or1@0", 7342);
				sendFrame126("@red@[@or1@HU@red@] XP: @or1@"+Misc.format(c.playerXP[22]), 7337);
				sendFrame126("@red@[@or1@HU@red@] Left: @or1@"+Misc.format((getXPForLevel(getLevelForXP(c.playerXP[22]) + 1)-c.playerXP[22])), 7343);
				if((getXPForLevel(getLevelForXP(c.playerXP[22]) + 1)-c.playerXP[22]) < 0)
					sendFrame126("@red@[@or1@HU@red@] Left: @or1@0", 7343);
				switch(c.playerRights) {
				case 1:
					sendFrame126("@red@[@or1@Rank@red@] : @cr1@<col=424242>Moderator</col>", 7335);
					break;
				case 2:
					sendFrame126("@red@[@or1@Rank@red@] : @cr2@<col=013ADF>Administrator</col>", 7335);
					break;
				case 3:
					sendFrame126("@red@[@or1@Rank@red@] : @cr2@<col=013ADF>Owner</col>", 7335);
					break;
				case 4:
					sendFrame126("@red@[@or1@Rank@red@] : @cr3@<col=ff0000>Donator</col>", 7335);
					break;
				case 5:
					sendFrame126("@red@[@or1@Rank@red@] : @cr4@<col=0101DF>Super Donator</col>", 7335);
					break;
				case 6:
					sendFrame126("@red@[@or1@Rank@red@] : @cr5@<col=088A08>Extreme Donator</col>", 7335);
					break;
				case 7:
					sendFrame126("@red@[@or1@Rank@red@] : @cr6@<col=5F04B4>Respected</col>", 7335);
					break;
				case 8:
					sendFrame126("@red@[@or1@Rank@red@] : @cr7@<col=8A4B08>Veteran</col>", 7335);
					break;
				case 9:
					sendFrame126("@red@[@or1@Rank@red@] : @cr8@<col=2880BC>Forums Admin</col>", 7335);
					break;
				default:
					sendFrame126("@red@[@or1@Rank@red@] : @whi@Player", 7335);
					break;
				}
				sendFrame126("", 7344);
				sendFrame126("", 7345);
				sendFrame126("", 7347);
				sendFrame126("", 7348);
				
				/*Members Quests*/
				sendFrame126("", 12772);
				sendFrame126("", 7352);
				sendFrame126("", 12129);
				sendFrame126("", 8438);
				sendFrame126("", 18517);
				sendFrame126("", 15847);
				sendFrame126("", 15487);
				sendFrame126("", 12852);
				sendFrame126("", 7354);
				sendFrame126("", 7355);
				sendFrame126("", 7356);
				sendFrame126("", 8679);
				sendFrame126("", 7459);
				sendFrame126("", 7357);
				sendFrame126("", 14912);
				sendFrame126("", 249);
				sendFrame126("", 6024);
				sendFrame126("", 191);
				sendFrame126("", 15235);
				sendFrame126("", 15592);
				sendFrame126("", 6987);
				sendFrame126("", 15098);
				sendFrame126("", 15352);
				sendFrame126("", 18306);
				sendFrame126("", 15499);
				sendFrame126("", 668);
				sendFrame126("", 18684);
				sendFrame126("", 6027);
				sendFrame126("", 18157);
				sendFrame126("", 15847);
				sendFrame126("", 16128);
				sendFrame126("", 12836);
				sendFrame126("", 16149);
				sendFrame126("", 15841);
				sendFrame126("", 7353);
				sendFrame126("", 7358);
				sendFrame126("", 17510);
				sendFrame126("", 7359);
				sendFrame126("", 14169);
				sendFrame126("", 10115);
				sendFrame126("", 14604);
				sendFrame126("", 7360);
				sendFrame126("", 12282);
				sendFrame126("", 13577);
				sendFrame126("", 12839);
				sendFrame126("", 7361);
				sendFrame126("", 11857);
				sendFrame126("", 7362);
				sendFrame126("", 7363);
				sendFrame126("", 7364);
				sendFrame126("", 10135);
				sendFrame126("", 4508);
				sendFrame126("", 11907);
				sendFrame126("", 7365);
				sendFrame126("", 7366);
				sendFrame126("", 7367);
				sendFrame126("", 13389);
				sendFrame126("", 7368);
				sendFrame126("", 11132);
				sendFrame126("", 7369);
				sendFrame126("", 12389);
				sendFrame126("", 13974);
				sendFrame126("", 7370);
				sendFrame126("", 8137);
				sendFrame126("", 7371);
				sendFrame126("", 12345);
				sendFrame126("", 7372);
				sendFrame126("", 8115);
				sendFrame126("", 8576);
				sendFrame126("", 12139);
				sendFrame126("", 7373);
				sendFrame126("", 7374);
				sendFrame126("", 8969);
				sendFrame126("", 7375);
				sendFrame126("", 7376);
				sendFrame126("", 1740);
				sendFrame126("", 3278);
				sendFrame126("", 7378);
				sendFrame126("", 6518);
				sendFrame126("", 7379);
				sendFrame126("", 7380);
				sendFrame126("", 7381);
				sendFrame126("", 11858);
				sendFrame126("", 9927);
				sendFrame126("", 7349);
				sendFrame126("", 7350);
				sendFrame126("", 7351);
				sendFrame126("", 13356);
				/*END OF ALL QUESTS*/
	}
	
	public void reloadConstructionStrings() {
		sendFrame126("@red@[@or1@CO@red@] XP: @or1@"+Misc.format(c.playerXP[21]), 7341);
		sendFrame126("@red@[@or1@CO@red@] Left: @or1@"+Misc.format((getXPForLevel(getLevelForXP(c.playerXP[21]) + 1)-c.playerXP[21])), 7342);
		if((getXPForLevel(getLevelForXP(c.playerXP[21]) + 1)-c.playerXP[21]) < 0)
			sendFrame126("@red@[@or1@CO@red@] Left: @or1@0", 7342);
	}
	
	public void reloadHunterStrings() {
		sendFrame126("@red@[@or1@HU@red@] XP: @or1@"+Misc.format(c.playerXP[22]), 7337);
		sendFrame126("@red@[@or1@HU@red@] Left: @or1@"+Misc.format((getXPForLevel(getLevelForXP(c.playerXP[22]) + 1)-c.playerXP[22])), 7343);
		if((getXPForLevel(getLevelForXP(c.playerXP[22]) + 1)-c.playerXP[22]) < 0)
			sendFrame126("@red@[@or1@HU@red@] Left: @or1@0", 7343);
	}
	
	/**Cooks Assitance**/
	public void cookFinish() {
		c.cookAss = 3;
		c.getPA().addSkillXP(300000, 7);
		c.getPA().showInterface(297);
		c.getPA().showInterface(12140);
		c.getPA().sendFrame126("You have completed: Cook's Assistant", 12144);
		c.getPA().sendFrame126("50,000 Cooking Experience", 12150);
		c.getPA().sendFrame126("", 12151);
		c.getPA().sendFrame126("", 12152);
		c.getPA().sendFrame126("", 12153);
		c.getPA().sendFrame126("", 12154);
		c.getPA().sendFrame126("", 12155);
		c.getPA().sendFrame126("@gre@Cook's Assistant", 7332);
	}
	/**Rune Mysteries*/
	public void RuneMysteriesFinish() {
		c.RuneMysteries = 4;
		c.getPA().showInterface(297);
		c.getPA().showInterface(12140);
		c.getPA().sendFrame126("You have completed: Rune Mysteries Quest", 12144);
		c.getPA().sendFrame126("Ability to mine Rune Essence.", 12150);
		c.getPA().sendFrame126("", 12151);
		c.getPA().sendFrame126("", 12152);
		c.getPA().sendFrame126("", 12153);
		c.getPA().sendFrame126("", 12154);
		c.getPA().sendFrame126("", 12155);
		c.getPA().sendFrame126("@gre@Rune Mysteries", 7332);
	}
	
	public void sendFrame126(String s, int id) {
		// synchronized(c) {
		if(!checkPacket126Update(s, id))
			return;
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrameVarSizeWord(126);
			c.getOutStream().writeString(s);
			c.getOutStream().writeWordA(id);
			c.getOutStream().endFrameVarSizeWord();
			c.flushOutStream();
		}

	}

	public void sendLink(String s) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrameVarSizeWord(187);
			c.getOutStream().writeString(s);
		}

	}

	public void setSkillLevel(int skillNum, int currentLevel, int XP) {
		//synchronized (c) {
			if (c.getOutStream() != null && c != null) {
				c.getOutStream().createFrame(134);
				c.getOutStream().writeByte(skillNum);
				c.getOutStream().writeDWord_v1(XP);
				c.getOutStream().writeByte(currentLevel);
				c.flushOutStream();
			}
		//}
	}

	public void sendFrame106(int sideIcon) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(106);
			c.getOutStream().writeByteC(sideIcon);
			c.flushOutStream();
			requestUpdates();
		}
	}

	public void sendFrame107() {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(107);
			c.flushOutStream();
		}
	}

	public void sendFrame36(int id, int state) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(36);
			c.getOutStream().writeWordBigEndian(id);
			c.getOutStream().writeByte(state);
			c.flushOutStream();
		}

	}

	public void sendFrame185(int Frame) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(185);
			c.getOutStream().writeWordBigEndianA(Frame);
		}

	}

	public void showInterface(int interfaceid) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(97);
			c.getOutStream().writeWord(interfaceid);
			c.flushOutStream();

		}
	}

	public void sendFrame248(int MainFrame, int SubFrame) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(248);
			c.getOutStream().writeWordA(MainFrame);
			c.getOutStream().writeWord(SubFrame);
			c.flushOutStream();

		}
	}

	public void sendFrame246(int MainFrame, int SubFrame, int SubFrame2) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(246);
			c.getOutStream().writeWordBigEndian(MainFrame);
			c.getOutStream().writeWord(SubFrame);
			c.getOutStream().writeWord(SubFrame2);
			c.flushOutStream();

		}
	}

	public void sendFrame171(int MainFrame, int SubFrame) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(171);
			c.getOutStream().writeByte(MainFrame);
			c.getOutStream().writeWord(SubFrame);
			c.flushOutStream();

		}
	}

	public void sendFrame200(int MainFrame, int SubFrame) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(200);
			c.getOutStream().writeWord(MainFrame);
			c.getOutStream().writeWord(SubFrame);
			c.flushOutStream();
		}
	}

	public void sendFrame70(int i, int o, int id) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(70);
			c.getOutStream().writeWord(i);
			c.getOutStream().writeWordBigEndian(o);
			c.getOutStream().writeWordBigEndian(id);
			c.flushOutStream();
		}

	}

	public void sendFrame75(int MainFrame, int SubFrame) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(75);
			c.getOutStream().writeWordBigEndianA(MainFrame);
			c.getOutStream().writeWordBigEndianA(SubFrame);
			c.flushOutStream();
		}

	}

	public void sendFrame164(int Frame) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(164);
			c.getOutStream().writeWordBigEndian_dup(Frame);
			c.flushOutStream();
		}

	}

	public void setPrivateMessaging(int i) { // friends and ignore list status
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(221);
			c.getOutStream().writeByte(i);
			c.flushOutStream();
		}

	}

	public void setChatOptions(int publicChat, int privateChat, int tradeBlock) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(206);
			c.getOutStream().writeByte(publicChat);
			c.getOutStream().writeByte(privateChat);
			c.getOutStream().writeByte(tradeBlock);
			c.flushOutStream();
		}

	}

	public void sendFrame87(int id, int state) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(87);
			c.getOutStream().writeWordBigEndian_dup(id);
			c.getOutStream().writeDWord_v1(state);
			c.flushOutStream();
		}

	}

	public void sendPM(long name, int rights, byte[] chatmessage,
			int messagesize) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrameVarSize(196);
			c.getOutStream().writeQWord(name);
			c.getOutStream().writeDWord(c.lastChatId++);
			c.getOutStream().writeByte(rights);
			c.getOutStream().writeBytes(chatmessage, messagesize, 0);
			c.getOutStream().endFrameVarSize();
			c.flushOutStream();
			// String chatmessagegot = Misc.textUnpack(chatmessage,
			// messagesize);
			// String target = Misc.longToPlayerName(name);
		}

	}

	public void createPlayerHints(int type, int id) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(254);
			c.getOutStream().writeByte(type);
			c.getOutStream().writeWord(id);
			c.getOutStream().write3Byte(0);
			c.flushOutStream();
		}

	}

	public void createObjectHints(int x, int y, int height, int pos) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(254);
			c.getOutStream().writeByte(pos);
			c.getOutStream().writeWord(x);
			c.getOutStream().writeWord(y);
			c.getOutStream().writeByte(height);
			c.flushOutStream();
		}

	}

	public void loadPM(long playerName, int world) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			if (world != 0) {
				world += 9;
			} else if (!Config.WORLD_LIST_FIX) {
				world += 1;
			}
			c.getOutStream().createFrame(50);
			c.getOutStream().writeQWord(playerName);
			c.getOutStream().writeByte(world);
			c.flushOutStream();
		}

	}

	public void removeAllWindows() {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(219);
			c.flushOutStream();
			c.isBanking = false;
		}

	}

	public void closeAllWindows() {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			resetVariables();
			c.getOutStream().createFrame(219);
			c.flushOutStream();
			c.isBanking = false;
		}

	}

	public void sendFrame34(int id, int slot, int column, int amount) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.outStream.createFrameVarSizeWord(34); // init item to smith screen
			c.outStream.writeWord(column); // Column Across Smith Screen
			c.outStream.writeByte(4); // Total Rows?
			c.outStream.writeDWord(slot); // Row Down The Smith Screen
			c.outStream.writeWord(id + 1); // item
			c.outStream.writeByte(amount); // how many there are?
			c.outStream.endFrameVarSizeWord();
		}

	}

	public void walkableInterface(int id) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(208);
			c.getOutStream().writeWordBigEndian_dup(id);
			c.flushOutStream();
		}

	}

	public int mapStatus = 0;

	public void sendFrame99(int state) { // used for disabling map
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			if (mapStatus != state) {
				mapStatus = state;
				c.getOutStream().createFrame(99);
				c.getOutStream().writeByte(state);
				c.flushOutStream();
			}

		}
	}

	/**
	 * public void sendCrashFrame() { // used for crashing cheat clients
	 * synchronized(c) { if(c.getOutStream() != null && c != null) {
	 * c.getOutStream().createFrame(123); c.flushOutStream(); } } }
	 **/

	/**
	 * Reseting animations for everyone
	 **/

	public void frame1() {
		// synchronized(c) {
		for (int i = 0; i < Config.MAX_PLAYERS; i++) {
			if (PlayerHandler.players[i] != null) {
				Client person = (Client) PlayerHandler.players[i];
				if (person != null) {
					if (person.getOutStream() != null && !person.disconnected) {
						if (c.distanceToPoint(person.getX(), person.getY()) <= 25) {
							person.getOutStream().createFrame(1);
							person.flushOutStream();
							person.getPA().requestUpdates();
						}
					}
				}

			}
		}
	}

	/**
	 * Creating projectile
	 **/
	public void createProjectile(int x, int y, int offX, int offY, int angle,
			int speed, int gfxMoving, int startHeight, int endHeight,
			int lockon, int time) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(85);
			c.getOutStream().writeByteC((y - (c.getMapRegionY() * 8)) - 2);
			c.getOutStream().writeByteC((x - (c.getMapRegionX() * 8)) - 3);
			c.getOutStream().createFrame(117);
			c.getOutStream().writeByte(angle);
			c.getOutStream().writeByte(offY);
			c.getOutStream().writeByte(offX);
			c.getOutStream().writeWord(lockon);
			c.getOutStream().writeWord(gfxMoving);
			c.getOutStream().writeByte(startHeight);
			c.getOutStream().writeByte(endHeight);
			c.getOutStream().writeWord(time);
			c.getOutStream().writeWord(speed);
			c.getOutStream().writeByte(16);
			c.getOutStream().writeByte(64);
			c.flushOutStream();

		}
	}

	public void createProjectile2(int x, int y, int offX, int offY, int angle,
			int speed, int gfxMoving, int startHeight, int endHeight,
			int lockon, int time, int slope) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(85);
			c.getOutStream().writeByteC((y - (c.getMapRegionY() * 8)) - 2);
			c.getOutStream().writeByteC((x - (c.getMapRegionX() * 8)) - 3);
			c.getOutStream().createFrame(117);
			c.getOutStream().writeByte(angle);
			c.getOutStream().writeByte(offY);
			c.getOutStream().writeByte(offX);
			c.getOutStream().writeWord(lockon);
			c.getOutStream().writeWord(gfxMoving);
			c.getOutStream().writeByte(startHeight);
			c.getOutStream().writeByte(endHeight);
			c.getOutStream().writeWord(time);
			c.getOutStream().writeWord(speed);
			c.getOutStream().writeByte(slope);
			c.getOutStream().writeByte(64);
			c.flushOutStream();
		}

	}

	// projectiles for everyone within 25 squares
	public void createPlayersProjectile(int x, int y, int offX, int offY,
			int angle, int speed, int gfxMoving, int startHeight,
			int endHeight, int lockon, int time) {
		// synchronized(c) {
		for (int i = 0; i < Config.MAX_PLAYERS; i++) {
			Player p = PlayerHandler.players[i];
			if (p != null) {
				Client person = (Client) p;
				if (person != null) {
					if (person.getOutStream() != null) {
						if (person.distanceToPoint(x, y) <= 25) {
							if (p.heightLevel == c.heightLevel)
								person.getPA().createProjectile(x, y, offX,
										offY, angle, speed, gfxMoving,
										startHeight, endHeight, lockon, time);
						}
					}
				}

			}
		}
	}
	
	private Map<Integer, TinterfaceText> interfaceText = new HashMap<Integer, TinterfaceText>();
	
	public class TinterfaceText {
		public int id;
		public String currentState;
		
		public TinterfaceText(String s, int id) {
			this.currentState = s;
			this.id = id;
		}
		
	}

	public boolean checkPacket126Update(String text, int id) {
		if(!interfaceText.containsKey(id)) {
			interfaceText.put(id, new TinterfaceText(text, id));
		} else {
			TinterfaceText t = interfaceText.get(id);
			if(text.equals(t.currentState)) {
				return false;
			}
			t.currentState = text;
		}
		return true;
	}

	public void createPlayersProjectile2(int x, int y, int offX, int offY,
			int angle, int speed, int gfxMoving, int startHeight,
			int endHeight, int lockon, int time, int slope) {
		// synchronized(c) {
		for (int i = 0; i < Config.MAX_PLAYERS; i++) {
			Player p = PlayerHandler.players[i];
			if (p != null) {
				Client person = (Client) p;
				if (person != null) {
					if (person.getOutStream() != null) {
						if (person.distanceToPoint(x, y) <= 25) {
							person.getPA().createProjectile2(x, y, offX, offY,
									angle, speed, gfxMoving, startHeight,
									endHeight, lockon, time, slope);
						}
					}
				}
			}

		}
	}

	/**
	 ** GFX
	 **/
	public void stillGfx(int id, int x, int y, int height, int time) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(85);
			c.getOutStream().writeByteC(y - (c.getMapRegionY() * 8));
			c.getOutStream().writeByteC(x - (c.getMapRegionX() * 8));
			c.getOutStream().createFrame(4);
			c.getOutStream().writeByte(0);
			c.getOutStream().writeWord(id);
			c.getOutStream().writeByte(height);
			c.getOutStream().writeWord(time);
			c.flushOutStream();
		}

	}

	// creates gfx for everyone
	public void createPlayersStillGfx(int id, int x, int y, int height, int time) {
		// synchronized(c) {
		for (int i = 0; i < Config.MAX_PLAYERS; i++) {
			Player p = PlayerHandler.players[i];
			if (p != null) {
				Client person = (Client) p;
				if (person != null) {
					if (person.getOutStream() != null) {
						if (person.distanceToPoint(x, y) <= 25) {
							person.getPA().stillGfx(id, x, y, height, time);
						}
					}
				}
			}

		}
	}

	/**
	 * Objects, add and remove
	 **/
	public void object(int objectId, int objectX, int objectY, int face,
			int objectType) {
		if (c.distanceToPoint(objectX, objectY) > 60)
			return;
					Region r = Region.getRegion(objectX, objectY);
					if (r != null)
						r.realObjects.add(new Objects(objectId, objectX, objectY, 0, face, objectType));
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(85);
			c.getOutStream().writeByteC(objectY - (c.getMapRegionY() * 8));
			c.getOutStream().writeByteC(objectX - (c.getMapRegionX() * 8));
			c.getOutStream().createFrame(101);
			c.getOutStream().writeByteC((objectType << 2) + (face & 3));
			c.getOutStream().writeByte(0);

			if (objectId != -1) { // removing
				c.getOutStream().createFrame(151);
				c.getOutStream().writeByteS(0);
				c.getOutStream().writeWordBigEndian(objectId);
				c.getOutStream().writeByteS((objectType << 2) + (face & 3));
			}
			c.flushOutStream();
		}

	}

	public void checkObjectSpawn(int objectId, int objectX, int objectY,
			int face, int objectType) {
		if (c.distanceToPoint(objectX, objectY) > 60)
			return;
		Region r = Region.getRegion(objectX, objectY);
		if (r != null)
			r.realObjects.add(new Objects(objectId, objectX, objectY, 0, face, objectType));
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(85);
			c.getOutStream().writeByteC(objectY - (c.getMapRegionY() * 8));
			c.getOutStream().writeByteC(objectX - (c.getMapRegionX() * 8));
			c.getOutStream().createFrame(101);
			c.getOutStream().writeByteC((objectType << 2) + (face & 3));
			c.getOutStream().writeByte(0);

			if (objectId != -1) { // removing
				c.getOutStream().createFrame(151);
				c.getOutStream().writeByteS(0);
				c.getOutStream().writeWordBigEndian(objectId);
				c.getOutStream().writeByteS((objectType << 2) + (face & 3));
			}
			c.flushOutStream();
		}

	}

	/**
	 * Show option, attack, trade, follow etc
	 **/
	public String optionType = "null";

	public void showOption(int i, int l, String s, int a) {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			if (!optionType.equalsIgnoreCase(s)) {
				optionType = s;
				c.getOutStream().createFrameVarSize(104);
				c.getOutStream().writeByteC(i);
				c.getOutStream().writeByteA(l);
				c.getOutStream().writeString(s);
				c.getOutStream().endFrameVarSize();
				c.flushOutStream();
			}

		}
	}

	/**
	 * Open bank
	 **/
	public void openUpBank() {
		// synchronized(c) {
		if (c.getOutStream() != null && c != null) {
			resetVariables();
			c.getItems().resetItems(5064);
			c.getBank().refresh();
			c.getItems().resetTempItems();
			c.getOutStream().createFrame(248);
			c.getOutStream().writeWordA(5292);
			c.getOutStream().writeWord(5063);
			c.flushOutStream();
			c.isBanking = true;
			c.sendMessage("@red@[@cr1@@red@]@bla@To bank your inventory use: @blu@::banki@bla@.");
			c.sendMessage("@red@[@cr1@@red@]@bla@To bank your equipment use: @blu@::banke@bla@.");
		}

	}

	/**
	 * Private Messaging
	 **/
	public void logIntoPM() {
		setPrivateMessaging(2);
		for (int i1 = 0; i1 < Config.MAX_PLAYERS; i1++) {
			Player p = PlayerHandler.players[i1];
			if (p != null && p.isActive) {
				Client o = (Client) p;
				if (o != null) {
					o.getPA().updatePM(c.playerId, 1);
				}
			}
		}
		boolean pmLoaded = false;

		for (int i = 0; i < c.friends.length; i++) {
			if (c.friends[i] != 0) {
				for (int i2 = 1; i2 < Config.MAX_PLAYERS; i2++) {
					Player p = PlayerHandler.players[i2];
					if (p != null
							&& p.isActive
							&& Misc.playerNameToInt64(p.playerName) == c.friends[i]) {
						Client o = (Client) p;
						if (o != null) {
							if (c.playerRights >= 2
									|| p.privateChat == 0
									|| (p.privateChat == 1 && o
											.getPA()
											.isInPM(Misc
													.playerNameToInt64(c.playerName)))) {
								loadPM(c.friends[i], 1);
								pmLoaded = true;
							}
							break;
						}
					}
				}
				if (!pmLoaded) {
					loadPM(c.friends[i], 0);
				}
				pmLoaded = false;
			}
			for (int i1 = 1; i1 < Config.MAX_PLAYERS; i1++) {
				Player p = PlayerHandler.players[i1];
				if (p != null && p.isActive) {
					Client o = (Client) p;
					if (o != null) {
						o.getPA().updatePM(c.playerId, 1);
					}
				}
			}
		}
	}

	public void updatePM(int pID, int world) { // used for private chat updates
		Player p = PlayerHandler.players[pID];
		if (p == null || p.playerName == null || p.playerName.equals("null")) {
			return;
		}
		Client o = (Client) p;
		long l = Misc
				.playerNameToInt64(PlayerHandler.players[pID].playerName);

		if (p.privateChat == 0) {
			for (int i = 0; i < c.friends.length; i++) {
				if (c.friends[i] != 0) {
					if (l == c.friends[i]) {
						loadPM(l, world);
						return;
					}
				}
			}
		} else if (p.privateChat == 1) {
			for (int i = 0; i < c.friends.length; i++) {
				if (c.friends[i] != 0) {
					if (l == c.friends[i]) {
						if (o.getPA().isInPM(
								Misc.playerNameToInt64(c.playerName))) {
							loadPM(l, world);
							return;
						} else {
							loadPM(l, 0);
							return;
						}
					}
				}
			}
		} else if (p.privateChat == 2) {
			for (int i = 0; i < c.friends.length; i++) {
				if (c.friends[i] != 0) {
					if (l == c.friends[i] && c.playerRights < 2) {
						loadPM(l, 0);
						return;
					}
				}
			}
		}
	}

	public boolean isInPM(long l) {
		for (int i = 0; i < c.friends.length; i++) {
			if (c.friends[i] != 0) {
				if (l == c.friends[i]) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Drink AntiPosion Potions
	 * 
	 * @param itemId
	 *            The itemId
	 * @param itemSlot
	 *            The itemSlot
	 * @param newItemId
	 *            The new item After Drinking
	 * @param healType
	 *            The type of poison it heals
	 */
	public void potionPoisonHeal(int itemId, int itemSlot, int newItemId,
			int healType) {
		c.attackTimer = c.getCombat().getAttackDelay(
				ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon])
						.toLowerCase());
		if (c.duelRule[5]) {
			c.sendMessage("Potions has been disabled in this duel!");
			return;
		}
		if (!c.isDead && System.currentTimeMillis() - c.foodDelay > 2000) {
			if (c.getItems().playerHasItem(itemId, 1, itemSlot)) {
				c.sendMessage("You drink the "
						+ ItemAssistant.getItemName(itemId).toLowerCase() + ".");
				c.foodDelay = System.currentTimeMillis();
				// Actions
				if (healType == 1) {
					// Cures The Poison
				} else if (healType == 2) {
					// Cures The Poison + protects from getting poison again
				}
				c.startAnimation(0x33D);
				c.getItems().deleteItem(itemId, itemSlot, 1);
				c.getItems().addItem(newItemId, 1);
				requestUpdates();
			}
		}
	}

	/**
	 * Magic on items
	 **/

	public void magicOnItems(int slot, int itemId, int spellId) {
		if (!c.getItems().playerHasItem(itemId, 1, slot)) {
			// add a method here for logging cheaters(If you want)
			return;
		}
		int[][] boltData = {
				{1155, 879, 9, 9236}, {1155, 9337, 17, 9240}, {1165, 9335, 19, 9237}, {1165, 880, 29, 9238},
				{1165, 9338, 37, 9241}, {1176, 9336, 39, 9239}, {1176, 9339, 59, 9242}, {1180, 9340, 67, 9243},
				{1187, 9341, 78, 9244}, {6003, 9342, 97, 9245}
			};
		switch (spellId) {
		case 1155: // Lvl-1 enchant sapphire
		case 1165: // Lvl-2 enchant emerald
		case 1176: // Lvl-3 enchant ruby
		case 1180: // Lvl-4 enchant diamond
		case 1187: // Lvl-5 enchant dragonstone
		case 6003: // Lvl-6 enchant onyx
			for(int i = 0; i < boltData.length; i++) {
				if(itemId == boltData[i][1]) {
					Magic.enchantBolt(c, spellId, itemId, 28);
				} else {
					Magic.enchantItem(c, itemId, spellId);
				}
			}
			break;

		case 1162: // low alch
			if(System.currentTimeMillis() - c.alchDelay > 1000) {	
				if(c.getItems().playerHasItem(itemId, 1, slot)){
				if(!c.getCombat().checkMagicReqs(49)) {
					break;
				}
				if(itemId == 995) {
				    return;
				}
				if(c.getItems().playerHasItem(itemId, 1, slot)){
				c.getItems().deleteItem(itemId, slot, 1);
				c.getItems().addItem(995, c.getShops().getItemShopValue(itemId)/3);
				c.startAnimation(c.MAGIC_SPELLS[49][2]);
				c.gfx100(c.MAGIC_SPELLS[49][3]);
				c.alchDelay = System.currentTimeMillis();
				sendFrame106(6);
				
				addSkillXP(c.MAGIC_SPELLS[49][7] * Config.MAGIC_EXP_RATE, 6);
				refreshSkill(6);
				
				}
				}
			}
			break;
			
			case 1178: // high alch
			if(System.currentTimeMillis() - c.alchDelay > 2000) {	
				if(c.getItems().playerHasItem(itemId, 1, slot)){
				if(!c.getCombat().checkMagicReqs(50)) {
					break;
				}
				if(itemId == 995) {
				    return;
				}		
				if(c.getItems().playerHasItem(itemId, 1, slot)){
				c.getItems().deleteItem(itemId, slot, 1);
				c.getItems().addItem(995, (int)(c.getShops().getItemShopValue(itemId)*.75));
				c.startAnimation(c.MAGIC_SPELLS[50][2]);
				c.gfx100(c.MAGIC_SPELLS[50][3]);
				c.alchDelay = System.currentTimeMillis();
				sendFrame106(6);
				
				addSkillXP(c.MAGIC_SPELLS[50][7] * Config.MAGIC_EXP_RATE, 6);
				refreshSkill(6);
				
				}
				}
			}
			break;
		}
	}

	/**
	 * Dieing
	 **/
	/*
	 * private int randomKillMessage; public void randomKillMessage() { Client o
	 * = (Client) Server.playerHandler.players[c.killerId]; switch
	 * (randomKillMessage) { case 0: o.sendMessage("You have defeated " +
	 * Misc.capitalize(c.playerName)+" in battle."); break; case 1:
	 * o.sendMessage("Well done, you've pwned " +
	 * Misc.capitalize(c.playerName)+"."); break; case 2:
	 * o.sendMessage(Misc.capitalize
	 * (c.playerName)+" was clearly no match for you."); break; case 3:
	 * o.sendMessage("You just made " +
	 * Misc.capitalize(c.playerName)+" lose the game."); break; case 4:
	 * o.sendMessage("You have proven your superiority over " +
	 * Misc.capitalize(c.playerName)+"."); break; case 5:
	 * o.sendMessage("Let all warriors learn from the fate of " +
	 * Misc.capitalize(c.playerName)+" and fear you."); break; case 6:
	 * o.sendMessage("It's official: you are far more awesome than " +
	 * Misc.capitalize(c.playerName)+" is."); break; } }
	 */

	public String killMessage() {
		int a = Misc.random(15);
		@SuppressWarnings("unused")
		Client o = (Client) PlayerHandler.players[c.killerId];
		switch (a) {
		case 0:
			return "With a crushing blow, you defeat "
					+ Misc.capitalize(c.playerName) + ".";
		case 1:
			return "It's a humiliating defeat for "
					+ Misc.capitalize(c.playerName) + ".";
		case 2:
			return "" + Misc.capitalize(c.playerName)
					+ " didn't stand a chance against you.";
		case 3:
			return "You've defeated " + Misc.capitalize(c.playerName) + ".";
		case 4:
			return "" + Misc.capitalize(c.playerName)
					+ " regrets the day they met you in combat.";
		case 5:
			return "It's all over for " + Misc.capitalize(c.playerName) + ".";
		case 6:
			return "" + Misc.capitalize(c.playerName)
					+ " falls before your might.";
		case 7:
			return "Can anyone defeat you? Certainly not "
					+ Misc.capitalize(c.playerName) + ".";
		case 8:
			return "You were clearly a better fighter than "
					+ Misc.capitalize(c.playerName) + ".";
		case 9:
			return "It's official: you are far more awesome than "
					+ Misc.capitalize(c.playerName) + " is.";
		case 10:
			return "Let all warriors learn from the fate of "
					+ Misc.capitalize(c.playerName) + " and fear you.";
		case 11:
			return "You have proven your superiority over "
					+ Misc.capitalize(c.playerName) + ".";
		case 12:
			return "You just made " + Misc.capitalize(c.playerName)
					+ " lose the game.";
		case 13:
			return "Well done, you've pwned " + Misc.capitalize(c.playerName)
					+ ".";
		case 14:
			return "" + Misc.capitalize(c.playerName)
					+ " was clearly no match for you.";
		default:
			return "You were clearly a better fighter than " + c.playerName
					+ "";
		}
	}

	/*public void applyDead() {
		c.respawnTimer = 15;
		c.isDead = false;

		if (c.duelStatus != 6) {
			// c.killerId = c.getCombat().getKillerId(c.playerId);
			c.killerId = findKiller();
			Client o = (Client) Server.playerHandler.players[c.killerId];
			Random generator = new Random();
			// int randomIndex = generator.nextInt(5);
			int roll = generator.nextInt(6) + 1;
			if (o != null) {
                if(!(c.npcIndex > 0) && c.inPits == false){
                }
				if (c.killerId != c.playerId)
					o.sendMessage(killMessage());
				  if(c.inWild() && c.npcIndex > 0){
				if (!PlayerKilling.hostOnList(o, c.connectedFrom) && c.inWild() && o.inWild()) {
					PlayerKilling.addHostToList(o, c.connectedFrom);
					// o.pkPoints++;
					o.magePoints += roll;
					o.sendMessage("You have received " + roll
							+ " points, you now have " + o.magePoints
							+ " PvP Points.");
				}
				} else {
					o.sendMessage("You have recently defeated " + c.playerName
							+ ", you don't receive any PvP Points.");
				}
				c.playerKilled = c.playerId;
				if (o.duelStatus == 5) {
					o.duelStatus++;
				}
			}
		}
		c.faceUpdate(0);
		EventManager.addEvent(new Event() {
			public void execute(EventContainer b) {
				c.npcIndex = 0;
				c.playerIndex = 0;
				b.stop();
			}
		}, 2500);
		c.stopMovement();
		if (c.duelStatus <= 4) {
			c.sendMessage("Oh dear, you are dead!");
		} else if (c.duelStatus != 6) {
			Client o = (Client) Server.playerHandler.players[c.killerId];
			c.sendMessage("You have lost the duel!");
			PlayerSave.saveGame(o);
			PlayerSave.saveGame(c);
		}
		resetDamageDone();
		c.specAmount = 10;
		c.getItems().addSpecialBar(c.playerEquipment[c.playerWeapon]);
		c.lastVeng = 0;
		c.vengOn = false;
		resetFollowers();
		c.attackTimer = 10;
	}
	
*/   
	public void sendSong(int id) {
		if (c.getOutStream() != null && c != null && id != -1) {
			c.getOutStream().createFrame(74);
			c.getOutStream().writeWordBigEndian(id);
		}
	}

	public void sendQuickSong(int id, int songDelay) {
		if (c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(121);
			c.getOutStream().writeWordBigEndian(id);
			c.getOutStream().writeWordBigEndian(songDelay);
			c.flushOutStream();
		}
	}
	public void sendColor(int id, int color) {
        if (c.getOutStream() != null && c != null) {
                c.outStream.createFrame(122);
                c.outStream.writeWordBigEndianA(id);
                c.outStream.writeWordBigEndianA(color);
        }
	}
	public void sendSound(int id, int type, int delay) {
		if(c.getOutStream() != null && c != null && id != -1) {
			c.getOutStream().createFrame(174);
			c.getOutStream().writeWordBigEndian(id);
			c.getOutStream().writeByte(type);
			c.getOutStream().writeWordBigEndian(delay);
			c.flushOutStream();
		}
	}
	public void applyDead() { 
		int weapon = c.playerEquipment[c.playerWeapon];
		c.getTradeAndDuel().stakedItems.clear();
		c.respawnTimer = 15;
		c.isDead = false;
    
	    if(c.duelStatus != 6) {
	        //c.killerId = c.getCombat().getKillerId(c.playerId);
	        c.killerId = findKiller();
	        Client o = (Client) PlayerHandler.players[c.killerId];
	        Random generator = new Random();
			// int randomIndex = generator.nextInt(5);
			@SuppressWarnings("unused")
			int roll = generator.nextInt(4) + 1;
	        if(o != null) {
	        	if (c.getItems().playerHasItem(6570,1) || c.playerEquipment[c.playerCape] == 6570) {
	        	     c.hasFCape = true;
	        	}
	            if(!(c.npcIndex > 0) && c.inPits == false){
	            }
	            //castlewars
                if (weapon == CastleWars.SARA_BANNER || weapon == CastleWars.ZAMMY_BANNER) {
                    c.getItems().removeItem(weapon, 3);
                    c.getItems().deleteItem2(weapon, 1);
                    CastleWars.dropFlag(c, weapon);
                }
	            if (c.killerId != c.playerId)
	                o.sendMessage("You have defeated "+Misc.optimizeText(c.playerName)+"!");
	            c.playerKilled = c.playerId;
	            if(o.duelStatus == 5) {
	                o.duelStatus++;
	            }
	        }
	    }
	    c.faceUpdate(0);
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				container.stop();
			}
			@Override
			public void stop() {
				c.npcIndex = 0;
				c.playerIndex = 0;
			}
		}, 4);
		c.stopMovement();
		if(c.duelStatus <= 4) {
			//castlewars
            if (CastleWars.isInCw(c)) {
            	Client o = (Client) PlayerHandler.players[c.killerId];
                c.cwDeaths += 1;
                o.cwKills += 1;
            }
			c.getTradeAndDuel().stakedItems.clear();
			c.sendMessage("Oh dear you are dead!");
		} else if(c.duelStatus != 6) {
			c.getTradeAndDuel().stakedItems.clear();
			c.sendMessage("You have lost the duel!");
		}
	    resetDamageDone();
	    c.specAmount = 100;
	    c.getItems().addSpecialBar(c.playerEquipment[c.playerWeapon]);
	    c.lastVeng = 0;
	    c.vengOn = false;
	    resetFollowers();
	    c.attackTimer = 10;
	}
	
	public void resetDamageDone() {
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			if (PlayerHandler.players[i] != null) {
				PlayerHandler.players[i].damageTaken[c.playerId] = 0;
			}
		}
	}

	public void vengMe() {
		if (System.currentTimeMillis() - c.lastVeng > 30000) {
			if (c.getItems().playerHasItem(557, 10)
					&& c.getItems().playerHasItem(9075, 4)
					&& c.getItems().playerHasItem(560, 2)) {
				c.vengOn = true;
				c.lastVeng = System.currentTimeMillis();
				c.startAnimation(4410);
				c.gfx100(604);
				c.getItems().deleteItem(557, c.getItems().getItemSlot(557), 10);
				c.getItems().deleteItem(560, c.getItems().getItemSlot(560), 2);
				c.getItems()
						.deleteItem(9075, c.getItems().getItemSlot(9075), 4);
			} else {
				c.sendMessage("You need 10 Earth runes, 2 Death runes and 4 Astral runes to cast this.");
			}
		} else {
			c.sendMessage("You must wait 30 seconds before casting this again.");
		}
	}

	public void resetFollowers() {
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				if (PlayerHandler.players[j].followId == c.playerId) {
					Client c = (Client) PlayerHandler.players[j];
					c.getPA().resetFollow();
				}
			}
		}
	}

	public void resetTb() {
		c.teleBlockLength = 0;
		c.teleBlockDelay = 0;
	}

	public void giveLife() {
		c.isDead = false;
		c.faceUpdate(-1);
		c.freezeTimer = 0;
		if (c.duelStatus <= 4 && !c.getPA().inPitsWait()) { // if we are not in
															// a duel we must be
															// in wildy so
															// remove items
			//castlewars
			if (!CastleWars.isInCw(c) && !c.inPits && !c.inFightCaves()) {
				c.getItems().resetKeepItems();
				if ((c.playerRights == 2 && Config.ADMIN_DROP_ITEMS)
						|| c.playerRights != 2) {
					if (!c.isSkulled) { // what items to keep
						c.getItems().keepItem(0, true);
						c.getItems().keepItem(1, true);
						c.getItems().keepItem(2, true);
					}
					if (c.hasFCape) {
						c.getItems().addItem(6570,1);
						c.hasFCape = false;
						}
					if (c.prayerActive[10]
							&& System.currentTimeMillis() - c.lastProtItem > 700) {
						c.getItems().keepItem(3, true);
					}
					c.getItems().dropAllItems(); // drop all items
					c.getItems().deleteAllItems(); // delete all items

					if (!c.isSkulled) { // add the kept items once we finish
										// deleting and dropping them
						for (int i1 = 0; i1 < 3; i1++) {
							if (c.itemKeptId[i1] > 0) {
								c.getItems().addItem(c.itemKeptId[i1], 1);
							}
						}
					}
					if (c.prayerActive[10]) { // if we have protect items
						if (c.itemKeptId[3] > 0) {
							c.getItems().addItem(c.itemKeptId[3], 1);
						}
					}
				}
				c.getItems().resetKeepItems();
			} else if (c.inPits) {
				c.duelStatus = 0;
				Server.fightPits.removePlayerFromPits(c.playerId);
				c.pitsStatus = 1;
			}
		}
		c.getCombat().resetPrayers();
		for (int i = 0; i < 20; i++) {
			c.playerLevel[i] = getLevelForXP(c.playerXP[i]);
			c.getPA().refreshSkill(i);
		}
		if (c.pitsStatus == 1) {
			c.pitsStatus = 0;
			movePlayer(2399, 5173, 0);
		} else if(c.inBH) {
			BountyHunter.handleBHDeath(c);
			c.getPA().movePlayer(3179, 3685, 0);
			//castlewars
		} else if (CastleWars.isInCw(c)) {
	            if (CastleWars.getTeamNumber(c) == 1) {
	                c.getPA().movePlayer(2426 + Misc.random(3), 3076 - Misc.random(3), 1);
	            } else {
	                c.getPA().movePlayer(2373 + Misc.random(3), 3131 - Misc.random(3), 1);
	            }
		} else if (c.duelStatus <= 4) { // if we are not in a duel repawn to
										// wildy
			movePlayer(Config.RESPAWN_X, Config.RESPAWN_Y, 0);
			c.isSkulled = false;
			c.skullTimer = 0;
			c.attackedPlayers.clear();
		} else if (c.inFightCaves()) {
			c.getPA().resetTzhaar();
		} else { // we are in a duel, respawn outside of arena
			Client o = (Client) PlayerHandler.players[c.duelingWith];
			if (o != null) {
				o.getPA().createPlayerHints(10, -1);
				if (o.duelStatus == 6) {
					o.getTradeAndDuel().duelVictory();
				}
			}
			c.getPA().movePlayer(
					Config.DUELING_RESPAWN_X
							+ (Misc.random(Config.RANDOM_DUELING_RESPAWN)),
					Config.DUELING_RESPAWN_Y
							+ (Misc.random(Config.RANDOM_DUELING_RESPAWN)), 0);
			o.getPA().movePlayer(
					Config.DUELING_RESPAWN_X
							+ (Misc.random(Config.RANDOM_DUELING_RESPAWN)),
					Config.DUELING_RESPAWN_Y
							+ (Misc.random(Config.RANDOM_DUELING_RESPAWN)), 0);
			if (c.duelStatus != 6) { // if we have won but have died, don't
										// reset the duel status.
				c.getTradeAndDuel().resetDuel();
			}
		}
		// PlayerSaving.requestSave(c.playerId);
		PlayerSave.saveGame(c);
		c.getCombat().resetPlayerAttack();
		resetAnimation();
		c.startAnimation(65535);
		frame1();
		resetTb();
		c.isSkulled = false;
		c.attackedPlayers.clear();
		c.headIconPk = -1;
		c.skullTimer = -1;
		c.damageTaken = new int[Config.MAX_PLAYERS];
		c.getPA().requestUpdates();
	}

	/**
	 * Location change for digging, levers etc
	 **/

	public void changeLocation() {
		switch (c.newLocation) {
		case 1:
			// sendFrame99(2);
			movePlayer(3578, 9706, -1);
			break;
		case 2:
			// sendFrame99(2);
			movePlayer(3568, 9683, -1);
			break;
		case 3:
			// sendFrame99(2);
			movePlayer(3557, 9703, -1);
			break;
		case 4:
			// sendFrame99(2);
			movePlayer(3556, 9718, -1);
			break;
		case 5:
			// sendFrame99(2);
			movePlayer(3534, 9704, -1);
			break;
		case 6:
			// sendFrame99(2);
			movePlayer(3546, 9684, -1);
			break;
		}
		c.newLocation = 0;
	}
	
	public void setUWAnim() {
		if (c != null) {
			c.prevplayerWalkIndex = c.playerWalkIndex;
			c.prevPlayerStandIndex = c.playerStandIndex;
			c.prevPrevPlayerRunIndex = c.playerRunIndex;
			c.prevPlayerTurnIndex = c.playerTurnIndex;
			c.prevPlayerTurn180Index = c.playerTurn180Index;
			c.prevPlayerTurn90CCWIndex = c.playerTurn90CCWIndex;
			c.prevPlayerTurn90CWIndex = c.playerTurn90CWIndex;
			c.prevRunning2 = c.isRunning2;
			c.playerRunIndex = 1851;
			c.playerStandIndex = 1501;
			c.playerWalkIndex = 1851;
			c.playerTurnIndex = 1501;
			c.playerTurn90CWIndex = 1501;
			c.playerTurn90CCWIndex = 1501;
			c.playerTurn180Index = 1501;
			c.getPA().requestUpdates();
			c.getPA().closeAllWindows();
		}
}

public void underWaterTele() {
		setUWAnim();
		c.fadeCrash(2972, 9507, 0);
		c.underWater = true;
}

	/**
	 * Teleporting
	 **/
	public void spellTeleport(int x, int y, int height) {
		c.getPA().startTeleport(x, y, height,
				c.playerMagicBook == 1 ? "ancient" : "modern");
	}

	public void startTeleport(int x, int y, int height, String teleportType) {
		if (c.duelStatus == 5) {
			c.sendMessage("You can't teleport during a duel!");
			return;
		}
		//castlewars
        if (CastleWars.isInCw(c)) {
            c.sendMessage("You cannot teleport away from a Castle Wars Game!");
            return;
        }
        if (CastleWars.isInCwWait(c)) {
            c.sendMessage("You cannot teleport away from a Castle Wars Game!");
            return;
        }
        //end
        if (c.inTrawlerGame()) {
        	 c.sendMessage("You cannot teleport away from Fishing Trawler!");
             return;
        }
        if(c.inPits) {
        	c.sendMessage("You cannot teleport away from a Fight Pits Game!");
        	return;
        }
		if(c.underWater) {
			c.getPA().resetAnimationsToPrevious();
			c.underWater = false;
		}
		if(c.bandosKills > 0 || c.armaKills > 0 || c.zamorakKills > 0 || c.saraKills > 0) {
			c.bandosKills = 0;
			c.zamorakKills = 0;
			c.saraKills = 0;
			c.armaKills = 0;
			c.getPA(). sendFrame126("@cya@" + c.bandosKills, 16217);
			c.getPA(). sendFrame126("@cya@" + c.zamorakKills, 16219);
			c.getPA(). sendFrame126("@cya@" + c.saraKills, 16218);
			c.getPA(). sendFrame126("@cya@" + c.armaKills, 16216);
		}
		if(c.gwdCoords()) {
			c.bandosKills = 0;
			c.zamorakKills = 0;
			c.saraKills = 0;
			c.armaKills = 0;
			c.getPA(). sendFrame126("@cya@" + c.bandosKills, 16217);
			c.getPA(). sendFrame126("@cya@" + c.zamorakKills, 16219);
			c.getPA(). sendFrame126("@cya@" + c.saraKills, 16218);
			c.getPA(). sendFrame126("@cya@" + c.armaKills, 16216);
		}
		if (c.inWild() && c.wildLevel > Config.NO_TELEPORT_WILD_LEVEL) {
			c.sendMessage("You can't teleport above level "
					+ Config.NO_TELEPORT_WILD_LEVEL + " in the wilderness.");
			return;
		}
		if (System.currentTimeMillis() - c.teleBlockDelay < c.teleBlockLength) {
			c.getPA().closeAllWindows();
			c.sendMessage("You are teleblocked and can't teleport.");
			return;
		}
		if(c.teleTimer != 0) {
			c.getPA().closeAllWindows();
		}
		if (!c.isDead && c.teleTimer == 0 && c.respawnTimer == -6) {
			if (c.playerIndex > 0 || c.npcIndex > 0)
				c.getCombat().resetPlayerAttack();
			c.stopMovement();
			removeAllWindows();
			resetVariables();	
			resetFollow();
			c.teleX = x;
			c.teleY = y;
			c.npcIndex = 0;
			c.playerIndex = 0;
			c.faceUpdate(0);
			c.teleHeight = height;
			if (teleportType.equalsIgnoreCase("modern")) {
				c.startAnimation(714);
				c.teleEndGfx = 0;
				c.teleTimer = 10;
				c.gfx100(111);
				c.teleEndAnimation = 715;
			}
			if (teleportType.equalsIgnoreCase("ancient")) {
				c.startAnimation(1979);
				c.teleGfx = 0;
				c.teleEndGfx = 455;
				c.teleTimer = 9;
				c.teleEndAnimation = 0;
				c.gfx0(392);
			}

		}
	}
	
	public void climbLadder(final boolean up, final int x, final int y, final int height) {
		c.startAnimation(up ? 828 : 827);
		CycleEventHandler.addEvent(c, new CycleEvent() {
			public void execute(CycleEventContainer container) {
				c.getPA().movePlayer(x, y, height);
				container.stop();
			}
			@Override
			public void stop() {
			}
		}, 1);
	}

	public void startTeleport2(int x, int y, int height) {
		if (c.duelStatus == 5) {
			c.sendMessage("You can't teleport during a duel!");
			return;
		}
		if (System.currentTimeMillis() - c.teleBlockDelay < c.teleBlockLength) {
			c.sendMessage("You are teleblocked and can't teleport.");
			return;
		}
		if (!c.isDead && c.teleTimer == 0) {
			c.stopMovement();
			removeAllWindows();
			c.teleX = x;
			c.teleY = y;
			c.npcIndex = 0;
			c.playerIndex = 0;
			c.faceUpdate(0);
			c.teleHeight = height;
			c.startAnimation(714);
			c.teleTimer = 11;
			c.gfx100(111);
			c.teleEndAnimation = 715;

		}
	}

	public void processTeleport() {
		c.teleportToX = c.teleX;
		c.teleportToY = c.teleY;
		c.heightLevel = c.teleHeight;
		if (c.teleEndAnimation > 0) {
			c.startAnimation(c.teleEndAnimation);
		}
		if(c.teleEndGfx > 0) {
			c.gfx0(c.teleEndGfx);
			c.canWalk = false;
			@SuppressWarnings("unused")
			int timer = c.playerMagicBook == 1 ? 6 : 5;
			CycleEventHandler.addEvent(c, new CycleEvent() {
				int timer;
				@Override
				public void execute(CycleEventContainer container) {
					if(timer == 0)
						container.stop();
					if(timer > 0)
						timer --;
				}
				@Override
				public void stop() {
					c.canWalk = true;
				}
			}, 1);
		}
	}

	/**
	 * Following
	 **/

	/*
	 * public void Player() { if(Server.playerHandler.players[c.followId] ==
	 * null || Server.playerHandler.players[c.followId].isDead) {
	 * c.getPA().resetFollow(); return; } if(c.freezeTimer > 0) { return; } int
	 * otherX = Server.playerHandler.players[c.followId].getX(); int otherY =
	 * Server.playerHandler.players[c.followId].getY(); boolean withinDistance =
	 * c.goodDistance(otherX, otherY, c.getX(), c.getY(), 2); boolean
	 * hallyDistance = c.goodDistance(otherX, otherY, c.getX(), c.getY(), 2);
	 * boolean bowDistance = c.goodDistance(otherX, otherY, c.getX(), c.getY(),
	 * 6); boolean rangeWeaponDistance = c.goodDistance(otherX, otherY,
	 * c.getX(), c.getY(), 2); boolean sameSpot = (c.absX == otherX && c.absY ==
	 * otherY); if(!c.goodDistance(otherX, otherY, c.getX(), c.getY(), 25)) {
	 * c.followId = 0; c.getPA().resetFollow(); return; }
	 * c.faceUpdate(c.followId+32768); if ((c.usingBow || c.mageFollow ||
	 * c.autocastId > 0 && (c.npcIndex > 0 || c.playerIndex > 0)) && bowDistance
	 * && !sameSpot) { c.stopMovement(); return; } if (c.usingRangeWeapon &&
	 * rangeWeaponDistance && !sameSpot && (c.npcIndex > 0 || c.playerIndex >
	 * 0)) { c.stopMovement(); return; } if(c.goodDistance(otherX, otherY,
	 * c.getX(), c.getY(), 1) && !sameSpot) { return; }
	 * c.outStream.createFrame(174); boolean followPlayer = c.followId > 0; if
	 * (c.freezeTimer <= 0) if (followPlayer) c.outStream.writeWord(c.followId);
	 * else c.outStream.writeWord(c.followId2); else c.outStream.writeWord(0);
	 * 
	 * if (followPlayer) c.outStream.writeByte(1); else
	 * c.outStream.writeByte(0); if (c.usingBow && c.playerIndex > 0)
	 * c.followDistance = 5; else if (c.usingRangeWeapon && c.playerIndex > 0)
	 * c.followDistance = 3; else if (c.spellId > 0 && c.playerIndex > 0)
	 * c.followDistance = 5; else c.followDistance = 1;
	 * c.outStream.writeWord(c.followDistance); }
	 */

	/*public void followPlayer() {
		if (PlayerHandler.players[c.followId] == null
				|| PlayerHandler.players[c.followId].isDead) {
			resetFollow();
			return;
		}
		if (c.freezeTimer > 0) {
			return;
		}
		if (c.isDead || c.playerLevel[3] <= 0)
			return;

		int otherX = PlayerHandler.players[c.followId].getX();
		int otherY = PlayerHandler.players[c.followId].getY();

		boolean sameSpot = (c.absX == otherX && c.absY == otherY);

		boolean hallyDistance = c.goodDistance(otherX, otherY, c.getX(),
				c.getY(), 2);

		boolean rangeWeaponDistance = c.goodDistance(otherX, otherY, c.getX(),
				c.getY(), 4);
		boolean bowDistance = c.goodDistance(otherX, otherY, c.getX(),
				c.getY(), 6);
		boolean mageDistance = c.goodDistance(otherX, otherY, c.getX(),
				c.getY(), 7);

		boolean castingMagic = (c.usingMagic || c.mageFollow || c.autocasting || c.spellId > 0)
				&& mageDistance;
		boolean playerRanging = (c.usingRangeWeapon) && rangeWeaponDistance;
		boolean playerBowOrCross = (c.usingBow) && bowDistance;

		if (!c.goodDistance(otherX, otherY, c.getX(), c.getY(), 25)) {
			c.followId = 0;
			resetFollow();
			return;
		}
		c.faceUpdate(c.followId + 32768);
		if (!sameSpot) {
			if (c.playerIndex > 0 && !c.usingSpecial && c.inWild()) {
				if (c.usingSpecial && (playerRanging || playerBowOrCross)) {
					c.stopMovement();
					return;
				}
				if (castingMagic || playerRanging || playerBowOrCross) {
					c.stopMovement();
					return;
				}
				if (c.getCombat().usingHally() && hallyDistance) {
					c.stopMovement();
					return;
				}
			}
		}
		if (otherX == c.absX && otherY == c.absY) {
			/*int r = Misc.random(3);
			switch (r) {
			case 0:
				walkTo(0, -1);
				break;
			case 1:
				walkTo(0, 1);
				break;
			case 2:
				walkTo(1, 0);
				break;
			case 3:
				walkTo(-1, 0);
				break;
			}*/
	      	/*walkClipped(c);
		} else if (c.isRunning2) {
			if (otherY > c.getY() && otherX == c.getX()) {
				playerWalk(otherX, otherY - 1);
			} else if (otherY < c.getY() && otherX == c.getX()) {
				playerWalk(otherX, otherY + 1);
			} else if (otherX > c.getX() && otherY == c.getY()) {
				playerWalk(otherX - 1, otherY);
			} else if (otherX < c.getX() && otherY == c.getY()) {
				playerWalk(otherX + 1, otherY);
			} else if (otherX < c.getX() && otherY < c.getY()) {
				playerWalk(otherX + 1, otherY + 1);
			} else if (otherX > c.getX() && otherY > c.getY()) {
				playerWalk(otherX - 1, otherY - 1);
			} else if (otherX < c.getX() && otherY > c.getY()) {
				playerWalk(otherX + 1, otherY - 1);
			} else if (otherX > c.getX() && otherY < c.getY()) {
				playerWalk(otherX + 1, otherY - 1);
			}
		} else {
			if (otherY > c.getY() && otherX == c.getX()) {
				playerWalk(otherX, otherY - 1);
			} else if (otherY < c.getY() && otherX == c.getX()) {
				playerWalk(otherX, otherY + 1);
			} else if (otherX > c.getX() && otherY == c.getY()) {
				playerWalk(otherX - 1, otherY);
			} else if (otherX < c.getX() && otherY == c.getY()) {
				playerWalk(otherX + 1, otherY);
			} else if (otherX < c.getX() && otherY < c.getY()) {
				playerWalk(otherX + 1, otherY + 1);
			} else if (otherX > c.getX() && otherY > c.getY()) {
				playerWalk(otherX - 1, otherY - 1);
			} else if (otherX < c.getX() && otherY > c.getY()) {
				playerWalk(otherX + 1, otherY - 1);
			} else if (otherX > c.getX() && otherY < c.getY()) {
				playerWalk(otherX - 1, otherY + 1);
			}
		}
		c.faceUpdate(c.followId + 32768);
	}*/
	
    @SuppressWarnings("unused")
	public void followPlayer() {
        if (PlayerHandler.players[c.followId] == null
                        || PlayerHandler.players[c.followId].isDead) {
			c.followId = 0;
                return;
        }
        if (c.freezeTimer > 0) {
                return;
        }
        
        if(inPitsWait()) {
			c.followId = 0;
		}
        
        if (c.isDead || c.playerLevel[3] <= 0)
                return;

        int otherX = PlayerHandler.players[c.followId].getX();
        int otherY = PlayerHandler.players[c.followId].getY();

        boolean sameSpot = (c.absX == otherX && c.absY == otherY);

        boolean hallyDistance = c.goodDistance(otherX, otherY, c.getX(),
                        c.getY(), 2);
		boolean withinDistance = c.goodDistance(otherX, otherY, c.getX(), c.getY(), 2);
        boolean rangeWeaponDistance = c.goodDistance(otherX, otherY, c.getX(),
                        c.getY(), 4);
        boolean bowDistance = c.goodDistance(otherX, otherY, c.getX(),
                        c.getY(), 6);
        boolean mageDistance = c.goodDistance(otherX, otherY, c.getX(),
                        c.getY(), 7);

        boolean castingMagic = (c.usingMagic || c.mageFollow || c.autocasting || c.spellId > 0)
                        && mageDistance;
        boolean playerRanging = (c.usingRangeWeapon)
                        && rangeWeaponDistance;
        boolean playerBowOrCross = (c.usingBow) && bowDistance;

		if(!c.goodDistance(otherX, otherY, c.getX(), c.getY(), 25)) {
			c.followId = 0;
			return;
		}
		if(c.goodDistance(otherX, otherY, c.getX(), c.getY(), 1)) {
			if (otherX != c.getX() && otherY != c.getY()) {
				stopDiagonal(otherX, otherY);
				return;
			}
		}
		
		if((c.usingBow || c.mageFollow || (c.playerIndex > 0 && c.autocastId > 0)) && bowDistance && !sameSpot) {
			return;
		}

		if(c.getCombat().usingHally() && hallyDistance && !sameSpot) {
			return;
		}

		if(c.usingRangeWeapon && rangeWeaponDistance && !sameSpot) {
			return;
		}
		
		c.faceUpdate(c.followId+32768);
        switch (c.otherDirection) {
        case 0:
            playerWalk(otherX + 0, otherY-1);
            break;
	    case 1:
	    case 2:
	    case 3:
	    	playerWalk(otherX - 1, otherY - 1);
	        break;
	    case 4:
	    	playerWalk(otherX-1,otherY + 0);
	        break;
	    case 6:
	    case 5:
	    case 7:
	    	playerWalk(otherX-1,otherY + 1);
	        break;
	    case 8:
	    	playerWalk(otherX+0, otherY+1);
	        break;
	        
	    case 10:
	    case 11:
	    case 9:
	    	playerWalk(otherX+1, otherY+1);
	        break;
	        
	    case 12:
	    	playerWalk(otherX+1, otherY +0);
	        break;
	        
	    case 14:
	    case 15:
	    case 13:
	    	playerWalk(otherX+1, otherY-1);
	        break;
        }
        if (otherX == c.absX && otherY == c.absY) {
               /* int r = Misc.random(3);
                switch (r) {
                case 0:
                        walkTo(0, -1);
                        break;
                case 1:
                        walkTo(0, 1);
                        break;
                case 2:
                        walkTo(1, 0);
                        break;
                case 3:
                        walkTo(-1, 0);
                        break;
                }*/
        	walkClipped(c);
        } else if (c.isRunning2 && !withinDistance) {
                if (otherY > c.getY() && otherX == c.getX()) {
                        playerWalk(otherX, otherY - 1);
                } else if (otherY < c.getY() && otherX == c.getX()) {
                        playerWalk(otherX, otherY + 1);
                } else if (otherX > c.getX() && otherY == c.getY()) {
                        playerWalk(otherX - 1, otherY);
                } else if (otherX < c.getX() && otherY == c.getY()) {
                        playerWalk(otherX + 1, otherY);
                } else if (otherX < c.getX() && otherY < c.getY()) {
                        playerWalk(otherX + 1, otherY + 1);
                } else if (otherX > c.getX() && otherY > c.getY()) {
                        playerWalk(otherX - 1, otherY - 1);
                } else if (otherX < c.getX() && otherY > c.getY()) {
                        playerWalk(otherX + 1, otherY - 1);
                } else if (otherX > c.getX() && otherY < c.getY()) {
                        playerWalk(otherX + 1, otherY - 1);
                }
        } else {
                if (otherY > c.getY() && otherX == c.getX()) {
                        playerWalk(otherX, otherY - 1);
                } else if (otherY < c.getY() && otherX == c.getX()) {
                        playerWalk(otherX, otherY + 1);
                } else if (otherX > c.getX() && otherY == c.getY()) {
                        playerWalk(otherX - 1, otherY);
                } else if (otherX < c.getX() && otherY == c.getY()) {
                        playerWalk(otherX + 1, otherY);
                } else if (otherX < c.getX() && otherY < c.getY()) {
                        playerWalk(otherX + 1, otherY + 1);
                } else if (otherX > c.getX() && otherY > c.getY()) {
                        playerWalk(otherX - 1, otherY - 1);
                } else if (otherX < c.getX() && otherY > c.getY()) {
                        playerWalk(otherX + 1, otherY - 1);
                } else if (otherX > c.getX() && otherY < c.getY()) {
                        playerWalk(otherX - 1, otherY + 1);
                }
        }
        c.faceUpdate(c.followId+32768);
}
	
	private static void walkClipped(Client c) {
		if (Region.getClipping(c.getX() - 1, c.getY(), c.heightLevel, -1, 0)) {
			c.getPA().walkTo(-1, 0);
			return;
		} else if (Region.getClipping(c.getX() + 1, c.getY(), c.heightLevel, 1, 0)) {
			c.getPA().walkTo(1, 0);
			return;
		} else if (Region.getClipping(c.getX(), c.getY() - 1, c.heightLevel, 0, -1)) {
			c.getPA().walkTo(0, -1);
			return;
		} else if (Region.getClipping(c.getX(), c.getY() + 1, c.heightLevel, 0, 1)) {
			c.getPA().walkTo(0, 1);
			return;
		}
		c.getPA().walkTo(-1, 0);
	}

	/*public void followNpc() {
		if (NPCHandler.npcs[c.followId2] == null
				|| NPCHandler.npcs[c.followId2].isDead) {
			c.followId2 = 0;
			return;
		}
		if (c.freezeTimer > 0) {
			return;
		}
		if (c.isDead || c.playerLevel[3] <= 0)
			return;

		int otherX = NPCHandler.npcs[c.followId2].getX();
		int otherY = NPCHandler.npcs[c.followId2].getY();
		boolean withinDistance = c.goodDistance(otherX, otherY, c.getX(),
				c.getY(), 2);
		@SuppressWarnings("unused")
		boolean goodDistance = c.goodDistance(otherX, otherY, c.getX(),
				c.getY(), 1);
		boolean hallyDistance = c.goodDistance(otherX, otherY, c.getX(),
				c.getY(), 2);
		boolean bowDistance = c.goodDistance(otherX, otherY, c.getX(),
				c.getY(), 8);
		boolean rangeWeaponDistance = c.goodDistance(otherX, otherY, c.getX(),
				c.getY(), 4);
		boolean sameSpot = c.absX == otherX && c.absY == otherY;
		if (!c.goodDistance(otherX, otherY, c.getX(), c.getY(), 25)) {
			c.followId2 = 0;
			return;
		}
		if (c.goodDistance(otherX, otherY, c.getX(), c.getY(), 1)) {
			if (otherX != c.getX() && otherY != c.getY()) {
				stopDiagonal(otherX, otherY);
				return;
			}
		}

		if ((c.usingBow || c.mageFollow || (c.npcIndex > 0 && c.autocastId > 0))
				&& bowDistance && !sameSpot) {
			return;
		}

		if (c.getCombat().usingHally() && hallyDistance && !sameSpot) {
			return;
		}

		if (c.usingRangeWeapon && rangeWeaponDistance && !sameSpot) {
			return;
		}

		c.faceUpdate(c.followId2);
		if (otherX == c.absX && otherY == c.absY) {
			/*int r = Misc.random(3);
			switch (r) {
			case 0:
				walkTo(0, -1);
				break;
			case 1:
				walkTo(0, 1);
				break;
			case 2:
				walkTo(1, 0);
				break;
			case 3:
				walkTo(-1, 0);
				break;
			}*/
	      	/*walkClipped(c);
		} else if (c.isRunning2 && !withinDistance) {
			/*
			 * if(otherY > c.getY() && otherX == c.getX()) { walkTo(0,
			 * getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY - 1)); }
			 * else if(otherY < c.getY() && otherX == c.getX()) { walkTo(0,
			 * getMove(c.getY(), otherY + 1) + getMove(c.getY(), otherY + 1)); }
			 * else if(otherX > c.getX() && otherY == c.getY()) {
			 * walkTo(getMove(c.getX(), otherX - 1) + getMove(c.getX(), otherX -
			 * 1), 0); } else if(otherX < c.getX() && otherY == c.getY()) {
			 * walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX +
			 * 1), 0); } else if(otherX < c.getX() && otherY < c.getY()) {
			 * walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX +
			 * 1), getMove(c.getY(), otherY + 1) + getMove(c.getY(), otherY +
			 * 1)); } else if(otherX > c.getX() && otherY > c.getY()) {
			 * walkTo(getMove(c.getX(), otherX - 1) + getMove(c.getX(), otherX -
			 * 1), getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY -
			 * 1)); } else if(otherX < c.getX() && otherY > c.getY()) {
			 * walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX +
			 * 1), getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY -
			 * 1)); } else if(otherX > c.getX() && otherY < c.getY()) {
			 * walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX +
			 * 1), getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY -
			 * 1)); } } else { if(otherY > c.getY() && otherX == c.getX()) {
			 * walkTo(0, getMove(c.getY(), otherY - 1)); } else if(otherY <
			 * c.getY() && otherX == c.getX()) { walkTo(0, getMove(c.getY(),
			 * otherY + 1)); } else if(otherX > c.getX() && otherY == c.getY())
			 * { walkTo(getMove(c.getX(), otherX - 1), 0); } else if(otherX <
			 * c.getX() && otherY == c.getY()) { walkTo(getMove(c.getX(), otherX
			 * + 1), 0); } else if(otherX < c.getX() && otherY < c.getY()) {
			 * walkTo(getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY +
			 * 1)); } else if(otherX > c.getX() && otherY > c.getY()) {
			 * walkTo(getMove(c.getX(), otherX - 1), getMove(c.getY(), otherY -
			 * 1)); } else if(otherX < c.getX() && otherY > c.getY()) {
			 * walkTo(getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY -
			 * 1)); } else if(otherX > c.getX() && otherY < c.getY()) {
			 * walkTo(getMove(c.getX(), otherX - 1), getMove(c.getY(), otherY +
			 * 1)); }
			 */
			/*if (otherY > c.getY() && otherX == c.getX()) {
				// walkTo(0, getMove(c.getY(), otherY - 1) + getMove(c.getY(),
				// otherY - 1));
				playerWalk(otherX, otherY - 1);
			} else if (otherY < c.getY() && otherX == c.getX()) {
				// walkTo(0, getMove(c.getY(), otherY + 1) + getMove(c.getY(),
				// otherY + 1));
				playerWalk(otherX, otherY + 1);
			} else if (otherX > c.getX() && otherY == c.getY()) {
				// walkTo(getMove(c.getX(), otherX - 1) + getMove(c.getX(),
				// otherX - 1), 0);
				playerWalk(otherX - 1, otherY);
			} else if (otherX < c.getX() && otherY == c.getY()) {
				// walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(),
				// otherX + 1), 0);
				playerWalk(otherX + 1, otherY);
			} else if (otherX < c.getX() && otherY < c.getY()) {
				// walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(),
				// otherX + 1), getMove(c.getY(), otherY + 1) +
				// getMove(c.getY(), otherY + 1));
				playerWalk(otherX + 1, otherY + 1);
			} else if (otherX > c.getX() && otherY > c.getY()) {
				// walkTo(getMove(c.getX(), otherX - 1) + getMove(c.getX(),
				// otherX - 1), getMove(c.getY(), otherY - 1) +
				// getMove(c.getY(), otherY - 1));
				playerWalk(otherX - 1, otherY - 1);
			} else if (otherX < c.getX() && otherY > c.getY()) {
				// walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(),
				// otherX + 1), getMove(c.getY(), otherY - 1) +
				// getMove(c.getY(), otherY - 1));
				playerWalk(otherX + 1, otherY - 1);
			} else if (otherX > c.getX() && otherY < c.getY()) {
				// walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(),
				// otherX + 1), getMove(c.getY(), otherY - 1) +
				// getMove(c.getY(), otherY - 1));
				playerWalk(otherX + 1, otherY - 1);
			}
		} else {
			if (otherY > c.getY() && otherX == c.getX()) {
				// walkTo(0, getMove(c.getY(), otherY - 1));
				playerWalk(otherX, otherY - 1);
			} else if (otherY < c.getY() && otherX == c.getX()) {
				// walkTo(0, getMove(c.getY(), otherY + 1));
				playerWalk(otherX, otherY + 1);
			} else if (otherX > c.getX() && otherY == c.getY()) {
				// walkTo(getMove(c.getX(), otherX - 1), 0);
				playerWalk(otherX - 1, otherY);
			} else if (otherX < c.getX() && otherY == c.getY()) {
				// walkTo(getMove(c.getX(), otherX + 1), 0);
				playerWalk(otherX + 1, otherY);
			} else if (otherX < c.getX() && otherY < c.getY()) {
				// walkTo(getMove(c.getX(), otherX + 1), getMove(c.getY(),
				// otherY + 1));
				playerWalk(otherX + 1, otherY + 1);
			} else if (otherX > c.getX() && otherY > c.getY()) {
				// walkTo(getMove(c.getX(), otherX - 1), getMove(c.getY(),
				// otherY - 1));
				playerWalk(otherX - 1, otherY - 1);
			} else if (otherX < c.getX() && otherY > c.getY()) {
				// walkTo(getMove(c.getX(), otherX + 1), getMove(c.getY(),
				// otherY - 1));
				playerWalk(otherX + 1, otherY - 1);
			} else if (otherX > c.getX() && otherY < c.getY()) {
				// walkTo(getMove(c.getX(), otherX - 1), getMove(c.getY(),
				// otherY + 1));
				playerWalk(otherX - 1, otherY + 1);
			}
		}
		c.faceUpdate(c.followId2);
	}*/
	
	public void followNpc() {
		if(NPCHandler.npcs[c.followId2] == null || NPCHandler.npcs[c.followId2].isDead) {
			c.followId2 = 0;
			return;
		}
		if(c.freezeTimer > 0) {
			return;
		}
		if (c.isDead || c.playerLevel[3] <= 0)
			return;
		int otherX = NPCHandler.npcs[c.followId2].getX(); //npcs[i].otherx and change when npc walks.
		int otherY = NPCHandler.npcs[c.followId2].getY();
		boolean withinDistance = c.goodDistance(otherX, otherY, c.getX(), c.getY(), 2);
		@SuppressWarnings("unused")
		boolean goodDistance = c.goodDistance(otherX, otherY, c.getX(), c.getY(), 1);
		boolean hallyDistance = c.goodDistance(otherX, otherY, c.getX(), c.getY(), 2);
		boolean bowDistance = c.goodDistance(otherX, otherY, c.getX(), c.getY(), 8);
		boolean rangeWeaponDistance = c.goodDistance(otherX, otherY, c.getX(), c.getY(), 4);
		boolean sameSpot = c.absX == otherX && c.absY == otherY;
		if(!c.goodDistance(otherX, otherY, c.getX(), c.getY(), 25)) {
			return;
		}
		/*if(c.goodDistance(otherX, otherY, c.getX(), c.getY(), 1)) {
			if (otherX != c.getX() && otherY != c.getY()) {
				stopDiagonal(otherX, otherY);
				return;
			} else {
				c.followId2 = 0;
				return;
			}
		}*/
		
		if((c.usingBow || c.mageFollow || (c.npcIndex > 0 && c.autocastId > 0)) && bowDistance && !sameSpot) {
			return;
		}

		if(c.getCombat().usingHally() && hallyDistance && !sameSpot) {
			return;
		}

		if(c.usingRangeWeapon && rangeWeaponDistance && !sameSpot) {
			return;
		}
		c.faceUpdate(c.followId2);
		if (otherX == c.absX && otherY == c.absY) {
		/*	int r = Misc.random(3);
			switch (r) {
				case 0:
					walkTo(0,-1);
				break;
				case 1:
					walkTo(0,1);
				break;
				case 2:
					walkTo(1,0);
				break;
				case 3:
					walkTo(-1,0);
				break;			
			}	*/
			walkClipped(c);
		} else if(c.isRunning2 && !withinDistance) {
			/*if(otherY > c.getY() && otherX == c.getX()) {
				walkTo(0, getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY - 1));
			} else if(otherY < c.getY() && otherX == c.getX()) {
				walkTo(0, getMove(c.getY(), otherY + 1) + getMove(c.getY(), otherY + 1));
			} else if(otherX > c.getX() && otherY == c.getY()) {
				walkTo(getMove(c.getX(), otherX - 1) + getMove(c.getX(), otherX - 1), 0);
			} else if(otherX < c.getX() && otherY == c.getY()) {
				walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX + 1), 0);
			} else if(otherX < c.getX() && otherY < c.getY()) {
				walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY + 1) + getMove(c.getY(), otherY + 1));
			} else if(otherX > c.getX() && otherY > c.getY()) {
				walkTo(getMove(c.getX(), otherX - 1) + getMove(c.getX(), otherX - 1), getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY - 1));
			} else if(otherX < c.getX() && otherY > c.getY()) {
				walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY - 1));
			} else if(otherX > c.getX() && otherY < c.getY()) {
				walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY - 1));
			} 
		} else {
			if(otherY > c.getY() && otherX == c.getX()) {
				walkTo(0, getMove(c.getY(), otherY - 1));
			} else if(otherY < c.getY() && otherX == c.getX()) {
				walkTo(0, getMove(c.getY(), otherY + 1));
			} else if(otherX > c.getX() && otherY == c.getY()) {
				walkTo(getMove(c.getX(), otherX - 1), 0);
			} else if(otherX < c.getX() && otherY == c.getY()) {
				walkTo(getMove(c.getX(), otherX + 1), 0);
			} else if(otherX < c.getX() && otherY < c.getY()) {
				walkTo(getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY + 1));
			} else if(otherX > c.getX() && otherY > c.getY()) {
				walkTo(getMove(c.getX(), otherX - 1), getMove(c.getY(), otherY - 1));
			} else if(otherX < c.getX() && otherY > c.getY()) {
				walkTo(getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY - 1));
			} else if(otherX > c.getX() && otherY < c.getY()) {
				walkTo(getMove(c.getX(), otherX - 1), getMove(c.getY(), otherY + 1));
			}*/
			if(otherY > c.getY() && otherX == c.getX()) {
				//walkTo(0, getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY - 1));
				playerWalk(otherX, otherY - 1);
			} else if(otherY < c.getY() && otherX == c.getX()) {
				//walkTo(0, getMove(c.getY(), otherY + 1) + getMove(c.getY(), otherY + 1));
				playerWalk(otherX, otherY + 1);
			} else if(otherX > c.getX() && otherY == c.getY()) {
				//walkTo(getMove(c.getX(), otherX - 1) + getMove(c.getX(), otherX - 1), 0);
				playerWalk(otherX - 1, otherY);
			} else if(otherX < c.getX() && otherY == c.getY()) {
				//walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX + 1), 0);
				playerWalk(otherX + 1, otherY);
			} else if(otherX < c.getX() && otherY < c.getY()) {
				//walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY + 1) + getMove(c.getY(), otherY + 1));
				playerWalk(otherX + 1, otherY + 1);
			} else if(otherX > c.getX() && otherY > c.getY()) {
				//walkTo(getMove(c.getX(), otherX - 1) + getMove(c.getX(), otherX - 1), getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY - 1));
				playerWalk(otherX - 1, otherY - 1);
			} else if(otherX < c.getX() && otherY > c.getY()) {
				//walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY - 1));
				playerWalk(otherX + 1, otherY - 1);
			} else if(otherX > c.getX() && otherY < c.getY()) {
				//walkTo(getMove(c.getX(), otherX + 1) + getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY - 1) + getMove(c.getY(), otherY - 1));
				playerWalk(otherX + 1, otherY - 1);
			}
		} else {
			if(otherY > c.getY() && otherX == c.getX()) {
				//walkTo(0, getMove(c.getY(), otherY - 1));
				playerWalk(otherX, otherY - 1);
			} else if(otherY < c.getY() && otherX == c.getX()) {
				//walkTo(0, getMove(c.getY(), otherY + 1));
				playerWalk(otherX, otherY + 1);
			} else if(otherX > c.getX() && otherY == c.getY()) {
				//walkTo(getMove(c.getX(), otherX - 1), 0);
				playerWalk(otherX - 1, otherY);
			} else if(otherX < c.getX() && otherY == c.getY()) {
				//walkTo(getMove(c.getX(), otherX + 1), 0);
				playerWalk(otherX + 1, otherY);
			} else if(otherX < c.getX() && otherY < c.getY()) {
				//walkTo(getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY + 1));
				playerWalk(otherX + 1, otherY + 1);
			} else if(otherX > c.getX() && otherY > c.getY()) {
				//walkTo(getMove(c.getX(), otherX - 1), getMove(c.getY(), otherY - 1));
				playerWalk(otherX - 1, otherY - 1);
			} else if(otherX < c.getX() && otherY > c.getY()) {
				//walkTo(getMove(c.getX(), otherX + 1), getMove(c.getY(), otherY - 1));
				playerWalk(otherX + 1, otherY - 1);
			} else if(otherX > c.getX() && otherY < c.getY()) {
				//walkTo(getMove(c.getX(), otherX - 1), getMove(c.getY(), otherY + 1));
				playerWalk(otherX - 1, otherY + 1);
			}
		}
		c.faceUpdate(c.followId2);
	}

	public int getRunningMove(int i, int j) {
		if (j - i > 2)
			return 2;
		else if (j - i < -2)
			return -2;
		else
			return j - i;
	}

	public void resetFollow() {
		c.followId = 0;
		c.followId2 = 0;
		c.mageFollow = false;
	}

	public void walkTo(int i, int j) {
		c.newWalkCmdSteps = 0;
		if (++c.newWalkCmdSteps > 50)
			c.newWalkCmdSteps = 0;
		int k = c.getX() + i;
		k -= c.mapRegionX * 8;
		c.getNewWalkCmdX()[0] = c.getNewWalkCmdY()[0] = 0;
		int l = c.getY() + j;
		l -= c.mapRegionY * 8;

		for (int n = 0; n < c.newWalkCmdSteps; n++) {
			c.getNewWalkCmdX()[n] += k;
			c.getNewWalkCmdY()[n] += l;
		}
	}

	public void walkTo2(int i, int j) {
		if (c.freezeDelay > 0)
			return;
		c.newWalkCmdSteps = 0;
		if (++c.newWalkCmdSteps > 50)
			c.newWalkCmdSteps = 0;
		int k = c.getX() + i;
		k -= c.mapRegionX * 8;
		c.getNewWalkCmdX()[0] = c.getNewWalkCmdY()[0] = 0;
		int l = c.getY() + j;
		l -= c.mapRegionY * 8;

		for (int n = 0; n < c.newWalkCmdSteps; n++) {
			c.getNewWalkCmdX()[n] += k;
			c.getNewWalkCmdY()[n] += l;
		}
	}

	public void walkTo3(int i, int j) {
		c.newWalkCmdSteps = 0;
		if (++c.newWalkCmdSteps > 50)
			c.newWalkCmdSteps = 0;
		int k = c.absX + i;
		k -= c.mapRegionX * 8;
		c.getNewWalkCmdX()[0] = c.getNewWalkCmdY()[0] = tmpNWCX[0] = tmpNWCY[0] = 0;
		int l = c.absY + j;
		l -= c.mapRegionY * 8;
		c.isRunning2 = false;
		c.isRunning = false;
		c.getNewWalkCmdX()[0] += k;
		c.getNewWalkCmdY()[0] += l;
		c.poimiY = l;
		c.poimiX = k;
	}
	
	/*if (Region.getClipping(c.getX() - 1, c.getY(), c.heightLevel, -1, 0)) {
		c.getPA().walkTo(-1, 0);
		return;
	} else if (Region.getClipping(c.getX() + 1, c.getY(), c.heightLevel, 1, 0)) {
		c.getPA().walkTo(1, 0);
		return;
	} else if (Region.getClipping(c.getX(), c.getY() - 1, c.heightLevel, 0, -1)) {
		c.getPA().walkTo(0, -1);
		return;
	} else if (Region.getClipping(c.getX(), c.getY() + 1, c.heightLevel, 0, 1)) {
		c.getPA().walkTo(0, 1);
		return;
	}
	c.getPA().walkTo(-1, 0);*/

	public void stopDiagonal(int otherX, int otherY) {
		if (c.freezeDelay > 0)
			return;
		c.newWalkCmdSteps = 1;
		int xMove = otherX - c.getX();
		int yMove = 0;
		if (xMove == 0)
			yMove = otherY - c.getY();

		int k = c.getX() + xMove;
		k -= c.mapRegionX * 8;
		c.getNewWalkCmdX()[0] = c.getNewWalkCmdY()[0] = 0;
		int l = c.getY() + yMove;
		l -= c.mapRegionY * 8;

		for (int n = 0; n < c.newWalkCmdSteps; n++) {
			c.getNewWalkCmdX()[n] += k;
			c.getNewWalkCmdY()[n] += l;
		}
		c.getPA().walkTo(-1, 0);

	}

	public void walkToCheck(int i, int j) {
		if (c.freezeDelay > 0)
			return;
		c.newWalkCmdSteps = 0;
		if (++c.newWalkCmdSteps > 50)
			c.newWalkCmdSteps = 0;
		int k = c.getX() + i;
		k -= c.mapRegionX * 8;
		c.getNewWalkCmdX()[0] = c.getNewWalkCmdY()[0] = 0;
		int l = c.getY() + j;
		l -= c.mapRegionY * 8;

		for (int n = 0; n < c.newWalkCmdSteps; n++) {
			c.getNewWalkCmdX()[n] += k;
			c.getNewWalkCmdY()[n] += l;
		}
	}

	public int getMove(int place1, int place2) {
		if (System.currentTimeMillis() - c.lastSpear < 4000)
			return 0;
		if ((place1 - place2) == 0) {
			return 0;
		} else if ((place1 - place2) < 0) {
			return 1;
		} else if ((place1 - place2) > 0) {
			return -1;
		}
		return 0;
	}

	public boolean fullVeracs() {
		return c.playerEquipment[c.playerHat] == 4753
				&& c.playerEquipment[c.playerChest] == 4757
				&& c.playerEquipment[c.playerLegs] == 4759
				&& c.playerEquipment[c.playerWeapon] == 4755;
	}

	public boolean fullGuthans() {
		return c.playerEquipment[c.playerHat] == 4724
				&& c.playerEquipment[c.playerChest] == 4728
				&& c.playerEquipment[c.playerLegs] == 4730
				&& c.playerEquipment[c.playerWeapon] == 4726;
	}

	/**
	 * reseting animation
	 **/
	public void resetAnimation() {
		c.getCombat().getPlayerAnimIndex(
				ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon])
						.toLowerCase());
		c.startAnimation(c.playerStandIndex);
		requestUpdates();
	}

	public void requestUpdates() {
		c.updateRequired = true;
		c.setAppearanceUpdateRequired(true);
	}

	public void handleAlt(int id) {
		if (!c.getItems().playerHasItem(id)) {
			c.getItems().addItem(id, 1);
		}
	}

	public void levelUp(int skill) {
		sendFrame126(""+totalLevel(), 22002);
		switch(skill) {
			case 0:
			sendFrame126("Congratulations! You've just advanced a Attack level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+"!", 4269);
			c.sendMessage("Congratulations! You've just advanced a attack level.");	
			sendFrame164(6247);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Attack! Congratulations!");
					}
				}
			}
			break;
			
			case 1:
            		sendFrame126("Congratulations! You've just advanced a Defence level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Defence level.");
			sendFrame164(6253);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Defence! Congratulations!");
					}
				}
			}
			break;
			
			case 2:
            		sendFrame126("Congratulations! You've just advanced a Strength level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Strength level.");
			sendFrame164(6206);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Strength! Congratulations!");
					}
				}
			}
			break;
			
			case 3:
            		sendFrame126("Congratulations! You've just advanced a Hitpoints level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Hitpoints level.");
			sendFrame164(6216);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Hitpoints! Congratulations!");
					}
				}
			}
			break;
			
			case 4:
            		sendFrame126("Congratulations! You've just advanced a Ranged level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Ranging level.");
			sendFrame164(4443);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Ranging! Congratulations!");
					}
				}
			}
			break;
			
			case 5:
            		sendFrame126("Congratulations! You've just advanced a Prayer level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Prayer level.");
			sendFrame164(6242);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Prayer! Congratulations!");
					}
				}
			}
			break;
			
			case 6:
            		sendFrame126("Congratulations! You've just advanced a Magic level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Magic level.");
			sendFrame164(6211);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Magic! Congratulations!");
					}
				}
			}
			break;
			
			case 7:
            		sendFrame126("Congratulations! You've just advanced a Cooking level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Cooking level.");
			sendFrame164(6226);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Cooking! Congratulations!");
					}
				}
			}
			break;
			
			case 8:
			sendFrame126("Congratulations! You've just advanced a Woodcutting level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Woodcutting level.");
			sendFrame164(4272);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Woodcutting! Congratulations!");
					}
				}
			}
            		break;
			
            		case 9:
            		sendFrame126("Congratulations! You've just advanced a Fletching level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Fletching level.");
			sendFrame164(6231);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Fletching! Congratulations!");
					}
				}
			}
           		break;
			
			case 10:
            		sendFrame126("Congratulations! You've just advanced a Fishing level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Fishing level.");
			sendFrame164(6258);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Fishing! Congratulations!");
					}
				}
			}
			break;
			
			case 11:
			sendFrame126("Congratulations! You've just advanced a Fire making level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Fire making level.");
			sendFrame164(4282);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Firemaking! Congratulations!");
					}
				}
			}
            		break;
			
            		case 12:
			sendFrame126("Congratulations! You've just advanced a Crafting level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Crafting level.");
			sendFrame164(6263);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Crafting! Congratulations!");
					}
				}
			}
            		break;
			
			case 13:
			sendFrame126("Congratulations! You've just advanced a Smithing level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Smithing level.");
			sendFrame164(6221);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Smithing! Congratulations!");
					}
				}
			}
			break;
			
			case 14:
			sendFrame126("Congratulations! You've just advanced a Mining level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Mining level.");
			sendFrame164(4416);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Mining! Congratulations!");
					}
				}
			}
            		break;
			
			case 15:
            		sendFrame126("Congratulations! You've just advanced a Herblore level!", 4268);
            		sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Herblore level.");
			sendFrame164(6237);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Herblore! Congratulations!");
					}
				}
			}
            		break;
			
			case 16:
			sendFrame126("Congratulations! You've just advanced a Agility level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Agility level.");
			sendFrame164(4277);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Agility! Congratulations!");
					}
				}
			}
           		break;
			
			case 17:
			sendFrame126("Congratulations! You've just advanced a Thieving level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
            		c.sendMessage("Congratulations! You've just advanced a Thieving level.");
			sendFrame164(4261);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Thieving! Congratulations!");
					}
				}
			}
			break;
			
			case 18:
			sendFrame126("Congratulations! You've just advanced a Slayer level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Slayer level.");
			sendFrame164(12122);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Slayer! Congratulations!");
					}
				}
			}
            		break;

            		case 19:
			sendFrame126("Congratulations! You've just advanced a Farming level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Farming level.");
			sendFrame164(5267);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Farming! Congratulations!");
					}
				}
			}
           	 	break;
            
            		case 20:
			sendFrame126("Congratulations! You've just advanced a Runecrafting level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Runecrafting level.");
			sendFrame164(4267);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Runecrafting! Congratulations!");
					}
				}
			}
           	 	break;
            		
			case 21:
			sendFrame126("Congratulations! You've just advanced a Construction level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Construction level.");
			sendFrame164(7267);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Construction! Congratulations!");
					}
				}
			}
            		break;
            
			case 22:
			sendFrame126("Congratulations! You've just advanced a Hunter level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Hunter level.");
			sendFrame164(8267);
			if(getLevelForXP(c.playerXP[skill]) == 99) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[99] @dre@"+c.playerName+" just advanced to 99 in Hunter! Congratulations!");
					}
				}
			}
            		break;

            		case 23:
			sendFrame126("Congratulations! You've just advanced a Summoning level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Summoning level.");
			sendFrame164(9267);
            		break;

            		case 24:
			sendFrame126("Congratulations! You've just advanced a Dungeoneering level!", 4268);
			sendFrame126("You have now reached level "+getLevelForXP(c.playerXP[skill])+".", 4269);
			c.sendMessage("Congratulations! You've just advanced a Dungeoneering level.");
			sendFrame164(10267);
            	break;
		}
		if(totalLevel() >= 2277) {
			for (int j = 0; j < PlayerHandler.players.length; j++) {
				if (PlayerHandler.players[j] != null) {
					Client c2 = (Client)PlayerHandler.players[j];
					c2.sendMessage("@cr7@@red@[Mastered] @dre@"+c.playerName+" just mastered all the skills! Congratulations!");
					c.getItems().addItemToBank(9813, 1);
					c.getItems().addItemToBank(9814, 1);
					c.sendMessage("A quest cape and hood has been added to your bank.");
					if(c.playerRights == 0) {
						c.playerRights = 8;
						c.veteran = 1;
						c.sendMessage("Please re-login for your new Veteran rank.");
					}
				}
			}
		}
		c.dialogueAction = 0;
		c.nextChat = 0;
        sendFrame126("Click here to continue", 358);
	}

	/*
	 * public void levelUp(int skill) { int totalLevel =
	 * (getLevelForXP(c.playerXP[0]) + getLevelForXP(c.playerXP[1]) +
	 * getLevelForXP(c.playerXP[2]) + getLevelForXP(c.playerXP[3]) +
	 * getLevelForXP(c.playerXP[4]) + getLevelForXP(c.playerXP[5]) +
	 * getLevelForXP(c.playerXP[6]) + getLevelForXP(c.playerXP[7]) +
	 * getLevelForXP(c.playerXP[8]) + getLevelForXP(c.playerXP[9]) +
	 * getLevelForXP(c.playerXP[10]) + getLevelForXP(c.playerXP[11]) +
	 * getLevelForXP(c.playerXP[12]) + getLevelForXP(c.playerXP[13]) +
	 * getLevelForXP(c.playerXP[14]) + getLevelForXP(c.playerXP[15]) +
	 * getLevelForXP(c.playerXP[16]) + getLevelForXP(c.playerXP[17]) +
	 * getLevelForXP(c.playerXP[18]) + getLevelForXP(c.playerXP[19]) +
	 * getLevelForXP(c.playerXP[20])); sendFrame126("Total Lvl: "+totalLevel,
	 * 3984); switch(skill) { case 0:
	 * sendFrame126("Congratulations, you just advanced an attack level!",
	 * 6248);
	 * sendFrame126("Your attack level is now "+getLevelForXP(c.playerXP[skill
	 * ])+".", 6249);
	 * c.sendMessage("Congratulations, you just advanced an attack level.");
	 * sendFrame164(6247); for (int HUH = 0; HUH < 25; HUH++) {
	 * if(getLevelForXP(c.playerXP[0]) == 99) { TutorialIsland.ATTACK(c, HUH);
	 * return; } } break;
	 * 
	 * case 1:
	 * sendFrame126("Congratulations, you just advanced a defence level!",
	 * 6254);
	 * sendFrame126("Your defence level is now "+getLevelForXP(c.playerXP[
	 * skill])+".", 6255);
	 * c.sendMessage("Congratulations, you just advanced a defence level.");
	 * sendFrame164(6253); for (int HUH = 0; HUH < 25; HUH++) {
	 * if(getLevelForXP(c.playerXP[1]) == 99) { TutorialIsland.DEFENCE(c, HUH);
	 * return; } } break;
	 * 
	 * case 2:
	 * sendFrame126("Congratulations, you just advanced a strength level!",
	 * 6207);
	 * sendFrame126("Your strength level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 6208);
	 * c.sendMessage("Congratulations, you just advanced a strength level.");
	 * sendFrame164(6206); for (int HUH = 0; HUH < 25; HUH++) {
	 * if(getLevelForXP(c.playerXP[2]) == 99) { TutorialIsland.STRENGTH(c, HUH);
	 * return; } } break;
	 * 
	 * case 3:
	 * sendFrame126("Congratulations, you just advanced a hitpoints level!",
	 * 6217);
	 * sendFrame126("Your hitpoints level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 6218);
	 * c.sendMessage("Congratulations, you just advanced a hitpoints level.");
	 * sendFrame164(6216); //hitpoints for (int HUH = 0; HUH < 25; HUH++) {
	 * if(getLevelForXP(c.playerXP[3]) == 99) { TutorialIsland.HITPOINTS(c,
	 * HUH); return; } } break;
	 * 
	 * case 4:
	 * sendFrame126("Congratulations, you just advanced a ranged level!", 5453);
	 * sendFrame126
	 * ("Your ranged level is now "+getLevelForXP(c.playerXP[skill])+".", 6114);
	 * c.sendMessage("Congratulations, you just advanced a ranging level.");
	 * sendFrame164(4443); for (int HUH = 0; HUH < 25; HUH++) {
	 * if(getLevelForXP(c.playerXP[4]) == 99) { TutorialIsland.RANGING(c, HUH);
	 * return; } } break;
	 * 
	 * case 5:
	 * sendFrame126("Congratulations, you just advanced a prayer level!", 6243);
	 * sendFrame126
	 * ("Your prayer level is now "+getLevelForXP(c.playerXP[skill])+".", 6244);
	 * c.sendMessage("Congratulations, you just advanced a prayer level.");
	 * sendFrame164(6242); for (int HUH = 0; HUH < 25; HUH++) {
	 * if(getLevelForXP(c.playerXP[5]) == 99) { TutorialIsland.PRAYER(c, HUH);
	 * return; } } break;
	 * 
	 * case 6: sendFrame126("Congratulations, you just advanced a magic level!",
	 * 6212);
	 * sendFrame126("Your magic level is now "+getLevelForXP(c.playerXP[skill
	 * ])+".", 6213);
	 * c.sendMessage("Congratulations, you just advanced a magic level.");
	 * sendFrame164(6211); for (int HUH = 0; HUH < 25; HUH++) {
	 * if(getLevelForXP(c.playerXP[6]) == 99) { TutorialIsland.MAGIC(c, HUH);
	 * return; } } break;
	 * 
	 * case 7:
	 * sendFrame126("Congratulations, you just advanced a cooking level!",
	 * 6227);
	 * sendFrame126("Your cooking level is now "+getLevelForXP(c.playerXP[
	 * skill])+".", 6228);
	 * c.sendMessage("Congratulations, you just advanced a cooking level.");
	 * sendFrame164(6226); for (int HUH = 0; HUH < 25; HUH++) {
	 * if(getLevelForXP(c.playerXP[7]) == 99) { TutorialIsland.COOKING(c, HUH);
	 * return; } } break;
	 * 
	 * case 8:
	 * sendFrame126("Congratulations, you just advanced a woodcutting level!",
	 * 4273);
	 * sendFrame126("Your woodcutting level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 4274);
	 * c.sendMessage("Congratulations, you just advanced a woodcutting level.");
	 * sendFrame164(4272); for (int HUH = 0; HUH < 25; HUH++) {
	 * if(getLevelForXP(c.playerXP[8]) == 99) { TutorialIsland.WOODCUTTING(c,
	 * HUH); return; } } break;
	 * 
	 * case 9:
	 * sendFrame126("Congratulations, you just advanced a fletching level!",
	 * 6232);
	 * sendFrame126("Your fletching level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 6233);
	 * c.sendMessage("Congratulations, you just advanced a fletching level.");
	 * sendFrame164(6231); break;
	 * 
	 * case 10:
	 * sendFrame126("Congratulations, you just advanced a fishing level!",
	 * 6259);
	 * sendFrame126("Your fishing level is now "+getLevelForXP(c.playerXP[
	 * skill])+".", 6260);
	 * c.sendMessage("Congratulations, you just advanced a fishing level.");
	 * sendFrame164(6258); break;
	 * 
	 * case 11:
	 * sendFrame126("Congratulations, you just advanced a fire making level!",
	 * 4283);
	 * sendFrame126("Your firemaking level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 4284);
	 * c.sendMessage("Congratulations, you just advanced a fire making level.");
	 * sendFrame164(4282); break;
	 * 
	 * case 12:
	 * sendFrame126("Congratulations, you just advanced a crafting level!",
	 * 6264);
	 * sendFrame126("Your crafting level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 6265);
	 * c.sendMessage("Congratulations, you just advanced a crafting level.");
	 * sendFrame164(6263); break;
	 * 
	 * case 13:
	 * sendFrame126("Congratulations, you just advanced a smithing level!",
	 * 6222);
	 * sendFrame126("Your smithing level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 6223);
	 * c.sendMessage("Congratulations, you just advanced a smithing level.");
	 * sendFrame164(6221); break;
	 * 
	 * case 14:
	 * sendFrame126("Congratulations, you just advanced a mining level!", 4417);
	 * sendFrame126
	 * ("Your mining level is now "+getLevelForXP(c.playerXP[skill])+".", 4438);
	 * c.sendMessage("Congratulations, you just advanced a mining level.");
	 * sendFrame164(4416); break;
	 * 
	 * case 15:
	 * sendFrame126("Congratulations, you just advanced a herblore level!",
	 * 6238);
	 * sendFrame126("Your herblore level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 6239);
	 * c.sendMessage("Congratulations, you just advanced a herblore level.");
	 * sendFrame164(6237); break;
	 * 
	 * case 16:
	 * sendFrame126("Congratulations, you just advanced a agility level!",
	 * 4278);
	 * sendFrame126("Your agility level is now "+getLevelForXP(c.playerXP[
	 * skill])+".", 4279);
	 * c.sendMessage("Congratulations, you just advanced an agility level.");
	 * sendFrame164(4277); break;
	 * 
	 * case 17:
	 * sendFrame126("Congratulations, you just advanced a thieving level!",
	 * 4263);
	 * sendFrame126("Your thieving level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 4264);
	 * c.sendMessage("Congratulations, you just advanced a thieving level.");
	 * sendFrame164(4261); break;
	 * 
	 * case 18:
	 * sendFrame126("Congratulations, you just advanced a slayer level!",
	 * 12123);
	 * sendFrame126("Your slayer level is now "+getLevelForXP(c.playerXP[
	 * skill])+".", 12124);
	 * c.sendMessage("Congratulations, you just advanced a slayer level.");
	 * sendFrame164(12122); break;
	 * 
	 * case 20:
	 * sendFrame126("Congratulations, you just advanced a runecrafting level!",
	 * 4268);
	 * sendFrame126("Your runecrafting level is now "+getLevelForXP(c.playerXP
	 * [skill])+".", 4269);
	 * c.sendMessage("Congratulations, you just advanced a runecrafting level."
	 * ); sendFrame164(4267); break; } c.dialogueAction = 0; c.nextChat = 0; }
	 */

	public void refreshSkill(int i) {
		sendFrame126(""+totalLevel(), 22002);
		switch (i) {
		case 0:
			sendFrame126("" + c.playerLevel[0] + "", 4004);
			sendFrame126("" + getLevelForXP(c.playerXP[0]) + "", 4005);
			sendFrame126("" + c.playerXP[0] + "", 4044);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[0]) + 1)
					+ "", 4045);
			break;

		case 1:
			sendFrame126("" + c.playerLevel[1] + "", 4008);
			sendFrame126("" + getLevelForXP(c.playerXP[1]) + "", 4009);
			sendFrame126("" + c.playerXP[1] + "", 4056);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[1]) + 1)
					+ "", 4057);
			break;

		case 2:
			sendFrame126("" + c.playerLevel[2] + "", 4006);
			sendFrame126("" + getLevelForXP(c.playerXP[2]) + "", 4007);
			sendFrame126("" + c.playerXP[2] + "", 4050);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[2]) + 1)
					+ "", 4051);
			break;

		case 3:
			sendFrame126("" + c.playerLevel[3] + "", 4016);
			sendFrame126("" + getLevelForXP(c.playerXP[3]) + "", 4017);
			sendFrame126("" + c.playerXP[3] + "", 4080);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[3]) + 1)
					+ "", 4081);
			break;

		case 4:
			sendFrame126("" + c.playerLevel[4] + "", 4010);
			sendFrame126("" + getLevelForXP(c.playerXP[4]) + "", 4011);
			sendFrame126("" + c.playerXP[4] + "", 4062);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[4]) + 1)
					+ "", 4063);
			break;

		case 5:
			sendFrame126("" + c.playerLevel[5] + "", 4012);
			sendFrame126("" + getLevelForXP(c.playerXP[5]) + "", 4013);
			sendFrame126("" + c.playerXP[5] + "", 4068);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[5]) + 1)
					+ "", 4069);
			sendFrame126("" + c.playerLevel[5] + "/"
					+ getLevelForXP(c.playerXP[5]) + "", 687);// Prayer frame
			break;

		case 6:
			sendFrame126("" + c.playerLevel[6] + "", 4014);
			sendFrame126("" + getLevelForXP(c.playerXP[6]) + "", 4015);
			sendFrame126("" + c.playerXP[6] + "", 4074);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[6]) + 1)
					+ "", 4075);
			break;

		case 7:
			sendFrame126("" + c.playerLevel[7] + "", 4034);
			sendFrame126("" + getLevelForXP(c.playerXP[7]) + "", 4035);
			sendFrame126("" + c.playerXP[7] + "", 4134);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[7]) + 1)
					+ "", 4135);
			break;

		case 8:
			sendFrame126("" + c.playerLevel[8] + "", 4038);
			sendFrame126("" + getLevelForXP(c.playerXP[8]) + "", 4039);
			sendFrame126("" + c.playerXP[8] + "", 4146);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[8]) + 1)
					+ "", 4147);
			break;

		case 9:
			sendFrame126("" + c.playerLevel[9] + "", 4026);
			sendFrame126("" + getLevelForXP(c.playerXP[9]) + "", 4027);
			sendFrame126("" + c.playerXP[9] + "", 4110);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[9]) + 1)
					+ "", 4111);
			break;

		case 10:
			sendFrame126("" + c.playerLevel[10] + "", 4032);
			sendFrame126("" + getLevelForXP(c.playerXP[10]) + "", 4033);
			sendFrame126("" + c.playerXP[10] + "", 4128);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[10]) + 1)
					+ "", 4129);
			break;

		case 11:
			sendFrame126("" + c.playerLevel[11] + "", 4036);
			sendFrame126("" + getLevelForXP(c.playerXP[11]) + "", 4037);
			sendFrame126("" + c.playerXP[11] + "", 4140);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[11]) + 1)
					+ "", 4141);
			break;

		case 12:
			sendFrame126("" + c.playerLevel[12] + "", 4024);
			sendFrame126("" + getLevelForXP(c.playerXP[12]) + "", 4025);
			sendFrame126("" + c.playerXP[12] + "", 4104);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[12]) + 1)
					+ "", 4105);
			break;

		case 13:
			sendFrame126("" + c.playerLevel[13] + "", 4030);
			sendFrame126("" + getLevelForXP(c.playerXP[13]) + "", 4031);
			sendFrame126("" + c.playerXP[13] + "", 4122);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[13]) + 1)
					+ "", 4123);
			break;

		case 14:
			sendFrame126("" + c.playerLevel[14] + "", 4028);
			sendFrame126("" + getLevelForXP(c.playerXP[14]) + "", 4029);
			sendFrame126("" + c.playerXP[14] + "", 4116);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[14]) + 1)
					+ "", 4117);
			break;

		case 15:
			sendFrame126("" + c.playerLevel[15] + "", 4020);
			sendFrame126("" + getLevelForXP(c.playerXP[15]) + "", 4021);
			sendFrame126("" + c.playerXP[15] + "", 4092);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[15]) + 1)
					+ "", 4093);
			break;

		case 16:
			sendFrame126("" + c.playerLevel[16] + "", 4018);
			sendFrame126("" + getLevelForXP(c.playerXP[16]) + "", 4019);
			sendFrame126("" + c.playerXP[16] + "", 4086);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[16]) + 1)
					+ "", 4087);
			break;

		case 17:
			sendFrame126("" + c.playerLevel[17] + "", 4022);
			sendFrame126("" + getLevelForXP(c.playerXP[17]) + "", 4023);
			sendFrame126("" + c.playerXP[17] + "", 4098);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[17]) + 1)
					+ "", 4099);
			break;

		case 18:
			sendFrame126("" + c.playerLevel[18] + "", 12166);
			sendFrame126("" + getLevelForXP(c.playerXP[18]) + "", 12167);
			sendFrame126("" + c.playerXP[18] + "", 12171);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[18]) + 1)
					+ "", 12172);
			break;

		case 19:
			sendFrame126("" + c.playerLevel[19] + "", 13926);
			sendFrame126("" + getLevelForXP(c.playerXP[19]) + "", 13927);
			sendFrame126("" + c.playerXP[19] + "", 13921);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[19]) + 1)
					+ "", 13922);
			break;

		case 20:
			sendFrame126("" + c.playerLevel[20] + "", 4152);
			sendFrame126("" + getLevelForXP(c.playerXP[20]) + "", 4153);
			sendFrame126("" + c.playerXP[20] + "", 4157);
			sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[20]) + 1)
					+ "", 4155);
			break;
		case 21:
			sendFrame126("" + c.playerLevel[21] + "", 22000);
			//sendFrame126("" + getLevelForXP(c.playerXP[21]) + "", 22001);
			//sendFrame126("" + c.playerXP[21] + "", 18806);
			//sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[21]) + 1) + "", 18807);
			break;
			case 22:
			sendFrame126("" + c.playerLevel[22] + "", 22001);
			//sendFrame126("" + getLevelForXP(c.playerXP[22]) + "", 18800);
			//sendFrame126("" + c.playerXP[22] + "", 18820);
			//sendFrame126("" + getXPForLevel(getLevelForXP(c.playerXP[22]) + 1) + "", 18821);
			break;
		}
		if (i >= 0 && i < c.playerLevel.length) {
			setSkillLevel(i, c.playerLevel[i], c.playerXP[i]);
		}
	}

	public int getXPForLevel(int level) {
		int points = 0;
		int output = 0;

		for (int lvl = 1; lvl <= level; lvl++) {
			points += Math.floor((double) lvl + 300.0
					* Math.pow(2.0, (double) lvl / 7.0));
			if (lvl >= level)
				return output;
			output = (int) Math.floor(points / 4);
		}
		return 0;
	}

	public int getLevelForXP(int exp) {
		int points = 0;
		int output = 0;
		if (exp > 13034430)
			return 99;
		for (int lvl = 1; lvl <= 99; lvl++) {
			points += Math.floor((double) lvl + 300.0
					* Math.pow(2.0, (double) lvl / 7.0));
			output = (int) Math.floor(points / 4);
			if (output >= exp) {
				return lvl;
			}
		}
		return 0;
	}
	
	public boolean addSkillXP(double amount, int skill) {
		if(c.lockedEXP == 1) {
			return false;
			}
		if (amount + c.playerXP[skill] < 0 || c.playerXP[skill] > 200000000) {
			if (c.playerXP[skill] > 200000000) {
				c.playerXP[skill] = 200000000;
			}
			return false;
		}
		amount *= Config.SERVER_EXP_BONUS * (c.rubbedLamp ? 1 : c.expModifier);
		int oldLevel = getLevelForXP(c.playerXP[skill]);
		c.playerXP[skill] += amount;
		if (oldLevel < getLevelForXP(c.playerXP[skill])) {
			if (c.playerLevel[skill] < c.getLevelForXP(c.playerXP[skill])
					&& skill != 3 && skill != 5)
				c.playerLevel[skill] = c.getLevelForXP(c.playerXP[skill]);
			levelUp(skill);
			c.gfx100(199);
			requestUpdates();
		}
		setSkillLevel(skill, c.playerLevel[skill], c.playerXP[skill]);
		refreshSkill(skill);
		return true;
	}

	public boolean addSkillXP(int amount, int skill) {
		if(c.lockedEXP == 1 || c.trollSpawned || c.treeSpawned || c.zombieSpawned
				 || c.golemSpawned) {
			return false;
			}
		if (amount + c.playerXP[skill] < 0 || c.playerXP[skill] > 200000000) {
			if (c.playerXP[skill] > 200000000) {
				c.playerXP[skill] = 200000000;
			}
			return false;
		}
		amount *= Config.SERVER_EXP_BONUS * (c.rubbedLamp ? 1 : c.expModifier);
		int oldLevel = getLevelForXP(c.playerXP[skill]);
		c.playerXP[skill] += amount;
		if (oldLevel < getLevelForXP(c.playerXP[skill])) {
			if (c.playerLevel[skill] < c.getLevelForXP(c.playerXP[skill])
					&& skill != 3 && skill != 5)
				c.playerLevel[skill] = c.getLevelForXP(c.playerXP[skill]);
			levelUp(skill);
			c.gfx100(199);
			requestUpdates();
		}
		setSkillLevel(skill, c.playerLevel[skill], c.playerXP[skill]);
		refreshSkill(skill);
		if(skill == 21)
			c.getPA().reloadConstructionStrings();
		else if(skill == 22)
			c.getPA().reloadHunterStrings();
		return true;
	}

	public void resetBarrows() {
		c.barrowsNpcs[0][1] = 0;
		c.barrowsNpcs[1][1] = 0;
		c.barrowsNpcs[2][1] = 0;
		c.barrowsNpcs[3][1] = 0;
		c.barrowsNpcs[4][1] = 0;
		c.barrowsNpcs[5][1] = 0;
		c.barrowsKillCount = 0;
		c.randomCoffin = Misc.random(3) + 1;
	}

	public static int Barrows[] = { 4708, 4710, 4712, 4714, 4716, 4718, 4720,
			4722, 4724, 4726, 4728, 4730, 4732, 4734, 4736, 4738, 4745, 4747,
			4749, 4751, 4753, 4755, 4757, 4759 };
	public static int Runes[] = { 4740, 558, 560, 565 };
	public static int Pots[] = {};
	public static int Gems[] = { 
		1625,1625,1617,1625,1625,1623,
		1625,1625,1619,1625,1625,1625,
		1625,1625,1621,1627,1627,1627,
		1627,1627,1629,1623,1629,1629,
		};

	public int randomBarrows() {
		return Barrows[(int) (Math.random() * Barrows.length)];
	}

	public int randomRunes() {
		return Runes[(int) (Math.random() * Runes.length)];
	}

	public int randomPots() {
		return Pots[(int) (Math.random() * Pots.length)];
	}
	
	public int randomGems() {
		return Gems[(int) (Math.random() * Gems.length)];
	}

	/**
	 * Show an arrow icon on the selected player.
	 * 
	 * @Param i - Either 0 or 1; 1 is arrow, 0 is none.
	 * @Param j - The player/Npc that the arrow will be displayed above.
	 * @Param k - Keep this set as 0
	 * @Param l - Keep this set as 0
	 */
	public void drawHeadicon(int i, int j, int k, int l) {
		// synchronized(c) {
		c.outStream.createFrame(254);
		c.outStream.writeByte(i);

		if (i == 1 || i == 10) {
			c.outStream.writeWord(j);
			c.outStream.writeWord(k);
			c.outStream.writeByte(l);
		} else {
			c.outStream.writeWord(k);
			c.outStream.writeWord(l);
			c.outStream.writeByte(j);
		}

	}

	public int getNpcId(int id) {
		for (int i = 0; i < NPCHandler.maxNPCs; i++) {
			if (NPCHandler.npcs[i] != null) {
				if (NPCHandler.npcs[i].npcId == id) {
					return i;
				}
			}
		}
		return -1;
	}

	public void removeObject(int x, int y) {
		object(-1, x, x, 10, 10);
	}

	private void objectToRemove(int X, int Y) {
		object(-1, X, Y, 10, 10);
	}

	private void objectToRemove2(int X, int Y) {
		object(-1, X, Y, -1, 0);
	}

	public void removeObjects() {
		objectToRemove(2638, 4688);
		objectToRemove2(2635, 4693);
		objectToRemove2(2634, 4693);
	}

	public void handleGlory(int gloryId) {
		c.getDH().sendOption4("Edgeville", "Al Kharid", "Karamja", "Mage Bank");
		c.usingGlory = true;
	}

	public void resetVariables() {
		if(c.playerIsCrafting)
			CraftingData.resetCrafting(c);
		if(c.playerIsFletching)
			Fletching.resetFletching(c);
		if(c.playerSkilling[Player.playerHerblore] || c.isGrinding || c.isPotionMaking)
			Herblore.resetHerblore(c);
		if(c.playerSkilling[14])
			Mining.resetMining(c);
		if(c.playerSkilling[7])
			Cooking.resetCooking(c);
		Fishing.resetFishing(c);
			Smelting.resetSmelting(c);
		if(c.isBanking)
			c.isBanking = false;
		if(c.isShopping)
			c.isShopping = false;
		if(c.inTrade)
			c.inTrade = false;
		if(c.openDuel)
			c.openDuel = false;
		if(c.duelStatus == 1)
			c.duelStatus = 0;
		c.usingGlory = false;
		c.smeltInterface = false;
		//c.smeltAmount = 0;
		if(c.dialogueAction > -1)
			c.dialogueAction = -1;
		if(c.teleAction > -1)
			c.teleAction = -1;
		if(c.craftDialogue)
			c.craftDialogue = false;
	}
	
	public void sendStatement(String s) { // 1 line click here to continue chat box interface
		c.getPA().sendFrame126(s, 357);
		c.getPA().sendFrame126("Click here to continue", 358);
		c.getPA().sendFrame164(356);
	}

	public boolean inPitsWait() {
		return c.getX() <= 2404 && c.getX() >= 2394 && c.getY() <= 5175
				&& c.getY() >= 5169;
	}
	
	public void handleObjectRegion(int objectId, int minX, int minY, int maxX, int maxY) {
		for (int i = minX; i < maxX+1; i++) {
			for (int j = minY; j < maxY+1; j++) {
				c.getPA().object(objectId, i, j, -1, 10);
			}
		}
	}
	
	public boolean itemUsedInRegion(int minX, int maxX, int minY, int maxY) {
		return (c.objectX >= minX && c.objectX <= maxX) && (c.objectY >= minY && c.objectY <= maxY);
	}

	public int antiFire() {
		int toReturn = 0;
		if (c.antiFirePot)
			toReturn++;
		if (c.playerEquipment[c.playerShield] == 1540 || c.prayerActive[12]
				|| c.playerEquipment[c.playerShield] == 11284)
			toReturn++;
		return toReturn;
	}

	public boolean checkForFlags() {
		int[][] itemsToCheck = { { 995, 100000000 }, { 35, 5 }, { 667, 5 },
				{ 2402, 5 }, { 746, 5 }, { 4151, 150 }, { 565, 100000 },
				{ 560, 100000 }, { 555, 300000 }, { 11235, 10 } };
		for (int j = 0; j < itemsToCheck.length; j++) {
			if (itemsToCheck[j][1] < c.getItems().getTotalCount(
					itemsToCheck[j][0]))
				return true;
		}
		return false;
	}

	private int randomHolidayItem = Misc.random(15);

	public void addHolidayItem() {
		if (randomHolidayItem == 0) {
			c.getItems().addItem(6856, 1);
			c.getItems().addItem(6857, 1);
		} else if (randomHolidayItem == 1) {
			c.getItems().addItem(6858, 1);
			c.getItems().addItem(6859, 1);
		} else if (randomHolidayItem == 2) {
			c.getItems().addItem(6860, 1);
			c.getItems().addItem(6861, 1);
		} else if (randomHolidayItem == 3) {
			c.getItems().addItem(6862, 1);
			c.getItems().addItem(6863, 1);
		} else if (randomHolidayItem == 4) {
			c.getItems().addItem(1037, 1);
		} else if (randomHolidayItem == 5) {
			c.getItems().addItem(1038, 1);
		} else if (randomHolidayItem == 6) {
			c.getItems().addItem(1040, 1);
		} else if (randomHolidayItem == 7) {
			c.getItems().addItem(1042, 1);
		} else if (randomHolidayItem == 8) {
			c.getItems().addItem(1044, 1);
		} else if (randomHolidayItem == 9) {
			c.getItems().addItem(1046, 1);
		} else if (randomHolidayItem == 10) {
			c.getItems().addItem(1048, 1);
		} else if (randomHolidayItem == 11) {
			c.getItems().addItem(1050, 1);
		} else if (randomHolidayItem == 12) {
			c.getItems().addItem(1053, 1);
		} else if (randomHolidayItem == 13) {
			c.getItems().addItem(1055, 1);
		} else if (randomHolidayItem == 14) {
			c.getItems().addItem(1057, 1);
		} else if (randomHolidayItem == 15) {
			c.getItems().addItem(1419, 1);
		}
	}

	public void addStarter() {
		if (!Connection.hasRecieved1stStarter(PlayerHandler.players[c.playerId].connectedFrom)) {
			if(c.adventurerPid) {
				c.getItems().addItem(1323, 1);
				c.getItems().addItem(1309, 1);
				c.getItems().addItem(1333, 1);
				c.getItems().addItem(1319, 1);
				c.getItems().addItem(7458, 1);
				c.getItems().addItem(380, 100);
				c.getItems().addItem(1265, 1);
				c.getItems().addItem(1351, 1);
				c.getItems().addItem(1205, 1);
				c.getItems().addItem(1277, 1);
				c.getItems().addItem(1171, 1);
				c.getItems().addItem(841, 1);
				c.getItems().addItem(882, 100);
				c.getItems().addItem(556, 100);
				c.getItems().addItem(558, 100);
				c.getItems().addItem(555, 80);
				c.getItems().addItem(557, 80);
				c.getItems().addItem(559, 80);
				c.getItems().addItem(995, 2000000);
				//c.getItems().addItem(2528, 1);
				c.getItems().addItem(4447, 1);
				//c.getItems().addItem(4151, 1);
				//c.getDH().sendDialogues(62, 0);
			} else if(c.pkerPid) {
				c.getItems().addItem(1704, 1); //Glory Amulet
				c.getItems().addItem(2550, 1); //Glory Amulet
				c.getItems().addItem(1129, 1); //Leather Body
				c.getItems().addItem(1095, 1); //Leather Chaps
				c.getItems().addItem(841, 1); //Shortbow
				c.getItems().addItem(882, 300); //Bronze Arrows
				c.getItems().addItem(1153, 1); //Iron Full Helm
				c.getItems().addItem(1115, 1); //Iron Platebody
				c.getItems().addItem(1067, 1); //Iron Platelegs
				c.getItems().addItem(1323, 1); //Iron Scimitar
				c.getItems().addItem(4587, 1); //Iron Scimitar
				c.getItems().addItem(1215, 1); //Iron Scimitar
				c.getItems().addItem(6107, 1); //Ghostly Robe Top
				c.getItems().addItem(6108, 1); //Ghostly Robe Bottom
				c.getItems().addItem(6109, 1); //Ghostly Robe Hood
				c.getItems().addItem(1381, 1); //Staff of air
				c.getItems().addItem(558, 150); //Mind Rune
				c.getItems().addItem(556, 250); //Air Rune
				c.getItems().addItem(555, 150); //Water Rune
				c.getItems().addItem(554, 150); //Fire Rune
				c.getItems().addItem(3105, 1); //Climbing Boots
				c.getItems().addItem(562, 150); //Chaos 
				c.getItems().addItem(374, 100); //Swordfish
				c.getItems().addItem(2435, 150); //Prayer Potion
				c.getItems().addItem(995, 2000000); //Cash Stack
				c.getItems().addItem(4447, 1);
				c.getItems().addItem(4447, 1);
				c.getItems().addItem(4447, 1);
			} else if(c.skillerPid) {
				c.getItems().addItem(2460, 1); //Glory Amulet
				c.getItems().addItem(2462, 1); //Glory Amulet
				c.getItems().addItem(2464, 1); //Leather Body
				c.getItems().addItem(2466, 1); //Leather Chaps
				c.getItems().addItem(2468, 1); //Shortbow
				c.getItems().addItem(2470, 1); //Bronze Arrows
				c.getItems().addItem(2472, 1); //Iron Full Helm
				c.getItems().addItem(2474, 1); //Iron Platebody
				c.getItems().addItem(2476, 1); //Iron Platelegs
				c.getItems().addItem(6107, 1); //Ghostly Robe Top
				c.getItems().addItem(6108, 1); //Ghostly Robe Bottom
				c.getItems().addItem(6109, 1); //Ghostly Robe Hood
				c.getItems().addItem(7051, 1); //Mind Rune
				c.getItems().addItem(995, 2000000); //Cash Stack
				c.getItems().addItem(4447, 1);
			}
		Connection.addIpToStarterList1(PlayerHandler.players[c.playerId].connectedFrom);
		Connection.addIpToStarter1(PlayerHandler.players[c.playerId].connectedFrom);
		//c.sendMessage("@red@Thanks for joining! You have received a special item: Whip.");
		c.sendMessage("@red@Thanks for joining Biohazard!");
		c.sendMessage("@red@Rub the lamp to advance a level to 70!");
		c.sendMessage("@red@Type ::train to train instantly.");
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c2 = (Client)PlayerHandler.players[j];
				c2.sendMessage("@cr1@@red@[Biohazard] @dre@"+c.playerName+" has joined Biohazard for the first time!");
			}
		}
	} else if (Connection.hasRecieved1stStarter(PlayerHandler.players[c.playerId].connectedFrom) && !Connection.hasRecieved2ndStarter(PlayerHandler.players[c.playerId].connectedFrom)) {
		if(c.adventurerPid) {
			c.getItems().addItem(1323, 1);
			c.getItems().addItem(1309, 1);
			c.getItems().addItem(1333, 1);
			c.getItems().addItem(1319, 1);
			c.getItems().addItem(7458, 1);
			c.getItems().addItem(380, 100);
			c.getItems().addItem(1265, 1);
			c.getItems().addItem(1351, 1);
			c.getItems().addItem(1205, 1);
			c.getItems().addItem(1277, 1);
			c.getItems().addItem(1171, 1);
			c.getItems().addItem(841, 1);
			c.getItems().addItem(882, 100);
			c.getItems().addItem(882, 100);
			c.getItems().addItem(556, 100);
			c.getItems().addItem(558, 100);
			c.getItems().addItem(555, 80);
			c.getItems().addItem(557, 80);
			c.getItems().addItem(559, 80);
			c.getItems().addItem(995, 2000000);
			c.getItems().addItem(4447, 1);
		} else if(c.pkerPid) {
			c.getItems().addItem(1704, 1); //Glory Amulet
			c.getItems().addItem(2550, 1); //Glory Amulet
			c.getItems().addItem(1129, 1); //Leather Body
			c.getItems().addItem(1095, 1); //Leather Chaps
			c.getItems().addItem(841, 1); //Shortbow
			c.getItems().addItem(882, 300); //Bronze Arrows
			c.getItems().addItem(1153, 1); //Iron Full Helm
			c.getItems().addItem(1115, 1); //Iron Platebody
			c.getItems().addItem(1067, 1); //Iron Platelegs
			c.getItems().addItem(1323, 1); //Iron Scimitar
			c.getItems().addItem(4587, 1); //Iron Scimitar
			c.getItems().addItem(1215, 1); //Iron Scimitar
			c.getItems().addItem(6107, 1); //Ghostly Robe Top
			c.getItems().addItem(6108, 1); //Ghostly Robe Bottom
			c.getItems().addItem(6109, 1); //Ghostly Robe Hood
			c.getItems().addItem(1381, 1); //Staff of air
			c.getItems().addItem(558, 150); //Mind Rune
			c.getItems().addItem(556, 250); //Air Rune
			c.getItems().addItem(555, 150); //Water Rune
			c.getItems().addItem(554, 150); //Fire Rune
			c.getItems().addItem(3105, 1); //Climbing Boots
			c.getItems().addItem(562, 150); //Chaos 
			c.getItems().addItem(374, 100); //Swordfish
			c.getItems().addItem(2435, 150); //Prayer Potion
			c.getItems().addItem(995, 2000000); //Cash Stack
			c.getItems().addItem(4447, 1);
			c.getItems().addItem(4447, 1);
			c.getItems().addItem(4447, 1);
		} else if(c.skillerPid) {
			c.getItems().addItem(2460, 1); //Glory Amulet
			c.getItems().addItem(2462, 1); //Glory Amulet
			c.getItems().addItem(2464, 1); //Leather Body
			c.getItems().addItem(2466, 1); //Leather Chaps
			c.getItems().addItem(2468, 1); //Shortbow
			c.getItems().addItem(2470, 1); //Bronze Arrows
			c.getItems().addItem(2472, 1); //Iron Full Helm
			c.getItems().addItem(2474, 1); //Iron Platebody
			c.getItems().addItem(2476, 1); //Iron Platelegs
			c.getItems().addItem(6107, 1); //Ghostly Robe Top
			c.getItems().addItem(6108, 1); //Ghostly Robe Bottom
			c.getItems().addItem(6109, 1); //Ghostly Robe Hood
			c.getItems().addItem(7051, 1); //Mind Rune
			c.getItems().addItem(995, 2000000); //Cash Stack
			c.getItems().addItem(4447, 1);
		}
		c.sendMessage("You have received 2 out of 2 starter packages.");
		c.sendMessage("@red@Rub the lamp to advance a level to 70!");
		c.sendMessage("@red@Type ::train to train instantly.");
		Connection.addIpToStarterList2(PlayerHandler.players[c.playerId].connectedFrom);
		Connection.addIpToStarter2(PlayerHandler.players[c.playerId].connectedFrom);
	} else if (Connection.hasRecieved1stStarter(PlayerHandler.players[c.playerId].connectedFrom) && Connection.hasRecieved2ndStarter(PlayerHandler.players[c.playerId].connectedFrom)) {
		c.sendMessage("You have already received 2 starters!"); //recieved received 
	}
	}

	public int getWearingAmount() {
		int count = 0;
		for (int j = 0; j < c.playerEquipment.length; j++) {
			if (c.playerEquipment[j] > 0)
				count++;
		}
		return count;
	}

	public void useOperate(int itemId) {
		switch (itemId) {
		case 1712:
		case 1710:
		case 1708:
		case 1706:
			handleGlory(itemId);
			break;
		case 11283:
		case 11284:
			if (c.playerIndex > 0) {
				c.getCombat().handleDfs();
			} else if (c.npcIndex > 0) {
				c.getCombat().handleDfsNPC();
			}
			break;
		}
	}

	public void getSpeared(int otherX, int otherY) {
		int x = c.absX - otherX;
		int y = c.absY - otherY;
		if (x > 0)
			x = 1;
		else if (x < 0)
			x = -1;
		if (y > 0)
			y = 1;
		else if (y < 0)
			y = -1;
		moveCheck(x, y);
		c.lastSpear = System.currentTimeMillis();
	}

	public void moveCheck(int xMove, int yMove) {
		movePlayer(c.absX + xMove, c.absY + yMove, c.heightLevel);
	}

	public int findKiller() {
		int killer = c.playerId;
		int damage = 0;
		for (int j = 0; j < Config.MAX_PLAYERS; j++) {
			if (PlayerHandler.players[j] == null)
				continue;
			if (j == c.playerId)
				continue;
			if (c.goodDistance(c.absX, c.absY, PlayerHandler.players[j].absX,
					PlayerHandler.players[j].absY, 40)
					|| c.goodDistance(c.absX, c.absY + 9400,
							PlayerHandler.players[j].absX,
							PlayerHandler.players[j].absY, 40)
					|| c.goodDistance(c.absX, c.absY,
							PlayerHandler.players[j].absX,
							PlayerHandler.players[j].absY + 9400, 40))
				if (c.damageTaken[j] > damage) {
					damage = c.damageTaken[j];
					killer = j;
				}
		}
		return killer;
	}

	public void resetTzhaar() {
		c.waveId = -1;
		c.tzhaarToKill = -1;
		c.tzhaarKilled = -1;
		c.getPA().movePlayer(2438, 5168, 0);
	}

	/*
	 * public void enterCaves() { c.getPA().movePlayer(2413,5117, c.playerId *
	 * 4); c.waveId = 0; c.tzhaarToKill = -1; c.tzhaarKilled = -1;
	 * EventManager.addEvent(new Event() { public void
	 * execute(EventContainer e) {
	 * Server.fightCaves.spawnNextWave((Client)Server
	 * .playerHandler.players[c.playerId]); e.stop(); } }, 10000); }
	 */

	public void enterCaves() {
		c.getPA().movePlayer(2413, 5117, c.playerId * 4);
		c.waveId = 0;
		c.tzhaarToKill = -1;
		c.tzhaarKilled = -1;
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				container.stop();
			}
			@Override
			public void stop() {
				Server.fightCaves.spawnNextWave((Client) PlayerHandler.players[c.playerId]);
			}
		}, 10);
	}

	public void appendPoison(final Client c, int damage) {
		if (c.playerEquipment[c.playerShield] == 18340) {
			c.sendMessage("Your Anti-poison totem prevents you from being poisoned.");
			return;
		}
		if (c.poisonDamage < 1) {
			c.sendMessage("You have been poisoned.");
			c.poisonDamage = damage;
			c.lastPoison = 36;
			CycleEventHandler.addEvent(c, new CycleEvent() {
				@Override
				public void execute(CycleEventContainer container) {
					if(c.lastPoison > 0) {
						c.lastPoison--;
					}
					if (c.lastPoison == 0) {
						c.poisonMask = !c.getHitUpdateRequired() ? 1 : 2;
						c.handleHitMask(c.poisonDamage);
						c.dealDamage(c.poisonDamage);
						c.getPA().refreshSkill(3);
						c.poisonDamage--;
						c.lastPoison = 36;
					}
					if (c.poisonDamage == 0 && c.isDead == false) {
						c.sendMessage("The poison has worn off.");
						container.stop();
					}
					if (c.isDead == true) {
						container.stop();
						
					}
				}
				@Override
				public void stop() {
					c.lastPoison = 0;
					c.poisonDamage = 0;
				}
			}, 1);
		}
	}

	public boolean checkForPlayer(int x, int y) {
		for (Player p : PlayerHandler.players) {
			if (p != null) {
				if (p.getX() == x && p.getY() == y)
					return true;
			}
		}
		return false;
	}

	public void checkPouch(int i) {
		if (i < 0)
			return;
		c.sendMessage("This pouch has " + c.pouches[i] + " rune ess in it.");
	}

	public void fillPouch(int i) {
		if (i < 0)
			return;
		int toAdd = c.POUCH_SIZE[i] - c.pouches[i];
		if (toAdd > c.getItems().getItemAmount(1436)) {
			toAdd = c.getItems().getItemAmount(1436);
		}
		if (toAdd > c.POUCH_SIZE[i] - c.pouches[i])
			toAdd = c.POUCH_SIZE[i] - c.pouches[i];
		if (toAdd > 0) {
			c.getItems().deleteItem(1436, toAdd);
			c.pouches[i] += toAdd;
		}
	}

	public void emptyPouch(int i) {
		if (i < 0)
			return;
		int toAdd = c.pouches[i];
		if (toAdd > c.getItems().freeSlots()) {
			toAdd = c.getItems().freeSlots();
		}
		if (toAdd > 0) {
			c.getItems().addItem(1436, toAdd);
			c.pouches[i] -= toAdd;
		}
	}

	public void fixAllBarrows() {
		int totalCost = 0;
		int cashAmount = c.getItems().getItemAmount(995);
		for (int j = 0; j < c.playerItems.length; j++) {
			boolean breakOut = false;
			for (int i = 0; i < c.getItems().brokenBarrows.length; i++) {
				if (c.playerItems[j] - 1 == c.getItems().brokenBarrows[i][1]) {
					if (totalCost + 80000 > cashAmount) {
						breakOut = true;
						c.sendMessage("You have run out of money.");
						break;
					} else {
						totalCost += 80000;
					}
					c.playerItems[j] = c.getItems().brokenBarrows[i][0] + 1;
				}
			}
			if (breakOut)
				break;
		}
		if (totalCost > 0)
			c.getItems().deleteItem(995, c.getItems().getItemSlot(995),
					totalCost);
	}

	public void handleLoginText() {
		loadQuests();
		c.getPA().sendFrame126("Monster Teleports", 13037);
		c.getPA().sendFrame126("Sailing & Traveling Teleport", 13047);
		c.getPA().sendFrame126("Minigame Teleports", 13055);
		c.getPA().sendFrame126("Boss Teleports", 13063);
		c.getPA().sendFrame126("PK Teleports", 13071);
		c.getPA().sendFrame126("Skilling Guilds", 13081);
		//c.getPA().sendFrame126("City Teleports", 13089);
		c.getPA().sendFrame126("Level 96 : Ghorrock Teleport", 13097);
		c.getPA().sendFrame126("Monster Teleports", 1300);
		c.getPA().sendFrame126("Teleports you to various monsters", 1301);
		c.getPA().sendFrame126("Sailing & Traveling Teleport", 1325);
		c.getPA().sendFrame126("Teleports you to various places", 1326);
		c.getPA().sendFrame126("Minigame Teleports", 1350);
		c.getPA().sendFrame126("Teleports you to various minigames", 1351);
		c.getPA().sendFrame126("Boss Teleports", 1382);
		c.getPA().sendFrame126("Teleports you to many bosses", 1383);
		c.getPA().sendFrame126("PK Teleports", 1415);
		c.getPA().sendFrame126("Teleports you to various PK areas", 1416);
		c.getPA().sendFrame126("Skilling Guilds", 1454);
		c.getPA().sendFrame126("Teleports you to any skilling guild", 1455);
		//c.getPA().sendFrame126("City Teleports", 7457);
		//c.getPA().sendFrame126("Teleports you to several cities", 7458);
	}
	
	public void handleWeaponStyle() {
		if (c.fightMode == 0) {
			sendFrame36(43, c.fightMode);
		} else if (c.fightMode == 1) {
			sendFrame36(43, 3);
		} else if (c.fightMode == 2) {
			sendFrame36(43, 1);
		} else if (c.fightMode == 3) {
			sendFrame36(43, 2);
		}
	}

	/*public void handleWeaponStyle() {
		if (c.fightMode == 0) {
			c.getPA().sendFrame36(43, c.fightMode);
		} else if (c.fightMode == 1) {
			c.getPA().sendFrame36(43, 3);
		} else if (c.fightMode == 2) {
			c.getPA().sendFrame36(43, 1);
		} else if (c.fightMode == 3) {
			c.getPA().sendFrame36(43, 2);
		}
	}*/

	public void hitPlayers(int x, int xx, int y, int yy, int damage) {
		if(c.inArea(x, xx, y, yy)) {
			c.gfx0(287);
			c.handleHitMask(damage);
			c.dealDamage(damage);
		}
	}

	/**
	 * Player Owned Shop Interface Methods
	 */
	
	// Interface IDs - Matched to client-side implementation
	private static final int POS_MAIN_INTERFACE = 43000;
	private static final int POS_BUY_INTERFACE = 44000;
	
	public boolean handlePOSButton(int buttonId) {
		server.game.content.PlayerOwnedShop.debug(c, "button " + buttonId);

		if (posClick(buttonId, 43004, 43051)) {
			c.getPA().closeAllWindows();
			return true;
		}
		if (posClick(buttonId, 43007, 43053, 44006)) {
			c.posSearchingItem = true;
			c.posSearchingPlayer = false;
			c.sendMessage("Enter an item name or id to search:");
			if (c.getOutStream() != null) {
				c.getOutStream().createFrame(187);
				c.flushOutStream();
			}
			return true;
		}
		if (posClick(buttonId, 43008, 43055, 44007)) {
			c.posSearchingPlayer = true;
			c.posSearchingItem = false;
			c.sendMessage("Enter a player name to search:");
			if (c.getOutStream() != null) {
				c.getOutStream().createFrame(187);
				c.flushOutStream();
			}
			return true;
		}
		if (posClick(buttonId, 43009, 43057)) {
			c.getPA().openPOSBuyInterface();
			return true;
		}
		if (posClick(buttonId, 43120, 43121)) {
			server.game.content.PlayerOwnedShop.claimGold(c);
			openPlayerOwnedShop();
			return true;
		}
		if (posClick(buttonId, 43123, 43124)) {
			server.game.content.PlayerOwnedShop.claimCollect(c);
			openPlayerOwnedShop();
			return true;
		}
		if (posClick(buttonId, 43140, 43141)) {
			startPOSList();
			return true;
		}
		for (int i = 0; i < 10; i++) {
			if (posClick(buttonId, 43100 + i, 43600 + i)) {
				startPOSList();
				return true;
			}
			if (posClick(buttonId, 43110 + i)) {
				confirmPOSRemove(i);
				return true;
			}
			if (posClick(buttonId, 43130 + i, 43150 + i)) {
				startPOSEditPrice(i);
				return true;
			}
		}
		if (posClick(buttonId, 44003, 44081)) {
			clearPOSBuySlots();
			c.getPA().closeAllWindows();
			return true;
		}
		if (posClick(buttonId, 44004, 44083)) {
			refreshPOSBrowse();
			return true;
		}
		if (posClick(buttonId, 44005)) {
			clearPOSBuySlots();
			c.getPA().openPlayerOwnedShop();
			return true;
		}
		if (posClick(buttonId, 44900)) {
			c.posSortMode = 1;
			refreshPOSBrowse();
			return true;
		}
		if (posClick(buttonId, 44901)) {
			c.posSortMode = 0;
			refreshPOSBrowse();
			return true;
		}
		if (posClick(buttonId, 44902)) {
			c.posSortMode = 2;
			refreshPOSBrowse();
			return true;
		}
		for (int i = 0; i < 20; i++) {
			if (posClick(buttonId, 44100 + i, 44800 + i, 44160 + i)) {
				startPOSBuy(c.posBuyListingIds[i]);
				return true;
			}
			if (posClick(buttonId, 44120 + i)) {
				buyPOSAmount(c.posBuyListingIds[i], 1);
				return true;
			}
			if (posClick(buttonId, 44140 + i)) {
				buyPOSAmount(c.posBuyListingIds[i], 10);
				return true;
			}
			if (posClick(buttonId, 44180 + i)) {
				buyPOSAmount(c.posBuyListingIds[i], Integer.MAX_VALUE);
				return true;
			}
		}
		return false;
	}

	private static int posAction(int interfaceId) {
		return ((interfaceId >> 8) & 0xff) * 1000 + (interfaceId & 0xff);
	}

	private static boolean posClick(int clicked, int... interfaceIds) {
		for (int id : interfaceIds) {
			if (clicked == id || clicked == posAction(id)) {
				return true;
			}
		}
		return false;
	}

	public void startPOSBuy(long listingId) {
		if (listingId <= 0) {
			c.sendMessage("No listing at this slot.");
			return;
		}
		server.game.content.PlayerOwnedShop.ShopListing listing = server.game.content.PlayerOwnedShop.getListingById(listingId);
		if (listing == null) {
			c.sendMessage("That listing is no longer available.");
			refreshPOSBrowse();
			return;
		}
		if (listing.ownerName.equalsIgnoreCase(c.playerName)) {
			c.sendMessage("You can't buy from your own shop.");
			return;
		}
		c.posBuying = true;
		c.posBuyListingId = listingId;
		c.posBuyMax = listing.amount;
		c.xInterfaceId = 44000;
		c.sendMessage("How many? 1 is unnoted, 2+ is noted. Stock " + listing.amount + " @ " + listing.price + "gp each.");
		if (c.getOutStream() != null) {
			c.getOutStream().createFrame(27);
			c.flushOutStream();
		}
	}

	private void buyPOSAmount(long listingId, int amount) {
		if (listingId <= 0) {
			c.sendMessage("No listing at this slot.");
			return;
		}
		if (server.game.content.PlayerOwnedShop.buyListing(c, listingId, amount)) {
			refreshPOSBrowse();
		} else {
			refreshPOSBrowse();
		}
	}

	private void startPOSList() {
		c.posSelling = true;
		c.posSellStep = 1;
		c.posEditListingId = 0;
		c.sendMessage("Click the item in your inventory. Noted items list as the real item.");
		c.sendMessage("You will enter amount, then price each. Coins cannot be listed.");
	}

	private void confirmPOSRemove(int index) {
		server.game.content.PlayerOwnedShop.PlayerShop shop = server.game.content.PlayerOwnedShop.getPlayerShop(c.playerName);
		if (index < 0 || index >= shop.listings.size()) {
			return;
		}
		server.game.content.PlayerOwnedShop.ShopListing listing = shop.listings.get(index);
		c.posConfirmRemoveId = listing.listingId;
		c.dialogueAction = 8801;
		c.getDH().sendOption2("Yes, remove this listing", "Never mind");
	}

	public void finishPOSRemove(boolean confirmed) {
		long id = c.posConfirmRemoveId;
		c.posConfirmRemoveId = 0;
		c.dialogueAction = 0;
		if (!confirmed || id <= 0) {
			openPlayerOwnedShop();
			return;
		}
		server.game.content.PlayerOwnedShop.reclaimListing(c, id);
		openPlayerOwnedShop();
	}

	private void startPOSEditPrice(int index) {
		server.game.content.PlayerOwnedShop.PlayerShop shop = server.game.content.PlayerOwnedShop.getPlayerShop(c.playerName);
		if (index < 0 || index >= shop.listings.size()) {
			return;
		}
		server.game.content.PlayerOwnedShop.ShopListing listing = shop.listings.get(index);
		c.posEditListingId = listing.listingId;
		c.xInterfaceId = 43002;
		c.sendMessage("Enter the new price EACH for " + server.game.content.PlayerOwnedShop.getItemName(listing.itemId) + ".");
		c.sendMessage(server.game.content.PlayerOwnedShop.priceHint(listing.itemId));
		if (c.getOutStream() != null) {
			c.getOutStream().createFrame(27);
			c.flushOutStream();
		}
	}

	private void clearPOSBuySlots() {
		for (int i = 0; i < 20; i++) {
			c.posBuySellers[i] = null;
			c.posBuyIndexes[i] = -1;
			c.posBuyListingIds[i] = 0;
		}
	}

	private void showPOSItem(int interfaceId, int itemId, int amount) {
		if (itemId <= 0 || amount <= 0) {
			sendFrame34a(interfaceId, -1, 0, 0);
			return;
		}
		sendFrame34a(interfaceId, itemId, 0, amount);
	}

	private String formatPosNumber(long value) {
		return String.format("%,d", value);
	}

	private String shortPosName(String name) {
		if (name == null) {
			return "";
		}
		if (name.length() > 14) {
			return name.substring(0, 14);
		}
		return name;
	}
	
	public void openPlayerOwnedShop() {
		showInterface(POS_MAIN_INTERFACE);
		server.game.content.PlayerOwnedShop.PlayerShop shop = server.game.content.PlayerOwnedShop.getPlayerShop(c.playerName);
		sendFrame126("Player Owned Shop", 43001);
		sendFrame126(formatPosNumber(shop.pendingGold), 43002);
		sendFrame126("Claim", 43077);
		sendFrame126(shop.collect.size() > 0 ? ("Collect: " + shop.collect.size()) : "", 43003);
		for (int i = 0; i < 10; i++) {
			if (i < shop.listings.size()) {
				server.game.content.PlayerOwnedShop.ShopListing listing = shop.listings.get(i);
				String itemName = server.game.content.PlayerOwnedShop.getItemName(listing.itemId);
				sendFrame126("Selling", 43010 + i);
				sendFrame126(shortPosName(itemName), 43040 + i);
				sendFrame126(formatPosNumber(listing.price), 43020 + i);
				showPOSItem(43200 + i, listing.itemId, listing.amount);
				sendFrame126("Remove", 43110 + i);
				sendFrame126("Price", 43130 + i);
				sendFrame171(1, 43900 + i);
			} else {
				sendFrame126("Empty", 43010 + i);
				sendFrame126("", 43040 + i);
				sendFrame126("", 43020 + i);
				showPOSItem(43200 + i, -1, 0);
				sendFrame126("", 43110 + i);
				sendFrame126("", 43130 + i);
				sendFrame171(0, 43900 + i);
			}
		}
		for (int i = 0; i < 5; i++) {
			if (i < shop.sales.size()) {
				String line = shop.sales.get(i);
				if (line.length() > 28) {
					line = line.substring(0, 28);
				}
				sendFrame126(line, 43030 + i);
				sendFrame126("", 43035 + i);
			} else {
				sendFrame126("", 43030 + i);
				sendFrame126("", 43035 + i);
			}
		}
		c.sendMessage("Opened your Player Owned Shop. List item, then click inventory.");
	}
	
	public void openPOSSearchInterface() {
		openPOSBuyInterface();
	}

	private void displayPOSBuyList(String title, java.util.List<server.game.content.PlayerOwnedShop.ShopListing> results) {
		c.posBrowseTitle = title;
		results = server.game.content.PlayerOwnedShop.sortedListings(results, c.posSortMode);
		showInterface(POS_BUY_INTERFACE);
		sendFrame126("Player Owned Shop", 44001);
		sendFrame126("", 44002);
		clearPOSBuySlots();
		int shown = Math.min(20, results.size());
		for (int i = 0; i < shown; i++) {
			server.game.content.PlayerOwnedShop.ShopListing listing = results.get(i);
			sendFrame126(shortPosName(server.game.content.PlayerOwnedShop.getItemName(listing.itemId)), 44010 + i);
			sendFrame126(listing.ownerName, 44030 + i);
			sendFrame126(formatPosNumber(listing.price), 44050 + i);
			sendFrame126("1", 44120 + i);
			sendFrame126("10", 44140 + i);
			sendFrame126("X", 44160 + i);
			sendFrame126("All", 44180 + i);
			c.posBuySellers[i] = listing.ownerName;
			c.posBuyIndexes[i] = server.game.content.PlayerOwnedShop.indexOfListing(listing.ownerName, listing);
			c.posBuyListingIds[i] = listing.listingId;
			showPOSItem(44200 + i, listing.itemId, listing.amount);
		}
		for (int i = shown; i < 20; i++) {
			sendFrame126("", 44010 + i);
			sendFrame126("", 44030 + i);
			sendFrame126("", 44050 + i);
			sendFrame126("", 44120 + i);
			sendFrame126("", 44140 + i);
			sendFrame126("", 44160 + i);
			sendFrame126("", 44180 + i);
			showPOSItem(44200 + i, -1, 0);
		}
	}

	public void searchPOSByItemName(String itemName) {
		if (itemName == null || itemName.trim().length() == 0) {
			c.sendMessage("Type part of an item name or an item id.");
			c.posSearchingItem = true;
			if (c.getOutStream() != null) {
				c.getOutStream().createFrame(187);
				c.flushOutStream();
			}
			return;
		}
		c.posBrowseType = 1;
		c.posBrowseQuery = itemName;
		java.util.List<server.game.content.PlayerOwnedShop.ShopListing> results = server.game.content.PlayerOwnedShop.searchByItemName(itemName);
		if (results.isEmpty()) {
			c.sendMessage("No items found matching: " + itemName + ". Try again.");
			c.posSearchingItem = true;
			if (c.getOutStream() != null) {
				c.getOutStream().createFrame(187);
				c.flushOutStream();
			}
			return;
		}
		displayPOSBuyList("Search: " + itemName, results);
		c.sendMessage("Found " + results.size() + " listings matching: " + itemName);
	}

	public void searchPOSByItem(int itemId) {
		c.posBrowseType = 1;
		c.posBrowseQuery = Integer.toString(itemId);
		java.util.List<server.game.content.PlayerOwnedShop.ShopListing> results = server.game.content.PlayerOwnedShop.searchByItem(itemId);
		if (results.isEmpty()) {
			c.sendMessage("No listings found for that item.");
			return;
		}
		displayPOSBuyList("Search: item " + itemId, results);
		c.sendMessage("Found " + results.size() + " listings for item " + itemId);
	}

	public void searchPOSByPlayer(String playerName) {
		if (playerName == null || playerName.trim().length() == 0) {
			c.sendMessage("Enter a player name.");
			c.posSearchingPlayer = true;
			if (c.getOutStream() != null) {
				c.getOutStream().createFrame(187);
				c.flushOutStream();
			}
			return;
		}
		c.posBrowseType = 2;
		c.posBrowseQuery = playerName;
		java.util.List<server.game.content.PlayerOwnedShop.ShopListing> results = server.game.content.PlayerOwnedShop.searchByOwner(playerName);
		if (results.isEmpty()) {
			c.sendMessage("No shop listings found for " + playerName + ".");
			c.posSearchingPlayer = true;
			if (c.getOutStream() != null) {
				c.getOutStream().createFrame(187);
				c.flushOutStream();
			}
			return;
		}
		displayPOSBuyList("Shop: " + playerName, results);
		c.sendMessage("Found " + results.size() + " listings for " + playerName);
	}

	public void openPOSBuyInterface() {
		c.posBrowseType = 0;
		c.posBrowseQuery = "";
		displayPOSBuyList("Recent Listings", server.game.content.PlayerOwnedShop.getRecentListings());
	}

	public void refreshPOSBrowse() {
		if (c.posBrowseType == 1 && c.posBrowseQuery != null && c.posBrowseQuery.length() > 0) {
			displayPOSBuyList("Search: " + c.posBrowseQuery, server.game.content.PlayerOwnedShop.searchByItemName(c.posBrowseQuery));
		} else if (c.posBrowseType == 2 && c.posBrowseQuery != null && c.posBrowseQuery.length() > 0) {
			displayPOSBuyList("Shop: " + c.posBrowseQuery, server.game.content.PlayerOwnedShop.searchByOwner(c.posBrowseQuery));
		} else {
			displayPOSBuyList("Recent Listings", server.game.content.PlayerOwnedShop.getRecentListings());
		}
	}

	public void addPOSListing(int itemId, int amount, int price) {
		if (server.game.content.PlayerOwnedShop.listItem(c, itemId, amount, price)) {
			c.sendMessage("Added listing to your shop.");
			openPlayerOwnedShop();
		}
	}

	public void removePOSListing(int index) {
		if (server.game.content.PlayerOwnedShop.reclaimListingByIndex(c, index)) {
			openPlayerOwnedShop();
		}
	}

	public void buyFromPOS(String sellerName, int listingIndex) {
		server.game.content.PlayerOwnedShop.ShopListing listing = server.game.content.PlayerOwnedShop.getListing(sellerName, listingIndex);
		if (listing == null) {
			c.sendMessage("Listing not found.");
			return;
		}
		if (server.game.content.PlayerOwnedShop.buyListing(c, listing.listingId)) {
			refreshPOSBrowse();
		}
	}

}
