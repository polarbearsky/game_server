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
				// 设置允许的域
				else if (name.equalsIgnoreCase("PolicyAllowedDomains")) {
					setAllowedDomains(tempNode);
				} else if (name.equalsIgnoreCase("EnableSysThread")) {
					ConfigData.ENABLE_SYS_THREAD = Boolean.parseBoolean(value);
				}
				// 设置系统处理器线程数目
				else if (name.equalsIgnoreCase("SysHandlerThreads")) {
					if (Integer.parseInt(value) < 1) {
						throw new ConfigException("Value in <SysHandlerThreads> block must be 1 at least");
					} else
						ConfigData.ST_HANDLER_THREADS = Integer.parseInt(value);
				}
				// 设置扩展处理线程数目
				else if (name.equalsIgnoreCase("ExtHandlerThreads")) {
					if (Integer.parseInt(value) < 1) {
						throw new ConfigException("Value in <ExtHandlerThreads> block must be 1 at least");
					} else
						ConfigData.XT_HANDLER_THREADS = Integer.parseInt(value);
				}
				// 设置ServerWriter的线程数目
				else if (name.equalsIgnoreCase("OutQueueThreads")) {
					if (Integer.parseInt(value) < 1) {
						throw new ConfigException("Value in <OutQueueThreads> block must be 1 at least");
					} else
						ConfigData.OUT_QUEUE_THREADS = Integer.parseInt(value);
				}
				// 设置Lost处理线程数目
				else if (name.equalsIgnoreCase("LostHandlerThreads")) {
					if (Integer.parseInt(value) < 1) {
						throw new ConfigException("Value in <LostHandlerThreads> block must be 1 at least");
					} else
						ConfigData.LOST_HANDLER_THREADS = Integer.parseInt(value);
				}
				// 设置用户最长未与服务器交互时间
				else if (name.equalsIgnoreCase("MaxUserIdleTime")) {
					ConfigData.MAX_USER_IDLETIME = Integer.parseInt(value);
				}
				// 设置游戏中玩家未与服务器交互的最长时间
				else if (name.equalsIgnoreCase("MaxGameIdleTime")) {
					ConfigData.MAX_GAME_IDLETIME = Integer.parseInt(value);
				}
				// 设置已建立连接但未登录的最长时间
				else if (name.equalsIgnoreCase("MaxUnloginIdleTime")) {
					ConfigData.MAX_UNLOGIN_IDLETIME = Integer.parseInt(value);
				}
				// 设置上行消息的最大长度
				else if (name.equalsIgnoreCase("MaxMsgByteLen")) {
					ConfigData.MAX_MSG_BYTE_LEN = Integer.parseInt(value);
				}
				// 设置同一个ip上允许登录的用户数目
				else if (name.equalsIgnoreCase("IpFilter")) {
					ConfigData.MAX_USERS_PER_IP = Integer.parseInt(value);
				}
				// 设置管理员区域名称
				else if (name.equalsIgnoreCase("AdminZoneName")) {
					ConfigData.ADMIN_ZONE_NAME = value;
				}
				// 设置管理员帐号
				else if (name.equalsIgnoreCase("AdminLogin")) {
					AdminConfig.adminName = value;
				}
				// 设置管理员密码
				else if (name.equalsIgnoreCase("AdminPassword")) {
					if (value.length() < 6) {
						throw new ConfigException("Admin password must be longer than 6 characters");
					}
					AdminConfig.adminPassword = value;
				}
				// 设置允许以管理员身份登录的域
				else if (name.equalsIgnoreCase("AdminAllowedAddresses")) {
					setAdminAddresses(tempNode);
				}
				// 设置每个房间的最大房间变量数目
				else if (name.equalsIgnoreCase("MaxRoomVars")) {
					int val = -1;
					val = Integer.parseInt(value);
					ConfigData.MAX_ROOM_VARS = val;
				}
				// 设置每个用户的最大用户变量数目
				else if (name.equalsIgnoreCase("MaxUserVars")) {
					int val = -1;
					val = Integer.parseInt(value);
					ConfigData.MAX_USER_VARS = val;
				}
				// 设置事件队列的容量
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
			
				// 设置当向某个连接发送消息失败时是否断开用户连接(1为断开)
				else if (name.equalsIgnoreCase("DeadChannelsPolicy")) {
					ConfigData.DEAD_CHANNELS_POLICY = value.equalsIgnoreCase("strict") ? 1 : 0;
				}
				// 设置用户消息队列相关变量
				else if (name.equalsIgnoreCase("ClientMessageQueue")) {
					LinkedList<Element> eList2 = XmlUtility.getChildElements(tempNode);
					for (Element subNode : eList2) {
						String nodeName = subNode.getName();
						String nodeValue = XmlUtility.getContent(subNode);
						// 设置单个用户的消息队列里等待发送的消息的最大数目
						// 注意：这里同<MaxWriterQueue>配置节修改的都是同一个变量ConfigData.MAX_CHANNEL_QUEUE,正常情况下其值依赖于本配置节,当本配置节不存在时<MaxWriterQueue>方起作用
						if (nodeName.equalsIgnoreCase("QueueSize")) {
							ConfigData.MAX_CHANNEL_QUEUE = Integer.parseInt(nodeValue);
						}
						// 设置允许单个丢失消息最大次数
						else if (nodeName.equalsIgnoreCase("MaxAllowedDroppedPackets")) {
							ConfigData.MAX_DROPPED_PACKETS = Integer.parseInt(nodeValue);
						}
					}
				}
				// 设置后台Task的执行时间间隔
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
			// 因为可能热更新，所以不能用已经load进来的xmlAll节点对象
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

		return (Element) eList.getFirst(); //目前没有多模块的情况	
	}	
}