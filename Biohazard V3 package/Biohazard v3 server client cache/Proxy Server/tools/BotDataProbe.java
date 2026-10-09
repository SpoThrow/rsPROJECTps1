import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import server.clip.region.ObjectDef;
import server.clip.region.Region;

/**
 * THROWAWAY read-only probe for the Bot Workshop editor design pass.
 *
 * <p>Not part of the server build (build.gradle only compiles src/ and test/).
 * Run with the working directory set to "Proxy Server" so ./Data resolves.
 *
 * <p>Answers: (1) ObjectDef action histogram + icon-rule counts + def-vs-handler
 * diff, and (3) measured realObjects counts and nearest-scan timings.
 *
 * <p>Note: the shipped {@code ObjectDef.readValues} reads strings with a 0x0A
 * terminator; this cache is 0x00-terminated, so this probe decodes names/actions
 * itself with the correct terminator (the hex dump at the top proves which).
 */
public final class BotDataProbe {

	private static final String[] ICON_RULES = {
			"Bank", "Chop down", "Mine", "Cook", "Smith", "Pray", "Recharge"
	};

	private static final String[] ICON_KEYWORDS = {
			"bank", "chop", "mine", "cook", "smith", "pray", "recharge"
	};

	/** Forces javac to compile the action registry (referenced only by name otherwise). */
	private static final Class<?> ANCHOR = server.game.players.actions.objects.ObjectHandler.class;

