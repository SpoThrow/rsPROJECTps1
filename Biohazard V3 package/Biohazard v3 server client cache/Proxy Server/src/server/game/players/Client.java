package server.game.players;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Future;

import org.apache.mina.common.IoSession;

import server.Config;
import server.Server;
import server.content.music.MusicTab;
import server.content.quests.DesertTreasure;
import server.content.quests.DoricsQuest;
import server.content.quests.HorrorFromTheDeep;
import server.content.quests.RecipeForDisaster;
import server.content.skills.Agility;
import server.content.skills.Farming;
import server.content.skills.Slayer;
import server.content.skills.Thieving;
import server.content.skills.misc.SkillInterfaces;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.event.RestoreSpecialAttack;
import server.game.items.GameItem;
import server.game.items.ItemAssistant;
import server.game.minigames.barrows.Barrows;
import server.game.minigames.bountyhunter.BountyHunter;
import server.game.minigames.castlewars.CastleWars;
import server.game.minigames.rangersguild.RangersGuild;
import server.game.minigames.treasuretrails.TreasureTrails;
import server.game.players.combat.CombatAssistant;
import server.game.shops.ShopAssistant;
import server.world.Clan;
import core.net.HostList;
import core.net.Packet;
import core.net.RS2LoginProtocolDecoder;
import core.net.StaticPacketBuilder;
import core.util.Misc;
import core.util.Stream;

public class Client extends Player {

	public byte buffer[] = null;
	public Stream inStream = null, outStream = null;
	private IoSession session;
	private ItemAssistant itemAssistant = new ItemAssistant(this);
	private ShopAssistant shopAssistant = new ShopAssistant(this);
	private TradeAndDuel tradeAndDuel = new TradeAndDuel(this);
	private PlayerAssistant playerAssistant = new PlayerAssistant(this);
	private CombatAssistant combatAssistant = new CombatAssistant(this);
	private ActionHandler actionHandler = new ActionHandler(this);
	private PlayerKilling playerKilling = new PlayerKilling(this);
	private TreasureTrails treasureTrails = new TreasureTrails(this);
	private DialogueHandler dialogueHandler = new DialogueHandler(this);
	private Queue<Packet> queuedPackets = new LinkedList<Packet>();
	private Potions potions = new Potions(this);
	private PotionMixing potionMixing = new PotionMixing(this);
	private Food food = new Food(this);
	public ArrayList<GameItem> fishingTrawlerReward = new ArrayList<GameItem>();
	//private TutorialIsland tutorialIsland = new TutorialIsland(this);

	private SkillInterfaces skillInterfaces = new SkillInterfaces(this);

	/**
	 * Quests
	 */
	private DesertTreasure desertTreasure = new DesertTreasure(this);
	private HorrorFromTheDeep horrorFromTheDeep = new HorrorFromTheDeep(this);
	private RecipeForDisaster recipeForDisaster = new RecipeForDisaster(this);
	private DoricsQuest doricsQuest = new DoricsQuest(this);
	private RangersGuild rangersGuild = new RangersGuild(this);

	/**
	 * Skill instances
	 */
	private Slayer slayer = new Slayer(this);
	private Agility agility = new Agility(this);
	private Thieving thieving = new Thieving(this);
	private Farming farming = new Farming(this);

	//private int somejunk;
	public int lowMemoryVersion = 0;
	public int timeOutCounter = 0;		
	public int returnCode = 2; 
	private Future<?> currentTask;
	public int currentRegion = 0;
	public boolean attackSkill = false;
	public boolean usingLevel = false;
	public boolean strengthSkill = false;
	public boolean defenceSkill = false;
	public boolean mageSkill = false;
	public boolean rangeSkill = false;
	public boolean prayerSkill = false;
	public boolean healthSkill = false;
	public boolean posSearchingItem = false;
	public boolean posSearchingPlayer = false;

	public Client(IoSession s, int _playerId) {
		super(_playerId);
		this.session = s;
		synchronized(this) {
			outStream = new Stream(new byte[Config.BUFFER_SIZE]);
			outStream.currentOffset = 0;

			inStream = new Stream(new byte[Config.BUFFER_SIZE]);
			inStream.currentOffset = 0;
			buffer = new byte[Config.BUFFER_SIZE];
		}
	}

	/**
	 * Shakes the player's screen.
	 * Parameters 1, 0, 0, 0 to reset.
	 * @param verticleAmount How far the up and down shaking goes (1-4).
	 * @param verticleSpeed How fast the up and down shaking is.
	 * @param horizontalAmount How far the left-right tilting goes.
	 * @param horizontalSpeed How fast the right-left tiling goes..
	 */

