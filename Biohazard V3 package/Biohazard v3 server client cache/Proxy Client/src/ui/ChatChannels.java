package ui;

import java.util.Properties;import game.client;



/**
 * RuneLite Chat Channels extras for clan chat: join/leave, kick confirm,
 * ignored recolour, online count, and clan-tab chat without a leading slash.
 */
public final class ChatChannels {

	public static boolean enabled = true;
	public static boolean joinLeave = true;
	static boolean kickConfirm = true;
	static boolean recolorIgnored = true;
	static boolean onlineCount = true;
	static boolean clanTabNoSlash = true;

	public static void load(Properties props) {
		enabled = readBool(props, "chatChannels", true);
		joinLeave = readBool(props, "chatJoinLeave", true);
		kickConfirm = readBool(props, "chatKickConfirm", true);
		recolorIgnored = readBool(props, "chatRecolorIgnored", true);
		onlineCount = readBool(props, "chatOnlineCount", true);
		clanTabNoSlash = readBool(props, "chatClanNoSlash", true);
	}

	public static void save(Properties props) {
		props.setProperty("chatChannels", Boolean.toString(enabled));
		props.setProperty("chatJoinLeave", Boolean.toString(joinLeave));
		props.setProperty("chatKickConfirm", Boolean.toString(kickConfirm));
		props.setProperty("chatRecolorIgnored", Boolean.toString(recolorIgnored));
		props.setProperty("chatOnlineCount", Boolean.toString(onlineCount));
		props.setProperty("chatClanNoSlash", Boolean.toString(clanTabNoSlash));
	}

	public static boolean hideJoinLeave(String message) {
		if (enabled && !joinLeave && message != null) {
			String lower = message.toLowerCase();
			if (lower.indexOf("has joined the clan") >= 0 || lower.indexOf("has left the clan") >= 0) {
				return true;
			}
		}
		return false;
	}

	public static boolean shouldStripClanSlash(int chatTypeView) {
		return enabled && clanTabNoSlash && chatTypeView == 11;
	}

	public static int gameColor(client c, String message) {
		if (!enabled || !recolorIgnored || c == null || message == null) {
			return 0;
		}
		if (c.messageMentionsIgnored(message)) {
			return 0x808080;
		}
		return 0;
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
