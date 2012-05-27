package uri.usb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DumpAnalyzer {

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("not enough arguments");
			return;
		}

		List<Event> events = new ArrayList<Event>();

		File dumpFile = new File(args[0]);
		try
		{
			UsbSnoopReader.read(dumpFile, events);
		}
		catch (IOException ex)
		{
			System.err.printf("error: %s", ex.getMessage());
			ex.printStackTrace();
			return;
		}
		int origSize = events.size();

		Transducer[] transducers = new Transducer[5];
		transducers[0] = new TrSimplifyUsb();
		transducers[1] = new TrSq113Commands();
		transducers[2] = new TrSq113CommandSequences();
		transducers[3] = new TrSq113HighLevel();
		transducers[4] = new TrRepetition();

		for (int i = 0; i < transducers.length; i++)
		{
			System.err.printf("applying transducer %d...\n", i + 1);
			events = transducers[i].transduce(events);
		}

		int numUnexplained = 0;
		for (Event e : events)
			if (e.getAttribute("level").getInteger() < 2)
				numUnexplained++;
		System.err.printf("unexplained: %d/%d (%.2f%%)\n",
				numUnexplained, events.size(),
				((float) numUnexplained / events.size()) * 100.0f);
		System.err.printf("size: %d -> %d (%.2f%%)\n", origSize, events.size(),
				((float) events.size() / origSize) * 100.0f);

		EventPrinter prn = new EventPrinter(System.out);
		prn.print(events);
	}

}
