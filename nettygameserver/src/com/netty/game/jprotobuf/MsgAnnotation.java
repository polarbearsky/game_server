package com.netty.game.jprotobuf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MsgAnnotation {
	public RequestResponse type();
	
	public String cmd();
		
	enum RequestResponse{
		REQUEST,
		RESPONSE
	}
}
