package server.game.players.packets;

import server.Server;
import server.game.players.Client;
import server.game.players.PacketType;
import server.world.Clan;
import core.util.Misc;

public class JoinChat implements PacketType {
	@Override
	public void processPacket(Client paramClient, int paramInt1, int paramInt2) {
		String str = Misc.longToPlayerName2(
				paramClient.getInStream().readQWord()).replaceAll("_", " ");
		if (paramClient.isBanking && (paramClient.awaitingBankSearch || paramClient.bankSearching)) {
			if (str != null && str.length() > 0) {
				paramClient.getBank().applySearch(str);
			} else {
				paramClient.getBank().clearSearch();
			}
			return;
		}
		if (paramClient.posSearchingItem || paramClient.posSearchingPlayer) {
			if (str != null && str.length() > 0) {
				if (paramClient.posSearchingItem) {
					paramClient.posSearchingItem = false;
					paramClient.posSearchingPlayer = false;
					paramClient.getPA().searchPOSByItemName(str);
				} else {
					paramClient.posSearchingItem = false;
					paramClient.posSearchingPlayer = false;
					paramClient.getPA().searchPOSByPlayer(str);
				}
			} else {
				paramClient.posSearchingItem = false;
				paramClient.posSearchingPlayer = false;
				paramClient.sendMessage("Search cancelled.");
			}
			return;
		}
		if ((str != null) && (str.length() > 0) && (paramClient.clan == null)) {
			Clan localClan = Server.clanManager.getClan(str);
			if (localClan != null)
				localClan.addMember(paramClient);
			else if (str.equalsIgnoreCase(paramClient.playerName))
				Server.clanManager.create(paramClient);
			else {
				paramClient.sendMessage(Misc.formatPlayerName(str)
						+ " has not created a clan yet.");
			}
		}
	}
}