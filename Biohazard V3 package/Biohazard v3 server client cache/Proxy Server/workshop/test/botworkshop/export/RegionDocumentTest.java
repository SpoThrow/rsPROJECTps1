package botworkshop.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import botworkshop.classify.ResourceRules;
import botworkshop.data.GroundMap;
import botworkshop.data.LocDefs;
import botworkshop.data.MapIndex;
import server.clip.region.ObjectSizes;

/**
 * The document builder, tested on a landscape synthesised tile by tile so the expected output is
 * known by construction rather than by observing what the code happens to emit.
 */
class RegionDocumentTest {

	private static final int SIZE = 64;

	@Test
	void tilePlaneIndexIsLocalXTimes64PlusLocalY() throws IOException {
		// The one thing a consumer cannot guess and would silently get wrong: transposing the index
		// draws a mirrored map with no error anywhere. Three distinct overlays at (0,0), (0,1) and
		// (1,0) pin it, because a transposed index would put the last two the other way round.
		GroundMap ground = GroundMap.parse(landscape(overlayAt(0, 0, 5), overlayAt(0, 1, 9), overlayAt(1, 0, 11)));

		String json = RegionDocument.toJson(entry(), ground, List.of(), null, null, null);
		int[] overlay = Rle.decode(readString(json, "overlay"), SIZE * SIZE);

		assertEquals(5, overlay[0], "tile (localX 0, localY 0) is index 0");
		assertEquals(9, overlay[1], "tile (localX 0, localY 1) is index 1 — localY varies fastest");
		assertEquals(11, overlay[64], "tile (localX 1, localY 0) is index 64");
	}

	@Test
	void everyPlaneIsEmittedEvenWithNothingInIt() throws IOException {
		String json = RegionDocument.toJson(entry(), GroundMap.parse(landscape()), List.of(), null, null, null);
		for (int plane = 0; plane < 4; plane++) {
			assertTrue(json.contains("\"plane\": " + plane), "plane " + plane + " missing");
		}
	}

	@Test
	void regionIdentityComesFromTheIndexEntry() throws IOException {
		String json = RegionDocument.toJson(entry(), GroundMap.parse(landscape()), List.of(), null, null, null);
		assertTrue(json.contains("\"regionId\": 12850"), json.substring(0, 200));
		assertTrue(json.contains("\"baseX\": 3200"));
		assertTrue(json.contains("\"baseY\": 3200"));
	}

	@Test
	void anObjectIsDescribedFromItsDefinitionAndTheSizeTable() throws IOException {
		LocDefs defs = LocDefs.load(
				botworkshop.WorkshopFixture.locDat(), botworkshop.WorkshopFixture.locIdx());
		ObjectSizes sizes = ObjectSizes.get();

		String json = RegionDocument.toJson(entry(), GroundMap.parse(landscape()),
				List.of(new Placement(1276, 3201, 3202, 0, 10, 0)), defs, sizes, null);

		assertTrue(json.contains("\"name\": \"Tree\""), "tree name");
		assertTrue(json.contains("\"Chop down\""), "tree action");
		assertTrue(json.contains("\"kind\": \"tree\""), "classified");
		assertTrue(json.contains("\"service\": false"), "a tree is a resource");
		// 2x2 comes from Data/objectSize.cfg via ObjectSizes, the same source Region.addObject uses.
		assertTrue(json.contains("\"sizeX\": 2") && json.contains("\"sizeY\": 2"), "footprint");
		assertTrue(json.contains("\"x\": 3201") && json.contains("\"y\": 3202"), "placement");
	}

	@Test
	void aBankBoothIsMarkedAsAServiceThroughItsName() throws IOException {
		LocDefs defs = LocDefs.load(
				botworkshop.WorkshopFixture.locDat(), botworkshop.WorkshopFixture.locIdx());
		String json = RegionDocument.toJson(entry(), GroundMap.parse(landscape()),
				List.of(new Placement(2213, 3201, 3202, 0, 10, 0)), defs, ObjectSizes.get(), null);
		assertTrue(json.contains("\"kind\": \"bank\""), "bank kind");
		assertTrue(json.contains("\"service\": true"), "banks are services");
	}

