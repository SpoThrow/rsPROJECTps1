import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

import cache.Decompressor;
import cache.StreamLoader;
import def.ItemDef;
import model.Model;
import net.OnDemandFetcherParent;
import ui.MakeX;
import ui.RSInterface;
import ui.Sprite;
import ui.TextDrawingArea;

/**
 * Builds the five quantity-first creation chatboxes out of the REAL cache and checks them.
 *
 * <p><b>Why this exists.</b> {@code MakeX} rearranges widgets the cache defines: it reparents each
 * interface's models, its four invisible make buttons and its title, then adds a quantity row and a
 * label per product. Whether that adds up is a fact about the client's copy of interfaces 8938,
 * 4429, 8880, 1743 and 2400 - which ids are children, and which of them are models, buttons and the
 * title - and none of it is knowable from the source. The tables in that class were read off the
 * interface bytes, so they are exactly the thing that can be silently wrong: a mistyped frame id
 * does not throw, it draws an item where nothing is and leaves an invisible hitbox somewhere else.
 *
 * <p><b>Why it has to be a probe and not the harness.</b> Interface definitions live in the cache
 * (index 0, archive 3) and {@code MakeX.build} runs from {@code RSInterface.unpack}, which needs the
 * real archive and the real font set. No hermetic test supplies a cache, so this is measured against
 * the real one, like {@code MouldInterfaceProbe} next to it.
 *
 * <p><b>What it can and cannot check.</b> It cannot re-derive the tables without copying them, and a
 * test that copies the code under test proves nothing. So it checks the SHAPE those tables have to
 * produce instead: every child resolves, exactly one live hitbox per product, the live hitbox is an
 * id the legacy interface already had (which is the whole reason a product click still reaches the
 * server), the four amounts pick four different ids per product, and the names land on the labels.
 * It also prints the legacy children it did not recognise, which is the one thing a bad table would
 * show up as - the corner ornaments are legitimately in that list, a mistyped model frame is not.
 *
 * <p>Exit code 0 = every check held (MAKEX_OK), 1 = a check failed or the cache could not be read.
 */
public class MakeXProbe {

	/* -------------------------------------------------------------------------
	 * Shared with MakeXPreview, which draws the same layout as a diagram, so the
	 * restated geometry below is package-private rather than private.
	 * ---------------------------------------------------------------------- */

	/** The five cache interfaces, in the order MakeX's own tables use. */
	static final int[] LEGACY = { 8938, 4429, 8880, 1743, 2400 };

	/** Products each family offers, which is also the slots its labels are laid out against. */
	static final int[] SLOTS = { 5, 1, 3, 1, 8 };

	/** For the report and the preview only; nothing here depends on these being right. */
	static final String[] FAMILY_NAMES = {
			"creation", "herblore", "fletching", "cooking", "smelting" };

	/* What MakeX lays out for the geometry assertions below.
	 *
	 * <p><b>These are the file's own numbers, read out of it rather than restated.</b> Every value
	 * here is one the layout editor moves: {@code EditChatbox.bat} drags a box, writes the constant
	 * back into {@code MakeX.java}, and a number this probe merely repeated would then be checking
	 * the layout against a stale opinion of it - which is exactly what happened, and why the read
	 * below exists. The defaults kept next to each name are the fallback for a run that cannot see
	 * the source at all, and the values in them are the ones the source last had.
	 *
	 * <p>The assertions still mean what they meant: they check the BUILT widgets against these
	 * numbers, so a build that does not reflect the source (a stale class, a bad childY) still fails.
	 */
	/**
	 * The row's length, which is also the number of pills. Four: 1 / 5 / 10 / All. The custom X
	 * entry is gone end to end, and with it the fifth pill the reference layout drew.
	 */
	static final int AMOUNTS = 4;
	/** What the SERVER can be asked for per product. The coupling, and the thing that cannot grow. */
	static final int SERVER_AMOUNTS = 4;
	static final int ALL_INDEX = 3;
	static final int WIDTH = 519;
	static final int HEIGHT = 142;
	static int SLOT_H = 82;
	static int SLOT_TOP = 52;
	/**
	 * Where the question and the help line sit, and how wide the question is.
	 *
	 * <p>The question is left-aligned against {@link #TITLE_X} rather than centred over the room the
	 * pills leave: it is the row's caption, the reference screenshot has it hard against the corner,
	 * and a caption wider than the pill block's gap would otherwise be centred under the pills.
	 *
	 * <p>Restated from the source, and the number that has moved twice: 14 was the first corner,
	 * where the play art's bevel still cut into the caption's first letters, and it is 22 now.
	 */
	static int TITLE_X = 22;
	static int TITLE_Y = 17;
	static int TITLE_H = 18;
	static int HELP_Y = 38;
	static int HELP_H = 12;
	/**
	 * The height of a product's own label widget, which is not a child of the container and so is
	 * never drawn: the name is kept only so {@code absorbName} has somewhere to put the text it
	 * takes off the cache's buttons, and the item's icon is the label now.
	 */
	static int LABEL_H = 12;
	/** Where those undrawn label widgets live, restated from MakeX's table. */
	static final int LABEL_FIRST = 51930;
	/**
	 * The label id stride, which is MakeX's own {@code SLOTS} of 8 and NOT the per-family product
	 * count: the ids are laid out against the widest family so they cannot move when a table grows.
	 */
	static final int LABEL_STRIDE = 8;
	/**
	 * The amount pills, in the top-right corner: a right-aligned block on the same two rows as the
	 * question and the help line, not a centred row under the items. Scaled from the reference
	 * screenshots (see MakeX's own comment for the raw numbers).
	 */
	static int QUANT_W = 28;
	static int QUANT_H = 31;
	static int QUANT_GAP = 9;
	static int QUANT_Y = 19;
	/** The clear space between the pill block and the region the question is given. */
	static final int TITLE_GAP = 6;
	static final int QUANTITY_TEXT_FIRST = 51990;
	/**
	 * The nominal width of one product's box, which is what every family gets except the one with
	 * more products than the click region can give that much room to. See {@link #slotWidth}.
	 */
	static int SLOT_W = 64;
	/** The clear space between neighbouring product boxes. Part of the pitch and part of the row's
	 * width, so every geometry assertion below has to account for it or it is checking a row that
	 * does not exist.
	 */
	static int SLOT_GAP = 6;
	/**
	 * How large a product's icon is drawn, as a fraction of the item's own {@code modelZoom}.
	 *
	 * <p>Restated because it is a size the player reported twice and because the direction is
	 * counter-intuitive: the zoom is the <em>distance</em> the model is pushed from the camera, so a
	 * fraction below 1 is what draws a bigger icon. See {@code iconSize}.
	 *
	 * <p>15/28 is 3/8 - the fraction that filled the box - scaled by 10/7, which is the draught the
	 * player asked for: every icon 30% smaller again than the one that filled its container.
	 */
	static int ICON_ZOOM_NUM = 15;
	static int ICON_ZOOM_DEN = 28;
	/**
	 * The two drop fractions, mirrored from the source like the layout numbers above and for the same
	 * reason: both are numbers the editor can reach, and a probe that kept its own opinion of them
	 * would be checking MakeX against a stale copy of itself.
	 *
	 * <p>{@code ICON_DROP_*} is the plain centring every product gets; {@code POTION_DROP_*} is the
	 * extra a bottle gets on top of it, which is the rule this was added for.
	 */
	static int ICON_DROP_NUM = 1;
	static int ICON_DROP_DEN = 1;
	static int POTION_DROP_NUM = 4;
	static int POTION_DROP_DEN = 3;
	/**
	 * Where the line drawn on the report falls, as a distance above the box's bottom edge in pixels.
	 *
	 * <p>Read off the screenshot the request came with: the box's frame is 82px of SLOT.png drawn
	 * one to one, and the red line lies seven pixels above the frame's bottom border. It is a mark
	 * drawn on the picture rather than art - the whole cache was searched for its colour, 230,27,27,
	 * and no sprite holds it - so it is the player's own statement of where a bottle should stand,
	 * which is exactly the thing a check on this should be pinned to.
	 */
	static final int REPORT_LINE_ABOVE_BOTTOM = 7;
	/**
	 * The dialog is drawn at (20, 20), so a child's panel position is its child position plus this.
	 * Restated here because a wrong offset would put every child 20px out and still look plausible in
	 * isolation — and because the x half of it is what the layout got wrong.
	 */
	static int DRAW_OFFSET = 20;
	static int DRAW_X = 20;

	/**
	 * The region the client will build a dialog's hitboxes in, restated from the three gates that
	 * decide it: {@code client.buildMenu} only builds them while the mouse is over the dialog at all,
	 * {@code client.buildInterfaceMenu} then requires the dialog's own rect — which the 20px draw
	 * offset puts at (20, 20) — and the chatbox's right-hand 16px is the message scrollbar's.
	 *
	 * <p>This is the assertion the layout family needs and cannot get from the panel alone: being
	 * drawn inside 519x142 is not the same as being clickable, and the "All" pill was inside neither.
	 */
	static final int CLICK_LEFT = 20;
	static final int CLICK_RIGHT = 490;
	static final int CLICK_TOP = 20;
	static final int CLICK_BOTTOM = HEIGHT;

	/** Where the pill block stops: {@link #CLICK_RIGHT}, not the panel edge. */
	static final int QUANT_RIGHT = CLICK_RIGHT;

	/** Where a child with no product is parked, restated from MakeX. Never on screen. */
	static final int OFF_PANEL = -WIDTH;

	/** One product's box width for a family with this many live products. */
	static int slotWidth(int slots) {
		if (slots <= 0) {
			return SLOT_W;
		}
		return Math.min(SLOT_W, (CLICK_RIGHT - CLICK_LEFT - (slots - 1) * SLOT_GAP) / slots);
	}

	/** The x of a product's box, the row of boxes and gaps centred in the click region. */
	static int slotX(int slots, int slot) {
		int span = CLICK_RIGHT - CLICK_LEFT;
		int width = slotWidth(slots);
		int used = slots * width + (slots - 1) * SLOT_GAP;
		return CLICK_LEFT + (span - used) / 2 + slot * (width + SLOT_GAP);
	}

	/** The width of the pill block, and so how much of the header the question does not get. */
	static int quantityLeft() {
		return QUANT_RIGHT - (AMOUNTS * QUANT_W + (AMOUNTS - 1) * QUANT_GAP);
	}

	/** The width the question and help line are centred in: everything left of the pills. */
	static int titleRight() {
		return quantityLeft() - TITLE_GAP;
	}

	private static int passed;
	private static int failed;

	/**
	 * The cache root this run resolved, kept so the model index can be opened on first use.
	 *
	 * <p>Models are read one at a time, so unlike the two archives in {@code main} - which are read
	 * whole and the files closed behind them - this stays open for the run and is never closed,
	 * which is what the client does with it too ({@code signlink.cache_idx}).
	 */
	private static String cacheRoot;
	private static Decompressor modelIndex;
	private static RandomAccessFile modelData;
	private static RandomAccessFile modelIdx;
	private static boolean modelsAllocated;
	private static final Set<Integer> loadedModels = new HashSet<Integer>();

	/**
	 * How many model slots to tell the client's model table it has.
	 *
	 * <p>Not a fact about this cache: the real count comes from the update server's version list, and
	 * what matters here is only that the array exists (nothing can be registered before it does) and
	 * is longer than every id the cache holds.
	 */
	static final int MODEL_SLOTS = 200000;

	/** Set by {@code --preview <dir>}: also draw each container to a PNG, for eyeballing. */
	private static boolean preview;
	private static String previewDir;

	public static void main(String[] args) throws Exception {
		preview = args.length >= 2 && "--preview".equals(args[0]);
		previewDir = preview ? args[1] : null;

		readLayoutFromSource();

		String cache = findCache();
		if (cache == null) {
			System.out.println("CACHE_NOT_FOUND");
			System.exit(1);
		}
		System.out.println("cache root: " + cache);

		byte[] archive;
		byte[] fonts;
		byte[] config;
		RandomAccessFile data = null;
		RandomAccessFile index = null;
		try {
			data = new RandomAccessFile(new File(cache, "main_file_cache.dat"), "r");
			index = new RandomAccessFile(new File(cache, "main_file_cache.idx0"), "r");
			Decompressor d = new Decompressor(data, index, 1);
			archive = d.decompress(3);
			fonts = d.decompress(1);
			// obj.dat / obj.idx, the archive client.java calls "config" - the same one its own
			// streamLoaderForName(2, ...) reads. Without it there are no items to measure an icon
			// against, only the stand-in ItemDef.forID answers with.
			config = d.decompress(2);
		} finally {
			if (data != null) data.close();
			if (index != null) index.close();
		}
		if (archive == null || fonts == null || config == null) {
			System.out.println("ARCHIVE_UNREADABLE");
			System.exit(1);
		}
		ItemDef.unpackConfig(new StreamLoader(config, "config"));
		cacheRoot = cache;

		StreamLoader title = new StreamLoader(fonts, "title");
		// The same four fonts, in the same order, that client.java hands to RSInterface.unpack.
		TextDrawingArea[] allFonts = {
				new TextDrawingArea(false, "p11_full", title),
				new TextDrawingArea(false, "p12_full", title),
				new TextDrawingArea(false, "b12_full", title),
				new TextDrawingArea(true, "q8_full", title),
		};

		// This is the call under test: unpack ends in Interfaces.loadInterfaces, which ends in
		// MakeX.build. If the build throws - a bad index, a null widget - it throws here.
		try {
			RSInterface.unpack(new StreamLoader(archive, "interface"), allFonts, null);
		} catch (Throwable t) {
			System.out.println();
			System.out.println("MakeX.build threw out of RSInterface.unpack: " + t);
			t.printStackTrace(System.out);
			System.out.println();
			System.out.println("MAKEX_FAIL");
			System.exit(1);
		}

		// Which child is which slot, captured once, while the row is still the widest layout the
		// build made: after the first recount the unused slots are parked off the panel and no
		// longer sortable, and everything below fills rows by slot.
		snapshotModels();
		System.out.println("model frames: " + describeModels());

		substitution();
		for (int family = 0; family < LEGACY.length; family++) {
			container(family);
		}
		quantityRow();
		clickability();
		names();
		iconSize();
		potionPlacement();
		background();
		// Last, because it leaves the rows at whatever count it last asserted.
		liveRow();

		if (preview) {
			MakeXPreview.render(cache);
		}

		System.out.println();
		System.out.println("passed: " + passed + "   failed: " + failed);
		System.out.println(failed == 0 ? "MAKEX_OK" : "MAKEX_FAIL");
		System.exit(failed == 0 ? 0 : 1);
	}

