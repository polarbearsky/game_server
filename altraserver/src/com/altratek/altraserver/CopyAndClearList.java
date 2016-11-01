package com.altratek.altraserver;

import java.util.ArrayList;
import java.util.List;

public class CopyAndClearList<T> {
	private final List<T> dataList = new ArrayList<T>();

	public synchronized List<T> copy() {
		if (this.dataList.size() == 0) {
			// 按常规，该返回empty list，考虑这个分支比较频繁，为了省点内存，所以返回null。
			// 只有一处会遍历该返回值，所以判断null带拉的麻烦可以接受。
			return null;
		}

		List<T> copyList = new ArrayList<T>(dataList);
		dataList.clear();

		return copyList;
	}

	public synchronized void addAll(List<T> list) {
		this.dataList.addAll(list);
	}

	public synchronized void add(T data) {
		this.dataList.add(data);
	}
}