	public void shakeScreen(int verticleAmount, int verticleSpeed, int horizontalAmount, int horizontalSpeed) {
		outStream.createFrame(35); // Creates frame 35.
		outStream.writeByte(verticleAmount);
		outStream.writeByte(verticleSpeed);
		outStream.writeByte(horizontalAmount);
		outStream.writeByte(horizontalSpeed);
	}

	/**
	 * Resets the shaking of the player's screen.
	 */
	public void resetShaking() {
		shakeScreen(1, 0, 0, 0);
	}

	//sync
	public void flushOutStream() {	
		if(disconnected || outStream.currentOffset == 0) return;
		synchronized(this) {	
			StaticPacketBuilder out = new StaticPacketBuilder().setBare(true);
			byte[] temp = new byte[outStream.currentOffset]; 
			System.arraycopy(outStream.buffer, 0, temp, 0, temp.length);
			out.addBytes(temp);
			session.write(out.toPacket());
			outStream.currentOffset = 0;
		}
	}

	public void sendClan(String name, String message, String clan, int rights) {
		outStream.createFrameVarSizeWord(217);
		outStream.writeString(name);
		outStream.writeString(message);
		outStream.writeString(clan);
		outStream.writeWord(rights);
		outStream.endFrameVarSize();
	}

	/*public static final int PACKET_SIZES[] = {
		0, 0, 0, 1, -1, 0, 0, 0, 0, 0, //0
		0, 0, 0, 0, 8, 0, 6, 2, 2, 0,  //10
		0, 2, 0, 6, 0, 12, 0, 0, 0, 0, //20
		0, 0, 0, 0, 0, 8, 4, 0, 0, 2,  //30
		2, 6, 0, 6, 0, -1, 0, 0, 0, 0, //40
		0, 0, 0, 12, 0, 0, 0, 8, 8, 12, //50
		8, 8, 0, 0, 0, 0, 0, 0, 0, 0,  //60
		6, 0, 2, 2, 8, 6, 0, -1, 0, 6, //70
		0, 0, 0, 0, 0, 1, 4, 6, 0, 0,  //80
		0, 0, 0, 0, 0, 3, 0, 0, -1, 0, //90
		0, 13, 0, -1, 0, 0, 0, 0, 0, 0,//100
		0, 0, 0, 0, 0, 0, 0, 6, 0, 0,  //110
		1, 0, 6, 0, 0, 0, -1, 0, 2, 6, //120
		0, 4, 6, 8, 0, 6, 0, 0, 0, 2,  //130
		0, 0, 0, 0, 0, 6, 0, 0, 0, 0,  //140
		0, 0, 1, 2, 0, 2, 6, 0, 0, 0,  //150
		0, 0, 0, 0, -1, -1, 0, 0, 0, 0,//160
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  //170
		0, 8, 0, 3, 0, 2, 0, 0, 8, 1,  //180
		0, 0, 12, 0, 0, 0, 0, 0, 0, 0, //190
		2, 0, 0, 0, 0, 0, 0, 0, 4, 0,  //200
		4, 0, 0, 0, 7, 8, 0, 0, 10, 0, //210
		0, 0, 0, 0, 0, 0, -1, 0, 6, 0, //220
		1, 0, 0, 0, 6, 0, 6, 8, 1, 0,  //230
		0, 4, 0, 0, 0, 0, -1, 0, -1, 4,//240
		0, 0, 6, 6, 0, 0, 0            //250
	};*/
	
