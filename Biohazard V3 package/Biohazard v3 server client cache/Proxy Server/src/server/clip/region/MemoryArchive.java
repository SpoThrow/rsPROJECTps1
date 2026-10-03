package server.clip.region;


public class MemoryArchive {

	private ByteStream cache;
	private ByteStream index;
	private static final int INDEX_DATA_CHUNK_SIZE = 12;

	public MemoryArchive(ByteStream cache, ByteStream index)
	{
		this.cache = cache;
		this.index = index;
	}

	public byte[] get(int dataIndex)
	{
		try {
			if (index == null || cache == null || dataIndex < 0)
				return null;
			int pos = dataIndex * INDEX_DATA_CHUNK_SIZE;
			if (pos < 0 || pos + INDEX_DATA_CHUNK_SIZE > index.length())
				return null;
			index.setOffset(pos);
			long fileOffset = index.getLong();
			int fileSize = index.getInt();
			if (fileSize <= 0 || fileOffset < 0 || fileOffset + fileSize > cache.length())
				return null;
			cache.setOffset(fileOffset);
			return cache.read(fileSize);
		} catch(Exception e) {
			return null;
		}
	}

	public int contentSize()
	{
		return index.length() / 12;
	}

}