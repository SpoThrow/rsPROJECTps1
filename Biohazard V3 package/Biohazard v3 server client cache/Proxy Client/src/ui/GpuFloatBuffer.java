package ui;

import java.util.Arrays;

/**
 * A growable float buffer for the GPU path (Phase 5.2).
 *
 * <p><b>Why this exists.</b> A renderer that uploads geometry has to assemble
 * vertex data somewhere before it is handed to the GL layer, and doing that with
 * a fresh {@code float[]} per model per frame would allocate at exactly the rate
 * the GPU path exists to avoid. This is the RuneLite {@code GpuFloatBuffer}
 * pattern: a plain data holder that grows on demand and is <i>reused</i> across
 * frames. ⚠️ <b>It makes no GL calls and knows nothing about OpenGL</b> - it is a
 * buffer, not a renderer, so it can be tested without a context (Phase 5.3).
 *
 * <p><b>How it is meant to be used.</b> One buffer is kept per drawing pass and
 * <i>not</i> discarded: {@link #clear()} resets the write position while keeping
 * the backing array, so after a short warm-up a steady scene reuses the same
 * storage and allocates nothing.
 *
 * <p><b>The two contracts worth reading before using it.</b>
 * <ol>
 *   <li>{@link #array()} returns the <b>live backing array, never a copy</b> - a
 *       copy would defeat the point. Only the first {@link #position()} entries
 *       are meaningful; the rest is stale data from earlier frames. Callers must
 *       not mutate the array, and must not read past {@code position()}.</li>
 *   <li>The signature of this class deliberately overlaps the {@code java.nio}
 *       buffer vocabulary ({@code put}, {@code position}, {@code clear}) but it
 *       has <b>no limit or flip</b>: a write is always an append past
 *       {@code position()}, which is the whole of its state. That is a smaller
 *       surface than a real {@code Buffer} and is what keeps it testable here.</li>
 * </ol>
 *
 * <p>⚠️ <b>Not thread-safe, by design.</b> It is written from the drawing thread
 * and nothing else.
 */
public final class GpuFloatBuffer {

	/** Enough for a few hundred vertices before the first grow. */
	private static final int DEFAULT_CAPACITY = 4096;

	private float[] data;
	private int position;

	public GpuFloatBuffer() {
		this(DEFAULT_CAPACITY);
	}

	public GpuFloatBuffer(int initialCapacity) {
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException("initial capacity must be positive: " + initialCapacity);
		}
		data = new float[initialCapacity];
	}

	/**
	 * Grows the backing array if it cannot hold {@code required} entries.
	 *
	 * <p>⚠️ <b>Never shrinks and never discards contents</b> - calling it with a
	 * smaller value than the current capacity is a no-op, so it is safe to call
	 * with a per-frame requirement that fluctuates.
	 */
	public GpuFloatBuffer ensureCapacity(int required) {
		if (required <= data.length) {
			return this;
		}
		int grown = data.length;
		while (grown < required) {
			int doubled = grown << 1;
			// Overflow guard: keep doubling until it would wrap, then jump straight
			// to what was asked for. An int index cannot address more than this
			// anyway, so the guard is about not silently allocating a tiny array.
			if (doubled <= grown) {
				grown = required;
				break;
			}
			grown = doubled;
		}
		data = Arrays.copyOf(data, grown);
		return this;
	}

	public GpuFloatBuffer put(float v) {
		ensureCapacity(position + 1);
		data[position++] = v;
		return this;
	}

	public GpuFloatBuffer put(float a, float b) {
		ensureCapacity(position + 2);
		data[position++] = a;
		data[position++] = b;
		return this;
	}

	public GpuFloatBuffer put(float a, float b, float c) {
		ensureCapacity(position + 3);
		data[position++] = a;
		data[position++] = b;
		data[position++] = c;
		return this;
	}

	public GpuFloatBuffer put(float a, float b, float c, float d) {
		ensureCapacity(position + 4);
		data[position++] = a;
		data[position++] = b;
		data[position++] = c;
		data[position++] = d;
		return this;
	}

	/**
	 * Appends a window of another array.
	 *
	 * <p>Copies with {@link System#arraycopy} rather than a loop, because this is
	 * the bulk path the per-frame upload will use.
	 */
	public GpuFloatBuffer put(float[] src, int offset, int length) {
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
	public float[] array() {
		return data;
	}

	/** Reads a written entry. Fails fast rather than returning stale data. */
	public float get(int index) {
		if (index < 0 || index >= position) {
			throw new IndexOutOfBoundsException("index " + index + " outside written range 0.." + (position - 1));
		}
		return data[index];
	}

	/**
	 * Resets the write position to zero, keeping the backing array so the next
	 * frame reuses it. ⚠️ <b>This is the method that makes the buffer allocation-free
	 * in steady state</b> - emptying it must not mean freeing it.
	 */
	public void clear() {
		position = 0;
	}
}
