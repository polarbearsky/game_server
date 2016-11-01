package com.altratek.altraserver.exception;

public class LoginException extends Exception
{
	private static final long serialVersionUID = 1;
	
	public LoginException()
	{
	}
	
	public LoginException(String errMsg)
	{
		super(errMsg);
	}
}
