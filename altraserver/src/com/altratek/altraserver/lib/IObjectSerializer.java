package com.altratek.altraserver.lib;

public interface IObjectSerializer<T>
{
	public ActionscriptObject serializeToActionscriptObject(T t);
}