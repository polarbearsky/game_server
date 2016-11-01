package com.altratek.altraserver.database;

import java.util.*;

public class DataRow
{
	public static final int DATAROW_TYPE_VALUEONLY = 0;//ֻ������ֵ����Ӧ�Ĵ洢����ΪarrayList��Ĭ��ֵ��
	public static final int DATAROW_TYPE_NAMEANDVALUE = 1;//�����������Ͷ�Ӧ��ֵ����Ӧ�Ĵ洢����ΪhashMap

	private int dataRowType;
	private ArrayList<Object> arrayList = null;
	private HashMap<String, Object> hashMap = null;

	/**
	 * ����Ĭ�ϵ�ֻ������ֵ��DataRow����DATAROW_TYPE_VALUEONLY��
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
	 * ��������DATAROW_TYPE_VALUEONLY��DataRow������
	 */
	public void addItem(String s)
	{
		if(arrayList != null)arrayList.add(s);
	}

	/**
	 * ��������DATAROW_TYPE_NAMEANDVALUE��DataRow������
	 */
	public void addItem(String key, String s)
	{
		if(hashMap != null)hashMap.put(key, s);
	}

	/**
	 * ��������DATAROW_TYPE_VALUEONLY��DataRow�����ã������򷵻�null 
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
	 * ��������DATAROW_TYPE_NAMEANDVALUE��DataRow�����ã������򷵻�null 
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