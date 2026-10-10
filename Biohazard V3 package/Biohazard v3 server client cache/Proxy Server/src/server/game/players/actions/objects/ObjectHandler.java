package server.game.players.actions.objects;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import server.game.players.Client;

/**
 * Registry of object actions, keyed on {@code (objectType, ObjectClick)}, consulted
 * before the switch in each of {@code ActionHandler}'s three object methods.
 *
 * <p>Same strangler arrangement as the button registry: dispatch runs first and the
 * switch is the fallthrough, so objects can be moved out one group at a time.
 *
 * <p>A duplicate registration throws rather than quietly shadowing. In a switch two
 * identical {@code case} labels do not compile; a map gives that guarantee up, so it
 * has to be re-established here.
 */
public final class ObjectHandler {

	private static final Map<ObjectClick, Map<Integer, ObjectAction>> byClick =
			new EnumMap<>(ObjectClick.class);

	static {
		for (ObjectClick click : ObjectClick.values()) {
			byClick.put(click, new ConcurrentHashMap<>());
		}
		WoodcuttingObjects.register();
		DwarfCannonObjects.register();
		TeleportObjects.register();
		RuneRiftObjects.register();
		BountyHunterObjects.register();
		AgilityObjects.register();
		MiningProspectObjects.register();
		FurnaceObjects.register();
		BankBoothObjects.register();
		CastleWarsDoorObjects.register();
		CastleWarsObjectClicks.register();
		DoorObjects.register();
		SpinningWheelObjects.register();
		PotteryWheelObjects.register();
		WeavingLoomObjects.register();
	}

	private ObjectHandler() {
	}

	public static void register(int objectType, ObjectClick click, ObjectAction action) {
		if (byClick.get(click).putIfAbsent(objectType, action) != null) {
			throw new IllegalStateException(
					"object " + objectType + " " + click + " is registered twice");
		}
	}

	/**
	 * @return true if an action claimed this object, meaning the legacy switch must
	 *         not also run for it.
	 */
	public static boolean dispatch(Client c, int objectType, ObjectClick click, int objectX, int objectY) {
		ObjectAction action = byClick.get(click).get(objectType);
		if (action == null) {
			return false;
		}
		action.handle(c, objectType, objectX, objectY);
		return true;
	}

	public static boolean isRegistered(int objectType, ObjectClick click) {
		return byClick.get(click).containsKey(objectType);
	}

	public static int size(ObjectClick click) {
		return byClick.get(click).size();
	}
}
