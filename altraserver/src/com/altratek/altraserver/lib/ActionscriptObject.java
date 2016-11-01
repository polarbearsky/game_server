package com.altratek.altraserver.lib;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.RowSet;

import com.altratek.altraserver.database.DataRow;
import com.altratek.altraserver.logger.ServerLogger;

/**
 * This class represents a Flash Actionscript Object (typed or untyped)
 * <p>
 * Making a class rather than just deserializing to a HashMap was chosen for the following reasons:<br/>
 * 1) "types" are not going to be native to Hashmap, table, etc.<br/>
 * 2) it helps in making the deserializer/serializer reflexive.
 * </p>
 *
 * @author Jim Whitfield (jwhitfield@macromedia.com)
 * @author Peter Farland
 * @version 1.0
 */
public class ActionscriptObject extends HashMap<String, Object> {
	private static final long serialVersionUID = 1613529666682805692L;

	private boolean inHashCode = false;
	private boolean inToString = false;

	/**
	 * the named type, if any.
	 */
	private String namedType = null;

	/**
	 * Create an Actionscript object.
	 */
	public ActionscriptObject() {
		super();
	}

	/**
	 * Return the hashcode of this object.  The hashcode is defined
	 * to be the sum of the hashcodes of each entry.
	 * @return
	 */
	public int hashCode() {
		int h = 0;
		if (!inHashCode) {
			inHashCode = true;
			try {
				Iterator<Entry<String, Object>> i = entrySet().iterator();
				while (i.hasNext()) {
					h += i.next().hashCode();
				}
			} finally {
				inHashCode = false;
			}
		}
		return h;
	}

	/**
	 * Returns a string representation of this object.  The string representation
	 * consists of a list of key-value mappings in the order returned by the
	 * map's <tt>entrySet</tt> view's iterator, enclosed in braces
	 * (<tt>"{}"</tt>).  Adjacent mappings are separated by the characters
	 * <tt>", "</tt> (comma and space).  Each key-value mapping is rendered as
	 * the key followed by an equals sign (<tt>"="</tt>) followed by the
	 * associated value.  Keys and values are converted to strings as by
	 * <tt>String.valueOf(Object)</tt>.<p>
	 *
	 * This implementation creates an empty string buffer, appends a left
	 * brace, and iterates over the map's <tt>entrySet</tt> view, appending
	 * the string representation of each <tt>map.entry</tt> in turn.  After
	 * appending each entry except the last, the string <tt>", "</tt> is
	 * appended.  Finally a right brace is appended.  A string is obtained
	 * from the stringbuilder, and returned.<p>
	 * 
	 * If the value is found to be recursive, <tt>"..."</tt> (three periods) are
	 * printed to indicate the loop.
	 *
	 * @return a String representation of this map.
	 */
	public String toString() {
		String className = getClass().getName();
		int dotIndex = className.lastIndexOf('.');

		StringBuilder sb = new StringBuilder();
		sb.append(className.substring(dotIndex + 1));
		sb.append("(").append(System.identityHashCode(this)).append(')');
		sb.append('{');
		if (!inToString) {
			inToString = true;
			try {
				boolean pairEmitted = false;

				Iterator<Entry<String, Object>> i = entrySet().iterator();
				while (i.hasNext()) {
					if (pairEmitted) {
						sb.append(", ");
					}
					Entry<String, Object> e = i.next();
					sb.append(e.getKey()).append('=').append(e.getValue());
					pairEmitted = true;
				}
			} finally {
				inToString = false;
			}
		} else {
			sb.append("...");
		}
		sb.append('}');
		return sb.toString();
	}

	//以下是扩展方法

	//========== private putters ===========

	private void putEmptyCollection(String key) {
		super.put(key, new ActionscriptObject[0]);
	}

	//========== public putters ===========

	/**
	 * @deprecated
	 */
	public Object put(String key, Object obj) {
		super.put(key, obj);
		return null;
	}

	public void putString(String key, String s) {
		super.put(key, s);
	}

	public void putString(int key, String s) {
		putString(String.valueOf(key), s);
	}

	public void putByte(String key, byte b) {
		super.put(key, b);
	}

	public void putShort(String key, short s) {
		super.put(key, s);
	}

	public void putInt(String key, int i) {
		super.put(key, i);
	}

	public void putInt(int key, int i) {
		putInt(String.valueOf(key), i);
	}

