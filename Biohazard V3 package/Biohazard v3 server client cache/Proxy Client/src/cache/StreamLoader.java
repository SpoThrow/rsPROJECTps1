package cache;

import java.io.*;
import java.util.zip.*;import net.Stream;



public class StreamLoader {

	public StreamLoader(byte[] b, String s) {
		try {
			// if (s.contains("2d"))
			// b = getBytesFromFile(new File("./data.dat"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		a(b);
	}

	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);
		long length = file.length();
		byte[] bytes = new byte[(int) length];

		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		if (offset < bytes.length) {
			is.close();
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		is.close();
		return bytes;
	}

	public void a(byte abyte0[]) {
		if (abyte0 == null || abyte0.length < 6) {
			aByteArray726 = new byte[0];
			return;
		}
		Stream stream = new Stream(abyte0);
		int i = stream.read3Bytes();
		int j = stream.read3Bytes();
		if (j == 0) {
			byte[] abyte1 = new byte[i];
			byte[] abyte3 = new byte[i];
			// ⚠️ This copy must never exceed the declared uncompressed size. `abyte1` is
			// sized to `i`, but the source is the raw file, so an over-long payload used to
			// overflow here - and this line sits OUTSIDE the try below, so it escaped as a
			// raw ArrayIndexOutOfBoundsException rather than degrading to "missing".
			int copy = Math.min(i, abyte0.length - 6);
			if (copy > 0) {
				System.arraycopy(abyte0, 6, abyte1, 0, copy);
			}
			try {
				DataInputStream datainputstream = new DataInputStream(
						new GZIPInputStream(new ByteArrayInputStream(abyte1)));
				datainputstream.readFully(abyte3, 0, abyte3.length);
			} catch (Exception exception) {
				exception.printStackTrace();
			}
			aByteArray726 = abyte3;
			stream = new Stream(aByteArray726);
			aBoolean732 = true;
		} else if (j != i) {
			byte abyte1[] = new byte[i];
			Bzip2Decompressor.method225(abyte1, i, abyte0, j, 6);
			aByteArray726 = abyte1;
			stream = new Stream(aByteArray726);
			aBoolean732 = true;
		} else {
			aByteArray726 = abyte0;
			aBoolean732 = false;
		}
		// The index table is `dataSize` fixed 10-byte entries read unconditionally by
		// readDWord/read3Bytes, neither of which is guarded - so a truncated table would
		// throw rather than degrade. Refuse the table if it does not fit. (This also catches
		// a buffer too short to hold a count at all: readUnsignedWord swallows its own
		// out-of-range read and fabricates 1795, which then fails this same fit test.)
		dataSize = stream.readUnsignedWord();
		if (stream.currentOffset + (long) dataSize * 10 > aByteArray726.length) {
			dataSize = 0;
			return;
		}
		anIntArray728 = new int[dataSize];
		anIntArray729 = new int[dataSize];
		anIntArray730 = new int[dataSize];
		anIntArray731 = new int[dataSize];
		int k = stream.currentOffset + dataSize * 10;
		for (int l = 0; l < dataSize; l++) {
			anIntArray728[l] = stream.readDWord();
			anIntArray729[l] = stream.read3Bytes();
			anIntArray730[l] = stream.read3Bytes();
			anIntArray731[l] = k;
			k += anIntArray730[l];
		}
	}

	public byte[] getDataForName(String s) {
		byte abyte0[] = null; // was a parameter
		int i = 0;
		s = s.toUpperCase();
		for (int j = 0; j < s.length(); j++)
			i = (i * 61 + s.charAt(j)) - 32;

		for (int k = 0; k < dataSize; k++)
			if (anIntArray728[k] == i) {
				int length = anIntArray729[k];
				int offset = anIntArray731[k];
				// ⚠️ The index table is trusted by the format, so a corrupt or hostile
				// archive whose index disagrees with its data used to read past the buffer:
				// the arraycopy branch below threw, and the LZ branch (Class13.method225)
				// ran off the end mid-decompression. In the LZ case the extent that must fit
				// is the COMPRESSED length; when the entry is already decompressed it is the
				// uncompressed one - hence `span`. Degrade to "missing" rather than guess.
				int span = aBoolean732 ? length : anIntArray730[k];
				if (aByteArray726 == null || length < 0 || offset < 0
						|| offset > aByteArray726.length - span)
					return null;
				if (abyte0 == null)
					abyte0 = new byte[length];
				if (!aBoolean732) {
					Bzip2Decompressor.method225(abyte0, length, aByteArray726,
							anIntArray730[k], offset);
				} else {
					System.arraycopy(aByteArray726, offset, abyte0,
							0, length);

				}
				return abyte0;
			}

		return null;
	}

	public byte[] aByteArray726;
	public int dataSize;
	public int[] anIntArray728;
	public int[] anIntArray729;
	public int[] anIntArray730;
	public int[] anIntArray731;
	public boolean aBoolean732;
}