	public static final int PACKET_SIZES[] = { 
		0, 0, 0, 1, -1, 0, 0, 0, 0, 0, // 0
		0, 0, 0, 0, 4, 0, 6, 2, 2, 0, // 10
		0, 2, 0, 6, 0, 12, 0, 0, 0, 0, // 20
		0, 0, 0, 0, 0, 8, 4, 0, 0, 2, // 30
		2, 6, 0, 6, 0, -1, 0, 0, 0, 0, // 40
		0, 0, 0, 12, 0, 0, 0, 8, 8, 12, // 50
		8, 8, 0, 0, 0, 0, 0, 0, 0, 0, // 60
		6, 0, 2, 2, 8, 6, 0, -1, 0, 6, // 70
		0, 0, 0, 0, 0, 1, 4, 6, 0, 0, // 80
		0, 0, 0, 0, 0, 3, 0, 0, -1, 0, // 90
		0, 13, 0, -1, 0, 0, 0, 0, 0, 0, // 100
		0, 0, 0, 0, 0, 0, 0, 6, 0, 0, // 110
		1, 0, 6, 0, 0, 0, -1, /* 0 */-1, 2, 6, // 120
		0, 4, 6, 8, 0, 6, 0, 0, 0, 2, // 130
		0, 0, 0, 0, 0, 6, 0, 0, 0, 0, // 140
		0, 0, 1, 2, 0, 2, 6, 0, 0, 0, // 150
		0, 0, 0, 0, -1, -1, 0, 0, 0, 0, // 160
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 170
		0, 8, 0, 3, 0, 2, 0, 0, 8, 1, // 180
		0, 0, 12, 0, 0, 0, 0, 0, 0, 0, // 190
		2, 0, 0, 0, 0, 0, 0, 0, 4, 0, // 200
		4, 0, 0, /* 0 */4, 7, 8, 0, 0, 10, 0, // 210
		0, 0, 0, 0, 0, 0, -1, 0, 6, 0, // 220
		1, 0, 0, 0, 6, 0, 6, 8, 1, 0, // 230
		0, 4, 0, 0, 0, 0, -1, 0, -1, 4, // 240
		0, 0, 6, 6, 0, 0, 0 // 250
	};

	public void destruct() {
		if(session == null) 
			return;
		if(this.underAttackBy > 0 || this.underAttackBy2 > 0)
			return;
		if(Server.trawler.players.contains(this)) {
			Server.trawler.players.remove(this);
		}
		if(BountyHunter.playerHasTarget(this))
			BountyHunter.resetTarget(this);
		//castlewars
        if (CastleWars.isInCwWait(this)) {
            CastleWars.leaveWaitingRoom(this);
        }
        if (CastleWars.isInCw(this)) {
            CastleWars.removePlayerFromCw(this);
        }
		if (inPits)
			Server.fightPits.removePlayerFromPits(playerId);
		if (clan != null) {
			//clan.removeMember(this);
			clan.removeMember(playerName);
		}
		Misc.println("[OFFLINE]: "+ Misc.capitalize(playerName) +"");
		CycleEventHandler.stopEvents(this);
		if(this.playerRights < 2 || this.playerRights > 3) {
			HiscoresHandler.saveHighScore(this);
		}
		HostList.getHostList().remove(session);
		PlayerSave.saveGame(this);
		disconnected = true;
		session.close();
		session = null;
		inStream = null;
		outStream = null;
		isActive = false;
		buffer = null;
		super.destruct();
	}
	
