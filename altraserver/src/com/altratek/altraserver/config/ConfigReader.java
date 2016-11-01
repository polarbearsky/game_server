package com.altratek.altraserver.config;

import java.util.*;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.altratek.altraserver.exception.*;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.util.AltraServerUtils;
import com.altratek.altraserver.util.XmlUtility;

public class ConfigReader {
	private static ConfigReader instance;

	private static String configFile = "config.xml";
	
	private Element xmlAll;	
	
	private ConfigReader() {
		configFile = AltraServerUtils.combinePath(ConfigData.COMMON_CONFIG_DIR, configFile);				
	}

	public static ConfigReader getInstance() {
		if (instance == null) {
			instance = new ConfigReader();
		}
		return instance;
	}

	private static Element loadConfigFile() throws Exception {
		Document document = DocumentHelper.parseText(new String(AltraServerUtils.readFileToByteArray(configFile)));
		Element root = document.getRootElement();		
		return root;
	}	

	public void readServerConfig() throws Exception {
		this.xmlAll = loadConfigFile();
		Element xmlServer = XmlUtility.getFirstChildNamed(xmlAll, "ServerSetup");
		if (xmlServer != null) {
			setServer(xmlServer);
		} else {
			throw new ConfigException("The <ServerSetup></ServerSetup> block is missing");
		}
	}

