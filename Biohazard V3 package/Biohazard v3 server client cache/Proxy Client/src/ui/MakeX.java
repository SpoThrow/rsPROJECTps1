package ui;

import def.ItemDef;
import model.Model;
import sign.signlink;

/**
 * The quantity-first creation chatbox: pick 1, 5, 10 or All, then click a product.
 *
 * <p><b>What it replaces.</b> Five cache interfaces draw the same thing — creation {@code 8938},
 * herblore {@code 4429}, fletching {@code 8880}, cooking {@code 1743} and smelting {@code 2400}.
 * Each is one row of products with four <em>invisible</em> text widgets stacked on the product as
 * the make 1/5/10/all hitboxes. There is no button art anywhere in the cache: what a player clicks
 * is four transparent rectangles carrying tooltips, and the only sprite any of the five draws is
 * {@code sworddecor}, the ornament at the two top corners. The panel behind them is not part of the
 * interface at all — it is {@code Sprites/Gameframe/chatarea.png}, drawn by the chat area before
 * the dialog and simply left visible underneath.
 *
 * <p><b>What it does instead.</b> The four hitboxes stay where they are and keep the widget ids the
 * server already reads; this class only changes where they sit and which one is live. A product
 * click therefore sends exactly the id it always did, so bringing a family into the new chatbox
 * costs no server change at all. See {@link #LEGACY} for which ids those are and why they may not
 * be renumbered.
 *
 * <p><b>Where the amount comes from.</b> The 1/5/10/All row is client-only: clicking it retargets
 * the live hitboxes and sends nothing. {@code client} answers its button ids in the {@code l == 315}
 * branch before packet 185 is ever built. The armed amount is <em>remembered</em>: it is a property
 * of the player's habit, not of the menu, so it survives closing and reopening a chatbox and is
 * shared across all five families. See {@link #select}.
 *
 * <p><b>The names.</b> The cache puts each product's name into one of that product's own buttons,
 * padded with line breaks to push it clear of the item model, so the name widget and a button are
 * the same widget. The client copies the name onto a label of its own and blanks the original,
 * which is why {@link #absorbName} sits on the string packet: it is what lets the name sit under
 * the icon instead of inside the click target.
 *
 * <p><b>The art.</b> Three placeholder files, all {@link #QUANT_W}x{@link #QUANT_H} or
 * {@link #SLOT_W}x{@link #SLOT_H}: {@code QUANTITY 0/1.png} are the idle and armed amount pills, and
 * {@code SLOT.png} is the frame drawn around each product. If a file is missing an equivalent is
 * filled in code, so nothing is ever invisible and a re-skin only has to replace these.
 */
public final class MakeX {

	/* ------------------------------------------------------------------ ids */

	/**
	 * The container each family is drawn in, in {@link #LEGACY} order.
	 *
	 * <p>They are the ids the server is told are open: {@link #substitute} rewrites packet 164 on
	 * the way in, so the server's idea of "interface 8938 is open" is never contradicted — it is
	 * only the client that draws something else.
	 */
	public static final int CONTAINER_FIRST = 51900;

	/**
	 * The amount row's buttons, in {@link #AMOUNT_LABELS} order.
	 *
	 * <p>These are the only ids here that never reach the server: {@code client} answers them and
	 * stops. They are on this side of the wire because an amount is a choice of how to make
	 * something, not a request to make it.
	 */
	public static final int QUANTITY_FIRST = 51980;

	/** The slot count every family's labels are laid out against; 2400 needs all eight. */
	private static final int SLOTS = 8;

	/**
	 * The frame drawn behind one product's icon, one per slot and per family.
	 *
	 * <p>Per family, because the width now depends on the family: smelting's eight boxes are 58
	 * wide and the rest are 64, and a widget is one widget — five families sharing eight ids would
	 * all be whatever the last one built, which is how a row of five got eight-wide boxes' width.
	 * The sprite is still shared, since it is the same art scaled by the box it draws in.
	 */
	public static final int SLOT_FRAME_FIRST = 52000;

	private static final int HELP_FIRST = 51920;
	private static final int LABEL_FIRST = 51930;
	private static final int QUANTITY_TEXT_FIRST = 51990;

	/* ------------------------------------------------------------- geometry */

	/**
	 * The panel is 519x142; the chat filter row is drawn at y 142, so nothing here may cross that.
	 *
	 * <p>These are the panel's own coordinates — where a thing ends up on screen — and placement
	 * subtracts {@link #DRAW_X} and {@link #DRAW_OFFSET} to get there. The whole layout is authored
	 * in this space on purpose: it is the space the reference screenshots were measured in, and the
	 * only one in which "inside the chatbox" means anything.
	 */
	private static final int WIDTH = 519;
	private static final int HEIGHT = 142;

	/**
	 * The region the client will build a dialog's hitboxes in, which every clickable thing this
	 * class places has to sit inside — being drawn inside the panel is not enough.
	 *
	 * <p>Three separate gates in {@code client} agree on it, and a widget outside any one of them is
	 * drawn but dead:
	 *
	 * <ul>
	 * <li>{@code buildMenu} only builds the open dialog's hitboxes at all while the mouse is inside
	 * the <em>message area</em>, a box that predates dialogs wanting the corner of the panel — hence
	 * {@link #CLICK_RIGHT} and {@link #CLICK_BOTTOM}, and {@code client.mouseOverChatDialog}, which
	 * is where that gate now says what it means;</li>
	 * <li>{@code buildInterfaceMenu} then requires the mouse to be inside the dialog's own rect,
	 * which the 20px draw offset puts at (20, 20) — hence {@link #CLICK_LEFT} and
	 * {@link #CLICK_TOP};</li>
	 * <li>and the chatbox's right-hand 16px is the message scrollbar
	 * ({@code client.mouseOverChatScrollbar}), which owns that strip even while a dialog is up.</li>
	 * </ul>
	 */
	private static final int CLICK_LEFT = 20;
	private static final int CLICK_RIGHT = 490;
	private static final int CLICK_TOP = 20;
	private static final int CLICK_BOTTOM = HEIGHT;

	/**
	 * The question, at the panel's top-left corner, and the help line under it.
	 *
	 * <p><b>Why the block starts 10 pixels lower than the art does.</b> {@code ChatArea} draws the
	 * dialog with {@code drawInterface(0, 20, menu, 20)}, and {@code drawInterface} clips the whole
	 * dialog to its own height starting at that 20. A child authored at 12 therefore has its top
	 * eight pixels cut away, which is precisely how the title and the pill art looked: letters and
	 * bevels sliced along their tops. Nothing here is authored above {@link #DRAW_OFFSET} any more.
	 *
	 * <p>The question is left-aligned in the corner rather than centred over the row: it is a
	 * caption for the row below it, and the reference screenshot has it hard against the top-left.
	 * It sits well right of that corner rather than on it: the play art's bevel eats into the first
	 * pixels and the caption read as touching the frame - at 14, and then still against it, so eight
	 * more pixels right to 22. The help line shares the column and moved with it.
	 *
	 * <p><b>Tunable, and why.</b> Everything this class lays out is a mouse-movable box in
	 * {@code tools/EditChatbox.bat}, which rewrites these declarations from the box the tool was
	 * dragged to. They are therefore plain static ints rather than constants: the tool edits the
	 * source text, so the values have to stay on one line each, in the shape
	 * {@code private static int NAME = VALUE;}, and the layout has to be readable from them at
	 * build time. Do not fold two onto a line or inline them into their uses.
	 */
	private static int TITLE_X = 22;
	private static int TITLE_Y = 17;
	private static int TITLE_H = 18;
	private static int HELP_Y = 32;
	private static int HELP_H = 12;

	/**
	 * The amount pills, in the top-right corner: a right-aligned block on the same two rows as the
	 * question and the help line, so it straddles the header rather than sitting under the items.
	 *
	 * <p><b>Measured, not chosen.</b> Taken off the reference screenshots, one of which shows the
	 * five-pill row this class draws:
	 *
	 * <ul>
	 * <li>pills at x 378, 419, 459, 499, 539 — a pitch of 40, widths 30/29/30/30/36, the widest
	 * being "All" because it is the only three-character caption;</li>
	 * <li>y 18..52, so 35 tall, starting level with the question and ending level with the help
	 * line;</li>
	 * <li>the rightmost pill ends at x 574 against a panel edge near 591, a margin of 17.</li>
	 * </ul>
	 *
	 * <p>Those are pixels in a 591-wide chatbox; this panel is 519, so they are scaled by 519/591
	 * = 0.878 and rounded. The one deliberate simplification is that the pills are a uniform width
	 * where the screenshots size each to its caption: one pair of sprites then serves the whole row,
	 * and 28 is wide enough for "All". Everything else is the measurement.
	 */
	private static int QUANT_W = 28;
	private static int QUANT_H = 31;
	private static int QUANT_GAP = 9;
	private static int QUANT_Y = 19;

