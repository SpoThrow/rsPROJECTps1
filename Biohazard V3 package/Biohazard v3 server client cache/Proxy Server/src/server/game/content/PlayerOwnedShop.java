package server.game.content;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import server.Config;
import server.game.items.Item;
import server.game.players.Client;
import server.game.players.PlayerHandler;

/**
 * Player Owned Shop System
 */
public class PlayerOwnedShop {

	public static final int MAX_LISTINGS = 10;
	private static final Object LOCK = new Object();
	private static final ReentrantLock SAVE_LOCK = new ReentrantLock();

	public static class ShopListing {
		public long listingId;
		public String ownerName;
		public int itemId;
		public int amount;
		public int price;
		public long listedTime;

		public ShopListing(String ownerName, int itemId, int amount, int price) {
			this(ownerName, itemId, amount, price, true);
		}

		private ShopListing(String ownerName, int itemId, int amount, int price, boolean assignId) {
			this.listingId = assignId ? nextListingId() : 0;
			this.ownerName = ownerName;
			this.itemId = itemId;
			this.amount = amount;
			this.price = price;
			this.listedTime = System.currentTimeMillis();
		}

		public String toString() {
			return ownerName + ":" + itemId + ":" + amount + ":" + price + ":" + listedTime + ":" + listingId;
		}

