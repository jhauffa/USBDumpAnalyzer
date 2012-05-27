package uri.usb;

import java.util.ArrayList;
import java.util.List;

import uri.util.ByteBuffer;
import uri.util.ByteUtil;


public class TrSq113CommandSequences implements Transducer {

	public List<Event> transduce(List<Event> in)
	{
		ArrayList<Event> out = new ArrayList<Event>(in.size());

		int idx = 0;
		while (idx < in.size())
		{
			Event eventIn = in.get(idx);

			int eventsConsumed = detectStatusRead(in, idx, out);
			if (eventsConsumed == 0)
				eventsConsumed = detectDmaReadWrite(in, idx, out);
			if (eventsConsumed == 0)
				eventsConsumed = detectSdramInit(in, idx, out);
			if (eventsConsumed == 0)
				eventsConsumed = detectButtonGpioInit(in, idx, out);
			if (eventsConsumed == 0)
			{
				out.add(eventIn);
				eventsConsumed++;
			}
			idx += eventsConsumed;
		}

		return out;
	}

	private int detectStatusRead(List<Event> in, int idx, List<Event> out)
	{
		if ((idx + 2) >= in.size())
			return 0;

		Event eventIn = in.get(idx);
		if (!eventIn.matchStringAttribute("SQ113_command", "write_register"))
			return 0;
		if (!eventIn.matchIntegerAttribute("regIdx", 0x8b))
			return 0;
		Event.Value v = eventIn.getAttribute("value");
		long regIdx = v.getInteger();

		eventIn = in.get(idx + 1);
		if (!eventIn.matchStringAttribute("usb_command",
				"get_device_descriptor"))
			return 0;

		eventIn = in.get(idx + 2);
		if (!eventIn.matchStringAttribute("SQ113_command", "receive_data"))
			return 0;
		v = eventIn.getAttribute("value");
		long gpioValue = v.getInteger();

		Event eventOut = new Event(3);
		eventOut.setAttribute("SQ113_sequence", new Event.Value("status_read"));
		eventOut.setAttribute("regIdx", new Event.Value(regIdx));
		eventOut.setAttribute("value", new Event.Value(gpioValue));
		out.add(eventOut);
		return 3;
	}

	private int detectDmaReadWrite(List<Event> in, int idx, List<Event> out)
	{
		if ((idx + 2) >= in.size())
			return 0;

		Event eventIn = in.get(idx);
		if (!eventIn.matchStringAttribute("SQ113_command",
				"write_register_multi"))
			return 0;
		for (int i = 0; i < 4; i++)
			if (!eventIn.matchIntegerAttribute("regIdx" + i, 0x7c + i))
				return 0;

		boolean isWrite = true;
		eventIn = in.get(idx + 1);
		if (!TrSq113Commands.matchControlMessage(eventIn, true, 4, 0x02, 4, 4))
			isWrite = false;
		if (!isWrite &&
			!TrSq113Commands.matchControlMessage(eventIn, true, 4, 0x03, 4, 4))
			return 0;
		Event.Value v = eventIn.getAttribute("req_data");
		if ((v == null) || (v.getType() != Event.Value.Type.RAW))
			return 0;
		ByteBuffer rawLength = v.getRaw();
		if (rawLength.size() != 4)
			return 0;
		long length = ByteUtil.byteArrayToLong(rawLength.getBuffer(), 0, 4);

		eventIn = in.get(idx + 2);
		if (!eventIn.matchStringAttribute("req_type",
				"URB_FUNCTION_BULK_OR_INTERRUPT_TRANSFER"))
			return 0;
		if (isWrite)
			v = eventIn.getAttribute("req_data");
		else
			v = eventIn.getAttribute("res_data");
		if ((v == null) || (v.getType() != Event.Value.Type.RAW))
			return 0;
		ByteBuffer data = v.getRaw();

		Event eventOut = new Event(3);
		if (isWrite)
			eventOut.setAttribute("SQ113_sequence",
					new Event.Value("dma_write"));
		else
			eventOut.setAttribute("SQ113_sequence",
					new Event.Value("dma_read"));
		eventOut.setAttribute("length", new Event.Value(length));
		eventOut.setAttribute("data", new Event.Value(data));
		out.add(eventOut);
		return 3;
	}

	private static final int[] sdramInitRegs =
		{0x87, 0x87, 0x87, 0x87, 0x87, 0x79};
	private static final int[] sdramInitValues =
		{0xf1, 0xa5, 0x91, 0x81, 0xf0, 0x40};

	private int detectSdramInit(List<Event> in, int idx, List<Event> out)
	{
		if ((idx + 1) >= in.size())
			return 0;

		Event eventIn = in.get(idx);
		if (!eventIn.matchStringAttribute("SQ113_command", "write_register"))
			return 0;
		if (!eventIn.matchIntegerAttribute("regIdx", 0x86))
			return 0;
		if (!eventIn.matchIntegerAttribute("value", 0))
			return 0;

		eventIn = in.get(idx + 1);
		if (!eventIn.matchStringAttribute("SQ113_command",
				"write_register_multi"))
			return 0;
		for (int i = 0; i < sdramInitRegs.length; i++)
		{
			if (!eventIn.matchIntegerAttribute("regIdx" + i, sdramInitRegs[i]))
				return 0;
			if (!eventIn.matchIntegerAttribute("value" + i, sdramInitValues[i]))
				return 0;
		}

		Event eventOut = new Event(3);
		eventOut.setAttribute("SQ113_sequence", new Event.Value("sdram_init"));
		out.add(eventOut);
		return 2;
	}

	private int detectButtonGpioInit(List<Event> in, int idx, List<Event> out)
	{
		Event eventIn = in.get(idx);
		if (!eventIn.matchStringAttribute("SQ113_command",
				"write_register_multi"))
			return 0;

		byte[] regs = new byte[4];
		byte[] values = new byte[4];
		for (int i = 0; i < 4; i++)
		{
			Event.Value v = eventIn.getAttribute("regIdx" + i);
			if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
				return 0;
			regs[i] = v.getInteger().byteValue();
			v = eventIn.getAttribute("value" + i);
			if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
				return 0;
			values[i] = v.getInteger().byteValue();
		}

		if ((regs[0] != (byte) 0x97) || (regs[1] != (byte) 0x98) ||
			(regs[2] != (byte) 0x95) || (regs[3] != (byte) 0x96))
			return 0;
		if ((values[0] != (byte) 0x01) || (values[1] != (byte) 0x81) ||
			(values[3] != (byte) 0x81))
			return 0;

		Event eventOut = new Event(3);
		eventOut.setAttribute("SQ113_command", new Event.Value("button_init"));
		eventOut.setAttribute("param", new Event.Value(values[2]));
		out.add(eventOut);
		return 1;
	}

}