	/**
	 * Where the pill block stops, which is {@link #CLICK_RIGHT} rather than the panel edge.
	 *
	 * <p>The screenshots put the block 17px from a 591-wide edge, which scales to 15 here — and 15
	 * is what this was, and it drew "All" five pixels past the panel and outside the region the
	 * client builds hitboxes for, so the one pill a player most wants was the one that could not be
	 * clicked. The chatbox's right-hand 16px is the message scrollbar, so the corner of the panel is
	 * not the chatbox's to use. See {@link #CLICK_RIGHT}.
	 */
	private static final int QUANT_RIGHT = CLICK_RIGHT;

	/** The caption's own height, which is what turns the pill's centre into a text top edge. */
	private static final int CAPTION_H = 11;
	/** One pixel down, because an 11px glyph sits a touch high in a 31px pill. */
	private static final int QUANT_TEXT_NUDGE = 1;

	/** The clear space between the pill block and the region the question is centred in. */
	private static final int TITLE_GAP = 6;

	/** One product's box: the framed slot, with the icon centred in it. */
	private static int SLOT_W = 64;
	private static int SLOT_H = 82;
	private static int SLOT_TOP = 51;
	private static int LABEL_H = 12;

	/**
	 * How large a product's icon is drawn, as a fraction of the item's own model zoom.
	 *
	 * <p>Every family used to leave this to the server, and each one sent a different zoom argument
	 * (creation 180, fletching 250, cooking 190, smelting 150), so the same item came out a
	 * different size in every menu and two items in one menu did not match. The item's own
	 * {@code modelZoom} is the one number that is already tuned per item to make it the right size
	 * next to every other item - it is what the inventory's 32px sprite is rendered from - so the
	 * icon is drawn from that and nothing else.
	 *
	 * <p><b>Why the fraction is under 1, and why it was wrong twice.</b>
	 * {@code modelZoom} is not a size, it is the <em>distance</em> the model is pushed away from
	 * the camera: {@code Model.method482} projects each vertex as {@code (coord << 9) / depth} and
	 * the depth carries the zoom. A larger zoom therefore draws a <em>smaller</em> icon, which is
	 * the opposite of what it looks like, and 3/2 - the inventory's own picked-up-item upscale -
	 * shrank every icon to about 21 pixels inside a box 82 tall. Multiplied the wrong way twice.
	 *
	 * <p>The inventory's 32px sprite is rendered at the item's plain {@code modelZoom}, and a box
	 * here is 82 tall, so a zoom of {@code 32/82 = 0.39} of it makes the icon span the box
	 * exactly. 3/8 - 0.375, a shade past the box, so icons were trimmed on their edges - is what
	 * this was, and it is four times the size 3/2 gave: the "four or five times bigger, filling the
	 * container" this was asked for. {@code client}'s type 6 branch clips an icon to its own box
	 * ({@code MakeX.owns}), so overshoot is trimmed rather than spilled; that clip is what makes a
	 * value picked this way safe.
	 *
	 * <p><b>15/28 is 3/8 taken 30% down.</b> The size an icon is drawn at is {@code 32 / fraction},
	 * so a fraction 10/7 of 3/8 makes every icon 30% smaller than the one that filled the box, with
	 * room to see the frame around it: 85 pixels become 60 in a box 82 tall. The pair is written as
	 * the two integers the editor writes back rather than as a decimal.
	 */
	private static int ICON_ZOOM_NUM = 15;
	private static int ICON_ZOOM_DEN = 28;

	/**
	 * How far down a product's icon is dropped inside its box, as a fraction of the shift that
	 * centres the model's own vertices on the box.
	 *
	 * <p><b>Why an icon needs dropping at all.</b> A model's vertices do not have to be centred on
	 * its own origin, and a potion's are not: the whole bottle sits <em>above</em> y 0, so drawing it
	 * around the box's centre puts the box's upper half on the bottle and leaves the lower half
	 * empty, with the cork over the frame. {@code ItemDef.getSprite} has always corrected for this -
	 * it hands {@code method482} half the model's height as a downward translation, which is the
	 * centring term - but the type 6 branch that draws an icon in a widget passes no translation at
	 * all, so every interface that shows an item has been drawing it as authored. Symmetric models
	 * hide it: their correction is zero, which is why only the potion menus ever looked wrong.
	 *
	 * <p>So the correction is computed per item from the model it is about to draw ({@link
	 * #dropOf}) and applied by {@code client}'s type 6 branch through {@link #iconDropY}. Full
	 * correction is 1/1; the fraction exists because a model's skirt can be much wider than its body
	 * and centring by the bounding box is not always what looks centred, and because the rotation a
	 * menu sends tilts the model into its own depth, which no single number can undo.
	 */
	private static int ICON_DROP_NUM = 1;
	private static int ICON_DROP_DEN = 1;

	/**
	 * How much further down a <em>potion</em> is dropped than the centring above asks for, as a
	 * fraction of that centring term - the potion rule, and by default a third further.
	 *
	 * <p><b>Why potions want more than centring.</b> Centring is computed from the model's vertices,
	 * and a bottle's vertices are not its picture. A flask's topmost vertices are the rim of the neck
	 * and the inside of the glass, which draw little or nothing at the icon's size, while its widest,
	 * most visible mass - the body - hangs below them, so centring the vertices centres something
	 * taller than the bottle that is drawn. The bottle that is drawn therefore ends up sitting high in
	 * its box: the complaint this answers. Measured against the box art, the frame draws a fine line
	 * near its bottom that a bottle is meant to stand on, and a quarter further left the flask's base
	 * about seven pixels clear of it; a third lands it two or three, which is standing on the line
	 * without touching it.
	 *
	 * <p><b>Why this is a separate fraction and not a tweak to {@link #ICON_DROP_NUM}.</b> Every other
	 * product - a dagger, a fish, an ore - is drawn where centring puts it and is right there; raising
	 * the general term would move all of them to fix one shape. Bottles are also the family this came
	 * up for, and the ones a herblore menu is nothing but.
	 *
	 * <p><b>What counts as a bottle.</b> See {@link #isPotion}: a name that says potion, a brew, a
	 * serum or a mixture, or a model drawn from one of the four dose bottles - the second half is what
	 * catches the drinks named after their stat rather than after being a potion.
	 */
	private static int POTION_DROP_NUM = 50;
	private static int POTION_DROP_DEN = 3;

	/**
	 * The clear space between neighbouring product boxes.
	 *
	 * <p>Boxes touching edge to edge read as one strip, and a name longer than its box ran straight
	 * into the next product's name. A gap of six is enough to separate them and to let the truncation
	 * below be about the name being long rather than about the boxes being narrow.
	 */
	private static int SLOT_GAP = 6;

	/**
	 * Where a child is parked when its product is not there.
	 *
	 * <p>A widget with no size draws nothing anyway, but a widget that is merely zero-sized can still
	 * be drawn at a coordinate that overlaps a live one, and it is cheaper to move the unwanted ones
	 * entirely outside the clip than to reason about whether zero pixels at the right place are
	 * invisible. This is the panel's left edge less than its width, which is never on screen.
	 */
	private static final int OFF_PANEL = -WIDTH;

	/**
	 * The dialog is drawn at (20, 20), not (0, 0): {@code ChatArea} calls
	 * {@code drawInterface(0, 20, menu, 20)}, whose second argument is added to every child's x
	 * exactly as its fourth is added to every child's y. So a child's x is its panel x less
	 * {@link #DRAW_X} and its y is its chat-area y less this — and the hit test offsets by the same
	 * pair, which is why its x is 20 in {@code buildInterfaceMenu} and its y is
	 * {@code chatDrawY() + 20}.
	 *
	 * <p>This is what {@link #childX} and {@link #childY} subtract, and they have to subtract
	 * <em>both</em>: y did and x did not, so every x this class chose landed 20 pixels to the right
	 * of where it meant. The block of {@link #CLICK_LEFT} is the same 20 pixels seen from the
	 * click side — a dialog cannot build a hitbox in the band its own inset eats.
	 */
	private static final int DRAW_OFFSET = 20;

	/** The x the whole dialog is shifted by, the twin of {@link #DRAW_OFFSET}. */
	private static final int DRAW_X = 20;

