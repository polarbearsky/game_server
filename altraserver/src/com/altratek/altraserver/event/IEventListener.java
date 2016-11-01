package com.altratek.altraserver.event;

public interface IEventListener
{
	public abstract void handleInternalEvent(SystemEvent event);
	public abstract void setOwner(String zoneName);
	public abstract String getOwnerZone();	
}