	public static void main(String[] args) throws Exception {
		if (ANCHOR == null) {
			return;
		}
		StringBuilder out = new StringBuilder();
		PrintStream file = new PrintStream(new File("tools/botdata-dump.txt"), "UTF-8");

		long t0 = System.currentTimeMillis();
		ObjectDef.loadConfig();
		long t1 = System.currentTimeMillis();
		line(out, file, "[probe] ObjectDef.loadConfig() = " + (t1 - t0) + " ms");

		int entryCount = archiveContentSize();
		line(out, file, "[probe] loc.dat entries (loc.idx/12) = " + entryCount);

		line(out, file, "");
		line(out, file, "=== raw loc.dat entry hex (first 64 bytes) ===");
		for (int id : new int[] { 1276, 1278, 2091, 2213, 1530 }) {
			byte[] raw = archiveEntry(id);
			line(out, file, "id " + id + ": " + (raw == null ? "<none>" : hex(raw, 64)));
		}

		Map<Integer, String> names = new HashMap<Integer, String>();
		Map<Integer, String[]> actionsById = new HashMap<Integer, String[]>();
		Map<String, Integer> hist = new TreeMap<String, Integer>();
		Map<String, List<String>> samples = new HashMap<String, List<String>>();
		int named = 0, withActions = 0, decodeFail = 0;

		for (int id = 0; id < entryCount; id++) {
			byte[] raw = archiveEntry(id);
			String[] dec = raw == null ? null : decode(raw);
			if (dec == null) {
				decodeFail++;
				continue;
			}
			String name = dec[0];
			if (name != null && name.length() > 1) {
				names.put(id, name);
				named++;
			}
			boolean any = false;
			String[] acts = new String[dec.length - 1];
			System.arraycopy(dec, 1, acts, 0, acts.length);
			for (String a : acts) {
				if (a != null && a.length() > 0) {
					any = true;
					hist.put(a, hist.getOrDefault(a, 0) + 1);
					List<String> s = samples.computeIfAbsent(a, k -> new ArrayList<String>());
					if (s.size() < 5 && name != null && name.length() > 1 && !s.contains(name)) {
						s.add(name);
					}
				}
			}
			if (any) {
				withActions++;
				actionsById.put(id, acts);
			}
		}
		line(out, file, "[probe] decoded ok = " + (entryCount - decodeFail)
				+ ", failed/unsupported = " + decodeFail);
		line(out, file, "[probe] objects with a name = " + named
				+ ", with >=1 action = " + withActions);
		line(out, file, "[probe] distinct action strings = " + hist.size());

		// (a) histogram
		line(out, file, "");
		line(out, file, "=== (1a) action string histogram (count, up to 5 samples) ===");
		List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(hist.entrySet());
		entries.sort((a, b) -> b.getValue() - a.getValue());
		for (Map.Entry<String, Integer> e : entries) {
			List<String> s = samples.getOrDefault(e.getKey(), Collections.<String>emptyList());
			line(out, file, String.format("%-22s %6d   %s", e.getKey(), e.getValue(), String.join(" | ", s)));
		}

		// (b) icon rules
		line(out, file, "");
		line(out, file, "=== (1b) icon-rule object matches (action substring; + name for Bank) ===");
		for (int r = 0; r < ICON_RULES.length; r++) {
			String kw = ICON_KEYWORDS[r];
			int matches = 0;
			List<String> ex = new ArrayList<String>();
			for (Map.Entry<Integer, String[]> e : actionsById.entrySet()) {
				if (matchesRule(e.getKey(), e.getValue(), names.get(e.getKey()), kw)) {
					matches++;
					if (ex.size() < 6) {
						String n = names.get(e.getKey());
						ex.add(e.getKey() + (n == null ? "" : "=" + n));
					}
				}
			}
			line(out, file, String.format("%-12s %6d   %s", ICON_RULES[r], matches, String.join(", ", ex)));
		}

		// (c) def-vs-handler diff
		line(out, file, "");
		line(out, file, "=== (1c) def-vs-handler diff ===");
		Map<String, Set<Integer>> regByClick = extractRegistryKeys();
		Map<String, Set<Integer>> swByClick = extractSwitchCases();
		Set<Integer> handlerAll = new HashSet<Integer>();
		for (Set<Integer> s : regByClick.values()) {
			handlerAll.addAll(s);
		}
		for (Set<Integer> s : swByClick.values()) {
			handlerAll.addAll(s);
		}
		line(out, file, "[handler] registry keys: FIRST=" + size(regByClick, "FIRST")
				+ " SECOND=" + size(regByClick, "SECOND") + " THIRD=" + size(regByClick, "THIRD"));
		line(out, file, "[handler] switch cases: FIRST=" + size(swByClick, "FIRST")
				+ " SECOND=" + size(swByClick, "SECOND") + " THIRD=" + size(swByClick, "THIRD"));
		line(out, file, "[handler] union of handler ids = " + handlerAll.size());

		Set<Integer> defOnly = new HashSet<Integer>();
		Set<Integer> both = new HashSet<Integer>();
		for (Integer id : actionsById.keySet()) {
			if (handlerAll.contains(id)) {
				both.add(id);
			} else {
				defOnly.add(id);
			}
		}
		Set<Integer> handlerOnly = new HashSet<Integer>(handlerAll);
		handlerOnly.removeAll(actionsById.keySet());
		line(out, file, "def-only (has action, no handler)   = " + defOnly.size());
		line(out, file, "handler-only (handler, no action)   = " + handlerOnly.size());
		line(out, file, "both                                 = " + both.size());
		int n = 0;
		line(out, file, "def-only sample:");
		for (Integer id : defOnly) {
			if (n++ >= 25) {
				break;
			}
			line(out, file, "   " + id + " " + names.getOrDefault(id, "<no name>")
					+ " " + java.util.Arrays.toString(actionsById.get(id)));
		}
		n = 0;
		line(out, file, "handler-only sample:");
		for (Integer id : handlerOnly) {
			if (n++ >= 25) {
				break;
			}
			line(out, file, "   " + id + " " + names.getOrDefault(id, "<no name>"));
		}

		// (3) region measurements
		long r0 = System.currentTimeMillis();
		Region.load();
		long r1 = System.currentTimeMillis();
		line(out, file, "");
		line(out, file, "[probe] Region.load() = " + (r1 - r0) + " ms");
		measureRegions(out, file);

		file.flush();
		file.close();
		System.out.println(out.toString());
		System.out.println("[probe] full dump written to Proxy Server/tools/botdata-dump.txt");
	}

	private static boolean matchesRule(int id, String[] acts, String name, String kw) {
		for (String a : acts) {
			if (a != null && a.toLowerCase().contains(kw)) {
				return true;
			}
		}
		if (kw.equals("bank") && name != null) {
			String ln = name.toLowerCase();
			return ln.contains("bank booth") || ln.contains("bank chest") || ln.equals("bank");
		}
		return false;
	}