	/**
	 * The panel remaps two inks before it draws text ({@code client}'s type 4 branch, under
	 * {@code DrawingArea.width == 519}): yellow becomes blue and 49152 becomes white. So the armed
	 * caption is white, not the yellow a first guess suggests.
	 */
	private static final int HELP_INK = 0x604020;
	private static final int LABEL_INK = 0x000000;
	private static final int QUANTITY_INK = 0x000000;
	private static final int QUANTITY_INK_ON = 0xFFFFFF;

	/* ---------------------------------------------------------------- tables */

	/**
	 * The cache interface each container stands in for, in the order every table below uses.
	 *
	 * <p>The ids are the interface's own, not invented: {@code client} substitutes only these five,
	 * so a sixth family is a row here and nowhere else.
	 */
	private static final int[] LEGACY = { 8938, 4429, 8880, 1743, 2400 };

	/** The frame the server writes each menu's question into, so it can be centred. */
	private static final int[] TITLE_FRAME = { 8966, 2800, 8898, 13721, 2403 };

	/** The item model frames, one per product, in row order, as the server writes them. */
	private static final int[][] MODEL_FRAMES = {
			{ 8941, 8942, 8943, 8944, 8945 },
			{ 1746 },
			{ 8883, 8884, 8885 },
			{ 13716 },
			{ 2405, 2406, 2407, 2409, 2410, 2411, 2412, 2413 },
	};

	/**
	 * The frame each product's name arrives in, one per product, in row order.
	 *
	 * <p>Four of the five write it into the product's own make-one button. Cooking does not — it
	 * writes into the make-all button ({@code 13717}) — and smelting never sends a name at all,
	 * because its eight are baked into the cache's widgets. Both are read back out by
	 * {@link #absorbName} or {@link #seedLabels}, so all five end up on a label of this class's own
	 * either way.
	 */
	private static final int[][] NAME_FRAMES = {
			{ 8949, 8953, 8957, 8961, 8965 },
			{ 2799 },
			{ 8889, 8893, 8897 },
			{ 13717 },
			{ 3987, 3991, 3995, 3999, 4003, 7441, 7446, 7450 },
	};

	/**
	 * Each product's four make buttons, in the order 1 / 5 / 10 / all.
	 *
	 * <p>These are cache widget ids and they are the whole coupling to the server: the server
	 * decodes a clicked widget id with {@code Misc.hexToInt}, reading its two bytes as
	 * {@code high * 1000 + low}, which is how a click on 8949 arrives as 34245. Nothing here may be
	 * renumbered without the server's tables moving with it.
	 *
	 * <p>Creation and fletching number a row's four buttons consecutively with make-all first
	 * ({@code all, 10, 5, 1}); the other three do not, so those are written out. Smelting's are the
	 * eight columns of {@code Smelting.buttons}, read back to front.
	 */
	private static final int[][] AMOUNT_IDS = {
			strideAmounts(8946, 5),
			{ 2799, 2798, 1748, 1747 },
			strideAmounts(8886, 3),
			{ 13720, 13719, 13718, 13717 },
			{
					3987, 3986, 2807, 2414,
					3991, 3990, 3989, 3988,
					3995, 3994, 3993, 3992,
					3999, 3998, 3997, 3996,
					4003, 4002, 4001, 4000,
					7441, 7440, 6397, 4158,
					7446, 7444, 7443, 7442,
					7450, 7449, 7448, 7447,
			},
	};

	/**
	 * What each of a product's four buttons asks the server for, in the interface's own order.
	 *
	 * <p>28 is not a spellable number: the interface's fourth button means "as many as the pack
	 * holds", and 28 is the inventory, which is how the server reads it.
	 */
	private static final int[] SERVER_AMOUNTS = { 1, 5, 10, 28 };

	/** The row's captions, left to right. */
	private static final String[] AMOUNT_LABELS = { "1", "5", "10", "All" };

	/** Where make-all sits in {@link #AMOUNT_LABELS}. */
	public static final int ALL_INDEX = 3;

	/**
	 * Which of a product's four server buttons carries each entry of the row, left to right.
	 *
	 * <p>The identity, now. Client row and server row are the same four in the same order -
	 * {@code 1, 5, 10, all}. When the custom X entry existed it had to borrow the make-all button as
	 * a click target, because a typed number cannot ride on a button id and the id the click carried
	 * was never forwarded. With X gone there is nothing to borrow and nothing to translate.
	 */
	private static final int[] SERVER_SLOT = { 0, 1, 2, 3 };

	/** The row's help line. */
	private static final String HELP_TEXT = "Choose a quantity, then click an item to begin.";

	/* ---------------------------------------------------------------- state */

	/**
	 * Which entry of {@link #AMOUNT_LABELS} the next product click will ask for.
	 *
	 * <p>Deliberately static and deliberately not reset when a menu opens: a player who has just
	 * made ten of something means to make ten of the next thing too. It lives for the session and
	 * is shared by all five families, so a preference set at a furnace is still there at a loom.
	 */
	private static int selected;

	/**
	 * The number of products each family actually has something to make, read off the cache widgets
	 * the server fills rather than off the tables above.
	 *
	 * <p>The tables say how many boxes a family <em>can</em> have - five for creation, eight for
	 * smelting - and the cache fills the unused ones with item {@code -1}. A row that still drew
	 * those was five boxes when the player had one uncut gem: four of them dead, and a row that read
	 * as a layout bug rather than as "cut your gem". This is how many are real, and it is what the
	 * layout, the hitboxes and the centring are all computed from. See {@link #recount}.
	 */
	private static final int[] liveCount = new int[LEGACY.length];

	/**
	 * How far down each product's icon has to be drawn, in model units, per family and slot: the
	 * negative of the middle of that item's own model, see {@link #dropOf}. Worked out by
	 * {@link #iconDropY} the first time a slot is drawn with its model loaded, because the model is
	 * fetched after the packet that names the item; read by {@code client}'s type 6 branch through
	 * the same method, which is the only caller.
	 */
	private static final int[][] iconDrop = iconDropTable();

	/**
	 * The item each slot's {@link #iconDrop} was measured for, or zero when there is nothing to
	 * trust in that slot yet - a cleared slot, or one whose model had not arrived when it was last
	 * drawn. Slot and item together are the key: an icon is only re-measured when the item in it
	 * changes.
	 */
	private static final int[][] iconDropItem = iconDropTable();

	private static int[][] iconDropTable() {
		int[][] table = new int[MODEL_FRAMES.length][];
		for (int family = 0; family < MODEL_FRAMES.length; family++) {
			table[family] = new int[MODEL_FRAMES[family].length];
		}
		return table;
	}

	/** Set once, by {@link #build}, for the log line that says how the row was laid out. */
	private static boolean started;

	private static Sprite quantityOff;
	private static Sprite quantityOn;
	/**
	 * The frame art itself, loaded once while the media cache is alive.
	 *
	 * <p>Deliberately not reloaded by {@link #recount}: the loader {@code imageLoader} reaches for is
	 * torn down at the end of {@code RSInterface.unpack}, and a recount happens long after that, when
	 * a menu opens. Reloading there would throw rather than draw, which is exactly what a row whose
	 * width changes at runtime - smelting with fewer than eight products - would hit. One base sprite
	 * plus {@link #scaleX} is all a width change needs.
	 */
	private static Sprite slotBase;
	/**
	 * One frame per family, because the art has to be the width of that family's box: the renderer
	 * draws a sprite at its own size and ignores the widget's width, so a single 64-wide SLOT.png
	 * would overlap its neighbour on a row of eight 53-wide boxes.
	 *
	 * <p>Rebuilt by {@link #recount} whenever a family's live count changes the width, which is why
	 * the width it was made for is kept next to it - rescaling art is not free and most recounts
	 * do not change it.
	 */
	private static final Sprite[] slotFrames = new Sprite[LEGACY.length];
	private static final int[] slotFrameWidth = new int[LEGACY.length];

	/**
	 * The four fonts the client unpacks, kept so that widgets built here can share them. The
	 * renderer picks a font by identity against those four, so a font from anywhere else would
	 * draw nothing at all.
	 */
	private static TextDrawingArea[] fonts;

	private MakeX() {
	}

	private static int[] strideAmounts(int allFrame, int slots) {
		int[][] rows = new int[slots][];
		for (int slot = 0; slot < slots; slot++) {
			int all = allFrame + slot * 4;
			rows[slot] = new int[] { all + 3, all + 2, all + 1, all };
		}
		return flatten(rows);
	}

