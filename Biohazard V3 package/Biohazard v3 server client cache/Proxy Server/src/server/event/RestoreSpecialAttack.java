package server.event;

import server.game.players.Client;

public class RestoreSpecialAttack {
	
	public static void execute(final Client player){
		player.isRestoringSpec = true;
		CycleEventHandler.addEvent(player, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
					if (player.specAmount < 100) {
						player.specAmount += 10;
						if (player.specAmount >= 100) {
							player.specAmount = 100;
							container.stop();
						}
						player.getItems().addSpecialBar(player.playerEquipment[player.playerWeapon]);
					}
			}
			@Override
			public void stop() {
				player.isRestoringSpec = false;
			}
		}, 29);
	}

}
