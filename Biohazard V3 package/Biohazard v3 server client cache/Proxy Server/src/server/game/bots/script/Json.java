package server.game.bots.script;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader, just enough for a bot script document.
 *
 * <p><b>Why this exists rather than a dependency.</b> The server has no JSON parser, and the workshop's
 * one lives in the {@code workshop} source set — which the server must not depend on, because the tool has
 * to be deletable without touching the server ({@code BOT_TOOLING.md} §11). A script document is a small,
 * fully-specified subset of JSON, so the honest options were "write thirty lines" or "add a library and a
 * coupling". This is the thirty lines, and it lives in the bot package so deleting the whole bot system
 * still takes this with it.
 *
 * <p><b>It is a reader only.</b> The tool owns the writing half ({@code BOT_TOOLING.md} §7), so there is
 * no serialiser here to drift from it.
 *
 * <p><b>Failures name an offset and a line/column.</b> A hand-authored document is edited by a human, and
 * "unexpected character at 214" is not an error message; "line 12, column 9" is.
 *
 * <p>Values come back as {@link Map}&lt;String,Object&gt; for objects, {@link List}&lt;Object&gt; for
 * arrays, {@link String}, {@link Boolean}, {@link Double} for every number, or {@code null}. Numbers are
 * uniform doubles — JSON has no integer type — and integral-ness is decided by the caller that wants an
 * {@code int}, so there is exactly one number representation to reason about.
 */
public final class Json {

	/** Thrown for any malformed input, with the position that broke it. */
	public static final class JsonException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		JsonException(String message) {
			super(message);
		}
	}

	private final String text;
	private int pos;

	private Json(String text) {
		this.text = text;
	}

	/**
	 * Parses exactly one JSON value, which may be an object, array, string, number, boolean or null.
	 *
	 * @throws JsonException if the text is malformed or has trailing content after the value
	 */
	public static Object parse(String text) {
		if (text == null) {
			throw new JsonException("no JSON given");
		}
		Json json = new Json(text);
		Object value = json.value();
		json.skipWhitespace();
		if (json.pos < text.length()) {
			throw json.error("trailing content after the value");
		}
		return value;
	}

	private Object value() {
		skipWhitespace();
		if (pos >= text.length()) {
			throw error("unexpected end of input");
		}
		char c = text.charAt(pos);
		if (c == '{') {
			return object();
		}
		if (c == '[') {
			return array();
		}
		if (c == '"') {
			return string();
		}
		if (c == 't') {
			return literal("true", Boolean.TRUE);
		}
		if (c == 'f') {
			return literal("false", Boolean.FALSE);
		}
		if (c == 'n') {
			return literal("null", null);
		}
		if (c == '-' || (c >= '0' && c <= '9')) {
			return number();
		}
		throw error("unexpected character '" + c + "'");
	}

	private Map<String, Object> object() {
		expect('{');
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		skipWhitespace();
		if (peek() == '}') {
			pos++;
			return map;
		}
		while (true) {
			skipWhitespace();
			if (peek() != '"') {
				throw error("expected a property name in quotes");
			}
			String key = string();
			skipWhitespace();
			expect(':');
			Object value = value();
			if (map.containsKey(key)) {
				// JSON permits it; a script document has no reason to, and silently keeping the last one
				// is how an author's edit appears to do nothing.
				throw error("duplicate key \"" + key + "\"");
			}
			map.put(key, value);
			skipWhitespace();
			char c = next();
			if (c == '}') {
				return map;
			}
			if (c != ',') {
				throw error("expected ',' or '}' but found '" + c + "'");
			}
		}
	}

	private List<Object> array() {
		expect('[');
		List<Object> list = new ArrayList<Object>();
		skipWhitespace();
		if (peek() == ']') {
			pos++;
			return list;
		}
		while (true) {
			list.add(value());
			skipWhitespace();
			char c = next();
			if (c == ']') {
				return list;
			}
			if (c != ',') {
				throw error("expected ',' or ']' but found '" + c + "'");
			}
		}
	}

	private String string() {
		expect('"');
		StringBuilder out = new StringBuilder();
		while (true) {
			if (pos >= text.length()) {
				throw error("unterminated string");
			}
			char c = text.charAt(pos++);
			if (c == '"') {
				return out.toString();
			}
			if (c == '\\') {
				out.append(escape());
				continue;
			}
			if (c < 0x20) {
				throw error("unescaped control character in a string");
			}
			out.append(c);
		}
	}

	private char escape() {
		if (pos >= text.length()) {
			throw error("unterminated escape");
		}
		char c = text.charAt(pos++);
		switch (c) {
		case '"':
			return '"';
		case '\\':
			return '\\';
		case '/':
			return '/';
		case 'b':
			return '\b';
		case 'f':
			return '\f';
		case 'n':
			return '\n';
		case 'r':
			return '\r';
		case 't':
			return '\t';
		case 'u':
			if (pos + 4 > text.length()) {
				throw error("truncated \\u escape");
			}
			String hex = text.substring(pos, pos + 4);
			pos += 4;
			try {
				return (char) Integer.parseInt(hex, 16);
			} catch (NumberFormatException e) {
				throw error("bad \\u escape \"" + hex + "\"");
			}
		default:
			throw error("unknown escape \\" + c);
		}
	}

	private Double number() {
		int start = pos;
		if (peek() == '-') {
			pos++;
		}
		while (pos < text.length()) {
			char c = text.charAt(pos);
			boolean part = (c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
			if (!part) {
				break;
			}
			pos++;
		}
		String literal = text.substring(start, pos);
		try {
			return Double.valueOf(literal);
		} catch (NumberFormatException e) {
			pos = start;
			throw error("bad number \"" + literal + "\"");
		}
	}

	private Object literal(String word, Object value) {
		if (!text.startsWith(word, pos)) {
			throw error("expected \"" + word + "\"");
		}
		pos += word.length();
		return value;
	}

	private void skipWhitespace() {
		while (pos < text.length()) {
			char c = text.charAt(pos);
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
				pos++;
				continue;
			}
			break;
		}
	}

	private char peek() {
		return pos < text.length() ? text.charAt(pos) : '\0';
	}

	private char next() {
		if (pos >= text.length()) {
			throw error("unexpected end of input");
		}
		return text.charAt(pos++);
	}

	private void expect(char expected) {
		if (peek() != expected) {
			throw error("expected '" + expected + "'");
		}
		pos++;
	}

	/** The message plus the line and column, which is the part a human editing a file actually needs. */
	private JsonException error(String message) {
		int line = 1;
		int column = 1;
		for (int i = 0; i < pos && i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				line++;
				column = 1;
			} else {
				column++;
			}
		}
		return new JsonException(message + " (line " + line + ", column " + column + ")");
	}
}
