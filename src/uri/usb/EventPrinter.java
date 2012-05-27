package uri.usb;

import java.io.PrintStream;
import java.util.List;

public class EventPrinter {

	private PrintStream out;

	public EventPrinter(PrintStream out)
	{
		this.out = out;
	}

	public void print(Event e)
	{
		out.println();
		for (int i = 0; i < e.size(); i++)
		{
			String name = e.getAttributeName(i);
			out.printf("\t%s = ", name);
			Event.Value value = e.getAttribute(i);
			switch (value.getType())
			{
			case INTEGER:
				long v = value.getInteger();
				if (v == 0)
					out.println("0");
				else if (v < 256)
					out.printf("0x%02x\n", v);
				else
					out.printf("0x%08x\n", v);
				break;
			case STRING:
				out.println(value.getString());
				break;
			case RAW:
				out.printf("\n%s\n", value.getRaw().formatAsHex());
				break;
			default:
				out.println("[unknown format]");
				break;
			}
		}
	}

	public void print(List<Event> l)
	{
		int idx = 1;
		for (Event e : l)
		{
			out.printf("Event %d:", idx++);
			print(e);
		}
	}

}
