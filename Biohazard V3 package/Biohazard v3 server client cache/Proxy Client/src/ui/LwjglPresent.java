package ui;

import java.awt.Component;

/**
 * LWJGL {@code Display.setParent} is not used. Parenting a native GL window
 * onto the AWT applet deadlocks Windows when the frame loses or gains focus
 * (both AWT and LWJGL pump the same HWND). OpenGL present is Java2D.
 */
public final class LwjglPresent {

	static boolean isSupported() {
		return false;
	}

	public static boolean isActive() {
		return false;
	}

	static void prepareNatives() {
	}

	static boolean present(int[] pixels, int width, int height, int destX, int destY, Component gameHost) {
		return false;
	}
}
