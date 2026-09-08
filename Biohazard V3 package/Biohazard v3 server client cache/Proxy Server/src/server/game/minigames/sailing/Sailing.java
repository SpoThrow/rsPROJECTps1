package server.game.minigames.sailing;

import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.players.Client;
import core.util.Misc;

public class Sailing {

	private static final int[][] TRAVEL_DATA = { 
		{2782, 3273, 5}, // 0 - fishing platform
		{2834, 3335, 15}, // 1 - From Port Sarim to Entrana
		{3048, 3234, 15}, // 2 - From Entrana to Port Sarim
		{2853, 3237, 12}, // 3 - From Port Sarim to Crandor
		{2834, 3335, 13}, // 4 - From Crandor to Port Sarim
		{2956, 3146, 8}, // 5 - From Port Sarim to Karajama
		{3029, 3217, 8}, // 6 - From Karajama to Port Sarim
		{2772, 3234, 3}, // 7 - From Ardougne to Brimhaven
		{3029, 3217, 3}, // 8 - From Brimhaven to Ardougne
		{2581, 3845, 5}, // 9 - miscellania
		{2629, 3692, 11}, // 10 - port sarim rellekka
		{3709, 3496, 23}, // 11 - From Port Khazard to port Phasmatys
		{2676, 3170, 23}, // 12 - From Ship Yard to Port Khazard
		{2804, 3421, 17}, // 13 - From Cairn Island to Catherby
		{2893, 2724, 12}, // 14 - From Port Sarim to Crash ISland
		{2550, 3759, 12}, // 15 - From Pest Control to Waterbirth
		{2805, 2707, 10}, // 16 - To Cairn Isle from Ape Atoll
	};

	public static void startTravel(final Client player, final int i) {
		player.getPA().showInterface(3281);
		player.getPA().sendFrame36(75, i);
		player.getPA().sendFrame99(2);
		player.getPA().movePlayer(0, 0, 0);
		player.canWalk = false;
		if(Misc.random(20) <= 1) {
			player.getPA().sendFrame36(75, -1);
			player.getPA().sendFrame99(0);
			player.canWalk = true;
			player.getPA().closeAllWindows();
			player.getPA().underWaterTele();
			return;
		}
		CycleEventHandler.addEvent(player, new CycleEvent() {
			@Override //player.underWater();
			public void execute(CycleEventContainer container) {
				player.getPA().movePlayer(getX(i), getY(i), 0);
				player.getPA().sendFrame36(75, -1);
				player.getPA().sendFrame99(0);
				player.canWalk = true;
				player.getPA().closeAllWindows();
				container.stop();
			}
			@Override
			public void stop() {
			}
		}, getTime(i));
		
		/*CycleEventHandler.addEvent(player, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				player.getPA().sendFrame36(75, -1);
				player.getPA().closeAllWindows();
				container.stop();
			}
			@Override
			public void stop() {
			}
		}, getTime(i));*/
	}

	public static int getX(int i) {
		return TRAVEL_DATA[i][0];
	}

	public static int getY(int i) {
		return TRAVEL_DATA[i][1];
	}
	
	public static int getTime(int i) {
		return TRAVEL_DATA[i][2];
	}

}