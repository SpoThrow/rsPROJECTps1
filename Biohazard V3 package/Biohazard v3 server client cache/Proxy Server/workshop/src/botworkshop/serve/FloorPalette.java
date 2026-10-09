package botworkshop.serve;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The tile colours the map viewer draws with.
 *
 * <p><b>Why a generated palette.</b> {@code Data/} carries no floor colour table. The terrain
 * stream gives floor <em>ids</em> and nothing else, and the client's own colours live in a
 * {@code flo} archive that is not part of this tree, so a tool whose contract is "reads what the
 * server owns" cannot have the real ones. Rather than invent a colour per id in the dark — which
 * would be a guess dressed as data — this generates a deterministic one and makes it overridable
 * from a plain-text file, so real colours can be dropped in later without touching code. The same
 * reasoning as the curated-first icon decision in {@code BOT_TOOLING.md} §1b.
 *
 * <p><b>Two namespaces, deliberately.</b> Overlay and underlay ids are drawn from different floor
 * tables, so overlay {@code 14} and underlay {@code 14} are unrelated floors. Keys are therefore
 * {@code <kind>:<id>}, and generated hues for the two kinds are offset so that a coincidence of ids
 * is still visibly two different terrains.
 *
 * <p><b>Generated once, in Java.</b> The client is served the finished palette over HTTP and never
 * computes a colour itself, so there is one implementation to test instead of one per language.
 *
 * <p>Id {@code 0} means "no floor of this kind" and is transparent, which is what lets an overlay of
 * 0 show the underlay beneath it.
 */
public final class FloorPalette {

	/** The namespaces. Overlay is painted over underlay, so the order matters to the renderer. */
	public static final List<String> KINDS = List.of("underlay", "overlay");

	/** "No floor here." Fully transparent rather than a colour, so it composites correctly. */
	public static final String NONE = "#00000000";

	/** Highest id the palette covers. The shipped cache uses ids in the low tens. */
	public static final int MAX_ID = 255;

	/**
	 * The hue step. The golden ratio conjugate spreads successive integers as far apart as an
	 * additive sequence can, so adjacent ids — which is what a terrain boundary usually is — come out
	 * clearly different instead of nearly identical.
	 */
	private static final double HUE_STEP = 0.618033988749895;

	/**
	 * A second stride for lightness, from the plastic number. Hue alone is not quite enough: 255 hues
	 * around the wheel quantise down to a 24-bit colour and two of them land on the same triple (the
	 * ids-are-distinct test caught exactly that). Varying lightness on an unrelated stride makes the
	 * id-to-colour map two-dimensional.
	 */
	private static final double LIGHTNESS_STEP = 0.7548776662466927;

	/**
	 * The band of hues the palette is confined to — about 22° to 187°, i.e. brown through yellow and
	 * green to cyan.
	 *
	 * <p>A palette spread over the whole wheel is *even*, and that is the problem: it paints grass
	 * purple and water orange, so the map reads as broken and an author cannot orient themselves in
	 * it. Confining the generated hues to the range that reads as landscape is a presentation choice
	 * and claims nothing about which id is which — it just stops the tool shouting. Nothing here
	 * says id 48 is grass; it says id 48 is not magenta.
	 */
	private static final double HUE_BAND_START = 0.06;

	private static final double HUE_BAND_WIDTH = 0.46;

	/** Exposed so the test can assert every generated hue really is inside the band. */
	public static double hueBandStart() {
		return HUE_BAND_START;
	}

	public static double hueBandWidth() {
		return HUE_BAND_WIDTH;
	}

	private FloorPalette() {
	}

	/**
	 * The generated colour for one id. Pure: the same pair always yields the same colour, which is
	 * what keeps a screenshot comparable between runs.
	 */
	public static String hex(String kind, int id) {
		if (id <= 0) {
			return NONE;
		}
		if (!KINDS.contains(kind)) {
			throw new IllegalArgumentException("unknown floor kind: " + kind);
		}
		double spread = (id * HUE_STEP) % 1.0;
		double twinkle = (id * LIGHTNESS_STEP) % 1.0;
		double saturation;
		double luminance;
		if ("overlay".equals(kind)) {
			// Half a turn in the spread space, which is about 83° once mapped into the band: far
			// enough that an id shared by the two tables is plainly two different terrains, without
			// leaving the band. Overlays are also lighter, which is what "painted on top" looks like.
			spread = (spread + 0.5) % 1.0;
			saturation = 0.22;
			luminance = 0.50 + 0.16 * twinkle;
		} else {
			saturation = 0.30;
			luminance = 0.30 + 0.18 * twinkle;
		}
		return hslToHex(HUE_BAND_START + HUE_BAND_WIDTH * spread, saturation, luminance);
	}

