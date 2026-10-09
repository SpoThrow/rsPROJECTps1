package botworkshop.serve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins the palette the map viewer draws with.
 *
 * <p>The palette is generated, so the properties worth pinning are not "this id is this colour" —
 * that is decoration and may be overridden — but the ones the viewer and a reader of the map depend
 * on: the same id always yields the same colour, neighbours are distinguishable, the two id
 * namespaces do not collide, and the override file is parsed strictly enough to catch a typo while
 * staying non-fatal.
 */
class FloorPaletteTest {

	/** Ids the shipped cache actually uses sit in the low tens and sixties. */
	private static final int REALISTIC_RANGE = 80;

	/** Below this, two terrains read as the same colour on screen. Out of 255 per channel. */
	private static final int MIN_CHANNEL_DELTA = 24;

	@Test
	void theSameIdAlwaysYieldsTheSameColour() {
		for (String kind : FloorPalette.KINDS) {
			for (int id = 0; id <= FloorPalette.MAX_ID; id++) {
				assertEquals(FloorPalette.hex(kind, id), FloorPalette.hex(kind, id),
						kind + ":" + id + " is not stable across calls");
			}
		}
	}

	@Test
	void idZeroIsTransparentSoTheLayerBeneathShowsThrough() {
		for (String kind : FloorPalette.KINDS) {
			assertEquals(FloorPalette.NONE, FloorPalette.hex(kind, 0));
		}
		// Negative ids are not valid floor ids; treating them as "no floor" is safer than
		// generating a colour for a value that cannot occur.
		assertEquals(FloorPalette.NONE, FloorPalette.hex("underlay", -1));
	}

	@Test
	void neighbouringFloorIdsAreVisiblyDifferent() {
		// A terrain boundary is usually id 5 next to id 6. If those render as the same colour the
		// map is unreadable exactly where it matters, so this is the property the hue step exists
		// to guarantee — and the one worth failing the build over.
		int worst = Integer.MAX_VALUE;
		String pair = "none";
		for (String kind : FloorPalette.KINDS) {
			for (int id = 1; id < REALISTIC_RANGE; id++) {
				int delta = channelDelta(FloorPalette.hex(kind, id), FloorPalette.hex(kind, id + 1));
				if (delta < worst) {
					worst = delta;
					pair = kind + ":" + id + " vs " + (id + 1);
				}
			}
		}
		assertTrue(worst >= MIN_CHANNEL_DELTA,
				"closest neighbouring floors are " + pair + ", only " + worst
						+ " apart per channel (want >= " + MIN_CHANNEL_DELTA + ")");
	}

	@Test
	void idsAreDistinctWithinEachKind() {
		for (String kind : FloorPalette.KINDS) {
			Map<String, Integer> seen = new HashMap<String, Integer>();
			for (int id = 1; id <= FloorPalette.MAX_ID; id++) {
				String colour = FloorPalette.hex(kind, id);
				Integer previous = seen.put(colour, id);
				assertNull(previous, kind + ":" + id + " and " + kind + ":" + previous
						+ " would draw identically as " + colour);
			}
		}
	}

	@Test
	void overlayAndUnderlayNeverShareAColourForTheSameId() {
		// The two ids come from different floor tables, so a collision here would make two
		// unrelated terrains indistinguishable in the editor.
		for (int id = 1; id <= FloorPalette.MAX_ID; id++) {
			assertNotEquals(FloorPalette.hex("underlay", id), FloorPalette.hex("overlay", id),
					"underlay and overlay both draw as " + FloorPalette.hex("underlay", id)
							+ " for id " + id);
		}
	}

