package server.game.content;

import java.io.*;
import java.util.*;

/**
 * Player Owned Shop System
 * Allows players to create and manage their own shops
 */
public class PlayerOwnedShop {
    
    // Shop listing data
    public static class ShopListing {
        public String ownerName;
        public int itemId;
        public int amount;
        public int price;
        public long listedTime;
        
        public ShopListing(String ownerName, int itemId, int amount, int price) {
            this.ownerName = ownerName;
            this.itemId = itemId;
            this.amount = amount;
            this.price = price;
            this.listedTime = System.currentTimeMillis();
        }
        
        public String toString() {
            return ownerName + ":" + itemId + ":" + amount + ":" + price + ":" + listedTime;
        }
        
        public static ShopListing fromString(String str) {
            String[] parts = str.split(":");
            if (parts.length >= 5) {
                ShopListing listing = new ShopListing(
                    parts[0],
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
                );
                listing.listedTime = Long.parseLong(parts[4]);
                return listing;
            }
            return null;
        }
    }
    
    // Player shop data
    public static class PlayerShop {
        public String ownerName;
        public List<ShopListing> listings;
        public long lastUpdated;
        public long pendingGold;
        
        public PlayerShop(String ownerName) {
            this.ownerName = ownerName;
            this.listings = new ArrayList<>();
            this.lastUpdated = System.currentTimeMillis();
            this.pendingGold = 0;
        }
        
        public void addListing(ShopListing listing) {
            listings.add(listing);
            lastUpdated = System.currentTimeMillis();
        }
        
        public void removeListing(int index) {
            if (index >= 0 && index < listings.size()) {
                listings.remove(index);
                lastUpdated = System.currentTimeMillis();
            }
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(ownerName).append(":").append(lastUpdated).append(":");
            for (ShopListing listing : listings) {
                sb.append(listing.toString()).append(";");
            }
            sb.append("|").append(pendingGold);
            return sb.toString();
        }
        
        public static PlayerShop fromString(String str) {
            long gold = 0;
            int pipe = str.lastIndexOf('|');
            if (pipe >= 0) {
                try {
                    gold = Long.parseLong(str.substring(pipe + 1).trim());
                } catch (NumberFormatException ignored) {
                    gold = 0;
                }
                str = str.substring(0, pipe);
            }
            String[] parts = str.split(":", 3);
            if (parts.length >= 2) {
                PlayerShop shop = new PlayerShop(parts[0]);
                shop.lastUpdated = Long.parseLong(parts[1]);
                shop.pendingGold = gold;
                if (parts.length > 2 && !parts[2].isEmpty()) {
                    String[] listingStrs = parts[2].split(";");
                    for (String listingStr : listingStrs) {
                        ShopListing listing = ShopListing.fromString(listingStr);
                        if (listing != null) {
                            shop.listings.add(listing);
                        }
                    }
                }
                return shop;
            }
            return null;
        }
    }
    
    // Global shop manager
    private static Map<String, PlayerShop> playerShops = new HashMap<>();
    private static List<ShopListing> allListings = new ArrayList<>();
    private static Map<Integer, String> itemNames = new HashMap<>();
    private static final String CONFIG_FILE = "Data/cfg/player-owned-shops.cfg";
    private static final String ITEM_CONFIG_FILE = "Data/cfg/item.cfg";
    