	/**
	 * Reads the override file. Its format is the repo's plain-text cfg idiom — one
	 * {@code <kind>:<id> = #rrggbb} per line, {@code #} comments, blank lines skipped — so it is
	 * hand-editable and git-diffable, and needs no JSON parser in the build.
	 *
	 * <p>A malformed line is reported and skipped rather than thrown on: this file is decoration, and
	 * refusing to start the viewer over a typo in a colour would be a poor trade. An unreadable file
	 * path is a real error and is thrown.
	 *
	 * @param file the override file, or {@code null} when there is none
	 * @return the parsed overrides, keyed {@code <kind>:<id>}
	 */
	public static Map<String, String> loadOverrides(Path file) throws IOException {
		Map<String, String> overrides = new LinkedHashMap<String, String>();
		if (file == null || !Files.isRegularFile(file)) {
			return overrides;
		}
		int lineNumber = 0;
		for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
			lineNumber++;
			String line = raw.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			int eq = line.indexOf('=');
			if (eq < 0) {
				System.out.println("[palette] " + file.getFileName() + ":" + lineNumber
						+ ": no '=' — skipping");
				continue;
			}
			String key = canonicalKey(line.substring(0, eq));
			String value = line.substring(eq + 1).trim();
			if (key == null || !isValidColor(value)) {
				System.out.println("[palette] " + file.getFileName() + ":" + lineNumber
						+ ": expected '<underlay|overlay>:<id> = #rrggbb', got '" + line + "' — skipping");
				continue;
			}
			overrides.put(key, value.toLowerCase(Locale.ROOT));
		}
		return overrides;
	}

	/**
	 * The whole palette as JSON, generated for {@code 0..MAX_ID} in both namespaces with the
	 * overrides applied on top.
	 */
	public static String toJson(Map<String, String> overrides) {
		Map<String, String> merged = new LinkedHashMap<String, String>();
		for (String kind : KINDS) {
			for (int id = 0; id <= MAX_ID; id++) {
				String key = kind + ":" + id;
				merged.put(key, hex(kind, id));
			}
		}
		merged.putAll(overrides);

		StringBuilder out = new StringBuilder();
		out.append("{\n");
		out.append("  \"note\": \"")
				.append("Synthetic palette: Data/ has no floor colour table, so these are generated "
						+ "from the floor id and are not the game's colours. Override any entry in "
						+ "Data/cfg/floor-palette.cfg.\"")
				.append(",\n");
		out.append("  \"kinds\": [\"underlay\", \"overlay\"],\n");
		out.append("  \"overridden\":").append(overrides.size()).append(",\n");
		out.append("  \"colors\": {\n");
		int index = 0;
		for (Map.Entry<String, String> entry : merged.entrySet()) {
			out.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue())
					.append('"');
			if (++index < merged.size()) {
				out.append(',');
			}
			out.append('\n');
		}
		out.append("  }\n");
		out.append("}\n");
		return out.toString();
	}

	/**
	 * Canonicalises {@code <kind>:<id>}, tolerating surrounding and internal whitespace — {@code
	 * "overlay:   11"} is what a hand-edited file tends to contain. Returns {@code null} for
	 * anything else.
	 *
	 * <p>Canonicalising rather than storing the raw text matters: the viewer looks colours up by
	 * {@code kind + ":" + id}, so a key kept verbatim would be an override that silently never
	 * applies.
	 */
	private static String canonicalKey(String key) {
		int colon = key.indexOf(':');
		if (colon <= 0 || colon == key.length() - 1) {
			return null;
		}
		String kind = key.substring(0, colon).trim();
		if (!KINDS.contains(kind)) {
			return null;
		}
		int id;
		try {
			id = Integer.parseInt(key.substring(colon + 1).trim());
		} catch (NumberFormatException e) {
			return null;
		}
		if (id < 0 || id > MAX_ID) {
			return null;
		}
		return kind + ":" + id;
	}

	/** {@code #rrggbb} or {@code #rrggbbaa}, because id 0's transparency is expressed that way. */
	private static boolean isValidColor(String value) {
		if (value.length() != 7 && value.length() != 9) {
			return false;
		}
		if (value.charAt(0) != '#') {
			return false;
		}
		for (int i = 1; i < value.length(); i++) {
			if (Character.digit(value.charAt(i), 16) < 0) {
				return false;
			}
		}
		return true;
	}

	private static String hslToHex(double hue, double saturation, double luminance) {
		double r = channel(hue + 1.0 / 3.0, saturation, luminance);
		double g = channel(hue, saturation, luminance);
		double b = channel(hue - 1.0 / 3.0, saturation, luminance);
		return String.format(Locale.ROOT, "#%02x%02x%02x", Math.round(r * 255), Math.round(g * 255),
				Math.round(b * 255));
	}

	private static double channel(double hue, double saturation, double luminance) {
		double h = hue;
		if (h < 0) {
			h += 1.0;
		}
		if (h > 1) {
			h -= 1.0;
		}
		double q = luminance < 0.5 ? luminance * (1 + saturation)
				: luminance + saturation - luminance * saturation;
		double p = 2 * luminance - q;
		if (h < 1.0 / 6.0) {
			return p + (q - p) * 6 * h;
		}
		if (h < 1.0 / 2.0) {
			return q;
		}
		if (h < 2.0 / 3.0) {
			return p + (q - p) * (2.0 / 3.0 - h) * 6;
		}
		return p;
	}
}