	@Test
	void everyGeneratedHueStaysInsideTheTerrainBand() {
		// The palette is decoration, but it is not arbitrary: an even spread over the hue wheel paints
		// grass purple and water orange, which makes the map unreadable and the tool look broken.
		// This pins the band, so widening it back to the full wheel is a deliberate act with a
		// failing test rather than a quiet one.
		double low = FloorPalette.hueBandStart();
		double high = low + FloorPalette.hueBandWidth();
		for (String kind : FloorPalette.KINDS) {
			for (int id = 1; id <= FloorPalette.MAX_ID; id++) {
				double hue = hueOf(FloorPalette.hex(kind, id));
				assertTrue(hue >= low - 0.005 && hue <= high + 0.005,
						String.format("%s:%d is hue %.3f, outside the band %.2f..%.2f (%s)",
								kind, id, hue, low, high, FloorPalette.hex(kind, id)));
			}
		}
	}

	@Test
	void everyGeneratedColourIsMutedEnoughToReadAsTerrain() {
		// Belt and braces on the same decision: a hue in the band at full saturation is still neon.
		for (String kind : FloorPalette.KINDS) {
			for (int id = 1; id <= FloorPalette.MAX_ID; id++) {
				String colour = FloorPalette.hex(kind, id);
				double[] hsl = hslOf(colour);
				assertTrue(hsl[1] <= 0.35, kind + ":" + id + " is " + colour + ", saturation " + hsl[1]);
				assertTrue(hsl[2] >= 0.20 && hsl[2] <= 0.70,
						kind + ":" + id + " is " + colour + ", luminance " + hsl[2]);
			}
		}
	}

