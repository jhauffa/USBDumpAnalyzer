package uri.usb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrSimplifyUsb implements Transducer {

	private static final String[] ignoredAttributes = {
		"ConfigurationDescriptor",
		"ConfigurationHandle",
		"Interface[0]: InterfaceHandle",
		"level",
		"RequestTypeReservedBits",
		"SetupPacket",
		"SetupPacket_data",
		"TransferBuffer",
		"TransferBufferMDL",
		"UrbLink"
	};

	public List<Event> transduce(List<Event> in)
	{
		ArrayList<Event> out = new ArrayList<Event>(in.size());

		for (Event eventIn : in)
		{
			long level = eventIn.getAttribute("level").getInteger();
			if (level != 0)
			{
				out.add(eventIn);
				continue;
			}

			Event eventOut = new Event(1);
			for (int i = 0; i < eventIn.size(); i++)
			{
				String name = eventIn.getAttributeName(i);
				Event.Value value = eventIn.getAttribute(i);

				String prefix = "";
				if ((name.length() > 3) && (name.charAt(3) == '_'))
				{
					prefix = name.substring(0, 4);
					name = name.substring(4);
				}

				if (Arrays.binarySearch(ignoredAttributes, name,
						String.CASE_INSENSITIVE_ORDER) < 0)
				{
					if (name.equals("TransferBufferMDL_data"))
					{
						// rename req/res_TransferBufferMDL_data to req/res_data 
						eventOut.setAttribute(prefix + "data", value);
					}
					else if (name.equals("PipeHandle"))
					{
						// extract endpoint ID from PipeHandle
						if (value.getType() == Event.Value.Type.STRING)
						{
							String endpointDesc = value.getString();
							Long endpointId = extractEndpointId(endpointDesc);
							if (endpointId != null)
								eventOut.setAttribute(prefix + "endpoint",
										new Event.Value(endpointId));
						}
					}
					else
						eventOut.setAttribute(prefix + name, value);
				}
			}
			out.add(eventOut);
		}

		return out;
	}

	private Long extractEndpointId(String desc)
	{
		Long id = null;
		int idx = desc.indexOf("endpoint");
		if (idx >= 0)
		{
			try
			{
				String hexPart = desc.substring(idx + 11, idx + 19);
				id = Long.parseLong(hexPart);
			}
			catch (Exception ex)
			{
			}
		}
		return id;
	}

}