	/** Java has no int[][] splat, so the stride helper builds one dimension at a time. */
	private static int[] flatten(int[][] rows) {
		int[] flat = new int[rows.length * 4];
		for (int slot = 0; slot < rows.length; slot++) {
			System.arraycopy(rows[slot], 0, flat, slot * 4, 4);
		}
		return flat;
	}

	/** The widget of one product's button for one of the SERVER's amounts. */
	private static int serverButton(int family, int slot, int serverSlot) {
		return AMOUNT_IDS[family][slot * 4 + serverSlot];
	}

	/** Which of a product's four server buttons the armed row entry asks for. */
	private static int serverSlot() {
		return SERVER_SLOT[selected];
	}

	private static int slotsIn(int family) {
		return AMOUNT_IDS[family].length / 4;
	}

	/* --------------------------------------------------------------- build */

	/**
	 * Builds the five containers, once, after the cache's own interfaces have been unpacked.
	 *
	 * <p>Called from {@code Interfaces.loadInterfaces}, which is the first moment the widgets this
	 * class rearranges and reparents exist.
	 */
	public static void build(TextDrawingArea[] buildFonts) {
		fonts = buildFonts;
		quantityOff = quantityChrome(0, false);
		quantityOn = quantityChrome(1, true);
		// The slot art, while aMRUNodes_238 still exists: unpack nulls it on the way out, so this is
		// the only moment it can be read. See the field for why recount must not reload it.
		slotBase = loadSlotBase();
		for (int family = 0; family < LEGACY.length; family++) {
			// Laid out from the tables first - the widest case, so nothing is ever missing - and
			// corrected to the server's real row by recount() when a menu opens. The width the
			// frame art was made for is kept so recount() can tell when it has to rescale.
			liveCount[family] = slotsIn(family);
			int width = slotWidth(liveCount[family]);
			slotFrames[family] = frameChrome(width);
			slotFrameWidth[family] = width;
			buildFamily(family);
		}
		applySelection();
		if (!started) {
			started = true;
			System.out.println("MakeX: " + LEGACY.length + " quantity-first chatboxes built, "
					+ AMOUNT_LABELS.length + " amounts each (" + AMOUNT_LABELS[selected] + " armed)");
		}
	}

	private static void buildFamily(int family) {
		int containerId = CONTAINER_FIRST + family;
		int slots = slotsIn(family);
		RSInterface legacy = RSInterface.interfaceCache[LEGACY[family]];

		RSInterface menu = RSInterface.addInterface(containerId);
		menu.type = 0;
		menu.width = WIDTH;
		menu.height = HEIGHT;
		menu.isMouseoverTriggered = false;

		int carried = countClaimed(family, legacy);
		// One slot frame per product, drawn first so the item model lands on top of it, plus the
		// question, a pill and caption per amount. Product names are no longer a child of the
		// container: the item's own icon says what it is and the label only cluttered the box.
		int total = carried + slots + 1 + AMOUNT_LABELS.length * 2;
		RSInterface.setChildren(total, menu);

		int next = 0;
		next = addSlotFrames(family, containerId, menu, next, slots);

		for (int i = 0; i < legacy.children.length; i++) {
			int id = legacy.children[i];
			// Only the widgets the tables name are carried over, and every one of them is placed
			// by this class rather than left where the cache put it.
			//
			// What is deliberately NOT carried is the rest of that interface, which for all five
			// of them is the two sworddecor ornaments the old row drew in its top corners. They
			// are decoration for a layout that no longer exists: left where the cache put them
			// they land across the header and the pill block. Dropping them is the safe half of
			// the choice - a widget that is not a child of anything is not drawn, and no id here
			// is one the server ever reads.
			int slot = slotOf(family, MODEL_FRAMES, id);
			if (slot >= 0) {
				RSInterface model = RSInterface.interfaceCache[id];
				model.parentID = containerId;
				model.width = slotWidth(slots);
				// The model widget IS the icon's box: the renderer centres the model on the
				// widget's own width and height, so a widget the size of the whole slot is what
				// puts the icon in the middle of the container rather than in the top strip of it.
				model.height = SLOT_H;
				model.anInt263 = 0;
				model.anInt265 = 0;
				menu.child(next++, id, childX(slotX(slots, slot)), childY(SLOT_TOP));
				continue;
			}
			if (isAmountButton(family, id)) {
				// The four stacked buttons of one product all share one box; which of them is
				// live is applySelection's job, not this one's.
				RSInterface box = RSInterface.interfaceCache[id];
				box.parentID = containerId;
				box.centerText = true;
				box.anInt263 = 0;
				box.anInt265 = 0;
				menu.child(next++, id, childX(slotX(slots, slotOfAmount(family, id))), childY(SLOT_TOP));
				continue;
			}
			if (id == TITLE_FRAME[family]) {
				RSInterface title = RSInterface.interfaceCache[id];
				title.parentID = containerId;
				// Left-aligned in the corner, not centred over the row: the question is the row's
				// caption, and the reference screenshot has it hard against the top-left.
				title.width = WIDTH - TITLE_X;
				title.height = TITLE_H;
				title.centerText = false;
				title.anInt263 = 0;
				title.anInt265 = 0;
				menu.child(next++, id, childX(TITLE_X), childY(TITLE_Y));
			}
		}

		next = addHelp(family, containerId, menu, next);

		// The name widgets still exist so absorbName/seedLabels have somewhere to put the text they
		// pull off the cache's own buttons, but they are not children of the container and so draw
		// nothing. Recipe names in the box were reported as clutter; the icon is the label now.
		for (int slot = 0; slot < slots; slot++) {
			int id = labelId(family, slot);
			RSInterface.addText(id, " ", fonts, 1, LABEL_INK, true, true);
			RSInterface label = RSInterface.interfaceCache[id];
			label.parentID = containerId;
			label.width = slotWidth(slots);
			label.height = LABEL_H;
		}
		seedLabels(family, slots);

		next = addQuantityRow(containerId, menu, next);

		if (next != total) {
			// A table and the cache disagree: the layout would draw something nowhere. Fail loudly
			// on the console rather than silently dropping the tail of the children list.
			signlink.reporterror("MakeX: family " + family + " wanted " + total
					+ " children but placed " + next);
		}
	}

	/**
	 * How many of a cache interface's children the tables claim, which is how many the row will
	 * carry over.
	 *
	 * <p>Counted rather than assumed: the count is what {@link RSInterface#setChildren} is given, so
	 * a table that stops matching the cache turns into the child-count mismatch reported at the end
	 * of {@link #buildFamily} instead of a half-built row.
	 */
	private static int countClaimed(int family, RSInterface legacy) {
		if (legacy == null || legacy.children == null) {
			return 0;
		}
		int count = 0;
		for (int i = 0; i < legacy.children.length; i++) {
			if (carried(family, legacy.children[i])) {
				count++;
			}
		}
		return count;
	}

	/** Whether the tables name this cache child: a product model, a make button, or the question. */
	private static boolean carried(int family, int id) {
		return slotOf(family, MODEL_FRAMES, id) >= 0 || isAmountButton(family, id)
				|| id == TITLE_FRAME[family];
	}

	/** The frame drawn behind one product's icon, per family because its width is. */
	public static int slotFrameId(int family, int slot) {
		return SLOT_FRAME_FIRST + family * SLOTS + slot;
	}

	/** One framed box per product, behind the icon, on the slot row. */
	private static int addSlotFrames(int family, int containerId, RSInterface menu, int next, int slots) {
		for (int slot = 0; slot < slots; slot++) {
			int id = slotFrameId(family, slot);
			RSInterface box = RSInterface.addInterface(id);
			box.type = 5;
			box.atActionType = 0;
			box.contentType = 0;
			box.mOverInterToTrigger = -1;
			box.width = slotWidth(slots);
			box.height = SLOT_H;
			box.parentID = containerId;
			box.sprite1 = slotFrames[family];
			box.sprite2 = slotFrames[family];
			menu.child(next++, id, childX(slotX(slots, slot)), childY(SLOT_TOP));
		}
		return next;
	}

	private static int addHelp(int family, int containerId, RSInterface menu, int next) {
		int id = HELP_FIRST + family;
		RSInterface.addText(id, HELP_TEXT, fonts, 0, HELP_INK, false, true);
		RSInterface help = RSInterface.interfaceCache[id];
		help.parentID = containerId;
		help.width = WIDTH - TITLE_X;
		help.height = HELP_H;
		menu.child(next++, id, childX(TITLE_X), childY(HELP_Y));
		return next;
	}

