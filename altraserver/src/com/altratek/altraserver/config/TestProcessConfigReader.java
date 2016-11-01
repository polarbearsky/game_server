package com.altratek.altraserver.config;

import java.util.ArrayList;

public class TestProcessConfigReader extends ProcessConfigReader {	

	@Override
	public ProcessConfigData readConfigData() throws Exception {		

		ProcessConfigData pd = new ProcessConfigData();

		pd.ip = "10.17.1.182";
		pd.port = 9338;
		pd.sysHandlerThreads = 9;
		pd.extHandlerThreads = 7;
		pd.outQueueThreads = 14;
		pd.lostHandlerThread = 3;		
		
		pd.logLevel = "warn";

		pd.zoneNames = new ArrayList<String>();
		pd.zoneNames.add("No.1 zone");
		pd.zoneNames.add("No.2 zone");

		return pd;
	}
}
