package server.content.skills;

import server.Config;
import server.Server;
import server.clip.region.Region;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.objects.Object;
import server.game.players.Client;
import server.world.Tile;

/**
 * @author Acquittal - Done
 **/

public class Firemaking {

	Client c;

	public Firemaking(Client c) {
		this.c = c;
	}

	@SuppressWarnings("unused")
	private static Tile currentTile;

	private static int[][] logsdata = {
		{1511, 1,  40,  2732},
		{2862, 1,  40,  2732},
		{1521, 15, 60,  2732},
		{1519, 30, 105, 2732},
		{6333, 35, 105, 2732},
		{1517, 45, 135, 2732},
		{10810,45, 135, 2732},
		{6332, 50, 158, 2732},
		{1515, 60, 203, 2732},
		{1513, 75, 304, 2732},
		{7404, 1, 256, 11404},
		{7405, 1, 256, 11405},
		{7406, 1, 256, 11406},
		{10328, 1, 256, 20000},
		{10329, 1, 256, 20001},
	};

	public static boolean playerLogs(Client c, int i, int l) {
		boolean flag = false;
		for(int kl = 0; kl < logsdata.length; kl++) {
			if((i == logsdata[kl][0] && requiredItem(c, l)) || (requiredItem(c, i) && l == logsdata[kl][0])) {
				flag = true;
			}
		}
		return flag;
	}

	private static int getAnimation(Client c, int item, int item1) {
		int[][] data = {{841, 6714}, {843, 6715}, {849, 6716}, {853, 6717},
				{857, 6718}, {861, 6719}};
		for(int i = 0; i < data.length; i++) {
			if(item == data[i][0] || item1 == data[i][0]) {
				return data[i][1];
			}
		}
		return 733;
	}

	private static boolean requiredItem(Client c, int i) {
		int[] data = {841, 843, 849, 853, 857, 861, 590};
		for(int l = 0; l < data.length; l++) {
			if(i == data[l]) {
				return true;
			}
		}
		return false;
	}

	public static void grabData(final Client c, final int useWith, final int withUse) {
		final int[] coords = new int[2];
		coords[0] = c.absX;
		coords[1] = c.absY;
		for(int i = 0; i < logsdata.length; i++) {
			if((requiredItem(c, useWith) && withUse == logsdata[i][0] || useWith == logsdata[i][0] && requiredItem(c, withUse))) {
				if(c.playerLevel[11] < logsdata[i][1]) {
					c.sendMessage("You need a higher firemaking level to light this log!");
					return;
				}
				/*if(coords[0] == o.getObjectX() && coords[1] == o.getObjectY() && o.getObjectId() > 0) {
					c.sendMessage("You can't light a fire here.");
					return;
				}*/
				if (System.currentTimeMillis() - c.lastFire > 1200) {

					if(c.playerIsFiremaking) {
						return;
					}

					final int[] time = new int[3];
					final int log = logsdata[i][0];
					final int fire = logsdata[i][3];
					if(System.currentTimeMillis() - c.lastFire > 3000) {
						c.startAnimation(getAnimation(c, useWith, withUse));
						time[0] = 4;
						time[1] = 3;
					} else {
						time[0] = 1;
						time[1] = 2;
					}

					c.playerIsFiremaking = true;

					Server.itemHandler.createGroundItem(c, log, coords[0], coords[1], 1, c.getId());

					CycleEventHandler.addEvent(c, new CycleEvent() {
						@Override
						public void execute(CycleEventContainer container) {
							new Object(fire, coords[0], coords[1], c.heightLevel, 0, 10, fire, -1, 1);
							Server.itemHandler.removeGroundItem(c, log, coords[0], coords[1], false);
							c.playerIsFiremaking = false;
							c.fmPoints++;
							c.getPA().sendFrame126("@red@Firemaking Points: @or2@"+c.fmPoints, 7334);
							container.stop();
						}
						@Override
						public void stop() {

						}
					}, time[0]);

					currentTile = new Tile(c.absX - 1, c.absY, c.heightLevel);

					if (Region.getClipping(c.getX() - 1, c.getY(), c.heightLevel, -1, 0)) {
						c.getPA().walkTo(-1, 0);
					} else if (Region.getClipping(c.getX() + 1, c.getY(), c.heightLevel, 1, 0)) {
						c.getPA().walkTo(1, 0);
					} else if (Region.getClipping(c.getX(), c.getY() - 1, c.heightLevel, 0, -1)) {
						c.getPA().walkTo(0, -1);
					} else if (Region.getClipping(c.getX(), c.getY() + 1, c.heightLevel, 0, 1)) {
						c.getPA().walkTo(0, 1);
					}
					c.sendMessage("You light the logs.");
					CycleEventHandler.addEvent(c, new CycleEvent() {
						@Override
						public void execute(CycleEventContainer container) {
							c.startAnimation(65535);
							container.stop();
						}
						@Override
						public void stop() {

						}
					}, time[1]);
					CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
						public void execute(CycleEventContainer container) {
							new Object(-1, coords[0], coords[1], 0, 0, 10, -1, -1, 1);
							new Object(-1, coords[0], coords[1], 1, 0, 10, -1, -1, 1);
							new Object(-1, coords[0], coords[1], 2, 0, 10, -1, -1, 1);
							new Object(-1, coords[0], coords[1], 3, 0, 10, -1, -1, 1);
							//Server.objectHandler.createAnObject(c, -1, coords[0], coords[1]);
							Server.itemHandler.createGroundItem(c, 592, coords[0], coords[1], 1, c.getId());
							container.stop();
						}
						@Override
						public void stop() {

						}
					}, 120);
					c.getPA().addSkillXP(logsdata[i][2] * Config.FIREMAKING_EXPERIENCE, 11);
					c.getItems().deleteItem(logsdata[i][0], c.getItems().getItemSlot(logsdata[i][0]), 1);
					c.turnPlayerTo(c.absX+1, c.absY);
					c.lastFire = System.currentTimeMillis();
				}
			}
		}
	}
}