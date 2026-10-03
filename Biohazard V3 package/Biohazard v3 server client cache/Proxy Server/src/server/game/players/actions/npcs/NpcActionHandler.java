package server.game.players.actions.npcs;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import server.game.players.Client;

/**
 * Registry of NPC actions, keyed on {@code (npcType, NpcClick)}, consulted before the
 * switch in each of {@code ActionHandler}'s three NPC methods.
 *
 * <p>Named {@code NpcActionHandler} rather than {@code NpcHandler} to keep it clearly
 * distinct from the pre-existing {@code server.game.npcs.NPCHandler}, which is the NPC
 * tick and update handler and has nothing to do with click dispatch.
 *
 * <p>Same strangler arrangement as the button and object registries: dispatch runs
 * first and the switch is the fallthrough. A duplicate registration throws, because a
 * map would otherwise silently shadow where a switch would not compile.
 */
public final class NpcActionHandler {

	private static final Map<NpcClick, Map<Integer, NpcAction>> byClick =
			new EnumMap<>(NpcClick.class);

	static {
		for (NpcClick click : NpcClick.values()) {
			byClick.put(click, new ConcurrentHashMap<>());
		}
		ShopNpcs.register();
		TalkNpcs.register();
		FixedSpeakerNpcs.register();
		TeleportNpcs.register();
		FishingNpcs.register();
		PickpocketNpcs.register();
		BankNpcs.register();
	}

	private NpcActionHandler() {
	}

	public static void register(int npcType, NpcClick click, NpcAction action) {
		if (byClick.get(click).putIfAbsent(npcType, action) != null) {
			throw new IllegalStateException("npc " + npcType + " " + click + " is registered twice");
		}
	}

	/**
	 * @return true if an action claimed this NPC, meaning the legacy switch must not
	 *         also run for it.
	 */
	public static boolean dispatch(Client c, int npcType, NpcClick click) {
		NpcAction action = byClick.get(click).get(npcType);
		if (action == null) {
			return false;
		}
		action.handle(c, npcType);
		return true;
	}

	public static boolean isRegistered(int npcType, NpcClick click) {
		return byClick.get(click).containsKey(npcType);
	}

	public static int size(NpcClick click) {
		return byClick.get(click).size();
	}
}
