package com.altratek.altraserver.message;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class MsgCharset {
	private final static String CHARSET_NAME = "UTF-8";
	public final CharsetEncoder encoder = Charset.forName(CHARSET_NAME).newEncoder();
	public final CharsetDecoder decoder = Charset.forName(CHARSET_NAME).newDecoder();
}
