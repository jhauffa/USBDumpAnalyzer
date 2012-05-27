package uri.usb;

import java.util.ArrayList;
import java.util.List;

import uri.util.ByteBuffer;
import uri.util.ByteUtil;


public class TrSq113HighLevel implements Transducer {

	public List<Event> transduce(List<Event> in)
	{
		ArrayList<Event> out = new ArrayList<Event>(in.size());

		int idx = 0;
		while (idx < in.size())
		{
			Event eventIn = in.get(idx);

			int eventsConsumed = detectButtonPoll(in, idx, out);
			if (eventsConsumed == 0)
				eventsConsumed = detectDmaWrite(in, idx, out);
			if (eventsConsumed == 0)
				eventsConsumed = detectDmaRead(in, idx, out);
			if (eventsConsumed == 0)
			{
				out.add(eventIn);
				eventsConsumed++;
			}
			idx += eventsConsumed;
		}

		return out;
	}

	private static final int[] gpioSeq = {0x1c, 0x02, 0x03};

	private int detectButtonPoll(List<Event> in, int idx, List<Event> out)
	{
		if ((idx + (gpioSeq.length - 2)) >= in.size())
			return 0;

		byte[] values = new byte[gpioSeq.length];
		for (int i = 0; i < gpioSeq.length; i++)
		{
			Event eventIn = in.get(idx + i);
			if (!eventIn.matchStringAttribute("SQ113_sequence", "status_read"))
				return 0;
			if (!eventIn.matchIntegerAttribute("regIdx", gpioSeq[i]))
				return 0;
			Event.Value v = eventIn.getAttribute("value");
			if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
				return 0;
			values[i] = v.getInteger().byteValue();
		}

		Event eventOut = new Event(4);
		eventOut.setAttribute("SQ113_operation",
				new Event.Value("poll_buttons"));
		for (int i = 0; i < gpioSeq.length; i++)
			eventOut.setAttribute("value" + i, new Event.Value(values[i]));
		out.add(eventOut);
		return 3;
	}

	private int detectDmaWrite(List<Event> in, int idx, List<Event> out)
	{
		if ((idx + 5) >= in.size())
			return 0;

		Event eventIn = in.get(idx);
		if (!eventIn.matchStringAttribute("SQ113_command",
				"write_register_multi"))
			return 0;
		byte[] addrBytes = new byte[6];
		for (int i = 0; i < 6; i++)
		{
			Event.Value v = eventIn.getAttribute("regIdx" + i);
			if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
				return 0;
			if (v.getInteger() != (0xa0 + i))
				return 0;
			v = eventIn.getAttribute("value" + i);
			if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
				return 0;
			addrBytes[i] = v.getInteger().byteValue();
		}

		eventIn = in.get(idx + 1);
		if (!eventIn.matchStringAttribute("SQ113_command", "unknown_fifo_op"))
			return 0;
		if (!eventIn.matchIntegerAttribute("length", 0x0c))
			return 0;

		eventIn = in.get(idx + 2);
		if (!eventIn.matchStringAttribute("SQ113_sequence", "dma_write"))
			return 0;
		long length;
		Event.Value v = eventIn.getAttribute("length");
		if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
			return 0;
		length = v.getInteger();
		ByteBuffer data;
		v = eventIn.getAttribute("data");
		if ((v == null) || (v.getType() != Event.Value.Type.RAW))
			return 0;
		data = v.getRaw();

		eventIn = in.get(idx + 3);
		if (!eventIn.matchStringAttribute("SQ113_command", "unknown_fifo_op"))
			return 0;
		if (!eventIn.matchIntegerAttribute("length", length))
			return 0;

		eventIn = in.get(idx + 4);
		if (!eventIn.matchStringAttribute("SQ113_command", "unknown_fifo_op"))
			return 0;
		if (!eventIn.matchIntegerAttribute("length", 0x40))
			return 0;

		eventIn = in.get(idx + 5);
		if (!eventIn.matchStringAttribute("SQ113_sequence", "dma_read"))
			return 0;
		if (!eventIn.matchIntegerAttribute("length", 2))
			return 0;

		Event eventOut = new Event(4);
		eventOut.setAttribute("SQ113_operation", new Event.Value("dma_write"));
		eventOut.setAttribute("start_addr", new Event.Value(
				ByteUtil.byteArrayToLong(addrBytes, 0, 3)));
		eventOut.setAttribute("end_addr", new Event.Value(
				ByteUtil.byteArrayToLong(addrBytes, 3, 3)));
		eventOut.setAttribute("length", new Event.Value(length));
		eventOut.setAttribute("data", new Event.Value(data));
		out.add(eventOut);
		return 6;
	}

	private int detectDmaRead(List<Event> in, int idx, List<Event> out)
	{
		if ((idx + 2) >= in.size())
			return 0;

		Event eventIn = in.get(idx);
		if (!eventIn.matchStringAttribute("SQ113_command",
				"write_register_multi"))
		return 0;
		byte[] addrBytes = new byte[6];
		for (int i = 0; i < 6; i++)
		{
			Event.Value v = eventIn.getAttribute("regIdx" + i);
			if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
				return 0;
			if (v.getInteger() != (0xa0 + i))
				return 0;
			v = eventIn.getAttribute("value" + i);
			if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
				return 0;
			addrBytes[i] = v.getInteger().byteValue();
		}

		eventIn = in.get(idx + 1);
		if (!eventIn.matchStringAttribute("SQ113_command", "clear_fifo"))
			return 0;

		eventIn = in.get(idx + 2);
		if (!eventIn.matchStringAttribute("SQ113_sequence", "dma_read"))
			return 0;
		long length;
		Event.Value v = eventIn.getAttribute("length");
		if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
			return 0;
		length = v.getInteger();
		ByteBuffer data;
		v = eventIn.getAttribute("data");
		if ((v == null) || (v.getType() != Event.Value.Type.RAW))
			return 0;
		data = v.getRaw();

		Event eventOut = new Event(4);
		eventOut.setAttribute("SQ113_operation", new Event.Value("dma_read"));
		eventOut.setAttribute("start_addr", new Event.Value(
				ByteUtil.byteArrayToLong(addrBytes, 0, 3)));
		eventOut.setAttribute("end_addr", new Event.Value(
				ByteUtil.byteArrayToLong(addrBytes, 3, 3)));
		eventOut.setAttribute("length", new Event.Value(length));
		eventOut.setAttribute("data", new Event.Value(data));
		out.add(eventOut);
		return 3;
	}

}
