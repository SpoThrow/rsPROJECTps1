package server.game.players;

import java.net.InetSocketAddress;

import server.Config;
import server.Server;
import server.game.npcs.NPCHandler;
import core.util.Misc;
import core.util.Stream;

/**
 * Owns the player array and drives one tick of every logged-in player.
 *
 * <p><b>Threading (Phase 5).</b> Everything in this class runs on the single game thread, which is
 * the thread the ticker in {@link Server} owns. The Mina I/O threads never call in here except for
 * {@link #newPlayerClient(Client)}, which takes the {@code players} monitor so slot discovery and
 * publication are atomic against {@link #process()}. Inbound packets are not handled here at all:
 * each client's I/O thread appends to its own concurrent queue and {@link #process()} drains it,
 * which is the "I/O threads enqueue, game thread drains" rule made concrete.
 */
public class PlayerHandler{



	public static Client[] players = new Client[Config.MAX_PLAYERS];
	public static String messageToAll = "";
	public static int playerCount = 0;
	public static String playersCurrentlyOn[] = new String[Config.MAX_PLAYERS];
	public static boolean updateAnnounced;
	public static boolean updateRunning;
	public static int updateSeconds;
	public static long updateStartTime;
	private boolean kickAllPlayers = false;

	static {
		for(int i = 0; i < Config.MAX_PLAYERS; i++)
			players[i] = null;
	}
	
	public static Client getPlayer(String name) {
		for (int d = 0; d < Config.MAX_PLAYERS; d++) {
			if (PlayerHandler.players[d] != null) {
				Client p = PlayerHandler.players[d];
				if (p.playerName.equalsIgnoreCase(name)) {
					return p;
				}
			}
		}
		return null;
	}

	public boolean newPlayerClient(Client client1)
	{
		// Called from a Mina I/O thread. Slot discovery and publication must be atomic
		// against process(), otherwise two concurrent logins can claim the same slot.
		synchronized (PlayerHandler.players) {
			for(int i = 1; i < Config.MAX_PLAYERS; i++) {
				if(players[i] != null && !players[i].disconnected
						&& players[i].playerName != null
						&& players[i].playerName.equalsIgnoreCase(client1.playerName)) {
					return false;
				}
			}
			int slot = -1;
			for(int i = 1; i < Config.MAX_PLAYERS; i++) {
				if((players[i] == null) || players[i].disconnected) {
					slot = i;
					break;
				}
			}
			if(slot == -1)
				return false;
			client1.handler = this;
			client1.playerId = slot;
			players[slot] = client1;
			players[slot].isActive = true;
			players[slot].connectedFrom = ((InetSocketAddress) client1.getSession().getRemoteAddress()).getAddress().getHostAddress();
			if(Config.SERVER_DEBUG)	
				Misc.println("Player Slot "+slot+" slot 0 "+players[0]+" Player Hit "+players[slot]);
			return true;
		}
	}

	public static int getPlayerCount() {
		return playerCount;
	}


