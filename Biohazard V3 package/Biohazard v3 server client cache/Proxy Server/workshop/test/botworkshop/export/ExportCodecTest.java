package botworkshop.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The two small pieces the document format is built from. */
class ExportCodecTest {

	// ---- run-length coding ------------------------------------------------------------------

	@Test
	void runsOfEqualValuesCollapseToOnePair() {
		assertEquals("3*5", Rle.encode(new int[] { 3, 3, 3, 3, 3 }));
	}

	@Test
	void runsAreSplitWhereTheValueChanges() {
		assertEquals("3*2,7*1,3*2", Rle.encode(new int[] { 3, 3, 7, 3, 3 }));
	}

	@Test
	void aConstantPlaneEncodesToASinglePair() {
		int[] plane = new int[64 * 64];
		assertEquals("0*4096", Rle.encode(plane));
	}

	@Test
	void anEmptyArrayEncodesToTheEmptyString() {
		assertEquals("", Rle.encode(new int[0]));
	}

	@Test
	void decodingRestoresTheOriginalExactly() {
		int[] original = new int[4096];
		for (int i = 0; i < original.length; i++) {
			original[i] = (i / 100) % 7;
		}
		assertArrayEquals(original, Rle.decode(Rle.encode(original), original.length));
	}

	@Test
	void aTruncatedStringIsRejectedRatherThanShorteningThePlane() {
		// This is the failure that matters: a short plane would silently shift every tile after
		// the gap, drawing the map wrong instead of failing.
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> Rle.decode("3*2,7*1", 4096));
		assertTrue(e.getMessage().contains("expected 4096"), e.getMessage());
	}

	@Test
	void anEmptyStringOnlyDecodesToAnEmptyArray() {
		assertArrayEquals(new int[0], Rle.decode("", 0));
		assertThrows(IllegalArgumentException.class, () -> Rle.decode("", 10));
	}

	@Test
	void malformedRunsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> Rle.decode("3", 1));
		assertThrows(IllegalArgumentException.class, () -> Rle.decode("3*0", 0));
		assertThrows(IllegalArgumentException.class, () -> Rle.decode("x*1", 1));
	}

	// ---- json ------------------------------------------------------------------------------

	@Test
	void buildsAnObjectWithFieldsInOrder() {
		Json json = new Json();
		json.openObject().field("a", 1).field("b", "two").closeObject();
		String expected = "{\n  \"a\": 1,\n  \"b\": \"two\"\n}";
		assertEquals(expected, json.toString());
	}

	@Test
	void separatingCommasAreInsertedButNotBeforeTheFirstEntry() {
		Json json = new Json();
		json.openArray().value(1).value(2).value(3).closeArray();
		assertEquals("[\n  1,\n  2,\n  3\n]", json.toString());
	}

	@Test
	void nestsObjectsInsideArraysInsideObjects() {
		Json json = new Json();
		json.openObject().name("planes").openArray().openObject().field("plane", 0).closeObject()
				.closeArray().closeObject();
		assertEquals("{\n  \"planes\": [\n    {\n      \"plane\": 0\n    }\n  ]\n}", json.toString());
	}

	@Test
	void quotesAndControlCharactersAreEscaped() {
		Json json = new Json();
		json.openObject().field("name", "Bank \"booth\"\n\tline").closeObject();
		assertTrue(json.toString().contains("\"Bank \\\"booth\\\"\\n\\tline\""), json.toString());
	}

	@Test
	void aNullNameIsWrittenAsJsonNullNotAnEmptyString() {
		// Most of the archive is unnamed scenery. Writing "" would be a lie the editor cannot
		// tell apart from an object genuinely called "".
		Json json = new Json();
		json.openObject().field("name", (String) null).closeObject();
		assertEquals("{\n  \"name\": null\n}", json.toString());
	}

	@Test
	void anUnclosedDocumentReportsItselfInsteadOfEmittingBrokenJson() {
		Json json = new Json();
		json.openObject().field("a", 1);
		assertThrows(IllegalStateException.class, json::toString);
	}

	@Test
	void anInlineNumberArrayStaysOnOneLine() {
		Json json = new Json();
		json.openObject().name("v").inlineIntArray(new int[] { 1, 2, 3 }).closeObject();
		assertEquals("{\n  \"v\": [1,2,3]\n}", json.toString());
	}
}
