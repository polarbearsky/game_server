package com.altratek.altraserver.extensions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.domain.User;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.event.*;
import com.altratek.altraserver.lib.*;
import com.altratek.altraserver.message.MessageChannel;
import com.altratek.altraserver.message.MsgSender;

public abstract class AbstractExtension implements IAltraExtension, IEventListener {
	protected String zoneName;

	public AbstractExtension() {
	}

	public final void setOwner(String zoneName) {
		this.zoneName = zoneName;
	}

	public final String getOwnerZone() {
		return zoneName;
	}

	public void handleInternalEvent(SystemEvent event) {
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}

	@Override
	public final void registerForEvents(Zone zone) {
		setOwner(zone.getName());
		short[] eventTypes = this.getInterestEvents();
		SystemEventRegistry registry = zone.getEventRegistry();
		if (eventTypes == null) {
			registry.registerAll(this);
		} else {
			registry.register(eventTypes, this);
		}
	}

	protected short[] getInterestEvents() {
		return this.getHiercarchyInterestEvent(AbstractExtension.class, "handleInternalEvent", SystemEvent.class);
	}

	protected final short[] getHiercarchyInterestEvent(Class<?> rootClass, String method, Class<?>... parameterTypes) {
		List<Method> toGetAnnotationMethods = new ArrayList<Method>();
		for (Class<?> c = this.getClass(); c != rootClass; c = c.getSuperclass()) {
			Method m = null;
			try {
				m = c.getMethod(method, parameterTypes);
			} catch (Exception e) {
			}

			if (m != null && m.getDeclaringClass() == c) {
				toGetAnnotationMethods.add(m);
			}
		}

		short[] noInterestEvent = new short[0];
		if (toGetAnnotationMethods.size() == 0) {
			// 没有override方法，不监听任何事件
			return noInterestEvent;
		}

		List<Short> interestEvents = new ArrayList<Short>();
		for (Method m : toGetAnnotationMethods) {
			Listen listen = m.getAnnotation(Listen.class);
			if (listen == null) {
				continue;
			}

			for (short i : listen.value()) {
				if (!interestEvents.contains(i)) {
					interestEvents.add(i);
				}
			}
		}

		if (interestEvents.size() == 0) {
			// 没有listen annotation，无事件过滤，监听所有事件。
			short[] allInterestEvent = null;
			return allInterestEvent;
		} else {
			Short[] objArr = interestEvents.toArray(new Short[interestEvents.size()]);
			short[] shortArr = new short[objArr.length];
			for (int i = 0; i < objArr.length; i++) {
				shortArr[i] = objArr[i];
			}
			return shortArr;
		}
	}

	public void trace(String msg) {
		String s = new StringBuilder("[ ").append(getClass().getName()).append(" ]: ").append(msg).toString();
		System.out.println(s);
	}

	public abstract void handleRequest(String cmd, ActionscriptObject asObject, User sender, int fromRoom);

	public abstract void handleRequest(byte cmd, IoBuffer byteBuffer, User sender, int fromRoom);

	public void sendResponse(ActionscriptObject asObject, MessageChannel recipient) {
		MsgSender.sendXtResponse(asObject, recipient, this);
	}

	public void sendResponse(ActionscriptObject asObject, Collection<? extends MessageChannel> recipients) {
		MsgSender.sendXtResponse(asObject, recipients, this);
	}

	public void sendByteResponse(byte[] arrByteCmd, MessageChannel recipient) {
		MsgSender.sendXtByteResponse(arrByteCmd, recipient, this);
	}

	public void sendByteResponse(byte[] arrByteCmd, Collection<? extends MessageChannel> recipients) {
		MsgSender.sendXtByteResponse(arrByteCmd, recipients, this);
	}
}