	private void setServer(Element xmlServer) throws Exception {
		LinkedList<Element> eList = XmlUtility.getChildElements(xmlServer);
		for (Element tempNode : eList) {
			String name = tempNode.getName();
			String value = XmlUtility.getContent(tempNode);
			try {				
				if (name.equalsIgnoreCase("AutoSendPolicyFile")) {
					ConfigData.AUTOSEND_CROSS_DOMAIN = value.equalsIgnoreCase("true");
				}
				// �����������
				else if (name.equalsIgnoreCase("PolicyAllowedDomains")) {
					setAllowedDomains(tempNode);
				} else if (name.equalsIgnoreCase("EnableSysThread")) {
					ConfigData.ENABLE_SYS_THREAD = Boolean.parseBoolean(value);
				}
				// ����ϵͳ�������߳���Ŀ
				else if (name.equalsIgnoreCase("SysHandlerThreads")) {
					if (Integer.parseInt(value) < 1) {
						throw new ConfigException("Value in <SysHandlerThreads> block must be 1 at least");
					} else
						ConfigData.ST_HANDLER_THREADS = Integer.parseInt(value);
				}
				// ������չ�����߳���Ŀ
				else if (name.equalsIgnoreCase("ExtHandlerThreads")) {
					if (Integer.parseInt(value) < 1) {
						throw new ConfigException("Value in <ExtHandlerThreads> block must be 1 at least");
					} else
						ConfigData.XT_HANDLER_THREADS = Integer.parseInt(value);
				}
				// ����ServerWriter���߳���Ŀ
				else if (name.equalsIgnoreCase("OutQueueThreads")) {
					if (Integer.parseInt(value) < 1) {
						throw new ConfigException("Value in <OutQueueThreads> block must be 1 at least");
					} else
						ConfigData.OUT_QUEUE_THREADS = Integer.parseInt(value);
				}
				// ����Lost�����߳���Ŀ
				else if (name.equalsIgnoreCase("LostHandlerThreads")) {
					if (Integer.parseInt(value) < 1) {
						throw new ConfigException("Value in <LostHandlerThreads> block must be 1 at least");
					} else
						ConfigData.LOST_HANDLER_THREADS = Integer.parseInt(value);
				}
				// �����û��δ�����������ʱ��
				else if (name.equalsIgnoreCase("MaxUserIdleTime")) {
					ConfigData.MAX_USER_IDLETIME = Integer.parseInt(value);
				}
				// ������Ϸ�����δ��������������ʱ��
				else if (name.equalsIgnoreCase("MaxGameIdleTime")) {
					ConfigData.MAX_GAME_IDLETIME = Integer.parseInt(value);
				}
				// �����ѽ������ӵ�δ��¼���ʱ��
				else if (name.equalsIgnoreCase("MaxUnloginIdleTime")) {
					ConfigData.MAX_UNLOGIN_IDLETIME = Integer.parseInt(value);
				}
				// ����������Ϣ����󳤶�
				else if (name.equalsIgnoreCase("MaxMsgByteLen")) {
					ConfigData.MAX_MSG_BYTE_LEN = Integer.parseInt(value);
				}
				// ����ͬһ��ip�������¼���û���Ŀ
				else if (name.equalsIgnoreCase("IpFilter")) {
					ConfigData.MAX_USERS_PER_IP = Integer.parseInt(value);
				}
				// ���ù���Ա��������
				else if (name.equalsIgnoreCase("AdminZoneName")) {
					ConfigData.ADMIN_ZONE_NAME = value;
				}
				// ���ù���Ա�ʺ�
				else if (name.equalsIgnoreCase("AdminLogin")) {
					AdminConfig.adminName = value;
				}
				// ���ù���Ա����
				else if (name.equalsIgnoreCase("AdminPassword")) {
					if (value.length() < 6) {
						throw new ConfigException("Admin password must be longer than 6 characters");
					}
					AdminConfig.adminPassword = value;
				}
				// ���������Թ���Ա��ݵ�¼����
				else if (name.equalsIgnoreCase("AdminAllowedAddresses")) {
					setAdminAddresses(tempNode);
				}
				// ����ÿ���������󷿼������Ŀ
				else if (name.equalsIgnoreCase("MaxRoomVars")) {
					int val = -1;
					val = Integer.parseInt(value);
					ConfigData.MAX_ROOM_VARS = val;
				}
				// ����ÿ���û�������û�������Ŀ
				else if (name.equalsIgnoreCase("MaxUserVars")) {
					int val = -1;
					val = Integer.parseInt(value);
					ConfigData.MAX_USER_VARS = val;
				}
				// �����¼����е�����
				else if (name.equalsIgnoreCase("MaxIncomingQueue")) {
					int val = -1;
					val = Integer.parseInt(value);
					if (val > 0) {
						ConfigData.MAX_INCOMING_QUEUE = val;
					}
				} else if (name.equalsIgnoreCase("MsgValidator")) {
					if (value.trim().length() > 0) {
						ConfigData.MSG_VALIDATOR = value.trim();
					}
				} else if (name.equalsIgnoreCase("AppEventHanlder")) {
					if (value.trim().length() > 0) {
						ConfigData.APP_EVENT_HANDLER = value.trim();
					}
				} else if (name.equalsIgnoreCase("IpWhiteListReader")) {
					if (value.trim().length() > 0) {
						ConfigData.IP_WHITE_LIST_READER = value.trim();
					}
				} else if (name.equalsIgnoreCase("UserBuilder")) {
					if (value.trim().length() > 0) {
						ConfigData.USER_BUILDER = value.trim();
					}
				} else if (name.equalsIgnoreCase("ProcessConfigReader")) {
					if (value.trim().length() > 0) {
						ConfigData.PROCESS_CONFIG_READER = value.trim();
					}
				}
			
				// ���õ���ĳ�����ӷ�����Ϣʧ��ʱ�Ƿ�Ͽ��û�����(1Ϊ�Ͽ�)
				else if (name.equalsIgnoreCase("DeadChannelsPolicy")) {
					ConfigData.DEAD_CHANNELS_POLICY = value.equalsIgnoreCase("strict") ? 1 : 0;
				}
				// �����û���Ϣ������ر���
				else if (name.equalsIgnoreCase("ClientMessageQueue")) {
					LinkedList<Element> eList2 = XmlUtility.getChildElements(tempNode);
					for (Element subNode : eList2) {
						String nodeName = subNode.getName();
						String nodeValue = XmlUtility.getContent(subNode);
						// ���õ����û�����Ϣ������ȴ����͵���Ϣ�������Ŀ
						// ע�⣺����ͬ<MaxWriterQueue>���ý��޸ĵĶ���ͬһ������ConfigData.MAX_CHANNEL_QUEUE,�����������ֵ�����ڱ����ý�,�������ýڲ�����ʱ<MaxWriterQueue>��������
						if (nodeName.equalsIgnoreCase("QueueSize")) {
							ConfigData.MAX_CHANNEL_QUEUE = Integer.parseInt(nodeValue);
						}
						// ������������ʧ��Ϣ������
						else if (nodeName.equalsIgnoreCase("MaxAllowedDroppedPackets")) {
							ConfigData.MAX_DROPPED_PACKETS = Integer.parseInt(nodeValue);
						}
					}
				}
				// ���ú�̨Task��ִ��ʱ����
				else if (name.equalsIgnoreCase("schedulerResolution")) {
					ConfigData.TASK_EXECUTE_INTERVAL = Integer.parseInt(value);
				} else if (name.equalsIgnoreCase("EnableProfile")) {
					ConfigData.ENABLE_PROFILE = value.equalsIgnoreCase("true");
				} else if (name.equalsIgnoreCase("EnableProfileEventWaitingDuration")) {
					ConfigData.ENABLE_PROFILE_EVENT_WAITING_DURATION = value.equalsIgnoreCase("true");
				} else if (name.equalsIgnoreCase("EnableMsgLengthDebug")) {
					ConfigData.ENABLE_MSG_LENGTH_DEBUG = value.equalsIgnoreCase("true");
				}
			} catch (ConfigException ce) {
				throw ce;
			} catch (Exception e) {
				throw new Exception(String.format("wrong config node : %s / %s", name, value));
			}
		}
	}

