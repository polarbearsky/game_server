package com.altratek.altraserver.event;

import java.util.ArrayList;
import java.util.List;

public class SystemEventRegistry {
	// 没有generic数组，so object[]
	// 未考虑扩展动态load，否则要减少对扩展的引用
	private final Object[] listenerMatrix = new Object[SystemEvent.eventNames.length];

	public SystemEventRegistry() {
		for (int i = 0; i < listenerMatrix.length; i++) {
			listenerMatrix[i] = new ArrayList<IEventListener>();
		}
	}

	@SuppressWarnings("unchecked")
	public List<IEventListener> getListeners(short eventType) {
		return (ArrayList<IEventListener>) listenerMatrix[eventType];
	}

	public void register(short[] eventTypes, IEventListener listener) {
		for (short et : eventTypes) {
			addListener(et, listener);
		}
	}

	public void registerAll(IEventListener listener) {
		for (short i = 0; i < listenerMatrix.length; i++) {
			addListener(i, listener);
		}
	}

	private void addListener(short eventType, IEventListener listener) {
		List<IEventListener> listeners = this.getListeners(eventType);
		if (listeners.contains(listener)) {
			return;
		}
		listeners.add(listener);
	}
}
