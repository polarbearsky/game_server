package com.altratek.altraserver.domain;

public class UserVariable
{
	protected String type;
	protected String value;

	public static final String TYPE_NULL = "x";
	public static final String TYPE_BOOLEAN = "b";
	public static final String TYPE_NUMBER = "n";
	public static final String TYPE_STRING = "s";

	public UserVariable(String type, String value)
	{
		this.type = type;
		this.value = value;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getType()
	{
		return type;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}
	
	public boolean getBoolValue() {
		return this.value == "1" ? true : false;
	}
	
	public int getIntValue() {
		return Integer.parseInt(this.value); 
	}
	
	public String getStringValue() {
		return this.value;		
	}
	
	public static UserVariable createUserVariable(Object value) {
		String t, v = "";
		if (value == null) {
			t = TYPE_NULL;
			v = "anything";
		} else {
			if (value instanceof Integer) {
				t = TYPE_NUMBER;				
			} else if (value instanceof Short) {
				t = TYPE_NUMBER;
			} else if (value instanceof Long) {
				t = TYPE_NUMBER;
			} else if (value instanceof Double) {
				t = TYPE_NUMBER;
			} else if (value instanceof Float) {
				t = TYPE_NUMBER;
			} else if (value instanceof String) {
				t = TYPE_STRING;			
			} else if (value instanceof Boolean) {
				t = TYPE_BOOLEAN;
			} else {
				throw new IllegalArgumentException("不支持该类型的用户变量。");
			}
			
			if (t == TYPE_NUMBER || t == TYPE_STRING) {
				v = value.toString();
			}
			if (t == TYPE_BOOLEAN) {
				v = ((Boolean)value) ? "1" : "0";
			}
		}
		
		return new UserVariable(t, v);
	}
}