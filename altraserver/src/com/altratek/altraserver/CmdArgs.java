package com.altratek.altraserver;

import com.altratek.altraserver.logger.SimpleLogger;

class CmdArgs {
	public final String processConfigReadType;
	public final String processId;
	public final String commonConfigDir;
	public final String processConfigDir;
	public final String logDir;

	public static final String PROCESS_CONFIG_READ_TYPE_LOCAL = "local";

	private static final String CMD_PROMPT = "wrong command args:\n"
			+ "0 args : all dirs are current dir.\n"
			+ "3 args : [common config dir] [process config dir] [log config dir]\n"
			+ "4 args : local [common config dir] [process config dir] [log config dir]\n"
			+ "4 args : db [process id] [common config dir] [log config dir]\n";

	public CmdArgs(String args[]) throws Exception {
		int argLen = args.length;
		switch (argLen) {
		case 0:// ����ѧϰģʽ���������ö��ڵ�ǰĿ¼
			processConfigReadType = PROCESS_CONFIG_READ_TYPE_LOCAL;
			processId = null;
			commonConfigDir = "";
			processConfigDir = "";
			logDir = "";
			break;
		case 3:// ���ݾɵ�ģʽ���ֱ��ƶ���ͬ�������õ�Ŀ¼(Ӧ�ü�/���̼�)
			processConfigReadType = PROCESS_CONFIG_READ_TYPE_LOCAL;
			processId = null;
			commonConfigDir = args[0];
			processConfigDir = args[1];
			logDir = args[2];
			break;
		case 4: // �²���ģʽ
			processConfigReadType = args[0].toLowerCase();
			if (processConfigReadType.equals(PROCESS_CONFIG_READ_TYPE_LOCAL)) {
				processId = null;
				commonConfigDir = args[1];
				processConfigDir = args[2];
				logDir = args[3];
			} else {
				processId = args[1];
				commonConfigDir = args[2];
				processConfigDir = null;
				logDir = args[3];
			}
			break;
		default:
			// log dir��û�㶨�أ�����д�ڵ�ǰĿ¼��
			SimpleLogger.init(""); 
			SimpleLogger.info(CMD_PROMPT);
			throw new Exception("wrong command args");
		}
	}
	
	public boolean readProcessConfigLocal() {
		return processConfigDir != null;
	}

	@Override
	public String toString() {
		return "[processConfigReadType=" + processConfigReadType
				+ ", processId=" + processId + ", commonConfigDir="
				+ commonConfigDir + ", processConfigDir=" + processConfigDir
				+ ", logDir=" + logDir + "]";
	}
}
