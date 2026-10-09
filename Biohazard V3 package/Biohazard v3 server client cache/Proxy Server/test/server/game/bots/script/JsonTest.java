package server.game.bots.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * The JSON reader, on its own.
 *
 * <p>Worth its own tests rather than being covered incidentally through the document loader: a parser bug
 * shows up as a confusing document error, and the position arithmetic in particular is easy to get subtly
 * wrong in a way that only bites on a large file.
 */
class JsonTest {

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(String text) {
		return (Map<String, Object>) Json.parse(text);
	}

	@SuppressWarnings("unchecked")
	private static List<Object> list(String text) {
		return (List<Object>) Json.parse(text);
	}

	@Test
	void parsesEveryScalarType() {
		Map<String, Object> values = map("{\"a\": 1, \"b\": \"x\", \"c\": true, \"d\": false, "
				+ "\"e\": null, \"f\": 2.5}");

		// Numbers are uniform doubles — JSON has no integer type, and integral-ness is decided by the
		// caller that wants an int.
		assertEquals(1.0, values.get("a"));
		assertEquals("x", values.get("b"));
		assertEquals(Boolean.TRUE, values.get("c"));
		assertEquals(Boolean.FALSE, values.get("d"));
		assertNull(values.get("e"));
		assertEquals(2.5, values.get("f"));
	}

	@Test
	void numbersComeBackAsDoubles() {
		assertTrue(Json.parse("7") instanceof Double);
		assertEquals(-0.5, Json.parse("-0.5"));
		assertEquals(1500.0, Json.parse("1.5e3"));
		assertEquals(1000.0, Json.parse("1E3"));
	}

	@Test
	void parsesNestedObjectsAndArrays() {
		Map<String, Object> doc = map("{\"root\": {\"node\": \"sequence\", \"children\": [{\"n\": 1}, "
				+ "{\"n\": 2}]}}");

		@SuppressWarnings("unchecked")
		Map<String, Object> root = (Map<String, Object>) doc.get("root");
		assertEquals("sequence", root.get("node"));
		@SuppressWarnings("unchecked")
		List<Object> children = (List<Object>) root.get("children");
		assertEquals(2, children.size());
		@SuppressWarnings("unchecked")
		Map<String, Object> second = (Map<String, Object>) children.get(1);
		assertEquals(2.0, second.get("n"));
	}

	@Test
	void parsesArrayElementsOfMixedTypes() {
		List<Object> values = list("[1, \"two\", true, null, [3], {\"four\": 4}]");

		assertEquals(1.0, values.get(0));
		assertEquals("two", values.get(1));
		assertEquals(Boolean.TRUE, values.get(2));
		assertNull(values.get(3));
		assertEquals(6, values.size());
	}

	@Test
	void parsesEmptyObjectsAndArrays() {
		assertTrue(map("{}").isEmpty());
		assertTrue(list("[]").isEmpty());
		assertTrue(map("  {  }  ").isEmpty());
	}

	@Test
	void toleratesWhitespaceAndNewlines() {
		Map<String, Object> values = map("{\n\t\"a\" :\r\n 1 ,\n \"b\"\t:\t2\n}");

		assertEquals(1.0, values.get("a"));
		assertEquals(2.0, values.get("b"));
	}

	@Test
	void decodesEscapes() {
		assertEquals("a\"b\\c/d", Json.parse("\"a\\\"b\\\\c\\/d\""));
		assertEquals("tab\there", Json.parse("\"tab\\there\""));
		assertEquals("nl\nhere", Json.parse("\"nl\\nhere\""));
		assertEquals("cr\rhere", Json.parse("\"cr\\rhere\""));
	}

	@Test
	void decodesUnicodeEscapes() {
		assertEquals("A", Json.parse("\"\\u0041\""));
		assertEquals("\u00e9", Json.parse("\"\\u00e9\""));
	}

	// ---- failures ------------------------------------------------------------------------------

	@Test
	void rejectsTrailingContent() {
		assertThrows(Json.JsonException.class, () -> Json.parse("{\"a\":1} extra"));
	}

	@Test
	void rejectsAnUnterminatedString() {
		assertThrows(Json.JsonException.class, () -> Json.parse("{\"a\": \"oops}"));
	}

	@Test
	void rejectsADuplicateKey() {
		// Silently keeping the last one is how an author's edit appears to do nothing.
		Json.JsonException error = assertThrows(Json.JsonException.class,
				() -> Json.parse("{\"a\": 1, \"a\": 2}"));
		assertTrue(error.getMessage().contains("duplicate key \"a\""), error.getMessage());
	}

	@Test
	void rejectsAMissingColon() {
		assertThrows(Json.JsonException.class, () -> Json.parse("{\"a\" 1}"));
	}

	@Test
	void rejectsABadEscape() {
		assertThrows(Json.JsonException.class, () -> Json.parse("\"\\q\""));
		assertThrows(Json.JsonException.class, () -> Json.parse("\"\\u00\""));
	}

	@Test
	void rejectsAnUnexpectedCharacter() {
		assertThrows(Json.JsonException.class, () -> Json.parse("nope"));
		assertThrows(Json.JsonException.class, () -> Json.parse("{"));
		assertThrows(Json.JsonException.class, () -> Json.parse(""));
	}

	@Test
	void rejectsNullInput() {
		assertThrows(Json.JsonException.class, () -> Json.parse(null));
	}

	@Test
	void errorsNameTheLineAndColumn() {
		// The reader is for hand-authored files, so the position is the part a human needs: "at 214" is
		// not an error message, "line 3, column 5" is.
		Json.JsonException error = assertThrows(Json.JsonException.class,
				() -> Json.parse("{\n  \"a\": 1,\n   oops\n}"));
		assertTrue(error.getMessage().contains("line 3"), error.getMessage());
	}
}