	public void putLong(String key, long l) {
		super.put(key, l);
	}

	public void putLong(int key, long l) {
		putLong(String.valueOf(key), l);
	}

	public void putNumber(String key, double d) {
		super.put(key, d);
	}

	public void putNumber(int key, double d) {
		putNumber(String.valueOf(key), d);
	}

	public void putBool(String key, boolean b) {
		super.put(key, Boolean.valueOf(b));
	}

	public void putBool(int key, boolean b) {
		putBool(String.valueOf(key), b);
	}

	public void putByteArray(String key, byte[] byteArray) {
		super.put(key, byteArray);
	}

	public void putByteArray(int key, byte[] byteArray) {
		putByteArray(String.valueOf(key), byteArray);
	}

	public void putASObj(String key, ActionscriptObject aObj) {
		super.put(key, aObj);
	}

	public void putASObj(int key, ActionscriptObject aObj) {
		putASObj(String.valueOf(key), aObj);
	}

	public void putDataRow(String key, DataRow row) {
		if (row.getDataRowType() == DataRow.DATAROW_TYPE_VALUEONLY) {
			super.put(key, row.getDataAsList().toArray());
		} else {
			super.put(key, row.getDataAsMap());
		}
	}

	public void putDataRow(int key, DataRow row) {
		putDataRow(String.valueOf(key), row);
	}

	public void putDataRows(String key, List<DataRow> dataRowList) {
		if (dataRowList.size() != 0) {
			Object[] rowData = new Object[dataRowList.size()];
			int i = 0;
			if (dataRowList.get(0).getDataRowType() == DataRow.DATAROW_TYPE_VALUEONLY) {
				for (Iterator<DataRow> it = dataRowList.iterator(); it.hasNext();) {
					rowData[i] = it.next().getDataAsList().toArray();
					i++;
				}
			} else {
				for (Iterator<DataRow> it = dataRowList.iterator(); it.hasNext();) {
					rowData[i] = it.next().getDataAsMap();
					i++;
				}
			}
			super.put(key, rowData);
		} else {
			putEmptyCollection(key);
		}
	}

	public void putArray(String key, Object[] arr) {
		super.put(key, arr);
	}

	public void putASObjectList(String key, List<ActionscriptObject> list) {
		super.put(key, list.toArray());
	}

	/**
	 * 存入T类型对象，必须实现并传入T类对应的IObjectSerializer对象（该对象用于序列化T类型对象）
	 */
	public <T> void putSerialObject(String key, T serialObj, IObjectSerializer<T> serializer) {
		putASObj(key, serializer.serializeToActionscriptObject(serialObj));
	}

	/**
	 * 存入T类型对象List，必须实现并传入T类对应的IObjectSerializer对象（该对象用于序列化T类型对象）
	 */
	public <T> void putSerialObjectList(String key, List<T> serialObjList, IObjectSerializer<T> serializer) {
		List<ActionscriptObject> list = new ArrayList<ActionscriptObject>(serialObjList.size());
		for (T so : serialObjList) {
			list.add(serializer.serializeToActionscriptObject(so));
		}
		putASObjectList(key, list);
	}

	/**
	 * 存入实现ISerialObject接口的任意对象
	 */
	public void putSerialObject(String key, ISerialObject serialObj) {
		putASObj(key, serialObj != null ? serialObj.serializeToASObject() : null);
	}

	/**
	 * 存入T类型对象List，其中T类必须实现ISerialObject接口
	 */
	public <T extends ISerialObject> void putSerialObjectList(String key, List<T> serialObjList) {
		List<ActionscriptObject> list = new ArrayList<ActionscriptObject>(serialObjList.size());
		for (ISerialObject so : serialObjList) {
			if (so != null)
				list.add(so.serializeToASObject());
		}
		putASObjectList(key, list);
	}

	public void putRowSet(String key, RowSet value) {
		this.putASObjectList(key, rowSet2AsObjList(value));
	}

	private List<ActionscriptObject> rowSet2AsObjList(RowSet rs) {
		List<ActionscriptObject> list = new ArrayList<ActionscriptObject>();

		try {
			ResultSetMetaData rsMetaData = rs.getMetaData();
			int totalColumn = rsMetaData.getColumnCount();
			while (rs.next()) {
				ActionscriptObject ao = new ActionscriptObject();
				// the first column is 1
				for (int i = 1; i <= totalColumn; i++) {
					ao.put(rsMetaData.getColumnName(i), rs.getObject(i));
				}
				list.add(ao);
			}
		} catch (Exception e) {
			ServerLogger.error("rowSet2AsObjectList", e);
		}

		return list;
	}

