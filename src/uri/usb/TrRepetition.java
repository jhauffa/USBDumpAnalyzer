package uri.usb;

import java.util.ArrayList;
import java.util.List;

public class TrRepetition implements Transducer {

	public List<Event> transduce(List<Event> in)
	{
		ArrayList<Event> out = new ArrayList<Event>(in.size());

		int idx = 0;
		while (idx < in.size())
		{
			Event eventIn = in.get(idx);
			long level = eventIn.getAttribute("level").getInteger();

			int seqIdx = idx + 1;
			while ((seqIdx < in.size()) && eventIn.equals(in.get(seqIdx)))
				seqIdx++;
			seqIdx--;

			out.add(eventIn);
			if (seqIdx > idx)
			{
				Event eventOut = new Event(level + 1);
				eventOut.setAttribute("num_repetitions",
						new Event.Value(seqIdx - idx));
				out.add(eventOut);
				idx = seqIdx + 1;
			}
			else
				idx++;
		}

		return out;
	}

}
