package uri.usb;

import java.util.ArrayList;
import java.util.List;

import uri.util.ByteBuffer;
import uri.util.ByteUtil;


public class TrSq113Commands implements Transducer {

	private static final byte[][] openSequence = {
		{0x64, 0x64, 0x64, 0x64},
		{0x65, 0x65, 0x65, 0x65},
		{0x44, 0x44, 0x44, 0x44},
		{0x45, 0x45, 0x45, 0x45}
	};
	private static final byte[][] closeSequence = {
		{0x64, 0x64, 0x64, 0x64},
		{0x65, 0x65, 0x65, 0x65},
		{0x16, 0x16, 0x16, 0x16},
		{0x17, 0x17, 0x17, 0x17}
	};

	private boolean seenDeviceDescriptor;

	public List<Event> transduce(List<Event> in)
	{
		ArrayList<Event> out = new ArrayList<Event>(in.size());

		int idx = 0;
		while (idx < in.size())
		{
			Event eventIn = in.get(idx);
			long level = eventIn.getAttribute("level").getInteger();
			if (level != 1)
			{
				out.add(eventIn);
				idx++;
				continue;
			}

			int eventsConsumed = skipDeviceDescriptor(in, idx, out);
			if (eventsConsumed == 0)
				eventsConsumed = detectRegisterWrite(in, idx, out);
			if (eventsConsumed == 0)
				eventsConsumed = detectReceiveData(in, idx, out);
			if (eventsConsumed == 0)
				eventsConsumed = detectFifoOp(in, idx, out);
			if (eventsConsumed == 0)
				eventsConsumed = detectAttnSequence(in, idx, out);
			if (eventsConsumed == 0)
			{
				out.add(eventIn);
				eventsConsumed++;
			}
			idx += eventsConsumed;
		}

		return out;
	}

	public static boolean matchControlMessage(Event e, boolean isWrite,
			int request, int value, int index, int length)
	{
		if (!e.matchStringAttribute("req_type", "URB_FUNCTION_VENDOR_DEVICE"))
			return false;

		Event.Value v = e.getAttribute("req_TransferFlags");
		if ((v == null) || (v.getType() != Event.Value.Type.INTEGER))
			return false;
		if (isWrite)
		{
			if ((v.getInteger() & 1) != 0)
				return false;
			if ((length >= 0) &&
				!e.matchIntegerAttribute("req_TransferBufferLength", length))
				return false;
		}
		else
		{
			if ((v.getInteger() & 1) == 0)
				return false;
			if ((length >= 0) &&
				!e.matchIntegerAttribute("res_TransferBufferLength", length))
				return false;
		}

		if (!e.matchIntegerAttribute("req_Request", request))
			return false;
		if (!e.matchIntegerAttribute("req_Value", value))
			return false;
		if (!e.matchIntegerAttribute("req_Index", index))
			return false;

		return true;
	}

	private int detectRegisterWrite(List<Event> in, int idx, List<Event> out)
	{
		Event eventIn = in.get(idx);
		if (!matchControlMessage(eventIn, true, 4, 0xb0, 0, -1))
			return 0;

		Event.Value v = eventIn.getAttribute("req_data");
		if (v == null)
			return 0;
		ByteBuffer data = v.getRaw();
		int n = data.size();
		if ((n % 2) != 0)
			return 0;

		Event eventOut = new Event(2);
		if ((n == 4) &&
			(data.get(0) == data.get(2)) && (data.get(1) == data.get(3)))
		{
			eventOut.setAttribute("SQ113_command",
					new Event.Value("write_register"));
			eventOut.setAttribute("regIdx", new Event.Value(data.get(0)));
			eventOut.setAttribute("value", new Event.Value(data.get(1)));
		}
		else
		{
			eventOut.setAttribute("SQ113_command",
					new Event.Value("write_register_multi"));
			for (int i = 0; i < (n / 2); i++)
			{
				eventOut.setAttribute("regIdx" + i, new Event.Value(
						data.get(i * 2)));
				eventOut.setAttribute("value" + i, new Event.Value(
						data.get((i * 2) + 1)));
			}
		}
		out.add(eventOut);
		return 1;
	}

