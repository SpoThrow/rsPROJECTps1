package server.game.players.actions.npcs;

/**
 * Which click an {@link NpcAction} responds to. NPCs are keyed on
 * {@code (npcType, NpcClick)} for the same reason objects are: one NPC usually
 * behaves differently on each click.
 */
public enum NpcClick {
	FIRST, SECOND, THIRD
}
