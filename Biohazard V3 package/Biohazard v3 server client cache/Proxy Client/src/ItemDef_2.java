import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ItemDef_2 {

	
	public static void Items(int i) {
		ItemDef itemDef = ItemDef.forID(i);
		switch (i) {

		case 15051:
			itemDef.itemActions = new String[5];
			itemDef.itemActions[1] = "Wield";
			itemDef.modelZoom = 789;
			itemDef.modelRotationY = 69;
			itemDef.modelRotationX = 1743;
			itemDef.modelOffset1 = -4;
			itemDef.modelOffset2 = -3;
			itemDef.modelID = 34411;
			itemDef.anInt165 = 6775; // male wield model
			itemDef.anInt200 = 14112; // femArmModel
			itemDef.name = "Slayer helmet";
			itemDef.description = "You don't want to wear it inside-out.".getBytes();// examine.
			break;
			
		case 405:
			itemDef.name = "2.5m Cash";
			itemDef.description = "Opening this casket will give you 2,500,000 coins.".getBytes();
			break;
			
		case 761:
			itemDef.name = "2x Global";
			itemDef.description = "Reading this will reward you will 2x experience for all skills. (30 Minutes)".getBytes();
			break;
			
		case 607:
			itemDef.name = "2x Combat";
			itemDef.description = "Reading this will reward you will 2x experience for combat skills only. (30 Minutes)".getBytes();
			break;
			
		case 608:
			itemDef.name = "2x Skilling";
			itemDef.description = "Reading this will reward you will 2x experience for non-combat skills only. (30 Minutes)".getBytes();
			break;

		case 15330:
			itemDef.name = "Halloween ring";
			itemDef.modelID = 2677;
			itemDef.modelZoom = 830;
			itemDef.modelRotationY = 322;
			itemDef.modelRotationX = 135;
			itemDef.itemActions[1] = "Wear";
			break;
			
		case 15332:
			itemDef.name = "Ring of slaying";
			itemDef.modelID = 2677;
			itemDef.modelZoom = 830;
			itemDef.modelRotationY = 322;
			itemDef.modelRotationX = 135;
			itemDef.modelOffset1 = -1;
			itemDef.modelOffset1 = 1;
			itemDef.itemActions[1] = "Wear";
			itemDef.originalModelColors = new int[1];
			itemDef.modifiedModelColors = new int[1];
			itemDef.originalModelColors[0] = 51111;
			itemDef.modifiedModelColors[0] = 127;
			break;
			
		case 15103:
			itemDef.itemActions = new String[5];
			itemDef.itemActions[1] = "Wield";
			itemDef.name = "Hammer"; // Name
			itemDef.description = "A wieldable hammer.".getBytes();
			itemDef.modelRotationY = 356;
			itemDef.modelRotationX = 2012;
			itemDef.modelOffset1 = -3;
			itemDef.modelOffset2 = -1;
			itemDef.modelID = 2376;
			itemDef.anInt165 = 491;
			itemDef.anInt200 = 491;//21886,253,167,491
			itemDef.modelZoom = 900;
			break;

		}
	}

	public static void dumpInfo(int item) {
		try {
			ItemDef itemDef = ItemDef.forID(item);
			System.out.println();
			System.out.println("Name : "+itemDef.name);
			System.out.println("Model : " + itemDef.modelID);
			System.out.println("Zoom : " + itemDef.modelZoom);
			System.out.println("RotY : " + itemDef.modelRotationY);
			System.out.println("RotX : " + itemDef.modelRotationX);
			System.out.println("Offset1 : "+itemDef.modelOffset1);
			System.out.println("Offset2 : "+itemDef.modelOffset2);
			System.out.println("anInt165(MALE WIELD) : "+itemDef.anInt165);
			System.out.println("anInt200(FEMALE WIELD) : "+itemDef.anInt200);
			System.out.println();
			System.out.println("Original Color[0] : "
					+ itemDef.originalModelColors[0]);
			System.out.println("Modified Color[0] : "
					+ itemDef.modifiedModelColors[0]);
			System.out.println("Original Color[1] : "
					+ itemDef.originalModelColors[1]);
			System.out.println("Modified Color[1] : "
					+ itemDef.modifiedModelColors[1]);
		} catch (Exception e) {

		}
	}

	private static String arrayToString(String[] arr) {
		String toReturn = "";
		for (String s : arr) {
			if (s != null) {
				toReturn += s + "\n";
			}
		}
		return toReturn;
	}

	private static String arrayToString(int[] arr) {
		String toReturn = "";
		for (int s : arr) {
			toReturn += s + "\t";
		}
		return toReturn;
	}

	public static void dumpItem(int item) {
		ItemDef itemDef = ItemDef.forID(item);
		String userHomeFolder = System.getProperty("user.home");
		File textFile = new File(userHomeFolder, +item + ".txt");
		String[] data = new String[10];
		data[0] = "itemDef.modelID = " + itemDef.modelID + ";";
		data[1] = "itemDef.modelZoom = " + itemDef.modelZoom + ";";
		data[2] = "itemDef.modelRotationX = " + itemDef.modelRotationX + ";";
		data[3] = "itemDef.modelRotationY = " + itemDef.modelRotationY + ";";
		data[4] = "itemDef.anInt165 = " + itemDef.anInt165 + ";";
		data[5] = "itemDef.anInt200 = " + itemDef.anInt200 + ";";
		data[6] = "itemDef.modelOffset1 = " + itemDef.modelOffset1 + ";";
		data[7] = "itemDef.modelOffset2 = " + itemDef.modelOffset2 + ";";
		data[8] = "Item actions : " + arrayToString(itemDef.itemActions);
		data[9] = "Original colors : "+ arrayToString(itemDef.originalModelColors);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(textFile));
			for (String s : data) {
				out.write(s);
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}