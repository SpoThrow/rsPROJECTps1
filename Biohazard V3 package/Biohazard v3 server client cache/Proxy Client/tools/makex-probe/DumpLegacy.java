import java.io.File;
import java.io.RandomAccessFile;

import cache.Decompressor;
import cache.StreamLoader;
import ui.MakeX;
import ui.RSInterface;
import ui.TextDrawingArea;

/**
 * TEMPORARY diagnostic: why does a product click not reach the client's click handler?
 *
 * <p>Replicates the two conditions {@code client.buildInterfaceMenu} applies before it will offer
 * a widget as clickable:
 *
 * <pre>
 * if (child.type == 0) recurse;                       // containers are not buttons
 * else if (child.atActionType == 1 &amp;&amp; inside) offer;   // anything else can be
 * </pre>
 *
 * <p>and prints the widget fields that decide it.
 */
public class DumpLegacy {

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

		int containerId = MakeX.CONTAINER_FIRST;
		RSInterface menu = RSInterface.interfaceCache[containerId];
		System.out.println("container " + containerId + " children=" + menu.children.length
				+ " width=" + menu.width + " height=" + menu.height);

		// Arm X the way the client does once the prompt comes back.
		System.out.println("acknowledgeAmount -> " + MakeX.acknowledgeAmount(7, containerId)
				+ "  selected=" + MakeX.selected());
		int armed = MakeX.slotOfClick(8946);
		System.out.println("slotOfClick(8946 make-all) = " + armed);

		System.out.println();
		System.out.println("   idx      id  wid  type atAction content  w   h   mOverInter tooltip");
		for (int i = 0; i < menu.children.length; i++) {
			int id = menu.children[i];
			RSInterface c = RSInterface.interfaceCache[id];
			if (c == null) {
				System.out.println("  " + pad(i) + " MISSING " + id);
				continue;
			}
			System.out.println("  " + pad(i) + pad(id) + pad(c.id) + pad(c.type) + pad(c.atActionType)
					+ pad(c.contentType) + pad(c.width) + pad(c.height) + pad(c.mOverInterToTrigger)
					+ "  " + c.tooltip + "   @(" + menu.childX[i] + "," + menu.childY[i] + ")"
					+ (c.sprite1 == null ? "  noSprite" : "")
					+ "  off(" + c.anInt263 + "," + c.anInt265 + ")");
		}

		// Replicate buildInterfaceMenu for a click in the middle of slot 0's icon.
		int slotX = menu.childX[firstSlot(menu)];
		int clickX = slotX + 32;
		int clickY = 48 + 26;
		System.out.println();
		System.out.println("simulating a click at (" + clickX + "," + clickY + ") with dialog y=20:");
		int clickYNoOffset = clickY - 20;
		offer(menu, clickX, clickYNoOffset, 0);
	}

	private static int firstSlot(RSInterface menu) {
		for (int i = 0; i < menu.children.length; i++) {
			if (menu.children[i] == MakeX.slotFrameId(0, 0)) {
				return i;
			}
		}
		return 0;
	}

	/** The two clauses of buildInterfaceMenu, plus what it recursed into instead. */
	private static void offer(RSInterface menu, int mx, int my, int depth) {
		for (int i = 0; i < menu.children.length; i++) {
			int id = menu.children[i];
			RSInterface c = RSInterface.interfaceCache[id];
			if (c == null) {
				continue;
			}
			int cx = menu.childX[i] + c.anInt263;
			int cy = menu.childY[i] + c.anInt265;
			boolean inside = mx >= cx && my >= cy && mx < cx + c.width && my < cy + c.height;
			if (c.type == 0) {
				if (inside && depth < 3) {
					System.out.println("    recurse into type-0 child " + id + " (atActionType "
							+ c.atActionType + ")");
					offer(c, mx, my, depth + 1);
				}
			} else if (c.atActionType == 1 && inside) {
				System.out.println("    OFFERED  id=" + id + " type=" + c.type + " tooltip="
						+ c.tooltip);
			}
		}
	}

	private static String pad(int v) {
		return pad(String.valueOf(v));
	}

	private static String pad(String s) {
		while (s.length() < 7) {
			s = " " + s;
		}
		return s;
	}
}
