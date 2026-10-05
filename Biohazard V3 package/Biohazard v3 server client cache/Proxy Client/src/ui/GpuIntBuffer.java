package ui;

import java.util.Arrays;

/**
 * A growable int buffer for the GPU path (Phase 5.2) - the index/colour sibling
 * of {@link GpuFloatBuffer}.
 *
 * <p><b>Why a second class rather than one generic buffer.</b> Java arrays are
 * not covariant over primitives, so a single type would either box every value -
 * the allocation this path exists to avoid - or store an {@code Object[]} and
 * cast on every access. Two mirrored primitive buffers are the cheaper and
 * clearer answer, and it is what the RuneLite pattern does.
 *
 * <p>⚠️ <b>The cost of that choice is divergence risk</b>: two near-identical
 * classes drift unless something forces them to agree. Both classes therefore
 * carry the same method set with the same semantics, and the harness runs the
 * identical operation sequence through both and asserts they report identical
 * positions and capacities. <b>If you change the growth or position behaviour
 * here, change it there too, or that test is what will catch you.</b>
 *
 * <p>The contracts match {@link GpuFloatBuffer} exactly, including the two that
 * matter:
 * <ol>
 *   <li>{@link #array()} is <b>live storage, not a copy</b>; only the first
 *       {@link #position()} entries are valid.</li>
 *   <li>No limit, no flip - a write always appends past {@code position()}.</li>
 * </ol>
 *
 * <p>⚠️ Not thread-safe, by design.
 */
public final class GpuIntBuffer {

	/** Enough for a few thousand indices before the first grow. */
	private static final int DEFAULT_CAPACITY = 4096;

	private int[] data;
	private int position;

	public GpuIntBuffer() {
		this(DEFAULT_CAPACITY);
	}

	public GpuIntBuffer(int initialCapacity) {
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException("initial capacity must be positive: " + initialCapacity);
		}
		data = new int[initialCapacity];
	}

	/**
	 * Grows the backing array if it cannot hold {@code required} entries.
	 *
	 * <p>⚠️ Never shrinks and never discards contents - calling it below the
	 * current capacity is a no-op.
	 */
	public GpuIntBuffer ensureCapacity(int required) {
		if (required <= data.length) {
			return this;
		}
		int grown = data.length;
		while (grown < required) {
			int doubled = grown << 1;
			if (doubled <= grown) {
				grown = required;
				break;
			}
			grown = doubled;
		}
		data = Arrays.copyOf(data, grown);
		return this;
	}

	public GpuIntBuffer put(int v) {
		ensureCapacity(position + 1);
		data[position++] = v;
		return this;
	}

	public GpuIntBuffer put(int a, int b) {
		ensureCapacity(position + 2);
		data[position++] = a;
		data[position++] = b;
		return this;
	}

	public GpuIntBuffer put(int a, int b, int c) {
		ensureCapacity(position + 3);
		data[position++] = a;
		data[position++] = b;
		data[position++] = c;
		return this;
	}

	/** Appends a triangle as three indices - the unit a face becomes on upload. */
	public GpuIntBuffer putTriangle(int a, int b, int c) {
		return put(a, b, c);
	}

	/** Appends a window of another array. The bulk path. */
	public GpuIntBuffer put(int[] src, int offset, int length) {
		if (length == 0) {
			return this;
		}
		ensureCapacity(position + length);
		System.arraycopy(src, offset, data, position, length);
		position += length;
		return this;
	}

	/** Number of entries written since the last {@link #clear()}. */
	public int position() {
		return position;
	}

	/** Current capacity of the backing array. Never shrinks. */
	public int capacity() {
		return data.length;
	}

	public boolean isEmpty() {
		return position == 0;
	}

	/**
	 * The live backing array. Only the first {@link #position()} entries are valid.
	 * ⚠️ Live storage - do not mutate, do not rely on entries past {@code position()}.
	 */
	public int[] array() {
		return data;
	}

	/** Reads a written entry. Fails fast rather than returning stale data. */
	public int get(int index) {
		if (index < 0 || index >= position) {
			throw new IndexOutOfBoundsException("index " + index + " outside written range 0.." + (position - 1));
		}
		return data[index];
	}

	/**
	 * Resets the write position to zero, keeping the backing array so the next
	 * frame reuses it. ⚠️ <b>This is the method that makes the buffer allocation-free
	 * in steady state.</b>
	 */
	public void clear() {
		position = 0;
	}
}
