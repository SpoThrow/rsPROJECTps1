package server.game.bots.world;

import java.util.Locale;

/**
 * What a {@link Location} is: a resource, a service, a travel point or a target.
 *
 * <p><b>One vocabulary, not two.</b> The ids here are the same strings
 * {@link ResourceKinds} classifies objects into, so a tree the map draws as {@code tree} and a tree
 * a bot asks for by {@code tree} are the same word. A second spelling for the same concept is how the
 * map and the runtime would drift apart, which is the failure {@code BOT_TOOLING.md} §11 names.
 *
 * <p><b>Aliases.</b> {@link #byId} also accepts the place-words {@code BOT_LOCATIONS.md} A.3 sketched
 * — {@code mine}, {@code anvil}, {@code range}, {@code altar}, {@code fish} — and folds them onto the
 * canonical kind. That keeps the doc's example rows loadable without giving the world two names for
 * one thing.
 */
public enum LocationKind {

	TREE("tree", false),
	ROCK("rock", false),
	FISHING("fishing", false),
	BANK("bank", true),
	COOKING("cooking", true),
	SMITHING("smithing", true),
	PRAYER("prayer", true),
	/** A shop, which exists only as an authored place — no object is classified as one. */
	SHOP("shop", true),
	/** A teleport landing tile, from {@code Data/cfg/teleports.cfg}. */
	TELEPORT("teleport", false),
	/** A monster spawn, from {@code Data/cfg/spawn-config.cfg}. */
	MONSTER("monster", false),
	/** A skill master or Slayer master, an authored place. */
	MASTER("master", true);

	private final String id;
	private final boolean service;

	LocationKind(String id, boolean service) {
		this.id = id;
		this.service = service;
	}

	/** The stable id, e.g. {@code "tree"}. */
	public String id() {
		return id;
	}

	/** True for a service (bank, range, anvil, altar, shop) rather than a gatherable resource. */
	public boolean isService() {
		return service;
	}

	/** True when this kind is one {@link ResourceKinds} can classify a world object into. */
	public boolean isObjectKind() {
		return this == TREE || this == ROCK || this == FISHING || this == BANK
				|| this == COOKING || this == SMITHING || this == PRAYER;
	}

	/** The kind for {@code id}, or {@code null} when it is not one this world knows. */
	public static LocationKind byId(String id) {
		if (id == null) {
			return null;
		}
		String key = id.trim().toLowerCase(Locale.ROOT);
		for (LocationKind kind : values()) {
			if (kind.id.equals(key)) {
				return kind;
			}
		}
		return alias(key);
	}

	/** The place-words the design doc uses, folded onto the canonical kind. */
	private static LocationKind alias(String key) {
		switch (key) {
		case "mine":
			return ROCK;
		case "anvil":
			return SMITHING;
		case "range":
		case "fire":
			return COOKING;
		case "altar":
			return PRAYER;
		case "fish":
			return FISHING;
		default:
			return null;
		}
	}
}
