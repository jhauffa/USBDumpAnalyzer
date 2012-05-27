package uri.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ConverterApp {

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("not enough arguments");
			return;
		}

		ByteBuffer buf = new ByteBuffer();

		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(args[0]));
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] parts = line.split("\\s");
				for (String part : parts)
					buf.append((byte) Integer.parseInt(part, 16));
			}
		}
		catch (IOException ex)
		{
			System.err.println(ex.getMessage());
			ex.printStackTrace();
			return;
		}

		for (int i = 0; i < (buf.size() / 2); i++)
		{
			int value = buf.get(i * 2) & 0xff;
			value |= (buf.get((i * 2) + 1) & 0xff) << 8;
			System.out.println(value);
		}
	}

}
