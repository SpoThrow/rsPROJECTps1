package ui;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * The GPU triangle sink (Phase 7.2b-1).
 *
 * <p><b>What this is.</b> The lowest layer of the GL scene path: it takes triangles
 * in <i>screen-pixel space</i> with a packed ARGB colour each, accumulates them into
 * growable buffers, uploads them to a single VBO, draws them into {@link GlScene}'s
 * offscreen framebuffer with a depth test, and reads the result back into an
 * {@code int[]} in the client's own pixel format.
 *
 * <p><b>Why screen-pixel input rather than a model matrix.</b> The two things the
 * scene submits do not arrive in the same space, and that asymmetry is the whole
 * reason 7.2 is staged:
 * <ul>
 *   <li><b>Ground</b> is handed to the software rasteriser ALREADY projected
 *       ({@link SceneRasterizer.Implementation#drawGroundTriangle} - "already
 *       projected to screen space"), so a GPU path receives pixels directly.</li>
 *   <li><b>Models</b> arrive in MODEL space with the camera as
 *       {@code camA..camD} sin/cos and {@code dx/dy/dz}, and the projection is
 *       performed INSIDE {@code Model.method443}. A GPU path must therefore
 *       reproduce that projection itself - it cannot read the result out of the
 *       software path, because those screen-space arrays are {@code static}
 *       scratch valid only mid-draw (Phase 5.1's finding).</li>
 * </ul>
 * So this class deliberately stops at "draw these pixel-space triangles"; whatever
 * produces those triangles - the ground seam directly, or a model projection built
 * for 7.2b-2 - is a caller concern. That keeps the one piece that must be exactly
 * right (the upload, the depth test, the readback format) separable and testable.
 *
 * <p><b>Threading is absolute: every method here must run on the GAME THREAD.</b>
 * The context is created there by {@link GlScene#ensure()} and is current only on
 * that thread. Nothing here is thread-safe and nothing here may be called from the
 * EDT - see {@link GlScene}'s class doc for why that split is the safe one.
 *
 * <p><b>Failure is a DECLINE, never a crash.</b> If {@link GlScene} is unavailable -
 * no LWJGL jars, no driver, incomplete FBO - {@link #beginFrame} returns
 * {@code false}, every later call becomes a no-op, and the caller keeps using the
 * software path. That mirrors {@link GlScene} and Phase 2's defensive posture.
 *
 * <p><b>The readback format is the contract that matters most.</b> The client's
 * buffers are {@code 0x00RRGGBB} - see {@code RSImageProducer}'s
 * {@code DirectColorModel(32, 0xff0000, 0xff00, 0xff)} - and {@code glReadPixels}
 * returns rows bottom-up, so {@link #readInto} flips Y and drops alpha. Getting
 * either wrong produces a picture that is subtly wrong rather than obviously
 * broken, so both are asserted by the probe rather than assumed.
 */
public final class GlBatcher implements TriangleSink {

	/**
	 * Position attribute: pixel x/y (y growing DOWN, matching the screen) plus z in
	 * {@code [0,1]} where 0 is nearest. Three floats.
	 */
	private static final int POSITION_COMPONENTS = 3;

	/**
	 * Colour attribute: one packed ARGB int per vertex, uploaded as four normalised
	 * bytes.
	 *
	 * <p>⚠️ <b>Byte order is why the vertex shader swizzles.</b> An {@code int}
	 * {@code 0xAARRGGBB} in little-endian memory is bytes {@code B,G,R,A}, so the
	 * attribute arrives as {@code (b,g,r,a)} and the shader reorders it to RGBA. This
	 * is exactly the kind of thing that renders "almost right" - red and blue
	 * swapped - so the probe checks a known colour rather than trusting the swizzle.
	 */
	private static final int COLOUR_COMPONENTS = 4;

	private static final String VERTEX_SOURCE = "#version 330 core\n"
			+ "layout(location = 0) in vec3 aPos;\n"
			+ "layout(location = 1) in vec4 aCol;\n"
			+ "uniform vec2 uViewport;\n"
			+ "out vec4 vCol;\n"
			+ "void main() {\n"
			+ "    float x = (aPos.x / uViewport.x) * 2.0 - 1.0;\n"
			+ "    float y = 1.0 - (aPos.y / uViewport.y) * 2.0;\n"
			+ "    float z = aPos.z * 2.0 - 1.0;\n"
			+ "    gl_Position = vec4(x, y, z, 1.0);\n"
			+ "    vCol = aCol.bgra;\n"
			+ "}\n";

	private static final String FRAGMENT_SOURCE = "#version 330 core\n"
			+ "in vec4 vCol;\n"
			+ "out vec4 fragColor;\n"
			+ "void main() { fragColor = vCol; }\n";

	/** Grown once and reused: the per-frame allocation this path exists to avoid. */
	private final GpuFloatBuffer positions = new GpuFloatBuffer();
	private final GpuIntBuffer colours = new GpuIntBuffer();

	/** Allocated on first readback and reused; never shrinks. */
	private ByteBuffer readback;

	private boolean attempted;
	private boolean ready;
	private String failure;

	private int program = -1;
	private int vao;
	private int positionVbo;
	private int colourVbo;
	private int uViewport = -1;

	private int frameWidth;
	private int frameHeight;

	/**
	 * Builds the shader program and the VAO/VBOs if it has not been tried yet.
	 *
	 * <p>Idempotent, like {@link GlScene#ensure()}: the attempt happens once and a
	 * failure is not retried, so a broken driver costs one message rather than one
	 * per frame.
	 *
	 * @return {@code true} if triangles can be drawn
	 */
	public synchronized boolean ensure() {
		if (attempted) {
			return ready;
		}
		attempted = true;
		if (!GlScene.ensure()) {
			ready = false;
			failure = "GlScene unavailable: " + GlScene.unavailableReason();
			return false;
		}
		try {
			createDeviceObjects();
			ready = true;
		} catch (Throwable t) {
			// Throwable for the same reason GlScene catches it: NoClassDefFoundError and
			// UnsatisfiedLinkError are realistic here and must degrade, not kill.
			ready = false;
			failure = describe(t);
			destroyQuietly();
		}
		return ready;
	}

	private void createDeviceObjects() {
		program = buildProgram();
		if (program == 0) {
			throw new IllegalStateException("shader program failed to build");
		}
		uViewport = GL20.glGetUniformLocation(program, "uViewport");

		vao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vao);

		positionVbo = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVbo);
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(0, POSITION_COMPONENTS, GL11.GL_FLOAT, false,
				POSITION_COMPONENTS * Float.BYTES, 0L);

		colourVbo = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colourVbo);
		GL20.glEnableVertexAttribArray(1);
		// Normalised bytes: the packed int arrives as (b,g,r,a) and the shader swizzles.
		GL20.glVertexAttribPointer(1, COLOUR_COMPONENTS, GL11.GL_UNSIGNED_BYTE, true,
				COLOUR_COMPONENTS, 0L);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
	}

	/**
	 * Starts a frame: makes {@link GlScene}'s framebuffer current, sets the viewport,
	 * clears it, and empties the batch.
	 *
	 * <p>Depth testing is switched back ON every frame rather than assumed, because
	 * the software compositing step runs after this and other GL users could disable
	 * it - a depth test that silently stayed off would paint triangles in submission
	 * order, which looks plausible and is wrong.
	 *
	 * @param clearArgb the framebuffer clear colour, {@code 0x00RRGGBB}
	 * @return {@code true} if a frame is in progress and triangles will draw
	 */
	public boolean beginFrame(int clearArgb) {
		if (!ensure()) {
			return false;
		}
		frameWidth = GlScene.width();
		frameHeight = GlScene.height();

		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, GlScene.framebufferId());
		GL11.glViewport(0, 0, frameWidth, frameHeight);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glClearColor(((clearArgb >> 16) & 0xff) / 255f, ((clearArgb >> 8) & 0xff) / 255f,
				(clearArgb & 0xff) / 255f, 1f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		positions.clear();
		colours.clear();
		return true;
	}

	/**
	 * Queues one triangle.
	 *
	 * @param x0,y0,z0 first vertex, pixel x/y with y growing DOWN, z in {@code [0,1]}
	 *                 where 0 is nearest
	 * @param argb0    first vertex colour, packed ARGB
	 * @return {@code true} if the triangle was queued (false when no frame is running)
	 */
	@Override
	public boolean triangle(float x0, float y0, float z0, int argb0,
			float x1, float y1, float z1, int argb1,
			float x2, float y2, float z2, int argb2) {
		if (!ready) {
			return false;
		}
		positions.put(x0, y0, z0).put(x1, y1, z1).put(x2, y2, z2);
		colours.putTriangle(argb0, argb1, argb2);
		return true;
	}

	/**
	 * ⚠ <b>{@code false}, and it must stay false until there is an atlas to sample.</b>
	 * This class has no texture storage, no second UV attribute and no sampler, so a
	 * textured triangle handed to it could only be dropped - and a dropped triangle that
	 * the pipeline had already counted as drawn would make
	 * {@link GlFacePipeline#allRepresentable()} lie to the wiring step, which is the one
	 * question that step has to be able to trust. Declining here makes the pipeline count
	 * textured faces as {@link GlFacePipeline#NEEDS_TEXTURE} instead, which is true.
	 *
	 * @return {@code false}; the texture path is the next step
	 */
	@Override
	public boolean supportsTextures() {
		return false;
	}

	/**
	 * ⚠ <b>Declines, unconditionally, and does not fall back to drawing the face
	 * untextured.</b> A textured face's colour slot holds a TEXTURE ID and its per-corner
	 * values are shade codes rather than colours (see {@code Model.faceTextureId}), so
	 * painting it flat would produce a wildly wrong colour rather than a slightly wrong
	 * one - the same reason {@link #supportsTextures()} returns {@code false} rather than
	 * something optimistic.
	 *
	 * @return {@code false} - nothing was queued
	 */
	@Override
	public boolean textured(float x0, float y0, float z0, float u0, float v0, float w0, int shade0,
			float x1, float y1, float z1, float u1, float v1, float w1, int shade1,
			float x2, float y2, float z2, float u2, float v2, float w2, int shade2,
			int textureId) {
		return false;
	}

	/** Number of triangles queued since {@link #beginFrame}. */
	public int triangleCount() {
		return positions.position() / (POSITION_COMPONENTS * 3);
	}

	/**
	 * Uploads and draws everything queued since {@link #beginFrame}.
	 *
	 * <p>Called once per frame, not per triangle: the whole point of buffering is
	 * that N triangles cost one upload and one draw call.
	 *
	 * @return {@code true} if a draw was issued
	 */
	public boolean flush() {
		if (!ready || positions.isEmpty()) {
			return false;
		}
		GL30.glBindVertexArray(vao);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionVbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, positions.capacity() * Float.BYTES,
				GL15.GL_STREAM_DRAW);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, toFloatBuffer(positions));

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colourVbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, colours.capacity() * Integer.BYTES,
				GL15.GL_STREAM_DRAW);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, toIntBuffer(colours));

		GL20.glUseProgram(program);
		GL20.glUniform2f(uViewport, frameWidth, frameHeight);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, positions.position() / POSITION_COMPONENTS);

		GL30.glBindVertexArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		return true;
	}

	/**
	 * Reads the framebuffer back into {@code dest}, in the client's pixel format.
	 *
	 * <p><b>The two conversions, both load-bearing:</b>
	 * <ol>
	 *   <li><b>Y flip.</b> {@code glReadPixels} returns rows bottom-up; the client's
	 *       buffers are top-down. Row 0 of the readback is the BOTTOM of the picture.</li>
	 *   <li><b>Alpha dropped.</b> The client's {@code DirectColorModel} has no alpha
	 *       mask, and the software path writes {@code 0x00RRGGBB}, so the byte must not
	 *       be invented as {@code 0xFF}.</li>
	 * </ol>
	 *
	 * @param dest       destination pixels, at least {@code width * height} long
	 * @param destStride pixels per destination row (usually the destination width)
	 * @param destX      column in {@code dest} to start writing at
	 * @param destY      row in {@code dest} to start writing at
	 * @return {@code true} if pixels were written
	 */
	public boolean readInto(int[] dest, int destStride, int destX, int destY) {
		if (!ready || dest == null || frameWidth <= 0 || frameHeight <= 0) {
			return false;
		}
		int needed = frameWidth * frameHeight * 4;
		if (readback == null || readback.capacity() < needed) {
			readback = BufferUtils.createByteBuffer(needed);
		}
		readback.clear();

		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, GlScene.framebufferId());
		GL11.glReadPixels(0, 0, frameWidth, frameHeight, GL11.GL_RGBA,
				GL11.GL_UNSIGNED_BYTE, readback);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

		for (int y = 0; y < frameHeight; y++) {
			int srcRow = (frameHeight - 1 - y) * frameWidth * 4;
			int dstRow = (destY + y) * destStride + destX;
			for (int x = 0; x < frameWidth; x++) {
				int o = srcRow + x * 4;
				int r = readback.get(o) & 0xff;
				int g = readback.get(o + 1) & 0xff;
				int b = readback.get(o + 2) & 0xff;
				dest[dstRow + x] = (r << 16) | (g << 8) | b;
			}
		}
		return true;
	}

	/** Whether a frame target exists. True only after a successful {@link #ensure()}. */
	public boolean ready() {
		return ready;
	}

	/** Why the batcher is unusable, or {@code null} when it is usable. */
	public String failureReason() {
		return failure;
	}

	/** A one-line summary for the console, whether it succeeded or not. */
	public String describe() {
		if (ready) {
			return "GL batcher ready (shader + VAO/VBO), viewport "
					+ GlScene.width() + "x" + GlScene.height();
		}
		return "GL batcher unavailable (" + failure + ")";
	}

	/** Releases GL objects. Safe to call more than once. */
	public synchronized void dispose() {
		destroyQuietly();
		ready = false;
	}

	private void destroyQuietly() {
		try {
			if (vao != 0) {
				GL30.glDeleteVertexArrays(vao);
			}
			if (positionVbo != 0) {
				GL15.glDeleteBuffers(positionVbo);
			}
			if (colourVbo != 0) {
				GL15.glDeleteBuffers(colourVbo);
			}
			if (program > 0) {
				GL20.glDeleteProgram(program);
			}
		} catch (Throwable ignored) {
			// Best effort: a failure here must not mask the original reason.
		}
		vao = 0;
		positionVbo = 0;
		colourVbo = 0;
		program = -1;
	}

	/**
	 * Wraps the live float storage without copying.
	 *
	 * <p>{@link GpuFloatBuffer#array()} is live storage and only the first
	 * {@code position()} entries are valid, so the buffer is limited to that window -
	 * uploading the whole capacity would send stale triangles from previous frames.
	 */
	private static java.nio.FloatBuffer toFloatBuffer(GpuFloatBuffer src) {
		java.nio.FloatBuffer fb = BufferUtils.createFloatBuffer(src.position());
		fb.put(src.array(), 0, src.position()).flip();
		return fb;
	}

	private static java.nio.IntBuffer toIntBuffer(GpuIntBuffer src) {
		java.nio.IntBuffer ib = BufferUtils.createIntBuffer(src.position());
		ib.put(src.array(), 0, src.position()).flip();
		return ib;
	}

	private static int buildProgram() {
		int vs = compile(GL20.GL_VERTEX_SHADER, VERTEX_SOURCE);
		int fs = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SOURCE);
		if (vs == 0 || fs == 0) {
			return 0;
		}
		int p = GL20.glCreateProgram();
		GL20.glAttachShader(p, vs);
		GL20.glAttachShader(p, fs);
		GL20.glLinkProgram(p);
		GL20.glDeleteShader(vs);
		GL20.glDeleteShader(fs);
		if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == 0) {
			System.out.println("GlBatcher: program link failed: " + GL20.glGetProgramInfoLog(p));
			GL20.glDeleteProgram(p);
			return 0;
		}
		return p;
	}

	private static int compile(int type, String source) {
		int s = GL20.glCreateShader(type);
		GL20.glShaderSource(s, source);
		GL20.glCompileShader(s);
		if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == 0) {
			System.out.println("GlBatcher: shader compile failed: " + GL20.glGetShaderInfoLog(s));
			GL20.glDeleteShader(s);
			return 0;
		}
		return s;
	}

	private static String describe(Throwable t) {
		String message = t.getMessage();
		return t.getClass().getName() + (message == null ? "" : ": " + message);
	}
}
