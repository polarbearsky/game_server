package com.altratek.altraserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.dom4j.Element;

import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.Room;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.exception.CreateRoomException;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.util.XmlUtility;

public class ZoneManager {

	public final static ZoneManager instance = new ZoneManager();

	private Map<String, Zone> zones = new ConcurrentHashMap<String, Zone>();

	public void init() {
	}

	public Zone getZoneByName(String zoneName) {
		return zones.get(zoneName);
	}

	public int getZoneCount() {
		return zones.size();
	}

	public int getRoomCount() {
		int result = 0;
		LinkedList<Zone> zoneList = new LinkedList<Zone>(zones.values());
		for (Zone zone : zoneList) {
			result += zone.getRoomCount();
		}
		return result;
	}

	private Zone createZone(String zoneName) {
		if (zoneName.equals(ConfigData.ADMIN_ZONE_NAME)) {
			// 管理员zone特殊创建，见createAdminZoneAndRoom
			return null;
		}
		Zone newZone = new Zone(zoneName);
		zones.put(zoneName, newZone);
		return newZone;
	}

	void createAdminZoneAndRoom() {
		ServerLogger.info("create admin zone ...");
		try {
			Zone adminZone = new Zone(ConfigData.ADMIN_ZONE_NAME);
			zones.put(ConfigData.ADMIN_ZONE_NAME, adminZone);
			adminZone.roomManager.createRoom("adminRoom", 5, false, false);
			Zone.adminZone = adminZone;

			ServerLogger.info("admin zone created.\n");
		} catch (CreateRoomException cre) {
			ServerLogger.errorf("Could not create admin zone: %s", cre.getMessage());
		}
	}

	public LinkedList<Zone> getAllZones() {
		return new LinkedList<Zone>(zones.values());
	}

	void destroyExtensions() {
		for (Zone zone : zones.values()) {
			zone.destoryExtensions();
		}
	}
	
	public void loadZones(Element zoneDetailConfigNode) throws Exception {
		for(String zoneName : ConfigData.zoneList) {
			setupZone(zoneName, zoneDetailConfigNode);
		}
	}

	public void setupZone(String zoneName, Element zoneNode) throws Exception {		

		Zone targetZone = createZone(zoneName);

		targetZone.initWithConfig(zoneNode);		

		setZoneAllRoom(zoneNode, targetZone);

		Element extNode = XmlUtility.getFirstChildNamed(zoneNode, "Extensions");
		if (extNode != null) {
			targetZone.extensionManager.parseExtensions(extNode);
		}

		ServerLogger.infof("Zone [%s] : %s rooms, %s extensions", targetZone.getName(), targetZone.getRoomCount(),
				targetZone.extensionManager.getExtensionCount());		
	}

	private void setZoneAllRoom(Element zoneNode, Zone zone) throws CreateRoomException {
		Element e = XmlUtility.getFirstChildNamed(zoneNode, "Rooms");
		if (e != null) {
			LinkedList<Element> eList = XmlUtility.getChildElements(e);
			for (Iterator<Element> it = eList.iterator(); it.hasNext();) {
				Element roomNode = it.next();

				setupRoom(roomNode, zone);
			}
		} else {
			throw new CreateRoomException("The <Rooms> Block in Zone[" + zone.getName() + "] is missing");
		}
	}

	private void setupRoom(Element room, Zone zone) throws CreateRoomException {
		String roomName = XmlUtility.getAttribute(room, "name", "");
		int maxUsers = XmlUtility.getAttributeAsInt(room, "maxUsers", 40);
		boolean isTemp = XmlUtility.getAttributeAsBool(room, "isTemp", false);
		Room rm = zone.roomManager.createRoom(roomName, maxUsers, isTemp, false);

		// 设置Room变量
		Element vars = XmlUtility.getFirstChildNamed(room, "Vars");
		if (vars != null) {
			LinkedList<Element> eList = XmlUtility.getChildElements(vars);
			for (Element roomVarNode : eList) {
				String varName = XmlUtility.getAttribute(roomVarNode, "name", "");
				String varType = XmlUtility.getAttribute(roomVarNode, "type", "");
				String varValue = XmlUtility.getContent(roomVarNode);
				boolean varPrivate = XmlUtility.getAttributeAsBool(roomVarNode, "private", false);
				boolean varPersist = XmlUtility.getAttributeAsBool(roomVarNode, "persistent", false);
				if (!varName.equals("")) {
					rm.setVariable(varName, varType, varValue, varPrivate, varPersist, null, true);
				}
			}
		}
	}	
}
