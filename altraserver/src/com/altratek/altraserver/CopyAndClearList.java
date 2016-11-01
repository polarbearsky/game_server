package com.altratek.altraserver;

import java.util.ArrayList;
import java.util.List;

public class CopyAndClearList<T> {
	private final List<T> dataList = new ArrayList<T>();

	public synchronized List<T> copy() {
		if (this.dataList.size() == 0) {
			// �����棬�÷���empty list�����������֧�Ƚ�Ƶ����Ϊ��ʡ���ڴ棬���Է���null��
			// ֻ��һ��������÷���ֵ�������ж�null�������鷳���Խ��ܡ�
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
