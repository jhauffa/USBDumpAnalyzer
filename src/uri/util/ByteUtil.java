package uri.util;

public class ByteUtil {

	public static long byteArrayToLong(byte[] a, int idx, int n)
	{
		long l = 0;
		for (int i = 0; i < n; i++)
			l |= (a[idx + i] & 0xff) << (i * 8);
		return l;
	}

}
