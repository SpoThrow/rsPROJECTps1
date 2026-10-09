package botworkshop.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import botworkshop.WorkshopFixture;

/**
 * The exporter walks regions from {@code map_index}, so a misread here would silently export the
 * wrong part of the world. These assert the layout against the shipped file rather than a fixture.
 */
class MapIndexTest {

	@Test
	void readsOneSixByteRecordPerRegion() throws IOException {
		List<MapIndex.Entry> entries = MapIndex.read(WorkshopFixture.mapIndex());
		// 7356 / 6 == 1226, the count Region.load() indexes.
		assertEquals(1226, entries.size(), "the shipped map_index holds 1226 regions");
	}

	@Test
	void regionIdsRoundTripToTheirSouthWestCorner() throws IOException {
		for (MapIndex.Entry entry : MapIndex.read(WorkshopFixture.mapIndex())) {
			int regionX = entry.regionId >> 8;
			int regionY = entry.regionId & 0xff;
			assertEquals(regionX * 64, entry.baseX(), "baseX for region " + entry.regionId);
			assertEquals(regionY * 64, entry.baseY(), "baseY for region " + entry.regionId);
		}
	}

	@Test
	void findsTheRegionContainingLumbridgeCastle() throws IOException {
		// 3222 >> 3 = 402, 402 / 8 = 50; same for y. Region id is (50 << 8) + 50.
		int wanted = (50 << 8) + 50;
		MapIndex.Entry found = null;
		for (MapIndex.Entry entry : MapIndex.read(WorkshopFixture.mapIndex())) {
			if (entry.regionId == wanted) {
				found = entry;
				break;
			}
		}
		assertNotNull(found, "Lumbridge's region must be in the index");
		assertEquals(3200, found.baseX());
		assertEquals(3200, found.baseY());
	}

	@Test
	void theMapShipsWithExactlyFiftyOneRegionsOfMissingData() throws IOException {
		// This is the number Region.load() reports at boot ("51 of 1226 region(s) have no map
		// data"). Pinning it keeps the exporter's skip-if-absent rule honest: if a future map
		// sync changes this, the exporter's behaviour changes with it and this test says so.
		int present = 0;
		int absent = 0;
		for (MapIndex.Entry entry : MapIndex.read(WorkshopFixture.mapIndex())) {
			if (entry.mapFiles(WorkshopFixture.mapDir()) == null) {
				absent++;
			} else {
				present++;
			}
		}
		assertEquals(1175, present, "regions with both map files");
		assertEquals(51, absent, "regions the directory lists but the map does not contain");
	}

	@Test
	void aRegionWithNoMapFilesIsReportedRatherThanGuessedAt() throws IOException {
		// 6483 is one of the 51: its ground file 3662 is not in Data/world/map.
		MapIndex.Entry absent = null;
		for (MapIndex.Entry entry : MapIndex.read(WorkshopFixture.mapIndex())) {
			if (entry.regionId == 6483) {
				absent = entry;
				break;
			}
		}
		assertNotNull(absent, "region 6483 is in the directory");
		assertEquals(3662, absent.groundFileId);
		assertNull(absent.mapFiles(WorkshopFixture.mapDir()), "and its files are missing");
	}

	@Test
	void rejectsABufferThatIsNotWholeRecords() {
		IOException e = assertThrows(IOException.class, () -> MapIndex.read(new byte[7]));
		assertTrue(e.getMessage().contains("multiple of 6"), "message names the real problem: " + e.getMessage());
	}

	@Test
	void rejectsAnEmptyFile() {
		assertThrows(IOException.class, () -> MapIndex.read(new byte[0]));
	}
}
