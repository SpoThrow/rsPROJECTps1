import java.io.DataInputStream;
import java.io.FileInputStream;

import sign.signlink;
/**
 *Loads models from a data file
 *
 *@author Ben
 */

public class ModelDecompressor {
	/** Custom model cache loading, useless */
	public static void loadModelDataFile() {
		try {
			DataInputStream indexFile = new DataInputStream(new FileInputStream(signlink.findcachedir()+"MAP_CACHE.idx"));
			DataInputStream dataFile = new DataInputStream(new FileInputStream(signlink.findcachedir()+"MAP_CACHE.dat"));
			int length = indexFile.readInt();
			for(int i = 0; i < length; i++) {
				int id = indexFile.readInt();
				int invlength = indexFile.readInt();
				byte[] data = new byte[invlength];
				dataFile.readFully(data);
				Model.method460(data, id);
			}
			indexFile.close();
			dataFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void load474object() {
		try {
			DataInputStream indexFile = new DataInputStream(new FileInputStream(signlink.findcachedir()+"MAP_CACHE.idx2"));
			//DataInputStream dataFile = new DataInputStream(new FileInputStream(signlink.findcachedir()+"null_cache_objects.dat"));
			int length = indexFile.readInt();
			for(int i = 0; i < length; i++) {
				int id = indexFile.readInt();
				int invlength = indexFile.readInt();
				byte[] data = new byte[invlength];
				//dataFile.readFully(data);
				Model.method460(data, id);
			}
			indexFile.close();
			//dataFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}