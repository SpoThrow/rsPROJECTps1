package ui;

import model.Model;
import model.Texture;

/**
 * The near-plane clipper (Phase 7.2b-2d): {@code Model.method485}, as a pure function.
 *
 * <p><b>Why this exists rather than letting the GPU clip.</b> A face with a vertex at or
 * behind the near plane is not projected at all - {@code method443} writes
 * {@link GlModelProjection#OFFSCREEN} into its X slot and leaves Y untouched - so there
 * is no triangle to hand the batcher. The software hands such a face to
 * {@code method485}, which rebuilds it in CAMERA space as a 3- or 4-gon.
 *
 * <p>⚠ <b>The obvious alternative - emit the uncut triangle and let the GL near plane cut
 * it - was rejected, and the reason is arithmetic rather than taste.</b> The batcher
 * receives x/y ALREADY DIVIDED by camera-space z, plus a {@code z} that is a linear
 * function of depth. Clipping in that space interpolates the projected coordinates
 * AFFINELY, whereas {@code method485} interpolates in camera space and then projects the
 * intersection exactly at depth 50. Interpolating {@code camX*512/camZ} along the edge is
 * not the same number as {@code camX(t)*512/50}, so the two would disagree slightly along
 * every clipped edge - a silent divergence in exactly the region this step exists to make
 * faithful, and one an oracle would then have to be written to tolerate. Reproducing the
 * software was the smaller and the more honest option.
 *
 * <p><b>What it does, exactly.</b> Walking the face's vertices in order (0, 1, 2), each
 * vertex at or in front of the near plane contributes its own projected point; each vertex
 * BEHIND the plane contributes the intersections of its edges with the plane, in
 * {@code method485}'s literal emission order - which matters, because the polygon's
 * winding and therefore whether it survives depend on it. The result is 0, 3 or 4 points,
 * never more.
 *
 * <p>⚠ <b>The cut is on CAMERA-SPACE Z against the constant 50</b>, which is a constant
 * for a whole model: {@code anIntArray1667} is {@code camZ - k2} and {@code k2} is the
 * model's scene depth, so {@code depth + sceneDepth} recovers {@code anIntArray1670}
 * without a third projected array. Taking the absolute Z from one place is what stops the
 * two drifting apart.
 *
 * <p>⚠ <b>The polygon MIXES the two kinds of point, and its DEPTHS therefore differ.</b> A
 * vertex in front of the plane keeps its own projected position and its own depth; an
 * intersection sits on the plane and has depth {@code 50 - sceneDepth} whatever the edge.
 * So a per-point depth is written out rather than one depth for the whole triangle - a
 * single value would flatten the front corners of every clipped face onto the plane and
 * lose the depth test between one clipped face and the next.
 *
 * <p>⚠ <b>The colour interpolation is the software's integer form, not a float one.</b> A
 * clipped point's colour is {@code cFrom + ((cTo - cFrom) * t >> 16)} where
 * {@code t = (50 - zFrom) * (65536 / (zTo - zFrom))}. A float version would disagree on
 * the low bits, which sounds negligible until the colour is a palette INDEX - where one
 * unit is a different entry and therefore a visibly different RGB.
 *
 * <p>⚠ <b>One case is refused rather than approximated.</b> The fraction divides through
 * {@link Texture#anIntArray1469}, a 2048-entry table, indexed unchecked. A clipped edge
 * spanning 2048 camera-space units therefore throws
 * {@code ArrayIndexOutOfBoundsException} in the software - and because {@code method443}
 * wraps the whole draw in a bare {@code try/catch}, that does not crash: it silently
 * ABORTS the model mid-draw, leaving the frame part rasterised. That is not worth
 * reproducing, so this returns {@link #UNUSABLE} and the caller counts the face as
 * unrepresentable rather than pretending. Reachable only for a very large model very close
 * to the camera.
 *
 * <p>⚠ <b>{@code Texture.aBoolean1462} is deliberately NOT reproduced.</b>
 * {@code method485} sets it to tell the software rasteriser whether a clipped point fell
 * off the side of the screen and must be clipped horizontally. A GL viewport does that in
 * hardware, with no flag to consult.
 *
 * <p>⚠ Not yet wired to anything: zero call sites in the client.
 * {@link GlFacePipeline} is its only production caller, and that class is itself unwired.
 */
