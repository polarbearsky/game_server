package com.altratek.altraserver.database;

import java.util.*;

public class DataRow
{
	public static final int DATAROW_TYPE_VALUEONLY = 0;//只带有列值，对应的存储属性为arrayList（默认值）
	public static final int DATAROW_TYPE_NAMEANDVALUE = 1;//包括有列名和对应列值，对应的存储属性为hashMap

	private int dataRowType;
	private ArrayList<Object> arrayList = null;
	private HashMap<String, Object> hashMap = null;

	/**
	 * 构造默认的只带有列值的DataRow对象（DATAROW_TYPE_VALUEONLY）
	 */
	public DataRow()
	{
		this(DATAROW_TYPE_VALUEONLY);
	}

	public DataRow(int dataRowType)
	{
		this.dataRowType = dataRowType;
		if(dataRowType == DATAROW_TYPE_VALUEONLY)
		{
			arrayList = new ArrayList<Object>();
		}
		else if(dataRowType == DATAROW_TYPE_NAMEANDVALUE)
		{
			hashMap = new HashMap<String, Object>();
		}
	}

	public int getDataRowType()
	{
		return dataRowType;
	}

	/**
	 * 仅对属于DATAROW_TYPE_VALUEONLY的DataRow起作用
	 */
	public void addItem(String s)
	{
		if(arrayList != null)arrayList.add(s);
	}

	/**
	 * 仅对属于DATAROW_TYPE_NAMEANDVALUE的DataRow起作用
	 */
	public void addItem(String key, String s)
	{
		if(hashMap != null)hashMap.put(key, s);
	}

	/**
	 * 仅对属于DATAROW_TYPE_VALUEONLY的DataRow起作用，其余则返回null 
	 * @param key
	 * @return
	 */
	public String getItem(int key)
	{
		try
		{
			if(arrayList != null)
			{
				return (String)arrayList.get(key);
			}
			else
			{
				return null;
			}
		}
		catch(IndexOutOfBoundsException indexoutofboundsexception)
		{
			return null;
		}
	}

	/**
	 * 仅对属于DATAROW_TYPE_NAMEANDVALUE的DataRow起作用，其余则返回null 
	 * @param key
	 * @return
	 */
	public String getItem(String key)
	{
		if(hashMap != null)
		{
			return (String)hashMap.get(key);
		}
		else
		{
			return null;
		}
	}

	public List<Object> getDataAsList()
	{
		return arrayList;
	}

	public Map<String, Object> getDataAsMap()
	{
		return hashMap;
	}

	public Object getObject()
	{
		if(dataRowType == DATAROW_TYPE_VALUEONLY)
		{
			return getDataAsList();
		}
		else return getDataAsMap();
	}
}