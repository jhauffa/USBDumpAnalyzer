package uri.util;

public class ByteBuffer {

	private static final int defaultBufferSize = 32;

	private byte[] buf;
	private int pos;
	private int limit;

	public ByteBuffer()
	{
		this(defaultBufferSize);
	}

	public ByteBuffer(int bufferSize)
	{
		buf = new byte[bufferSize];
		limit = bufferSize;
		pos = 0;
	}

	public ByteBuffer(byte[] data)
	{
		buf = data;
		limit = data.length;
		pos = limit;
	}

	public void append(byte b)
	{
		if (pos >= limit)
			grow();
		buf[pos] = b;
		pos++;
	}

	public void append(byte[] b)
	{
		append(b, b.length);
	}

	public void append(byte[] b, int n)
	{
		while ((pos + n) > limit)
			grow();
		System.arraycopy(b, 0, buf, pos, n);
		pos += n;		
	}

	public void append(ByteBuffer other)
	{
		append(other.getBuffer(), other.size());
	}

	private void grow()
	{
		limit *= 2;
		byte[] newBuf;
		try
		{
			newBuf = new byte[limit];
		}
		catch (OutOfMemoryError ex)
		{
			System.err.printf("not enough memory to grow buffer to %d bytes\n",
					limit);
			throw ex;
		}
		System.arraycopy(buf, 0, newBuf, 0, buf.length);
		buf = newBuf;
	}

	public int size()
	{
		return pos;
	}

	public byte[] getBuffer()
	{
		return buf;
	}

	public void clear()
	{
		pos = 0;
	}

	public byte get(int idx)
	{
		if (idx >= pos)
			throw new IndexOutOfBoundsException();
		return buf[idx];
	}

	public String formatAsAscii()
	{
		StringBuffer str = new StringBuffer();
		for (int i = 0; i < pos; i++)
		{
			if ((buf[i] >= 0) && (buf[i] < 128))
				str.append((char) buf[i]);
			else
				str.append('?');
		}
		return str.toString();
	}

	public String formatAsHex()
	{
		StringBuffer str = new StringBuffer();
		int i = 0;
		while (i < pos)
		{
			// convert from unsigned byte to signed integer
			int v = buf[i];
			if (v < 0)
				v = 256 + v;

			if (v < 16)
				str.append('0');
			str.append(Integer.toHexString(v));

			if ((++i % 16) == 0)
				str.append('\n');
			else
				str.append(' ');
		}
		return str.toString();
	}

	public boolean equals(ByteBuffer other)
	{
		if (pos != other.pos)
			return false;
		for (int i = 0; i < pos; i++)
			if (buf[i] != other.buf[i])
				return false;
		return true;
	}

	public boolean equals(byte[] other)
	{
		if (pos != other.length)
			return false;
		for (int i = 0; i < pos; i++)
			if (buf[i] != other[i])
				return false;
		return true;
	}

}
