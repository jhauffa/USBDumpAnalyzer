package uri.usb;

import java.util.List;

public interface Transducer {

	public List<Event> transduce(List<Event> in);

}
