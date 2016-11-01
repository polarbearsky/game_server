package com.altratek.altraserver.config;

import java.util.ArrayList;
import java.util.LinkedList;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.altratek.altraserver.util.AltraServerUtils;
import com.altratek.altraserver.util.XmlUtility;

public class DefaultProcessConfigReader extends ProcessConfigReader {

	private final static String wsConfigFile = "wsConfig.xml";

	@Override
	public ProcessConfigData readConfigData() throws Exception {
		String configFullPath = AltraServerUtils.combinePath(this.processConfigFileDir, wsConfigFile);
		Document document = DocumentHelper.parseText(new String(AltraServerUtils.readFileToByteArray(configFullPath)));
		Element wsConfigRoot = document.getRootElement();

		Element c = XmlUtility.getFirstChildNamed(wsConfigRoot, "config");
		Element ss = XmlUtility.getFirstChildNamed(c, "ServerSetup");
		Element z = XmlUtility.getFirstChildNamed(c, "Zones");

		ProcessConfigData pd = new ProcessConfigData();

		pd.ip = XmlUtility.getContent_FirstChildNamed(ss, "ServerIP", null);
		pd.port = XmlUtility.getContent_FirstChildNamedAsInt(ss, "ServerPort", 0);
		pd.sysHandlerThreads = XmlUtility.getContent_FirstChildNamedAsInt(ss, "SysHandlerThreads");
		pd.extHandlerThreads = XmlUtility.getContent_FirstChildNamedAsInt(ss, "ExtHandlerThreads");
		pd.outQueueThreads = XmlUtility.getContent_FirstChildNamedAsInt(ss, "OutQueueThreads");
		pd.lostHandlerThread = XmlUtility.getContent_FirstChildNamedAsInt(ss, "LostHandlerThreads");
		pd.enableProfile = XmlUtility.getContent_FirstChildNamedAsBool(ss, "enableProfile");
		pd.enableMsgLengthDebug = XmlUtility.getContent_FirstChildNamedAsBool(ss, "enableMsgLengthDebug");

		pd.zoneNames = new ArrayList<String>();
		LinkedList<Element> eList = XmlUtility.getChildElements(z);
		for (Element zoneNode : eList) {
			pd.zoneNames.add(XmlUtility.getAttribute(zoneNode, "name", null));
		}

		return pd;
	}
}