	private void setAdminAddresses(Element node) throws Exception {
		LinkedList<Element> eList = XmlUtility.getChildElements(node);
		for (Element addressNode : eList) {
			String ip = XmlUtility.getContent(addressNode, "");
			if (!ip.equals("")) {
				AdminConfig.addAdminAddress(ip);
			}
		}
	}

	private void setAllowedDomains(Element node) throws Exception {
		ConfigData.ALLOWED_DOMAINS = new ArrayList<String>();
		LinkedList<Element> eList = XmlUtility.getChildElements(node);
		for (Element domainNode : eList) {
			String allowedDomain = XmlUtility.getContent(domainNode, "");
			if (!allowedDomain.equals("")) {
				ConfigData.ALLOWED_DOMAINS.add(allowedDomain);
			}
		}
	}

	public static List<String> loadIpWhiteList() {
		List<String> ipList = new ArrayList<String>();
		try {
			// ��Ϊ�����ȸ��£����Բ������Ѿ�load������xmlAll�ڵ����
			Element root = loadConfigFile();
			Element xmlServer = XmlUtility.getFirstChildNamed(root, "ServerSetup");
			Element whiteListNode = XmlUtility.getFirstChildNamed(xmlServer, "NoLimitAddresses");
			LinkedList<Element> eList = XmlUtility.getChildElements(whiteListNode);
			for (Element addressNode : eList) {
				String ip = XmlUtility.getContent(addressNode, "");
				if (!ip.equals("")) {
					ipList.add(ip.trim());
				}
			}
		} catch (Exception e) {
			ServerLogger.error("load ip white list error", e);
		}
		return ipList;
	}
	
	public Element getZoneDetailConfigNode() throws Exception {
		LinkedList<Element> eList = XmlUtility.getChildElements(XmlUtility.getFirstChildNamed(xmlAll, "TemplateZones"));
		if (eList.isEmpty()) {
			throw new ConfigException("No found template zone");
		}

		return (Element) eList.getFirst(); //Ŀǰû�ж�ģ������	
	}	
}