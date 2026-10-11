import java.io.File;
import java.io.RandomAccessFile;

import cache.Decompressor;
import cache.StreamLoader;
import ui.MakeX;
import ui.RSInterface;
import ui.TextDrawingArea;

/**
 * TEMPORARY diagnostic: what does a click on this chatbox actually offer the client?
 *
 * <p>Replicates {@code client.buildMenu}'s chatbox branch and {@code buildInterfaceMenu}'s
 * two clauses (containers recurse, {@code atActionType == 1} and inside is offered) over the
 * container {@code MakeX} builds, and prints every widget a click would offer. A widget that
 * is offered but is not a product button is a click the server will ignore.
 *
 * <p>Also applies the gate {@code buildMenu} puts in front of the whole thing
 * ({@code mouseX < 490 && mouseY < chatDrawY() + 125}), which the layout has to stay inside.
 */
public class OfferScan {

	public static void main(String[] args) throws Exception {
		String cache = args.length > 0 ? args[0] : System.getProperty("user.home") + "\\Biohazard.474";

		byte[] archive;
		byte[] fonts;
		RandomAccessFile data = null;
		RandomAccessFile index = null;
		try {
			data = new RandomAccessFile(new File(cache, "main_file_cache.dat"), "r");
			index = new RandomAccessFile(new File(cache, "main_file_cache.idx0"), "r");
			Decompressor d = new Decompressor(data, index, 1);
			archive = d.decompress(3);
			fonts = d.decompress(1);
		} finally {
			if (data != null) data.close();
			if (index != null) index.close();
		}

		StreamLoader title = new StreamLoader(fonts, "title");
		TextDrawingArea[] allFonts = {
				new TextDrawingArea(false, "p11_full", title),
				new TextDrawingArea(false, "p12_full", title),
				new TextDrawingArea(false, "b12_full", title),
				new TextDrawingArea(true, "q8_full", title),
		};
		RSInterface.unpack(new StreamLoader(archive, "interface"), allFonts, null);

		// chatDrawY() is 0 in these coordinates, so the gate is the panel's own.
		int chatDrawY = 0;
		int containerId = MakeX.CONTAINER_FIRST;
		RSInterface menu = RSInterface.interfaceCache[containerId];

		System.out.println("=== widgets the container makes clickable at all ===");
		System.out.println("  idx      id atAction content   w   h  drawn@(x,y)   tooltip");
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface c = RSInterface.interfaceCache[menu.children[i]];
			if (c == null || c.atActionType == 0 || c.width <= 0 || c.height <= 0) {
				continue;
			}
			System.out.println("  " + pad(i) + pad(c.id) + pad(c.atActionType) + pad(c.contentType)
					+ pad(c.width) + pad(c.height) + "  ("
					+ (menu.childX[i] + 20) + "," + (menu.childY[i] + 20) + ")"
					+ " .. (" + (menu.childX[i] + 20 + c.width) + ","
					+ (menu.childY[i] + 20 + c.height) + ")" + "  " + c.tooltip);
		}

		for (int amount = 0; amount < 5; amount++) {
			if (amount == MakeX.X_INDEX) {
				MakeX.acknowledgeAmount(7, containerId);
			} else {
				MakeX.select(amount);
			}
			System.out.println();
			System.out.println("=== " + MakeX.selected() + " armed ("
					+ (MakeX.selected() == MakeX.X_INDEX ? "X = " + MakeX.xAmount() : "fixed")
					+ ") ===");
			for (int slot = 0; slot < 5; slot++) {
				int[] rect = slotRect(menu, slot);
				if (rect == null) {
					continue;
				}
				// Centre of the product box, in the coordinates buildInterfaceMenu is given.
				int mx = (rect[0] + rect[2]) / 2;
				int my = (rect[1] + rect[3]) / 2;
				report("slot " + slot + " centre", menu, mx, my, chatDrawY);
			}
			for (int pill = 0; pill < 5; pill++) {
				int[] rect = rectOf(menu, MakeX.QUANTITY_FIRST + pill);
				if (rect == null) {
					continue;
				}
				int mx = (rect[0] + rect[2]) / 2;
				int my = (rect[1] + rect[3]) / 2;
				report("pill " + pill + " centre", menu, mx, my, chatDrawY);
			}
		}
	}

	/** One product box: the armed hitbox's drawn rect, found by size. */
	private static int[] slotRect(RSInterface menu, int slot) {
		int[] rect = null;
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface c = RSInterface.interfaceCache[menu.children[i]];
			if (c == null || c.atActionType == 0 || c.width != 64 || c.height != 82) {
				continue;
			}
			int[] here = { menu.childX[i] + 20, menu.childY[i] + 20,
					menu.childX[i] + 20 + c.width, menu.childY[i] + 20 + c.height };
			// Slots are ordered left to right by x.
			if (rect == null || here[0] < rect[0]) {
				// collect all then pick the nth
			}
		}
		int at = 0;
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface c = RSInterface.interfaceCache[menu.children[i]];
			if (c == null || c.atActionType == 0 || c.width != 64 || c.height != 82) {
				continue;
			}
			if (at == slot) {
				rect = new int[] { menu.childX[i] + 20, menu.childY[i] + 20,
						menu.childX[i] + 20 + c.width, menu.childY[i] + 20 + c.height };
				break;
			}
			at++;
		}
		return rect;
	}

	private static int[] rectOf(RSInterface menu, int id) {
		for (int i = 0; i < menu.children.length; i++) {
			if (menu.children[i] != id) {
				continue;
			}
			RSInterface c = RSInterface.interfaceCache[id];
			return new int[] { menu.childX[i] + 20, menu.childY[i] + 20,
					menu.childX[i] + 20 + c.width, menu.childY[i] + 20 + c.height };
		}
		return null;
	}

	/** buildMenu's gate, then buildInterfaceMenu, then what the click would do. */
	private static void report(String what, RSInterface menu, int mx, int my, int chatDrawY) {
		boolean gate = mx > 0 && my > chatDrawY && mx < 490 && my < chatDrawY + 125;
		System.out.print("  " + pad(what) + " mouse(" + mx + "," + my + ")  gate=" + (gate ? "open" : "SHUT"));
		if (!gate) {
			System.out.println("   -> the click never reaches the dialog");
			return;
		}
		StringBuilder offered = new StringBuilder();
		int last = -1;
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface c = RSInterface.interfaceCache[menu.children[i]];
			if (c == null) {
				continue;
			}
			int cx = menu.childX[i] + 20 + c.anInt263;
			int cy = menu.childY[i] + 20 + c.anInt265;
			if (c.type != 0 && c.atActionType == 1 && mx >= cx && my >= cy && mx < cx + c.width
					&& my < cy + c.height) {
				boolean friends = c.contentType != 0;
				offered.append(c.id).append(friends ? "(friends!)" : "").append(' ');
				if (!friends) {
					last = c.id;
				}
			}
		}
		System.out.print("  offers [ " + offered + "]  last=" + last);
		System.out.println("  MakeX claims slot " + MakeX.slotOfClick(last));
	}

	private static String pad(int v) {
		return pad(String.valueOf(v));
	}

	private static String pad(String s) {
		while (s.length() < 8) {
			s = " " + s;
		}
		return s;
	}
}