	/**
	 * Reads the layout numbers the editor can move out of MakeX.java into the constants above.
	 *
	 * <p>See the note over that block for why this is a read and not a restatement. Deliberately
	 * silent about names it does not mirror: {@code MakeX} carries plenty of other ints - ids, the
	 * click region - and only the ones this probe actually asserts against are taken here.
	 */
	private static void readLayoutFromSource() {
		File source = new File("src/ui/MakeX.java");
		if (!source.isFile()) {
			System.out.println("layout: " + source.getPath()
					+ " not found; using the numbers this probe carries.");
			return;
		}
		String text;
		try {
			text = new String(java.nio.file.Files.readAllBytes(source.toPath()), "UTF-8");
		} catch (Exception e) {
			System.out.println("layout: could not read " + source.getPath() + ": " + e);
			return;
		}
		java.util.regex.Matcher m = java.util.regex.Pattern
				.compile("private\\s+static\\s+(?:final\\s+)?int\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(-?\\d+)\\s*;")
				.matcher(text);
		StringBuilder changed = new StringBuilder();
		int read = 0;
		while (m.find()) {
			String name = m.group(1);
			int value = Integer.parseInt(m.group(2));
			switch (name) {
			case "TITLE_X": moved(changed, name, TITLE_X, value); TITLE_X = value; break;
			case "TITLE_Y": moved(changed, name, TITLE_Y, value); TITLE_Y = value; break;
			case "TITLE_H": moved(changed, name, TITLE_H, value); TITLE_H = value; break;
			case "HELP_Y": moved(changed, name, HELP_Y, value); HELP_Y = value; break;
			case "HELP_H": moved(changed, name, HELP_H, value); HELP_H = value; break;
			case "QUANT_W": moved(changed, name, QUANT_W, value); QUANT_W = value; break;
			case "QUANT_H": moved(changed, name, QUANT_H, value); QUANT_H = value; break;
			case "QUANT_GAP": moved(changed, name, QUANT_GAP, value); QUANT_GAP = value; break;
			case "QUANT_Y": moved(changed, name, QUANT_Y, value); QUANT_Y = value; break;
			case "SLOT_W": moved(changed, name, SLOT_W, value); SLOT_W = value; break;
			case "SLOT_H": moved(changed, name, SLOT_H, value); SLOT_H = value; break;
			case "SLOT_TOP": moved(changed, name, SLOT_TOP, value); SLOT_TOP = value; break;
			case "SLOT_GAP": moved(changed, name, SLOT_GAP, value); SLOT_GAP = value; break;
			case "LABEL_H": moved(changed, name, LABEL_H, value); LABEL_H = value; break;
			case "ICON_ZOOM_NUM": moved(changed, name, ICON_ZOOM_NUM, value); ICON_ZOOM_NUM = value; break;
			case "ICON_ZOOM_DEN": moved(changed, name, ICON_ZOOM_DEN, value); ICON_ZOOM_DEN = value; break;
			case "ICON_DROP_NUM": moved(changed, name, ICON_DROP_NUM, value); ICON_DROP_NUM = value; break;
			case "ICON_DROP_DEN": moved(changed, name, ICON_DROP_DEN, value); ICON_DROP_DEN = value; break;
			case "POTION_DROP_NUM": moved(changed, name, POTION_DROP_NUM, value); POTION_DROP_NUM = value; break;
			case "POTION_DROP_DEN": moved(changed, name, POTION_DROP_DEN, value); POTION_DROP_DEN = value; break;
			default: continue;
			}
			read++;
		}
		if (changed.length() > 0) {
			System.out.println("layout: src/ui/MakeX.java has moved since this probe was last written - "
					+ changed);
		}
		System.out.println("layout: " + read + " numbers read from src/ui/MakeX.java");
	}

	/** Records a mirrored value the source no longer agrees with, for the one line above. */
	private static void moved(StringBuilder log, String name, int was, int now) {
		if (was == now) {
			return;
		}
		if (log.length() > 0) {
			log.append(", ");
		}
		log.append(name).append(' ').append(was).append(" -> ").append(now);
	}

