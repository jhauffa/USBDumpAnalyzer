package uri.usb;

import java.util.List;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import uri.util.ByteBuffer;


public class UsbSnoopReader {

	public static void read(File dump, List<Event> events) throws IOException
	{
		Event curEvent = null;
		boolean inResponse = false;
		String prevKey = null;

		BufferedReader reader = new BufferedReader(new FileReader(dump));
		String line;
		while ((line = reader.readLine()) != null)
		{
			int len = line.length();
			if (len == 0)
				continue;

			Event.Value v;
			String curKey = null;

			switch (line.charAt(0))
			{
			case '[':
				if (line.indexOf(">>>") > 0)
				{
					if (curEvent != null)
						events.add(curEvent);
					curEvent = new Event(0);
					inResponse = false;
				}
				else if (line.indexOf("<<<") > 0)
					inResponse = true;
				prevKey = null;
				break;
			case '-':
				v = new Event.Value(line.substring(3, len - 1));
				curKey = generateKey("type", inResponse);
				if (curEvent != null)
					curEvent.setAttribute(curKey, v);
				prevKey = curKey;
				break;
			case ' ':
				if (line.startsWith("    "))
				{
					ByteBuffer curData = parseByteSequence(line);
					curKey = prevKey + "_data";
					v = curEvent.getAttribute(curKey);
					if (v != null)
					{
						ByteBuffer buf = v.getRaw();
						buf.append(curData);
					}
					else
						v = new Event.Value(curData);
					curEvent.setAttribute(curKey, v);
				}
				else
				{
					String[] parts = line.split("=");
					curKey = generateKey(parts[0].trim(), inResponse);

					if (parts.length > 1)
					{
						if (parts[1].indexOf('[') >= 0)
							v = new Event.Value(parts[1].trim());
						else
						{
							Long l = parseHexValue(parts[1]);
							if (l != null)
								v = new Event.Value(l);
							else
								v = new Event.Value(parts[1].trim());
						}
					}
					else
						v = new Event.Value("");

					curEvent.setAttribute(curKey, v);
					prevKey = curKey;
				}
				break;
			}
		}

		if ((curEvent != null) && (curEvent.size() > 1))
			events.add(curEvent);
	}

	private static String generateKey(String name, boolean inResponse)
	{
		String key;
		if (inResponse)
			key = "res_";
		else
			key = "req_";
		key += name;
		return key;
	}

	private static Long parseHexValue(String part)
	{
		Long v = null;
		try
		{
			String hexPart;
			if (part.startsWith(" 0x"))
				hexPart = part.substring(3, 11);
			else
				hexPart = part.substring(1, 9);
			v = Long.parseLong(hexPart, 16);
		}
		catch (Exception ex)
		{
		}
		return v;
	}

	private static ByteBuffer parseByteSequence(String line)
	{
		ByteBuffer buf = new ByteBuffer(16);
		try
		{
			String seq = line.substring(line.indexOf(':') + 2);
			String[] parts = seq.split("\\s+");
			for (String p : parts)
				buf.append((byte) Integer.parseInt(p, 16));
		}
		catch (Exception ex)
		{
			System.err.printf("malformed byte sequence: %s\n", line);
		}
		return buf;
	}

}
