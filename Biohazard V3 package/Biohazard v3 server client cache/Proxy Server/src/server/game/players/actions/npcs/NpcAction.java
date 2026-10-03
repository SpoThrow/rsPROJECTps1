package server.game.players.actions.npcs;

import server.game.players.Client;

/**
 * Handles one {@code (npcType, click)} pair, migrated out of the switches in
 * {@code ActionHandler}.
 *
 * <p>Note there are no coordinates: {@code firstClickNpc} and friends take only the
 * npc type, unlike the object methods which are passed the clicked tile.
 */
public interface NpcAction {

	void handle(Client c, int npcType);
}
