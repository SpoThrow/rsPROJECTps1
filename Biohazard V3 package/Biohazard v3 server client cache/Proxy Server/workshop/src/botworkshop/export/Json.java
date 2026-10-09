package botworkshop.export;

/**
 * A minimal, dependency-free JSON writer.
 *
 * <p>Hand-rolled rather than pulled in because the exporter's only job is to produce a file the
 * editor reads back, and the alternative is another jar on the server's classpath for a tool that
 * is supposed to be deletable. Commas and indentation are tracked from the call sequence, so the
 * callers read as the document they emit.
 *
 * <p>Output is indented and newline-per-field on purpose: these files are committed and reviewed,
 * so a diff should show the one object that changed rather than the whole line.
 */
public final class Json {

	private static final String INDENT = "  ";

	private final StringBuilder out = new StringBuilder(1 << 16);

	/** Nesting kinds, so {@link #close} can emit the right bracket. */
	private final boolean[] isArray = new boolean[64];
	private int depth;

	private boolean needComma;

	/** Set between a name and its value, so the value does not get its own separator. */
	private boolean afterName;

	public Json openObject() {
		separator();
		out.append('{');
		push(false);
		return this;
	}

	public Json closeObject() {
		close();
		return this;
	}

	public Json openArray() {
		separator();
		out.append('[');
		push(true);
		return this;
	}

	public Json closeArray() {
		close();
		return this;
	}

	/** A field name. Must be followed by exactly one value. */
	public Json name(String field) {
		separator();
		quote(field);
		out.append(": ");
		needComma = false;
		afterName = true;
		return this;
	}

	public Json value(String value) {
		separator();
		if (value == null) {
			out.append("null");
		} else {
			quote(value);
		}
		needComma = true;
		return this;
	}

	public Json value(long value) {
		separator();
		out.append(value);
		needComma = true;
		return this;
	}

	public Json value(boolean value) {
		separator();
		out.append(value);
		needComma = true;
		return this;
	}

	/** A pre-rendered value emitted verbatim — used for the encoded tile strings. */
	public Json raw(String json) {
		separator();
		out.append(json);
		needComma = true;
		return this;
	}

	public Json field(String field, String value) {
		return name(field).value(value);
	}

	public Json field(String field, long value) {
		return name(field).value(value);
	}

	public Json field(String field, boolean value) {
		return name(field).value(value);
	}

	/** A whole array of numbers on one line, as {@code [1, 2, 3]}. */
	public Json inlineIntArray(int[] values) {
		separator();
		out.append('[');
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				out.append(',');
			}
			out.append(values[i]);
		}
		out.append(']');
		needComma = true;
		return this;
	}

	private void push(boolean array) {
		if (depth >= isArray.length) {
			throw new IllegalStateException("JSON nesting past " + isArray.length);
		}
		isArray[depth++] = array;
		needComma = false;
	}

	private void close() {
		if (depth == 0) {
			throw new IllegalStateException("unbalanced close in JSON document");
		}
		boolean array = isArray[--depth];
		// An empty container is written as {} or [] with nothing between, rather than opening a
		// line it will immediately close.
		if (needComma) {
			newline();
		}
		out.append(array ? ']' : '}');
		needComma = true;
	}

	/**
	 * Starts the next entry: a comma if one is due, then a newline and the indent for the current
	 * depth. A value directly after its name is the one exception — the name already positioned it.
	 */
	private void separator() {
		if (afterName) {
			afterName = false;
			return;
		}
		if (needComma) {
			out.append(',');
		}
		if (depth > 0) {
			newline();
		}
	}

	private void newline() {
		out.append('\n');
		for (int i = 0; i < depth; i++) {
			out.append(INDENT);
		}
	}

	private void quote(String value) {
		out.append('"');
		escape(value);
		out.append('"');
	}

	private void escape(String value) {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '"':
				out.append("\\\"");
				break;
			case '\\':
				out.append("\\\\");
				break;
			case '\n':
				out.append("\\n");
				break;
			case '\r':
				out.append("\\r");
				break;
			case '\t':
				out.append("\\t");
				break;
			default:
				if (c < 0x20) {
					out.append(String.format("\\u%04x", (int) c));
				} else {
					out.append(c);
				}
			}
		}
	}

	public boolean isComplete() {
		return depth == 0;
	}

	@Override
	public String toString() {
		if (depth != 0) {
			throw new IllegalStateException("JSON document is " + depth + " level(s) unclosed");
		}
		return out.toString();
	}
}
