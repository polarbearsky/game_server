package com.altratek.altraserver.domain;

public class RoomVariable extends UserVariable
{
	private User owner;
	private boolean persistent;
	private boolean priv;

	public RoomVariable(String type, String value, User owner, boolean persistent, boolean priv)
	{
		super(type, value);
		this.owner = owner;
		this.persistent = persistent;
		this.priv = priv;
	}

	public boolean isPersistent()
	{
		return persistent;
	}

	public User getOwner()
	{
		return owner;
	}

	public boolean isPrivate()
	{
		return priv;
	}

	public void setPersistent(boolean b)
	{
		persistent = b;
	}

	public void setPrivate(boolean b)
	{
		priv = b;
	}

	public void setOwner(User user)
	{
		owner = user;
	}

	public boolean equals(Object o)
	{
		if(o instanceof RoomVariable)
		{
			RoomVariable roomVariable = (RoomVariable)o;
			return roomVariable.getValue().equals(value)
					&& roomVariable.getType().equals(type)
					&& roomVariable.isPersistent() == persistent
					&& roomVariable.isPrivate() == priv;
		}
		else return false;
	}

	public int hashCode()
	{
		int result = 31;
		result = result * 37 + type.hashCode();
		result = result * 37 + value.hashCode();
		result = result * 37 + (priv ? 0 : 1);
		result = result * 37 + (persistent ? 0 : 1);
		return result;
	}

	public String toString()
	{
		//Variable value: %s, private: %b, persistent: %b, owner: %s
		StringBuilder result = new StringBuilder("Variable value:").append(value).append(", private:").append(priv).append(", persistent: ").append(persistent).append(", owner:").append(owner.getName());
		return result.toString();
	}
}