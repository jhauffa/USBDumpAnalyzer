package uri.usb;

import java.util.HashMap;
import java.util.ArrayList;

import uri.util.ByteBuffer;


public class Event {

	private static class Attribute
	{
		public int key;
		public Value value;

		public Attribute(int key, Value value)
		{
			this.key = key;
			this.value = value;
		}
	}

	public static class Value
	{
		enum Type { STRING, INTEGER, RAW };

		private Type type;
		private Object data;

		public Value(String data)
		{
			type = Type.STRING;
			this.data = data;
		}

		public Value(long data)
		{
			type = Type.INTEGER;
			this.data = data;
		}

		public Value(byte data)
		{
			type = Type.INTEGER;
			Long value = (long) data;
			if (value < 0)
				value = 256 + value;
			this.data = value;
		}

		public Value(byte[] data)
		{
			type = Type.RAW;
			this.data = new ByteBuffer(data);
		}

		public Value(ByteBuffer data)
		{
			type = Type.RAW;
			this.data = data;
		}

		public Type getType()
		{
			return type;
		}

		public String getString()
		{
			return (String) data;
		}

		public Long getInteger()
		{
			return (Long) data;
		}

		public ByteBuffer getRaw()
		{
			return (ByteBuffer) data;
		}

		public boolean equals(Value other)
		{
			if (type != other.type)
				return false;
			switch (type)
			{
			case STRING:
				return ((String) data).equals((String) other.data);
			case INTEGER:
				return ((Long) data).equals((Long) other.data);
			case RAW:
				return ((ByteBuffer) data).equals((ByteBuffer) other.data);
			}
			return false;
		}
	}

	private static HashMap<String, Integer> attributeNamesMap;
	private static ArrayList<String> attributeNames;
	private ArrayList<Attribute> attributes;

	public Event(long level)
	{
		ensureAttributeNameMap();
		attributes = new ArrayList<Attribute>();
		setAttribute("level", new Value(level));
	}

	private synchronized void ensureAttributeNameMap()
	{
		if (attributeNamesMap == null)
		{
			attributeNamesMap = new HashMap<String, Integer>();
			attributeNames = new ArrayList<String>();
		}
	}

	public synchronized Value getAttribute(String name)
	{
		Integer key = attributeNamesMap.get(name);
		if (key == null)
			return null;

		for (Attribute a : attributes)
			if (a.key == key)
				return a.value;
		return null;
	}

	public synchronized void setAttribute(String name, Value v)
	{
		Integer key = attributeNamesMap.get(name);
		if (key == null)
		{
			key = attributeNames.size();
			attributeNames.add(name);
			attributeNamesMap.put(name, key);
		}
		else
		{
			for (Attribute a : attributes)
				if (a.key == key)
				{
					a.value = v;
					return;
				}
		}
		attributes.add(new Attribute(key, v));
	}

	public synchronized Value getAttribute(int index)
	{
		Attribute a = attributes.get(index);
		return a.value;
	}

	public synchronized String getAttributeName(int index)
	{
		Attribute a = attributes.get(index);
		return attributeNames.get(a.key);		
	}

	public int size()
	{
		return attributes.size();
	}

	public synchronized boolean equals(Event other)
	{
		if (attributes.size() != other.attributes.size())
			return false;
		for (Attribute a1 : attributes)
		{
			Value v2 = null;
			for (Attribute a2 : other.attributes)
				if (a2.key == a1.key)
					v2 = a2.value;
			if ((v2 == null) || (!v2.equals(a1.value)))
				return false;
		}
		return true;
	}

	public boolean matchStringAttribute(String name, String expectedValue)
	{
		Value v = getAttribute(name);
		if (v == null)
			return false;
		if (v.getType() != Value.Type.STRING)
			return false;
		if (!v.getString().equals(expectedValue))
			return false;
		return true;
	}

	public boolean matchIntegerAttribute(String name, long expectedValue)
	{
		Value v = getAttribute(name);
		if (v == null)
			return false;
		if (v.getType() != Value.Type.INTEGER)
			return false;
		if (!v.getInteger().equals(expectedValue))
			return false;
		return true;
	}

	public boolean matchRawAttribute(String name, byte[] expectedData)
	{
		Value v = getAttribute(name);
		if (v == null)
			return false;
		if (v.getType() != Value.Type.RAW)
			return false;
		ByteBuffer data = v.getRaw();
		return data.equals(expectedData);
	}

}
