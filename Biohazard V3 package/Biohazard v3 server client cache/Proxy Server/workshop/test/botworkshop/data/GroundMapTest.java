package botworkshop.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import botworkshop.WorkshopFixture;

/**
 * Locks the terrain decoder to the shipped landscape files.
 *
 * <p>These cannot compare against {@code Region}, because it discards everything checked here —
 * that discarding is the reason this decoder exists. What they can do is prove the loop consumes
 * exactly the bytes the file holds: a wrong payload length anywhere desynchronises the stream and
 * shows up as a truncated buffer on the very next region, so "every region in the index decodes"
 * is a strong statement about the opcode table.
 */
class GroundMapTest {

	@Test
	void everyRegionThatHasMapDataDecodes() throws IOException {
		// The 51 regions with no files are skipped, exactly as Region.load() skips them. A wrong
		// payload length anywhere desynchronises the tile grid, so the very next region would
		// fail to decode — which is what makes this a test of the opcode table, not just of I/O.
		int decoded = 0;
		for (MapIndex.Entry entry : MapIndex.read(WorkshopFixture.mapIndex())) {
			java.nio.file.Path[] files = entry.mapFiles(WorkshopFixture.mapDir());
			if (files == null) {
				continue;
			}
			GroundMap map = GroundMap.read(files[0]);
			assertNotNull(map, "region " + entry.regionId);
			decoded++;
		}
		assertEquals(1175, decoded, "every region the map contains must decode");
	}

	@Test
	void everyTileOfEveryPlaneIsPopulated() throws IOException {
		GroundMap map = GroundMap.read(WorkshopFixture.mapDir().resolve(groundFileForLumbridge() + ".gz"));
		for (int plane = 0; plane < GroundMap.planes(); plane++) {
			for (int x = 0; x < GroundMap.size(); x++) {
				for (int y = 0; y < GroundMap.size(); y++) {
					assertNotNull(map.tile(plane, x, y), "tile " + plane + "/" + x + "/" + y);
				}
			}
		}
	}

	@Test
	void flagsStayInsideTheOpcodeRangeTheyComeFrom() throws IOException {
		// The value comes from opcodes 50..81 as opcode - 49, so 1..32. A 0 here is a tile
		// with no flags; anything above 32 means the decoded opcode was out of range.
		GroundMap map = GroundMap.read(WorkshopFixture.mapDir().resolve(groundFileForLumbridge() + ".gz"));
		for (int plane = 0; plane < GroundMap.planes(); plane++) {
			for (int x = 0; x < GroundMap.size(); x++) {
				for (int y = 0; y < GroundMap.size(); y++) {
					int flags = map.tile(plane, x, y).flags;
					assertTrue(flags >= 0 && flags <= 32, "flags " + flags + " at " + plane + "/" + x + "/" + y);
				}
			}
		}
	}

	@Test
	void lumbridgeCastleHasTerrainToDraw() throws IOException {
		// The point of this decoder: Region throws the overlay away, so if this were zero the
		// editor would have nothing to render and the whole class would be pointless.
		GroundMap map = GroundMap.read(WorkshopFixture.mapDir().resolve(groundFileForLumbridge() + ".gz"));
		int withOverlay = 0;
		for (int x = 0; x < GroundMap.size(); x++) {
			for (int y = 0; y < GroundMap.size(); y++) {
				if (map.tile(0, x, y).hasOverlay()) {
					withOverlay++;
				}
			}
		}
		assertTrue(withOverlay > 100, "plane 0 of Lumbridge should be mostly drawn floor, got " + withOverlay);
	}

	@Test
	void occupancyMatchesTheBitRegionTests() throws IOException {
		// Region.loadMaps blocks a tile when (flags & 1) == 1, and that is the only part of this
		// data the server keeps. If isOccupied() disagreed with the raw bit, the exporter and the
		// server would place blockers on different tiles.
		GroundMap map = GroundMap.read(WorkshopFixture.mapDir().resolve(groundFileForLumbridge() + ".gz"));
		for (int plane = 0; plane < GroundMap.planes(); plane++) {
			for (int x = 0; x < GroundMap.size(); x++) {
				for (int y = 0; y < GroundMap.size(); y++) {
					GroundTile tile = map.tile(plane, x, y);
					assertEquals((tile.flags & 1) == 1, tile.isOccupied(),
							"occupancy at " + plane + "/" + x + "/" + y);
				}
			}
		}
	}

	@Test
	void outOfRangeCoordinatesReturnNullRatherThanThrowing() throws IOException {
		GroundMap map = GroundMap.read(WorkshopFixture.mapDir().resolve(groundFileForLumbridge() + ".gz"));
		assertNull(map.tile(4, 0, 0));
		assertNull(map.tile(0, 64, 0));
		assertNull(map.tile(0, -1, 0));
	}

	@Test
	void aTruncatedBufferFailsLoudlyInsteadOfShiftingEveryLaterTile() {
		byte[] shortBuffer = new byte[4096];
		IOException e = assertThrows(IOException.class, () -> GroundMap.parse(shortBuffer));
		assertTrue(e.getMessage().contains("truncated"), "message names the real problem: " + e.getMessage());
	}

	@Test
	void aBufferUnderTheMinimumIsRejectedTheWayRegionRejectsIt() {
		assertThrows(IOException.class, () -> GroundMap.parse(new byte[9]));
	}

	/** The ground file id {@code map_index} gives for the region containing (3200, 3200). */
	private static int groundFileForLumbridge() throws IOException {
		for (MapIndex.Entry entry : MapIndex.read(WorkshopFixture.mapIndex())) {
			if (entry.baseX() == 3200 && entry.baseY() == 3200) {
				return entry.groundFileId;
			}
		}
		throw new IllegalStateException("Lumbridge's region is not in map_index");
	}
}
