package ui;

import model.Model;

/**
 * The model-space to screen-space projection (Phase 7.2b-2a).
 *
 * <p><b>What this is and why it has to exist.</b> The GL arm cannot take the scene over
 * until it can place MODELS, and models do not arrive projected. The ground seam hands
 * a rasteriser screen pixels, but a model arrives from {@link SceneRasterizer} in MODEL
 * space with the camera as {@code camA..camD} sin/cos plus {@code dx/dy/dz}, and the
 * projection happens INSIDE {@code Model.method443}. The projected arrays
 * ({@code anIntArray1665/1666/1667}) are {@code private static} scratch shared by every
 * model and valid only mid-draw - 5.1's finding - so a GPU path can neither read nor
 * safely borrow them. It must reproduce the transform itself. This is that transform,
 * as a pure function.
 *
 * <p><b>Fidelity, line by line.</b> Every operation below is the same operation
 * {@code method443} performs, on the same values, in the same order, with the same
 * {@code >> 16} fixed point. The one deliberate change is SHAPE, not arithmetic: the
 * result is written into caller-owned arrays instead of the model's static scratch, so
 * it is reusable, testable, and cannot be invalidated by the next model drawn. No
 * rounding is "tidied" and no operation is reordered, because either would move pixels.
 *
 * <p><b>Two constants are part of the contract, not conveniences.</b> A vertex nearer
 * than {@link #NEAR_PLANE} is not projected, and {@code method443} marks it by writing
 * {@link #OFFSCREEN} into the X array and leaving Y UNTOUCHED - so a caller must consult
 * the depth to know whether Y is meaningful. Reproducing that exactly (including the
 * untouched Y) is what makes this comparable to the software path at all.
 *
 * <p><b>The verification is an ORACLE, not a fixture, and that is the point.</b> Rather
 * than hand-computing expected pixels - which would only prove this agrees with the
 * same reading of {@code method443} that produced it - the harness drives the REAL
 * {@code method443}, reads the REAL {@code anIntArray1665/1666/1667} it leaves behind,
 * and requires this class to reproduce them EXACTLY. A hand-derived fixture cannot
 * dispute an implementation; the software path can, which is 5.3's lesson applied here.
 *
 * <p>⚠ Not yet wired to anything: zero call sites, like 5.1's accessors and 5.2's
 * buffers when they landed. It is inert by construction until 7.2b-2b feeds it.
 */
public final class GlModelProjection {

	/**
	 * The marker {@code method443} writes into the projected-X array for a vertex at or
	 * behind the near plane. Named rather than inlined because a caller that mistakes it
	 * for a coordinate silently projects a vertex to a wild offscreen column.
	 */
	public static final int OFFSCREEN = -5000;

	/** The camera-space depth at which {@code method443} stops projecting a vertex. */
	public static final int NEAR_PLANE = 50;

	private GlModelProjection() {
	}