	@Test
	void anUnknownObjectIdStillProducesAUsableEntry() throws IOException {
		// The exporter must not throw on an id the archive does not know: a map can reference one,
		// and the editor would rather draw the object with no name than fail the whole region.
		String json = RegionDocument.toJson(entry(), GroundMap.parse(landscape()),
				List.of(new Placement(999999, 3201, 3202, 0, 10, 0)), null, null, null);
		assertTrue(json.contains("\"id\": 999999"));
		assertTrue(json.contains("\"name\": null"), "no name is null, not a guess");
		assertTrue(json.contains("\"kind\": null"), "unclassified, not forced into a category");
	}

	@Test
	void theClipValueIsTakenFromTheInjectedSourceRatherThanComputed() throws IOException {
		// The export must not carry its own copy of the collision rules. Whatever Region.getClipping
		// says is what lands in the file, which is what makes server parity true by construction.
		TileClip clip = (x, y, plane) -> x * 1000 + y;
		String json = RegionDocument.toJson(entry(), GroundMap.parse(landscape()),
				List.of(new Placement(1276, 3201, 3202, 0, 10, 0)), null, null, clip);
		assertTrue(json.contains("\"clip\": 3204202"), "clip from the injected source");
	}

	@Test
	void aRegionWithNoObjectsStillEmitsAnEmptyArray() throws IOException {
		String json = RegionDocument.toJson(entry(), GroundMap.parse(landscape()), List.of(), null, null, null);
		assertTrue(json.contains("\"objects\": []"), json);
	}

	@Test
	void theDocumentIsBalancedJson() throws IOException {
		String json = RegionDocument.toJson(entry(), GroundMap.parse(landscape()),
				List.of(new Placement(1276, 3201, 3202, 0, 10, 0)), null, null, null);
		int depth = 0;
		boolean inString = false;
		for (int i = 0; i < json.length(); i++) {
			char c = json.charAt(i);
			if (inString) {
				if (c == '\\') {
					i++;
				} else if (c == '"') {
					inString = false;
				}
				continue;
			}
			if (c == '"') {
				inString = true;
			} else if (c == '{' || c == '[') {
				depth++;
			} else if (c == '}' || c == ']') {
				depth--;
				assertTrue(depth >= 0, "closed more than opened");
			}
		}
		assertEquals(0, depth, "unclosed nesting");
		assertFalse(inString, "unterminated string");
	}

	// ---- helpers ---------------------------------------------------------------------------

	private static MapIndex.Entry entry() throws IOException {
		for (MapIndex.Entry e : MapIndex.read(botworkshop.WorkshopFixture.mapIndex())) {
			if (e.regionId == 12850) {
				return e;
			}
		}
		throw new IllegalStateException("region 12850 not in the index");
	}

	/** A tile coordinate paired with the overlay id to give it. */
	private static int[] overlayAt(int localX, int localY, int overlayId) {
		return new int[] { localX, localY, overlayId };
	}

	/**
	 * Builds a landscape buffer for all four planes. Tiles listed in {@code overlays} get an
	 * opcode-2 overlay record; everything else is a bare end-of-tile. Plane 0 only, so the other
	 * three planes stay empty and are cheap.
	 */
	private static byte[] landscape(int[]... overlays) throws IOException {
		List<int[]> list = Arrays.asList(overlays);
		ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 16);
		for (int plane = 0; plane < 4; plane++) {
			for (int localX = 0; localX < SIZE; localX++) {
				for (int localY = 0; localY < SIZE; localY++) {
					if (plane == 0) {
						for (int[] overlay : list) {
							if (overlay[0] == localX && overlay[1] == localY) {
								out.write(2); // opcode 2..49: an overlay record follows
								out.write(overlay[2]);
								break;
							}
						}
					}
					out.write(0); // end of tile
				}
			}
		}
		return out.toByteArray();
	}

	/** The value of the first {@code "name": "..."} string field in a JSON document. */
	private static String readString(String json, String field) {
		String needle = "\"" + field + "\": \"";
		int at = json.indexOf(needle);
		if (at < 0) {
			throw new IllegalStateException("field " + field + " not found");
		}
		int start = at + needle.length();
		int end = start;
		while (json.charAt(end) != '"' || json.charAt(end - 1) == '\\') {
			end++;
		}
		return json.substring(start, end);
	}
}
