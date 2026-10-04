// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import java.io.*;
import java.net.Socket;

final class RSSocket
		implements Runnable
{

	public RSSocket(RSApplet RSApplet_, Socket socket1)
		throws IOException
	{
		closed = false;
		isWriter = false;
		hasIOError = false;
		rsApplet = RSApplet_;
		socket = socket1;
		socket.setSoTimeout(30000);
		socket.setTcpNoDelay(true);
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
	}

	public void close()
	{
		closed = true;
		try
		{
			if(inputStream != null)
				inputStream.close();
			if(outputStream != null)
				outputStream.close();
			if(socket != null)
				socket.close();
		}
		catch(IOException _ex)
		{
			System.out.println("Error closing stream");
		}
		isWriter = false;
		synchronized(this)
		{
			notify();
		}
		buffer = null;
	}

	public int read()
		throws IOException
	{
		if(closed)
			return 0;
		else
		{
			int value = inputStream.read();
			// Phase 0.3 packet tap. Guarded by enabled() so normal play is unaffected.
			if(value >= 0 && PacketTap.enabled())
				PacketTap.incoming(new byte[] { (byte) value }, 0, 1);
			return value;
		}
	}

	public int available()
		throws IOException
	{
		if(closed)
			return 0;
		else
			return inputStream.available();
	}

	public void flushInputStream(byte abyte0[], int j)
		throws IOException
	{
		int i = 0;//was parameter
		if(closed)
			return;
		int requested = j;   // Phase 0.3: remember the length, the loop consumes j
		int k;
		for(; j > 0; j -= k)
		{
			k = inputStream.read(abyte0, i, j);
			if(k <= 0)
				throw new IOException("EOF");
			i += k;
		}
		if(PacketTap.enabled())
			PacketTap.incoming(abyte0, 0, requested);

	}

	public void queueBytes(int i, byte abyte0[])
		throws IOException
	{
		if(closed)
			return;
		if(hasIOError)
		{
			hasIOError = false;
			throw new IOException("Error in writer thread");
		}
		// Phase 0.3: tap the whole frame here rather than in the writer thread, because
		// outputStream.write() is called with whatever the ring buffer happens to hold
		// and would split/coalesce frames differently on every run.
		if(PacketTap.enabled())
			PacketTap.outgoing(abyte0, 0, i);
		if(buffer == null)
			buffer = new byte[5000];
		synchronized(this)
		{
			for(int l = 0; l < i; l++)
			{
				buffer[buffIndex] = abyte0[l];
				buffIndex = (buffIndex + 1) % 5000;
				if(buffIndex == (writeIndex + 4900) % 5000)
					throw new IOException("buffer overflow");
			}

			if(!isWriter)
			{
				isWriter = true;
				rsApplet.startRunnable(this, 3);
			}
			notify();
		}
	}

	public void run()
	{
		while(isWriter)
		{
			int i;
			int j;
			synchronized(this)
			{
				if(buffIndex == writeIndex)
					try
					{
						wait();
					}
					catch(InterruptedException _ex) { }
				if(!isWriter)
					return;
				j = writeIndex;
				if(buffIndex >= writeIndex)
					i = buffIndex - writeIndex;
				else
					i = 5000 - writeIndex;
			}
			if(i > 0)
			{
				try
				{
					outputStream.write(buffer, j, i);
				}
				catch(IOException _ex)
				{
					hasIOError = true;
				}
				writeIndex = (writeIndex + i) % 5000;
				try
				{
					if(buffIndex == writeIndex)
						outputStream.flush();
				}
				catch(IOException _ex)
				{
					hasIOError = true;
				}
			}
		}
	}

	public void printDebug()
	{
		System.out.println("dummy:" + closed);
		System.out.println("tcycl:" + writeIndex);
		System.out.println("tnum:" + buffIndex);
		System.out.println("writer:" + isWriter);
		System.out.println("ioerror:" + hasIOError);
		try
		{
			System.out.println("available:" + available());
		}
		catch(IOException _ex)
		{
		}
	}

	private InputStream inputStream;
	private OutputStream outputStream;
	private final Socket socket;
	private boolean closed;
	private final RSApplet rsApplet;
	private byte[] buffer;
	private int writeIndex;
	private int buffIndex;
	private boolean isWriter;
	private boolean hasIOError;
}
