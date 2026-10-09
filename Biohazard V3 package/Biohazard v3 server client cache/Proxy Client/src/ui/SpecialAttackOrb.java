package ui;

/**
 * The client's view of the special-attack bar the SERVER is currently using, and the
 * button the orb has to press to toggle it.
 *
 * <p><b>Why the orb cannot just pick a bar.</b> A player carries a different special bar
 * per weapon family and the server swaps between them: equipping a whip shows bar
 * {@code 12323}, a scimitar {@code 7599}, a godsword {@code 7574}, and so on. Every bar
 * keeps whatever text it was last given, so at any moment SEVERAL bars carry a
 * "Special Attack (n%)" string and the ones the player is not holding are stale. Picking
 * the first match in a fixed list therefore targets the wrong bar as soon as the player has
 * touched more than one spec weapon - which is exactly how the orb used to fail: it kept
 * sending the whip's button after the player had switched to anything else.
 *
 * <p><b>What decides instead.</b> The server writes the bar text with {@code sendFrame126}
 * on every event that changes the current weapon or its energy - equip, login, toggle, spec
 * use, and the periodic restore. So the bar written MOST RECENTLY is the live one, and that
 * is the one signal this class keeps. {@link #onStringUpdate} is fed every string frame by
 * the packet handler and latches the frame id.
 *
 * <p><b>Two deliberate guards.</b> The latch only accepts a frame id this client knows to be
 * a spec bar, and only text that carries a percentage - so the cache's default bar caption
 * ({@code "S P E C I A L  A T T A C K"}, letter-spaced, no percentage, no colour code) can
 * never latch and an unrelated interface cannot hijack the orb.
 *
 * <p><b>The button ids are the SERVER's contract, not the client tree's.</b> The spec bar
 * containers in the live cache hang a low button ({@code 12311} for the whip) off child 0,
 * while the server registers the large ids below ({@code 48023} for the whip) - verified
 * against the real interface cache with {@code tools\spec-bar-probe}. Only the server's ids
 * toggle anything, so those are what the orb sends. The two exceptions are documented on
 * {@link #BARS}: {@code 29038} and {@code 29063} are the gmaul and dragon battleaxe
 * buttons, whose server bodies execute the special outright rather than flipping a flag,
 * and they are paired here because that is what pressing those bars does.
 *
 * <p><b>Why a latched bar can still be the wrong bar.</b> The latch says which bar the server
 * last spoke to, not whether the weapon in hand can spec at all. Equipping a weapon with no
 * special attack does not write any bar text - it HIDES the spec bar containers instead
 * ({@code sendFrame171(1, container)} in {@code addSpecialBar}'s default arm) and leaves
 * {@code specBarId} pointing at the last spec weapon. Without a second signal the orb would
 * go on showing that weapon's energy and pressing its button. {@link #onHiddenUpdate}
 * consumes those frames, so a hidden container disables the orb.
 *
 * <p>Pure logic on purpose: it holds no {@code client} reference and touches no interface
 * cache, so the harness drives it directly.
 */
public final class SpecialAttackOrb {

	/**
	 * {@code {spec-bar text frame, toggle button, spec-bar container}}. Mirrors the server's
	 * {@code SpecialAttackButtons} table plus the two execute-style bodies in
	 * {@code ClickingButtons}, and covers every bar {@code ItemAssistant.addSpecialBar}
	 * can show. The container is the frame the server shows or hides; the text frame is a
	 * child of it, and in the live cache the two ids are always 12 apart.
	 */
	private static final int[][] BARS = {
			{ 12335, 48023, 12323 }, // whip
			{ 7611, 29163, 7599 }, // dragon scimitar
			{ 8505, 33033, 8493 }, // dragon halberd
			{ 7486, 29038, 7474 }, // granite maul (executes)
			{ 7511, 29063, 7499 }, // dragon battleaxe (executes)
			{ 7812, 30108, 7800 }, // dragon claws
			{ 7586, 29138, 7574 }, // dragon dagger / longsword and most 2h swords
			{ 7561, 29113, 7549 }, // bows
			{ 7686, 29238, 7674 }, // dragon spear and other controlled weapons
			{ 7636, 29188, 7624 } // dragon mace
	};

