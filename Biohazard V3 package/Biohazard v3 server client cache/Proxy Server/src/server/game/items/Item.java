package server.game.items;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import server.Config;
import server.Server;
public class Item {

	
	/* Fullbody is an item that covers your arms. */
    //private static String[] fullbody = {"top","shirt","platebody","Ahrims robetop","Karils leathertop","brassard","Robe top","robetop","platebody (t)","platebody (g)","chestplate","torso", "hauberk", "Dragon chainbody", "robe (t)"};
    /* Fullhat covers your head but not your beard. */
   // private static String[] fullhat = {"med helm","coif","Dharok's helm","hood","Initiate helm","Coif","Helm of neitiznot","Armadyl helmet","Berserker helm", "Archer helm", "Farseer helm", "Warrior helm", "Void"};
    /* Fullmask covers your entire head. */
   // private static String[] fullmask = {"helm (t)","full helm","mask","Verac's helm","Guthan's helm","Karil's coif","mask","Torag's helm", "Void", "sallet"};
	
	public static String getItemName(int id) {
		for (int j = 0; j < Server.itemHandler.ItemList.length; j++) {
			if (Server.itemHandler.ItemList[j] != null)
				if (Server.itemHandler.ItemList[j].itemId == id)
					return Server.itemHandler.ItemList[j].itemName;
		}
		return null;
	}

	public static boolean playerCape(int itemId) {
		String[] data = {
			"TokHaar-Kal", "Ava's accumulator","cloak", "cape", "Ava's attractor",
			"bonesack", "Bonesack", "Cape", "apparatus"
		};
		String item = getItemName(itemId);
		if (item == null) {
			return false;
		}
		boolean item1 = false;
		for(int i = 0; i < data.length; i++ ) {
			if(item.endsWith(data[i]) || item.contains(data[i])) {
				item1 = true;
			}
		}
		return item1;
	}

	public static boolean playerBoots(int itemId) {
		String[] data = {
			"Glaiven boots","Ragefire boots","Steadfast boots", "Flippers","Shoes", "shoes", "boots", "Boots"
		};
		String item = getItemName(itemId);
		if (item == null) {
			return false;
		}
		boolean item1 = false;
		for(int i = 0; i < data.length; i++ ) {
			if(item.endsWith(data[i]) || item.contains(data[i])) {
				item1 = true;
			}
		}
		return item1;
	}

	public static boolean playerGloves(int itemId) {
		String[] data = {
			"Combat bracelet (4)", "Combat bracelet", "Bracelet", "Gloves", "gloves", "glove", "Glove", "gauntlets", "Gauntlets", "vamb"
		};
		String item = getItemName(itemId);
		if (item == null) {
			return false;
		}
		boolean item1 = false;
		for(int i = 0; i < data.length; i++ ) {
			if(item.endsWith(data[i]) || item.contains(data[i])) {
				item1 = true;
			}
		}
		return item1;
	}

	public static boolean playerShield(int itemId) {
		String[] data = {
			"lantern", "Toktz-ket-xil","defender","kiteshield", "Book", "book", "Kiteshield", "shield", "Shield", "Kite", "kite", "spirit", "Mages' book"
		};
		String item = getItemName(itemId);
		if (item == null) {
			return false;
		}
		boolean item1 = false;
		for(int i = 0; i < data.length; i++ ) {
			if(item.endsWith(data[i]) || item.contains(data[i])) {
				item1 = true;
			}
		}
		return item1;
	}

	public static boolean playerAmulet(int itemId) {
		String[] data = {
				"Gnome scarf","scarf","Phoenix necklace","necklace","amulet", "Amulet", "Pendant", "pendant", "Symbol", "symbol", "Arcane stream"
		};
		String item = getItemName(itemId);
		if (item == null) {
			return false;
		}
		boolean item1 = false;
		for(int i = 0; i < data.length; i++ ) {
			if(item.endsWith(data[i]) || item.contains(data[i])) {
				item1 = true;
			}
		}
		return item1;
	}

	public static boolean playerArrows(int itemId) {
		String[] data = {
			"Arrows", "arrows", "Arrow", "arrow", "Bolts", "bolts", "Bolt rack"
		};
		String item = getItemName(itemId);
		if (item == null) {
			return false;
		}
		boolean item1 = false;
		for(int i = 0; i < data.length; i++ ) {
			if(item.endsWith(data[i]) || item.contains(data[i])) {
				item1 = true;
			}
		}
		return item1;
	}

	public static boolean playerRings(int itemId) {
		String[] data = {
			"ring", "rings", "Ring", "Rings",
		};
		String item = getItemName(itemId);
		if (item == null) {
			return false;
		}
		boolean item1 = false;
		for(int i = 0; i < data.length; i++ ) {
			if(item.endsWith(data[i]) || item.contains(data[i])) {
				item1 = true;
			}
		}
		return item1;
	}

	public static boolean playerHats(int itemId) {
		String[] data = {
				"Wolf mask","Bat mask","Penguin mask","Cat mask","Guthix mitre","Saradomin mitre","Zamorak mitre","mitre","Feather headdress","boater", "cowl", "peg", "coif", "helm", 
			"coif", "mask", "hat", "headband", "hood", "headdress",
			"disguise", "cavalier", "full", "tiara", "Tiara",
			"helmet", "Hat", "ears", "partyhat", "helm(t)", "Sleeping cap",
			"helm(g)", "beret", "facemask", "sallet", "A powdered wig",
			"hat(g)", "hat(t)", "bandana", "Helm", "Bearhead"
		};
		String item = getItemName(itemId);
		if (item == null) {
			return false;
		}
		boolean item1 = false;
		for(int i = 0; i < data.length; i++ ) {
			if((item.endsWith(data[i]) || item.contains(data[i])) && (!item.contains("rystal bow full"))) {
				item1 = true;
			}
		}
		return item1;
	}

