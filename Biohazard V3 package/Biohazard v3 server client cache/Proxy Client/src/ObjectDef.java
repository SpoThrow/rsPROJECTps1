import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;


public final class ObjectDef {
	
	public static boolean dumpObjects = false;

	public static ObjectDef forID(int i) {
		//if (i > streamIndices.length)
			//i = streamIndices.length - 1;
		for (int j = 0; j < 20; j++)
			if (cache[j].type == i)
				return cache[j];
		if (i > streamIndices474.length)
			return forID525(i);
		cacheIndex = (cacheIndex + 1) % 20;
		ObjectDef class46 = cache[cacheIndex];
		stream474.currentOffset = streamIndices474[i];
		class46.type = i;
		if(i > 0 && dumpObjects) {
			try {
		    File file = new File ("C:/Users/jordyortega/Desktop/objects/"+i+".txt");
		    try {
		        file.createNewFile();
		   }
		   catch (IOException e) {
		   e.printStackTrace();
		   }
		    PrintWriter printWriter;
				printWriter = new PrintWriter (file);
		    printWriter.println (i);
		    printWriter.close ();    
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		class46.setDefaults();
		class46.readValues474(stream474);
		if (i == 11214) {
			class46.name = "Empty space"; 
			class46.description = "You can build here something.".getBytes();
			class46.anInt744 = 1;
			class46.anInt761 = 1;
			class46.anIntArray773 = new int[1];
			class46.anIntArray773[0] = 50099;
			class46.actions = new String[5];
			class46.actions[0] = "Build";
			class46.hasActions = true;
			class46.aBoolean767 = false;
			class46.aBoolean762 = false;
			class46.aBoolean769 = false;
		}
		if(i == 7149 || i == 7147) {
			class46.hasActions = true;
			class46.actions = new String[1];
			class46.actions[0] = "Squeeze-Through";
			class46.name = "Gap";
		}
		if(i == 7152 || i == 7144) {
			class46.hasActions = true;
			class46.actions = new String[1];
			class46.actions[0] = "Distract";
			class46.name = "Eyes";
		}
		if (i == 2164) {
			class46.hasActions = true;
			class46.actions[0] = "Fix";
			class46.actions[1] = null;
			class46.name = "Trawler Net";
		}
		if(i == 1293) {
			class46.hasActions = true;
			class46.actions[0] = "Teleport";
			class46.actions[1] = null;
			class46.name = "Spirit Tree";
		}
		if(i == 7152 || i == 7144) {
			class46.hasActions = true;
			class46.actions = new String[1];
			class46.actions[0] = "Burn Down";
			class46.name = "Boil";
		}
		if(i == 7152 || i == 7144) {
			class46.hasActions = true;
			class46.actions = new String[1];
			class46.actions[0] = "Chop";
			class46.name = "Tendrils";
		}
		if(i == 2452) {
			class46.hasActions = true;
			class46.actions = new String[1];
			class46.actions[0] = "Go Through";
			class46.name = "Passage";
		}
		if(i == 7153) {
			class46.hasActions = true;
			class46.actions = new String[1];
			class46.actions[0] = "Mine";
			class46.name = "Rock";
		}
		if(i == 2452 || i == 2455 || i == 2456 || i == 2454 || i == 2453 || i == 2461 || i == 2457 || i == 2461 || i == 2459 || i == 2460) {
			class46.hasActions = true;
			class46.name = "Mysterious Ruins";
		}
		switch(i){
		case 10638:
			class46.hasActions = true;
			return class46;		
			}
		return class46;
	}
	
	public static ObjectDef forID525(int i) {
		for(int j = 0; j < 20; j++)
			if(cache[j].type == i)
				return cache[j];
		cacheIndex = (cacheIndex + 1) % 20;
		ObjectDef objectDef = cache[cacheIndex];
		stream525.currentOffset = streamIndices525[i];
		objectDef.type = i;
		if(i > 0 && dumpObjects) {
			try {
		    File file = new File ("C:/Users/jordyortega/Desktop/objects/"+i+".txt");
		    try {
		        file.createNewFile();
		   }
		   catch (IOException e) {
		   e.printStackTrace();
		   }
		    PrintWriter printWriter;
				printWriter = new PrintWriter (file);
		    printWriter.println (i);
		    printWriter.close ();    
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		objectDef.setDefaults();
		try {
		objectDef.readValues525(stream525);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return objectDef;
	}
	
	private void readValues525(Stream buffer) {
		int i = -1;
		label0: do {
			int opcode;
			do {
				opcode = buffer.readUnsignedByte();
				if (opcode == 0)
					break label0;
				if (opcode == 1) {
					int k = buffer.readUnsignedByte();
					if (k > 0)
						if (anIntArray773 == null || lowMem) {
							anIntArray776 = new int[k];
							anIntArray773 = new int[k];
							for (int k1 = 0; k1 < k; k1++) {
								anIntArray773[k1] = buffer.readUnsignedWord();
								anIntArray776[k1] = buffer.readUnsignedByte();
							}
						} else {
							buffer.currentOffset += k * 3;
						}
				} else if (opcode == 2)
					name = buffer.readString();
				else if (opcode == 3)
					description = buffer.readBytes();
				else if (opcode == 5) {
					int l = buffer.readUnsignedByte();
					if (l > 0)
						if (anIntArray773 == null || lowMem) {
							anIntArray776 = null;
							anIntArray773 = new int[l];
							for (int l1 = 0; l1 < l; l1++)
								anIntArray773[l1] = buffer.readUnsignedWord();
						} else {
							;//buffer.currentOffset += l * 2;
						}
				} else if (opcode == 14)
					anInt744 = buffer.readUnsignedByte();
				else if (opcode == 15)
					anInt761 = buffer.readUnsignedByte();
				else if (opcode == 17)
					aBoolean767 = false;
				else if (opcode == 18)
					aBoolean757 = false;
				else if (opcode == 19) {
					i = buffer.readUnsignedByte();
					if (i == 1)
						hasActions = true;
				} else if (opcode == 21)
					aBoolean762 = true;
				else if (opcode == 22)
					aBoolean769 = false;//
				else if (opcode == 23)
					aBoolean764 = true;
				else if (opcode == 24) {
					anInt781 = buffer.readUnsignedWord();
					if (anInt781 == 65535)
						anInt781 = -1;
				} else if (opcode == 28)
					anInt775 = buffer.readUnsignedByte();
				else if (opcode == 29)
					buffer.readSignedByte();
				else if (opcode == 39)
					buffer.readSignedByte();
				else if (opcode >= 30 && opcode < 39) {
					if (actions == null)
						actions = new String[10];
					actions[opcode - 30] = buffer.readString();
					if (actions[opcode - 30].equalsIgnoreCase("hidden"))
						actions[opcode - 30] = null;
				} else if (opcode == 40) {
					int i1 = buffer.readUnsignedByte();
					modifiedModelColors = new int[i1];
					originalModelColors = new int[i1];
					for (int i2 = 0; i2 < i1; i2++) {
						modifiedModelColors[i2] = buffer.readUnsignedWord();
						originalModelColors[i2] = buffer.readUnsignedWord();
					}
				} else if (opcode == 60)
					anInt746 = buffer.readUnsignedWord();
				else if (opcode == 62)
					aBoolean751 = true;
				else if (opcode == 64)
					aBoolean779 = false;
				else if (opcode == 65)
					anInt748 = buffer.readUnsignedWord();
				else if (opcode == 66)
					anInt772 = buffer.readUnsignedWord();
				else if (opcode == 67)
					anInt740 = buffer.readUnsignedWord();
				else if (opcode == 68)
					anInt758 = buffer.readUnsignedWord();
				else if (opcode == 69)
					anInt768 = buffer.readUnsignedByte();
				else if (opcode == 70)
					anInt738 = buffer.readSignedWord();
				else if (opcode == 71)
					anInt745 = buffer.readSignedWord();
				else if (opcode == 72)
					anInt783 = buffer.readSignedWord();
				else if (opcode == 73)
					aBoolean736 = true;
				else if (opcode == 74) {
					aBoolean766 = true;
				} else {
					if (opcode != 75)
						continue;
					anInt760 = buffer.readUnsignedByte();
				}
				continue label0;
			} while (opcode != 77);
				anInt774 = buffer.readUnsignedWord();
			if (anInt774 == 65535)
				anInt774 = -1;
				anInt749 = buffer.readUnsignedWord();
			if (anInt749 == 65535)
				anInt749 = -1;
			int j1 = buffer.readUnsignedByte();
			childrenIDs = new int[j1 + 1];
			for (int j2 = 0; j2 <= j1; j2++) {
				childrenIDs[j2] = buffer.readUnsignedWord();
				if (childrenIDs[j2] == 65535)
					childrenIDs[j2] = -1;
			}

		} while (true);
		if (i == -1) {
			hasActions = anIntArray773 != null && (anIntArray776 == null || anIntArray776[0] == 10);
			if (actions != null)
				hasActions = true;
		}
		if (aBoolean766) {
			aBoolean767 = false;
			aBoolean757 = false;
		}
		if (anInt760 == -1)
			anInt760 = aBoolean767 ? 1 : 0;
    }

	public void setDefaults() {
		anIntArray773 = null;
		anIntArray776 = null;
		name = null;
		description = null;
		modifiedModelColors = null;
		originalModelColors = null;
		anInt744 = 1;
		anInt761 = 1;
		aBoolean767 = true;
		aBoolean757 = true;
		hasActions = false;
		aBoolean762 = false;
		aBoolean769 = false;
		aBoolean764 = false;
		anInt781 = -1;
		anInt775 = 16;
		aByte737 = 0;
		aByte742 = 0;
		actions = null;
		anInt746 = -1;
		anInt758 = -1;
		aBoolean751 = false;
		aBoolean779 = true;
		anInt748 = 128;
		anInt772 = 128;
		anInt740 = 128;
		anInt768 = 0;
		anInt738 = 0;
		anInt745 = 0;
		anInt783 = 0;
		aBoolean736 = false;
		aBoolean766 = false;
		anInt760 = -1;
		anInt774 = -1;
		anInt749 = -1;
		childrenIDs = null;
	}

	public void method574(OnDemandFetcher class42_sub1) {
		if (anIntArray773 == null)
			return;
		for (int j = 0; j < anIntArray773.length; j++)
			class42_sub1.method560(anIntArray773[j] & 0xffff, 0);
	}

	public static void nullLoader() {
		mruNodes1 = null;
		mruNodes2 = null;
		streamIndices474 = null;
		cache = null;
		stream474 = null;
	}

	public static void unpackConfig(StreamLoader streamLoader) {
		stream474 = new Stream(streamLoader.getDataForName("loc.dat"));
		Stream idxBuffer474 = new Stream(streamLoader.getDataForName("loc.idx"));
		
		stream525 = new Stream(streamLoader.getDataForName("525loc.dat"));
		Stream idxBuffer525 = new Stream(streamLoader.getDataForName("525loc.idx"));
		int totalObjects474 = idxBuffer474.readUnsignedWord();
		int totalObjects525 = idxBuffer525.readUnsignedWord();
		System.out.println("474 Object Amount: " + totalObjects474);
		System.out.println("525 Object Amount: " + totalObjects525);
		streamIndices474 = new int[totalObjects474];
		int i = 2;
		for (int j = 0; j < totalObjects474; j++) {
			streamIndices474[j] = i;
			i += idxBuffer474.readUnsignedWord();
		}
		totalObjects474 = totalObjects525;
		streamIndices525 = new int[totalObjects474];
		i = 2;
		for (int j = 0; j < totalObjects474; j++) {
			streamIndices525[j] = i;
			i += idxBuffer525.readUnsignedWord();
		}
		cache = new ObjectDef[20];
		for (int k = 0; k < 20; k++)
			cache[k] = new ObjectDef();
	}

	public boolean method577(int i) {
		if (anIntArray776 == null) {
			if (anIntArray773 == null)
				return true;
			if (i != 10)
				return true;
			boolean flag1 = true;
			for (int k = 0; k < anIntArray773.length; k++)
				flag1 &= Model.method463(anIntArray773[k] & 0xffff);

			return flag1;
		}
		for (int j = 0; j < anIntArray776.length; j++)
			if (anIntArray776[j] == i)
				return Model.method463(anIntArray773[j] & 0xffff);

		return true;
	}

	public Model method578(int i, int j, int k, int l, int i1, int j1, int k1) {
		Model model = method581(i, k1, j);
		if (model == null)
			return null;
		if (aBoolean762 || aBoolean769)
			model = new Model(aBoolean762, aBoolean769, model);
		if (aBoolean762) {
			int l1 = (k + l + i1 + j1) / 4;
			for (int i2 = 0; i2 < model.anInt1626; i2++) {
				int j2 = model.anIntArray1627[i2];
				int k2 = model.anIntArray1629[i2];
				int l2 = k + ((l - k) * (j2 + 64)) / 128;
				int i3 = j1 + ((i1 - j1) * (j2 + 64)) / 128;
				int j3 = l2 + ((i3 - l2) * (k2 + 64)) / 128;
				model.anIntArray1628[i2] += j3 - l1;
			}

			model.method467();
		}
		return model;
	}

	public boolean method579() {
		if (anIntArray773 == null)
			return true;
		boolean flag1 = true;
		for (int i = 0; i < anIntArray773.length; i++)
			flag1 &= Model.method463(anIntArray773[i] & 0xffff);
		return flag1;
	}

	public ObjectDef method580() {
		int i = -1;
		if (anInt774 != -1) {
			VarBit varBit = VarBit.cache[anInt774];
			int j = varBit.anInt648;
			int k = varBit.anInt649;
			int l = varBit.anInt650;
			int i1 = client.anIntArray1232[l - k];
			i = clientInstance.variousSettings[j] >> k & i1;
		} else if (anInt749 != -1)
			i = clientInstance.variousSettings[anInt749];
		if (i < 0 || i >= childrenIDs.length || childrenIDs[i] == -1)
			return null;
		else
			return forID(childrenIDs[i]);
	}

	public Model method581(int j, int k, int l) {
		Model model = null;
		long l1;
		if (anIntArray776 == null) {
			if (j != 10)
				return null;
			l1 = (long) ((type << 6) + l) + ((long) (k + 1) << 32);
			Model model_1 = (Model) mruNodes2.insertFromCache(l1);
			if (model_1 != null)
				return model_1;
			if (anIntArray773 == null)
				return null;
			boolean flag1 = aBoolean751 ^ (l > 3);
			int k1 = anIntArray773.length;
			for (int i2 = 0; i2 < k1; i2++) {
				int l2 = anIntArray773[i2];
				if (flag1)
					l2 += 0x10000;
				model = (Model) mruNodes1.insertFromCache(l2);
				if (model == null) {
					model = Model.method462(l2 & 0xffff);
					if (model == null)
						return null;
					if (flag1)
						model.method477();
					mruNodes1.removeFromCache(model, l2);
				}
				if (k1 > 1)
					aModelArray741s[i2] = model;
			}

			if (k1 > 1)
				model = new Model(k1, aModelArray741s);
		} else {
			int i1 = -1;
			for (int j1 = 0; j1 < anIntArray776.length; j1++) {
				if (anIntArray776[j1] != j)
					continue;
				i1 = j1;
				break;
			}

			if (i1 == -1)
				return null;
			l1 = (long) ((type << 8) + (i1 << 3) + l) + ((long) (k + 1) << 32);
			Model model_2 = (Model) mruNodes2.insertFromCache(l1);
			if (model_2 != null)
				return model_2;
			int j2 = anIntArray773[i1];
			boolean flag3 = aBoolean751 ^ (l > 3);
			if (flag3)
				j2 += 0x10000;
			model = (Model) mruNodes1.insertFromCache(j2);
			if (model == null) {
				model = Model.method462(j2 & 0xffff);
				if (model == null)
					return null;
				if (flag3)
					model.method477();
				mruNodes1.removeFromCache(model, j2);
			}
		}
		boolean flag;
		flag = anInt748 != 128 || anInt772 != 128 || anInt740 != 128;
		boolean flag2;
		flag2 = anInt738 != 0 || anInt745 != 0 || anInt783 != 0;
		Model model_3 = new Model(modifiedModelColors == null, Class36
				.method532(k), l == 0 && k == -1 && !flag && !flag2, model);
		if (k != -1) {
			model_3.method469();
			model_3.method470(k);
			model_3.anIntArrayArray1658 = null;
			model_3.anIntArrayArray1657 = null;
		}
		while (l-- > 0)
			model_3.method473();
		if (modifiedModelColors != null) {
			for (int k2 = 0; k2 < modifiedModelColors.length; k2++)
				model_3.method476(modifiedModelColors[k2],
						originalModelColors[k2]);

		}
		if (flag)
			model_3.method478(anInt748, anInt740, anInt772);
		if (flag2)
			model_3.method475(anInt738, anInt745, anInt783);
		//model_3.method479(64 + aByte737, 768 + aByte742 * 5, -50, -10, -50,
				//!aBoolean769);
		model_3.method479(74, 1000, -90, -580, -90, !aBoolean769);
		if (anInt760 == 1)
			model_3.anInt1654 = model_3.modelHeight;
		mruNodes2.removeFromCache(model_3, l1);
		return model_3;
	}

	public void readValues474(Stream stream) {
		int flag = -1;
		do {
			int type = stream.readUnsignedByte();
			if (type == 0)
				break;
			if (type == 1) {
				int len = stream.readUnsignedByte();
				if (len > 0) {
					if (anIntArray773 == null || lowMem) {
						anIntArray776 = new int[len];
						anIntArray773 = new int[len];
						for (int k1 = 0; k1 < len; k1++) {
							anIntArray773[k1] = stream.readUnsignedWord();
							anIntArray776[k1] = stream.readUnsignedByte();
						}
					} else {
						stream.currentOffset += len * 3;
					}
				}
			} else if (type == 2)
				name = stream.readNewString();
			else if (type == 3)
				description = stream.readBytes();
			else if (type == 5) {
				int len = stream.readUnsignedByte();
				if (len > 0) {
					if (anIntArray773 == null || lowMem) {
						anIntArray776 = null;
						anIntArray773 = new int[len];
						for (int l1 = 0; l1 < len; l1++)
							anIntArray773[l1] = stream.readUnsignedWord();
					} else {
						stream.currentOffset += len * 2;
					}
				}
			} else if (type == 14)
				anInt744 = stream.readUnsignedByte();
			else if (type == 15)
				anInt761 = stream.readUnsignedByte();
			else if (type == 17)
				aBoolean767 = false;
			else if (type == 18)
				aBoolean757 = false;
			else if (type == 19)
				hasActions = (stream.readUnsignedByte() == 1);
			else if (type == 21)
				aBoolean762 = true;
			else if (type == 22)
				aBoolean769 = true;
			else if (type == 23)
				aBoolean764 = true;
			else if (type == 24) {
				anInt781 = stream.readUnsignedWord();
				if (anInt781 == 65535)
					anInt781 = -1;
			} else if (type == 28)
				anInt775 = stream.readUnsignedByte();
			else if (type == 29)
				aByte737 = stream.readSignedByte();
			else if (type == 39)
				aByte742 = stream.readSignedByte();
			else if (type >= 30 && type < 39) {
				if (actions == null)
					actions = new String[5];
				actions[type - 30] = stream.readNewString();
				if (actions[type - 30].equalsIgnoreCase("hidden"))
					actions[type - 30] = null;
			} else if (type == 40) {
				int i1 = stream.readUnsignedByte();
				modifiedModelColors = new int[i1];
				originalModelColors = new int[i1];
				for (int i2 = 0; i2 < i1; i2++) {
					modifiedModelColors[i2] = stream.readUnsignedWord();
					originalModelColors[i2] = stream.readUnsignedWord();
				}

			} else if (type == 60)
				anInt746 = stream.readUnsignedWord();
			else if (type == 62)
				aBoolean751 = true;
			else if (type == 64)
				aBoolean779 = false;
			else if (type == 65)
				anInt748 = stream.readUnsignedWord();
			else if (type == 66)
				anInt772 = stream.readUnsignedWord();
			else if (type == 67)
				anInt740 = stream.readUnsignedWord();
			else if (type == 68)
				anInt758 = stream.readUnsignedWord();
			else if (type == 69)
				anInt768 = stream.readUnsignedByte();
			else if (type == 70)
				anInt738 = stream.readSignedWord();
			else if (type == 71)
				anInt745 = stream.readSignedWord();
			else if (type == 72)
				anInt783 = stream.readSignedWord();
			else if (type == 73)
				aBoolean736 = true;
			else if (type == 74)
				aBoolean766 = true;
			else if (type == 75)
				anInt760 = stream.readUnsignedByte();
			else if (type == 77) {
				anInt774 = stream.readUnsignedWord();
				if (anInt774 == 65535)
					anInt774 = -1;
				anInt749 = stream.readUnsignedWord();
				if (anInt749 == 65535)
					anInt749 = -1;
				int j1 = stream.readUnsignedByte();
				childrenIDs = new int[j1 + 1];
				for (int j2 = 0; j2 <= j1; j2++) {
					childrenIDs[j2] = stream.readUnsignedWord();
					if (childrenIDs[j2] == 65535)
						childrenIDs[j2] = -1;
				}
			}
		} while (true);
		if (flag == -1  && name != "null" && name != null) {
			hasActions = anIntArray773 != null
			&& (anIntArray776 == null || anIntArray776[0] == 10);
			if (actions != null)
				hasActions = true;
		}
		if (aBoolean766) {
			aBoolean767 = false;
			aBoolean757 = false;
		}
		if (anInt760 == -1)
			anInt760 = aBoolean767 ? 1 : 0;
	}

	public ObjectDef() {
		type = -1;
	}

	public boolean aBoolean736;
	public byte aByte737;
	public int anInt738;
	public String name;
	public int anInt740;
	public static final Model[] aModelArray741s = new Model[4];
	public byte aByte742;
	public int anInt744;
	public int anInt745;
	public int anInt746;
	public int[] originalModelColors;
	public int anInt748;
	public int anInt749;
	public boolean aBoolean751;
	public static boolean lowMem;
	public static Stream stream474;
	private static Stream stream525;
	public int type;
	public static int[] streamIndices474;
	private static int[] streamIndices525;
	public boolean aBoolean757;
	public int anInt758;
	public int childrenIDs[];
	public int anInt760;
	public int anInt761;
	public boolean aBoolean762;
	public boolean aBoolean764;
	public static client clientInstance;
	public boolean aBoolean766;
	public boolean aBoolean767;
	public int anInt768;
	public boolean aBoolean769;
	public static int cacheIndex;
	public int anInt772;
	public int[] anIntArray773;
	public int anInt774;
	public int anInt775;
	public int[] anIntArray776;
	public byte description[];
	public boolean hasActions;
	public boolean aBoolean779;
	public static MRUNodes mruNodes2 = new MRUNodes(30);
	public int anInt781;
	public static ObjectDef[] cache;
	public int anInt783;
	public int[] modifiedModelColors;
	public static MRUNodes mruNodes1 = new MRUNodes(500);
	public String actions[];

}
