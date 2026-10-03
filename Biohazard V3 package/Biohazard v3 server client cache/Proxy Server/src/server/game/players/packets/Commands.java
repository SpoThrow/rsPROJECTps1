package server.game.players.packets;

import server.Config;
import server.clip.region.Region;
import server.clip.region.SmartPathFinder;
import server.game.players.Client;
import server.game.players.packets.commands.CommandHandler;
import core.util.Misc;


public class Commands implements PacketType {
	public boolean resetAnim = false;

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		String playerCommand = c.getInStream().readString();
		if (Config.SERVER_DEBUG) {
			Misc.println(c.playerName + " playerCommand: " + playerCommand);
		}
		if (playerCommand.startsWith("/")) {
			if (c.clan != null) {
				String message = playerCommand.substring(1);
				c.clan.sendChat(c, message);
			}
		}
		// ::noclip is a client-side command, but the old chain carried a server-side
		// guard for it anyway -- written at the bottom of the `yell` block, where it
		// could never fire. It belongs here, before dispatch.
		if (blocksNoclip(c, playerCommand)) {
			return;
		}
		CommandHandler.dispatch(c, playerCommand);
	}

	/**
	 * The guard the old chain had for ::noclip, lifted out of the unreachable spot at
	 * the bottom of the `yell` block. Owner-only, because noclip is a client-side
	 * command that anyone else should not be able to ask the server about.
	 *
	 * @return true when the command should be dropped without dispatch
	 */
	public static boolean blocksNoclip(Client c, String playerCommand) {
		return playerCommand.startsWith("noclip") && c.playerRights != 3;
	}

	public static void handleItemSpawnCommand(Client c, String playerCommand) {
		String[] args = playerCommand.split(" ");
		if (args.length >= 3) {
			try {
				int newItemID = Integer.parseInt(args[1]);
				int newItemAmount = Integer.parseInt(args[2]);
				if ((newItemID <= 20000) && (newItemID >= 0)) {
					c.getItems().addItem(newItemID, newItemAmount);
					c.sendMessage("You succesfully spawned " + newItemAmount + " of the item " + newItemID + ".");
					System.out.println("Spawned: " + newItemID + " by: " + Misc.capitalize(c.playerName));
				} else {
					c.sendMessage("Could not complete spawn request.");
				}
				return;
			} catch (NumberFormatException ignored) {
			}
		}
		String query = playerCommand.length() > 5 ? playerCommand.substring(5).trim() : "";
		server.game.content.ItemSpawnSearch.start(c, query);
	}

	/**
	 * ::clip — feet + last walk dest (white X).
	 * ::clip x y — specific tile.
	 * ::clip x y z — specific tile + height.
	 */
	public static void handleClipDebug(Client c, String playerCommand) {
		int z = c.position.heightLevel;
		int feetX = c.position.absX;
		int feetY = c.position.absY;
		int destX = c.walkRepath.lastWalkDestX;
		int destY = c.walkRepath.lastWalkDestY;
		String[] parts = playerCommand.trim().split("\\s+");
		if (parts.length >= 3) {
			try {
				destX = Integer.parseInt(parts[1]);
				destY = Integer.parseInt(parts[2]);
				if (parts.length >= 4) {
					z = Integer.parseInt(parts[3]);
				}
			} catch (NumberFormatException e) {
				c.sendMessage("Usage: ::clip  OR  ::clip x y  OR  ::clip x y z");
				return;
			}
		}
		c.sendMessage("--- Clip debug (server Region matrix) ---");
		sendClipTile(c, "Feet", feetX, feetY, z);
		if (destX > 0 && destY > 0) {
			sendClipTile(c, "Dest", destX, destY, z);
			int cheb = Math.max(Math.abs(destX - feetX), Math.abs(destY - feetY));
			int[][] path = SmartPathFinder.get().route(feetX, feetY, destX, destY, z, true, 1, 1);
			int pathLen = path == null ? -1 : path.length;
			boolean reaches = false;
			if (path != null && pathLen > 0) {
				reaches = path[pathLen - 1][0] == destX && path[pathLen - 1][1] == destY;
			}
			c.sendMessage("Dist cheb=" + cheb + " Route: "
					+ (pathLen < 0 ? "NULL" : pathLen + " waypoints")
					+ " endsAtDest=" + reaches
					+ (pathLen >= 48 ? " (at waypoint cap)" : ""));
			c.sendMessage("Proj LoS feet->dest: "
					+ server.game.players.PathFinder.hasLineOfSight(feetX, feetY, 1, destX, destY, 1, z)
					+ " (false = solid tree/wall on Bresenham line)");
		} else {
			c.sendMessage("Dest: (none — click a tile first, or ::clip x y)");
		}
		c.sendMessage("canStep from feet N/E/S/W/NE/NW/SE/SW: "
				+ stepFlags(feetX, feetY, z));
		System.out.println("[ClipDebug] " + c.playerName + " feet=" + feetX + "," + feetY
				+ " dest=" + destX + "," + destY + " z=" + z
				+ " feetClip=0x" + Integer.toHexString(Region.getClipping(feetX, feetY, z))
				+ (destX > 0 ? " destClip=0x" + Integer.toHexString(Region.getClipping(destX, destY, z)) : ""));
	}

	private static void sendClipTile(Client c, String label, int x, int y, int z) {
		int clip = Region.getClipping(x, y, z);
		boolean walkSolid = (clip & 0x100) != 0 || (clip & 0x20000) != 0;
		boolean projSolid = (clip & 0x20000) != 0;
		boolean floorDeco = (clip & 0x200000) != 0;
		c.sendMessage(label + " " + x + "," + y + "," + z
				+ " clip=0x" + Integer.toHexString(clip)
				+ " walkSolid=" + walkSolid
				+ " projSolid=" + projSolid
				+ (floorDeco ? " floorDeco" : ""));
	}

	private static String stepFlags(int x, int y, int z) {
		int[][] d = new int[][] {
			{ 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 },
			{ 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 }
		};
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < d.length; i++) {
			if (i > 0) {
				sb.append('/');
			}
			sb.append(SmartPathFinder.canStep(x, y, d[i][0], d[i][1], z) ? 'Y' : 'N');
		}
		return sb.toString();
	}
}
