package botworkshop.export;

import java.util.List;

import server.game.bots.world.Location;
import server.game.bots.world.LocationKind;
import server.game.bots.world.LocationsConfig;

/**
 * Serialises the authored {@code locations.cfg} for the editor's place overlay
 * ({@code BOT_ROADMAP.md} §5.2).
 *
 * <p>Read by {@link LocationsConfig}, not re-parsed: the editor must draw the places the server
 * would resolve, and a second parser is exactly how the two would come to disagree — which is also
 * why each row carries the canonical text {@link LocationsConfig#toRow} produces, so the editor can
 * show what is really in the file rather than a re-spelling of it.
 */
public final class LocationsDoc {

	private LocationsDoc() {
	}

	public static String toJson(List<Location> locations) {
		Json json = new Json();
		json.openObject();
		json.field("count", locations.size());
		// The kinds the parser accepts, so the editor's picker comes from the server rather than from
		// a second list in JavaScript that would have to be kept in step by hand.
		json.name("kinds").openArray();
		for (String kind : LocationsConfig.knownKindIds()) {
			json.value(kind);
		}
		json.closeArray();
		// Which of those kinds the world can be scanned for. The editor counts the objects in a
		// drafted box to warn before a row is written, and for `shop`/`teleport`/`monster`/`master`
		// there is no object to count — a report of "0 found" there would be a false alarm on every
		// such row. The list comes from the runtime's own predicate for the same reason the kinds do.
		json.name("objectKinds").openArray();
		for (LocationKind kind : LocationKind.values()) {
			if (kind.isObjectKind()) {
				json.value(kind.id());
			}
		}
		json.closeArray();
		json.name("rows").openArray();
		for (Location location : locations) {
			json.openObject();
			json.field("name", location.name());
			json.field("kind", location.kind().id());
			json.field("x", location.x());
			json.field("y", location.y());
			json.field("w", location.width());
			json.field("h", location.height());
			json.field("plane", location.plane());
			json.field("point", location.isPoint());
			json.name("tags").openArray();
			for (String tag : location.tags()) {
				json.value(tag);
			}
			json.closeArray();
			json.field("row", LocationsConfig.toRow(location));
			json.closeObject();
		}
		json.closeArray();
		json.closeObject();
		return json.toString();
	}
}
