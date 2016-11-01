package com.altratek.altraserver.exception;

public class ConfigException extends Exception
{
	private static final long serialVersionUID = 1L;
	
    public ConfigException()
    {
    }

    public ConfigException(String msg)
    {
        super(msg);
    }
}
