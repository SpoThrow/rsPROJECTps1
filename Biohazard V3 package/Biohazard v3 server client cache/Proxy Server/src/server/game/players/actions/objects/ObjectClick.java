package server.game.players.actions.objects;

/**
 * Which click an {@link ObjectAction} responds to. Objects are keyed on
 * {@code (objectType, ObjectClick)} because the same object usually behaves
 * differently on each click.
 */
public enum ObjectClick {
	FIRST, SECOND, THIRD
}
