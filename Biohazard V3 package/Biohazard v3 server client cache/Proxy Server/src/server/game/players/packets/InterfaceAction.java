package server.game.players.packets;

import server.game.players.Client;
import server.game.players.PacketType;
import server.game.players.PlayerSave;
import server.world.Clan;
import core.util.Misc;

public class InterfaceAction implements PacketType {

	@Override
	public void processPacket(Client player, int packetType, int packetSize) {
		int id = player.getInStream().readUnsignedWord();
		int action = player.getInStream().readUnsignedWord();
		switch (id) {
		case 50304:
			if (action == 1) {
				player.getPA().getClan().delete();
				player.getPA().setClanData();
			}
			break;
		case 50307:
		case 50310:
		case 50313:
		case 50316:
			Clan clan = player.getPA().getClan();
			if (clan != null) {
				if (id == 50307) {
					clan.setRankCanJoin(action == 0 ? -1 : action);
				} else if (id == 50310) {
					clan.setRankCanTalk(action == 0 ? -1 : action);
				} else if (id == 50313) {
					clan.setRankCanKick(action == 0 ? -1 : action);
				} else if (id == 50316) {
					clan.setRankCanBan(action == 0 ? -1 : action);
				}
				String title = "";
				if (id == 50307) {
					title = clan.getRankTitle(clan.whoCanJoin)
							+ (clan.whoCanJoin > Clan.Rank.ANYONE
									&& clan.whoCanJoin < Clan.Rank.OWNER ? "+"
									: "");
				} else if (id == 50310) {
					title = clan.getRankTitle(clan.whoCanTalk)
							+ (clan.whoCanTalk > Clan.Rank.ANYONE
									&& clan.whoCanTalk < Clan.Rank.OWNER ? "+"
									: "");
				} else if (id == 50313) {
					title = clan.getRankTitle(clan.whoCanKick)
							+ (clan.whoCanKick > Clan.Rank.ANYONE
									&& clan.whoCanKick < Clan.Rank.OWNER ? "+"
									: "");
				} else if (id == 50316) {
					title = clan.getRankTitle(clan.whoCanBan)
							+ (clan.whoCanBan > Clan.Rank.ANYONE
									&& clan.whoCanBan < Clan.Rank.OWNER ? "+"
									: "");
				}
				player.getPA().sendFrame126(title, id + 2);
			}
			break;

		default:
			// System.out.println("Interface action: [id=" + id +",action=" +
			// action +"]");
			break;
		}
		if (id >= 50323 && id < 50423) {
			Clan clan = player.getPA().getClan();
			if (clan != null && clan.rankedMembers != null
					&& !clan.rankedMembers.isEmpty()) {
				String member = clan.rankedMembers.get(id - 50323);
				switch (action) {
				case 0:
					clan.demote(member);
					break;
				default:
					clan.setRank(member, action);
					break;
				}
				player.getPA().setClanData();
			}
		}
		if (id >= 50424 && id < 50524) {
			Clan clan = player.getPA().getClan();
			if (clan != null && clan.bannedMembers != null
					&& !clan.bannedMembers.isEmpty()) {
				String member = clan.bannedMembers.get(id - 50424);
				switch (action) {
				case 0:
					clan.unbanMember(member);
					break;
				}
				player.getPA().setClanData();
			}
		}
		if (id >= 50144 && id < 50244) {
			for (int index = 0; index < 100; index++) {
				if (id == index + 50144) {
					String member = player.clan.activeMembers.get(id - 50144);
					switch (action) {
					case 0:
						if (player.clan.isFounder(player.playerName)) {
							player.getPA().showInterface(50300);
						}
						break;
					case 1:
						if (member.equalsIgnoreCase(player.playerName)) {
							player.sendMessage("You can't kick yourself!");
						} else {
							if (player.clan.canKick(player.playerName)) {
								player.clan.kickMember(member);
							} else {
								player.sendMessage("You do not have sufficient privileges to do this.");
							}
						}
						break;
					case 2:
						if (member.length() == 0) {
							break;
						} else if (member.length() > 12) {
							member = member.substring(0, 12);
						}
						if (member.equalsIgnoreCase(player.playerName)) {
							break;
						}
						if (!PlayerSave.playerExists(member)) {
							player.sendMessage("This player doesn't exist!");
							break;
						}
						Clan clan = player.getPA().getClan();
						if (clan.isRanked(member)) {
							player.sendMessage("You cannot ban a ranked member.");
							break;
						}
						if (clan != null) {
							clan.banMember(Misc.formatPlayerName(member));
							player.getPA().setClanData();
							clan.save();
						}
						break;
					}
					break;
				}
			}
		}
	}
}