	@Test
	void anUnknownKindIsRejectedRatherThanGuessed() {
		try {
			FloorPalette.hex("overlayy", 12);
			assertTrue(false, "a misspelled kind should not silently produce a colour");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("overlayy"));
		}
	}

	@Test
	void theJsonCoversEveryIdInBothKindsAsValidCssColours() {
		String json = FloorPalette.toJson(new HashMap<String, String>());
		Pattern entry = Pattern.compile("\"(underlay|overlay):(\\d+)\": \"(#[0-9a-f]{6,8})\"");
		Matcher matcher = entry.matcher(json);
		int count = 0;
		boolean sawMaxId = false;
		while (matcher.find()) {
			count++;
			int id = Integer.parseInt(matcher.group(2));
			assertTrue(id <= FloorPalette.MAX_ID, "id past the covered range: " + id);
			sawMaxId |= id == FloorPalette.MAX_ID;
		}
		// 512 = 2 kinds x 256 ids. The viewer looks every id up by key, so a missing one is a hole
		// in the map rather than an exception — this is what stops that being silent.
		assertEquals(2 * (FloorPalette.MAX_ID + 1), count, "the palette is missing ids");
		assertTrue(sawMaxId, "the last id is not in the palette");
		assertFalse(json.contains("null"), "a null colour would render as black");
	}

	@Test
	void anOverrideReplacesTheGeneratedColourForThatKeyOnly() {
		Map<String, String> overrides = new HashMap<String, String>();
		overrides.put("underlay:48", "#123456");

		String json = FloorPalette.toJson(overrides);

		assertTrue(json.contains("\"underlay:48\": \"#123456\""), "the override was not applied");
		assertTrue(json.contains("\"overlay:48\": \"" + FloorPalette.hex("overlay", 48) + "\""),
				"an override leaked across namespaces");
		assertTrue(json.contains("\"underlay:49\": \"" + FloorPalette.hex("underlay", 49) + "\""),
				"an override leaked to a neighbouring id");
	}

	@Test
	void theOverrideFileIsParsedWithCommentsAndWhitespaceAndAlpha(@TempDir Path dir)
			throws IOException {
		Path file = dir.resolve("floor-palette.cfg");
		Files.writeString(file, "# a comment\n"
				+ "\n"
				+ "   \n"
				+ "underlay:48 = #3F6B2F\n"
				+ "overlay:10=#a89070aa\n"
				+ "  overlay:   11   =   #ffffff  \n", StandardCharsets.UTF_8);

		Map<String, String> overrides = FloorPalette.loadOverrides(file);

		assertEquals(3, overrides.size(), "expected exactly the three real lines: " + overrides);
		assertEquals("#3f6b2f", overrides.get("underlay:48"), "colours are lowercased for comparison");
		assertEquals("#a89070aa", overrides.get("overlay:10"), "8-digit alpha should be accepted");
		assertEquals("#ffffff", overrides.get("overlay:11"), "key whitespace should be tolerated");
	}

	@Test
	void aTypoInTheOverrideFileIsSkippedWithoutLosingTheGoodLines(@TempDir Path dir)
			throws IOException {
		// The file is decoration. Refusing to open the viewer because one colour has a typo would
		// be the wrong trade, but silently ignoring the whole file would be too.
		Path file = dir.resolve("floor-palette.cfg");
		Files.writeString(file, "underlay:48 = #3f6b2f\n"
				+ "underlay48 = #3f6b2f\n"          // no colon
				+ "underlay:notanumber = #3f6b2f\n" // id is not a number
				+ "overlay:999 = #3f6b2f\n"         // id past what the palette covers
				+ "underlay:10 = 3f6b2f\n"          // missing the hash
				+ "underlay:11 = #gggggg\n"         // not hex
				+ "underlay:12 = #fff\n"            // wrong length
				+ "overlay:10 = #a89070\n", StandardCharsets.UTF_8);

		Map<String, String> overrides = FloorPalette.loadOverrides(file);

		assertEquals(2, overrides.size(),
				"only the two well-formed lines should survive, got " + overrides);
		assertEquals("#3f6b2f", overrides.get("underlay:48"));
		assertEquals("#a89070", overrides.get("overlay:10"));
	}

	@Test
	void aMissingOverrideFileIsAnEmptyPaletteNotAnError(@TempDir Path dir) throws IOException {
		assertTrue(FloorPalette.loadOverrides(null).isEmpty());
		assertTrue(FloorPalette.loadOverrides(dir.resolve("does-not-exist.cfg")).isEmpty());
		// A directory in the file's place is the same story: nothing to read, nothing to apply.
		assertTrue(FloorPalette.loadOverrides(dir).isEmpty());
	}

	/** Largest per-channel difference. Two colours are "visibly the same" when it is small. */
	private static int channelDelta(String a, String b) {
		int worst = 0;
		for (int channel = 0; channel < 3; channel++) {
			int offset = 1 + channel * 2;
			int left = Integer.parseInt(a.substring(offset, offset + 2), 16);
			int right = Integer.parseInt(b.substring(offset, offset + 2), 16);
			worst = Math.max(worst, Math.abs(left - right));
		}
		return worst;
	}

	/** The hue of a {@code #rrggbb} colour, 0..1, computed from the channels. */
	private static double hueOf(String colour) {
		return hslOf(colour)[0];
	}

	/** {@code [hue, saturation, luminance]} of a {@code #rrggbb} colour. */
	private static double[] hslOf(String colour) {
		double r = Integer.parseInt(colour.substring(1, 3), 16) / 255.0;
		double g = Integer.parseInt(colour.substring(3, 5), 16) / 255.0;
		double b = Integer.parseInt(colour.substring(5, 7), 16) / 255.0;
		double max = Math.max(r, Math.max(g, b));
		double min = Math.min(r, Math.min(g, b));
		double luminance = (max + min) / 2;
		double delta = max - min;
		if (delta == 0) {
			return new double[] { 0, 0, luminance };
		}
		double saturation = delta / (1 - Math.abs(2 * luminance - 1));
		double hue;
		if (max == r) {
			hue = ((g - b) / delta) % 6;
		} else if (max == g) {
			hue = (b - r) / delta + 2;
		} else {
			hue = (r - g) / delta + 4;
		}
		hue /= 6;
		if (hue < 0) {
			hue += 1;
		}
		return new double[] { hue, saturation, luminance };
	}
}