	public static boolean isPlayerOn(String playerName) {
		synchronized (PlayerHandler.players) {
			for (int i = 0; i < Config.MAX_PLAYERS; i++) {
				if(playersCurrentlyOn[i] != null){
					if(playersCurrentlyOn[i].equalsIgnoreCase(playerName)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * One tick for every logged-in player.
	 *
	 * <p><b>The order of the two passes is load-bearing and is now stated here rather than in a
	 * comment beside one loop.</b> Pass 1 runs the per-player tick
	 * ({@code packets → process → merge walk → step → follow+swing}): a teleport or walk request
	 * drained in step 2 has to be applied before step 4 moves the player, and the combat swing in
	 * step 6 has to see the position step 4 produced. Pass 2 then initialises a player who has just
	 * connected or sends this tick's update to everybody else — kept separate because a player
	 * initialised in pass 2 must not also have been stepped in pass 1.
	 *
	 * <p>The release check is asked once per player per pass, through {@link #shouldRelease}, and
	 * the exit itself goes through {@link #releasePlayer}. It used to be spelled out twice, with
	 * identical and therefore dead duplicate code in pass 2 (pass 1 had already nulled the slot,
	 * so pass 2's copy could never run) — and both copies saved the character <em>and</em> then
	 * called {@link Client#destruct()}, which saves again.
	 */
	public void process() {
		synchronized (PlayerHandler.players) {
			updatePlayerNames();

			if(kickAllPlayers) {
				for(int i = 1; i < Config.MAX_PLAYERS; i++) {
					if(players[i] != null) {
						players[i].disconnected = true;
					}
				}
			}

			// Pass 1: packets → timers/hits → merge walk → step → follow+swing
			for(int i = 0; i < Config.MAX_PLAYERS; i++) {
				Client player = players[i];
				if(player == null || !player.isActive) continue;
				try {
					if(shouldRelease(player)) {
						releasePlayer(i, player);
						continue;
					}

					player.preProcessing();
					while(player.processQueuedPackets());
					player.process();
					player.postProcessing();
					player.getNextPlayerMovement();
					player.processCombatAfterMovement();

				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			// Pass 2: initialise a new login, or send the update to everyone already in the world.
			for(int i = 0; i < Config.MAX_PLAYERS; i++) {
				Client player = players[i];
				if(player == null || !player.isActive) continue;
				try {
					if(!player.initialized) {
						player.initialize();
						player.initialized = true;
					} else {
						player.update();
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			if(updateRunning && !updateAnnounced) {
				updateAnnounced = true;
				Server.UpdateServer = true;
			}
			if(updateRunning && (System.currentTimeMillis() - updateStartTime > (updateSeconds*1000))) {
				kickAllPlayers  = true;
			}

			for(int i = 0; i < Config.MAX_PLAYERS; i++) {
				if(players[i] == null || !players[i].isActive) continue;
				try {
					players[i].clearUpdateFlags();
				} catch(Exception e) {
					e.printStackTrace();
				}	
			}
		}
	}

	/**
	 * Whether this player should leave the world on this tick: a disconnected client whose logout
	 * hold has expired, one that logged out properly, or anybody at all once an update-kick runs.
	 * The update-kick clause is why the check cannot live only in the logout packet — it is what
	 * gives a scheduled restart a way to drop players who are still in combat.
	 */
	private boolean shouldRelease(Client player) {
		return player.disconnected
				&& (System.currentTimeMillis() - player.timers.logoutDelay > 10000
						|| player.properLogout
						|| kickAllPlayers);
	}

	/**
	 * The single exit from the world: settle any trade or duel the leaving player was in, then free
	 * the slot. {@link #removePlayer} runs {@link Client#destruct()}, which performs the one
	 * character write — so this method deliberately does not save as well.
	 */
	private void releasePlayer(int index, Client player) {
		if(player.inTrade) {
			Client o = (Client) PlayerHandler.players[player.tradeWith];
			if(o != null) {
				o.getTradeAndDuel().declineTrade();
			}
		}
		if(player.duelStatus == 5) {
			Client o = (Client) PlayerHandler.players[player.duelingWith];
			if(o != null) {
				o.getTradeAndDuel().duelVictory();
			}
		} else if (player.duelStatus <= 4 && player.duelStatus >= 1) {
			Client o = (Client) PlayerHandler.players[player.duelingWith];
			if(o != null) {
				o.getTradeAndDuel().declineDuel();
			}
		}
		removePlayer(player);
		players[index] = null;
	}

	/**
	 * Writes every logged-in character to disk, at most once each.
	 *
	 * <p>This is what the server's shutdown hook calls, so stopping the server always persists
	 * everyone. It is safe to call after the logout path has already written some of them, because
	 * {@link Client#saveCharacterOnce()} is what actually performs the write and it will not write
	 * the same character twice.
	 *
	 * @return how many characters this call actually wrote
	 */
	public static int saveAllPlayers() {
		int saved = 0;
		for (Client player : players) {
			if (player == null) {
				continue;
			}
			if (player.saveCharacterOnce()) {
				saved++;
			}
		}
		return saved;
	}

	public void updateNPC(Player plr, Stream str) {
		synchronized(plr) {
			updateBlock.currentOffset = 0;

			str.createFrameVarSizeWord(65);
			str.initBitAccess();

			str.writeBits(8, plr.npcListSize);
			int size = plr.npcListSize;
			plr.npcListSize = 0;
			for(int i = 0; i < size; i++) {
				if(plr.RebuildNPCList == false && plr.withinDistance(plr.npcList[i]) == true) {
					plr.npcList[i].updateNPCMovement(str);
					plr.npcList[i].appendNPCUpdateBlock(updateBlock);
					plr.npcList[plr.npcListSize++] = plr.npcList[i];
				} else {
					int id = plr.npcList[i].npcId;
					plr.npcInListBitmap[id>>3] &= ~(1 << (id&7));		
					str.writeBits(1, 1);
					str.writeBits(2, 3);		
				}
			}


			for(int i = 0; i < NPCHandler.maxNPCs; i++) {
				if(NPCHandler.npcs[i] != null) {
					int id = NPCHandler.npcs[i].npcId;
					if (plr.RebuildNPCList == false && (plr.npcInListBitmap[id>>3]&(1 << (id&7))) != 0) {

					} else if (plr.withinDistance(NPCHandler.npcs[i]) == false) {

					} else {
						plr.addNewNPC(NPCHandler.npcs[i], str, updateBlock);
					}
				}
			}

			plr.RebuildNPCList = false;

			if(updateBlock.currentOffset > 0) {
				str.writeBits(14, 16383);	
				str.finishBitAccess();
				str.writeBytes(updateBlock.buffer, updateBlock.currentOffset, 0);
			} else {
				str.finishBitAccess();
			}
			str.endFrameVarSizeWord();
		}
	}

	private Stream updateBlock = new Stream(new byte[Config.BUFFER_SIZE]);

	/**
	 * Serialises the player's own movement and update block, then the blocks of every nearby player.
	 *
	 * <p>Called only from {@link server.game.players.Client#update()}, which {@link #process()} runs
	 * on the game thread while holding the {@code players} monitor — so no extra locking is needed
	 * here. The {@code synchronized (plr)} this method used to carry is commented out directly above
	 * and below the body; it was disabled before Phase 5 and is left disabled deliberately, because
	 * {@link Client#logout()} also synchronises on the client and nesting the two monitors the other
	 * way round would invite a deadlock for no benefit.
	 */
	public void updatePlayer(Player plr, Stream str) {
		//synchronized(plr) {
			updateBlock.currentOffset = 0;
			if(updateRunning && !updateAnnounced) {
				str.createFrame(114);
				str.writeWordBigEndian(updateSeconds*50/30);
			}
			plr.updateThisPlayerMovement(str);		
			boolean saveChatTextUpdate = plr.isChatTextUpdateRequired();
			plr.setChatTextUpdateRequired(false);
			plr.appendPlayerUpdateBlock(updateBlock);
			plr.setChatTextUpdateRequired(saveChatTextUpdate);
			str.writeBits(8, plr.playerListSize);
			int size = plr.playerListSize;
			if (size > 255)
				size = 255;
			plr.playerListSize = 0;	
			for(int i = 0; i < size; i++) {			
				if(!plr.didTeleport && !plr.playerList[i].didTeleport && plr.withinDistance(plr.playerList[i])) {
					plr.playerList[i].updatePlayerMovement(str);
					plr.playerList[i].appendPlayerUpdateBlock(updateBlock);
					plr.playerList[plr.playerListSize++] = plr.playerList[i];
				} else {
					int id = plr.playerList[i].playerId;
					plr.playerInListBitmap[id>>3] &= ~(1 << (id&7));
					str.writeBits(1, 1);
					str.writeBits(2, 3);
				}
			}

			for(int i = 0; i < Config.MAX_PLAYERS; i++) {
				if(players[i] == null || !players[i].isActive || players[i] == plr)
					continue;
				int id = players[i].playerId;
				if((plr.playerInListBitmap[id>>3]&(1 << (id&7))) != 0)
					continue;	
				if(!plr.withinDistance(players[i])) 
					continue;		
				plr.addNewPlayer(players[i], str, updateBlock);
			}

			if(updateBlock.currentOffset > 0) {
				str.writeBits(11, 2047);	
				str.finishBitAccess();				
				str.writeBytes(updateBlock.buffer, updateBlock.currentOffset, 0);
			}
			else str.finishBitAccess();

			str.endFrameVarSizeWord();
			//castlewars
			if(plr.updateRegion) {
				Server.objectManager.loadObjects((Client)plr);
				plr.updateRegion = false;
			}
			//end
		//}
	}
	
	public static void updatePlayerNames() {
        playerCount = 0;
        for (int i = 0; i < Config.MAX_PLAYERS; i++) {
            if (players[i] != null) {
                playersCurrentlyOn[i] = players[i].playerName;
                playerCount++;
            } else {
                playersCurrentlyOn[i] = "";
            }
        }
    }

	private void removePlayer(Player plr) {
		if (plr instanceof Client) {
			server.game.content.DwarfCannon.logout((Client) plr);
		}
		if(plr.privateChat != 2) { 
			for(int i = 1; i < Config.MAX_PLAYERS; i++) {
				if (players[i] == null || players[i].isActive == false) continue;
				Client o = (Client)PlayerHandler.players[i];
				if(o != null) {
					o.getPA().updatePM(plr.playerId, 0);
				}
			}
		}
		plr.destruct();
	}

}