	/** The colour code {@code ItemAssistant.updateSpecialBar} prefixes when special is on. */
	private static final String ACTIVE_PREFIX = "@yel@";

	/**
	 * Whether each bar's container is currently shown, indexed like {@link #BARS}.
	 *
	 * <p>Starts {@code true} - fail open. The server only sends a visibility frame for a spec
	 * container on equip and login, so a bar this client has latched is assumed live until the
	 * server explicitly hides it, which is what the non-spec arm of {@code addSpecialBar} does.
	 */
	private final boolean[] shown = new boolean[BARS.length];

	/** Index into {@link #BARS} of the latched bar, or {@code -1}. */
	private int row = -1;

	public SpecialAttackOrb() {
		for (int i = 0; i < shown.length; i++) {
			shown[i] = true;
		}
	}

	/**
	 * Feeds one {@code sendFrame126} to the latch. Called for every string frame, so an
	 * unrelated frame simply does not match and is ignored.
	 */
	public void onStringUpdate(int frameId, String text) {
		int r = rowFor(frameId);
		if (r >= 0 && isSpecText(text)) {
			row = r;
		}
	}

	/**
	 * Feeds one interface visibility frame (opcode 171). The wire flag is the server's
	 * HIDE - {@code addSpecialBar}'s non-spec arm sends {@code 1} to every spec container -
	 * so the argument is named for what it means rather than for what the byte is called.
	 * Called for every such frame, so an unrelated interface is ignored.
	 */
	public void onHiddenUpdate(int containerId, boolean hidden) {
		int r = rowForContainer(containerId);
		if (r >= 0) {
			shown[r] = !hidden;
		}
	}

	/** True when a bar has been latched AND the server has not hidden its container. */
	public boolean live() {
		return row >= 0 && shown[row];
	}

	/** The frame id of the live spec bar, or {@code -1} if there is none. */
	public int frame() {
		return live() ? BARS[row][0] : -1;
	}

	/** The button to send for the live bar, or {@code -1} when there is no live bar. */
	public int button() {
		return live() ? BARS[row][1] : -1;
	}

	/**
	 * True for the text the server writes to a spec bar. Requires the percentage, so the
	 * cache's letter-spaced caption and any other mention of special attacks are rejected.
	 */
	public static boolean isSpecText(String text) {
		if (text == null || text.indexOf('%') < 0) {
			return false;
		}
		return text.toLowerCase().indexOf("special attack") >= 0;
	}

	/** The percentage in {@code "@bla@ Special Attack (62%)"}, or {@code 0}. */
	public static int percentOf(String text) {
		if (text == null) {
			return 0;
		}
		int open = text.lastIndexOf('(');
		int percent = text.lastIndexOf('%');
		if (open < 0 || percent <= open) {
			return 0;
		}
		try {
			int value = Integer.parseInt(text.substring(open + 1, percent).trim());
			if (value < 0) {
				return 0;
			}
			return value > 100 ? 100 : value;
		} catch (NumberFormatException notANumber) {
			return 0;
		}
	}

	/** True when the bar's text carries the server's "special attack is on" colour. */
	public static boolean isActive(String text) {
		return text != null && text.startsWith(ACTIVE_PREFIX);
	}

	/** The {@link #BARS} row whose text frame is {@code frameId}, or {@code -1}. */
	static int rowFor(int frameId) {
		for (int i = 0; i < BARS.length; i++) {
			if (BARS[i][0] == frameId) {
				return i;
			}
		}
		return -1;
	}

	/** The {@link #BARS} row whose container is {@code containerId}, or {@code -1}. */
	static int rowForContainer(int containerId) {
		for (int i = 0; i < BARS.length; i++) {
			if (BARS[i][2] == containerId) {
				return i;
			}
		}
		return -1;
	}
}