public final class GlClipper {

	/**
	 * The face could not be clipped the way the software clips it, so nothing was
	 * written. Currently only the {@link Texture#anIntArray1469} bound above produces it.
	 */
	public static final int UNUSABLE = -1;

	/** The most points {@code method485} can emit: a quad, when two vertices are cut. */
	public static final int MAX_POINTS = 4;

	private GlClipper() {
	}

	/**
	 * Cuts {@code face} against the near plane exactly as {@code Model.method485} does.
	 *
	 * @param model      the model the face belongs to; its corner-colour arrays must be
	 *                   present, which {@code method485} also requires
	 * @param face       the face index
	 * @param screenX    projected screen X per vertex, {@code method443}'s array 1665
	 * @param screenY    projected screen Y per vertex, arrays 1666
	 * @param camX       camera-space X per vertex, arrays 1668
	 * @param camY       camera-space Y per vertex, arrays 1669
	 * @param depth      camera-space Z minus the scene depth per vertex, arrays 1667
	 * @param sceneDepth the model's scene depth, {@code method443}'s {@code k2} - the
	 *                   offset that turns {@code depth} back into an absolute camera Z
	 * @param centreX    screen centre X, {@code Texture.textureInt1}
	 * @param centreY    screen centre Y, {@code Texture.textureInt2}
	 * @param outX       receives the polygon's screen X, at least {@link #MAX_POINTS} long
	 * @param outY       receives the polygon's screen Y, at least {@link #MAX_POINTS} long
	 * @param outDepth   receives each point's depth, {@code camZ - sceneDepth} - ready for
	 *                   the same mapping the uncut path uses, at least {@link #MAX_POINTS}
	 *                   long
	 * @param outColour  receives the polygon's colour CODE (not RGB), at least
	 *                   {@link #MAX_POINTS} long
	 * @return the number of points written, {@code 0} to {@link #MAX_POINTS} - a count
	 *         below 3 means the face lies wholly behind the plane and the software draws
	 *         nothing - or {@link #UNUSABLE}
	 */
	public static int clip(Model model, int face,
			int[] screenX, int[] screenY, int[] camX, int[] camY, int[] depth, int sceneDepth,
			int centreX, int centreY,
			int[] outX, int[] outY, int[] outDepth, int[] outColour) {
		if (model == null || screenX == null || screenY == null || camX == null || camY == null
				|| depth == null || outX == null || outY == null || outDepth == null
				|| outColour == null) {
			return UNUSABLE;
		}
		if (face < 0 || face >= model.faceCount()) {
			return UNUSABLE;
		}
		if (outX.length < MAX_POINTS || outY.length < MAX_POINTS || outDepth.length < MAX_POINTS
				|| outColour.length < MAX_POINTS) {
			return UNUSABLE;
		}
		int[] faceA = model.faceVertexA();
		int[] faceB = model.faceVertexB();
		int[] faceC = model.faceVertexC();
		int[] colourA = model.faceCornerColoursA();
		int[] colourB = model.faceCornerColoursB();
		int[] colourC = model.faceCornerColoursC();
		int[] recip = Texture.anIntArray1469;
		if (faceA == null || faceB == null || faceC == null
				|| colourA == null || colourB == null || colourC == null || recip == null) {
			return UNUSABLE;
		}

		int v0 = faceA[face];
		int v1 = faceB[face];
		int v2 = faceC[face];
		int z0 = depth[v0] + sceneDepth;
		int z1 = depth[v1] + sceneDepth;
		int z2 = depth[v2] + sceneDepth;
		int c0 = colourA[face];
		int c1 = colourB[face];
		int c2 = colourC[face];
		int near = GlModelProjection.NEAR_PLANE;
		// Every intersection lies ON the plane, so they share one depth; the front vertices
		// keep their own. Both are camZ - sceneDepth, which is the space the caller maps.
		int planeDepth = near - sceneDepth;

		int l = 0;

		// --- vertex 0, in method485's literal emission order ---
		if (z0 >= near) {
			outX[l] = screenX[v0];
			outY[l] = screenY[v0];
			outDepth[l] = depth[v0];
			outColour[l++] = c0;
		} else {
			if (z2 >= near) {
				int t = ratio(z0, z2, recip);
				if (t == UNUSABLE) {
					return UNUSABLE;
				}
				outX[l] = clippedScreen(centreX, camX[v0], camX[v2], t);
				outY[l] = clippedScreen(centreY, camY[v0], camY[v2], t);
				outDepth[l] = planeDepth;
				outColour[l++] = lerp(c0, c2, t);
			}
			if (z1 >= near) {
				int t = ratio(z0, z1, recip);
				if (t == UNUSABLE) {
					return UNUSABLE;
				}
				outX[l] = clippedScreen(centreX, camX[v0], camX[v1], t);
				outY[l] = clippedScreen(centreY, camY[v0], camY[v1], t);
				outDepth[l] = planeDepth;
				outColour[l++] = lerp(c0, c1, t);
			}
		}

		// --- vertex 1 ---
		if (z1 >= near) {
			outX[l] = screenX[v1];
			outY[l] = screenY[v1];
			outDepth[l] = depth[v1];
			outColour[l++] = c1;
		} else {
			if (z0 >= near) {
				int t = ratio(z1, z0, recip);
				if (t == UNUSABLE) {
					return UNUSABLE;
				}
				outX[l] = clippedScreen(centreX, camX[v1], camX[v0], t);
				outY[l] = clippedScreen(centreY, camY[v1], camY[v0], t);
				outDepth[l] = planeDepth;
				outColour[l++] = lerp(c1, c0, t);
			}
			if (z2 >= near) {
				int t = ratio(z1, z2, recip);
				if (t == UNUSABLE) {
					return UNUSABLE;
				}
				outX[l] = clippedScreen(centreX, camX[v1], camX[v2], t);
				outY[l] = clippedScreen(centreY, camY[v1], camY[v2], t);
				outDepth[l] = planeDepth;
				outColour[l++] = lerp(c1, c2, t);
			}
		}

		// --- vertex 2 ---
		if (z2 >= near) {
			outX[l] = screenX[v2];
			outY[l] = screenY[v2];
			outDepth[l] = depth[v2];
			outColour[l++] = c2;
		} else {
			if (z1 >= near) {
				int t = ratio(z2, z1, recip);
				if (t == UNUSABLE) {
					return UNUSABLE;
				}
				outX[l] = clippedScreen(centreX, camX[v2], camX[v1], t);
				outY[l] = clippedScreen(centreY, camY[v2], camY[v1], t);
				outDepth[l] = planeDepth;
				outColour[l++] = lerp(c2, c1, t);
			}
			if (z0 >= near) {
				int t = ratio(z2, z0, recip);
				if (t == UNUSABLE) {
					return UNUSABLE;
				}
				outX[l] = clippedScreen(centreX, camX[v2], camX[v0], t);
				outY[l] = clippedScreen(centreY, camY[v2], camY[v0], t);
				outDepth[l] = planeDepth;
				outColour[l++] = lerp(c2, c0, t);
			}
		}
		return l;
	}

	/**
	 * {@code method485}'s fixed-point fraction along an edge,
	 * {@code (50 - fromZ) * (65536 / (toZ - fromZ))}.
	 *
	 * @return the fraction, or {@link #UNUSABLE} if the divisor leaves the reciprocal table
	 */
	private static int ratio(int fromZ, int toZ, int[] recip) {
		int d = toZ - fromZ;
		if (d <= 0 || d >= recip.length) {
			return UNUSABLE;
		}
		return (GlModelProjection.NEAR_PLANE - fromZ) * recip[d];
	}

	/**
	 * The screen position of a point held at the near plane: the camera-space coordinate is
	 * interpolated to the plane first, then projected exactly there - which is why this
	 * divides by the constant 50 and not by a depth that varies per point.
	 */
	private static int clippedScreen(int centre, int fromCam, int toCam, int t) {
		return centre + ((fromCam + ((toCam - fromCam) * t >> 16)) << 9)
				/ GlModelProjection.NEAR_PLANE;
	}

	/** {@code method485}'s colour interpolation, truncating exactly as the software does. */
	private static int lerp(int from, int to, int t) {
		return from + ((to - from) * t >> 16);
	}
}
