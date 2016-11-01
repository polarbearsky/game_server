package com.netty.game.server.validate;

import java.util.HashSet;
import java.util.Set;

import com.netty.game.server.util.StringUtils;

public class AccountValidate {
	private static final Set<String> WHITE_LIST = new HashSet<>();
	
	static{
		WHITE_LIST.add("polarbear");
	}
	
	public static final AccountValidate INSTANCE = new AccountValidate();
	
	public boolean isValidate(String userName, String pwd){
		if(StringUtils.isBlank(userName) || StringUtils.isBlank(pwd)
				|| !userName.equalsIgnoreCase(pwd)){
			return false;
		}
		return WHITE_LIST.contains(userName);
	}
}