	public static boolean playerLegs(int itemId) {
		String[] data = {
			"tassets", "chaps", "bottoms", "gown", "trousers", 
			"platelegs", "robe", "plateskirt", "legs", "leggings", 
			"shorts", "Skirt", "skirt", "cuisse", "Trousers", "Pantaloons"
		};
		String item = getItemName(itemId);
		if (item == null) {
			return false;
		}
		boolean item1 = false;
		for(int i = 0; i < data.length; i++ ) {
			if((item.endsWith(data[i]) || item.contains(data[i])) && (!item.contains("top") && (!item.contains("robe (g)") && (!item.contains("robe (t)") && (!item.contains("Doctor") && (!item.contains("Priest gown"))))))) {
				item1 = true;
			}
		}
		return item1;
	}

    public static boolean playerBody(int itemId) {
        String[] data = {
            "body", "top", "Priest gown", "apron", "shirt", "Doctors' gown",
            "platebody", "robetop", "body(g)", "body(t)", "Doctor",
            "Wizard robe (g)", "Wizard robe (t)", "body", "brassard", "blouse", 
            "tunic", "leathertop", "Saradomin plate", "chainbody", 
            "hauberk", "Shirt", "torso", "chestplate", "Saradomin", "Guthix", "Zamorak"
        };
        String item = getItemName(itemId);
        if (item == null || item.contains("mjolnir") || item.contains("aradomin sword") || item.contains("godsword")) {
            return false;
        }
        boolean item1 = false;
        for(int i = 0; i < data.length; i++ ) {
            if(item.endsWith(data[i]) || item.contains(data[i])) {
                item1 = true;
            }
        }
        return item1;
    }

	private static String[] fullbody = {
		"top", "chestplate", "shirt","platebody","Ahrims robetop",
		"Karils leathertop","brassard","Robe top","robetop",
		"platebody (t)","platebody (g)","chestplate",
		"torso", "hauberk", "Dragon chainbody", "Doctors' gown"
	};

	private static String[] fullhat = {
		"Wolf mask","Bat mask","Penguin mask","witchdoctor mask","Cat mask","med helm", "coif", "Dharok's helm", "hood", "Initiate helm",
		"Coif","Helm of neitiznot","Armadyl helmet","Berserker helm", "mage helm",
		"Archer helm", "Farseer helm", "Warrior helm", "Void", "Lumberjack hat",
		"melee helm", "ranger helm"
	};

	private static String[] fullmask = {
		"Bearhead","witchdoctor mask","Wolf mask","Bat mask","Penguin mask","Cat mask","Slayer helmet","Full slayer helmet", "full helm", "mask", "Verac's helm", "Guthan's helm", "Karil's coif", "mask", "Torag's helm", "sallet", "Saradomin helm",
	};
	
	
	public static boolean isFullBody(int itemId) {
        String weapon = getItemName(itemId);
		if (weapon == null)
			return false;
        for (int i = 0; i < fullbody.length; i++) {
            if (weapon.endsWith(fullbody[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFullHelm(int itemId) {
        String weapon = getItemName(itemId);
		if (weapon == null)
			return false;
        for (int i = 0; i < fullhat.length; i++) {
            if (weapon.endsWith(fullhat[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFullMask(int itemId) {
        String weapon = getItemName(itemId);
		if (weapon == null)
			return false;
        for (int i = 0; i < fullmask.length; i++) {
            if (weapon.endsWith(fullmask[i])) {
                return true;
            }
        }
        return false;
    }
	
	
	public static boolean[] itemStackable = new boolean[Config.ITEM_LIMIT];
	public static boolean[] itemIsNote = new boolean[Config.ITEM_LIMIT];
	public static int[] targetSlots = new int[Config.ITEM_LIMIT];
	static {
		int counter = 0;
		int c;
		
		try {
			FileInputStream dataIn = new FileInputStream(new File("./Data/data/stackable.dat"));
			while ((c = dataIn.read()) != -1) {
				if (counter >= itemStackable.length) {
					break;
				}
				if (c == 0) {
					itemStackable[counter] = false;
				} else {
					itemStackable[counter] = true;
				}
				counter++;
			}
			dataIn.close();
		} catch (IOException e) {
			System.out.println("Critical error while loading stackabledata! Trace:");
			e.printStackTrace();
		}

		counter = 0;
		
		try {
			FileInputStream dataIn = new FileInputStream(new File("./Data/data/notes.dat"));
			while ((c = dataIn.read()) != -1) {
				if (counter >= itemIsNote.length) {
					break;
				}
				if (c == 0) {
					itemIsNote[counter] = true;
				} else {
					itemIsNote[counter] = false;
				}
				counter++;
			}
			dataIn.close();
		} catch (IOException e) {
			System.out.println("Critical error while loading notedata! Trace:");
			e.printStackTrace();
		}
		
		counter = 0;
		try {
			FileInputStream dataIn = new FileInputStream(new File("./Data/data/equipment.dat"));
			while ((c = dataIn.read()) != -1) {
				if (counter >= targetSlots.length) {
					break;
				}
				targetSlots[counter++] = c;
			}
			dataIn.close();
		} catch (IOException e) {
			System.out.println("Critical error while loading notedata! Trace:");
			e.printStackTrace();
		}
		

	}


	
	

}