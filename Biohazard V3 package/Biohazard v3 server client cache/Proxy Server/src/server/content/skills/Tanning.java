package server.content.skills;

import java.util.Arrays;

import server.game.players.Client;

/**
 * The tanner: he takes a hide and a handful of coins and hands back leather, through the same
 * chatbox creation menu the wheel and the chisel use.
 *
 * <p><b>A row is a product, not a hide.</b> The obvious model — one row per hide the player is
 * carrying — is wrong, and the table says so: cowhide {@code 1739} tans into <em>soft leather</em>
 * at 1 gp or <em>hard leather</em> at 3 gp, two different products from the same hide. So the rows
 * are the leathers whose hide the player is holding, which is what makes the soft/hard choice a
 * choice. The row's icon is the leather for the same reason: that is what the old overlay drew in
 * its item frame, and it is also the thing the two rows actually differ by.
 *
 * <p><b>The interface this replaced was its own.</b> Interface 14670 was a purpose-built overlay
 * with a row per product, each with a name frame, a price frame and an item frame — and the price
 * frames were the reason it existed, because tanning is the one crafting family that charges. That
 * is also why the rows here are labelled rather than bare: the chatbox menu's label frames are the
 * only text it has, so the price rides on the name. "Green d'hide - 20 gp" is what the old price
 * frame said, in the same words.
 *
 * <p><b>Rows are only what the player can actually use.</b> The old overlay showed all eight rows
 * whether or not the player held the hide, and reddened the price when they could not afford it. A
 * menu built from the pack cannot do either — and does not need to, because both of those are
 * things a player finds out by not having the hide or the coins.
 *
 * <p><b>Seven rows, not eight, and 15 gp not 20.</b> The table held snakeskin twice, from the same
 * hide {@code 6287} to the same leather {@code 6289}, once at 15 gp and once at 20. Both rows were
 * reachable on the old overlay and the player could pay either; as rows here they would have been
 * two rows keyed on one product, so the duplicate is gone rather than left as unreachable data.
 * The cheaper of the two is kept, which is the one a player would have picked.
 *
 * <p>Tanning stays instant and unlevelled, as it was: this is a shop counter, not a skill action,
 * and the old code neither ticked it nor asked for a level.
 */
public class Tanning extends CraftingData {

	/**
	 * The tanner's menu, opened by talking to him.
	 *
	 * <p>Also the refresh: {@link #select} calls this again after a tan so the rows show what is
	 * left, which is what the old overlay did by staying on screen with live frames.
	 */
	public static void sendTanningInterface(final Client c) {
		int[] products = offeredProducts(c);
		if (products.length == 0) {
			CreationMenu.close(c);
			c.sendMessage("You have no hides for the tanner to tan.");
			return;
		}
		int shown = Math.min(products.length, CreationMenu.ROWS);
		CreationMenu.open(c, CreationMenu.Kind.TANNING, Arrays.copyOf(products, shown),
				labelsFor(Arrays.copyOf(products, shown)));
		if (shown < products.length) {
			c.sendMessage("The tanner can only offer five kinds of leather at a time.");
		}
	}

	/**
	 * The leathers the tanner would offer this player, in the table's order: one per row whose hide
	 * is in the pack.
	 *
	 * <p>Keyed on the leather because that is what a row makes and what its icon shows. Two rows
	 * can share a hide — cowhide gives both soft and hard leather — which is exactly why the hide
	 * cannot be the key.
	 */
	public static int[] offeredProducts(final Client c) {
		int[] found = new int[tanningData.values().length];
		int n = 0;
		for (tanningData t : tanningData.values()) {
			if (c.getItems().playerHasItem(t.getHideId(), 1)) {
				found[n++] = t.getLeatherId();
			}
		}
		return Arrays.copyOf(found, n);
	}

	/**
	 * One label per row, carrying the price because the menu has nowhere else to put it.
	 *
	 * <p>The label frames are the only text on this interface, so the price rides on the name
	 * exactly as the old overlay's price frame did.
	 */
	static String[] labelsFor(final int[] products) {
		String[] labels = new String[products.length];
		for (int i = 0; i < products.length; i++) {
			tanningData t = forProduct(products[i]);
			labels[i] = t == null ? null : t.getName() + " - " + t.getPrice() + " gp";
		}
		return labels;
	}

	/** The row that makes a leather, or null when the id is not one the tanner makes. */
	public static tanningData forProduct(final int leatherId) {
		for (tanningData t : tanningData.values()) {
			if (t.getLeatherId() == leatherId) {
				return t;
			}
		}
		return null;
	}

	/**
	 * A row's worth of tanning. Called from {@link CreationMenu#route} with the row's leather and
	 * the amount its button asks for.
	 *
	 * <p>The amount is what the player asked for bounded by what they can pay for and what they are
	 * actually carrying, which is the arithmetic the old button handler did in a roundabout way: it
	 * clamped the price to the coins, then divided the price back out to recover the count. Asking
	 * how many are affordable says the same thing once.
	 */
	public static void select(final Client c, final int leatherId, final int amount) {
		tanningData t = forProduct(leatherId);
		if (t == null || amount <= 0) {
			return;
		}
		if (!c.getItems().playerHasItem(t.getHideId(), 1)) {
			c.sendMessage("You do not have any hides to tan.");
			sendTanningInterface(c);
			return;
		}
		int price = t.getPrice();
		int affordable = price <= 0 ? amount : c.getItems().getItemCount(995) / price;
		int wanted = Math.min(amount, Math.min(c.getItems().getItemCount(t.getHideId()), affordable));
		if (wanted <= 0) {
			c.sendMessage("You do not have enough coins to tan this hide.");
			return;
		}
		c.getItems().deleteItem2(t.getHideId(), wanted);
		if (price > 0) {
			c.getItems().deleteItem(995, c.getItems().getItemSlot(995), wanted * price);
		}
		c.getItems().addItem(leatherId, wanted);
		c.sendMessage("The tanner tans the hides for you.");
		// Re-opens with what is left, so a second click cannot be answered from a stale row.
		sendTanningInterface(c);
	}
}
