package botworkshop;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves the real server data files for the Workshop tests. */
public final class WorkshopFixture {

	private WorkshopFixture() {
	}

	/**
	 * The server's {@code Data} directory, passed in by the {@code workshopTest} task so no test
	 * depends on the working directory. Mirrors the {@code objectSizeCfg} property the server's
	 * own test task uses.
	 */
	public static Path dataRoot() {
		String root = System.getProperty("workshopDataRoot");
		if (root == null || root.isEmpty()) {
			throw new IllegalStateException(
					"workshopDataRoot is not set — run the tests through the workshopTest task");
		}
		return Paths.get(root);
	}

	public static Path mapIndex() {
		return dataRoot().resolve("world/map_index");
	}

	public static Path mapDir() {
		return dataRoot().resolve("world/map");
	}

	public static Path locDat() {
		return dataRoot().resolve("world/object/loc.dat");
	}

	public static Path locIdx() {
		return dataRoot().resolve("world/object/loc.idx");
	}
}
