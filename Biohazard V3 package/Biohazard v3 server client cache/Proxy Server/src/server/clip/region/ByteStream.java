package server.clip.region;

public class ByteStream {

	private byte[] buffer;
	private int offset;

	public ByteStream(byte[] buffer)
	{
		this.buffer = buffer;
		this.offset = 0;
	}

	public void skip(int length)
	{
		offset += length;
	}

	public void setOffset(int position)
	{
		offset = position;
	}
	
	public void setOffset(long position)
	{
		offset = (int) position;
	}

	public int length()
	{
		return buffer.length;
	}
	
	public int remaining()
	{
		if (buffer == null)
			return 0;
		return buffer.length - offset;
	}

	public byte getByte()
	{
		if (buffer == null || offset >= buffer.length)
			return 0;
		return buffer[offset++];
	}

	public int getUByte()
	{
		if (buffer == null || offset >= buffer.length)
			return 0;
		return buffer[offset++] & 0xff;
	}

	public int getShort()
	{
		int val = (getByte() << 8) + getByte();
		if(val > 32767)
            		val -= 0x10000;
		return val;
	}

	public int getUShort()
	{
        return (getUByte() << 8) + getUByte();
    }
	
	public int getInt()
	{
		return (getUByte() << 24) + (getUByte() << 16) + (getUByte() << 8) + getUByte();
	}
	
	public long getLong()
	{
		return (getUByte() << 56) + (getUByte() << 48) + (getUByte() << 40) + (getUByte() << 32) + (getUByte() << 24) + (getUByte() << 16) + (getUByte() << 8) + getUByte();
	}

	public int getUSmart()
	{
		if (buffer == null || offset >= buffer.length)
			return 0;
        int i = buffer[offset] & 0xff;
        if (i < 128) {
        	return getUByte();
        } else {
        	if (remaining() < 2)
        		return 0;
        	return getUShort() - 32768;
        }
    }

	public String getNString()
    {
		int i = offset;
		while(buffer[offset++] != 0) ;
		return new String(buffer, i, offset - i - 1);
    }

	public byte[] getBytes()
    {
        int i = offset;
        while(buffer[offset++] != 10) ;
        byte abyte0[] = new byte[offset - i - 1];
        System.arraycopy(buffer, i, abyte0, i - i, offset - 1 - i);
        return abyte0;
    }
	
	public byte[] read(int length)
	{
		if (length < 0)
			length = 0;
		if (buffer == null || offset >= buffer.length)
			return new byte[0];
		if (offset + length > buffer.length)
			length = buffer.length - offset;
		byte[] b = new byte[length];
		for (int i = 0; i < length; i++)
			b[i] = buffer[offset++];
		return b;
	}

}