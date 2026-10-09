package botworkshop.export;

import java.util.ArrayList;
import java.util.List;

/**
 * Run-length codec for the tile planes.
 *
 * <p>A region is 4 planes x 64 x 64 = 16384 tiles, and each tile carries three small numbers. As
 * one JSON number per value that is roughly 40 KB per region and about 50 MB for the world —
 * unusable to commit and miserable to diff. Terrain is overwhelmingly repeated values, so encoding
 * each plane as {@code value*count} pairs typically shrinks it by one to two orders of magnitude
 * while staying readable in a diff.
 *
 * <p>Format: pairs joined by {@code ,}, each {@code value*count}. An empty plane is the empty
 * string. Only non-negative counts are produced, and {@link #decode} verifies the total is exactly
 * the length it was asked for — a truncated string must not silently shorten a plane and shift
 * every tile after it.
 */
public final class Rle {

	private Rle() {
	}

	public static String encode(int[] values) {
		if (values.length == 0) {
			return "";
		}
		StringBuilder out = new StringBuilder(values.length / 2);
		int runValue = values[0];
		int runLength = 1;
		for (int i = 1; i < values.length; i++) {
			if (values[i] == runValue) {
				runLength++;
			} else {
				append(out, runValue, runLength);
				runValue = values[i];
				runLength = 1;
			}
		}
		append(out, runValue, runLength);
		return out.toString();
	}

	public static int[] decode(String encoded, int expectedLength) {
		if (encoded == null || encoded.isEmpty()) {
			if (expectedLength != 0) {
				throw new IllegalArgumentException("empty run-length string for " + expectedLength + " values");
			}
			return new int[0];
		}
		List<int[]> runs = new ArrayList<int[]>();
		int total = 0;
		for (String pair : encoded.split(",")) {
			int star = pair.indexOf('*');
			if (star <= 0 || star == pair.length() - 1) {
				throw new IllegalArgumentException("malformed run \"" + pair + "\"");
			}
			int value;
			int count;
			try {
				value = Integer.parseInt(pair.substring(0, star).trim());
				count = Integer.parseInt(pair.substring(star + 1).trim());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("malformed run \"" + pair + "\"");
			}
			if (count <= 0) {
				throw new IllegalArgumentException("non-positive run length in \"" + pair + "\"");
			}
			runs.add(new int[] { value, count });
			total += count;
		}
		if (total != expectedLength) {
			throw new IllegalArgumentException("run lengths total " + total + ", expected " + expectedLength);
		}
		int[] out = new int[total];
		int at = 0;
		for (int[] run : runs) {
			for (int i = 0; i < run[1]; i++) {
				out[at++] = run[0];
			}
		}
		return out;
	}

	private static void append(StringBuilder out, int value, int length) {
		if (out.length() > 0) {
			out.append(',');
		}
		out.append(value).append('*').append(length);
	}
}