	private static void measureRegions(StringBuilder out, PrintStream f) throws Exception {
		Map<Integer, Region> byId = allRegions();
		int total = 0, nonEmpty = 0, max = 0, min = Integer.MAX_VALUE, maxRegion = -1;
		List<Integer> sizes = new ArrayList<Integer>();
		for (Map.Entry<Integer, Region> e : byId.entrySet()) {
			int sz = e.getValue().realObjects.size();
			total += sz;
			sizes.add(sz);
			if (sz > 0) {
				nonEmpty++;
			}
			if (sz > max) {
				max = sz;
				maxRegion = e.getKey();
			}
			if (sz < min) {
				min = sz;
			}
		}
		double avg = byId.isEmpty() ? 0 : (double) total / byId.size();
		Collections.sort(sizes);
		int median = sizes.isEmpty() ? 0 : sizes.get(sizes.size() / 2);
		line(out, f, "");
		line(out, f, "=== (3) Region.realObjects measurements ===");
		line(out, f, "regions in registry          = " + byId.size());
		line(out, f, "regions with >=1 realObject  = " + nonEmpty);
		line(out, f, "total realObjects            = " + total);
		line(out, f, "per-region min/avg/median/max= " + min + " / " + String.format("%.1f", avg)
				+ " / " + median + " / " + max + " (max region " + maxRegion + ")");

		Map<Integer, String[]> memoA = new HashMap<Integer, String[]>();
		Map<Integer, String> memoN = new HashMap<Integer, String>();
		int[] counts = new int[ICON_RULES.length];
		for (Region r : byId.values()) {
			for (server.game.objects.Objects ob : r.realObjects) {
				int id = ob.objectId;
				if (id < 0) {
					continue;
				}
				String[] a = memoA.get(id);
				if (a == null && !memoA.containsKey(id)) {
					byte[] raw = archiveEntry(id);
					String[] dec = raw == null ? null : decode(raw);
					a = dec == null ? new String[0] : java.util.Arrays.copyOfRange(dec, 1, dec.length);
					memoA.put(id, a);
					memoN.put(id, dec == null ? null : dec[0]);
				}
				String nm = memoN.get(id);
				for (int k = 0; k < ICON_RULES.length; k++) {
					if (matchesRule(id, a, nm, ICON_KEYWORDS[k])) {
						counts[k]++;
					}
				}
			}
		}
		line(out, f, "live realObjects per icon rule:");
		for (int k = 0; k < ICON_RULES.length; k++) {
			line(out, f, String.format("   %-12s %7d", ICON_RULES[k], counts[k]));
		}

		int probeX = 3087, probeY = 3236;
		if (Region.getRegion(probeX, probeY) == null) {
			line(out, f, "Draynor region not loaded; skipping scan timing");
			return;
		}
		List<Region> nines = new ArrayList<Region>();
		for (int dx = -64; dx <= 64; dx += 64) {
			for (int dy = -64; dy <= 64; dy += 64) {
				Region r = Region.getRegion(probeX + dx, probeY + dy);
				if (r != null) {
					nines.add(r);
				}
			}
		}
		int objs = 0;
		for (Region r : nines) {
			objs += r.realObjects.size();
		}
		line(out, f, "");
		line(out, f, "nearest-scan window: " + nines.size() + " regions (" + objs
				+ " objects) around " + probeX + "," + probeY);

		int iters = 500;
		for (int i = 0; i < 50; i++) {
			scan(nines, probeX, probeY, false);
			scan(nines, probeX, probeY, true);
		}
		long s1 = System.nanoTime();
		for (int i = 0; i < iters; i++) {
			scan(nines, probeX, probeY, false);
		}
		long s2 = System.nanoTime();
		for (int i = 0; i < iters; i++) {
			scan(nines, probeX, probeY, true);
		}
		long s3 = System.nanoTime();
		line(out, f, String.format("distance-only scan  : %.3f ms/scan", (s2 - s1) / 1e6 / iters));
		line(out, f, String.format("distance+name scan  : %.3f ms/scan", (s3 - s2) / 1e6 / iters));
	}

	private static int scan(List<Region> regions, int px, int py, boolean withName) {
		int best = Integer.MAX_VALUE, found = 0;
		Map<Integer, String> memo = new HashMap<Integer, String>();
		for (Region r : regions) {
			for (server.game.objects.Objects ob : r.realObjects) {
				if (ob.objectId < 0) {
					continue;
				}
				int d = Math.max(Math.abs(ob.objectX - px), Math.abs(ob.objectY - py));
				if (withName) {
					String nm = memo.get(ob.objectId);
					if (nm == null && !memo.containsKey(ob.objectId)) {
						byte[] raw = archiveEntry(ob.objectId);
						String[] dec = raw == null ? null : decode(raw);
						nm = dec == null ? "" : (dec[0] == null ? "" : dec[0]);
						memo.put(ob.objectId, nm);
					}
					if (nm.toLowerCase().contains("oak")) {
						found++;
						best = Math.min(best, d);
					}
				} else {
					best = Math.min(best, d);
				}
			}
		}
		return withName ? found : best;
	}