	private static int addQuantityRow(int containerId, RSInterface menu, int next) {
		for (int amount = 0; amount < AMOUNT_LABELS.length; amount++) {
			int x = childX(quantityX(amount));
			int buttonId = QUANTITY_FIRST + amount;
			RSInterface button = RSInterface.addInterface(buttonId);
			button.type = 5;
			button.atActionType = 1;
			button.contentType = 0;
			button.mOverInterToTrigger = -1;
			button.width = QUANT_W;
			button.height = QUANT_H;
			button.tooltip = tooltipFor(amount);
			button.sprite1 = quantityOff;
			button.sprite2 = quantityOn;
			button.parentID = containerId;
			menu.child(next++, buttonId, x, childY(QUANT_Y));

			int textId = QUANTITY_TEXT_FIRST + amount;
			// The caption is drawn by its own widget rather than baked into the pill, so that the
			// armed pill can ink it white without needing a second pair of pill sprites.
			RSInterface.addText(textId, AMOUNT_LABELS[amount], fonts, 1, QUANTITY_INK, true, true);
			RSInterface text = RSInterface.interfaceCache[textId];
			text.parentID = containerId;
			text.width = QUANT_W;
			text.height = CAPTION_H;
			// A text widget is drawn from its top, so the pill's centre is turned into a top edge
			// here rather than by eye: (QUANT_H - CAPTION_H) / 2 IS the centring.
			int captionTop = QUANT_Y + (QUANT_H - CAPTION_H) / 2 + QUANT_TEXT_NUDGE;
			menu.child(next++, textId, x, childY(captionTop));
		}
		return next;
	}

	private static String tooltipFor(int amount) {
		if (amount == ALL_INDEX) {
			return "Make All";
		}
		return "Make " + AMOUNT_LABELS[amount];
	}

	/* --------------------------------------------------------------- layout */

	/**
	 * Where a child of this class's layout goes: the layout is authored in the panel's own
	 * 0..{@link #WIDTH} by 0..{@link #HEIGHT} space and the dialog is drawn and hit-tested shifted
	 * by ({@link #DRAW_X}, {@link #DRAW_OFFSET}), so both are subtracted in one place here.
	 *
	 * <p>This was two places and only one of them was right: y subtracted its offset and x did not,
	 * which put every x this class chose 20 pixels to the right of where it meant, off the panel.
	 */
	private static int childX(int x) {
		return x - DRAW_X;
	}

	/** The y twin of {@link #childX}. */
	private static int childY(int y) {
		return y - DRAW_OFFSET;
	}

	/** The width of the amount block down the right-hand side. */
	private static int quantityBlockWidth() {
		return AMOUNT_LABELS.length * QUANT_W + (AMOUNT_LABELS.length - 1) * QUANT_GAP;
	}

	private static int quantityLeft() {
		return QUANT_RIGHT - quantityBlockWidth();
	}

	/** The x of one pill, left to right with an even gap. */
	private static int quantityX(int amount) {
		return quantityLeft() + amount * (QUANT_W + QUANT_GAP);
	}

	/**
	 * How wide the region the question and help line are centred in: everything left of the pills.
	 *
	 * <p>This is the one piece of the reference layout that is easy to get wrong, because centring
	 * on the full 519 looks almost right and is tens of pixels out. Measured: across the eight
	 * screenshots the question and the help line share a centre, and that centre sits left of the
	 * panel's by about half the pill block.
	 */
	private static int titleRight() {
		return quantityLeft() - TITLE_GAP;
	}

	/**
	 * How wide one product's box is: {@link #SLOT_W}, or less when the row has more products than
	 * the click region can give 64 each plus the gaps between them.
	 *
	 * <p>Only a full row of eight is affected - 8 x 53 against a region 470 wide, which leaves the
	 * 42 pixels the seven gaps need - and it is the case that made this a function rather than a
	 * constant: at 64 a row of eight is 512 wide, which is a whole 22 pixels wider than the panel,
	 * and the last product was drawn over the edge of it with half of its box outside anything the
	 * client would hit-test.
	 *
	 * <p>Zero is not a row: it is "nothing to make yet", the state between the dialog opening and
	 * the first model frame arriving, and it has no width to divide by.
	 */
	private static int slotWidth(int slots) {
		if (slots <= 0) {
			return SLOT_W;
		}
		int span = CLICK_RIGHT - CLICK_LEFT - (slots - 1) * SLOT_GAP;
		return Math.min(SLOT_W, span / slots);
	}

	/**
	 * The x of a slot: the row is centred in the click region with an even pitch, so it stays
	 * centred and fully clickable for any product count.
	 *
	 * <p>The pitch is the box plus {@link #SLOT_GAP}, and the centring subtracts the gaps as well as
	 * the boxes: centring on the boxes alone leaves the row half a gap short on the right, which is
	 * the sort of two-pixel error that reads as "slightly off".
	 */
	private static int slotX(int slots, int slot) {
		int span = CLICK_RIGHT - CLICK_LEFT;
		int width = slotWidth(slots);
		int used = slots * width + (slots - 1) * SLOT_GAP;
		return CLICK_LEFT + (span - used) / 2 + slot * (width + SLOT_GAP);
	}

	private static int labelId(int family, int slot) {
		return LABEL_FIRST + family * SLOTS + slot;
	}

	/** The index of {@code id} in this family's slice of a per-slot table, or -1. */
	private static int slotOf(int family, int[][] table, int id) {
		int[] frames = table[family];
		for (int slot = 0; slot < frames.length; slot++) {
			if (frames[slot] == id) {
				return slot;
			}
		}
		return -1;
	}

	/** Whether {@code id} is one of a family's own make buttons, and the product it belongs to. */
	private static boolean isAmountButton(int family, int id) {
		return slotOfAmount(family, id) >= 0;
	}

	/**
	 * Whether a widget is one of the item frames this class lays a product's icon in, and so is a
	 * widget whose drawing {@code client}'s type 6 branch has to correct.
	 *
	 * <p><b>Why this predicate exists separately from {@link #owns}.</b> {@code owns} answers "is this
	 * chatbox open" and is asked with the <em>container's</em> id, because that is what the client
	 * opens as the dialog. The icons are children of that container, so a gate on "is this one of my
	 * dialogs" answers no for every icon - which is what the type 6 branch asked, and why the drop
	 * below had no effect in the client while a probe calling {@link #iconDropY} directly said it
	 * worked. This asks the question the branch actually has: is this one of my item frames?
	 *
	 * <p>The dialogue chat heads are the reason the question has to be asked by identity rather than
	 * by "is an item": a head is a type 6 widget too, and it is left exactly as the cache drew it.
	 */
	public static boolean isProductFrame(int widgetId) {
		for (int family = 0; family < MODEL_FRAMES.length; family++) {
			if (slotOf(family, MODEL_FRAMES, widgetId) >= 0) {
				return true;
			}
		}
		return false;
	}

