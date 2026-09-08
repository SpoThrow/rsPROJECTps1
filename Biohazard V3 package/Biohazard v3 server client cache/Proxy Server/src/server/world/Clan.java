package server.world;

import java.util.Collections;
import java.util.LinkedList;

import server.Server;
import server.game.players.Client;
import server.game.players.PlayerHandler;
import server.game.players.PlayerSave;
import core.util.Misc;
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Clan {
	public String title;
	public String founder;

	public LinkedList<String> activeMembers = new LinkedList();
	public LinkedList<String> bannedMembers = new LinkedList();
	public LinkedList<String> rankedMembers = new LinkedList();
	public LinkedList<Integer> ranks = new LinkedList();

	public int whoCanJoin = -1;
	public int whoCanTalk = -1;
	public int whoCanKick = 6;
	public int whoCanBan = 7;

	public void addMember(Client paramClient) {
		if (isBanned(paramClient.playerName)) {
			paramClient
			.sendMessage("<col=FF0000>You are currently banned from this clan chat.</col>");
			return;
		}
		if ((this.whoCanJoin > -1) && (!isFounder(paramClient.playerName))
				&& (getRank(paramClient.playerName) < this.whoCanJoin)) {
			paramClient.sendMessage("Only " + getRankTitle(this.whoCanJoin)
					+ "s+ may join this chat.");
			return;
		}

		paramClient.clan = this;
		paramClient.lastClanChat = getFounder();
		this.activeMembers.add(paramClient.playerName);
		paramClient.getPA().sendFrame126("Leave chat", 50135);
		paramClient.getPA().sendFrame126(
				"Talking in: <col=FFFF64>" + getTitle() + "</col>", 50139);
		paramClient.getPA().sendFrame126(
				"Owner: <col=FFFFFF>" + Misc.formatPlayerName(getFounder())
				+ "</col>", 50140);
		paramClient.sendMessage("Now talking in clan chat <col=FFFF64><shad=0>"
				+ getTitle() + "</shad></col>.");
		paramClient
		.sendMessage("To talk, start each line of chat with the / symbol.");
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client) PlayerHandler.players[j];
				if ((c != null)
						&& (this.activeMembers.contains(c.playerName)))
					c.sendMessage("@red@[@cr1@"+getTitle()+"]<clan=" + getRank(paramClient.playerName) + ">@bla@"+Misc.optimizeText(paramClient.playerName)+" has joined the clan!");
			}
		}
		updateMembers();
	}

	public void removeMember(Client paramClient) {
		for (int i = 0; i < this.activeMembers.size(); i++) {
			if (this.activeMembers.get(i).equalsIgnoreCase(
					paramClient.playerName)) {
				paramClient.clan = null;
				resetInterface(paramClient);
				this.activeMembers.remove(i);
			}
		}
		updateMembers();
	}

	public void removeMember(String paramString) {
		for (int i = 0; i < this.activeMembers.size(); i++) {
			if (this.activeMembers.get(i).equalsIgnoreCase(paramString)) {
				Client localClient = PlayerHandler.getPlayer(paramString);
				if (localClient != null) {
					localClient.clan = null;
					resetInterface(localClient);
					this.activeMembers.remove(i);
				}
			}
		}
		updateMembers();
	}

	public void updateInterface(Client paramClient) {
		paramClient.getPA().sendFrame126(
				"Talking in: <col=FFFF64>" + getTitle() + "</col>", 50139);
		paramClient.getPA().sendFrame126(
				"Owner: <col=FFFFFF>" + Misc.formatPlayerName(getFounder())
				+ "</col>", 50140);
		Collections.sort(this.activeMembers);
		for (int i = 0; i < 100; i++)
			if (i < this.activeMembers.size())
				paramClient.getPA().sendFrame126(
						 "<clan=" + getRank(this.activeMembers.get(i)) + ">" +
						 Misc.formatPlayerName(this.activeMembers.get(i)), 50144 + i);
			else
				paramClient.getPA().sendFrame126(" ", 50144 + i);
	}

	public void updateMembers() {
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client) PlayerHandler.players[j];
				if ((c != null) && (this.activeMembers != null)
						&& (this.activeMembers.contains(c.playerName)))
					updateInterface(c);
			}
		}
	}

	public void resetInterface(Client paramClient) {
		paramClient.getPA().sendFrame126("Join chat", 50135);
		paramClient.getPA().sendFrame126("Talking in: Not in chat", 50139);
		paramClient.getPA().sendFrame126("Owner: None", 50140);
		for (int i = 0; i < 100; i++)
			paramClient.getPA().sendFrame126("", 50144 + i);
	}

	public void sendChat(Client paramClient, String paramString) {
		if (getRank(paramClient.playerName) < this.whoCanTalk) {
			paramClient.sendMessage("Only " + getRankTitle(this.whoCanTalk)
					+ "s+ may talk in this chat.");
			return;
		}
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client) PlayerHandler.players[j];
				if ((c != null)
						&& (this.activeMembers.contains(c.playerName)))
					c.sendMessage("@red@["+getTitle()+"]<clan=" + getRank(paramClient.playerName) + ">@bla@"+Misc.optimizeText(paramClient.playerName)+": "+Misc.optimizeText(paramString)+"");
					//c.getPA().sendClan(paramClient.playerName,
							//paramString, getTitle(), paramClient.playerRights);
			}
		}
	}

	public void sendMessage(String paramString) {
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client) PlayerHandler.players[j];
				if ((c != null)
						&& (this.activeMembers.contains(c.playerName)))
					c.sendMessage(paramString);
			}
		}
	}

	public void setRank(String paramString, int paramInt) {
		if (this.rankedMembers.contains(paramString)) {
			this.ranks.set(this.rankedMembers.indexOf(paramString),
					Integer.valueOf(paramInt));
		} else {
			this.rankedMembers.add(paramString);
			this.ranks.add(Integer.valueOf(paramInt));
		}
		save();
	}

	public void demote(String paramString) {
		if (!this.rankedMembers.contains(paramString)) {
			return;
		}
		int i = this.rankedMembers.indexOf(paramString);
		this.rankedMembers.remove(i);
		this.ranks.remove(i);
		save();
	}

	public int getRank(String paramString) {
		paramString = Misc.formatPlayerName(paramString);
		if (this.rankedMembers.contains(paramString)) {
			return this.ranks.get(this.rankedMembers.indexOf(paramString))
					.intValue();
		}
		if (isFounder(paramString)) {
			return 7;
		}
		if (PlayerSave.isFriend(getFounder(), paramString)) {
			return 0;
		}
		return -1;
	}

	public boolean canKick(String paramString) {
		if (isFounder(paramString)) {
			return true;
		}
		if (getRank(paramString) >= this.whoCanKick) {
			return true;
		}
		return false;
	}

	public boolean canBan(String paramString) {
		if (isFounder(paramString)) {
			return true;
		}
		if (getRank(paramString) >= this.whoCanBan) {
			return true;
		}
		return false;
	}

	public boolean isFounder(String paramString) {
		if (getFounder().equalsIgnoreCase(paramString)) {
			return true;
		}
		return false;
	}

	public boolean isRanked(String paramString) {
		paramString = Misc.formatPlayerName(paramString);
		if (this.rankedMembers.contains(paramString)) {
			return true;
		}
		return false;
	}

	public boolean isBanned(String paramString) {
		paramString = Misc.formatPlayerName(paramString);
		if (this.bannedMembers.contains(paramString)) {
			return true;
		}
		return false;
	}

	public void kickMember(String paramString) {
		if (!this.activeMembers.contains(paramString)) {
			return;
		}
		if (paramString.equalsIgnoreCase(getFounder())) {
			return;
		}
		removeMember(paramString);
		Client localClient = PlayerHandler.getPlayer(paramString);
		if (localClient != null) {
			localClient.sendMessage("You have been kicked from the clan chat.");
		}
		sendMessage(Misc.formatPlayerName(paramString)
				+ " has been kicked from the clan chat.");
	}

	public void banMember(String paramString) {
		paramString = Misc.formatPlayerName(paramString);
		if (this.bannedMembers.contains(paramString)) {
			return;
		}
		if (paramString.equalsIgnoreCase(getFounder())) {
			return;
		}
		if (isRanked(paramString)) {
			return;
		}
		removeMember(paramString);
		this.bannedMembers.add(paramString);
		save();
		Client localClient = PlayerHandler.getPlayer(paramString);
		if ((localClient != null) && (localClient.clan == this)) {
			localClient.sendMessage("You have been banned from the clan chat.");
		}
		sendMessage(Misc.formatPlayerName(paramString)
				+ " has been banned from the clan chat.");
	}

	public void unbanMember(String paramString) {
		paramString = Misc.formatPlayerName(paramString);
		if (this.bannedMembers.contains(paramString)) {
			this.bannedMembers.remove(paramString);
			save();
		}
	}

	public void save() {
		Server.clanManager.save(this);
		updateMembers();
	}

	public void delete() {
		for (String str : this.activeMembers) {
			removeMember(str);
			Client localClient = PlayerHandler.getPlayer(str);
			localClient.sendMessage("The clan you were in has been deleted.");
		}
		Server.clanManager.delete(this);
	}

	public Clan(Client paramClient) {
		setTitle(paramClient.playerName + "'s Clan");
		setFounder(paramClient.playerName.toLowerCase());
	}

	public Clan(String paramString1, String paramString2) {
		setTitle(paramString1);
		setFounder(paramString2);
	}

	public String getFounder() {
		return this.founder;
	}

	public void setFounder(String paramString) {
		this.founder = paramString;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String paramString) {
		this.title = paramString;
	}

	public String getRankTitle(int paramInt) {
		switch (paramInt) {
		case -1:
			return "Anyone";
		case 0:
			return "Friend";
		case 1:
			return "Recruit";
		case 2:
			return "Corporal";
		case 3:
			return "Sergeant";
		case 4:
			return "Lieutenant";
		case 5:
			return "Captain";
		case 6:
			return "General";
		case 7:
			return "Only Me";
		}
		return "";
	}

	public void setRankCanJoin(int paramInt) {
		this.whoCanJoin = paramInt;
	}

	public void setRankCanTalk(int paramInt) {
		this.whoCanTalk = paramInt;
	}

	public void setRankCanKick(int paramInt) {
		this.whoCanKick = paramInt;
	}

	public void setRankCanBan(int paramInt) {
		this.whoCanBan = paramInt;
	}

	public static class Rank {
		public static final int ANYONE = -1;
		public static final int FRIEND = 0;
		public static final int RECRUIT = 1;
		public static final int CORPORAL = 2;
		public static final int SERGEANT = 3;
		public static final int LIEUTENANT = 4;
		public static final int CAPTAIN = 5;
		public static final int GENERAL = 6;
		public static final int OWNER = 7;
	}
}