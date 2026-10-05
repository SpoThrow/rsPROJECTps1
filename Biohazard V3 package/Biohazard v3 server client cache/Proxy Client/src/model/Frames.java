package model;

import java.io.File;

import sign.signlink;import cache.FileOperations;
import cache.JavaUncompress;
import def.CurseData667;
import game.client;
import net.Stream;



public final class Frames {

	private static void loadPacked(byte[][] dest, String filename) {
		try {
			byte[] packed = FileOperations.ReadFile(signlink.findcachedir() + filename);
			if (packed == null) {
				return;
			}
			Stream stream = new Stream(packed);
			int count = stream.g2();
			int[] ids = new int[count];
			byte[][] blobs = new byte[count][];
			int maxId = 0;
			for (int i = 0; i < count; i++) {
				int fileID = stream.g2();
				int compressedSize = stream.g4();
				byte[] compressedData = stream.getData(new byte[compressedSize]);
				ids[i] = fileID;
				blobs[i] = JavaUncompress.decompress(compressedData);
				if (fileID > maxId) {
					maxId = fileID;
				}
			}
			byte[][] data = new byte[Math.max(count, maxId + 1)][];
			if (dest != null && dest.length > data.length) {
				data = new byte[dest.length][];
			}
			for (int i = 0; i < count; i++) {
				if (ids[i] >= 0 && ids[i] < data.length) {
					data[ids[i]] = blobs[i];
				}
			}
			if (filename.startsWith("Frames")) {
				frameData = data;
			} else {
				skinData = data;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void loadSkins() {
		loadPacked(skinData, "Skins.dat");
	}

	public static void loadFrames() {
		loadPacked(frameData, "Frames.dat");
		loadLooseFrameFiles();
	}

	/** Loads numeric *.dat frame archives sitting in the cache root (e.g. Nex 3502.dat). */
	private static void loadLooseFrameFiles() {
		try {
			File dir = new File(signlink.findcachedir());
			File[] files = dir.listFiles();
			if (files == null) {
				return;
			}
			for (int i = 0; i < files.length; i++) {
				String name = files[i].getName();
				if (!name.endsWith(".dat") || name.length() <= 4) {
					continue;
				}
				String base = name.substring(0, name.length() - 4);
				boolean digits = base.length() > 0;
				for (int c = 0; c < base.length(); c++) {
					char ch = base.charAt(c);
					if (ch < '0' || ch > '9') {
						digits = false;
						break;
					}
				}
				if (!digits) {
					continue;
				}
				int id = Integer.parseInt(base);
				// The slot budget is owned in ONE place (Phase 6.5.2), not by this literal.
				if (FrameSlots.isLooseSlot(id)) {
					ensureFileSlot(id);
					load_647(id);
				}
			}
		} catch (Exception e) {
		}
	}

	public static byte[] getData(int i1, int i2) {
		if (i1 == 0) {
			if (frameData == null || i2 < 0 || i2 >= frameData.length) {
				return null;
			}
			return frameData[i2];
		}
		if (skinData == null || i2 < 0 || i2 >= skinData.length) {
			return null;
		}
		return skinData[i2];
	}

	public static void load_647(int file) {
		try {
			ensureFileSlot(file);
			byte[] data = FileOperations.ReadFile(signlink.findcachedir() + file + ".dat");
			if (data == null) {
				return;
			}
			Stream stream = new Stream(data);
			Skin class18 = new Skin(stream, 0);
			int k1 = stream.readUnsignedWord();
			animationlist[file] = new Frames[(int) (k1 * 3.0)];
			decodeFrames(stream, class18, k1, file);
		} catch (Exception exception) {
		}
	}

	public static void method528(int i) {
		int size = 8000;
		if (i > size) {
			size = i + 8;
		}
		if (size < 4096) {
			size = 4096;
		}
		animationlist = new Frames[size][0];
	}

	private static void ensureFileSlot(int file) {
		if (animationlist == null) {
			animationlist = new Frames[Math.max(8000, file + 8)][0];
			return;
		}
		if (file < animationlist.length) {
			return;
		}
		Frames[][] grown = new Frames[file + 128][0];
		System.arraycopy(animationlist, 0, grown, 0, animationlist.length);
		animationlist = grown;
	}

	public static void load(int file) {
		try {
			ensureFileSlot(file);
			byte[] frames = getData(0, file);
			byte[] skins = getData(1, file);
			if (frames == null || skins == null) {
				return;
			}
			Stream stream = new Stream(frames);
			Stream stream1 = new Stream(skins);
			Skin class18 = new Skin(stream1, 0);
			int k1 = stream.readUnsignedWord();
			animationlist[file] = new Frames[(int) (k1 * 3.0)];
			decodeFrames(stream, class18, k1, file);
		} catch (Exception exception) {
		}
	}

	public static void load(int file, byte[] fileData) {
		try {
			ensureFileSlot(file);
			Stream stream = new Stream(fileData);
			Skin class18 = new Skin(stream, 0);
			int k1 = stream.readUnsignedWord();
			animationlist[file] = new Frames[(int) (k1 * 3.0)];
			decodeFrames(stream, class18, k1, file);
		} catch (Exception exception) {
		}
	}

	private static void decodeFrames(Stream stream, Skin class18, int k1, int file) {
		int[] ai = new int[500];
		int[] ai1 = new int[500];
		int[] ai2 = new int[500];
		int[] ai3 = new int[500];
		for (int l1 = 0; l1 < k1; l1++) {
			int i2 = stream.readUnsignedWord();
			if (i2 >= animationlist[file].length) {
				Frames[] grown = new Frames[i2 + 8];
				System.arraycopy(animationlist[file], 0, grown, 0, animationlist[file].length);
				animationlist[file] = grown;
			}
			Frames class36 = animationlist[file][i2] = new Frames();
			class36.aClass18_637 = class18;
			int j2 = stream.readUnsignedByte();
			int l2 = 0;
			int k2 = -1;
			for (int i3 = 0; i3 < j2; i3++) {
				int j3 = stream.readUnsignedByte();
				if (j3 > 0) {
					if (class18.anIntArray342[i3] != 0) {
						for (int l3 = i3 - 1; l3 > k2; l3--) {
							if (class18.anIntArray342[l3] != 0) {
								continue;
							}
							ai[l2] = l3;
							ai1[l2] = 0;
							ai2[l2] = 0;
							ai3[l2] = 0;
							l2++;
							break;
						}
					}
					ai[l2] = i3;
					short c = 0;
					if (class18.anIntArray342[i3] == 3) {
						c = (short) 128;
					}
					if ((j3 & 1) != 0) {
						ai1[l2] = (short) stream.readShort2();
					} else {
						ai1[l2] = c;
					}
					if ((j3 & 2) != 0) {
						ai2[l2] = stream.readShort2();
					} else {
						ai2[l2] = c;
					}
					if ((j3 & 4) != 0) {
						ai3[l2] = stream.readShort2();
					} else {
						ai3[l2] = c;
					}
					k2 = i3;
					l2++;
				}
			}
			class36.anInt638 = l2;
			class36.anIntArray639 = new int[l2];
			class36.anIntArray640 = new int[l2];
			class36.anIntArray641 = new int[l2];
			class36.anIntArray642 = new int[l2];
			for (int k3 = 0; k3 < l2; k3++) {
				class36.anIntArray639[k3] = ai[k3];
				class36.anIntArray640[k3] = ai1[k3];
				class36.anIntArray641[k3] = ai2[k3];
				class36.anIntArray642[k3] = ai3[k3];
			}
		}
	}

	public static void nullLoader() {
		animationlist = null;
	}

	public static Frames method531(int j) {
		try {
			if (j <= 0 || animationlist == null) {
				return null;
			}
			int file = j >>> 16;
			int frame = j & 0xffff;
			if (file < 0) {
				return null;
			}
			ensureFileSlot(file);
			if (animationlist[file].length == 0) {
				File extra = new File(signlink.findcachedir() + file + ".dat");
				if (extra.exists()) {
					load_647(file);
				}
				if (animationlist[file].length == 0 && getData(0, file) != null
						&& getData(1, file) != null) {
					load(file);
				}
				// Curse frames only — remapped high IDs. Never steal OG frame slots.
				if (animationlist[file].length == 0) {
					CurseData667.loadAnimationFile(file);
				}
				if (animationlist[file].length == 0 && client.onDemandFetcher != null) {
					client.onDemandFetcher.method558(1, file);
					return null;
				}
			}
			if (animationlist[file].length == 0 || frame < 0 || frame >= animationlist[file].length) {
				return null;
			}
			return animationlist[file][frame];
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean method532(int i) {
		return i == -1;
	}

	private Frames() {
	}

	private static Frames animationlist[][];
	public int anInt636;
	public Skin aClass18_637;
	public int anInt638;
	public static byte[][] frameData = null;
	public static byte[][] skinData = null;
	public int anIntArray639[];
	public int anIntArray640[];
	public int anIntArray641[];
	public int anIntArray642[];
}
