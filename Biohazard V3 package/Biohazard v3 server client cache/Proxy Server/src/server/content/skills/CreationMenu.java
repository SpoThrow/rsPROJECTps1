package server.content.skills;

import server.game.items.ItemAssistant;
import server.game.players.Client;

/**
 * The chatbox creation menu that every "what would you like to make?" family shares.
 *
 * <p><b>Nothing here is drawn.</b> Interface 8938 already exists in the cache — it is five rows of
 * one product each, with four buttons a row for make 1/5/10/28, and snakeskin leather and the
 * potter's wheel have used it for as long as this server has had them. The frame, the title and
 * the buttons come from the cache; the only things that travel are each row's model, each row's
 * label, which row was clicked and how many that button asked for. That is why bringing a new
 * family into the chatbox costs no client work at all.
 *
 * <p><b>The amounts are the interface's, and they are shared.</b> Row <em>n</em> sends the same
 * four button ids whichever family opened the menu, so a button cannot be interpreted by looking
 * at it: it has to be interpreted by looking at what the player has open. That is what
 * {@link #route} is for. It reads {@code creationMenu} and {@code creationRows} off the player and
 * answers exactly one family, so two menus can never both act on one click.
 *
 * <p><b>Five rows is a real ceiling.</b> A family with more than five products has to cap or page
 * itself; {@link #open} truncates and says so through {@link #rowsShown} rather than silently
 * dropping the tail, because a product missing from a menu with no explanation reads as a product
 * the server cannot make.
 */
public final class CreationMenu {

	/** The five-row chatbox the creation families share. */
	public static final int INTERFACE = 8938;

	/**
	 * How many products the interface can offer at once. The rows are fixed in the cache at five,
	 * so this is a fact about the interface rather than a preference.
	 */
	public static final int ROWS = 5;

	/** The model frames, one per row, in row order. */
	private static final int ITEM_FRAME_FIRST = 8941;

	/** The label frames, one per row, four apart. */
	private static final int NAME_FRAME_FIRST = 8949;
	private static final int NAME_STRIDE = 4;

	/** How large a row's item model is drawn. The wheel and the snakeskin rows both use 180. */
	private static final int ZOOM = 180;

	/**
	 * The four buttons each row carries, in row order, each row in the order 1/5/10/28.
	 *
	 * <p>Read by every family and written down in one place, because these ids are the whole
	 * coupling between the interface and the server and a typo in one family's copy would hand its
	 * click to another. Rows 4 and 5 are the two the wheel's own table takes from the snakeskin
	 * rows; see {@code Pottery.Shape}.
	 */
	public static final int[][] BUTTONS = {
			{ 34245, 34244, 34243, 34242 },
			{ 34249, 34248, 34247, 34246 },
			{ 34253, 34252, 34251, 34250 },
			{ 35001, 35000, 34255, 34254 },
			{ 35005, 35004, 35003, 35002 },
	};

	/**
	 * What each of a row's four buttons asks for, in the order they appear on the interface.
	 *
	 * <p>28 is the inventory, which is what the interface's own fourth button means; the client
	 * prints no number for it.
	 */
	public static final int[] AMOUNTS = { 1, 5, 10, 28 };

	/**
	 * The largest count a make-X request will be honoured with.
	 *
	 * <p>Not a game rule - the pack runs out long before this in every real case, and the families
	 * stop there. It is a ceiling on how much work one packet can ask for, kept equal to the
	 * client's own clamp so that a legitimate request is never quietly reduced.
	 */
	public static final int MAX_X = 9999;

	/**
	 * Which family has the menu open, so a shared button is answered once.
	 *
	 * <p>Held on the player rather than in a field here: one player's menu must not answer another
	 * player's click, which is the same reason {@code herbloreItem1} lives on the player.
	 */
	public enum Kind {

		/** The chisel's menu. Rows are uncut gems the player is carrying. */
		GEM_CUTTING,

		/**
		 * The chisel's second menu. Rows are the <em>cut</em> gems the player is carrying, and each
		 * row makes that gem's bolt tips.
		 *
		 * <p>Separate from {@link #GEM_CUTTING} because the two are different products from
		 * different items and only the first can be reached by level: a sapphire and an uncut
		 * sapphire are both gems in the pack and both rows of a chisel menu, and answering a click
		 * with the wrong one would cut a gem the player asked to tip.
		 */
		BOLT_TIPS,

		/** The tanner's menu. Rows are hides the player is carrying. */
		TANNING,

		/**
		 * The jeweller's menu. One row, the product clicked in interface 4161.
		 *
		 * <p>This is the odd one of the three: the other two build their rows from the pack, because
		 * the pack is where the choice is. Here the choice was made in the mould interface, which
		 * cannot ask for more than ten of anything — so the menu's single row exists for the amount
		 * buttons, and "make all" is reachable only because this menu offers it.
		 */
		JEWELLERY,
	}

	private CreationMenu() {
	}

	/**
	 * Opens the menu on up to {@link #ROWS} products, labelled with their own names.
	 *
	 * @return how many rows were shown, which is fewer than {@code itemIds.length} when the family
	 *         offered more than the interface can hold.
	 */
	public static int open(Client c, Kind kind, int[] itemIds) {
		return open(c, kind, itemIds, null);
	}