	/**
	 * Projects {@code model}'s local vertices exactly as {@code Model.method443} does.
	 *
	 * @param model       the model whose local vertices are read (5.1's accessors)
	 * @param orientation the model's rotation index; 0 means "no rotation applied"
	 * @param camA..camD  the current camera sin/cos pair, as
	 *                    {@code WorldController.anInt458..anInt461}
	 * @param dx,dy,dz    the model's position relative to the camera origin
	 * @param centreX     the screen centre X, {@code Texture.textureInt1}
	 * @param centreY     the screen centre Y, {@code Texture.textureInt2}
	 * @param outX        receives projected screen X, or {@link #OFFSCREEN}
	 * @param outY        receives projected screen Y - ONLY meaningful where {@code outX}
	 *                    is not {@link #OFFSCREEN}; {@code method443} leaves it untouched
	 *                    otherwise and so does this
	 * @param outDepth    receives {@code method443}'s depth, {@code i8 - k2}
	 * @param outCamX     receives the camera-space X, {@code method443}'s
	 *                    {@code anIntArray1668}. ⚠ Needed by the near-plane clipper and by
	 *                    nothing else - see the class doc on {@link GlClipper}.
	 * @param outCamY     receives the camera-space Y, {@code method443}'s
	 *                    {@code anIntArray1669}. Written for EVERY vertex, unconditionally
	 * @return the number of vertices sampled (the model's vertex count), or {@code 0} if
	 *         the model or arrays are unusable
	 */
	public static int project(Model model, int orientation, int camA, int camB, int camC, int camD,
			int dx, int dy, int dz, int centreX, int centreY,
			int[] outX, int[] outY, int[] outDepth, int[] outCamX, int[] outCamY) {
		if (model == null || outX == null || outY == null || outDepth == null
				|| outCamX == null || outCamY == null) {
			return 0;
		}
		int[] vx = model.vertexXs();
		int[] vy = model.vertexYs();
		int[] vz = model.vertexZs();
		if (vx == null || vy == null || vz == null) {
			return 0;
		}
		int count = model.vertexCount();
		if (count <= 0 || outX.length < count || outY.length < count || outDepth.length < count
				|| outCamX.length < count || outCamY.length < count) {
			return 0;
		}

		// method443's model-origin depth offset. NOTE the axes: k2 uses camA/camB while
		// j2 uses camC/camD - that asymmetry is in the original and swapping it produces
		// a projection that looks plausible and is wrong, so it is mirrored, not tidied.
		int k2 = modelDepth(camA, camB, camC, camD, dx, dy, dz);

		int orientationCos = 0;
		int orientationSin = 0;
		if (orientation != 0) {
			orientationCos = Model.modelIntArray1[orientation];
			orientationSin = Model.modelIntArray2[orientation];
		}

		for (int v = 0; v < count; v++) {
			int localX = vx[v];
			int localY = vy[v];
			int localZ = vz[v];

			if (orientation != 0) {
				int rotated = (localZ * orientationCos + localX * orientationSin) >> 16;
				localZ = (localZ * orientationSin - localX * orientationCos) >> 16;
				localX = rotated;
			}
			localX += dx;
			localY += dy;
			localZ += dz;

			// Yaw about the vertical axis, then pitch about the horizontal one.
			int rotated = (localZ * camC + localX * camD) >> 16;
			localZ = (localZ * camD - localX * camC) >> 16;
			localX = rotated;

			rotated = (localY * camB - localZ * camA) >> 16;
			localZ = (localY * camA + localZ * camB) >> 16;
			localY = rotated;

			outDepth[v] = localZ - k2;
			// ⚠ The camera-space pair is written for EVERY vertex, whereas method443 writes
			// it only when `flag || anInt1642 > 0` - i.e. only when some vertex is behind the
			// near plane, or the model is textured. That difference is deliberate and safe:
			// inside method443 those slots are read ONLY by method485 (the clipper, which is
			// reachable only when flag was true) and by the textured method378 path, so a
			// value written when neither applies is never observed. Writing them always
			// keeps this a pure function of its inputs, which is the whole point of the class.
			outCamX[v] = localX;
			outCamY[v] = localY;
			if (localZ >= NEAR_PLANE) {
				outX[v] = centreX + (localX << 9) / localZ;
				outY[v] = centreY + (localY << 9) / localZ;
			} else {
				// Y is deliberately NOT written: method443 cannot project it either, and
				// inventing a value here would make the arrays disagree with the oracle.
				outX[v] = OFFSCREEN;
			}
		}
		return count;
	}

	/**
	 * {@code method443}'s model-origin depth, {@code k2} - the value it assigns to
	 * {@code Fog.sceneDepth} for the duration of the draw.
	 *
	 * <p><b>Why this is exposed rather than inline.</b> {@code k2} is not an
	 * intermediate of the vertex transform: the software rasterisers read it as their
	 * fog distance, so anything reproducing their COLOURS needs it as well as anything
	 * reproducing their geometry. {@link #project} computes it through this method so
	 * there is exactly one copy of the camera-axis formula - {@code k2} uses
	 * {@code camA}/{@code camB} while {@code j2} uses {@code camC}/{@code camD}, and a
	 * second transcription is a second chance to swap them.
	 *
	 * @param camA..camD the current camera sin/cos pair
	 * @param dx,dy,dz   the model's position relative to the camera origin
	 * @return {@code k2}
	 */
	public static int modelDepth(int camA, int camB, int camC, int camD,
			int dx, int dy, int dz) {
		int j2 = (dz * camD - dx * camC) >> 16;
		return (dy * camA + j2 * camB) >> 16;
	}
}
