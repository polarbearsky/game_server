package com.altratek.altraserver.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

class SimpleLogFileFormatter extends Formatter {
	private static String newLineStr = System.getProperty("line.separator");

	SimpleLogFileFormatter() {
	}

	public String format(LogRecord record) {

		StringBuilder sb = new StringBuilder();
		sb.append(String.valueOf(formatTime(record)));
		sb.append(" - ");	
		sb.append(record.getMessage()).append(newLineStr);

		String s = sb.toString();

		Throwable th = record.getThrown();
		if (th == null) {
			return s;
		}

		sb.append(" ").append(th.toString()).append(newLineStr);

		StackTraceElement elements[] = th.getStackTrace();

		for (int i = 0; i < elements.length; i++) {
			sb.append("\t").append(elements[i].toString()).append(newLineStr);
		}

		return sb.toString();
	}

	private String formatTime(LogRecord record) {
		Date date = new Date(record.getMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}
}
