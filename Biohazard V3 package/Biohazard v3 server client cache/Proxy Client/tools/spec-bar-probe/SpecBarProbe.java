import java.io.RandomAccessFile;

import cache.Decompressor;
import cache.StreamLoader;
import sign.signlink;
import ui.RSInterface;
import ui.TextDrawingArea;

/**
 * Ground truth for the special-attack orb: what the LIVE client interface tree actually
 * contains for the spec bars, after the cache unpack AND the Interfaces patches have both
 * run.
 *
 * <p>WHY THIS EXISTS. The orb has to send the button id the SERVER's button table listens
 * for, and there are two mutually exclusive candidates in the tree: the id the interface
 * code writes into the bar container (`specialBar` -> `addActionButton(id - 12, ...)`) and
 * the large id the server's table pairs with the bar's TEXT id (`48023 -> 12335`). Reading
 * either one off the source is not enough, because `RSInterface.unpack` loads the bar from
 * the CACHE first and `Interfaces.loadInterfaces` then PATCHES the same container - so the
 * live value is whichever of the two wins at runtime, and that is what this probe measures.
 *
 * <p>It also reports, for every spec-bar text id the server writes to, whether the message
 * is present and whether a button-shaped child sits in the same container - which is the
 * pair the orb's selection has to key off.
 *
 * <p>Deliberately non-hermetic: it needs the real cache. Run it with
 * {@code tools\spec-bar-probe\run.ps1}.
 *
 * <p>Exit code 0 = the interface archive unpacked; 2 = the cache is missing.
 */
public class SpecBarProbe {

	/** {textId, buttonId} exactly as the server's SpecialAttackButtons/switch pairs them. */
	private static final int[][] SERVER_PAIRS = {
			{ 12335, 48023 }, { 7611, 29163 }, { 8505, 33033 }, { 7486, 29038 },
			{ 7812, 30108 }, { 7586, 29138 }, { 7561, 29113 }, { 7686, 29238 },
			{ 7636, 29188 }
	};

	public static void main(String[] args) throws Exception {
		String dir = signlink.findcachedir();
		System.out.println("cache dir: " + dir);

		RandomAccessFile data = new RandomAccessFile(dir + "main_file_cache.dat", "r");
		RandomAccessFile[] idx = new RandomAccessFile[5];
		Decompressor[] dec = new Decompressor[5];
		for (int i = 0; i < 5; i++) {
			idx[i] = new RandomAccessFile(dir + "main_file_cache.idx" + i, "r");
			dec[i] = new Decompressor(data, idx[i], i + 1);
		}

		// Mirrors client.streamLoaderForName: interfaces/media/title all come out of the
		// index-1 decompressor, keyed by archive.
		StreamLoader ifaceLoader = new StreamLoader(dec[0].decompress(3), "interface");
		StreamLoader mediaLoader = new StreamLoader(dec[0].decompress(4), "media");
		StreamLoader titleLoader = new StreamLoader(dec[0].decompress(1), "title");
		if (ifaceLoader.getDataForName("data") == null) {
			System.out.println("interface archive not readable from this cache.");
			System.exit(2);
			return;
		}

		TextDrawingArea[] fonts = {
				new TextDrawingArea(false, "p11_full", titleLoader),
				new TextDrawingArea(false, "p12_full", titleLoader),
				new TextDrawingArea(false, "b12_full", titleLoader),
				new TextDrawingArea(true, "q8_full", titleLoader)
		};

		try {
			RSInterface.unpack(ifaceLoader, fonts, mediaLoader);
		} catch (Throwable t) {
			System.out.println("unpack threw (" + t + ") - reporting the partial tree.");
		}
		System.out.println("interfaceCache: " + RSInterface.interfaceCache.length + " slots");
		System.out.println();

		System.out.println("=== every interface whose tooltip mentions a special attack ===");
		for (int id = 0; id < RSInterface.interfaceCache.length; id++) {
			RSInterface r = RSInterface.interfaceCache[id];
			if (r == null || r.tooltip == null) {
				continue;
			}
			if (r.tooltip.toLowerCase().indexOf("special") < 0) {
				continue;
			}
			System.out.println("  id=" + id
					+ " atActionType=" + r.atActionType
					+ " type=" + r.type
					+ " parent=" + r.parentID
					+ " w=" + r.width + " h=" + r.height
					+ " tooltip=" + quote(r.tooltip));
		}
		System.out.println();

		System.out.println("=== server's {textId -> buttonId} pairs, checked against the tree ===");
		for (int[] pair : SERVER_PAIRS) {
			int textId = pair[0];
			int buttonId = pair[1];
			RSInterface text = at(textId);
			RSInterface button = at(buttonId);
			System.out.println("text " + textId + " -> button " + buttonId);
			System.out.println("    text iface    : " + describe(text));
			if (text != null) {
				System.out.println("    text message  : " + quote(text.message));
				System.out.println("    text parent   : " + text.parentID + " -> " + describe(at(text.parentID)));
			}
			System.out.println("    button iface  : " + describe(button));
			int container = text != null ? text.parentID : -1;
			System.out.println("    container kids: " + childrenOf(container));
		}
		System.out.println();

		System.out.println("=== the bar containers named by the server's sendFrame171 calls ===");
		int[] containers = { 12323, 7599, 7574, 7624, 7499, 7549, 8493, 7800, 7474, 7674 };
		for (int c : containers) {
			RSInterface r = at(c);
			System.out.println("  container " + c + ": " + describe(r));
			System.out.println("      kids: " + childrenOf(c));
		}
	}

	private static RSInterface at(int id) {
		if (id < 0 || id >= RSInterface.interfaceCache.length) {
			return null;
		}
		return RSInterface.interfaceCache[id];
	}

	private static String describe(RSInterface r) {
		if (r == null) {
			return "(absent)";
		}
		return "type=" + r.type + " atActionType=" + r.atActionType
				+ " id=" + r.id + " parent=" + r.parentID
				+ " w=" + r.width + " h=" + r.height
				+ (r.tooltip != null ? " tooltip=" + quote(r.tooltip) : "")
				+ (r.message != null ? " message=" + quote(r.message) : "");
	}

	private static String childrenOf(int container) {
		RSInterface r = at(container);
		if (r == null || r.children == null) {
			return "(no children)";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < r.children.length; i++) {
			int child = r.children[i];
			RSInterface cr = at(child);
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(i).append(":").append(child);
			if (cr != null && (cr.atActionType != 0 || cr.tooltip != null)) {
				sb.append("(at=").append(cr.atActionType);
				if (cr.tooltip != null) {
					sb.append(" tip=").append(quote(cr.tooltip));
				}
				sb.append(")");
			}
		}
		return sb.toString();
	}

	private static String quote(String s) {
		if (s == null) {
			return "null";
		}
		return "\"" + s.replace("\n", "\\n") + "\"";
	}
}