	/**
	 * The type-4 header line the layout put at {@code (TITLE_X, y)}: the question at {@link #TITLE_Y}
	 * and the help line at {@link #HELP_Y}.
	 *
	 * <p>Found by where it was placed rather than by its id, for the same reason the constants above
	 * are read out of the source: the id table is the layout's business and a probe that restated it
	 * would be checking the layout against a stale copy of itself.
	 */
	private static int headerLineAt(int family, int y) {
		RSInterface menu = RSInterface.interfaceCache[MakeX.CONTAINER_FIRST + family];
		if (menu == null || menu.children == null) {
			return -1;
		}
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface child = RSInterface.interfaceCache[menu.children[i]];
			if (child != null && child.type == 4 && menu.childX[i] + DRAW_X == TITLE_X
					&& menu.childY[i] + DRAW_OFFSET == y) {
				return menu.children[i];
			}
		}
		return -1;
	}

	/**
	 * Where a header line's text actually stops, in the dialog's own coordinates.
	 *
	 * <p>Read off the widget the layout put the text in - its message and its font - rather than
	 * guessed from the width the widget was given, because that width is a layout allowance (the
	 * whole panel less the indent) and says nothing about where the letters stop. Falls back to the
	 * panel's right edge when the widget has no font or no text, which is the conservative case.
	 */
	private static int drawnTextRight(int widgetId, int left, String what) {
		RSInterface w = widgetId < 0 ? null : RSInterface.interfaceCache[widgetId];
		if (w == null || w.textDrawingAreas == null || w.message == null) {
			System.out.println("    (" + what + ": no widget to measure, using the panel's right edge)");
			return WIDTH;
		}
		return left + w.textDrawingAreas.getTextWidth(w.message);
	}

	// ------------------------------------------------------------- substitution

	private static void substitution() {
		System.out.println();
		System.out.println("opening a chatbox (packet 164)");
		boolean mapped = true;
		for (int family = 0; family < LEGACY.length; family++) {
			mapped &= MakeX.substitute(LEGACY[family]) == MakeX.CONTAINER_FIRST + family;
		}
		check("substitute: each of the five opens its own container", mapped);
		check("substitute: an unrelated interface is handed back untouched",
				MakeX.substitute(3213) == 3213);
		check("substitute: a quantity button id is not mistaken for a menu",
				MakeX.substitute(MakeX.QUANTITY_FIRST) == MakeX.QUANTITY_FIRST);

		// The transparent-chatbox exception is asked for by id, and it has to be exactly these five:
		// ChatArea holds the background solid while owns(backDialogID) is true, so an id that
		// answered here by accident would be a dialog that silently lost the player's preference.
		boolean owns = true;
		for (int family = 0; family < LEGACY.length; family++) {
			owns &= MakeX.owns(MakeX.CONTAINER_FIRST + family);
		}
		check("chatbox background: the solid-background exception names exactly the five containers",
				owns && !MakeX.owns(LEGACY[0]) && !MakeX.owns(5292) && !MakeX.owns(-1));

		// A filled row before anything is read off the armed hitboxes, so that "the first product's
		// widget" is a widget and not an index off the end of an empty row.
		allFilled(0);
		allFilled(4);

		// The amount is the player's habit, not a property of the menu: a player who armed All and
		// walked away must find All still armed when the next menu opens. Asserted both ways round,
		// because a build that re-armed make-one would pass a check that only looked at the index
		// and a build that forgot to re-arm at all would pass one that only looked at the widgets.
		MakeX.select(ALL_INDEX);
		int allId = liveIds(0)[0];
		MakeX.substitute(8938);
		check("substitute: reopening a menu keeps the amount last used armed",
				armed() == ALL_INDEX && liveIds(0)[0] == allId);

		// And it is shared: arming "make 10" at a furnace leaves it armed at a loom.
		MakeX.select(2);
		int smeltId = liveIds(4)[0];
		MakeX.substitute(8938);
		check("substitute: the remembered amount carries across families",
				armed() == 2 && liveIds(4)[0] == smeltId);

		// Opening re-lays the row out from what the server has already filled rather than from the
		// tables: a row closed while it had five products in it must not open five boxes for a menu
		// that now has one. Asserted through the widgets, since that is what the player sees.
		fill(0, SLOTS[0]);
		allFilled(0);
		MakeX.substitute(8938);
		check("substitute: a full row keeps all " + SLOTS[0] + " boxes",
				visibleSlots(0) == SLOTS[0]);
		fill(0, 1);
		MakeX.substitute(8938);
		check("substitute: a one-product row opens one box, not the five it had last time",
				visibleSlots(0) == 1);

		// Every amount is a plain one: X went end to end with the pill, so no entry has a second
		// meaning and no entry can ask for a number the client never typed.
		boolean plain = true;
		for (int amount = 0; amount < AMOUNTS; amount++) {
			MakeX.select(amount);
			plain &= armed() == amount;
		}
		check("quantity: all " + AMOUNTS + " entries arm a fixed amount and none prompts", plain);
		MakeX.select(0);

		// A product click has to be exactly one widget, wherever on the box the player aims: two
		// widgets under one point is how a row whose model and hitbox disagree still "works" for
		// one product and clicks the wrong one for its neighbour.
		boolean oneLive = true;
		for (int family = 0; family < LEGACY.length; family++) {
			int slots = SLOTS[family];
			allFilled(family);
			for (int slot = 0; slot < slots; slot++) {
				int cx = slotX(slots, slot) + slotWidth(slots) / 2;
				int cy = SLOT_TOP + SLOT_H / 2;
				oneLive &= hitsAt(family, cx, cy) == 1;
			}
		}
		check("products: every box centre offers exactly one widget, not none and not two", oneLive);
		MakeX.select(0);
		allFilled(0);
	}

	// -------------------------------------------------------------- live row

	/**
	 * The row the player actually gets: only the slots the server filled, centred, spaced, and
	 * clickable nowhere else.
	 *
	 * <p><b>Why this is the section that matters.</b> Every other check here asks about the layout
	 * the tables describe. This one asks about the layout after {@code MakeX.recount} has been told
	 * how many products there are, which is the state every menu is really drawn in - and the state
	 * the reported bug lived in: five boxes when the player had one uncut gem, four of them dead,
	 * and a live click on each of them that made nothing.
	 *
	 * <p><b>How the server is simulated.</b> A model frame with no product is one whose
	 * {@code anInt233} is zero - that is exactly what packet 246 writes for item {@code -1} - so the
	 * row is filled by writing that field and calling the same {@code onModelUpdate} the packet
	 * handler calls. The model frames are found by shape (a non-sprite widget on the icon row)
	 * rather than by restating MakeX's table, so a table that stopped matching the cache shows up
	 * here as a family with the wrong number of models.
	 */
	private static void liveRow() {
		System.out.println();
		System.out.println("the live row: only the products the server filled are drawn");
		for (int family = 0; family < LEGACY.length; family++) {
			int slots = SLOTS[family];
			int[] models = modelIds[family];
			if (models.length != slots) {
				check(FAMILY_NAMES[family] + ": the container has one item model per slot ("
						+ models.length + "/" + slots + ")", false);
				continue;
			}
			int[] counts = rowCounts(slots);
			for (int i = 0; i < counts.length; i++) {
				fill(family, counts[i]);
				MakeX.onModelUpdate(models[0]);
				rowIsRight(family, models, counts[i],
						FAMILY_NAMES[family] + " (" + counts[i] + " of " + slots + "): ");
			}
			allFilled(family);
		}
		// And the state a menu opens in before any model frame arrives: no boxes and no click, rather
		// than stale boxes from the menu before it.
		fill(0, 0);
		MakeX.onModelUpdate(modelIds[0][0]);
		check("a row with no model frames yet draws no boxes and reports no click",
				visibleSlots(0) == 0 && sortedHitboxes(0).length == 0);
		allFilled(0);
	}

	/** The counts a row is checked at: the interesting sizes for this family, widest included. */
	private static int[] rowCounts(int slots) {
		int[] wanted = { 1, 3, 5, 8 };
		int[] out = new int[wanted.length + 1];
		int n = 0;
		for (int i = 0; i < wanted.length; i++) {
			if (wanted[i] < slots) {
				out[n++] = wanted[i];
			}
		}
		out[n++] = slots;
		int[] trimmed = new int[n];
		System.arraycopy(out, 0, trimmed, 0, n);
		return trimmed;
	}

	/** Everything a row of {@code count} products has to be: drawn, spaced, centred and clickable. */
	private static void rowIsRight(int family, int[] models, int count, String what) {
		int width = slotWidth(count);
		int[] frames = visibleSlotFrames(family);
		int[] labels = visibleLabels(family);
		int[] live = sortedHitboxes(family);
		check(what + "exactly " + count + " framed boxes are drawn", frames.length == count);
		// The name widgets are not children, so a row of names cannot sit across the icons: see
		// MakeX's own note where it builds the labels.
		check(what + "no product label is drawn over the icons", labels.length == 0);
		check(what + "exactly " + count + " products have a live hitbox, and the empty slots none",
				live.length == count);

		boolean sameBox = frames.length == count && live.length == count;
		boolean shownRow = true;
		int rowLeft = Integer.MAX_VALUE;
		int rowRight = Integer.MIN_VALUE;
		for (int slot = 0; slot < count; slot++) {
			int wantX = slotX(count, slot);
			int frameId = MakeX.slotFrameId(family, slot);
			int frameX = drawnXOf(MakeX.CONTAINER_FIRST + family, frameId);
			int modelX = drawnXOf(MakeX.CONTAINER_FIRST + family, models[slot]);
			int liveX = live.length == count ? drawnXOf(MakeX.CONTAINER_FIRST + family, live[slot])
					: Integer.MIN_VALUE;
			// The frame, the item and its hitbox have to be the same box: the model is what the
			// player aims at and the hitbox is what receives the click, so a row where they differ
			// is a row that reads as "the button does not work".
			sameBox &= frameX == wantX && modelX == wantX && liveX == wantX
					&& rsWidth(frameId) == width;
			// Every drawn box has to be inside the region the client builds hitboxes in, and the
			// gap between neighbours has to be the layout's own.
			shownRow &= frameX >= CLICK_LEFT && frameX + width <= CLICK_RIGHT;
			if (slot > 0) {
				int previous = slotX(count, slot - 1);
				shownRow &= frameX - (previous + width) == SLOT_GAP;
			}
			rowLeft = Math.min(rowLeft, frameX);
			rowRight = Math.max(rowRight, frameX + width);
		}
		check(what + "each product's frame, item model and armed hitbox are the same box", sameBox);
		check(what + "the row is inside the " + CLICK_LEFT + ".." + CLICK_RIGHT
				+ " region the client hit-tests in", shownRow);
		check(what + "the row is evenly spaced at " + width + "+" + SLOT_GAP, shownRow);
		// Centred means the space left of the row equals the space right of it, to the pixel the
		// integer division is allowed to lose.
		check(what + "the row is centred (" + rowLeft + ".." + rowRight + ")",
				count > 0 && Math.abs((rowLeft - CLICK_LEFT) - (CLICK_RIGHT - rowRight)) <= 1);

		// And nothing is left live where there is no product: the slots past the count are parked
		// off the panel with no size at all, rather than sitting on an invisible button.
		boolean parked = true;
		for (int slot = count; slot < SLOTS[family]; slot++) {
			int frameId = MakeX.slotFrameId(family, slot);
			// OFF_PANEL is a CHILD coordinate here: the widget is moved to it, not drawn at it.
			parked &= rawXOf(frameId, MakeX.CONTAINER_FIRST + family) == OFF_PANEL
					&& rsWidth(frameId) == 0 && rsHeight(frameId) == 0
					&& rsWidth(models[slot]) == 0;
		}
		check(what + "the unused slots are parked off the panel with no size", parked);
	}

	// ---------------------------------------------------------------- container

	private static void container(int family) {
		int containerId = MakeX.CONTAINER_FIRST + family;
		int legacyId = LEGACY[family];
		int slots = SLOTS[family];
		System.out.println();
		System.out.println(FAMILY_NAMES[family] + ": legacy " + legacyId + " -> container " + containerId);

		RSInterface legacy = RSInterface.interfaceCache[legacyId];
		RSInterface menu = RSInterface.interfaceCache[containerId];
		if (legacy == null || menu == null) {
			check(FAMILY_NAMES[family] + ": both interfaces exist", false);
			return;
		}
		check(FAMILY_NAMES[family] + ": the container is " + menu.width + "x" + menu.height,
				menu.width == WIDTH && menu.height == HEIGHT);
		// Each product contributes a model, its four make buttons and a frame; the header
		// contributes the question and the help line, and the amount row a pill and a caption per
		// entry. The name widgets are deliberately NOT children, which is why the seven-per-product
		// this used to count is six. The row is built to exactly that count, so a carried-over
		// ornament or a table that stopped naming a widget both land here.
		int expected = 6 * slots + 2 + 2 * AMOUNTS;
		String left = untouched(legacy, menu);
		check(FAMILY_NAMES[family] + ": the row is exactly the layout, nothing carried over "
				+ "untouched", menu.children.length == expected);

		int unresolved = 0;
		int live = 0;
		for (int i = 0; i < menu.children.length; i++) {
			int id = menu.children[i];
			if (id == 0 || RSInterface.interfaceCache[id] == null) {
				unresolved++;
				continue;
			}
			if (isHitbox(menu, i, slots)) {
				live++;
			}
		}
		System.out.println("  row: " + menu.children.length + " children (expected "
				+ expected + ", legacy had " + legacy.children.length + ", unresolved "
				+ unresolved + ")" + (left.length() == 0 ? ", nothing left out" : ", left out: " + left));
		check(FAMILY_NAMES[family] + ": every child id resolves to a widget", unresolved == 0);
		check(FAMILY_NAMES[family] + ": exactly one live hitbox for each of its " + slots
				+ " products", live == slots);

		// THE COUPLING. A product click must report a widget id the server already knows, and the
		// server knows these because they are the legacy interface's own children. A mistyped table
		// is invisible everywhere else; it stops being invisible here.
		int[] ids = liveIds(family);
		boolean allLegacy = ids.length == slots;
		for (int i = 0; i < ids.length; i++) {
			allLegacy &= isChildOf(legacy, ids[i]);
		}
		check(FAMILY_NAMES[family] + ": each armed hitbox is an id the legacy interface already had",
				allLegacy);

		// The row is longer than the server's table on purpose: five entries, four buttons. Each
		// entry must ask for a DIFFERENT one of the four, and the union must be all four - if two
		// entries aliased, a pill would look distinct and ask the server for the same thing.
		Set<Integer> distinct = new HashSet<Integer>();
		for (int amount = 0; amount < AMOUNTS; amount++) {
			MakeX.select(amount);
			int[] armed = liveIds(family);
			if (armed.length != slots) {
				check(FAMILY_NAMES[family] + ": entry " + amount + " arms " + slots + " hitboxes",
						false);
				continue;
			}
			for (int i = 0; i < armed.length; i++) {
				distinct.add(Integer.valueOf(armed[i]));
			}
		}
		check(FAMILY_NAMES[family] + ": the row's " + AMOUNTS + " entries ask the server for its "
				+ SERVER_AMOUNTS + " amounts (" + (slots * SERVER_AMOUNTS) + " distinct widgets)",
				distinct.size() == slots * SERVER_AMOUNTS);

		// And the models must be one per product, filling its slot: the model widget IS the icon's
		// box, so it is the whole slot tall rather than a sub-box of it, and the renderer centres
		// the icon on that box.
		int box = slotWidth(slots);
		int models = 0;
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface child = RSInterface.interfaceCache[menu.children[i]];
			if (child != null && child.type != 5 && child.type != 4 && child.width == box
					&& child.height == SLOT_H && menu.childY[i] + DRAW_OFFSET == SLOT_TOP) {
				models++;
			}
		}
		check(FAMILY_NAMES[family] + ": " + slots + " product models fill their " + box + "x" + SLOT_H
				+ " slots", models == slots);

		// THE ONE THAT WAS BROKEN IN PLAY. A product is three widgets - the frame, the item model and
		// the armed make button - and they have to be at the same x, because the model is what the
		// player aims at and the make button is what receives the click. The models used to be left
		// at the LEGACY interface's own child coordinates while the buttons moved to the layout's
		// slot positions, which put the picture of an item and its hitbox in different places: the
		// click landed on empty space and the row read as "the button does not work".
		int[] armedIds = liveIds(family);
		boolean oneBox = armedIds.length == slots;
		for (int slot = 0; slot < slots; slot++) {
			int frameX = drawnXOf(containerId, MakeX.slotFrameId(family, slot));
			int modelAt = modelIndexAt(menu, frameX, slots);
			int modelX = modelAt >= 0 ? menu.childX[modelAt] + DRAW_X : Integer.MIN_VALUE;
			int liveX = drawnXOf(containerId, armedIds[slot]);
			oneBox &= frameX == slotX(slots, slot) && modelX == frameX && liveX == frameX;
		}
		check(FAMILY_NAMES[family] + ": each product's frame, item model and armed hitbox are the "
				+ "same box, so the item drawn is the item clicked", oneBox);

		// One framed slot per product, drawn BEFORE the model it frames. Order matters and is easy
		// to lose: a frame listed after its model would be painted over the item.
		int frames = 0;
		boolean behind = true;
		boolean artFits = true;
		for (int slot = 0; slot < slots; slot++) {
			int frameId = MakeX.slotFrameId(family, slot);
			RSInterface frame = RSInterface.interfaceCache[frameId];
			if (frame != null && frame.type == 5 && frame.width == box && frame.height == SLOT_H) {
				frames++;
			}
			// The art has to be the width of the box: a type-5 widget is drawn at its sprite's own
			// size, so a 64-wide frame on a 58-wide box is 6 pixels painted over its neighbour.
			artFits &= frame != null && frame.sprite1 != null && frame.sprite1.myWidth == box;
			int frameAt = childIndexOf(menu, frameId);
			// The model is found by sharing the frame's own x rather than by restating the slot
			// arithmetic here: a wrong slot pitch would then show up as a missing model, not as a
			// second copy of the same mistake.
			int modelAt = modelIndexAt(menu, drawnXOf(containerId, frameId), slots);
			behind &= frameAt >= 0 && modelAt >= 0 && frameAt < modelAt;
		}
		check(FAMILY_NAMES[family] + ": " + slots + " slot frames are " + box + "x" + SLOT_H,
				frames == slots);
		check(FAMILY_NAMES[family] + ": the frame art is the box's own " + box + "px, not the "
				+ "nominal " + SLOT_W, artFits);
		check(FAMILY_NAMES[family] + ": each slot frame is drawn behind its item model", behind);

		// The panel is a hard bound - the chat filter row is drawn at y 142 and would sit on top of
		// anything past it - and it is not the interesting one, since it is checked in the panel's
		// coordinates rather than the child's. What the layout is really held to is that the panel
		// and the child space are the SAME space: every x this class places is offset by the dialog's
		// own draw offset, so a child x read on its own is 20 pixels left of where it lands, which is
		// how a pill came to be drawn five pixels past the right edge with this check passing.
		int outside = 0;
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface child = RSInterface.interfaceCache[menu.children[i]];
			if (child == null || child.width <= 0 || child.height <= 0) {
				continue;
			}
			boolean fits = menu.childX[i] + DRAW_X >= 0
					&& menu.childX[i] + DRAW_X + child.width <= WIDTH
					&& menu.childY[i] + DRAW_OFFSET >= 0
					&& menu.childY[i] + DRAW_OFFSET + child.height <= HEIGHT;
			if (!fits) {
				outside++;
				System.out.println("    off-panel: widget " + menu.children[i] + " at "
						+ (menu.childX[i] + DRAW_X) + "," + (menu.childY[i] + DRAW_OFFSET)
						+ " size " + child.width + "x" + child.height);
			}
		}
		check(FAMILY_NAMES[family] + ": everything the layout placed fits in the " + WIDTH + "x"
				+ HEIGHT + " dialog, clear of the chat filter row", outside == 0);

		// No product label is drawn: the name widgets exist so a name has somewhere to land, but they
		// are not children of the container, so a row of names cannot sit across the icons.
		check(FAMILY_NAMES[family] + ": no product label is drawn over the icons",
				labelCount(family) == 0 && visibleLabels(family).length == 0);

		// The question is a left-aligned caption in the panel's top-left corner: not centred, and
		// starting clear of the pill block rather than under it. This is the corner the reported
		// "up five and right ten" moved it to.
		int questions = 0;
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface child = RSInterface.interfaceCache[menu.children[i]];
			if (child != null && child.type == 4 && child.width == WIDTH - TITLE_X && !child.centerText
					&& menu.childX[i] + DRAW_X == TITLE_X && menu.childY[i] + DRAW_OFFSET == TITLE_Y) {
				questions++;
			}
		}
		check(FAMILY_NAMES[family] + ": the question is left-aligned at " + TITLE_X + "," + TITLE_Y,
				questions == 1);
		check(FAMILY_NAMES[family] + ": the question starts clear of the pill block at "
				+ quantityLeft(), TITLE_X < quantityLeft());
		// The help line shares the question's column, which is what keeps the two reading as one
		// caption rather than as two things that happen to be near each other.
		int helps = 0;
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface child = RSInterface.interfaceCache[menu.children[i]];
			if (child != null && child.type == 4 && menu.childX[i] + DRAW_X == TITLE_X
					&& menu.childY[i] + DRAW_OFFSET == HELP_Y) {
				helps++;
			}
		}
		check(FAMILY_NAMES[family] + ": the help line is under the question at y " + HELP_Y,
				helps == 1);

		MakeX.select(0);
	}

	/**
	 * The ids of the cache interface's children the row did not take: what the layout dropped.
	 *
	 * <p>Printed by the caller and asserted empty. It has to be empty - a child left where the cache
	 * put it sits at the OLD interface's coordinates, which after this rearrangement is inside the
	 * header or across the pill block - and the ornaments that used to legitimate this list are
	 * dropped now. Anything else in here is a table that stopped naming a widget.
	 *
	 * <p>Looked up by id rather than walked index-for-index: the slot frames and the labels are
	 * inserted around the cache's children, so an index walk would match nothing and report
	 * "(none)" no matter what the tables said.
	 */
	private static String untouched(RSInterface legacy, RSInterface menu) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < legacy.children.length; i++) {
			if (childIndexOf(menu, legacy.children[i]) < 0) {
				sb.append(legacy.children[i]).append(' ');
			}
		}
		return sb.toString();
	}

	// ------------------------------------------------------------- quantity row

	private static void quantityRow() {
		System.out.println();
		System.out.println("the 1 / 5 / 10 / All row");

		boolean buttons = true;
		boolean art = true;
		for (int amount = 0; amount < AMOUNTS; amount++) {
			RSInterface button = RSInterface.interfaceCache[MakeX.QUANTITY_FIRST + amount];
			if (button == null) {
				buttons = false;
				art = false;
				continue;
			}
			buttons &= button.type == 5 && button.atActionType == 1
					&& button.width == QUANT_W && button.height == QUANT_H
					&& button.tooltip != null && button.tooltip.length() > 0;
			art &= button.sprite1 != null && button.sprite2 != null
					&& button.sprite1.myWidth == QUANT_W && button.sprite1.myHeight == QUANT_H;
		}
		check("quantity: all " + AMOUNTS + " are " + QUANT_W + "x" + QUANT_H
				+ " type-5 buttons with an action and a tooltip", buttons);
		check("quantity: all " + AMOUNTS + " carry both a " + QUANT_W + "x" + QUANT_H
				+ " idle and armed sprite", art);

		// Order matters: 1, 5, 10, All left to right, one even gap, flush to the right edge of
		// the click region rather than the panel's own edge, which the scrollbar occupies.
		int[] xs = new int[AMOUNTS];
		for (int amount = 0; amount < AMOUNTS; amount++) {
			xs[amount] = drawnXOf(MakeX.CONTAINER_FIRST, MakeX.QUANTITY_FIRST + amount);
		}
		int gap = xs[1] - xs[0];
		boolean even = true;
		for (int amount = 1; amount < AMOUNTS; amount++) {
			even &= xs[amount] == xs[0] + amount * gap;
		}
		check("quantity: left to right with one even gap of " + (QUANT_W + QUANT_GAP),
				even && gap == QUANT_W + QUANT_GAP);
		check("quantity: the row is flush to the right edge of the click region, at "
				+ QUANT_RIGHT, xs[0] == quantityLeft() && xs[AMOUNTS - 1] + QUANT_W == QUANT_RIGHT);

		// And every pill is inside the region the client will actually build hitboxes in. This is
		// the assertion that caught "All" being drawn past the panel: it was inside neither the
		// panel nor the region, so the one pill a player most wants was the one that could not be
		// clicked. Drawn position, not child position, or the 20px draw offset hides the failure.
		boolean inside = xs[0] >= CLICK_LEFT && xs[AMOUNTS - 1] + QUANT_W <= CLICK_RIGHT;
		for (int amount = 0; amount < AMOUNTS; amount++) {
			inside &= xs[amount] >= CLICK_LEFT && xs[amount] + QUANT_W <= CLICK_RIGHT;
		}
		check("quantity: every pill is inside the " + CLICK_LEFT + ".." + CLICK_RIGHT
				+ " region the client builds hitboxes in (" + xs[0] + ".."
				+ (xs[AMOUNTS - 1] + QUANT_W) + ")", inside);

		// The row is on the HEADER rows, beside the question, not under the items. This is the
		// difference from the obvious layout and the easiest thing to regress back to.
		int pillTop = drawnYOf(MakeX.CONTAINER_FIRST, MakeX.QUANTITY_FIRST);
		check("quantity: the row is on the header rows (" + pillTop + ".." + (pillTop + QUANT_H)
				+ "), above the first slot at " + SLOT_TOP,
				pillTop == QUANT_Y && pillTop + QUANT_H <= SLOT_TOP);

		// And the header's own two lines do not run into it. This used to be asserted as "the pills
		// share the question's and the help line's rows", which was a proxy for this fact and a bad
		// one: it held the help line's y below the pill block's bottom edge, so the moment the layout
		// was tuned - the help line moved up to y 32, which the player did with the editor - it
		// failed while the two were 30 pixels apart horizontally. What is actually required is that
		// the text and the pill block do not share PIXELS, so the text is measured.
		int questionRight = drawnTextRight(headerLineAt(0, TITLE_Y), TITLE_X, "the question");
		int helpRight = drawnTextRight(headerLineAt(0, HELP_Y), TITLE_X, "the help line");
		check("quantity: the question's text ends at " + questionRight
				+ ", left of the pill block at " + quantityLeft(), questionRight <= quantityLeft());
		check("quantity: the help line's text ends at " + helpRight
				+ ", left of the pill block at " + quantityLeft(), helpRight <= quantityLeft());

		// The pill block is a right-aligned block, so its left edge is the right edge less the four
		// pills and the gaps between them - the number the question's column is measured against.
		check("quantity: the pill block spans " + quantityLeft() + ".." + QUANT_RIGHT,
				quantityLeft() + (AMOUNTS * QUANT_W + (AMOUNTS - 1) * QUANT_GAP) == QUANT_RIGHT
						&& quantityLeft() == QUANT_RIGHT - (AMOUNTS * QUANT_W
								+ (AMOUNTS - 1) * QUANT_GAP));
		check("quantity: the question's column starts well left of the pills",
				TITLE_X + 20 < quantityLeft());

		// The four captions sit on the four buttons, on the text row of each button.
		boolean captions = true;
		for (int amount = 0; amount < AMOUNTS; amount++) {
			RSInterface text = RSInterface.interfaceCache[QUANTITY_TEXT_FIRST + amount];
			captions &= text != null && text.type == 4
					&& drawnXOf(MakeX.CONTAINER_FIRST, QUANTITY_TEXT_FIRST + amount) == xs[amount];
		}
		check("quantity: each caption is laid over its own button", captions);

		// And they read 1, 5, 10, All. Pinned because the row's whole point is which amounts it
		// offers, and a caption is the only place that is visible. There is no X: it is gone end to
		// end, so the four captions are also the proof that nothing in the row can ask for a number
		// the client never typed.
		String[] expected = { "1", "5", "10", "All" };
		boolean labels = true;
		for (int amount = 0; amount < AMOUNTS; amount++) {
			RSInterface text = RSInterface.interfaceCache[QUANTITY_TEXT_FIRST + amount];
			labels &= text != null && expected[amount].equals(text.message);
		}
		check("quantity: the captions read 1 / 5 / 10 / All, with no X", labels);
		MakeX.select(0);

		// Arming must move the armed sprite onto exactly one button and take it off the rest.
		MakeX.select(1);
		boolean swapped = true;
		for (int amount = 0; amount < AMOUNTS; amount++) {
			RSInterface button = RSInterface.interfaceCache[MakeX.QUANTITY_FIRST + amount];
			swapped &= (amount == 1) == (button.sprite1 == armedSprite());
		}
		check("quantity: arming one moves the armed sprite onto it and off the others", swapped);
		check("quantity: the idle pill is not the armed pill",
				RSInterface.interfaceCache[MakeX.QUANTITY_FIRST].sprite1 != armedSprite());
		check("quantity: the armed pill is armed on the second click of the same button too",
				armedPillIsDistinct());

		// The captions are black on the idle pill and white on the armed one, so the pills have to
		// be light and dark respectively or one of the two states is unreadable. Pinned by
		// luminance: nothing else in this class stops the two sprites being swapped or both dark.
		Sprite idle = RSInterface.interfaceCache[MakeX.QUANTITY_FIRST].sprite1;
		Sprite armedPill = armedSprite();
		int idleCentre = centrePixel(idle);
		int armedCentre = centrePixel(armedPill);
		System.out.println("  idle centre 0x" + Integer.toHexString(idleCentre & 0xffffff)
				+ ", armed centre 0x" + Integer.toHexString(armedCentre & 0xffffff));
		check("quantity: the idle pill is light enough for its black caption",
				luminance(idleCentre) > 0x80);
		check("quantity: the armed pill is dark enough for its white caption",
				luminance(armedCentre) < 0x80);
		MakeX.select(0);
	}

	// ------------------------------------------------------------- clickability

	/**
	 * The three gates a product click has to pass, asked at the centre of every product's box.
	 *
	 * <p><b>Why this is its own section.</b> Everything above checks what the layout DRAWS. Drawn is
	 * not clickable: {@code client.buildMenu} only builds an open dialog's hitboxes at all while the
	 * mouse is inside {@code mouseOverChatDialog}, {@code buildInterfaceMenu} then requires the
	 * dialog's own rect, and the chatbox's right-hand strip belongs to the message scrollbar. A
	 * widget can be drawn inside the panel and outside all three, and then it is decoration - the
	 * "All" pill was exactly that, and a creation row whose boxes hang past the scrollbar would be
	 * the same failure with five symptoms.
	 *
	 * <p>The last check closes the loop the game closes: the id the client would offer at that point
	 * has to be that product's own armed widget, read off the row. A layout that offers the wrong
	 * product's widget still makes something, just not what was clicked.
	 */
	private static void clickability() {
		System.out.println();
		System.out.println("clicking a product");
		for (int family = 0; family < LEGACY.length; family++) {
			int slots = SLOTS[family];
			int box = slotWidth(slots);
			// Every check below is about a full row, so the row is filled before it is read: the
			// live row's own empty-slot behaviour is liveRow's job, not this one's.
			allFilled(family);
			int[] live = sortedHitboxes(family);
			boolean inside = true;
			boolean oneWidget = true;
			boolean theRightOne = live.length == slots;
			for (int slot = 0; slot < slots; slot++) {
				// The point a player would click: the middle of the box, which is on the icon.
				int cx = slotX(slots, slot) + box / 2;
				int cy = SLOT_TOP + SLOT_H / 2;
				inside &= cx >= CLICK_LEFT && cx < CLICK_RIGHT
						&& cy >= CLICK_TOP && cy < CLICK_BOTTOM;
				oneWidget &= hitsAt(family, cx, cy) == 1;
				theRightOne &= live.length == slots && hitIdAt(family, cx, cy) == live[slot];
			}
			String what = FAMILY_NAMES[family] + " (" + slots + " products, " + box + " wide): ";
			check(what + "every product's box centre is inside the " + CLICK_LEFT + ".." + CLICK_RIGHT
					+ " x " + CLICK_TOP + ".." + CLICK_BOTTOM + " region the client hit-tests in",
					inside);
			check(what + "a click there offers exactly one widget, not none and not two",
					oneWidget);
			check(what + "the id offered there is that product's own armed widget", theRightOne);
		}
		MakeX.select(0);
		allFilled(0);
	}
	private static int centrePixel(Sprite sprite) {
		return sprite.myPixels[(sprite.myHeight / 2) * sprite.myWidth + sprite.myWidth / 2];
	}

	private static int luminance(int pixel) {
		int r = (pixel >> 16) & 0xff;
		int g = (pixel >> 8) & 0xff;
		int b = pixel & 0xff;
		return (r + g + b) / 3;
	}

	/** Selecting the already-armed amount must not leave two buttons looking armed. */
	private static boolean armedPillIsDistinct() {
		MakeX.select(1);
		int armed = 0;
		for (int amount = 0; amount < AMOUNTS; amount++) {
			if (RSInterface.interfaceCache[MakeX.QUANTITY_FIRST + amount].sprite1 == armedSprite()) {
				armed++;
			}
		}
		return armed == 1;
	}

	private static Sprite armedSprite() {
		return RSInterface.interfaceCache[MakeX.QUANTITY_FIRST].sprite2;
	}

	// ------------------------------------------------------------- name routing

	private static void names() {
		System.out.println();
		System.out.println("product names");

		// Creation puts each name in the make-one button of the product it belongs to, padded down
		// the hitbox with line breaks whose only job is to clear the item model.
		String[] names = { "Dagger", "Sword", "Longsword", "Scimitar", "2h Sword" };
		boolean routed = true;
		for (int slot = 0; slot < names.length; slot++) {
			routed &= MakeX.absorbName(8949 + slot * 4, names[slot] + "\\n\\n\\n");
		}
		check("names: the creation frames are recognised", routed);
		check("names: a frame that is not a product's name is left alone",
				!MakeX.absorbName(8941, "noise") && !MakeX.absorbName(12345, "noise"));
		check("names: a null text is not a name", !MakeX.absorbName(8949, null));

		String[] labels = labels(0);
		boolean placed = labels.length == names.length;
		for (int slot = 0; slot < labels.length && slot < names.length; slot++) {
			placed &= names[slot].equals(labels[slot]);
		}
		check("names: each name lands on its own product's label, padding stripped", placed);
		if (!placed) {
			for (int slot = 0; slot < labels.length; slot++) {
				System.out.println("    label " + slot + ": " + labels[slot]);
			}
		}

		// Smelting never sends a name - its eight are baked into the cache - so the build itself has
		// to have moved them across or its labels would stay blank forever.
		String[] smelt = labels(4);
		boolean seeded = smelt.length == SLOTS[4];
		for (int slot = 0; slot < smelt.length; slot++) {
			seeded &= smelt[slot] != null && smelt[slot].length() > 0 && smelt[slot].charAt(0) != ' ';
		}
		check("names: the eight smelting labels were seeded from the cache's own widgets", seeded);
		if (!seeded) {
			for (int slot = 0; slot < smelt.length; slot++) {
				System.out.println("    smelt label " + slot + ": " + smelt[slot]);
			}
		}
	}

	// ------------------------------------------------------------------ helpers

	/** The live product hitboxes of a family, left to right, as {@code select} last left them. */
	static int[] liveIds(int family) {
		int slots = SLOTS[family];
		RSInterface menu = RSInterface.interfaceCache[MakeX.CONTAINER_FIRST + family];
		int[] found = new int[slots];
		int[] x = new int[slots];
		int at = 0;
		for (int i = 0; i < menu.children.length; i++) {
			if (!isHitbox(menu, i, slots)) {
				continue;
			}
			if (at < slots) {
				found[at] = menu.children[i];
				x[at] = menu.childX[i];
				at++;
			}
		}
		if (at != slots) {
			return new int[0];
		}
		for (int i = 0; i < at; i++) {
			for (int j = i + 1; j < at; j++) {
				if (x[j] < x[i]) {
					int t = x[i];
					x[i] = x[j];
					x[j] = t;
					t = found[i];
					found[i] = found[j];
					found[j] = t;
				}
			}
		}
		return found;
	}

	/**
	 * A live hitbox: an actionable widget the size of a product box, on the slot row. The three
	 * amounts that are not armed are zeroed by {@code applySelection}, which is what keeps a click
	 * landing on the armed one, so "live" is exactly "has a size". The width is deliberately not
	 * compared to a slot width: the row is drawn at the width its live count asks for, and this
	 * predicate has to mean the same thing at every count.
	 */
	static boolean isHitbox(RSInterface menu, int index, int slots) {
		RSInterface child = RSInterface.interfaceCache[menu.children[index]];
		return child != null && child.atActionType != 0 && child.width > 0
				&& child.height == SLOT_H && menu.childY[index] + DRAW_OFFSET == SLOT_TOP;
	}

	/**
	 * The armed product hitboxes of a family, left to right, whatever the row's live count is.
	 *
	 * <p>Distinct from {@link #liveIds}, which answers "the row as the tables built it" and so
	 * insists on one per slot. This one is what the live-row checks use: it reports the boxes the
	 * player actually gets, which is fewer than the slots whenever the server filled fewer.
	 */
	static int[] sortedHitboxes(int family) {
		RSInterface menu = RSInterface.interfaceCache[MakeX.CONTAINER_FIRST + family];
		int[] found = new int[menu.children.length];
		int[] x = new int[menu.children.length];
		int at = 0;
		for (int i = 0; i < menu.children.length; i++) {
			if (!isHitbox(menu, i, SLOTS[family])) {
				continue;
			}
			found[at] = menu.children[i];
			x[at] = menu.childX[i];
			at++;
		}
		return sortedByX(found, x, at);
	}

	/** The drawn slot frames of a family, left to right: a type-5 box on the slot row with a size. */
	static int[] visibleSlotFrames(int family) {
		RSInterface menu = RSInterface.interfaceCache[MakeX.CONTAINER_FIRST + family];
		int[] found = new int[menu.children.length];
		int[] x = new int[menu.children.length];
		int at = 0;
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface child = RSInterface.interfaceCache[menu.children[i]];
			if (child != null && child.type == 5 && child.width > 0 && child.height == SLOT_H
					&& menu.childY[i] + DRAW_OFFSET == SLOT_TOP) {
				found[at] = menu.children[i];
				x[at] = menu.childX[i];
				at++;
			}
		}
		return sortedByX(found, x, at);
	}

	/**
	 * The product labels a family <em>draws</em>: none, and that is the assertion.
	 *
	 * <p>The name widgets exist so {@code absorbName} has somewhere to put the text it takes off the
	 * cache's own buttons, but they are not children of the container, so nothing draws them. The
	 * name used to be a child on its own row under each icon and was reported as clutter in a box
	 * the size of the icon; this is the check that keeps it from coming back by accident.
	 *
	 * <p>Found by the ids MakeX builds the labels at rather than by shape, because shape no longer
	 * tells them apart: a label was a 12px-tall text child and so is the help line, and a product's
	 * own name widget is a text child of exactly the slot's width.
	 */
	static int[] visibleLabels(int family) {
		RSInterface menu = RSInterface.interfaceCache[MakeX.CONTAINER_FIRST + family];
		int[] found = new int[LABEL_STRIDE];
		int[] x = new int[LABEL_STRIDE];
		int at = 0;
		for (int slot = 0; slot < LABEL_STRIDE; slot++) {
			int id = LABEL_FIRST + family * LABEL_STRIDE + slot;
			int index = childIndexOf(menu, id);
			if (index >= 0) {
				found[at] = id;
				x[at] = menu.childX[index];
				at++;
			}
		}
		return sortedByX(found, x, at);
	}

	/** How many boxes a family draws right now. */
	static int visibleSlots(int family) {
		return visibleSlotFrames(family).length;
	}

	/** How many actionable widgets a click at a panel point reaches, by the client's own hit test. */
	static int hitsAt(int family, int x, int y) {
		RSInterface menu = RSInterface.interfaceCache[MakeX.CONTAINER_FIRST + family];
		int hits = 0;
		for (int i = 0; i < menu.children.length; i++) {
			if (childAt(menu, i, x, y) != null) {
				hits++;
			}
		}
		return hits;
	}

	/** The widget the client would offer at a panel point, or -1: later children win, as drawn. */
	static int hitIdAt(int family, int x, int y) {
		RSInterface menu = RSInterface.interfaceCache[MakeX.CONTAINER_FIRST + family];
		int found = -1;
		for (int i = 0; i < menu.children.length; i++) {
			if (childAt(menu, i, x, y) != null) {
				found = menu.children[i];
			}
		}
		return found;
	}

	/** The client's hit test for one child at a panel point, or null. */
	private static RSInterface childAt(RSInterface menu, int index, int x, int y) {
		RSInterface child = RSInterface.interfaceCache[menu.children[index]];
		if (child == null || child.atActionType == 0 || child.width <= 0 || child.height <= 0) {
			return null;
		}
		int left = menu.childX[index] + DRAW_X;
		int top = menu.childY[index] + DRAW_OFFSET;
		if (x >= left && x < left + child.width && y >= top && y < top + child.height) {
			return child;
		}
		return null;
	}

	/** A child's width, for the geometry assertions that read it off the widget. */
	static int rsWidth(int id) {
		RSInterface child = RSInterface.interfaceCache[id];
		return child == null ? -1 : child.width;
	}

	/** The y twin of {@link #rsWidth}. */
	static int rsHeight(int id) {
		RSInterface child = RSInterface.interfaceCache[id];
		return child == null ? -1 : child.height;
	}

	/** An index/position pair sorted into left-to-right position order. */
	private static int[] sortedByX(int[] ids, int[] x, int at) {
		int[] out = new int[at];
		System.arraycopy(ids, 0, out, 0, at);
		for (int i = 0; i < at; i++) {
			for (int j = i + 1; j < at; j++) {
				if (x[j] < x[i]) {
					int t = x[i];
					x[i] = x[j];
					x[j] = t;
					t = out[i];
					out[i] = out[j];
					out[j] = t;
				}
			}
		}
		return out;
	}

	/* ----------------------------------------------------- the server's fills */

	/**
	 * The item model frames of each family, in slot order, captured once from the widest layout.
	 *
	 * <p>Read off the built container rather than restated from {@code MakeX}'s table, so a table
	 * that stopped matching the cache shows up as a family with the wrong number of models. It has
	 * to be captured before the first recount: after that the unused slots are parked at the same
	 * off-panel x and no longer sort into slot order.
	 */
	static int[][] modelIds = new int[LEGACY.length][];

	private static void snapshotModels() {
		for (int family = 0; family < LEGACY.length; family++) {
			modelIds[family] = modelsOf(family);
		}
	}

	private static int[] modelsOf(int family) {
		int slots = SLOTS[family];
		RSInterface menu = RSInterface.interfaceCache[MakeX.CONTAINER_FIRST + family];
		int[] found = new int[slots];
		int[] x = new int[slots];
		int at = 0;
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface child = RSInterface.interfaceCache[menu.children[i]];
			if (child != null && child.type != 5 && child.type != 4 && child.height == SLOT_H
					&& child.width > 0 && menu.childY[i] + DRAW_OFFSET == SLOT_TOP) {
				if (at < slots) {
					found[at] = menu.children[i];
					x[at] = menu.childX[i];
					at++;
				}
			}
		}
		if (at != slots) {
			return new int[0];
		}
		return sortedByX(found, x, at);
	}

	private static String describeModels() {
		StringBuilder sb = new StringBuilder();
		for (int family = 0; family < LEGACY.length; family++) {
			sb.append(FAMILY_NAMES[family]).append('=').append(modelIds[family].length);
			if (family + 1 < LEGACY.length) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	/**
	 * Fills (or empties) a family's first {@code count} slots the way packet 246 does.
	 *
	 * <p>{@code anInt233} is the item render kind, and packet 246 sets it to {@code 0} for item
	 * {@code -1}: that is exactly the mark {@code MakeX.filledSlots} reads, so writing it here is
	 * the server's own behaviour rather than a stand-in for it. {@code mediaID} is deliberately left
	 * alone, because the packet leaves the previous item's id in place and MakeX does not read it.
	 */
	static void fill(int family, int count) {
		int[] models = modelIds[family];
		for (int slot = 0; slot < models.length; slot++) {
			RSInterface model = RSInterface.interfaceCache[models[slot]];
			if (model != null) {
				model.anInt233 = slot < count ? 4 : 0;
			}
		}
	}

	/** Fills a family completely and lets MakeX re-lay it out: the widest state it can be in. */
	static void allFilled(int family) {
		if (modelIds[family] == null || modelIds[family].length == 0) {
			return;
		}
		fill(family, SLOTS[family]);
		MakeX.onModelUpdate(modelIds[family][0]);
	}

	// -------------------------------------------------------- icon size and place

	/**
	 * The size a product's icon is drawn at, and where inside its box it sits.
	 *
	 * <p>{@code MakeX} discards the zoom the server sends and draws every product from the item's own
	 * {@code modelZoom} scaled by one fixed fraction, so the same item is the same size in every
	 * menu. That is the whole reason the fraction exists, and it is invisible here: a fraction the
	 * wrong way round draws a smaller icon, which is not a crash and not a missing widget, so the
	 * layout checks all pass while the player sees the wrong thing - which is how this was reported
	 * twice.
	 *
	 * <p>So the fraction is checked against the sprite it is trying to match. The inventory draws an
	 * item at its plain {@code modelZoom} into a 32px tile; a product box is {@code SLOT_H} tall, so
	 * 32/SLOT_H is the fraction that fills the box and this one is a tenth more than that, which is
	 * the 30% smaller icon that was asked for after seeing the first. And because zoom is a distance
	 * rather than a scale, the check that pins the direction is that the fraction is below 1.
	 *
	 * <p>The icon's <em>place</em> is the second half, and it is the half that was missed. The type 6
	 * branch in {@code client} hands {@code method482} no y translation at all, so an item is drawn
	 * around its own origin; a potion's whole bottle is above y 0, which puts its cork over the top
	 * of the box and its body in the top half of it. {@code MakeX} works out the inventory sprite's
	 * own correction for that, per item, and the check here is against a real potion from the cache -
	 * the item the report was about - rather than against the number the code would like.
	 */
	private static void iconSize() {
		System.out.println();
		System.out.println("the product icon's size and place in its box");
		int frame = modelIds[0][0];
		RSInterface model = RSInterface.interfaceCache[frame];
		int item = firstModelledItem();
		int itemZoom = zoomOf(item);
		writeIcon(frame, item, itemZoom * 7);
		int want = itemZoom * ICON_ZOOM_NUM / ICON_ZOOM_DEN;
		check("icon: the model is drawn from the item's own zoom scaled by " + ICON_ZOOM_NUM + "/"
				+ ICON_ZOOM_DEN + ", not the zoom the server sent", model.modelZoom == want);
		double fraction = ICON_ZOOM_NUM / (double) ICON_ZOOM_DEN;
		check("icon: the fraction is below 1, which is what draws it larger than the 32px sprite "
				+ "rather than smaller", fraction < 1.0);
		// Drawn size, in the pixels the inventory's own tile would use: an item whose zoom is this
		// fraction of its own is drawn 32/fraction tall. That has to come to less than the box - the
		// whole point of taking 3/8 down - and to a tenth over the box-filling size, which is the
		// 30% asked for rather than a number picked here.
		double drawn = 32.0 / fraction;
		double ofFill = (3.0 / 8.0) / fraction;
		System.out.println("    drawn " + Math.round(drawn) + "px in a " + SLOT_H + "px box, "
				+ Math.round(ofFill * 100) + "% of what the filling fraction drew");
		check("icon: " + ICON_ZOOM_NUM + "/" + ICON_ZOOM_DEN + " draws the icon inside its " + SLOT_H
				+ "px box with a margin, " + Math.round(drawn) + "px", drawn < SLOT_H);
		check("icon: it draws 30% smaller than the fraction that filled the box, to within 5% ("
				+ Math.round(ofFill * 100) + "%)", Math.abs(ofFill - 0.7) <= 0.05);

		// The place, and the control that has to come before the fact. A product's item arrives in a
		// packet, and its model arrives later, in answer to a fetch that drawing the item triggers -
		// so a measurement taken when the packet lands reads a model that is not there, and because
		// the slot is only measured again when the server writes to it, that first reading would
		// stand for as long as the menu is open. Which is why the first check is that nothing is
		// claimed while the model is missing, and the model is fetched only after it.
		int potion = firstPotionItem();
		String potionName = nameOf(potion);
		writeIcon(frame, potion, zoomOf(potion) * 7);
		check("icon: nothing is claimed about a box whose model has not been fetched, so the answer "
				+ "can change once it is", MakeX.iconDropY(frame) == 0);
		check("icon: the cache does have " + potionName + "'s model to fetch", loadModel(potion));

		// The potion itself, which is what the report showed: its model is authored standing on its
		// origin - every vertex at or above y 0, the bottle reaching up and away from it - so without
		// a drop it is drawn from the top of its box downward and its cork crosses the frame's top
		// edge. The drop has to be downward, not zero and not the other way.
		int[] bounds = yBounds(potion);
		int potionDrop = MakeX.iconDropY(frame);
		int potionWant = expectedDrop(potion);
		check("icon: " + potionName + "'s model stands on its origin (" + bounds[0] + ".."
				+ bounds[1] + "), which is why its cork crossed the frame", bounds[1] <= 0);
		check("icon: so a potion in a product box is dropped down " + potionDrop
				+ " model units, not left on the box's top edge",
				potionDrop > 0 && potionDrop == potionWant);

		// A product box is the only type 6 widget the client drops, so a widget that is not one has
		// to come back zero - that is the dialogue heads, which are drawn as the cache authored them.
		check("icon: a widget that is not a product box is not dropped", MakeX.iconDropY(-1) == 0);

		// And the gate that decides which widgets those are, which is the part that was wrong: the
		// client asked whether a widget was one of the five CONTAINERS, and every icon it meant to
		// move is a CHILD of a container rather than a container itself, so it answered no for all of
		// them and nothing below this line reached the screen. The drop was right, the probe agreed
		// with it, and the player saw the old placement - so the gate is checked here, in both
		// directions, rather than left to the type 6 branch in client to get right.
		boolean everyFrameIsAProduct = true;
		for (int family = 0; family < modelIds.length; family++) {
			for (int slot = 0; slot < modelIds[family].length; slot++) {
				everyFrameIsAProduct &= MakeX.isProductFrame(modelIds[family][slot]);
			}
		}
		check("icon: every one of the " + frameCount() + " product frames is recognised as one, which "
				+ "is the gate the client drops them behind", everyFrameIsAProduct);
		boolean containersAreNotProducts = true;
		for (int family = 0; family < LEGACY.length; family++) {
			containersAreNotProducts &= !MakeX.isProductFrame(MakeX.CONTAINER_FIRST + family);
		}
		check("icon: none of the five containers is itself a product frame - the bug this replaced "
				+ "asked about the container and so never dropped anything",
				containersAreNotProducts && !MakeX.isProductFrame(LEGACY[0])
						&& !MakeX.isProductFrame(-1));

		// A product frame is also the only thing the client clips to its box, so a frame whose icon is
		// drawn taller than its box would now be cut off where before it spilled. The box-filling
		// fraction is what stops that, and this is the check that it holds for the wide, sprawling
		// items too rather than only for the tidy shape above - measured, not assumed, because a
		// clipped model is invisible in every geometry assertion and obvious to a player.
		String[] spread = { "Rune 2h sword", "Rune platebody", "Raw shark", "Rune scimitar", "Air rune",
				"Rune axe", "Yew logs" };
		int measured = 0;
		boolean everySpreadFits = true;
		for (int i = 0; i < spread.length; i++) {
			int id = itemNamed(spread[i]);
			if (id < 0 || !loadModel(id)) {
				continue;
			}
			writeIcon(frame, id, zoomOf(id) * 7);
			RSInterface wide = RSInterface.interfaceCache[frame];
			wide.modelRotation1 = ItemDef.forID(id).modelRotationY;
			wide.modelRotation2 = ItemDef.forID(id).modelRotationX;
			int[] proj = projectYs(id, frame, MakeX.iconDropY(frame));
			measured++;
			if (proj[0] < -(wide.height / 2) || proj[1] > wide.height / 2) {
				System.out.println("    " + spread[i] + " projects " + proj[0] + ".." + proj[1]
						+ " in a box of +-" + (wide.height / 2));
				everySpreadFits = false;
			}
		}
		check("icon: none of the " + measured + " wide items measured is drawn taller than the box "
				+ "the client now clips them to", everySpreadFits && measured >= 5);

		// And the ordinary case: an item written into the same box, its model fetched, dropped by
		// whatever centres its own model. Usually zero, because most models are authored around their
		// own middle - which is why this is a check about the measurement and not about the number.
		writeIcon(frame, item, itemZoom * 7);
		boolean fetched = loadModel(item);
		int itemDrop = MakeX.iconDropY(frame);
		int itemWant = expectedDrop(item);
		check("icon: the box is dropped by the amount that centres the item's own model in it ("
				+ itemDrop + " model units for " + nameOf(item) + ", wanted " + itemWant + ")",
				fetched && itemDrop == itemWant);

		int[] itemBounds = yBounds(item);
		System.out.println("    " + nameOf(item) + " model y " + itemBounds[0] + ".." + itemBounds[1]
				+ " of " + itemBounds[2] + " vertices, dropped " + itemDrop + " units");
		System.out.println("    " + potionName + " model y " + bounds[0] + ".." + bounds[1] + " of "
				+ bounds[2] + " vertices, dropped " + potionDrop + " units, drawn "
				+ Math.round(potionDrop * 512.0 / (zoomOf(potion) * ICON_ZOOM_NUM
						/ (double) ICON_ZOOM_DEN)) + "px below the box's centre");
	}

	/**
	 * How many product frames the five families were built with, for the messages that count them.
	 */
	private static int frameCount() {
		int total = 0;
		for (int family = 0; family < modelIds.length; family++) {
			total += modelIds[family] == null ? 0 : modelIds[family].length;
		}
		return total;
	}

	/**
	 * Writes an item into a product frame exactly as the item packet does before the server asks
	 * MakeX to look at the frame, with a deliberately wrong zoom for the build to overwrite.
	 */
	private static void writeIcon(int frame, int item, int zoom) {
		RSInterface model = RSInterface.interfaceCache[frame];
		model.mediaID = item;
		model.anInt233 = 4;
		model.modelZoom = zoom;
		MakeX.onModelUpdate(frame);
	}

	// ------------------------------------------------------- the potion rule
	/**
	 * Where a potion's icon actually lands inside its box, and that it is lower than a non-potion's.
	 *
	 * <p><b>Why this is measured in pixels and not in the drop.</b> The drop is model units added to a
	 * translation, and a translation is not what the player sees: what they see is where the projected
	 * vertices fall, which also depends on the item's rotation, the zoom and the depth the model is
	 * pushed to. A drop that reads as a large number can move almost nothing, and the reverse, so a
	 * check on the number alone would have passed for the version that drew every potion with its cork
	 * through the top of its frame. So the vertices are projected the way {@code method482} projects
	 * them and the box is compared against what comes out.
	 *
	 * <p><b>Why the item is written the way the packet writes it.</b> The packet does not use the zoom
	 * or the rotations the cache left on the frame: it writes the ITEM's own
	 * {@code modelRotationY}/{@code modelRotationX} onto it, and MakeX then replaces the zoom the
	 * server sent with the item's own scaled by the icon fraction. Measuring against the cache's
	 * zeros measures a rotation no product box is ever drawn with, and a rotated model sits
	 * differently - which is the second time that has mattered here.
	 */
	private static void potionPlacement() {
		System.out.println();
		System.out.println("the potion rule: where a bottle lands in its box");
		int frame = modelIds[1][0];
		int potion = prayerPotion();
		if (potion < 0) {
			check("potion: the cache has a prayer potion to measure", false);
			return;
		}
		String potionName = nameOf(potion);

		// The same box, fetched and rotated the way the packet would leave it, for a potion and then
		// for an ordinary item.
		loadModel(potion);
		writeIcon(frame, potion, zoomOf(potion) * 7);
		RSInterface box = RSInterface.interfaceCache[frame];
		box.modelRotation1 = ItemDef.forID(potion).modelRotationY;
		box.modelRotation2 = ItemDef.forID(potion).modelRotationX;
		int potionDrop = MakeX.iconDropY(frame);
		int[] potionProj = projectYs(potion, frame, potionDrop);
		int potionTop = potionProj[0];
		int potionBottom = potionProj[1];
		int boxTop = -(box.height / 2);
		int boxBottom = box.height / 2;

		check("potion: " + potionName + " is drawn entirely inside its box (" + potionTop + ".."
				+ potionBottom + " of " + boxTop + ".." + boxBottom + ")",
				potionTop >= boxTop && potionBottom <= boxBottom);

		// The complaint itself, as a check: the base of the bottle is in the lower half of the box,
		// not left up against its top edge. With no drop at all the whole model sits from the top edge
		// downward, which is what the screenshot showed.
		check("potion: " + potionName + "'s base is at " + potionBottom + ", in the lower half of the "
				+ box.height + "px box rather than at its top", potionBottom > 0);

		// And it is the whole family, not one item. A drop tuned to a single mesh would be a patch
		// rather than a rule, so the same measurement is taken for a drink of every shape the menus
		// make - each dose, finished and unfinished, a brew, a serum, the dose bottles that are not
		// drinks - and every one of them has to land inside its box and low in it.
		String[] family = { "Attack potion(4)", "Attack potion(1)", "Prayer potion(4)",
				"Super attack(3)", "Super strength(2)", "Super defence(1)", "Super energy(4)",
				"Super restore(3)", "Antipoison(2)", "Weapon poison", "Restore potion(4)",
				"Energy potion(2)", "Ranarr potion(unf)", "Torstol potion(unf)", "Zamorak brew(4)",
				"Khali brew", "Serum 207 (4)", "Olive oil(4)", "Sacred oil(2)", "Vial of water" };
		int placed = 0;
		boolean everyOneInside = true;
		boolean everyOneLow = true;
		boolean everyOneWanted = true;
		StringBuilder lows = new StringBuilder();
		for (int i = 0; i < family.length; i++) {
			int id = itemNamed(family[i]);
			if (id < 0 || !loadModel(id)) {
				System.out.println("    not in this cache: " + family[i]);
				continue;
			}
			writeIcon(frame, id, zoomOf(id) * 7);
			RSInterface f = RSInterface.interfaceCache[frame];
			f.modelRotation1 = ItemDef.forID(id).modelRotationY;
			f.modelRotation2 = ItemDef.forID(id).modelRotationX;
			int drop = MakeX.iconDropY(frame);
			int[] proj = projectYs(id, frame, drop);
			placed++;
			if (proj[0] < -(f.height / 2) || proj[1] > f.height / 2) {
				System.out.println("    " + family[i] + " projects " + proj[0] + ".." + proj[1]
						+ " of +-" + (f.height / 2));
				everyOneInside = false;
			}
			if (proj[1] <= 0) {
				System.out.println("    " + family[i] + " is not in the lower half: base " + proj[1]);
				everyOneLow = false;
			}
			if (drop != expectedDrop(id)) {
				System.out.println("    " + family[i] + " drew " + drop + ", wanted " + expectedDrop(id));
				everyOneWanted = false;
			}
			lows.append(family[i]).append('=').append(proj[1]).append(' ');
		}
		check("potion: all " + placed + " shapes a potion is drawn in land inside their box (" + lows
				+ ")", everyOneInside && placed >= 15);
		check("potion: and every one of them lands low in its box rather than at its top", everyOneLow);
		check("potion: every one of them is dropped by the potion rule and by no other amount",
				everyOneWanted);

		// Standing on the line the report drew. The picture the request came with has a fine red line
		// across the bottom of the box - 230,27,27, and no sprite in the cache holds that colour, so
		// it is a mark drawn on the screenshot rather than part of the interface: the player's own
		// answer to "how low should this sit". It falls REPORT_LINE_ABOVE_BOTTOM pixels above the
		// frame's bottom border, which is the number the placement is pinned by - a rule that only
		// said "lower than half way" would let the flask float most of a bottle's width above it and
		// still pass.
		int line = (box.height / 2) - REPORT_LINE_ABOVE_BOTTOM;
		check("potion: " + potionName + "'s base is at " + potionBottom + ", within 8px above the line "
				+ "the report drew at " + line + " rather than floating clear of it",
				potionBottom <= line && potionBottom >= line - 8);

		// The rule has to be doing something, and the thing it does has to be downward.
		int centring = -((yBounds(potion)[0] + yBounds(potion)[1]) / 2)
				+ ItemDef.forID(potion).modelOffset2;
		check("potion: the potion rule drops " + potionName + " " + potionDrop + " model units, "
				+ "further than the " + centring + " which only centres it", potionDrop > centring);

		// And the ordinary item in the same box is NOT given the potion's extra: the rule is about
		// bottles, so a dagger, a fish and an ore keep the plain centring.
		int ordinary = firstModelledItem();
		loadModel(ordinary);
		writeIcon(frame, ordinary, zoomOf(ordinary) * 7);
		RSInterface ordinaryBox = RSInterface.interfaceCache[frame];
		ordinaryBox.modelRotation1 = ItemDef.forID(ordinary).modelRotationY;
		ordinaryBox.modelRotation2 = ItemDef.forID(ordinary).modelRotationX;
		int ordinaryDrop = MakeX.iconDropY(frame);
		check("potion: " + nameOf(ordinary) + ", an ordinary item, is left at the plain centring of "
				+ expectedDrop(ordinary) + " (drawn " + ordinaryDrop + ")",
				!MakeX.isPotion(ItemDef.forID(ordinary)) && ordinaryDrop == expectedDrop(ordinary));

		// The rule itself, which is a policy and so the one part the cache cannot simply be asked
		// about. Two directions: the drinks the cache holds are recognised, and the things that share
		// their names - the herbs, the prayers, the empty vials - are not.
		String[] drinks = { "Attack potion(3)", "Prayer potion(4)", "Prayer potion(3)",
				"Super attack(3)", "Super strength(2)", "Super defence(1)", "Super energy(4)",
				"Super restore(3)", "Antipoison(3)", "Weapon poison", "Restore potion(4)",
				"Energy potion(2)", "Ranarr potion(unf)", "Zamorak brew(4)", "Khali brew",
				"Serum 207 (4)" };
		boolean allDrinks = true;
		for (int i = 0; i < drinks.length; i++) {
			if (!isPotionNamed(drinks[i])) {
				System.out.println("    not recognised: " + drinks[i]);
				allDrinks = false;
			}
		}
		check("potion: all " + drinks.length + " drinks the cache holds - including the super attack, "
				+ "strength, defence, energy and restore families, which are potions without the word "
				+ "- are recognised by the rule", allDrinks);

		// The names that share a drink's word but are not bottles at all: herbs, prayers, food, ore.
		String[] notDrinks = { "Ranarr weed", "Torstol", "Attack", "Strength", "Prayer", "Iron ore",
				"Raw shark", "Guam leaf", "Pestle and mortar", "Kebab mix", "Cannon base",
				"Bones", "Coins", "Rune scimitar" };
		boolean noFalsePositives = true;
		for (int i = 0; i < notDrinks.length; i++) {
			if (isPotionNamed(notDrinks[i])) {
				System.out.println("    wrongly recognised: " + notDrinks[i]);
				noFalsePositives = false;
			}
		}
		check("potion: none of the " + notDrinks.length + " items that share a drink's word - the "
				+ "herbs, the prayers, the ore, the food - are", noFalsePositives);

		// And the half of the rule the names cannot do: the four dose bottles. The point of that half
		// is that it catches the drinks called after their stat rather than after being a potion, so
		// what has to be true is that those drinks are caught by the MESH and not by their names -
		// otherwise the meshes could be dropped entirely and nothing would change.
		String[] meshOnly = { "Super attack(3)", "Super strength(2)", "Super defence(1)",
				"Super energy(4)", "Super restore(3)", "Antipoison(3)" };
		boolean meshIsDoingWork = true;
		for (int i = 0; i < meshOnly.length; i++) {
			if (!isPotionNamed(meshOnly[i]) || nameSaysPotion(meshOnly[i])) {
				System.out.println("    the mesh is not what catches: " + meshOnly[i]);
				meshIsDoingWork = false;
			}
		}
		check("potion: " + meshOnly.length + " drinks - the super attack, strength, defence, energy "
				+ "and restore families - are caught by their dose bottle rather than by their names",
				meshIsDoingWork);

		// What the meshes catch beyond the names, printed rather than assumed: they are bottle-shaped
		// items, and a vial sits high in its box for the same reason a potion does. This is the list
		// that says how wide the rule is in practice.
		int doseDrawn = 0;
		int namedDrawn = 0;
		StringBuilder extra = new StringBuilder();
		for (int id = 1; id < 4000; id++) {
			ItemDef d = ItemDef.forID(id);
			if (d.name == null || !isDoseDrawn(d.modelID)) {
				continue;
			}
			doseDrawn++;
			if (nameSaysPotion(d.name)) {
				namedDrawn++;
			} else if (extra.length() < 200) {
				extra.append(id).append(' ').append(d.name).append("; ");
			}
		}
		check("potion: the four dose bottles are a real signal - they draw " + doseDrawn
				+ " items, far more than the " + namedDrawn + " of them their names identify",
				doseDrawn > namedDrawn + 30);
		check("potion: the bottle every unfinished potion shares is not the signal - 1225 items use "
				+ "it, a cannon base among them", !isDoseDrawn(2429));

		// Stated rather than left implicit, because it is the one place the rule is wider than its		// name: a vial and a bottle of oil are drawn from the same meshes and are the same shape, so
		// they are placed the same way. Nothing else non-drink is a dose bottle - the list below is
		// the whole of it, and a seventh entry appearing here means a new item family has shown up and
		// this decision should be looked at again.
		String[] otherBottles = { "Vial", "Vial of water", "Olive oil", "Sacred oil", "Liquid honey",
				"Blamish oil", "Sulphuric broline", "Nitroglycerin", "Chemical compound", "Firework" };
		boolean allBottles = true;
		for (int i = 0; i < otherBottles.length; i++) {
			if (!anyPotionNamed(otherBottles[i])) {
				System.out.println("    a dose bottle is NOT recognised: " + otherBottles[i]);
				allBottles = false;
			}
		}
		check("potion: the other bottles the meshes draw - a vial, an oil, the digsite chemicals - are "
				+ "placed by the rule too, because they are the same shape standing on the same origin",
				allBottles);
		System.out.println("    a dose bottle also draws, beyond the names: " + extra);

		System.out.println("    " + potionName + " drawn " + potionTop + ".." + potionBottom + " in a "
				+ box.height + "px box (drop " + potionDrop + " against " + centring
				+ " for centring alone)");
	}

	/** Whether the rule recognises an item by name: looked up, then asked. */
	private static boolean isPotionNamed(String name) {
		int id = itemNamed(name);
		return id >= 0 && MakeX.isPotion(ItemDef.forID(id));
	}

	/**
	 * Whether ANY item whose name starts with this is recognised.
	 *
	 * <p>Needed because a name is not an item here: "Vial" names several items and the first of them
	 * is not the one drawn from a dose bottle, and "Olive oil" only exists in doses. Both are facts
	 * about this cache rather than about the rule, so the check asks the question that answers them.
	 */
	private static boolean anyPotionNamed(String prefix) {
		String lower = prefix.toLowerCase();
		for (int id = 1; id < 4000; id++) {
			ItemDef d = ItemDef.forID(id);
			if (d.name != null && d.name.toLowerCase().startsWith(lower) && MakeX.isPotion(d)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether a mesh is one of the four dose bottles the rule treats as a potion.
	 *
	 * <p>Restated here for the same reason as everything else in this file: a check that asked MakeX
	 * would pass whatever MakeX said.
	 */
	private static boolean isDoseDrawn(int model) {
		return model == 2789 || model == 2697 || model == 2384 || model == 2621;
	}

	/**
	 * Whether a name is one the RULE would recognise on its own, without the mesh.
	 *
	 * <p>The rule's own words, restated - the one place in this file that has to restate a policy,
	 * because the check it serves is "the mesh is doing work the names are not" and there is no way to
	 * ask that question without knowing what the names say.
	 */
	private static boolean nameSaysPotion(String name) {
		String lower = name.toLowerCase();
		return lower.indexOf("potion") >= 0 || lower.indexOf("brew") >= 0
				|| lower.indexOf("serum") >= 0 || lower.indexOf("antidote") >= 0
				|| lower.indexOf("mixture") >= 0 || lower.indexOf("mix(") >= 0;
	}

	/** The item the potion checks are about: a prayer potion if the cache has one, else any potion. */
	private static int prayerPotion() {
		int four = itemNamed("Prayer potion(4)");
		if (four >= 0) {
			return four;
		}
		int three = itemNamed("Prayer potion(3)");
		return three >= 0 ? three : firstPotionItem();
	}



	/**
	 * Projects a written icon's vertices the way {@code method482} does, and returns the topmost and
	 * bottommost result as offsets from the middle of the box.
	 *
	 * <p>This is the half of the render path the drop feeds into, copied rather than called because
	 * the client's own is buried in its type 6 branch: the translation is added to each vertex, the
	 * two rotations are applied, the vertex is dropped if it has gone behind the camera, and the rest
	 * are projected as {@code (y << 9) / depth}. The one thing it leaves out is the rasteriser, so
	 * what comes back is the extent of the SHAPE - vertices, not lit pixels - which is why the
	 * margins it reports are a little wider than what a screenshot of the same box shows.
	 *
	 * <p>A vertex whose projected depth is not positive is skipped: {@code method482} culls those,
	 * and including them would report a box taller than anything drawn in it.
	 */
	private static int[] projectYs(int item, int frame, int drop) {
		RSInterface w = RSInterface.interfaceCache[frame];
		Model model = ItemDef.forID(item).method202(1);
		if (model == null) {
			return new int[] { 0, 0 };
		}
		int r1 = w.modelRotation1;
		int r2 = w.modelRotation2;
		int yTrans = (Model.modelIntArray1[r1] * w.modelZoom >> 16) + drop;
		int xTrans = Model.modelIntArray2[r1] * w.modelZoom >> 16;
		int l2 = Model.modelIntArray1[r2];
		int i3 = Model.modelIntArray2[r2];
		int l3 = Model.modelIntArray1[r1];
		int i4 = Model.modelIntArray2[r1];
		int[] xs = model.vertexXs();
		int[] ys = model.vertexYs();
		int[] zs = model.vertexZs();
		int n = model.vertexCount();
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		for (int v = 0; v < n; v++) {
			int l4 = xs[v];
			int i5 = ys[v];
			int j5 = zs[v];
			if (r2 != 0) {
				int i6 = j5 * l2 + l4 * i3 >> 16;
				j5 = j5 * i3 - l4 * l2 >> 16;
				l4 = i6;
			}
			i5 += yTrans;
			j5 += xTrans;
			int j6 = i5 * i4 - j5 * l3 >> 16;
			j5 = i5 * l3 + j5 * i4 >> 16;
			i5 = j6;
			if (j5 <= 0) {
				continue;
			}
			int sy = (i5 << 9) / j5;
			if (sy < low) {
				low = sy;
			}
			if (sy > high) {
				high = sy;
			}
		}
		if (low > high) {
			return new int[] { 0, 0 };
		}
		return new int[] { low, high };
	}

	/**
	 * The first item in the cache whose name is exactly this, or -1.
	 *
	 * <p>By name because the potion rule is a policy about names, and the check has to be able to name
	 * the items it is about - {@code Prayer potion(4)} - rather than hard-code the id a cache may not
	 * agree with. Scanned rather than indexed, which is fast enough for a probe that runs once.
	 */
	private static int itemNamed(String wanted) {
		for (int id = 1; id < 4000; id++) {
			String name = ItemDef.forID(id).name;
			if (name != null && name.equalsIgnoreCase(wanted)) {
				return id;
			}
		}
		return -1;
	}

	/*
	 * Every helper below takes an ITEM ID rather than an ItemDef, and that is not a style choice.
	 * ItemDef.forID keeps a ten-entry ring of mutable definitions and re-parses into the oldest one
	 * on a miss, so a reference held across another lookup can silently become a different item: a
	 * version of this section that held one did exactly that, and reported a potion's model as the
	 * model of the item in the box, which fails in the most confusing possible way - the drop looked
	 * wrong when the number the two halves were computed from was not even about the same item. The
	 * client never holds one either; it looks the item up again at each use.
	 */

	/** An item's name, or null. */
	private static String nameOf(int item) {
		return ItemDef.forID(item).name;
	}

	/** An item's own modelZoom - the one the inventory's sprite is rendered from. */
	private static int zoomOf(int item) {
		return ItemDef.forID(item).modelZoom;
	}

	/**
	 * Registers an item's model with the client, the way the client ends up with it.
	 *
	 * <p>The same two steps {@code client.startUp} takes before any model can be drawn:
	 * {@code Model.method459} sizes the slot a fetched model lands in - {@code Model.method460} does
	 * nothing at all until that has happened - and {@code method460} is handed the model's inflated
	 * bytes. The cache keeps them gzipped (every entry checked here began {@code 1f 8b}), which
	 * {@code client.preloadModels} inflates for the loose files it reads and this does for the
	 * packed ones.
	 *
	 * <p>Returns false when the cache has no such model. Nothing is registered in that case, and the
	 * caller must not go on to ask for the item's model: {@code Model.method462} answers a miss by
	 * asking the fetcher it was built with to fetch it, and it is a stub here.
	 */
	private static boolean loadModel(int item) {
		int model = ItemDef.forID(item).modelID;
		if (model <= 0) {
			return false;
		}
		if (loadedModels.contains(Integer.valueOf(model))) {
			return true;
		}
		byte[] packed = modelIndex().decompress(model);
		if (packed == null) {
			return false;
		}
		try {
			Model.method460(gunzip(packed), model);
		} catch (Throwable t) {
			return false;
		}
		loadedModels.add(Integer.valueOf(model));
		return true;
	}

	/** The model index, opened and allocated on first use. See {@link #cacheRoot}. */
	private static Decompressor modelIndex() {
		if (modelIndex == null) {
			try {
				modelData = new RandomAccessFile(new File(cacheRoot, "main_file_cache.dat"), "r");
				modelIdx = new RandomAccessFile(new File(cacheRoot, "main_file_cache.idx1"), "r");
			} catch (Exception e) {
				throw new IllegalStateException("no model index in " + cacheRoot, e);
			}
			// Index 1 with type tag 2, which is how client.java builds decompressors[1] - the same
			// +1 the interface archive is read with at index 0.
			modelIndex = new Decompressor(modelData, modelIdx, 2);
		}
		if (!modelsAllocated) {
			modelsAllocated = true;
			// The count client.java passes is the update server's; what matters is that the array
			// exists and is long enough for every id the cache holds.
			Model.method459(MODEL_SLOTS, stubFetcher());
		}
		return modelIndex;
	}

	/**
	 * A fetcher that does nothing, installed where the client installs itself.
	 *
	 * <p>{@code Model.method462} answers a model it has not been sent by asking the fetcher it was
	 * built with for it ({@code method548}), and a null there is a crash rather than the "not here
	 * yet" the caller is written to handle. A stub makes a miss behave here exactly as it does in the
	 * client - request ignored, null returned - which is what lets the check below ask about a box
	 * before its model is fetched. Built by reflection rather than by subclassing, because
	 * {@code OnDemandFetcherParent} has no constructor a class outside its package can call.
	 */
	private static OnDemandFetcherParent stubFetcher() {
		try {
			java.lang.reflect.Constructor<OnDemandFetcherParent> c = OnDemandFetcherParent.class
					.getDeclaredConstructor();
			c.setAccessible(true);
			return c.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("cannot build a stand-in for the model fetcher", e);
		}
	}

	/** Inflates a gzipped cache entry, and passes through anything that is not gzipped. */
	private static byte[] gunzip(byte[] packed) throws Exception {
		if (packed.length < 2 || (packed[0] & 0xff) != 0x1f || (packed[1] & 0xff) != 0x8b) {
			return packed;
		}
		java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(
				new java.io.ByteArrayInputStream(packed));
		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(packed.length * 4);
		byte[] buffer = new byte[8192];
		int n;
		while ((n = gis.read(buffer)) > 0) {
			bos.write(buffer, 0, n);
		}
		gis.close();
		return bos.toByteArray();
	}

	/**
	 * The first item in the cache the item definitions will answer for, for the size check: one with
	 * a name, a zoom and a model to measure, since a zoom alone can belong to an item whose model id
	 * is 0 or is not in the cache.
	 */
	private static int firstModelledItem() {
		for (int id = 1; id < 4000; id++) {
			// Read out in one go, before anything else can look up a definition: see the note above
			// about the ring these come out of.
			ItemDef def = ItemDef.forID(id);
			String name = def.name;
			int zoom = def.modelZoom;
			int model = def.modelID;
			if (name != null && zoom > 0 && hasModel(model)) {
				return id;
			}
		}
		throw new IllegalStateException("no item in the cache has a zoom and a model");
	}

	/** The cache's first potion, which is the item the icon's place was reported about. */
	private static int firstPotionItem() {
		for (int id = 1; id < 4000; id++) {
			ItemDef def = ItemDef.forID(id);
			String name = def.name;
			int zoom = def.modelZoom;
			int model = def.modelID;
			if (name != null && zoom > 0 && hasModel(model)
					&& name.toLowerCase().contains("potion")) {
				return id;
			}
		}
		throw new IllegalStateException("no potion in the cache");
	}

	/**
	 * Whether the cache HOLDS an item's model - read off the cache's own index, deliberately not by
	 * loading it. The one check that needs the model to be absent first has to be able to find an
	 * item without fetching what it finds.
	 */
	private static boolean hasModel(int model) {
		return model > 0 && modelIndex().decompress(model) != null;
	}

	/**
	 * The item's model y range and vertex count, read off the model the renderer will draw it from.
	 *
	 * <p>Only valid once {@link #loadModel} has registered that item's model.
	 */
	private static int[] yBounds(int item) {
		ItemDef def = ItemDef.forID(item);
		int modelId = def.modelID;
		Model model = def.method202(1);
		if (model == null || model.vertexCount() <= 0) {
			throw new IllegalStateException("item model " + modelId + " is not loaded");
		}
		int[] ys = model.vertexYs();
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		for (int i = 0; i < model.vertexCount(); i++) {
			int y = ys[i];
			if (y < low) {
				low = y;
			}
			if (y > high) {
				high = y;
			}
		}
		return new int[] { low, high, model.vertexCount() };
	}

	/**
	 * The drop {@code MakeX} should have worked out for this item: the middle of its model's own y
	 * range, negated so it is a downward translation, plus the item's own offset.
	 *
	 * <p>Measured on the model the renderer will hand {@code method482} - {@code method202}, the raw
	 * one - not on the lit and scaled copy the inventory's sprite is rendered from.
	 */
	private static int expectedDrop(int item) {
		// The offset is read out of the definition before the bounds are measured, because measuring
		// looks the item up again and hands back a definition that may no longer be this one.
		ItemDef def = ItemDef.forID(item);
		int offset = def.modelOffset2;
		int[] bounds = yBounds(item);
		int centring = -((bounds[0] + bounds[1]) / 2) + offset;
		// The potion rule, restated here for the same reason as the rest of the arithmetic: a check
		// that called MakeX.dropOf would be checking the code against itself. Only "which items are
		// potions" is borrowed, because that is a policy rather than a sum.
		if (MakeX.isPotion(def)) {
			return centring * POTION_DROP_NUM / POTION_DROP_DEN * ICON_DROP_NUM / ICON_DROP_DEN;
		}
		return centring * ICON_DROP_NUM / ICON_DROP_DEN;
	}

	// -------------------------------------------------------------- background

	/**
	 * The panel the creation chatbox is drawn on.
	 *
	 * <p>{@code ChatArea} draws {@code ItemCraftingChatInterface/Chatboxbackground.png} solid behind
	 * a MakeX dialog and re-cuts the filter row from the ordinary chat art, which is the one visual
	 * the layout can be checked against outside the game. The file is checked where the client will
	 * load it, at the dialog's own 519x142: a panel of another size would be drawn over the filter
	 * row or leave a strip of the old one showing.
	 */
	private static void background() {
		System.out.println();
		System.out.println("the chatbox panel");
		String cache = findCache();
		File png = cache == null ? null
				: new File(cache, "Sprites/ItemCraftingChatInterface/Chatboxbackground.png");
		if (png == null || !png.isFile()) {
			check("background: " + (png == null ? "the cache" : png.getPath()) + " exists", false);
			return;
		}
		int w = -1;
		int h = -1;
		try {
			java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(png);
			if (image != null) {
				w = image.getWidth();
				h = image.getHeight();
			}
		} catch (Exception e) {
			System.out.println("  could not read " + png + ": " + e);
		}
		check("background: the creation panel is " + WIDTH + "x" + HEIGHT + " (found " + w + "x" + h
				+ ")", w == WIDTH && h == HEIGHT);
		check("background: it is drawn behind the dialog rather than the ordinary chat art "
				+ "(the gate MakeX.owns answers)", MakeX.owns(MakeX.CONTAINER_FIRST));
	}

	private static boolean isChildOf(RSInterface container, int id) {
		for (int i = 0; container.children != null && i < container.children.length; i++) {
			if (container.children[i] == id) {
				return true;
			}
		}
		return false;
	}

	private static int labelCount(int family) {
		return labelIndices(family).length;
	}

	/**
	 * What the layout's own labels say for a family, left to right.
	 *
	 * <p>Read off the label <em>widgets</em> rather than off children, because they are not children:
	 * the text is still what a name packet or the cache's own widgets put there, which is what the
	 * name routing checks are about. Whether they are drawn is {@link #visibleLabels}.
	 */
	private static String[] labels(int family) {
		int slots = SLOTS[family];
		String[] out = new String[slots];
		for (int slot = 0; slot < slots; slot++) {
			RSInterface label = RSInterface.interfaceCache[LABEL_FIRST + family * LABEL_STRIDE + slot];
			out[slot] = label == null ? null : label.message;
		}
		return out;
	}

	/**
	 * The container indices of a family's product labels: always none, because they are not
	 * children. Kept so the "no label is drawn" assertion reads the same way the drawn-widget
	 * helpers do.
	 */
	static int[] labelIndices(int family) {
		return visibleLabels(family);
	}

	/** Which amount {@code select} last armed, read back off the buttons rather than remembered. */
	private static int armed() {
		Sprite armed = armedSprite();
		for (int amount = 0; amount < AMOUNTS; amount++) {
			if (RSInterface.interfaceCache[MakeX.QUANTITY_FIRST + amount].sprite1 == armed) {
				return amount;
			}
		}
		return 0;
	}

	/**
	 * Where a child lands on the panel, which is its child coordinates plus the dialog's own draw
	 * offset. Every assertion about position goes through this: a child x on its own is 20 pixels
	 * left of where the player sees it, and checking the raw one is how the layout passed a
	 * "fits in the panel" assertion while drawing a pill outside it.
	 */
	static int drawnXOf(int containerId, int childId) {
		return rawXOf(childId, containerId) + DRAW_X;
	}

	/** The y twin of {@link #drawnXOf}. */
	static int drawnYOf(int containerId, int childId) {
		return rawYOf(childId, containerId) + DRAW_OFFSET;
	}

	private static int rawXOf(int childId, int containerId) {
		RSInterface menu = RSInterface.interfaceCache[containerId];
		for (int i = 0; i < menu.children.length; i++) {
			if (menu.children[i] == childId) {
				return menu.childX[i];
			}
		}
		return Integer.MIN_VALUE;
	}

	private static int rawYOf(int childId, int containerId) {
		RSInterface menu = RSInterface.interfaceCache[containerId];
		for (int i = 0; i < menu.children.length; i++) {
			if (menu.children[i] == childId) {
				return menu.childY[i];
			}
		}
		return Integer.MIN_VALUE;
	}

	/** A child's index in its container's list, which is its draw order, or -1. */
	static int childIndexOf(RSInterface menu, int childId) {
		for (int i = 0; i < menu.children.length; i++) {
			if (menu.children[i] == childId) {
				return i;
			}
		}
		return -1;
	}

	/** The index of the item model sharing a slot's x, or -1 if the slot has no model on it. */
	private static int modelIndexAt(RSInterface menu, int x, int slots) {
		int box = slotWidth(slots);
		for (int i = 0; i < menu.children.length; i++) {
			RSInterface child = RSInterface.interfaceCache[menu.children[i]];
			if (child != null && child.type != 5 && child.type != 4 && child.width == box
					&& child.height == SLOT_H && menu.childX[i] + DRAW_X == x) {
				return i;
			}
		}
		return -1;
	}

	// ------------------------------------------------- shared with MakeXPreview

	/** Where {@code --preview} writes. */
	static String previewDir() {
		return previewDir;
	}

	private static void check(String what, boolean ok) {
		if (ok) {
			passed++;
			System.out.println("  PASS  " + what);
		} else {
			failed++;
			System.out.println("  FAIL  " + what);
		}
	}

	private static String findCache() {
		String home = System.getProperty("user.home");
		String[] candidates = {
				"Biohazard.474",
				"Documents" + File.separator + "GitHub" + File.separator + "Biohazard.474",
				"Biohazard.474.pre-deathly",
		};
		for (int i = 0; i < candidates.length; i++) {
			File f = new File(home, candidates[i]);
			if (f.isDirectory() && new File(f, "main_file_cache.dat").isFile()) {
				return f.getPath();
			}
		}
		return null;
	}

	private MakeXProbe() {
	}
}