	public void antiFirePotion(){
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				container.stop();
			}
			@Override
			public void stop() {
				antiFirePot = false;	
				sendMessage("Your resistance to dragon fire has worn off.");
			}
		}, 200);		
	}

	//sync
	public void sendMessage(String s) {
		synchronized (this) {
		if(getOutStream() != null) {
			outStream.createFrameVarSize(253);
			outStream.writeString(s);
			outStream.endFrameVarSize();
		}
		}

	}

	//sync
	public void setSidebarInterface(int menuId, int form) {
		synchronized (this) {
		if(getOutStream() != null) {
			outStream.createFrame(71);
			outStream.writeWord(form);
			outStream.writeByteA(menuId);
		}
		}
	}

	//sync
	public void initialize() {
		synchronized (this) {
		outStream.createFrame(249);
		outStream.writeByteA(1);		// 1 for members, zero for free
		outStream.writeWordBigEndianA(playerId);
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (j == playerId)
				continue;
			if (PlayerHandler.players[j] != null) {
				if (PlayerHandler.players[j].playerName.equalsIgnoreCase(playerName))
					disconnected = true;
			}
		}
		if (addStarter)
			getPA().showInterface(18452);
		for (int i = 0; i < 25; i++) {
			getPA().setSkillLevel(i, playerLevel[i], playerXP[i]);
			getPA().refreshSkill(i);
		}
		for(int p = 0; p < PRAYER.length; p++) { // reset prayer glows 
			prayerActive[p] = false;
			getPA().sendFrame36(PRAYER_GLOW[p], 0);	
		}
		getPA().handleWeaponStyle();
		getPA().handleLoginText();
		accountFlagged = getPA().checkForFlags();
		getPA().sendFrame36(108, 0);//resets autocast button
		getPA().sendFrame36(172, 1);
		getPA().sendFrame107(); // reset screen
		getPA().setChatOptions(0, 0, 0); // reset private messaging options
		getPA().setSidebarInterfaces(this, true);
		correctCoordinates();
		getPA().sendFrame36(173,1);
		/**
		 * The server double exp events - return the boolean
		 * condition as true for the weekend from 01200 to 0100
		 */
		sendMessage("Welcome to @blu@"+Config.SERVER_NAME+".");
		sendMessage("Type @red@::commands@bla@ for all the available commands.");
		//sendMessage("Join 'help' clan chat for public clan chat.");
		if(playerRights == 4 || playerRights == 6 || playerRights == 5) {
			sendMessage("Thank you for contributing to us and enjoy your day playing @blu@Biohazard!");
		}
		getPA().showOption(4, 0,"Follow", 4);
		getPA().showOption(5, 0,"Trade With", 3);
		getItems().resetItems(3214);
		getItems().sendWeapon(playerEquipment[playerWeapon], ItemAssistant.getItemName(playerEquipment[playerWeapon]));
		getItems().resetBonus();
		getItems().getBonus();
		getItems().writeBonus();
		getItems().setEquipment(playerEquipment[playerHat],1,playerHat);
		getItems().setEquipment(playerEquipment[playerCape],1,playerCape);
		getItems().setEquipment(playerEquipment[playerAmulet],1,playerAmulet);
		getItems().setEquipment(playerEquipment[playerArrows],playerEquipmentN[playerArrows],playerArrows);
		getItems().setEquipment(playerEquipment[playerChest],1,playerChest);
		getItems().setEquipment(playerEquipment[playerShield],1,playerShield);
		getItems().setEquipment(playerEquipment[playerLegs],1,playerLegs);
		getItems().setEquipment(playerEquipment[playerHands],1,playerHands);
		getItems().setEquipment(playerEquipment[playerFeet],1,playerFeet);
		getItems().setEquipment(playerEquipment[playerRing],1,playerRing);
		getItems().setEquipment(playerEquipment[playerWeapon],playerEquipmentN[playerWeapon],playerWeapon);
		getCombat().getPlayerAnimIndex(ItemAssistant.getItemName(playerEquipment[playerWeapon]).toLowerCase());
		getPA().logIntoPM();
		UUID = RS2LoginProtocolDecoder.UUID;
		getItems().addSpecialBar(playerEquipment[playerWeapon]);
		if(this.specAmount < 100)
			RestoreSpecialAttack.execute(this);
		saveTimer = Config.SAVE_TIMER;
		saveCharacter = true;
		Misc.println("[ONLINE]: "+Misc.capitalize(playerName)+"");
		handler.updatePlayer(this, outStream);
		handler.updateNPC(this, outStream);
		flushOutStream();
		getPA().clearClanChat();
		getPA().resetFollow();
		getPA().setClanData();
		if (lastClanChat != null && lastClanChat.length() > 0) {
			Clan clan = Server.clanManager.getClan(lastClanChat);
			if (clan != null)
				clan.addMember(this);
		}
		if (addStarter) {
			addToHelpCc();
			MusicTab.initializeMusicBooleanFirstTime(this);
		}
		if (autoRet == 1)
			getPA().sendFrame36(172, 1);
		else
			getPA().sendFrame36(172, 0);
		MusicTab.loadMusicTab(this);
		totalLevel = getPA().totalLevel();
		xpTotal = getPA().xpTotal();
		BountyHunter.checkBHTimer(this);
		//loadRegion();
		if(addStarter) {
			canWalk = false;
			getPA().starterSideBars(this);
			getPA().closeAllWindows();
			getDH().sendDialogues(460, 2244);
			//startGuide();
			//getPA().addStarter();
			//getPA().showInterface(3559); 
			//canChangeAppearance = true;
		}
		getPA().requestUpdates();
		}
	}
	
	/*private void loadRegion() {
		Music.playMusic(this);
		Server.itemHandler.reloadItems(this);
		clearLists();
		Server.objectManager.loadObjects(this);
		if(skullTimer > 0) {
			isSkulled = true;	
			headIconPk = 0;
		}
	}*/

	
	//sync
	public void update() {
		synchronized (this) {
		handler.updatePlayer(this, outStream);
		handler.updateNPC(this, outStream);
		flushOutStream();
		}
	}

	//sync
	public void logout() {
		synchronized (this) {
		if(System.currentTimeMillis() - logoutDelay > 10000) {
			if(Server.trawler.players.contains(this)) {
				Server.trawler.players.remove(this);
			}
			if (clan != null) {
				//clan.removeMember(this);
				clan.removeMember(playerName);
			}
			if(BountyHunter.playerHasTarget(this))
				BountyHunter.resetTarget(this);
			//castlewars
	        if (CastleWars.isInCwWait(this)) {
	            CastleWars.leaveWaitingRoom(this);
	        }
	        if (CastleWars.isInCw(this)) {
	            CastleWars.removePlayerFromCw(this);
	        }
			outStream.createFrame(109);
			CycleEventHandler.stopEvents(this);
			/*if(this.playerRights < 2 || this.playerRights > 3) {
				HiscoresHandler.saveHighScore(this);
			}*/
			properLogout = true;
		} else {
			sendMessage("You must wait a few seconds from being out of combat to logout.");
		}
		}
	}
	
	public void trawlerFade(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					      getPA().resetAnimationsToPrevious();
					      updateRequired = true;
					      appearanceUpdateRequired = true;
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 0) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						}
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
				}
			}, 1);
		}
	}
	
	public void fadeStarterTele3(final int x, final int y, final int height) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 0) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						}
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					getDH().sendDialogues(471, 2244);
				}
			}, 1);
	}
	
	public void fadeStarterTele2(final int x, final int y, final int height) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 0) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						}
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					getDH().sendDialogues(598, 2244);
				}
			}, 1);
	}
	
	public void fadeStarterTele(final int x, final int y, final int height) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 0) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						}
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					getDH().sendDialogues(597, 2244);
				}
			}, 1);
	}
	
	public void fade(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 0) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						}
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
				}
			}, 1);
		}
	}
	
	public void fadeKQ(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 0) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						}
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					if(!getItems().playerHasItem(954)){
						getDH().sendStatement("I may need a rope to enter this tunnel.");
					}
				}
			}, 1);
		}
	}
	
	public void fadeDesert(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 0) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						}
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					getDH().sendStatement("The desert is too dry ... and you begin to see a Mirage.");
				}
			}, 1);
		}
	}
	
	public void fadeDungeon(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 0) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						}
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					getDH().sendStatement("The dungeon begins to collapse ... and you fell down.");
				}
			}, 1);
		}
	}
	
	public void fadeCrash(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 0) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						}
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					getDH().sendStatement("The boat crashed ... and you reached the sea bottom.");
				}
			}, 1);
		}
	}
	
	/*public void trawlerFade(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 5;
				public void execute(CycleEventContainer container) {
					if (tStage == 5) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 4) {
					      getPA().movePlayer(x, y, height);
					      getPA().resetAnimationsToPrevious();
					      appearanceUpdateRequired = true;
					    }
					    if (tStage == 3) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 1) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						  }
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
				}
			}, 1);
		}
	}
	
	public void fade(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 5) {
					      getPA().movePlayer(x, y, height);
					      updateRequired = true;
					      appearanceUpdateRequired = true;
					    }
					    if (tStage == 4) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 1) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						  }
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
				}
			}, 1);
		}
	}
	
	public void fadeKQ(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 5) {
					      getPA().movePlayer(x, y, height);
					      updateRequired = true;
					      appearanceUpdateRequired = true;
					    }
					    if (tStage == 4) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 1) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						  }
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					if(!getItems().playerHasItem(954)){
						getDH().sendStatement("I may need a rope to enter this tunnel.");
					}
				}
			}, 1);
		}
	}
	
	public void fadeDesert(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 5) {
					      getPA().movePlayer(x, y, height);
					      updateRequired = true;
					      appearanceUpdateRequired = true;
					    }
					    if (tStage == 4) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 1) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						  }
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					getDH().sendStatement("The desert is too dry ... and you begin to see a Mirage.");
				}
			}, 1);
		}
	}
	
	public void fadeDungeon(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 5) {
					      getPA().movePlayer(x, y, height);
					      updateRequired = true;
					      appearanceUpdateRequired = true;
					    }
					    if (tStage == 4) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 1) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						  }
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					getDH().sendStatement("The dungeon begins to collapse ... and you fell down.");
				}
			}, 1);
		}
	}
	
	public void fadeCrash(final int x, final int y, final int height) {
		if (System.currentTimeMillis() - lastAction > 5000) {
			lastAction = System.currentTimeMillis();
			resetWalkingQueue();
			dialogueAction = -1;
			teleAction = -1;
			CycleEventHandler.addEvent(this, new CycleEvent() {
				int tStage = 6;
				public void execute(CycleEventContainer container) {
					if (tStage == 6) {
					      getPA().showInterface(18460);
					    }
					    if (tStage == 5) {
					      getPA().movePlayer(x, y, height);
					      updateRequired = true;
					      appearanceUpdateRequired = true;
					    }
					    if (tStage == 4) {
					      getPA().showInterface(18452);
					    }
						if (tStage == 1) {
							container.stop();
							return;
					    }
						if (tStage > 0) {
							tStage--;
						  }
				}
				public void stop() {
					getPA().closeAllWindows();
					tStage = 0;
					getDH().sendStatement("The boat crashed ... and you reached the sea bottom.");
				}
			}, 1);
		}
	}*/

	public int packetSize = 0, packetType = -1;

	public void process() {
		if(!isResting) {
			if (playerEnergy < 100 && System.currentTimeMillis() - lastIncrease >= getPA().raiseTimer()) {
				playerEnergy += 1;
				lastIncrease = System.currentTimeMillis();
			}
		}
		if(isResting) {
			if (playerEnergy < 100 && System.currentTimeMillis() - lastIncrease >= getPA().raiseTimer2()) {
				playerEnergy += 1;
				lastIncrease = System.currentTimeMillis();
			}
		}
		getPA().writeEnergy();
		/*if(System.currentTimeMillis() - specDelay > Config.INCREASE_SPECIAL_AMOUNT) {
			specDelay = System.currentTimeMillis();
			if(specAmount < 100) {
				specAmount += 5;
				if (specAmount > 100)
					specAmount = 100;
				getItems().addSpecialBar(playerEquipment[playerWeapon]);
			}
		}*/

		if(followId > 0) {
			getPA().followPlayer();
		} else if (followId2 > 0) {
			getPA().followNpc();
		}
		getCombat().handlePrayerDrain(this);
		if(System.currentTimeMillis() - singleCombatDelay >  3300) {
			underAttackBy = 0;
		}
		if (System.currentTimeMillis() - singleCombatDelay2 > 3300) {
			underAttackBy2 = 0;
		}

		if(System.currentTimeMillis() - restoreStatsDelay >  60000) {
			restoreStatsDelay = System.currentTimeMillis();
			for (int level = 0; level < playerLevel.length; level++)  {
				if (playerLevel[level] < getLevelForXP(playerXP[level])) {
					if(level != 5) { // prayer doesn't restore
						playerLevel[level] += 1;
						getPA().setSkillLevel(level, playerLevel[level], playerXP[level]);
						getPA().refreshSkill(level);
					}
				} else if (playerLevel[level] > getLevelForXP(playerXP[level])) {
					playerLevel[level] -= 1;
					getPA().setSkillLevel(level, playerLevel[level], playerXP[level]);
					getPA().refreshSkill(level);
				}
			}
		}

		if(inWild()) {
			int modY = absY > 6400 ?  absY - 6400 : absY;
			wildLevel = (((modY - 3520) / 8) + 1);
			getPA().walkableInterface(197);
			if(Config.SINGLE_AND_MULTI_ZONES) {
				if(inMulti()) {
					getPA().sendFrame126("@yel@Level: "+wildLevel, 199);
				} else {
					getPA().sendFrame126("@yel@Level: "+wildLevel, 199);
				}
			} else {
				getPA().multiWay(-1);
				getPA().sendFrame126("@yel@Level: "+wildLevel, 199);
			}
			getPA().showOption(3, 0, "Attack", 1);
		} else if(this.inBH) {
			getPA().showOption(3, 0, "Attack", 1);
			getPA().walkableInterface(25347);
		} else if (inDuelArena()) {
			getPA().walkableInterface(201);
			if(duelStatus == 5) {
				getPA().showOption(3, 0, "Attack", 1);
			} else {
				getPA().showOption(3, 0, "Challenge", 1);
			}
		} else if(inPcBoat()) {
			getPA().walkableInterface(21119);
		} else if(inPcGame()) {
			getPA().walkableInterface(21100);
			//castlewars
		} else if (CastleWars.isInCw(this)) {
	        getPA().showOption(3, 0, "Attack", 1);
		} else if(inBarrows()){
			getPA().sendFrame99(2);
			getPA().sendFrame126("Kill Count: "+Barrows.getKillcount(this), 4536);
			getPA().walkableInterface(4535);
		} else if (gwdCoords()) {
			getPA().walkableInterface(16210);
			getPA(). sendFrame126("@cya@" + bandosKills, 16217);
			getPA(). sendFrame126("@cya@" + zamorakKills, 16219);
			getPA(). sendFrame126("@cya@" + saraKills, 16218);
			getPA(). sendFrame126("@cya@" + armaKills, 16216);
		} else if (inPits) {
			getPA().showOption(3, 0, "Attack", 1);	
		} else if (getPA().inPitsWait()) {
			getPA().showOption(3, 0, "Null", 1);
		} else if(Server.trawler.players.contains(this)) {
			getPA().walkableInterface(11908);
			//castlewars
	    } else if (!CastleWars.isInCwWait(this)) {
	        getPA().sendFrame99(0);
	        getPA().walkableInterface(-1);
	        getPA().showOption(3, 0, "Null", 1);
		}

		if(!hasMultiSign && inMulti()) {
			hasMultiSign = true;
			getPA().multiWay(1);
		}

		if(hasMultiSign && !inMulti()) {
			hasMultiSign = false;
			getPA().multiWay(-1);
		}

		if(skullTimer > 0) {
			skullTimer--;
			if(skullTimer == 1) {
				isSkulled = false;
				attackedPlayers.clear();
				headIconPk = -1;
				skullTimer = -1;
				getPA().requestUpdates();
			}	
		}

		if(isDead && respawnTimer == -6) {
			getPA().applyDead();
		}

		if(respawnTimer == 7) {
			respawnTimer = -6;
			getPA().giveLife();
		} else if(respawnTimer == 12) {
			respawnTimer--;
			startAnimation(0x900);
			poisonDamage = -1;
		}	

		if(respawnTimer > -6) {
			respawnTimer--;
		}
		if(freezeTimer > -6) {
			freezeTimer--;
			if (frozenBy > 0) {
				if (PlayerHandler.players[frozenBy] == null) {
					freezeTimer = -1;
					frozenBy = -1;
				} else if (!goodDistance(absX, absY, PlayerHandler.players[frozenBy].absX, PlayerHandler.players[frozenBy].absY, 20)) {
					freezeTimer = -1;
					frozenBy = -1;
				}
			}
		}

		if(hitDelay > 0) {
			hitDelay--;
		}
		if(teleTimer > 0) {
			teleTimer--;
			if (!isDead) {
				if(teleTimer == 1 && newLocation > 0) {
					teleTimer = 0;
					getPA().changeLocation();
				}
				if(teleTimer == 5) {
					teleTimer--;
					getPA().processTeleport();
				}
				if(teleTimer == 9 && teleGfx > 0) {
					teleTimer--;
					gfx100(teleGfx);
				}
			} else {
				teleTimer = 0;
			}
		}	

		if(hitDelay == 1) {
			if(oldNpcIndex > 0) {
				getCombat().delayedHit(oldNpcIndex);
			}
			if(oldPlayerIndex > 0) {
				getCombat().playerDelayedHit(oldPlayerIndex);				
			}		
		}

		if(attackTimer > 0) {
			attackTimer--;
		}

		if(attackTimer == 1){
			if(npcIndex > 0 && clickNpcType == 0) {
				getCombat().attackNpc(npcIndex);
			}
			if(playerIndex > 0) {
				getCombat().attackPlayer(playerIndex);
			}
		} else if (attackTimer <= 0 && (npcIndex > 0 || playerIndex > 0)) {
			if (npcIndex > 0) {
				attackTimer = 0;
				getCombat().attackNpc(npcIndex);
			} else if (playerIndex > 0) {
				attackTimer = 0;
				getCombat().attackPlayer(playerIndex);
			}
		}

		if(inTrade && tradeResetNeeded){
			Client o = (Client) PlayerHandler.players[tradeWith];
			if(o != null){
				if(o.tradeResetNeeded){
					getTradeAndDuel().resetTrade();
					o.getTradeAndDuel().resetTrade();
				}
			}
		}
	}
	
	public void addToHelpCc() {
		if (clan == null) {
			Clan localClan = Server.clanManager.getClan("help");
			if (localClan != null)
				localClan.addMember(this);
			else {
				sendMessage(Misc.formatPlayerName("help")
						+ " has not created a clan yet.");
			}
		}
	}

	public void setCurrentTask(Future<?> task) {
		currentTask = task;
	}

	public Future<?> getCurrentTask() {
		return currentTask;
	}

	public synchronized Stream getInStream() {
		return inStream;
	}

	public synchronized int getPacketType() {
		return packetType;
	}

	public synchronized int getPacketSize() {
		return packetSize;
	}

	public synchronized Stream getOutStream() {
		return outStream;
	}

	public ItemAssistant getItems() {
		return itemAssistant;
	}

	public PlayerAssistant getPA() {
		return playerAssistant;
	}

	public DialogueHandler getDH() {
		return dialogueHandler;
	}
	
	public Thieving getThieving() {
		return thieving;
	}
	
	public Farming getFarming() {
		return farming;
	}

	public ShopAssistant getShops() {
		return shopAssistant;
	}

	public TradeAndDuel getTradeAndDuel() {
		return tradeAndDuel;
	}

	public CombatAssistant getCombat() {
		return combatAssistant;
	}

	public ActionHandler getActions() {
		return actionHandler;
	}

	public PlayerKilling getKill() {
		return playerKilling;
	}

	public IoSession getSession() {
		return session;
	}

	public Potions getPotions() {
		return potions;
	}

	public PotionMixing getPotMixing() {
		return potionMixing;
	}

	public Food getFood() {
		return food;
	}

	private boolean isBusy = false;
	private boolean isBusyHP = false;
	public boolean isBusyFollow = false;

	public boolean checkBusy() {
		/*if (getCombat().isFighting()) {
			return true;
		}*/
		if (isBusy) {
			//actionAssistant.sendMessage("You are too busy to do that.");
		}
		return isBusy;
	}

	public boolean checkBusyHP() {
		return isBusyHP;
	}

	public boolean checkBusyFollow() {
		return isBusyFollow;
	}

	public void setBusy(boolean isBusy) {
		this.isBusy = isBusy;
	}
	
	public void isBusy(boolean isBusy) {
		this.isBusy = isBusy;
	}

	public void setBusyFollow(boolean isBusyFollow) {
		this.isBusyFollow = isBusyFollow;
	}

	public void setBusyHP(boolean isBusyHP) {
		this.isBusyHP = isBusyHP;
	}

	public boolean isBusyHP() {
		return isBusyHP;
	}
	public boolean isBusyFollow() {
		return isBusyFollow;
	}	

	public boolean canWalk = true;
	public long waitTime;

	public boolean canWalk() {
		return canWalk;
	}

	public void setCanWalk(boolean canWalk) {
		this.canWalk = canWalk;
	}

	public PlayerAssistant getPlayerAssistant() {
		return playerAssistant;
	}

	public DesertTreasure getDT() {
		return desertTreasure;
	}

	public HorrorFromTheDeep getHfd() {
		return horrorFromTheDeep;
	}

	public RecipeForDisaster getRfd() {
		return recipeForDisaster;
	}
	
	public TreasureTrails getTT() {
		return treasureTrails;
	}

	public DoricsQuest getDQ() {
		return doricsQuest;
	}
	
	public RangersGuild getRG() {
		return rangersGuild;
	}

	public SkillInterfaces getSI() {
		return skillInterfaces;
	}

	/**
	 * Skill Constructors
	 */
	public Slayer getSlayer() {
		return slayer;
	}

	public Agility getAgil() {
		return agility;
	}
	
	//castlewars
	public boolean inCw() {
		return CastleWars.isInCwWait(this) || CastleWars.isInCw(this);
	}

	/**
	 * End of Skill Constructors
	 */

	public void queueMessage(Packet arg1) {
		//synchronized(queuedPackets) {
			//if (arg1.getId() != 41)
			queuedPackets.add(arg1);
			//else
			//processPacket(arg1);
		//}
	}

	public boolean processQueuedPackets() {
		Packet p = null;
		//synchronized(queuedPackets) {
			p = queuedPackets.poll();
		//}
		if(p == null) {
			return false;
		}
		inStream.currentOffset = 0;
		packetType = p.getId();
		packetSize = p.getLength();
		inStream.buffer = p.getData();
		if(packetType > 0) {
			//sendMessage("PacketType: " + packetType);
			PacketHandler.processPacket(this, packetType, packetSize);
		}
		timeOutCounter = 0;
		return true;
	}

	public boolean processPacket(Packet p) {
		//synchronized (this) {
			if(p == null) {
				return false;
			}
			inStream.currentOffset = 0;
			packetType = p.getId();
			packetSize = p.getLength();
			inStream.buffer = p.getData();
			if(packetType > 0) {
				//sendMessage("PacketType: " + packetType);
				PacketHandler.processPacket(this, packetType, packetSize);
			}
			timeOutCounter = 0;
			return true;
		//}
	}
	
	public void correctCoordinates() {
		if (inPcGame()) {
			getPA().movePlayer(2657, 2639, 0);
		}
		if (inFightCaves()) {
			getPA().movePlayer(3087, 3500, 0);

		}

	}

}