    // Load all shops from config
    public static void loadShops() {
        playerShops.clear();
        allListings.clear();
        loadItemNames();
        
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("shop = ") || line.startsWith("shop=")) {
                    String content = line.substring(line.indexOf("=") + 1).trim();
                    PlayerShop shop = PlayerShop.fromString(content);
                    if (shop != null) {
                        playerShops.put(shop.ownerName.toLowerCase(), shop);
                        allListings.addAll(shop.listings);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Sort listings by time (newest first)
        allListings.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
    }
    
    // Load item names for display
    private static void loadItemNames() {
        itemNames.clear();
        File itemCfgFile = new File(ITEM_CONFIG_FILE);
        if (!itemCfgFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(itemCfgFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("item = ") || line.startsWith("item=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int itemId = Integer.parseInt(parts[0]);
                            String itemName = parts[1].replace("_", " ");
                            itemNames.put(itemId, itemName);
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Get item name by ID
    public static String getItemName(int itemId) {
        return itemNames.getOrDefault(itemId, "Unknown Item");
    }
    
    // Save all shops to config
    public static void saveShops() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CONFIG_FILE))) {
            writer.println("// Player Owned Shops Configuration");
            writer.println("// Format: shop = ownerName:lastUpdated:listing1;listing2;...");
            writer.println();
            
            for (PlayerShop shop : playerShops.values()) {
                writer.println("shop = " + shop.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Get or create player shop
    public static PlayerShop getPlayerShop(String ownerName) {
        String key = ownerName.toLowerCase();
        if (!playerShops.containsKey(key)) {
            playerShops.put(key, new PlayerShop(ownerName));
        }
        return playerShops.get(key);
    }
    
    // Add a listing to player's shop
    public static boolean addListing(String ownerName, int itemId, int amount, int price) {
        PlayerShop shop = getPlayerShop(ownerName);
        ShopListing listing = new ShopListing(ownerName, itemId, amount, price);
        shop.addListing(listing);
        allListings.add(listing);
        
        // Keep sorted
        allListings.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
        
        saveShops();
        return true;
    }
    
    // Remove a listing
    public static boolean removeListing(String ownerName, int index) {
        PlayerShop shop = getPlayerShop(ownerName);
        if (index >= 0 && index < shop.listings.size()) {
            ShopListing removed = shop.listings.get(index);
            shop.removeListing(index);
            allListings.remove(removed);
            saveShops();
            return true;
        }
        return false;
    }
    
    // Remove a listing and return item details
    public static ShopListing removeListingAndGetDetails(String ownerName, int index) {
        PlayerShop shop = getPlayerShop(ownerName);
        if (index >= 0 && index < shop.listings.size()) {
            ShopListing removed = shop.listings.get(index);
            shop.removeListing(index);
            allListings.remove(removed);
            saveShops();
            return removed;
        }
        return null;
    }
    
    // Get all listings
    public static List<ShopListing> getAllListings() {
        return new ArrayList<>(allListings);
    }
    
    // Search listings by item ID
    public static List<ShopListing> searchByItem(int itemId) {
        List<ShopListing> results = new ArrayList<>();
        for (ShopListing listing : allListings) {
            if (listing.itemId == itemId) {
                results.add(listing);
            }
        }
        return results;
    }
    
    // Search listings by owner name
    public static List<ShopListing> searchByOwner(String ownerName) {
        List<ShopListing> results = new ArrayList<>();
        PlayerShop shop = playerShops.get(ownerName.toLowerCase());
        if (shop != null) {
            results.addAll(shop.listings);
        }
        return results;
    }
    
    // Get recent listings (last 20)
    public static List<ShopListing> getRecentListings() {
        List<ShopListing> recent = new ArrayList<>();
        int count = Math.min(20, allListings.size());
        for (int i = 0; i < count; i++) {
            recent.add(allListings.get(i));
        }
        return recent;
    }
    
    public static int indexOfListing(String sellerName, ShopListing listing) {
        PlayerShop shop = playerShops.get(sellerName.toLowerCase());
        if (shop == null) {
            return -1;
        }
        return shop.listings.indexOf(listing);
    }

    public static long claimGold(String ownerName) {
        PlayerShop shop = getPlayerShop(ownerName);
        long amount = shop.pendingGold;
        shop.pendingGold = 0;
        shop.lastUpdated = System.currentTimeMillis();
        saveShops();
        return amount;
    }

    // Buy an item from a listing
    public static boolean buyItem(String buyerName, String sellerName, int listingIndex) {
        PlayerShop shop = playerShops.get(sellerName.toLowerCase());
        if (shop == null || listingIndex < 0 || listingIndex >= shop.listings.size()) {
            return false;
        }
        
        ShopListing listing = shop.listings.get(listingIndex);
        
        shop.removeListing(listingIndex);
        allListings.remove(listing);
        shop.pendingGold += listing.price;
        
        saveShops();
        return true;
    }

    public static ShopListing getListing(String sellerName, int listingIndex) {
        PlayerShop shop = playerShops.get(sellerName.toLowerCase());
        if (shop == null || listingIndex < 0 || listingIndex >= shop.listings.size()) {
            return null;
        }
        return shop.listings.get(listingIndex);
    }
}
