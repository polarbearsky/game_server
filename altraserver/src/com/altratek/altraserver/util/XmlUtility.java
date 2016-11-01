package com.altratek.altraserver.util;

import java.util.Iterator;
import java.util.LinkedList;

import org.dom4j.Element;
import org.dom4j.Node;

public class XmlUtility {
	/**
	 * 返回节点target的属性attributeName的值<br>
	 * 找不到该属性/属性值为""，返回defaultValue
	 */
	public static String getAttribute(Element target, String attributeName, String defaultValue) {
		String result = target.attributeValue(attributeName, defaultValue);
		if (!"".equals(result)) {
			return result;
		}
		return defaultValue;
	}

	private static int stringAsInt(String str, int defaultValue) {
		if (str != null && str.length() > 0) {
			return Integer.parseInt(str);
		}
		return defaultValue;
	}
	
	// 如果str == null || str.len == 0，返回null。
	private static Integer stringAsInt(String str) {
		if (str != null && str.length() > 0) {
			return Integer.parseInt(str);
		}
		return null;
	}

	private static boolean stringAsBool(String str, boolean defaultValue) {
		if (str != null && str.length() > 0) {
			return Boolean.parseBoolean(str);
		}
		return defaultValue;
	}
	
	private static Boolean stringAsBool(String str) {
		if (str != null && str.length() > 0) {
			return Boolean.parseBoolean(str);
		}
		return null;
	}

	public static int getAttributeAsInt(Element target, String attributeName, int defaultValue) {
		return stringAsInt(getAttribute(target, attributeName, ""), defaultValue);
	}
	
	public static Integer getAttributeAsInt(Element target, String attributeName) {
		return stringAsInt(getAttribute(target, attributeName, null));
	}

	public static boolean getAttributeAsBool(Element target, String attributeName, boolean defaultValue) {
		return stringAsBool(getAttribute(target, attributeName, ""), defaultValue);
	}
	
	public static Boolean getAttributeAsBool(Element target, String attributeName) {
		return stringAsBool(getAttribute(target, attributeName, ""));
	}

	/**
	 * 返回节点target的第一个名为tagName子节点对象<br>
	 * 若找不到子节点，返回null
	 */
	public static Element getFirstChildNamed(Element target, String tagName) {
		return target.element(tagName);
	}

	/**
	 * 返回节点target的第一个名为tagName的子节点的内容
	 */
	public static String getContent_FirstChildNamed(Element target, String tagName) {
		return getFirstChildNamed(target, tagName).getText();
	}

	/**
	 * 返回节点target的第一个名为tagName的子节点的内容<br>
	 * 若节点null/子节点null/子节点内容null/出现异常，返回defaultValue
	 */
	public static String getContent_FirstChildNamed(Element target, String tagName, String defaultValue) {
		try {
			String result = getFirstChildNamed(target, tagName).getText();
			if (result != null)
				return result;
			else
				return defaultValue;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static int getContent_FirstChildNamedAsInt(Element target, String tagName, int defaultValue) {
		return stringAsInt(getContent_FirstChildNamed(target, tagName, null), defaultValue);
	}
	
	public static Integer getContent_FirstChildNamedAsInt(Element target, String tagName) {
		return stringAsInt(getContent_FirstChildNamed(target, tagName, null));
	}

	public static boolean getContent_FirstChildNamedAsBool(Element target, String tagName, boolean defaultValue) {
		return stringAsBool(getContent_FirstChildNamed(target, tagName, null), defaultValue);
	}
	
	public static Boolean getContent_FirstChildNamedAsBool(Element target, String tagName) {
		return stringAsBool(getContent_FirstChildNamed(target, tagName, null));
	}

	/**
	 * 返回节点target的内容
	 */
	public static String getContent(Element target) {
		return target.getText();
	}

	/**
	 * 返回节点target的内容<br>
	 * 若节点null/内容null/出现异常，返回defaultValue
	 */
	public static String getContent(Element target, String defaultValue) {
		try {
			String result = target.getText();
			if (result != null)
				return result;
			else
				return defaultValue;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * 返回target的所有Element类型子节点
	 */
	public static LinkedList<Element> getChildElements(Element target) {
		LinkedList<Element> result = new LinkedList<Element>();

		for (int i = 0, size = target.nodeCount(); i < size; i++) {
			Node node = target.node(i);
			if (node instanceof Element) {
				result.add((Element) node);
			}
		}
		return result;
	}

	/**
	 * attach下的所有Element类型子节点合并到base下
	 */
	public static void mergeElement(Element base, Element attach) {
		LinkedList<Element> eList = getChildElements(attach);
		for (Iterator<Element> it = eList.iterator(); it.hasNext();) {
			Element e = it.next();
			base.add(e.createCopy());
		}
	}
}