	private static int slotOfAmount(int family, int id) {
		int[] ids = AMOUNT_IDS[family];
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == id) {
				return i / 4;
			}
		}
		return -1;
	}

	/* ------------------------------------------------------------ selection */

	/**
	 * Arms one of the row's amounts. This is the whole of what the amount row does.
	 *
	 * <p>The choice is remembered for the rest of the session and is not per-menu: see
	 * {@link #selected}.
	 *
	 * @param index into {@link #AMOUNT_LABELS} order: 0 is make one, {@link #ALL_INDEX} is make all
	 */
	public static void select(int index) {
		if (index < 0 || index >= AMOUNT_LABELS.length) {
			return;
		}
		selected = index;
		applySelection();
	}

	/** Which amount is armed, as an index into {@link #AMOUNT_LABELS}. */
	public static int selected() {
		return selected;
	}

	/* ------------------------------------------------------- live row count */

	/**
	 * How many of a family's slots the server has actually put a product in.
	 *
	 * <p>Read off the cache widgets rather than remembered: packet 246 sets {@code anInt233} to the
	 * item's render kind and to {@code 0} when the item is {@code -1}, so a slot with no product is
	 * exactly a model frame whose {@code anInt233} is zero. {@code mediaID} is deliberately not
	 * tested - the blank branch leaves the previous item's id in place, so it says what the slot
	 * used to hold rather than what it holds now.
	 */
	static int filledSlots(int family) {
		int slots = slotsIn(family);
		int count = 0;
		for (int slot = 0; slot < slots; slot++) {
			RSInterface model = RSInterface.interfaceCache == null ? null
					: RSInterface.interfaceCache[MODEL_FRAMES[family][slot]];
			if (model != null && model.anInt233 != 0) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Follows the model frames the server just wrote and re-lays out the family whose row changed.
	 *
	 * <p>Called for every model frame, not only a family's, because the caller is the packet handler
	 * and does not know which family a frame belongs to. Ignoring the ones that are not ours is what
	 * keeps this a sentence rather than a registration.
	 */
	public static void onModelUpdate(int frameId) {
		for (int family = 0; family < LEGACY.length; family++) {
			int slot = slotOf(family, MODEL_FRAMES, frameId);
			if (slot >= 0) {
				applyIcon(family, slot, frameId);
				recount(family);
				return;
			}
		}
	}

	/**
	 * Forces a product's icon to the item's own size, so every item in every menu matches.
	 *
	 * <p>Packet 246 sets {@code modelZoom = itemDef.modelZoom * 100 / <server zoom>}, and each
	 * family sends its own zoom argument, so the same item was a different size in every menu and
	 * two items in one menu did not agree. {@code itemDef.modelZoom} is the number the inventory's
	 * sprite is rendered from - already tuned per item so all items look the right size next to
	 * each other - so the server's argument is discarded and the icon rebuilt from it.
	 *
	 * <p>Only called for a slot the server has filled ({@code anInt233 != 0}); {@code mediaID} is
	 * left over from the last item when a slot is emptied, so it must not be trusted on its own.
	 *
	 * <p>Nothing is worked out about the model's shape here, though a slot has just been filled:
	 * the write happens when the packet arrives, which is before the model has been fetched, so
	 * every measurement taken now reads a model that is not there. {@link #iconDropY} takes it at
	 * draw time instead, when the model the renderer is about to draw is necessarily loaded.
	 */
	private static void applyIcon(int family, int slot, int frameId) {
		RSInterface model = RSInterface.interfaceCache == null ? null
				: RSInterface.interfaceCache[frameId];
		if (model == null || model.anInt233 == 0 || model.mediaID <= 0) {
			return;
		}
		ItemDef def = ItemDef.forID(model.mediaID);
		if (def == null) {
			return;
		}
		if (def.modelZoom > 0) {
			model.modelZoom = def.modelZoom * ICON_ZOOM_NUM / ICON_ZOOM_DEN;
		}
		// Whatever was measured for the item that was here is no longer this slot's answer, and a
		// packet is also how the server changes a product's rotation, which the measurement feeds on.
		iconDropItem[family][slot] = 0;
	}

	/**
	 * The downward translation, in model units, that puts an item's model in the middle of the box
	 * it is drawn in: the negative of the middle of the model's own vertices, plus the item's own
	 * offset, scaled by {@link #ICON_DROP_NUM}. Null when the item's model has not been fetched yet.
	 *
	 * <p>{@code method482} adds its y translation to every vertex before projecting, and the type 6
	 * branch passes none, so the model is drawn around its own origin whatever that origin is
	 * relative to the shape. Reading the bounds off the model the renderer will actually draw
	 * ({@code method202} - the raw one, not the scaled and lit one the inventory sprite uses) is
	 * therefore the whole correction, and it is zero for the models - nearly all of them - that are
	 * authored around their own middle.
	 *
	 * <p>This is {@code ItemDef.getSprite}'s own term. That has always passed
	 * {@code model.modelHeight / 2} as the translation {@code method482} is handed, which for a
	 * model standing on its own origin - the way a bottle is authored, base at y 0 and cork above -
	 * is the same number as the middle of the range; the range is used rather than {@code
	 * modelHeight} so that a model with anything below its origin is centred too. {@code
	 * modelOffset2} is the item's own fudge for the cases the bounds do not cover, and the
	 * inventory's sprite includes it, so this does too.
	 *
	 * <p><b>Why "not fetched yet" is not the same as zero.</b> {@code Model.method462} returns null
	 * for a model the client has not been sent, and asks for it; zero is a legitimate answer for a
	 * model that is already centred. Reporting both as zero would let the first frame's answer -
	 * taken before the fetch - stand for the life of the menu, which is the whole defect, so the
	 * caller gets told which of the two it is.
	 *
	 * <p>A potion gets more than this centring, by {@link #POTION_DROP_NUM}: its vertices are a
	 * taller shape than it draws, so the term above lands the bottle high. See that constant for why
	 * the extra is a rule of its own rather than a nudge to the general one.
	 */
	static Integer dropOf(ItemDef def) {
		if (def == null) {
			return Integer.valueOf(0);
		}
		Model model = def.method202(1);
		if (model == null) {
			return null;
		}
		int[] ys = model.vertexYs();
		if (ys == null || model.vertexCount() <= 0) {
			return Integer.valueOf(0);
		}
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		int n = Math.min(model.vertexCount(), ys.length);
		for (int i = 0; i < n; i++) {
			int y = ys[i];
			if (y < low) {
				low = y;
			}
			if (y > high) {
				high = y;
			}
		}
		if (low > high) {
			return Integer.valueOf(0);
		}
		int centring = -((low + high) / 2) + def.modelOffset2;
		if (isPotion(def)) {
			// The potion rule: a bottle is drawn lower than its vertices say it is, because the
			// vertices include a neck and glass that hardly draw at this size. See POTION_DROP_NUM.
			return Integer.valueOf(centring * POTION_DROP_NUM / POTION_DROP_DEN * ICON_DROP_NUM
					/ ICON_DROP_DEN);
		}
		return Integer.valueOf(centring * ICON_DROP_NUM / ICON_DROP_DEN);
	}

	/**
	 * The words a potion's name is recognised by, lower case, as {@link #isPotion} looks for them.
	 *
	 * <p>Name matching rather than a flag because {@code ItemDef} has nothing better and the models
	 * do not separate these: one bottle mesh is an unfinished potion, three finished doses and a
	 * barbarian mix, and the doses differ only in how much of the mesh is filled. Herblore's whole
	 * product list is here, including the drinks that are potions without the word - a brew, a
	 * mixture, a serum - so a menu that makes them gets the same placement as one that makes
	 * "Attack potion(3)".
	 *
	 * <p>{@code mix(} and not {@code mix} is deliberate: a kebab mix and a naphtha mix are made
	 * things, but they are not drinks, and the barbarian mixes are the only ones with a dose in
	 * brackets.
	 */
	private static final String[] POTION_WORDS = { "potion", "brew", "serum", "antidote", "mixture",
			"mix(" };

	/**
	 * The four bottle meshes a finished dose is drawn from, which is the other half of {@link
	 * #isPotion} and the half the names cannot do.
	 *
	 * <p><b>Why the words are not enough on their own.</b> Half the potions in this cache are not
	 * named for being one. "Super attack(3)", "Super strength(2)", "Super energy(4)", "Super
	 * restore(3)" and "Antipoison(3)" are all drinks, and not one of them contains "potion" - naming
	 * each family in the word list would be a list that grows every time a server adds a potion. They
	 * do all share something the words do not reach: they are drawn from one of the four dose
	 * bottles, 4/3/2/1, which is the one fact about a potion the client can read off an item without
	 * being told.
	 *
	 * <p><b>Measured, and wider than "potion" - deliberately.</b> 127 items in this cache are drawn
	 * from these four meshes and they are not all drinks: a vial, olive oil, sacred oil, liquid honey,
	 * nitroglycerin, a firework. They are all, though, the same shape drawn the same way, which is
	 * the only thing this rule is about - a vial is authored standing on its origin exactly as a
	 * bottle is, so it sits high in its box for exactly the same reason and wants exactly the same
	 * correction. {@code MakeXProbe} lists what the meshes catch beyond the names, so the breadth is
	 * known rather than assumed.
	 *
	 * <p>The bottle every unfinished potion shares is deliberately absent: 1225 items use it, a
	 * cannon base among them, so it identifies nothing.
	 */
	private static final int[] POTION_DOSE_MODELS = { 2789, 2697, 2384, 2621 };

	/**
	 * Whether an item is a bottle that should be placed by the potion rule.
	 *
	 * <p>Either half is enough: named a potion, or drawn from a dose bottle. Deliberately narrow on
	 * the name half - "restore", "attack" and "strength" are absent because they are also the herbs
	 * the potions are made from, and the dose bottles catch the items carrying those names anyway.
	 *
	 * <p>Public because the probe asks this rather than keeping its own copy of the word list: which
	 * items count as bottles is a decision, and two lists would be two decisions.
	 */
	public static boolean isPotion(ItemDef def) {
		if (def == null) {
			return false;
		}
		for (int i = 0; i < POTION_DOSE_MODELS.length; i++) {
			if (def.modelID == POTION_DOSE_MODELS[i]) {
				return true;
			}
		}
		String name = def.name;
		if (name == null) {
			return false;
		}
		String lower = name.toLowerCase();
		for (int i = 0; i < POTION_WORDS.length; i++) {
			if (lower.indexOf(POTION_WORDS[i]) >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * How far down to draw one of this class's product icons, in model units, or zero for a widget
	 * that is not a product frame or whose item's model has not arrived yet.
	 *
	 * <p>Called by {@code client}'s type 6 branch, which is the only place that draws one: the number
	 * has to be applied to the translation {@code method482} is handed, not to the widget, because a
	 * widget's own offset moves its clip rectangle with it and so cannot move an icon inside its box.
	 *
	 * <p><b>Why the answer is worked out here and not when the item was written to the slot.</b> A
	 * product's item arrives in a packet, and the model it names arrives separately, some frames
	 * later, from a fetch this same drawing path triggers. Measuring on the packet would measure a
	 * model that is not there, and - since a slot is only re-measured when the server writes to it
	 * again - the menu would keep that first reading for the rest of the time it was open. So the
	 * measurement lives at draw time, where the model is the one about to be drawn, and the answer is
	 * remembered per slot against the item it was taken for.
	 */
	public static int iconDropY(int widgetId) {
		for (int family = 0; family < LEGACY.length; family++) {
			int slot = slotOf(family, MODEL_FRAMES, widgetId);
			if (slot < 0) {
				continue;
			}
			RSInterface icon = RSInterface.interfaceCache == null ? null
					: RSInterface.interfaceCache[widgetId];
			int item = icon == null ? 0 : icon.mediaID;
			if (item <= 0) {
				return 0;
			}
			if (iconDropItem[family][slot] != item) {
				Integer drop = dropOf(ItemDef.forID(item));
				if (drop == null) {
					// Not fetched yet. The request has gone out; ask again next frame rather than
					// remembering a number that would be a lie.
					return 0;
				}
				iconDrop[family][slot] = drop.intValue();
				iconDropItem[family][slot] = item;
			}
			return iconDrop[family][slot];
		}
		return 0;
	}

	/**
	 * Re-lays out a family from its live count: how many boxes, how wide, how far apart, and where
	 * the row is centred. Called when the dialog opens and again whenever a model frame lands.
	 *
	 * <p><b>Why this cannot be done at build time.</b> {@link #build} runs when the cache's
	 * interfaces are unpacked, long before any server has said what is in the player's pack, and the
	 * same container serves every player and every menu. The row is therefore laid out from the
	 * tables once - the widest case, so nothing is ever missing - and corrected here to what the
	 * server actually filled.
	 *
	 * <p><b>Ids do not move.</b> Only the coordinates and the sizes change; the child list and every
	 * child id stay exactly as built, because the button ids are the server's contract and the
	 * probe checks the layout against the same tables.
	 */
	static void recount(int family) {
		RSInterface menu = RSInterface.interfaceCache == null ? null
				: RSInterface.interfaceCache[CONTAINER_FIRST + family];
		if (menu == null || menu.children == null) {
			return;
		}
		int slots = slotsIn(family);
		int count = filledSlots(family);
		int previous = liveCount[family];
		liveCount[family] = count;
		int width = slotWidth(count);
		if (slotFrameWidth[family] != width) {
			slotFrames[family] = frameChrome(width);
			slotFrameWidth[family] = width;
			restyleFrames(family);
		}
		for (int slot = 0; slot < slots; slot++) {
			boolean shown = slot < count;
			int x = shown ? childX(slotX(count, slot)) : OFF_PANEL;
			place(menu, slotFrameId(family, slot), x, childY(SLOT_TOP), shown ? width : 0,
					shown ? SLOT_H : 0);
			// The model frame is the icon's box as well as its position: the renderer centres the
			// icon on the widget's height, so the widget spans the whole slot and the icon lands in
			// the middle of the container.
			place(menu, MODEL_FRAMES[family][slot], x, childY(SLOT_TOP), shown ? width : 0,
					shown ? SLOT_H : 0);
			if (shown) {
				applyIcon(family, slot, MODEL_FRAMES[family][slot]);
			}
			for (int server = 0; server < SERVER_AMOUNTS.length; server++) {
				place(menu, serverButton(family, slot, server), x, childY(SLOT_TOP), shown ? width : 0,
						shown ? SLOT_H : 0);
			}
		}
		if (previous != count) {
			// The armed hitbox widths and the question of which slots have one at all both follow
			// the count, so the row is re-armed whenever it changed.
			applySelection();
		}
	}

	/** Points every box in a family at the frame art of the width the row settled on. */
	private static void restyleFrames(int family) {
		for (int slot = 0; slot < slotsIn(family); slot++) {
			RSInterface frame = RSInterface.interfaceCache[slotFrameId(family, slot)];
			if (frame != null) {
				frame.sprite1 = slotFrames[family];
				frame.sprite2 = slotFrames[family];
			}
		}
	}
	/** Moves and sizes one child of a built container, found by id. */
	private static void place(RSInterface menu, int id, int x, int y, int width, int height) {
		for (int i = 0; i < menu.children.length; i++) {
			if (menu.children[i] == id) {
				menu.childX[i] = x;
				menu.childY[i] = y;
				RSInterface child = RSInterface.interfaceCache[id];
				if (child != null) {
					child.width = width;
					child.height = height;
				}
				return;
			}
		}
	}

	private static boolean isContainer(int id) {
		return id >= CONTAINER_FIRST && id < CONTAINER_FIRST + LEGACY.length;
	}

	/**
	 * Whether this chatbox is the one showing, which is what {@code ChatArea} asks before it decides
	 * how to paint the panel behind it.
	 *
	 * <p>In resizable mode the chatbox can be drawn translucent over the world, and these five are
	 * the one family of dialogs that cannot be: the row is read as a row of buttons, and buttons
	 * want a background behind them rather than the scenery. The other dialogs are left translucent
	 * — this is an exception for this interface, not a rule about dialogs.
	 *
	 * @param dialogId the dialog the client currently has open ({@code backDialogID})
	 */
	public static boolean owns(int dialogId) {
		return isContainer(dialogId);
	}

	/**
	 * Points every product's live hitbox at the armed amount and re-inks the amount row.
	 *
	 * <p>Making the armed hitbox the only one with a size is what keeps the click honest. The four
	 * stacked buttons of a product are read last-child-wins, so shrinking the other three is the
	 * only way to decide which one a click lands on without renumbering any of them.
	 */
	private static void applySelection() {
		if (RSInterface.interfaceCache == null) {
			return;
		}
		for (int family = 0; family < LEGACY.length; family++) {
			int slots = slotsIn(family);
			int live = liveCount[family];
			int width = slotWidth(live);
			for (int slot = 0; slot < slots; slot++) {
				// Zeroed first and the armed one sized after, rather than in one pass over the
				// row: the four buttons are stacked on the one box, so exactly one may be live.
				for (int server = 0; server < SERVER_AMOUNTS.length; server++) {
					RSInterface box = RSInterface.interfaceCache[serverButton(family, slot, server)];
					if (box != null) {
						box.atActionType = 0;
						box.width = 0;
						box.height = 0;
					}
				}
				// A slot with no product gets no button at all. It used to, and an empty box you
				// could click that then did nothing is the other half of what was reported.
				if (slot >= live) {
					continue;
				}
				RSInterface armed = RSInterface.interfaceCache[serverButton(family, slot, serverSlot())];
				if (armed != null) {
					armed.atActionType = 1;
					// The box's own width, not SLOT_W: a full row of eight is 53 wide and a 64-wide
					// hitbox on the 59 pitch overlaps its neighbour, so a click near the right edge
					// of one product would make the one after it.
					armed.width = width;
					armed.height = SLOT_H;
				}
			}
		}
		for (int amount = 0; amount < AMOUNT_LABELS.length; amount++) {
			RSInterface button = RSInterface.interfaceCache[QUANTITY_FIRST + amount];
			if (button != null) {
				button.sprite1 = amount == selected ? quantityOn : quantityOff;
				button.sprite2 = quantityOn;
			}
			RSInterface text = RSInterface.interfaceCache[QUANTITY_TEXT_FIRST + amount];
			if (text != null) {
				text.textColor = amount == selected ? QUANTITY_INK_ON : QUANTITY_INK;
			}
		}
	}

	/* -------------------------------------------------------------- packets */

	/**
	 * The interface the chatbox should really open, given the one the server asked for.
	 *
	 * <p>Sits on packet 164, which is how all five of these menus are opened and the only place
	 * the server has to be told one is. Anything that is not one of the five is returned untouched.
	 *
	 * <p>It deliberately does not touch {@link #selected}: the amount is the player's habit and is
	 * carried from one menu to the next, so reopening a chatbox arms whatever they last chose. The
	 * armed hitboxes are already pointing at it, and re-arming here would only be needed if
	 * something else had moved them.
	 */
	public static int substitute(int id) {
		int family = familyOf(id);
		if (family < 0) {
			return id;
		}
		// Re-laid out from what the server has already filled. A row the player closed while it had
		// five products in it must not open five boxes for a menu that now has one.
		recount(family);
		return CONTAINER_FIRST + family;
	}

	private static int familyOf(int legacy) {
		for (int family = 0; family < LEGACY.length; family++) {
			if (LEGACY[family] == legacy) {
				return family;
			}
		}
		return -1;
	}

	/**
	 * Takes a written product name off the button the cache keeps it in and puts it on the label
	 * that draws it.
	 *
	 * <p>Runs on packet 126. The name is padded with line breaks whose only job is to push it clear
	 * of the item model inside the click target, and the padding is stripped here because the label
	 * is positioned rather than padded.
	 *
	 * @return true when the text belonged to a button, so the caller blanks the original
	 */
	public static boolean absorbName(int frame, String text) {
		if (text == null) {
			return false;
		}
		for (int family = 0; family < LEGACY.length; family++) {
			int slot = slotOf(family, NAME_FRAMES, frame);
			if (slot < 0) {
				continue;
			}
			RSInterface label = RSInterface.interfaceCache[labelId(family, slot)];
			if (label != null) {
				label.message = fit(clean(text), slotWidth(liveCount[family]));
				label.width = slotWidth(liveCount[family]);
			}
			return true;
		}
		return false;
	}

	/**
	 * Copies the names the cache keeps baked into its own buttons onto this class's labels.
	 *
	 * <p>Smelting is why this exists: its eight names live in the interface data, not in a packet,
	 * so nothing would ever reach {@link #absorbName} for them.
	 */
	private static void seedLabels(int family, int slots) {
		for (int slot = 0; slot < slots; slot++) {
			RSInterface source = RSInterface.interfaceCache[NAME_FRAMES[family][slot]];
			if (source == null || source.message == null || source.message.length() == 0) {
				continue;
			}
			RSInterface label = RSInterface.interfaceCache[labelId(family, slot)];
			label.message = fit(clean(source.message), slotWidth(liveCount[family]));
			source.message = "";
		}
	}

	/** The cache pads a name down the hitbox with line breaks; a label sets its own position. */
	private static String clean(String text) {
		String stripped = text.replace("\\n", " ").trim();
		return stripped.length() == 0 ? " " : stripped;
	}

	/**
	 * Shortens a name until it fits the box it is drawn in, ending it in {@code ..} when it had to
	 * be cut.
	 *
	 * <p>The label is centred and does not wrap, so a name wider than its box is not clipped - it
	 * spills the same amount into both neighbours, and a row of five long names reads as one long
	 * smear across the panel rather than as five products. A box is 53 to 64 pixels and "Uncut
	 * dragonstone" is nearer ninety, so this is the ordinary case and not the edge one.
	 *
	 * <p>Measured with the label's own font rather than estimated from the character count, because
	 * the client's font is not monospaced and an estimate that is right for "Broad bolts" is out by
	 * half a box for "Uncut dragonstone".
	 */
	private static String fit(String text, int width) {
		TextDrawingArea font = fonts != null && fonts.length > 1 ? fonts[1] : null;
		if (font == null || width <= 0 || width >= WIDTH || font.getTextWidth(text) <= width) {
			return text;
		}
		int end = text.length();
		while (end > 0 && font.getTextWidth(text.substring(0, end) + "..") > width) {
			end--;
		}
		return end > 0 ? text.substring(0, end) + ".." : text;
	}

	/* ------------------------------------------------------------------ art */

	private static Sprite quantityChrome(int frame, boolean armed) {
		Sprite loaded = RSInterface.imageLoader(frame, "Interfaces/MakeX/QUANTITY");
		if (loaded != null && loaded.myWidth > 0) {
			return loaded;
		}
		return drawnPill(armed);
	}

	/**
	 * The frame behind one product, at the width this family's box is.
	 *
	 * <p>Scaled rather than merely reused. A type-5 widget is drawn by
	 * {@code sprite.drawSprite(x, y)}, which honours the sprite's own size and not the widget's
	 * width — so one 64-wide SLOT.png on smelting's eight 58-wide boxes is 6 pixels of frame drawn
	 * over each neighbour. The widget's declared width is what the layout promises; this is what
	 * makes the art the same size as the promise.
	 *
	 * <p>Scales {@link #slotBase}, loaded once at build, instead of loading the file again: this runs
	 * from {@code recount}, after {@code RSInterface.unpack} has torn the media loader down. A
	 * missing file falls back to the drawn box, which needs no loader at all.
	 */
	private static Sprite frameChrome(int width) {
		if (slotBase != null) {
			return slotBase.myWidth == width ? slotBase : scaleX(slotBase, width);
		}
		return bevelled(width, SLOT_H, 2, 0x6E5A3C, 0xE6DAB8, 0xC8B591, 0xA08B6A);
	}

	/** The slot art as it sits on disk, or null when it is missing or the loader is gone. */
	private static Sprite loadSlotBase() {
		if (!RSInterface.mediaCacheAvailable()) {
			return null;
		}
		Sprite loaded = RSInterface.imageLoader(0, "Interfaces/MakeX/SLOT");
		return loaded != null && loaded.myWidth > 0 && loaded.myHeight > 0 ? loaded : null;
	}

	/** A copy of a sprite in a new width, nearest-neighbour along x only. Height is untouched. */
	private static Sprite scaleX(Sprite src, int width) {
		Sprite out = new Sprite(width, src.myHeight);
		for (int y = 0; y < src.myHeight; y++) {
			for (int x = 0; x < width; x++) {
				out.myPixels[y * width + x] = src.myPixels[y * src.myWidth + x * src.myWidth / width];
			}
		}
		return out;
	}

	/**
	 * The pill the placeholder files carry, drawn in code.
	 *
	 * <p>It exists so the row is never invisible when the two files are missing, so it has to match
	 * them rather than merely be visible: the idle pill is light because its caption is drawn in
	 * black, and the armed pill is dark because its caption is drawn in white — which is also how
	 * the reference screenshots distinguish the armed one. {@code tools/make-quantity-sprites.ps1}
	 * draws the files from these same four colours.
	 */
	private static Sprite drawnPill(boolean armed) {
		int border = armed ? 0x3A2A10 : 0x6E5A3C;
		int highlight = armed ? 0xF2D070 : 0xE6DAB8;
		int face = armed ? 0x7A5416 : 0xC8B591;
		int shade = armed ? 0x54380C : 0xA08B6A;
		return bevelled(QUANT_W, QUANT_H, 1, border, highlight, face, shade);
	}

	/**
	 * A bevelled rectangle: a 1px border, a highlight on the top and left inside it, a shade on the
	 * bottom and right, a flat face, and the four corners knocked out so it does not read as a box.
	 *
	 * <p>Also the fallback art: {@code frameChrome} draws the box this way when
	 * {@code Sprites/Interfaces/MakeX/SLOT.png} is missing, in the same four colours as the pill, so
	 * a missing placeholder file leaves the row visible rather than empty.
	 *
	 * @param inset how far in the highlight/shade pair starts; 1 for a pill, 2 for a slot frame
	 */
	private static Sprite bevelled(int w, int h, int inset, int border, int highlight, int face,
			int shade) {
		Sprite sprite = new Sprite(w, h);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				boolean outer = x < inset || y < inset || x >= w - inset || y >= h - inset;
				boolean nearEdge = x < inset + 1 || y < inset + 1
						|| x >= w - inset - 1 || y >= h - inset - 1;
				int colour;
				if (outer) {
					colour = 0xFF000000 | border;
				} else if (nearEdge) {
					colour = 0xFF000000 | ((x < inset + 1 || y < inset + 1) ? highlight : shade);
				} else {
					colour = 0xFF000000 | face;
				}
				sprite.myPixels[y * w + x] = colour;
			}
		}
		// Corners transparent, so the frame does not sit on a hard rectangle of border colour.
		sprite.myPixels[0] = 0;
		sprite.myPixels[w - 1] = 0;
		sprite.myPixels[(h - 1) * w] = 0;
		sprite.myPixels[(h - 1) * w + w - 1] = 0;
		return sprite;
	}
}
