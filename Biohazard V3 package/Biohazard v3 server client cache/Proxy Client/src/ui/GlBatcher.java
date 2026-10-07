package ui;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import model.Texture;
import scene.Fog;

/**
 * The GPU triangle sink (Phase 7.2b-1, extended with textures in 7.2b-2f).
 *
 * <p><b>What this is.</b> The lowest layer of the GL scene path: it takes triangles
 * in <i>screen-pixel space</i> with either a packed ARGB colour or a texture id plus
 * per-corner shade code, accumulates them into growable buffers, uploads them to a
 * single VBO, draws them into {@link GlScene}'s offscreen framebuffer with a depth
 * test, and reads the result back into an {@code int[]} in the client's own pixel
 * format.
 *
 * <p><b>Both geometry kinds go down ONE program and ONE draw call.</b> An untextured
 * vertex carries {@code layer = -1} and a textured one carries a texture id, so the
 * fragment shader picks the branch per fragment rather than the renderer picking a
 * program per face. That is not only fewer state changes: it keeps the two paths from
 * drifting, because they share the same vertex format, the same depth handling and the
 * same readback.
 *
 * <p><b>The textures are a {@code GL_TEXTURE_2D_ARRAY} of the software rasteriser's own
 * texels</b>, uploaded once from {@link GlTextures}/{@link Texture} and sampled with
 * {@code texelFetch}. The format is integer ({@code RGBA8UI}) and the darkening is done
 * on the packed integer in GLSL, because {@code Texture.method371}'s arithmetic borrows
 * across colour channels - a transcription in float RGB would be a different, subtly
 * wrong colour. See the fragment-shader source for the details that matter.
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
public final class GlBatcher implements SceneBatch {

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

	/**
	 * Texture-mapping attribute: the three RAMP NUMERATORS {@code (uNum, vNum, wNum)} per
	 * vertex, three floats - <b>not</b> camera-space coordinates.
	 *
	 * <p>⚠️⚠️ <b>Corrected in Phase 7.2b-2l, and the correction is worth reading because
	 * the old text argued for the wrong thing convincingly.</b> This attribute used to
	 * carry camera-space {@code (u, v, w)}. The reasoning was that the software's ramps
	 * are affine in screen space, so a {@code noperspective} varying of a triple divided
	 * per fragment is the faithful arrangement. <b>The premise was true and the conclusion
	 * did not follow:</b> it is the RAMPS that are affine, and the camera-space triple is
	 * what the ramps are <i>built from</i>, not the ramps. Interpolating the inputs and
	 * dividing is a different function - measured against the real rasteriser it matches
	 * at 12 of 10962 pixels, against 92.5% for the numerators.
	 *
	 * <p>⚠️ <b>So the DIVIDE is still left to the fragment shader, and that part was
	 * right.</b> {@code Texture.method379} divides per PIXEL, and {@code uNum/vNum/wNum}
	 * are affine in screen space, so a {@code noperspective} varying of those three,
	 * divided in the fragment shader, reproduces {@code uNum/(wNum &gt;&gt; 12)} as well as
	 * anything can (see {@link TextureRamps} - the plain ratio is measured equal to the
	 * shifted form at 99.9% of pixels). Using GL's default perspective correction would
	 * interpolate the ratios instead and disagree along every textured surface - the same
	 * class of error {@link GlClipper} refuses to make.
	 */
	private static final int UVW_COMPONENTS = 3;

	/**
	 * Texture control attribute: {@code (shade, layer)}, two floats.
	 *
	 * <p>{@code shade} is the model's RAW per-corner shade code ({@code 127 - light} -
	 * not a colour), and {@code layer} is the texture id, or <b>-1 for an untextured
	 * vertex</b>. Packing them into one attribute keeps the untextured path on exactly
	 * one code path: an untextured triangle simply carries {@code layer = -1} and the
	 * fragment shader takes the colour branch, so there is one program and one draw call
	 * rather than two of each.
	 *
	 * <p>⚠️ <b>{@code layer} is always an EXACT integer in its float, and the shader
	 * relies on that.</b> It converts with a plain truncation, which is exact for a
	 * texture id and - the case that caught a real bug - maps {@code -1.0} to {@code -1}.
	 * A "safer looking" {@code int(y + 0.5)} rounding would map {@code -1.0} to {@code 0},
	 * i.e. every untextured triangle would take the sampler branch and sample layer 0,
	 * which is empty - the whole flat scene would vanish while the textured geometry kept
	 * drawing, which is exactly what the probe observed before this was fixed.
	 */
	private static final int TEXTURE_CONTROL_COMPONENTS = 2;

	/**
	 * Layer value marking an UNTEXTURED vertex. Negative so it cannot collide with a real
	 * texture id, and tested by the fragment shader to choose between the colour path and
	 * the sampler - which keeps both on one program and one draw call.
	 */
	private static final float UNTEXTURED = -1f;

	private static final String VERTEX_SOURCE = "#version 330 core\n"
			+ "layout(location = 0) in vec3 aPos;\n"
			+ "layout(location = 1) in vec4 aCol;\n"
			+ "layout(location = 2) in vec3 aUvW;\n"
			+ "layout(location = 3) in vec2 aTex;\n"
			+ "uniform vec2 uViewport;\n"
			+ "out vec4 vCol;\n"
			+ "noperspective out vec3 vUvW;\n"
			+ "noperspective out float vShade;\n"
			+ "flat out int vLayer;\n"
			+ "void main() {\n"
			+ "    float x = (aPos.x / uViewport.x) * 2.0 - 1.0;\n"
			+ "    float y = 1.0 - (aPos.y / uViewport.y) * 2.0;\n"
			+ "    float z = aPos.z * 2.0 - 1.0;\n"
			+ "    gl_Position = vec4(x, y, z, 1.0);\n"
			+ "    vCol = aCol.bgra;\n"
			+ "    vUvW = aUvW;\n"
			+ "    vShade = aTex.x;\n"
			+ "    vLayer = int(aTex.y);\n"
			+ "}\n";

	/**
	 * ⚠️ <b>The shade arithmetic here is a transcription of {@code Texture.method371}
	 * plus {@code method379}, and it is done on the PACKED integer deliberately.</b>
	 * The darkening is written {@code k - (k >>> n) & 0xf8f8ff}, where {@code k} is the
	 * already-masked value - so the subtraction BORROWS ACROSS CHANNELS and the mask
	 * repairs it. Darkening each channel in isolation is the obvious thing to write and
	 * gives a different, subtly wrong colour.
	 *
	 * <p>The texture array is integer ({@code usampler2DArray}), so the texel arrives
	 * as exact bytes with no filtering or colour-space conversion in the way - which is
	 * what makes a bit-exact transcription possible at all. Integer textures cannot be
	 * linearly filtered, so {@code NEAREST} is forced by the format as well as chosen.
	 *
	 * <p>{@code shade} is TRUNCATED rather than rounded, because {@code method379} reads
	 * it with a right shift ({@code j1 >> 23}); a non-negative float-to-int conversion is
	 * a floor, which is the same thing.
	 *
	 * <p>⚠️ <b>{@code texelFetch} here, NOT {@code texture()} - and this is not style.</b>
	 * {@code texture()} takes NORMALISED coordinates even for an integer sampler, so
	 * passing a texel index through it normalises it a second time and clamps the result
	 * to the last texel, i.e. every textured fragment silently samples the bottom-right
	 * corner. {@code texelFetch} is the unnormalised one. The probe catches this because
	 * the mapping texture encodes each texel's own (col,row), so "which texel was read" is
	 * directly readable from the painted pixel rather than inferred.
	 */
	private static final String FRAGMENT_SOURCE = "#version 330 core\n"
			+ "in vec4 vCol;\n"
			+ "noperspective in vec3 vUvW;\n"
			+ "noperspective in float vShade;\n"
			+ "flat in int vLayer;\n"
			+ "uniform usampler2DArray uAtlas;\n"
			+ "uniform float uLayerSize;\n"
			+ "out vec4 fragColor;\n"
			+ "void main() {\n"
			+ "    if (vLayer < 0) {\n"
			+ "        fragColor = vCol;\n"
			+ "        return;\n"
			+ "    }\n"
			+ "    int size = int(uLayerSize + 0.5);\n"
			+ "    if (vUvW.z == 0.0) {\n"
			+ "        discard;\n"
			+ "    }\n"
			+ "    vec2 uv = vUvW.xy / vUvW.z;\n"
			+ "    ivec2 texel = ivec2(floor(uv * float(size)));\n"
			+ "    texel = clamp(texel, ivec2(0), ivec2(size - 1));\n"
			+ "    uvec4 t = texelFetch(uAtlas, ivec3(texel, vLayer), 0);\n"
			+ "    int shade = int(vShade);\n"
			+ "    uint rgb = (t.r << 16u) | (t.g << 8u) | t.b;\n"
			+ "    int block = (shade >> 4) & 3;\n"
			+ "    if (block == 1) {\n"
			+ "        rgb = (rgb - (rgb >> 3u)) & 0xF8F8FFu;\n"
			+ "    } else if (block == 2) {\n"
			+ "        rgb = (rgb - (rgb >> 2u)) & 0xF8F8FFu;\n"
			+ "    } else if (block == 3) {\n"
			+ "        rgb = (rgb - (rgb >> 2u) - (rgb >> 3u)) & 0xF8F8FFu;\n"
			+ "    }\n"
			+ "    rgb = rgb >> uint(shade >> 6);\n"
			+ "    if (rgb == 0u) {\n"
			+ "        discard;\n"
			+ "    }\n"
			+ "    fragColor = vec4(float((rgb >> 16u) & 0xFFu) / 255.0,\n"
			+ "                     float((rgb >> 8u) & 0xFFu) / 255.0,\n"
			+ "                     float(rgb & 0xFFu) / 255.0,\n"
			+ "                     1.0);\n"
			+ "}\n";

	/** Grown once and reused: the per-frame allocation this path exists to avoid. */
	private final GpuFloatBuffer positions = new GpuFloatBuffer();
	private final GpuIntBuffer colours = new GpuIntBuffer();
	private final GpuFloatBuffer uvws = new GpuFloatBuffer();
	private final GpuFloatBuffer textureControls = new GpuFloatBuffer();

	/** Allocated on first readback and reused; never shrinks. */
	private ByteBuffer readback;

	private boolean attempted;
	private boolean ready;
	private String failure;

	private int program = -1;
	private int vao;
	private int positionVbo;
	private int colourVbo;
	private int uvWvbo;
	private int textureControlVbo;
	private int uViewport = -1;
	private int uAtlas = -1;
	private int uLayerSize = -1;

	/** The texture array, or 0 when it could not be built. */
	private int atlas;
	private boolean atlasReady;
	private int layerSize;
	private int layerCount;
	private String atlasFailure;

	private int frameWidth;
	private int frameHeight;

	/** Textured triangles accepted since {@link #beginFrame}. */
	private int texturedTriangles;

	/**
	 * Brings up the frame target, the shader program and the VAO/VBOs if they have not been
	 * tried yet, and RESTATES the frame target when the drawing area has changed.
	 *
	 * <p>Idempotent apart from the frame target's size: the attempt happens once, and a later
	 * call at a DIFFERENT drawing area restates the frame target rather than rebuilding the
	 * device objects - the viewport is a per-frame uniform, so the program, the VAO and the
	 * VBOs are size-independent. See {@link SceneBatch#ensure(int, int)}.
	 *
	 * @param width  the drawing area's width in pixels, {@code > 0}
	 * @param height the drawing area's height in pixels, {@code > 0}
	 * @return {@code true} if triangles can be drawn at that size
	 */
	@Override
	public synchronized boolean ensure(int width, int height) {
		if (attempted) {
			if (!ready) {
				return false;
			}
			return GlScene.ensure(width, height);
		}
		attempted = true;
		if (!GlScene.ensure(width, height)) {
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
		if (ready) {
			// Separate from the device objects on purpose: see createAtlas's note on why
			// a texture failure must cost the texture path rather than the whole batcher.
			createAtlas();
		}
		return ready;
	}

	private void createDeviceObjects() {
		program = buildProgram();
		if (program == 0) {
			throw new IllegalStateException("shader program failed to build");
		}
		uViewport = GL20.glGetUniformLocation(program, "uViewport");
		uAtlas = GL20.glGetUniformLocation(program, "uAtlas");
		uLayerSize = GL20.glGetUniformLocation(program, "uLayerSize");

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

		uvWvbo = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvWvbo);
		GL20.glEnableVertexAttribArray(2);
		GL20.glVertexAttribPointer(2, UVW_COMPONENTS, GL11.GL_FLOAT, false,
				UVW_COMPONENTS * Float.BYTES, 0L);

		textureControlVbo = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, textureControlVbo);
		GL20.glEnableVertexAttribArray(3);
		GL20.glVertexAttribPointer(3, TEXTURE_CONTROL_COMPONENTS, GL11.GL_FLOAT, false,
				TEXTURE_CONTROL_COMPONENTS * Float.BYTES, 0L);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
	}

	/**
	 * Builds the texture array: one layer per texture id, each {@code layerSize} square.
	 *
	 * <p><b>Why one layer per id rather than a packed atlas.</b> Texture ids are already
	 * dense in {@code 0..50}, so a layer index IS the id and no UV remapping, no padding
	 * and no bleed between neighbours is needed - and a {@code GL_TEXTURE_2D_ARRAY}
	 * requires every layer to be the same size, which holds here because the software
	 * resamples them all to the same square.
	 *
	 * <p><b>The texels are the software's own.</b> {@link Texture#texturePixels} returns
	 * the block {@code method371} builds and {@code method379} samples, so nothing is
	 * re-decoded or re-resampled on this side - this method only reorders bytes.
	 *
	 * <p>⚠️ <b>Failure here is contained rather than fatal.</b> It runs in its own
	 * try/catch, so a driver that dislikes integer 3D textures costs the TEXTURE path
	 * only: {@link #supportsTextures()} then answers {@code false}, the pipeline counts
	 * textured faces as {@code NEEDS_TEXTURE}, and the flat path keeps drawing. The
	 * alternative - failing the whole batcher - would trade a degraded scene for no
	 * scene.
	 */
	private void createAtlas() {
		try {
			layerSize = GlTextures.layerSize();
			layerCount = GlTextures.TEXTURE_COUNT;
			atlas = GL11.glGenTextures();
			GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, atlas);
			// Allocate the storage; the layers are filled one at a time below.
			GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL30.GL_RGBA8UI, layerSize,
					layerSize, layerCount, 0, GL30.GL_RGBA_INTEGER, GL11.GL_UNSIGNED_BYTE,
					(ByteBuffer) null);
			GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER,
					GL11.GL_NEAREST);
			GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER,
					GL11.GL_NEAREST);
			GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S,
					GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T,
					GL12.GL_CLAMP_TO_EDGE);

			int texels = layerSize * layerSize;
			ByteBuffer scratch = BufferUtils.createByteBuffer(texels * 4);
			int uploaded = 0;
			for (int id = 0; id < layerCount; id++) {
				int[] pixels = Texture.texturePixels(id);
				scratch.clear();
				if (pixels != null) {
					for (int i = 0; i < texels; i++) {
						int p = pixels[i];
						scratch.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p)
								.put((byte) 0xff);
					}
					uploaded++;
				}
				scratch.flip();
				// A missing texture leaves an all-zero layer, whose texels the shade rule
				// reads as transparent - so it can never be mistaken for real content.
				GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, id, layerSize,
						layerSize, 1, GL30.GL_RGBA_INTEGER, GL11.GL_UNSIGNED_BYTE, scratch);
			}
			GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);

			atlasReady = uploaded > 0;
			if (!atlasReady) {
				atlasFailure = "no textures loaded yet";
			}
		} catch (Throwable t) {
			atlasReady = false;
			atlasFailure = describe(t);
			if (atlas != 0) {
				try {
					GL11.glDeleteTextures(atlas);
				} catch (Throwable ignored) {
					// Best effort; the failure above is the one worth reporting.
				}
				atlas = 0;
			}
		}
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
		// ⚠ NOT ensure(): the caller has already stated the size it is about to draw, which
		// is what SceneBatch#ensure(int, int) is for, and this method does not know it.
		if (!ready) {
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
		uvws.clear();
		textureControls.clear();
		texturedTriangles = 0;
		untexturedTriangles = 0;
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
		// Keep every attribute buffer vertex-aligned; the texture branch below fills
		// these with real values and the shader takes the colour path when layer is -1.
		uvws.put(0f, 0f, 1f).put(0f, 0f, 1f).put(0f, 0f, 1f);
		textureControls.put(0f, UNTEXTURED).put(0f, UNTEXTURED).put(0f, UNTEXTURED);
		noteTriangle(false);
		return true;
	}

	/**
	 * Whether this sink can draw a textured triangle - i.e. whether the texture array
	 * exists AND there is a shader to sample it.
	 *
	 * <p>⚠ <b>This used to be a hard {@code false}, and the reason it changed is that
	 * a false here is not a neutral answer:</b> a textured face this sink cannot draw is
	 * counted as {@link GlFacePipeline#NEEDS_TEXTURE} rather than submitted and silently
	 * dropped, which is the difference between {@link GlFacePipeline#allRepresentable()}
	 * being a fact and being a hope.
	 *
	 * <p>⚠ <b>It is still allowed to be {@code false}, and will be whenever the atlas
	 * could not be built.</b> The atlas is created after the device objects and in its
	 * own guard, so a driver that refuses integer 3D textures loses the texture path
	 * while the flat path keeps drawing - and this method reports that honestly rather
	 * than optimistically.
	 *
	 * @return {@code true} only if {@link #textured} can actually draw something
	 */
	@Override
	public boolean supportsTextures() {
		return ready && atlasReady;
	}

	/**
	 * Queues one textured triangle.
	 *
	 * <p>⚠ <b>Declines rather than approximating in three cases, and each is a real
	 * state rather than a defensive formality:</b>
	 * <ol>
	 *   <li><b>No atlas</b> - see {@link #supportsTextures()}.</li>
	 *   <li><b>The id is not loaded.</b> {@code Texture.unpack} swallows per-texture
	 *       failures, so an id in {@code 0..50} can legitimately be empty - and an
	 *       unsampled layer is all-zero, which would draw transparent. Note this is NOT
	 *       checked against the model's own texture count, which would be the wrong
	 *       bound: the id is a GLOBAL cache index.</li>
	 *   <li><b>The denominator is zero at a vertex.</b> The shader divides by {@code wNum}, so
	 *       an exact zero there is a division by zero, and a triangle with a vertex ON the
	 *       crossing is degenerate rather than merely steep. ⚠ <b>This used to reject a
	 *       triangle whose three {@code wNum} values did not share a sign, and that was
	 *       corrected in 7.4f: the SOFTWARE does not reject it.</b> {@code method379} guards
	 *       only the exact zero ({@code if (i5 != 0)} - it SKIPS those pixels) and CLAMPS the
	 *       rest by clamping, so a face that crosses zero is drawn with a thin clamped band.
	 *       The fragment shader now mirrors that (it discards exactly where {@code vUvW.z}
	 *       is zero), because rejecting the whole face withheld an entire frame on ONE face
	 *       of 122. ⚠ Note {@code w} as a ramp NUMERATOR is signed - see 7.2b-2l - so a
	 *       uniformly-negative triangle is valid and divides correctly.</li>
	 * </ol>
	 *
	 * <p>The shade is passed to the GPU RAW, exactly as the model produced it. Decoding
	 * it here would be a second copy of the mechanism, and the whole point of the
	 * fragment shader's transcription is that there is one.
	 *
	 * @return {@code true} if the triangle was queued
	 */
	@Override
	public boolean textured(float x0, float y0, float z0, float u0, float v0, float w0, int shade0,
			float x1, float y1, float z1, float u1, float v1, float w1, int shade1,
			float x2, float y2, float z2, float u2, float v2, float w2, int shade2,
			int textureId) {
		if (!ready || !atlasReady) {
			return false;
		}
		if (textureId < 0 || textureId >= layerCount || !GlTextures.available(textureId)) {
			return false;
		}
		if (denominatorsAreDegenerate(w0, w1, w2)) {
			return false;
		}
		// The colour slot is unused for a textured face - its slot holds a SHADE, not a
		// colour - but it must still be written so the buffers stay vertex-aligned.
		colours.putTriangle(0xffffffff, 0xffffffff, 0xffffffff);
		uvws.put(u0, v0, w0).put(u1, v1, w1).put(u2, v2, w2);
		float layer = textureId;
		textureControls.put(shade0, layer).put(shade1, layer).put(shade2, layer);
		noteTriangle(true);
		return true;
	}

	/** Number of triangles queued since {@link #beginFrame}. */
	public int triangleCount() {
		return positions.position() / (POSITION_COMPONENTS * 3);
	}

	/**
	 * ⚠⚠ PHASE 7.4f, EXTRACTED SO IT CAN BE PINNED RATHER THAN MERELY DESCRIBED: is this
	 * triangle's interpolated denominator degenerate at a VERTEX?
	 *
	 * <p><b>Only the exact zero, and that is the whole content of 7.4f.</b> This used to reject
	 * a triangle whose three {@code wNum} values did not share a sign, on the reasoning that the
	 * interpolated denominator would cross zero somewhere inside and blow up between two
	 * vertices that each looked usable. ⚠ <b>The software does not do that.</b>
	 * {@code method379} walks its own spans with an integer denominator and guards ONLY the
	 * exact zero ({@code if (i5 != 0)} - it SKIPS those pixels) and CLAMPS the rest, so a face
	 * that crosses zero is DRAWN, with a thin clamped band where it crosses. The fragment shader
	 * now mirrors that ({@code if (vUvW.z == 0.0) discard;}), and rejecting the whole face -
	 * which withheld an entire frame on ONE face of 122 - was a far bigger fidelity loss than
	 * the band it avoided.
	 *
	 * <p>⚠ Note a {@code w} here is a ramp NUMERATOR and is signed (7.2b-2l), so a
	 * uniformly-negative triangle is valid and divides correctly; there is no sign test.
	 *
	 * <p>⚠ Public rather than private <b>only so the harness can call it</b>: a rule this
	 * specific - the difference between "reject a sign change" and "tolerate it" - is exactly
	 * the kind of thing a later tidy-up would undo while every other check stayed green.
	 */
	public static boolean denominatorsAreDegenerate(float w0, float w1, float w2) {
		return w0 == 0f || w1 == 0f || w2 == 0f;
	}

	/** Number of TEXTURED triangles accepted since {@link #beginFrame}. */
	public int texturedCount() {
		return texturedTriangles;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>⚠ A triangle count rather than a buffer position, because the four attribute buffers
	 * advance by DIFFERENT amounts per triangle (9 floats, 3 ints, 9 floats, 6 floats) and a
	 * caller must not have to know that.
	 */
	@Override
	public int mark() {
		return texturedTriangles + untexturedTriangles;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>⚠⚠ <b>The textured count is recomputed from a per-triangle record rather than
	 * decremented by the number discarded.</b> The window being dropped is a MIX - a model is
	 * exactly where textured and untextured faces interleave - so "how many were textured"
	 * cannot be derived from its length alone, and guessing would drift
	 * {@link #texturedCount()} away from the truth permanently (it is a running counter, never
	 * recomputed).
	 */
	@Override
	public void rewind(int token) {
		int now = texturedTriangles + untexturedTriangles;
		if (token < 0 || token > now) {
			throw new IllegalArgumentException("rewind(" + token + ") outside 0.." + now);
		}
		int droppedTextured = 0;
		for (int i = token; i < now; i++) {
			if (texturedFlag[i] != 0) {
				droppedTextured++;
			}
		}
		positions.truncate(token * POSITION_COMPONENTS * 3);
		colours.truncate(token * 3);
		uvws.truncate(token * UVW_COMPONENTS * 3);
		textureControls.truncate(token * TEXTURE_CONTROL_COMPONENTS * 3);
		texturedTriangles -= droppedTextured;
		untexturedTriangles = token - texturedTriangles;
	}

	/** Per-triangle record of whether it was textured, so {@link #rewind} can recount. */
	private int[] texturedFlag = new int[4096];

	/** Triangles queued since {@link #beginFrame} that were NOT textured. */
	private int untexturedTriangles;

	/**
	 * Records one submitted triangle's kind. ⚠ Called by BOTH submit paths, so the ordinal
	 * {@link #mark} returns and the flag array stay in step by construction.
	 */
	private void noteTriangle(boolean textured) {
		int ordinal = texturedTriangles + untexturedTriangles;
		if (ordinal >= texturedFlag.length) {
			texturedFlag = java.util.Arrays.copyOf(texturedFlag,
					Math.max(ordinal + 1, texturedFlag.length << 1));
		}
		texturedFlag[ordinal] = textured ? 1 : 0;
		if (textured) {
			texturedTriangles++;
		} else {
			untexturedTriangles++;
		}
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

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvWvbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvws.capacity() * Float.BYTES,
				GL15.GL_STREAM_DRAW);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, toFloatBuffer(uvws));

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, textureControlVbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, textureControls.capacity() * Float.BYTES,
				GL15.GL_STREAM_DRAW);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, toFloatBuffer(textureControls));

		GL20.glUseProgram(program);
		GL20.glUniform2f(uViewport, frameWidth, frameHeight);
		if (atlasReady) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, atlas);
			GL20.glUniform1i(uAtlas, 0);
			GL20.glUniform1f(uLayerSize, layerSize);
		}
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, positions.position() / POSITION_COMPONENTS);
		if (atlasReady) {
			GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
		}

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

	/**
	 * The frame width this batcher would render at, from {@link GlScene}.
	 *
	 * <p>⚠ Answerable BEFORE {@link #beginFrame}, deliberately: the canvas is fixed at
	 * context-creation time, but the software drawing area is not, so a caller has to be
	 * able to compare the two before it commits the frame. See
	 * {@link GlSceneRenderer}'s size gate.
	 */
	@Override
	public int viewportWidth() {
		return GlScene.width();
	}

	/** The frame height this batcher would render at; see {@link #viewportWidth()}. */
	@Override
	public int viewportHeight() {
		return GlScene.height();
	}

	/** The scene background, cleared to by a frame whose caller passes this. See the seam. */
	@Override
	public int sceneBackground() {
		return Fog.sceneBackgroundRgb();
	}

	/** Why the batcher is unusable, or {@code null} when it is usable. */
	public String failureReason() {
		return failure;
	}

	/** A one-line summary for the console, whether it succeeded or not. */
	public String describe() {
		if (ready) {
			String texture = atlasReady
					? ("atlas " + layerCount + " layers of " + layerSize + "x" + layerSize
							+ ", NEAREST, integer")
					: ("no textured path (" + atlasFailure + ")");
			return "GL batcher ready (shader + VAO/VBO), viewport "
					+ GlScene.width() + "x" + GlScene.height() + ", " + texture;
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
			if (uvWvbo != 0) {
				GL15.glDeleteBuffers(uvWvbo);
			}
			if (textureControlVbo != 0) {
				GL15.glDeleteBuffers(textureControlVbo);
			}
			if (atlas != 0) {
				GL11.glDeleteTextures(atlas);
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
		uvWvbo = 0;
		textureControlVbo = 0;
		atlas = 0;
		atlasReady = false;
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