		public static ShopListing fromString(String str) {
			String[] parts = str.split(":");
			if (parts.length >= 5) {
				ShopListing listing = new ShopListing(parts[0], Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), false);
				listing.listedTime = Long.parseLong(parts[4]);
				if (parts.length >= 6) {
					listing.listingId = Long.parseLong(parts[5]);
					if (listing.listingId >= listingIdSeq) {
						listingIdSeq = listing.listingId + 1;
					}
				} else {
					listing.listingId = nextListingId();
				}
				return listing;
			}
			return null;
		}
	}

	public static class CollectItem {
		public int itemId;
		public int amount;

		public CollectItem(int itemId, int amount) {
			this.itemId = itemId;
			this.amount = amount;
		}
	}

	public static class PlayerShop {
		public String ownerName;
		public List<ShopListing> listings;
		public long lastUpdated;
		public long pendingGold;
		public List<CollectItem> collect;
		public List<String> sales;

		public PlayerShop(String ownerName) {
			this.ownerName = ownerName;
			this.listings = new ArrayList<ShopListing>();
			this.collect = new ArrayList<CollectItem>();
			this.sales = new ArrayList<String>();
			this.lastUpdated = System.currentTimeMillis();
			this.pendingGold = 0;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(ownerName).append(":").append(lastUpdated).append(":");
			for (ShopListing listing : listings) {
				sb.append(listing.toString()).append(";");
			}
			sb.append("|").append(pendingGold).append("|");
			for (int i = 0; i < collect.size(); i++) {
				if (i > 0) {
					sb.append(";");
				}
				sb.append(collect.get(i).itemId).append("x").append(collect.get(i).amount);
			}
			sb.append("|");
			for (int i = 0; i < sales.size(); i++) {
				if (i > 0) {
					sb.append(";");
				}
				sb.append(sales.get(i).replace(';', ',').replace('|', '/'));
			}
			return sb.toString();
		}

		public static PlayerShop fromString(String str) {
			String[] extra = str.split("\\|", -1);
			String head = extra[0];
			long gold = 0;
			if (extra.length > 1) {
				try {
					gold = Long.parseLong(extra[1].trim());
				} catch (NumberFormatException ignored) {
					gold = 0;
				}
			}
			String[] parts = head.split(":", 3);
			if (parts.length >= 2) {
				PlayerShop shop = new PlayerShop(parts[0]);
				shop.lastUpdated = Long.parseLong(parts[1]);
				shop.pendingGold = gold;
				if (parts.length > 2 && !parts[2].isEmpty()) {
					String[] listingStrs = parts[2].split(";");
					for (int i = 0; i < listingStrs.length; i++) {
						ShopListing listing = ShopListing.fromString(listingStrs[i]);
						if (listing != null) {
							listing.itemId = unnotedId(listing.itemId);
							shop.listings.add(listing);
						}
					}
				}
				if (extra.length > 2 && extra[2].length() > 0) {
					String[] bits = extra[2].split(";");
					for (int i = 0; i < bits.length; i++) {
						int x = bits[i].indexOf('x');
						if (x <= 0) {
							continue;
						}
						try {
							shop.collect.add(new CollectItem(Integer.parseInt(bits[i].substring(0, x)),
									Integer.parseInt(bits[i].substring(x + 1))));
						} catch (NumberFormatException ignored) {
						}
					}
				}
				if (extra.length > 3 && extra[3].length() > 0) {
					String[] logs = extra[3].split(";");
					for (int i = 0; i < logs.length; i++) {
						if (logs[i].length() > 0) {
							shop.sales.add(logs[i]);
						}
					}
				}
				return shop;
			}
			return null;
		}
	}

	private static Map<String, PlayerShop> playerShops = new HashMap<String, PlayerShop>();
	private static List<ShopListing> allListings = new ArrayList<ShopListing>();
	private static Map<Integer, String> itemNames = new HashMap<Integer, String>();
	private static Map<Integer, List<Integer>> saleHistory = new HashMap<Integer, List<Integer>>();
	private static final String CONFIG_FILE = "Data/cfg/player-owned-shops.cfg";
	private static final String ITEM_CONFIG_FILE = "Data/cfg/item.cfg";
	private static long listingIdSeq = 1;
	private static boolean dirty;

	private static long nextListingId() {
		return listingIdSeq++;
	}

	public static void loadShops() {
		synchronized (LOCK) {
			playerShops.clear();
			allListings.clear();
			loadItemNames();
			File configFile = new File(CONFIG_FILE);
			if (!configFile.exists()) {
				return;
			}
			try {
				BufferedReader reader = new BufferedReader(new FileReader(configFile));
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("shop = ") || line.startsWith("shop=")) {
						String content = line.substring(line.indexOf("=") + 1).trim();
						PlayerShop shop = PlayerShop.fromString(content);
						if (shop != null) {
							playerShops.put(shop.ownerName.toLowerCase(), shop);
							allListings.addAll(shop.listings);
						}
					} else if (line.startsWith("hist = ") || line.startsWith("hist=")) {
						String content = line.substring(line.indexOf("=") + 1).trim();
						int colon = content.indexOf(':');
						if (colon > 0) {
							try {
								int itemId = unnotedId(Integer.parseInt(content.substring(0, colon)));
								List<Integer> prices = new ArrayList<Integer>();
								String[] bits = content.substring(colon + 1).split(",");
								for (int i = 0; i < bits.length; i++) {
									if (bits[i].length() > 0) {
										prices.add(Integer.parseInt(bits[i]));
									}
								}
								saleHistory.put(itemId, prices);
							} catch (NumberFormatException ignored) {
							}
						}
					}
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Collections.sort(allListings, new Comparator<ShopListing>() {
				public int compare(ShopListing a, ShopListing b) {
					return Long.compare(b.listedTime, a.listedTime);
				}
			});
		}
	}

	public static void generateFakeListings() {
		synchronized (LOCK) {
			String[] fakePlayers = { "Alice", "Bob", "Charlie", "Dave", "Eve" };
			int[] commonItems = { 4151, 11694, 14484, 995, 385, 560, 565, 561 };
			java.util.Random random = new java.util.Random();
			for (int i = 0; i < 40; i++) {
				String playerName = fakePlayers[random.nextInt(fakePlayers.length)];
				PlayerShop shop = getPlayerShopUnlocked(playerName);
				if (shop.listings.size() >= MAX_LISTINGS) {
					continue;
				}
				ShopListing listing = new ShopListing(playerName, commonItems[random.nextInt(commonItems.length)],
						random.nextInt(50) + 1, random.nextInt(5000) + 100);
				shop.listings.add(listing);
				allListings.add(listing);
			}
			Collections.sort(allListings, new Comparator<ShopListing>() {
				public int compare(ShopListing a, ShopListing b) {
					return Long.compare(b.listedTime, a.listedTime);
				}
			});
			dirty = true;
			saveShopsUnlocked();
		}
	}

	private static void loadItemNames() {
		itemNames.clear();
		File itemCfgFile = new File(ITEM_CONFIG_FILE);
		if (!itemCfgFile.exists()) {
			return;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(itemCfgFile));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("item = ") || line.startsWith("item=")) {
					String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
					if (parts.length >= 2) {
						try {
							int itemId = Integer.parseInt(parts[0]);
							itemNames.put(itemId, parts[1].replace("_", " "));
						} catch (NumberFormatException e) {
						}
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getItemName(int itemId) {
		return itemNames.getOrDefault(itemId, "Unknown Item");
	}

	public static void saveShops() {
		synchronized (LOCK) {
			saveShopsUnlocked();
		}
	}

	private static void saveShopsUnlocked() {
		SAVE_LOCK.lock();
		try {
			File target = new File(CONFIG_FILE);
			File temp = new File(CONFIG_FILE + ".tmp");
			PrintWriter writer = new PrintWriter(new FileWriter(temp));
			writer.println("// Player Owned Shops Configuration");
			writer.println("// Format: shop = ownerName:lastUpdated:listing1;listing2;...|pendingGold");
			writer.println();
			for (PlayerShop shop : playerShops.values()) {
				writer.println("shop = " + shop.toString());
			}
			for (Map.Entry<Integer, List<Integer>> entry : saleHistory.entrySet()) {
				StringBuilder hist = new StringBuilder();
				hist.append("hist = ").append(entry.getKey()).append(":");
				List<Integer> prices = entry.getValue();
				for (int i = 0; i < prices.size(); i++) {
					if (i > 0) {
						hist.append(",");
					}
					hist.append(prices.get(i));
				}
				writer.println(hist.toString());
			}
			writer.close();
			if (target.exists() && !target.delete()) {
				return;
			}
			if (!temp.renameTo(target)) {
				PrintWriter fallback = new PrintWriter(new FileWriter(target));
				BufferedReader copy = new BufferedReader(new FileReader(temp));
				String line;
				while ((line = copy.readLine()) != null) {
					fallback.println(line);
				}
				copy.close();
				fallback.close();
				temp.delete();
			}
			dirty = false;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			SAVE_LOCK.unlock();
		}
	}

	public static void process() {
		synchronized (LOCK) {
			if (dirty) {
				saveShopsUnlocked();
			}
		}
	}

	public static PlayerShop getPlayerShop(String ownerName) {
		synchronized (LOCK) {
			return getPlayerShopUnlocked(ownerName);
		}
	}

	private static PlayerShop getPlayerShopUnlocked(String ownerName) {
		String key = ownerName.toLowerCase();
		PlayerShop shop = playerShops.get(key);
		if (shop == null) {
			shop = new PlayerShop(ownerName);
			playerShops.put(key, shop);
		}
		return shop;
	}

	public static boolean addListing(String ownerName, int itemId, int amount, int price) {
		synchronized (LOCK) {
			itemId = unnotedId(itemId);
			if (cannotList(itemId) != null || amount <= 0 || price < 0) {
				return false;
			}
			PlayerShop shop = getPlayerShopUnlocked(ownerName);
			if (shop.listings.size() >= MAX_LISTINGS) {
				return false;
			}
			ShopListing listing = new ShopListing(ownerName, itemId, amount, price);
			shop.listings.add(listing);
			shop.lastUpdated = System.currentTimeMillis();
			allListings.add(0, listing);
			dirty = true;
			saveShopsUnlocked();
			return true;
		}
	}

	public static String cannotList(int itemId) {
		itemId = unnotedId(itemId);
		if (itemId == 995 || itemId == 996) {
			return "You cannot list coins in a player shop.";
		}
		if (itemId <= 0) {
			return "That item cannot be listed.";
		}
		if (!tradeableItem(itemId)) {
			return "That item cannot be traded, so it cannot be listed.";
		}
		String name = getItemName(itemId).toLowerCase();
		if (name.contains("pet") || name.contains("follower")) {
			return "Pets cannot be listed in a player shop.";
		}
		if (name.contains("broken") || name.contains("degraded")) {
			return "Broken or degraded items cannot be listed.";
		}
		if (name.contains("uncharged") || name.endsWith("(0)")) {
			return "Uncharged items cannot be listed.";
		}
		return null;
	}

	private static boolean tradeableItem(int itemId) {
		int[] blocked = Config.ITEM_TRADEABLE;
		if (blocked != null) {
			for (int i = 0; i < blocked.length; i++) {
				if (itemId == blocked[i]) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean listItem(Client c, int itemId, int amount, int price) {
		if (c == null) {
			return false;
		}
		int unnoted = unnotedId(itemId);
		String why = cannotList(unnoted);
		if (why != null) {
			c.sendMessage(why);
			return false;
		}
		if (amount <= 0 || price < 0) {
			c.sendMessage("Invalid listing details.");
			return false;
		}
		if (ownedCount(c, unnoted) < amount) {
			c.sendMessage("You don't have enough of that item.");
			return false;
		}
		synchronized (LOCK) {
			PlayerShop shop = getPlayerShopUnlocked(c.playerName);
			for (int i = 0; i < shop.listings.size(); i++) {
				ShopListing existing = shop.listings.get(i);
				if (existing.itemId == unnoted && existing.price == price) {
					if (!takeOwned(c, unnoted, amount)) {
						c.sendMessage("Failed to remove those items from your inventory.");
						return false;
					}
					existing.amount += amount;
					shop.lastUpdated = System.currentTimeMillis();
					dirty = true;
					saveShopsUnlocked();
					c.sendMessage("Added to your existing " + getItemName(unnoted) + " listing @ " + price + "gp each.");
					return true;
				}
			}
			if (shop.listings.size() >= MAX_LISTINGS) {
				c.sendMessage("Your shop is full (max " + MAX_LISTINGS + " listings).");
				return false;
			}
			if (!takeOwned(c, unnoted, amount)) {
				c.sendMessage("Failed to remove those items from your inventory.");
				return false;
			}
			ShopListing listing = new ShopListing(c.playerName, unnoted, amount, price);
			shop.listings.add(listing);
			shop.lastUpdated = System.currentTimeMillis();
			allListings.add(0, listing);
			dirty = true;
			saveShopsUnlocked();
			return true;
		}
	}

	public static boolean removeListing(String ownerName, int index) {
		synchronized (LOCK) {
			PlayerShop shop = getPlayerShopUnlocked(ownerName);
			if (index < 0 || index >= shop.listings.size()) {
				return false;
			}
			ShopListing removed = shop.listings.remove(index);
			allListings.remove(removed);
			shop.lastUpdated = System.currentTimeMillis();
			dirty = true;
			saveShopsUnlocked();
			return true;
		}
	}

	public static ShopListing removeListingAndGetDetails(String ownerName, int index) {
		synchronized (LOCK) {
			PlayerShop shop = getPlayerShopUnlocked(ownerName);
			if (index < 0 || index >= shop.listings.size()) {
				return null;
			}
			ShopListing removed = shop.listings.remove(index);
			allListings.remove(removed);
			shop.lastUpdated = System.currentTimeMillis();
			dirty = true;
			saveShopsUnlocked();
			return removed;
		}
	}

	public static boolean reclaimListing(Client c, long listingId) {
		if (c == null) {
			return false;
		}
		synchronized (LOCK) {
			ShopListing listing = findByIdUnlocked(listingId);
			if (listing == null || !listing.ownerName.equalsIgnoreCase(c.playerName)) {
				c.sendMessage("That listing is no longer available.");
				return false;
			}
			PlayerShop shop = getPlayerShopUnlocked(c.playerName);
			shop.listings.remove(listing);
			allListings.remove(listing);
			shop.lastUpdated = System.currentTimeMillis();
			if (!giveShopItems(c, listing.itemId, listing.amount)) {
				addCollectUnlocked(shop, listing.itemId, listing.amount);
				c.sendMessage("Inventory was full. " + listing.amount + "x " + getItemName(listing.itemId)
						+ " was sent to your shop collect box. Use Collect items.");
			} else {
				c.sendMessage("Removed listing and returned " + listing.amount + "x " + getItemName(listing.itemId) + ".");
			}
			dirty = true;
			saveShopsUnlocked();
			return true;
		}
	}

	public static boolean reclaimListingByIndex(Client c, int index) {
		synchronized (LOCK) {
			PlayerShop shop = getPlayerShopUnlocked(c.playerName);
			if (index < 0 || index >= shop.listings.size()) {
				c.sendMessage("Failed to remove listing.");
				return false;
			}
			return reclaimListing(c, shop.listings.get(index).listingId);
		}
	}

	public static List<ShopListing> getAllListings() {
		synchronized (LOCK) {
			return new ArrayList<ShopListing>(allListings);
		}
	}

	public static List<ShopListing> searchByItem(int itemId) {
		synchronized (LOCK) {
			int unnoted = unnotedId(itemId);
			List<ShopListing> results = new ArrayList<ShopListing>();
			for (ShopListing listing : allListings) {
				if (listing.itemId == unnoted) {
					results.add(listing);
				}
			}
			return results;
		}
	}

	public static List<ShopListing> searchByItemName(String itemName) {
		synchronized (LOCK) {
			List<ShopListing> results = new ArrayList<ShopListing>();
			if (itemName == null) {
				return results;
			}
			String query = itemName.toLowerCase().trim();
			try {
				int asId = Integer.parseInt(query);
				return searchByItem(asId);
			} catch (NumberFormatException ignored) {
			}
			for (ShopListing listing : allListings) {
				String name = getItemName(listing.itemId).toLowerCase();
				if (name.contains(query) || name.replace(" ", "").contains(query.replace(" ", ""))) {
					results.add(listing);
				}
			}
			return results;
		}
	}

	public static List<ShopListing> searchByOwner(String ownerName) {
		synchronized (LOCK) {
			List<ShopListing> results = new ArrayList<ShopListing>();
			PlayerShop shop = playerShops.get(ownerName.toLowerCase());
			if (shop != null) {
				results.addAll(shop.listings);
			}
			return results;
		}
	}

	public static List<ShopListing> getRecentListings() {
		synchronized (LOCK) {
			int count = Math.min(20, allListings.size());
			return new ArrayList<ShopListing>(allListings.subList(0, count));
		}
	}

	public static int indexOfListing(String sellerName, ShopListing listing) {
		synchronized (LOCK) {
			PlayerShop shop = playerShops.get(sellerName.toLowerCase());
			if (shop == null) {
				return -1;
			}
			return shop.listings.indexOf(listing);
		}
	}

	public static long claimGold(String ownerName) {
		synchronized (LOCK) {
			PlayerShop shop = getPlayerShopUnlocked(ownerName);
			long amount = shop.pendingGold;
			if (amount <= 0) {
				return 0;
			}
			long give = Math.min(amount, Integer.MAX_VALUE);
			shop.pendingGold = amount - give;
			shop.lastUpdated = System.currentTimeMillis();
			dirty = true;
			saveShopsUnlocked();
			return give;
		}
	}

	public static boolean claimGold(Client c) {
		long claimed = claimGold(c.playerName);
		if (claimed <= 0) {
			c.sendMessage("You have no gold to claim.");
			return false;
		}
		if (!c.getItems().addItem(995, (int) claimed)) {
			synchronized (LOCK) {
				PlayerShop shop = getPlayerShopUnlocked(c.playerName);
				shop.pendingGold += claimed;
				dirty = true;
				saveShopsUnlocked();
			}
			c.sendMessage("You don't have space for those coins. Gold was left in your shop.");
			return false;
		}
		c.sendMessage("You claimed " + claimed + " coins from your shop.");
		PlayerShop shop = getPlayerShop(c.playerName);
		if (shop.pendingGold > 0) {
			c.sendMessage("Your shop still has " + shop.pendingGold + " coins waiting. Claim again.");
		}
		return true;
	}

	public static boolean buyItem(String buyerName, String sellerName, int listingIndex) {
		synchronized (LOCK) {
			PlayerShop shop = playerShops.get(sellerName.toLowerCase());
			if (shop == null || listingIndex < 0 || listingIndex >= shop.listings.size()) {
				return false;
			}
			ShopListing listing = shop.listings.get(listingIndex);
			long totalCost = (long) listing.price * (long) listing.amount;
			shop.listings.remove(listingIndex);
			allListings.remove(listing);
			shop.pendingGold += totalCost;
			shop.lastUpdated = System.currentTimeMillis();
			dirty = true;
			saveShopsUnlocked();
			return true;
		}
	}

	public static boolean buyListing(Client buyer, long listingId) {
		ShopListing listing = getListingById(listingId);
		if (listing == null) {
			if (buyer != null) {
				buyer.sendMessage("That listing is no longer available.");
			}
			return false;
		}
		return buyListing(buyer, listingId, listing.amount);
	}

	public static boolean buyListing(Client buyer, long listingId, int amount) {
		if (buyer == null) {
			return false;
		}
		if (amount < 1) {
			buyer.sendMessage("You must buy at least 1.");
			return false;
		}
		synchronized (LOCK) {
			ShopListing listing = findByIdUnlocked(listingId);
			if (listing == null) {
				buyer.sendMessage("That listing is no longer available.");
				return false;
			}
			if (listing.ownerName.equalsIgnoreCase(buyer.playerName)) {
				buyer.sendMessage("You can't buy from your own shop.");
				return false;
			}
			if (amount > listing.amount) {
				amount = listing.amount;
			}
			long totalCost = (long) listing.price * (long) amount;
			if (totalCost <= 0 || totalCost > Integer.MAX_VALUE) {
				buyer.sendMessage("That purchase is too large.");
				return false;
			}
			if (buyer.getItems().getItemAmount(995) < totalCost) {
				buyer.sendMessage("You don't have enough coins. Cost: " + totalCost + "gp");
				return false;
			}
			int giveId = deliveryId(listing.itemId, amount);
			if (!buyer.getItems().hasSpaceFor(giveId, amount)) {
				buyer.sendMessage("You don't have enough inventory space.");
				return false;
			}
			PlayerShop shop = getPlayerShopUnlocked(listing.ownerName);
			int coinsBefore = buyer.getItems().getItemAmount(995);
			buyer.getItems().deleteItem2(995, (int) totalCost);
			if (coinsBefore - buyer.getItems().getItemAmount(995) < totalCost) {
				int taken = coinsBefore - buyer.getItems().getItemAmount(995);
				if (taken > 0) {
					buyer.getItems().addItem(995, taken);
				}
				buyer.sendMessage("Failed to take the coins. Purchase cancelled.");
				return false;
			}
			if (!giveShopItems(buyer, listing.itemId, amount)) {
				buyer.getItems().addItem(995, (int) totalCost);
				buyer.sendMessage("Failed to give the item. Purchase cancelled.");
				return false;
			}
			if (amount >= listing.amount) {
				shop.listings.remove(listing);
				allListings.remove(listing);
			} else {
				listing.amount -= amount;
			}
			shop.pendingGold += totalCost;
			shop.lastUpdated = System.currentTimeMillis();
			recordSaleUnlocked(shop, buyer.playerName, listing.itemId, amount, listing.price, totalCost);
			dirty = true;
			saveShopsUnlocked();
			buyer.sendMessage("Bought " + amount + "x " + getItemName(listing.itemId) + " for " + totalCost + "gp.");
			notifyPlayer(listing.ownerName, buyer.playerName + " bought " + amount + "x " + getItemName(listing.itemId)
					+ " from your shop for " + totalCost + "gp.");
			return true;
		}
	}

	public static ShopListing getListing(String sellerName, int listingIndex) {
		synchronized (LOCK) {
			PlayerShop shop = playerShops.get(sellerName.toLowerCase());
			if (shop == null || listingIndex < 0 || listingIndex >= shop.listings.size()) {
				return null;
			}
			return shop.listings.get(listingIndex);
		}
	}

	public static ShopListing getListingById(long listingId) {
		synchronized (LOCK) {
			return findByIdUnlocked(listingId);
		}
	}

	private static ShopListing findByIdUnlocked(long listingId) {
		for (ShopListing listing : allListings) {
			if (listing.listingId == listingId) {
				return listing;
			}
		}
		return null;
	}

	public static int unnotedId(int itemId) {
		if (itemId < 0) {
			return itemId;
		}
		if (itemId < Item.itemIsNote.length && Item.itemIsNote[itemId]) {
			int unnoted = itemId - 1;
			if (unnoted >= 0 && (unnoted >= Item.itemIsNote.length || !Item.itemIsNote[unnoted])) {
				return unnoted;
			}
		}
		return itemId;
	}

	public static int notedId(int unnoted) {
		int noted = unnoted + 1;
		if (noted >= 0 && noted < Item.itemIsNote.length && Item.itemIsNote[noted]) {
			return noted;
		}
		return -1;
	}

	public static int deliveryId(int unnoted, int amount) {
		unnoted = unnotedId(unnoted);
		if (amount <= 1) {
			return unnoted;
		}
		int noted = notedId(unnoted);
		if (noted != -1) {
			return noted;
		}
		return unnoted;
	}

	public static int ownedCount(Client c, int unnoted) {
		unnoted = unnotedId(unnoted);
		int total = c.getItems().getItemAmount(unnoted);
		int noted = notedId(unnoted);
		if (noted != -1) {
			total += c.getItems().getItemAmount(noted);
		}
		return total;
	}

	private static boolean takeOwned(Client c, int unnoted, int amount) {
		unnoted = unnotedId(unnoted);
		if (ownedCount(c, unnoted) < amount) {
			return false;
		}
		int remaining = amount;
		int noted = notedId(unnoted);
		if (noted != -1) {
			int haveNoted = c.getItems().getItemAmount(noted);
			int take = Math.min(haveNoted, remaining);
			if (take > 0) {
				c.getItems().deleteItem2(noted, take);
				remaining -= take;
			}
		}
		if (remaining > 0) {
			c.getItems().deleteItem2(unnoted, remaining);
		}
		return true;
	}

	public static boolean giveShopItems(Client c, int unnoted, int amount) {
		if (amount < 1) {
			return false;
		}
		int giveId = deliveryId(unnoted, amount);
		return c.getItems().addItem(giveId, amount);
	}

	private static void addCollectUnlocked(PlayerShop shop, int itemId, int amount) {
		itemId = unnotedId(itemId);
		for (int i = 0; i < shop.collect.size(); i++) {
			if (shop.collect.get(i).itemId == itemId) {
				shop.collect.get(i).amount += amount;
				return;
			}
		}
		shop.collect.add(new CollectItem(itemId, amount));
	}

	public static boolean claimCollect(Client c) {
		if (c == null) {
			return false;
		}
		synchronized (LOCK) {
			PlayerShop shop = getPlayerShopUnlocked(c.playerName);
			if (shop.collect.isEmpty()) {
				c.sendMessage("You have no items waiting in your shop collect box.");
				return false;
			}
			int claimed = 0;
			for (int i = shop.collect.size() - 1; i >= 0; i--) {
				CollectItem box = shop.collect.get(i);
				int giveId = deliveryId(box.itemId, box.amount);
				if (!c.getItems().hasSpaceFor(giveId, box.amount)) {
					continue;
				}
				if (giveShopItems(c, box.itemId, box.amount)) {
					c.sendMessage("Collected " + box.amount + "x " + getItemName(box.itemId) + ".");
					shop.collect.remove(i);
					claimed++;
				}
			}
			if (claimed > 0) {
				dirty = true;
				saveShopsUnlocked();
			}
			if (!shop.collect.isEmpty()) {
				c.sendMessage("Some items are still in the collect box. Free inventory space and try again.");
			}
			return claimed > 0;
		}
	}

	public static boolean editListingPrice(Client c, long listingId, int newPrice) {
		if (c == null) {
			return false;
		}
		if (newPrice < 0) {
			c.sendMessage("Price cannot be negative.");
			return false;
		}
		synchronized (LOCK) {
			ShopListing listing = findByIdUnlocked(listingId);
			if (listing == null || !listing.ownerName.equalsIgnoreCase(c.playerName)) {
				c.sendMessage("That listing is no longer available.");
				return false;
			}
			listing.price = newPrice;
			PlayerShop shop = getPlayerShopUnlocked(c.playerName);
			shop.lastUpdated = System.currentTimeMillis();
			dirty = true;
			saveShopsUnlocked();
			c.sendMessage(getItemName(listing.itemId) + " is now " + newPrice + "gp each.");
			return true;
		}
	}

	public static List<ShopListing> sortedListings(List<ShopListing> source, int sortMode) {
		List<ShopListing> copy = new ArrayList<ShopListing>(source);
		if (sortMode == 1) {
			Collections.sort(copy, new Comparator<ShopListing>() {
				public int compare(ShopListing a, ShopListing b) {
					if (a.price != b.price) {
						return a.price < b.price ? -1 : 1;
					}
					return Long.compare(b.listedTime, a.listedTime);
				}
			});
		} else if (sortMode == 2) {
			Collections.sort(copy, new Comparator<ShopListing>() {
				public int compare(ShopListing a, ShopListing b) {
					if (a.amount != b.amount) {
						return b.amount - a.amount;
					}
					return Long.compare(b.listedTime, a.listedTime);
				}
			});
		} else {
			Collections.sort(copy, new Comparator<ShopListing>() {
				public int compare(ShopListing a, ShopListing b) {
					return Long.compare(b.listedTime, a.listedTime);
				}
			});
		}
		return copy;
	}

	public static String priceHint(int itemId) {
		itemId = unnotedId(itemId);
		List<Integer> prices;
		synchronized (LOCK) {
			prices = saleHistory.get(itemId);
			if (prices == null || prices.isEmpty()) {
				return "No recent sales for " + getItemName(itemId) + ".";
			}
			List<Integer> copy = new ArrayList<Integer>(prices);
			int last = copy.get(copy.size() - 1);
			Collections.sort(copy);
			int median = copy.get(copy.size() / 2);
			return getItemName(itemId) + " last sold " + last + "gp (median " + median + "gp).";
		}
	}

	private static void recordSaleUnlocked(PlayerShop shop, String buyer, int itemId, int amount, int each, long total) {
		String line = buyer + " bought " + amount + "x " + getItemName(itemId) + " for " + total + "gp";
		shop.sales.add(0, line);
		while (shop.sales.size() > 8) {
			shop.sales.remove(shop.sales.size() - 1);
		}
		List<Integer> hist = saleHistory.get(itemId);
		if (hist == null) {
			hist = new ArrayList<Integer>();
			saleHistory.put(itemId, hist);
		}
		hist.add(each);
		while (hist.size() > 20) {
			hist.remove(0);
		}
	}

	private static void notifyPlayer(String name, String message) {
		if (PlayerHandler.players == null) {
			return;
		}
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			if (PlayerHandler.players[i] instanceof Client) {
				Client other = (Client) PlayerHandler.players[i];
				if (other.playerName != null && other.playerName.equalsIgnoreCase(name)) {
					other.sendMessage(message);
					return;
				}
			}
		}
	}

	public static void notifyOnLogin(Client c) {
		if (c == null) {
			return;
		}
		PlayerShop shop = getPlayerShop(c.playerName);
		if (shop.pendingGold > 0) {
			c.sendMessage("Your player shop has " + shop.pendingGold + "gp waiting. Open ::pos and Claim.");
		}
		if (!shop.collect.isEmpty()) {
			c.sendMessage("Your player shop has items in the collect box.");
		}
		if (!shop.sales.isEmpty()) {
			c.sendMessage("Last shop sale: " + shop.sales.get(0));
		}
	}

	public static void debug(Client c, String message) {
		if (Config.SERVER_DEBUG && c != null) {
			c.sendMessage("[POS] " + message);
		}
	}
}