	private int detectReceiveData(List<Event> in, int idx, List<Event> out)
	{
		Event eventIn = in.get(idx);
		if (!matchControlMessage(eventIn, false, 4, 0x07, 0, 1))
			return 0;

		Event.Value v = eventIn.getAttribute("res_data");
		if (v == null)
			return 0;
		ByteBuffer data = v.getRaw();
		if (data.size() != 1)
			return 0;

		Event eventOut = new Event(2);
		eventOut.setAttribute("SQ113_command", new Event.Value("receive_data"));
		eventOut.setAttribute("value", new Event.Value(data.get(0)));
		out.add(eventOut);
		return 1;
	}

	private int detectFifoOp(List<Event> in, int idx, List<Event> out)
	{
		if ((idx + 1) >= in.size())
			return 0;

		Event eventIn = in.get(idx);
		if (!matchControlMessage(eventIn, true, 4, 0x05, 0, 4))
			return 0;
		Event.Value v = eventIn.getAttribute("req_data");
		if ((v == null) || (v.getType() != Event.Value.Type.RAW))
			return 0;
		ByteBuffer seq = v.getRaw();
		if (seq.size() != 4)
			return 0;

		eventIn = in.get(idx + 1);
		if (!matchControlMessage(eventIn, true, 4, 0xc0, 0, 4))
			return 0;
		v = eventIn.getAttribute("req_data");
		if ((v == null) || (v.getType() != Event.Value.Type.RAW))
			return 0;
		if (!v.getRaw().equals(seq))
			return 0;

		Event eventOut = new Event(2);
		long l = ByteUtil.byteArrayToLong(seq.getBuffer(), 0, 4);
		if (l == 0)
			eventOut.setAttribute("SQ113_command",
					new Event.Value("clear_fifo"));
		else
		{
			eventOut.setAttribute("SQ113_command",
					new Event.Value("unknown_fifo_op"));
			eventOut.setAttribute("length", new Event.Value(l));
		}	
		out.add(eventOut);
		return 2;
	}

	private int detectAttnSequence(List<Event> in, int idx, List<Event> out)
	{
		if ((idx + 3) >= in.size())
			return 0;

		boolean isOpenSequence = true;
		boolean isCloseSequence = true;
		for (int i = 0; i < 4; i++)
		{
			Event eventIn = in.get(idx + i);
			if (!matchControlMessage(eventIn, true, 4, 0x90, 0, 4))
				return 0;
			if (!eventIn.matchRawAttribute("req_data", openSequence[i]))
				isOpenSequence = false;
			if (!eventIn.matchRawAttribute("req_data", closeSequence[i]))
				isCloseSequence = false;

			if (!isOpenSequence && !isCloseSequence)
				return 0;
		}

		Event eventOut = new Event(2);
		if (isOpenSequence)
			eventOut.setAttribute("SQ113_command", new Event.Value("open"));
		else
			eventOut.setAttribute("SQ113_command", new Event.Value("close"));
		out.add(eventOut);
		return 4;
	}

	private int skipDeviceDescriptor(List<Event> in, int idx, List<Event> out)
	{
		Event eventIn = in.get(idx);

		if (!eventIn.matchStringAttribute("req_type",
				"URB_FUNCTION_GET_DESCRIPTOR_FROM_DEVICE"))
			return 0;
		if (!eventIn.matchIntegerAttribute("res_TransferBufferLength", 0x12))
			return 0;
		if (!eventIn.matchIntegerAttribute("req_LanguageId", 0))
			return 0;
		if (!eventIn.matchIntegerAttribute("req_Index", 0))
			return 0;
		if (!eventIn.matchIntegerAttribute("req_DescriptorType", 1))
			return 0;

		Event eventOut = new Event(2);
		eventOut.setAttribute("usb_command",
				new Event.Value("get_device_descriptor"));
		if (!seenDeviceDescriptor)
		{
			eventOut.setAttribute("data", eventIn.getAttribute("res_data"));
			seenDeviceDescriptor = true;
		}
		out.add(eventOut);
		return 1;
	}

}