	/**
	 * Opens the menu on up to {@link #ROWS} products.
	 *
	 * <p>Every row is written, including the ones past the end of {@code itemIds}: the interface
	 * keeps whatever was last drawn in a frame nobody overwrites, so a five-row menu opened after a
	 * three-row one would otherwise still show the last menu's bottom two products. Those rows get
	 * model {@code -1} and a blank label, and {@code creationRows} marks them as empty so a click on
	 * one does nothing rather than repeating the row above.
	 *
	 * <p>Opening also closes the two older flags that live on the same interface. A button fires
	 * whichever flag is set, so the wheel and the snakeskin menus have to be cleared here for the
	 * same reason they clear each other.
	 *
	 * @param labels one per product, or null to use each product's item name
	 */
	public static int open(Client c, Kind kind, int[] itemIds, String[] labels) {
		int shown = Math.min(itemIds.length, ROWS);
		c.getPA().sendFrame164(INTERFACE);
		for (int row = 0; row < ROWS; row++) {
			boolean filled = row < shown;
			int item = filled ? itemIds[row] : -1;
			c.getPA().itemOnInterface(ITEM_FRAME_FIRST + row, ZOOM, item);
			c.getPA().sendFrame126(labelFor(item, labels, row), NAME_FRAME_FIRST + row * NAME_STRIDE);
			c.creationRows[row] = item;
		}
		c.creationMenu = kind;
		c.potteryDialogue = false;
		c.craftDialogue = false;
		return shown;
	}

	private static String labelFor(int item, String[] labels, int row) {
		if (item < 0) {
			return " ";
		}
		if (labels != null && row < labels.length && labels[row] != null) {
			return labels[row];
		}
		return ItemAssistant.getItemName(item);
	}

	/**
	 * Closes the menu without telling the client, which does not need telling: a walk clears the
	 * chatbox on its own, and a button press is followed by {@code removeAllWindows}.
	 *
	 * <p>Safe to call with no menu open, so it can sit on the walk path unconditionally.
	 */
	public static void close(Client c) {
		c.creationMenu = null;
		for (int row = 0; row < ROWS; row++) {
			c.creationRows[row] = -1;
		}
	}

	/**
	 * Answers a chatbox button, if it belongs to the menu the player has open.
	 *
	 * <p>Returns whether the button was this menu's, so the caller can stop before the legacy
	 * button families get a look at it. A button that is no row of the interface, or a row the open
	 * menu left empty, is still claimed when a menu is open — the alternative is a click that falls
	 * through to whatever else is listening.
	 *
	 * @return true when the button belonged to the open menu and has been handled
	 */
	public static boolean route(Client c, int button) {
		Kind kind = c.creationMenu;
		if (kind == null) {
			return false;
		}
		int row = rowFor(button);
		if (row < 0) {
			return false;
		}
		int item = c.creationRows[row];
		if (item < 0) {
			return true;
		}
		handTo(kind, c, item, amountFor(button));
		return true;
	}

	/**
	 * Answers a make-X request: a row of the open menu and a count of the player's own choosing.
	 *
	 * <p>Called from {@code MakeXAmount}, which is the only caller, and it exists so that make-X and
	 * a make-10 click reach the families through the same door. The four buttons cannot express a
	 * custom count, which is the entire reason the client sends a packet instead - but the work
	 * after the count is known is the same work, and duplicating the hand-off would be how the two
	 * drifts apart.
	 *
	 * <p><b>Nothing here is trusted to be in range.</b> The row is checked against the menu the
	 * player actually has open rather than against the table of buttons, because the two are not
	 * the same question: a row can be on the interface and empty, and an empty row is a click that
	 * must do nothing rather than repeat the row above it. The count is clamped here as well as on
	 * the client, since the client is not a validator.
	 *
	 * @return true when the request belonged to an open menu and has been handled
	 */
	public static boolean submit(Client c, int row, int amount) {
		Kind kind = c.creationMenu;
		if (kind == null || row < 0 || row >= ROWS) {
			return false;
		}
		int item = c.creationRows[row];
		if (item < 0) {
			return true;
		}
		if (amount < 1) {
			return true;
		}
		handTo(kind, c, item, Math.min(amount, MAX_X));
		return true;
	}

	/** One place where a family is told to make an item, whichever way the request arrived. */
	private static void handTo(Kind kind, Client c, int item, int amount) {
		switch (kind) {
		case GEM_CUTTING:
			GemCutting.select(c, item, amount);
			break;
		case BOLT_TIPS:
			Fletching.selectBoltTips(c, item, amount);
			break;
		case TANNING:
			Tanning.select(c, item, amount);
			break;
		case JEWELLERY:
			JewelryMaking.select(c, item, amount);
			break;
		default:
			break;
		}
	}

	/** The row a button belongs to, or -1 when it is not one of the interface's. */
	public static int rowFor(int button) {
		for (int row = 0; row < BUTTONS.length; row++) {
			for (int i = 0; i < AMOUNTS.length; i++) {
				if (BUTTONS[row][i] == button) {
					return row;
				}
			}
		}
		return -1;
	}

	/** What the button on {@code row} asks for, or 0 when the button is not this row's. */
	public static int amountFor(int button) {
		int row = rowFor(button);
		if (row < 0) {
			return 0;
		}
		for (int i = 0; i < AMOUNTS.length; i++) {
			if (BUTTONS[row][i] == button) {
				return AMOUNTS[i];
			}
		}
		return 0;
	}
}
