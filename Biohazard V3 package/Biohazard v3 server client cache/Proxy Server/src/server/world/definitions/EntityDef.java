package server.world.definitions;

import core.util.FileOperations;
import core.util.Stream;

public class EntityDef {

	public static final EntityDef forID(int i) {
		for (int j = 0; j < 20; j++)
			if (cache[j].type == (long) i)
				return cache[j];

		cacheIndex = (cacheIndex + 1) % 20;
		EntityDef npcDef = cache[cacheIndex] = new EntityDef();
		if (i < streamIndices.length)
			npcData.currentOffset = streamIndices[i];
		npcDef.type = i;
		if (i < totalNpcs)
			npcDef.readValues(npcData);
		return npcDef;
	}

	public static int NPCAMOUNT = 8000;

	public static final void unpackConfig() {
		npcData = new Stream(FileOperations.ReadFile("./Data/data/npc.dat"));
		Stream indexStream = new Stream(
				FileOperations.ReadFile("./Data/data/npc.idx"));
		totalNpcs = indexStream.readUnsignedWord();
		newTotalNpcs = 15505;
		streamIndices = new int[totalNpcs];
		int i = 2;
		for (int j = 0; j < totalNpcs; j++) {
			streamIndices[j] = i;
			i += indexStream.readUnsignedWord();
		}

		cache = new EntityDef[20];
		for (int k = 0; k < 20; k++)
			cache[k] = new EntityDef();
		System.out.println("Succesfully loaded npc config");
	}

	private final void readValues(Stream str) {
		do {
			int opcode = str.readUnsignedByte();
			if (opcode == 0)
				return;
			if (opcode == 1) {
				int j = str.readUnsignedByte();
				for (int j1 = 0; j1 < j; j1++)
					str.readUnsignedWord();
			} else if (opcode == 2)
				name = str.readString().replaceAll("_", " ");
			else if (opcode == 3)
				examine = str.readString();
			else if (opcode == 12)
				boundDim = str.readSignedByte();
			else if (opcode == 13)
				standAnim = str.readUnsignedWord();
			else if (opcode == 14)
				walkAnim = str.readUnsignedWord();
			else if (opcode == 17) {
				walkAnim = str.readUnsignedWord();
				str.readUnsignedWord();
				str.readUnsignedWord();
				str.readUnsignedWord();
			} else if (opcode >= 30 && opcode < 40) {
				if (actions == null)
					actions = new String[10];
				try {
					actions[opcode - 30] = str.readString();
					if (actions[opcode - 30].equalsIgnoreCase("hidden"))
						actions[opcode - 30] = null;
				} catch (Exception e) {
				}
			} else if (opcode == 40) {
				int k = str.readSignedByte();
				for (int k1 = 0; k1 < k; k1++) {
					str.readUnsignedWord();
					str.readUnsignedWord();
				}
			} else if (opcode == 60) {
				int l = str.readUnsignedByte();
				for (int l1 = 0; l1 < l; l1++)
					str.readUnsignedWord();
			} else if (opcode == 90)
				str.readUnsignedWord();
			else if (opcode == 91)
				str.readUnsignedWord();
			else if (opcode == 92)
				str.readUnsignedWord();
			else if (opcode == 93) {
			} else if (opcode == 95)
				combatLevel = str.readUnsignedWord();
			else if (opcode == 97)
				str.readUnsignedWord();
			else if (opcode == 98)
				str.readUnsignedWord();
			else if (opcode == 99) {
			} else if (opcode == 100)
				str.readSignedByte();
			else if (opcode == 101)
				str.readSignedByte();
			else if (opcode == 102)
				str.readUnsignedWord();
			else if (opcode == 103)
				str.readUnsignedWord();
			else if (opcode == 106) {
				str.readUnsignedWord();
				str.readUnsignedWord();
				int i1 = str.readUnsignedByte();
				for (int i2 = 0; i2 <= i1; i2++) {
					str.readUnsignedWord();
				}
			} else if (opcode == 107) {

			}
		} while (true);
	}

	EntityDef() {
		boundDim = 1;
		type = -1L;
	}

	public int combatLevel;
	public int walkAnim;
	public int standAnim;
	private static int cacheIndex;
	private static Stream npcData;
	public static int totalNpcs;
	public static int newTotalNpcs;
	public String name;
	public String actions[];
	public byte boundDim;
	private static int streamIndices[];
	public long type;
	public static EntityDef cache[];
	public String examine;
}
