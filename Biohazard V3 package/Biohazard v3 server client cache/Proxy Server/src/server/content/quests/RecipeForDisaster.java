package server.content.quests;

import server.Server;
import server.game.players.Client;

/**
* Recipe for Disaster
* @author Acquittal
*/

public class RecipeForDisaster {

	public Client client;
	
	public RecipeForDisaster(Client client) {
		this.client = client;
	}
	
	public void changeRfdRound() {
		if(client != null) {
			client.spawned = false;
			switch(client.roundNpc) {
				case 1:
				client.roundNpc = 2;
				client.rfdRound = client.rfdRound == 0 ? 1 : client.rfdRound;
				break;
				
				case 2:
				client.roundNpc = 3;
				client.rfdRound = client.rfdRound == 1 ? 2 : client.rfdRound;
				break;
				
				case 3:
				client.roundNpc = 4;
				client.rfdRound = client.rfdRound == 2 ? 3 : client.rfdRound;
				break;
				
				case 4:
				client.roundNpc = 5;
				client.rfdRound = client.rfdRound == 3 ? 4 : client.rfdRound;
				break;
			}
		}
	}
	
	public void endRfd() {
		if(client != null) {
			client.spawned = false;
			client.roundNpc = 0;
			client.getPA().movePlayer(3218, 9621, 0);
			client.height = 0;
		}
	}
	
	public void startRfd() {
		if(client != null) {
			client.height = (client.playerId * 4) + 2;
			client.getPA().removeAllWindows();
			client.getPA().movePlayer(1898, 5361, client.height);
			client.roundNpc = 1;
			client.spawned = true;
			Server.npcHandler.spawnNpc(client, 3493, 1899, 5354, client.height, 1, 200, 17, 70, 60, true, true);
		}
	}
	
	public void showInformation() {
		for(int i = 8144; i < 8195; i++) {
			client.getPA().sendFrame126("", i);
		}
		client.getPA().sendFrame126("@dre@Recipe for Disaster", 8144);
		client.getPA().sendFrame126("", 8145);
		if(client.rfdRound == 0) {
			client.getPA().sendFrame126("Talk to Sir Amik Varze in Lumbridge", 8147);
			client.getPA().sendFrame126("Dungeon to start this quest.", 8148);
		} else if(client.rfdRound > 0 && client.rfdRound < 4) {
			client.getPA().sendFrame126("@str@Talk to Sir Amik Varze in Lumbridge", 8147);
			client.getPA().sendFrame126("@str@Dungeon to start this quest.", 8148);
			client.getPA().sendFrame126("", 8149);
			client.getPA().sendFrame126("Talk to Sir Amik Varze and he will", 8150);
			client.getPA().sendFrame126("Teleport you back to the game.", 8151);
		} else if(client.rfdRound == 4) {
			client.getPA().sendFrame126("@str@Talk to Sir Amik Varze in Lumbridge", 8147);
			client.getPA().sendFrame126("@str@Dungeon to start this quest.", 8148);
			client.getPA().sendFrame126("", 8149);
			client.getPA().sendFrame126("@str@Talk to Sir Amik Varze and he will", 8150);
			client.getPA().sendFrame126("@str@Teleport you back to the game.", 8151);
			client.getPA().sendFrame126("", 8152);
			client.getPA().sendFrame126("You have completed this quest!", 8153);
		}
		client.getPA().showInterface(8134);
	}
	
}
