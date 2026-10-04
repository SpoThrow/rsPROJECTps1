// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public final class VarBit {

	public static void unpackConfig(StreamLoader streamLoader)
	{
		Stream stream = new Stream(streamLoader.getDataForName("varbit.dat"));
		int cacheSize = stream.readUnsignedWord();
		if(cache == null)
			cache = new VarBit[cacheSize];
		for(int j = 0; j < cacheSize; j++)
		{
			if(cache[j] == null)
				cache[j] = new VarBit();
			cache[j].readValues(stream);
			// Phase 2.2: anInt648 is read out of varbit.dat while Varp.cache is sized from
			// varp.dat's entry count, so a corrupt or mismatched cache can point past the
			// end of it (or reach here with Varp still unloaded). Skip instead of crashing
			// during cache load; for a well-formed cache the condition never triggers.
			if(cache[j].aBoolean651 && Varp.cache != null && cache[j].anInt648 >= 0
					&& cache[j].anInt648 < Varp.cache.length
					&& Varp.cache[cache[j].anInt648] != null)
				Varp.cache[cache[j].anInt648].aBoolean713 = true;
		}

		if(stream.currentOffset != stream.buffer.length)
			System.out.println("varbit load mismatch");
	}

	private void readValues(Stream stream)
	{
		do
		{
			int j = stream.readUnsignedByte();
			if(j == 0)
				return;
			if(j == 1)
			{
				anInt648 = stream.readUnsignedWord();
				anInt649 = stream.readUnsignedByte();
				anInt650 = stream.readUnsignedByte();
			} else
			if(j == 10)
				stream.readString();
			else
			if(j == 2)
				aBoolean651 = true;
			else
			if(j == 3)
				stream.readDWord();
			else
			if(j == 4)
				stream.readDWord();
			else
				System.out.println("Error unrecognised config code: " + j);
		} while(true);
	}

	private VarBit()
	{
		aBoolean651 = false;
	}

	public static VarBit cache[];
	public int anInt648;
	public int anInt649;
	public int anInt650;
	private boolean aBoolean651;
}
