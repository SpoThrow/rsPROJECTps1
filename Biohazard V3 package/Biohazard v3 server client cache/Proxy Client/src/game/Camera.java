package game;

import model.Model;

import static game.client.*;

/**
 * Camera and view state, lifted out of client.java in Phase 3.2.2.
 *
 * The zoom methods are static because they operate on the static cameraZoom and are
 * reached from static contexts (setScreenMode, RSApplet's wheel handler); the two
 * position methods are instance methods, because they mutate the client's camera
 * fields. State stays on client - this class reaches it through the receiver it is
 * constructed with, the same pattern ClientSettings uses.
 */
public final class Camera {
	private final client c;

	public Camera(client c) {
		this.c = c;
	}

	private static final int CAMERA_ZOOM_MIN = 0;
	private static final int CAMERA_ZOOM_MAX = 1200;
	private static final int CAMERA_ZOOM_STEP_MIN = 20;
	private static final int CAMERA_ZOOM_STEP_MAX = 50;

	public static void adjustCameraZoom(int rotation) {
		if (instance == null || !instance.loggedIn || rotation == 0) {
			return;
		}
		int step = (int) Math.round(interpolate(cameraZoom, CAMERA_ZOOM_MIN, CAMERA_ZOOM_MAX,
				CAMERA_ZOOM_STEP_MIN, CAMERA_ZOOM_STEP_MAX) * zoomSensitivity / 100.0);
		if (step < 1) {
			step = 1;
		}
		cameraZoom += step * rotation;
		clampCameraZoom();
		instance.markSceneDirty();
	}

	public static void clampCameraZoom() {
		if (cameraZoom < CAMERA_ZOOM_MIN) {
			cameraZoom = CAMERA_ZOOM_MIN;
		}
		if (cameraZoom > CAMERA_ZOOM_MAX) {
			cameraZoom = CAMERA_ZOOM_MAX;
		}
	}

	private static double interpolate(double value, double minValue, double maxValue,
			double minResult, double maxResult) {
		if (value < minValue) {
			value = minValue;
		}
		if (value > maxValue) {
			value = maxValue;
		}
		if (maxValue <= minValue) {
			return minResult;
		}
		return minResult + (value - minValue) * (maxResult - minResult) / (maxValue - minValue);
	}

	public void calcCameraPos() {
		if (c.mouseWheelDown) {
			return;
		}
		int i = c.anInt1098 * 128 + 64;
		int j = c.anInt1099 * 128 + 64;
		int k = c.method42(c.plane, j, i) - c.anInt1100;
		if (c.xCameraPos < i) {
			c.xCameraPos += c.anInt1101 + ((i - c.xCameraPos) * c.anInt1102) / 1000;
			if (c.xCameraPos > i)
				c.xCameraPos = i;
		}
		if (c.xCameraPos > i) {
			c.xCameraPos -= c.anInt1101 + ((c.xCameraPos - i) * c.anInt1102) / 1000;
			if (c.xCameraPos < i)
				c.xCameraPos = i;
		}
		if (c.zCameraPos < k) {
			c.zCameraPos += c.anInt1101 + ((k - c.zCameraPos) * c.anInt1102) / 1000;
			if (c.zCameraPos > k)
				c.zCameraPos = k;
		}
		if (c.zCameraPos > k) {
			c.zCameraPos -= c.anInt1101 + ((c.zCameraPos - k) * c.anInt1102) / 1000;
			if (c.zCameraPos < k)
				c.zCameraPos = k;
		}
		if (c.yCameraPos < j) {
			c.yCameraPos += c.anInt1101 + ((j - c.yCameraPos) * c.anInt1102) / 1000;
			if (c.yCameraPos > j)
				c.yCameraPos = j;
		}
		if (c.yCameraPos > j) {
			c.yCameraPos -= c.anInt1101 + ((c.yCameraPos - j) * c.anInt1102) / 1000;
			if (c.yCameraPos < j)
				c.yCameraPos = j;
		}
		i = c.anInt995 * 128 + 64;
		j = c.anInt996 * 128 + 64;
		k = c.method42(c.plane, j, i) - c.anInt997;
		int l = i - c.xCameraPos;
		int i1 = k - c.zCameraPos;
		int j1 = j - c.yCameraPos;
		int k1 = (int) Math.sqrt(l * l + j1 * j1);
		int l1 = (int) (Math.atan2(i1, k1) * 325.94900000000001D) & 0x7ff;
		int i2 = (int) (Math.atan2(l, j1) * -325.94900000000001D) & 0x7ff;
		if (l1 < 128)
			l1 = 128;
		if (l1 > 383)
			l1 = 383;
		if (c.yCameraCurve < l1) {
			c.yCameraCurve += c.anInt998 + ((l1 - c.yCameraCurve) * c.anInt999) / 1000;
			if (c.yCameraCurve > l1)
				c.yCameraCurve = l1;
		}
		if (c.yCameraCurve > l1) {
			c.yCameraCurve -= c.anInt998 + ((c.yCameraCurve - l1) * c.anInt999) / 1000;
			if (c.yCameraCurve < l1)
				c.yCameraCurve = l1;
		}
		int j2 = i2 - c.xCameraCurve;
		if (j2 > 1024)
			j2 -= 2048;
		if (j2 < -1024)
			j2 += 2048;
		if (j2 > 0) {
			c.xCameraCurve += c.anInt998 + (j2 * c.anInt999) / 1000;
			c.xCameraCurve &= 0x7ff;
		}
		if (j2 < 0) {
			c.xCameraCurve -= c.anInt998 + (-j2 * c.anInt999) / 1000;
			c.xCameraCurve &= 0x7ff;
		}
		int k2 = i2 - c.xCameraCurve;
		if (k2 > 1024)
			k2 -= 2048;
		if (k2 < -1024)
			k2 += 2048;
		if (k2 < 0 && j2 > 0 || k2 > 0 && j2 < 0)
			c.xCameraCurve = i2;
	}

	public void setCameraPos(int j, int k, int l, int i1, int j1, int k1) {
		int l1 = 2048 - k & 0x7ff;
		int i2 = 2048 - j1 & 0x7ff;
		int j2 = 0;
		int k2 = 0;
		int l2 = j;
		if (l1 != 0) {
			int i3 = Model.modelIntArray1[l1];
			int k3 = Model.modelIntArray2[l1];
			int i4 = k2 * k3 - l2 * i3 >> 16;
			l2 = k2 * i3 + l2 * k3 >> 16;
			k2 = i4;
		}
		if (i2 != 0) {
			/*
			 * xxx if(cameratoggle){ if(zoom == 0) zoom = k2; if(lftrit == 0)
			 * lftrit = j2; if(fwdbwd == 0) fwdbwd = l2; k2 = zoom; j2 = lftrit;
			 * l2 = fwdbwd; }
			 */
			int j3 = Model.modelIntArray1[i2];
			int l3 = Model.modelIntArray2[i2];
			int j4 = l2 * j3 + j2 * l3 >> 16;
			l2 = l2 * l3 - j2 * j3 >> 16;
			j2 = j4;
		}
		c.xCameraPos = l - j2;
		c.zCameraPos = i1 - k2;
		c.yCameraPos = k1 - l2;
		c.yCameraCurve = k;
		c.xCameraCurve = j1;
	}
}
