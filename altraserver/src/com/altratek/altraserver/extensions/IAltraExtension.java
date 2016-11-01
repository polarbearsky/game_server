package com.altratek.altraserver.extensions;

import com.altratek.altraserver.buffer.IoBuffer;
import com.altratek.altraserver.domain.*;
import com.altratek.altraserver.lib.ActionscriptObject;

public interface IAltraExtension {
	public abstract void init();

	public abstract void destroy();

	public abstract void registerForEvents(Zone zone);

	public abstract void handleRequest(String cmd, ActionscriptObject asObject, User sender, int fromRoom);

	public abstract void handleRequest(byte cmd, IoBuffer byteBuffer, User sender, int fromRoom);
}