	private static int size(Map<String, Set<Integer>> m, String k) {
		Set<Integer> s = m.get(k);
		return s == null ? 0 : s.size();
	}

	@SuppressWarnings("unchecked")
	private static Map<Integer, Region> allRegions() throws Exception {
		Field f = Region.class.getDeclaredField("regionById");
		f.setAccessible(true);
		return (Map<Integer, Region>) f.get(null);
	}

	private static Object archiveObj;

	private static Object archive() throws Exception {
		if (archiveObj == null) {
			Field f = ObjectDef.class.getDeclaredField("archive");
			f.setAccessible(true);
			archiveObj = f.get(null);
		}
		return archiveObj;
	}

	private static int archiveContentSize() {
		try {
			Method m = archive().getClass().getMethod("contentSize");
			return (Integer) m.invoke(archive());
		} catch (Throwable t) {
			return 20000;
		}
	}

	private static byte[] archiveEntry(int id) {
		try {
			Method m = archive().getClass().getMethod("get", int.class);
			return (byte[]) m.invoke(archive(), id);
		} catch (Throwable t) {
			return null;
		}
	}

	private static String hex(byte[] b, int max) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < Math.min(max, b.length); i++) {
			s.append(String.format("%02x ", b[i] & 0xff));
		}
		return s.toString();
	}

	// ---- null-terminated loc decoder (474 opcode table, 0x00 strings) ----

	private static final class R {
		final byte[] b;
		int p;

		R(byte[] b) {
			this.b = b;
		}

		int u8() {
			return b[p++] & 0xff;
		}

		int s8() {
			return b[p++];
		}

		int u16() {
			p += 2;
			return ((b[p - 2] & 0xff) << 8) + (b[p - 1] & 0xff);
		}

		void skip(int n) {
			p += n;
		}

		String str0() {
			int s = p;
			while (b[p++] != 0) {
			}
			return new String(b, s, p - s - 1, StandardCharsets.ISO_8859_1);
		}
	}

	/** @return [name, a0..a9] or null if the definition cannot be walked. */
	private static String[] decode(byte[] raw) {
		try {
			R r = new R(raw);
			String name = null;
			String[] actions = null;
			while (true) {
				int op = r.u8();
				if (op == 0) {
					break;
				}
				if (op == 1) {
					int len = r.u8();
					r.skip(len * 3);
				} else if (op == 2) {
					name = r.str0();
				} else if (op == 3) {
					r.str0();
				} else if (op == 5) {
					int len = r.u8();
					r.skip(len * 2);
				} else if (op >= 30 && op < 39) {
					if (actions == null) {
						actions = new String[9];
					}
					String a = r.str0();
					actions[op - 30] = a.equalsIgnoreCase("hidden") ? null : a;
				} else if (op == 14 || op == 15 || op == 19 || op == 28 || op == 69 || op == 75) {
					r.u8();
				} else if (op == 29 || op == 39) {
					r.s8();
				} else if (op == 17 || op == 18 || op == 21 || op == 22 || op == 23 || op == 27
						|| op == 62 || op == 64 || op == 73 || op == 74 || op == 82 || op == 88
						|| op == 89 || op == 90 || op == 91 || op == 94 || op == 95 || op == 96
						|| op == 97) {
					// no payload
				} else if (op == 24 || op == 60 || op == 65 || op == 66 || op == 67 || op == 68
						|| op == 70 || op == 71 || op == 72) {
					r.u16();
				} else if (op == 40) {
					int n0 = r.u8();
					r.skip(n0 * 4);
				} else if (op == 41) {
					int n0 = r.u8();
					r.skip(n0 * 4);
				} else if (op == 42) {
					int n0 = r.u8();
					r.skip(n0);
				} else if (op == 77 || op == 92) {
					r.u16();
					r.u16();
					if (op == 92) {
						r.u16();
					}
					int n0 = r.u8();
					for (int j = 0; j <= n0; j++) {
						r.u16();
					}
				} else if (op == 78) {
					r.skip(3);
				} else if (op == 79) {
					r.skip(5);
					int n0 = r.u8();
					r.skip(n0 * 2);
				} else if (op == 81) {
					r.skip(1);
				} else if (op == 93) {
					r.skip(2);
				} else if (op == 249) {
					int n0 = r.u8();
					for (int j = 0; j < n0; j++) {
						boolean b = r.u8() == 1;
						r.skip(3);
						if (b) {
							r.str0();
						} else {
							r.skip(4);
						}
					}
				} else {
					return null;
				}
			}
			String[] res = new String[1 + (actions == null ? 0 : actions.length)];
			res[0] = name;
			if (actions != null) {
				System.arraycopy(actions, 0, res, 1, actions.length);
			}
			return res;
		} catch (Throwable t) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Set<Integer>> extractRegistryKeys() {
		Map<String, Set<Integer>> out = new LinkedHashMap<String, Set<Integer>>();
		try {
			Class<?> oh = Class.forName("server.game.players.actions.objects.ObjectHandler");
			Field f = oh.getDeclaredField("byClick");
			f.setAccessible(true);
			Map<Object, Object> byClick = (Map<Object, Object>) f.get(null);
			for (Map.Entry<Object, Object> e : byClick.entrySet()) {
				Map<Integer, Object> inner = (Map<Integer, Object>) e.getValue();
				out.put(String.valueOf(e.getKey()), new HashSet<Integer>(inner.keySet()));
			}
		} catch (Throwable t) {
			System.out.println("[probe] registry reflection failed: " + t);
		}
		return out;
	}

	private static Map<String, Set<Integer>> extractSwitchCases() {
		Map<String, Set<Integer>> out = new LinkedHashMap<String, Set<Integer>>();
		try {
			String src = new String(Files.readAllBytes(
					Paths.get("src/server/game/players/ActionHandler.java")), StandardCharsets.UTF_8);
			String clean = clean(src);
			out.put("FIRST", switchCases(clean, "void firstClickObject(", "switch(objectType)"));
			out.put("SECOND", switchCases(clean, "void secondClickObject(", "switch(objectType)"));
			out.put("THIRD", switchCases(clean, "void thirdClickObject(", "switch(objectType)"));
		} catch (Throwable t) {
			System.out.println("[probe] switch parse failed: " + t);
		}
		return out;
	}

	private static Set<Integer> switchCases(String src, String methodSig, String switchHead) {
		Set<Integer> ids = new HashSet<Integer>();
		int mi = src.indexOf(methodSig);
		if (mi < 0) {
			return ids;
		}
		int si = src.indexOf(switchHead, mi);
		if (si < 0) {
			si = src.indexOf(switchHead.replace("switch(", "switch ("), mi);
		}
		if (si < 0) {
			return ids;
		}
		int open = src.indexOf('{', si + 6);
		if (open < 0) {
			return ids;
		}
		StringBuilder top = new StringBuilder();
		int depth = 1;
		for (int i = open + 1; i < src.length() && depth > 0; i++) {
			char c = src.charAt(i);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
			} else if (depth == 1) {
				top.append(c);
			}
		}
		java.util.regex.Matcher mm = java.util.regex.Pattern.compile("case\\s+(\\d+)\\s*:").matcher(top);
		while (mm.find()) {
			ids.add(Integer.parseInt(mm.group(1)));
		}
		return ids;
	}

	private static String clean(String s) {
		StringBuilder b = new StringBuilder(s.length());
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
				while (i < n && s.charAt(i) != '\n') {
					b.append(' ');
					i++;
				}
				if (i < n) {
					b.append('\n');
				}
			} else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
				b.append(' ');
				b.append(' ');
				i += 2;
				while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
					b.append(s.charAt(i) == '\n' ? '\n' : ' ');
					i++;
				}
				if (i + 1 < n) {
					b.append(' ');
					b.append(' ');
					i++;
				}
			} else if (c == '"') {
				b.append(' ');
				i++;
				while (i < n && s.charAt(i) != '"') {
					if (s.charAt(i) == '\\') {
						b.append(' ');
						i++;
					}
					if (i < n) {
						b.append(s.charAt(i) == '\n' ? '\n' : ' ');
					}
					i++;
				}
				if (i < n) {
					b.append(' ');
				}
			} else if (c == '\'') {
				b.append(' ');
				i++;
				while (i < n && s.charAt(i) != '\'') {
					if (s.charAt(i) == '\\') {
						b.append(' ');
						i++;
					}
					if (i < n) {
						b.append(s.charAt(i) == '\n' ? '\n' : ' ');
					}
					i++;
				}
				if (i < n) {
					b.append(' ');
				}
			} else {
				b.append(c);
			}
		}
		return b.toString();
	}

	private static void line(StringBuilder out, PrintStream f, String s) {
		f.println(s);
		out.append(s).append('\n');
	}

	private BotDataProbe() {
	}
}