	//========== getters ==========

	public Object get(String key) {
		return super.get(key);
	}

	public Object get(int key) {
		return get(String.valueOf(key));
	}

	public String getString(String key) {
		String s = (String) super.get(key);
		return s == null ? "" : s;
	}

	public String getString(int key) {
		return getString(String.valueOf(key));
	}

	public double getNumber(String key) {
		Object obj = super.get(key);
		if (obj instanceof Integer) {
			Integer var = (Integer) obj;
			return var == null ? 0.0D : var.doubleValue();
		} else {
			Double var = (Double) obj;
			return var == null ? 0.0D : var.doubleValue();
		}
	}

	// In AMF 3 integers are serialized using a variable length unsigned 29-bit integer. 
	// The ActionScript 3.0 integer types - a signed 'int' type and an unsigned 'uint' type - are also represented using 29-bits in AVM+. 
	// If the value of an unsigned integer (uint) is greater or equal to 2^29 or 
	// if the value of a signed integer (int) is greater than or equal to 2^28 then 
	// it will be represented by AVM+ as a double and thus serialized in using the AMF 3 double type.
	public Integer getInt(String key) {
		Object obj = super.get(key);

		if (obj == null) {
			return null;
		}

		if (obj instanceof Integer) {
			return (Integer) obj;
		}

		// 2^28 < int < 2^31
		if (obj instanceof Double) {
			return ((Double) obj).intValue();
		}

		throw new IllegalArgumentException("no int for key:" + key);
	}

	public Long getLong(String key) {
		Object obj = super.get(key);

		if (obj == null) {
			return null;
		}

		if (obj instanceof Integer) {
			return ((Integer) obj).longValue();
		}

		if (obj instanceof Double) {
			return ((Double) obj).longValue();
		}

		//no long type in AMF3

		throw new IllegalArgumentException("no long for key:" + key);
	}

	public double getNumber(int key) {
		return getNumber(String.valueOf(key));
	}

	public boolean getBool(String key) {
		Boolean var = (Boolean) super.get(key);
		return var == null ? false : var.booleanValue();
	}

	public boolean getBool(int key) {
		return getBool(String.valueOf(key));
	}

	public byte[] getByteArray(String key) {
		return (byte[]) super.get(key);
	}

	public byte[] getByteArray(int key) {
		return this.getByteArray(String.valueOf(key));
	}

	//只有之前所put的对象为ActionscriptObject或ASObject，该方法才会正确返回ActionscriptObject对象
	public ActionscriptObject getASObj(String key) {
		return (ActionscriptObject) super.get(key);
	}

	//只有之前所put的对象为ActionscriptObject或ASObject，该方法才会正确返回ActionscriptObject对象
	public ActionscriptObject getASObj(int key) {
		return getASObj(String.valueOf(key));
	}

	/**
	 * 获取指定key的Object[]对象<br/>
	 * 注意：获取的Object[]是从客户端传递上来的，或者是服务端以List方式保存到本ActionscriptObject对象中
	 */
	public Object[] getArray(String key) {
		return (Object[]) super.get(key);
	}

	/**
	 * 获取指定key的Object[]对象<br/>
	 * 注意：获取的Object[]是从客户端传递上来的，或者是服务端以List方式保存到本ActionscriptObject对象中
	 */
	public Object[] getArray(int key) {
		return getArray(String.valueOf(key));
	}

	//=========== hashmap methods ===========

	public Object removeElement(String key) {
		return super.remove(key);
	}

	public Object removeElement(int key) {
		return removeElement(String.valueOf(key));
	}

	//=========== AMF3 Methods ===========

	/**
	 * get the named type, if any.  (otherwise, return null, implying it is unnamed).
	 * @return the type.
	 */
	public String getType() {
		return namedType;
	}

	/**
	 * Sets the named type.  <br/>
	 * This operation is mostly meaningless on an object that came in off the wire,
	 * but will be helpful for objects that will be serialized out to Flash.
	 * @param type the type of the object.
	 */
	public void setType(String type) {
		namedType = type;
	}

	//=========== IExMessageCodec ===========

	public static final IExMessageCodec exMessageCodec = new Amf3ExMessageCodec();
}