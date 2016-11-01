package com.altratek.altraserver.extensions;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.dom4j.Element;

import com.altratek.altraserver.config.ConfigData;
import com.altratek.altraserver.domain.Zone;
import com.altratek.altraserver.logger.ServerLogger;
import com.altratek.altraserver.util.XmlUtility;

public class ExtensionManager {
	private final Zone zone;
	private Map<Integer, IAltraExtension> id_Extension;
	private Map<Byte, IAltraExtension> byteCmd_Extension;
	private boolean shuttingDown = false;

	public ExtensionManager(Zone zone) {
		this.zone = zone;
		id_Extension = new ConcurrentHashMap<Integer, IAltraExtension>();
		byteCmd_Extension = new ConcurrentHashMap<Byte, IAltraExtension>();
	}

	private void add(int extensionId, IAltraExtension ext) {
		id_Extension.put(extensionId, ext);
	}

	private void add(Byte extensionByteCmd, IAltraExtension ext) {
		byteCmd_Extension.put(extensionByteCmd, ext);
	}

	private boolean contains(int extensionId) {
		return id_Extension.containsKey(extensionId);
	}

	public int getExtensionCount() {
		return this.id_Extension.size();
	}

	public void destory() {
		if (!shuttingDown) {
			shuttingDown = true;
			for (Entry<Integer, IAltraExtension> en : id_Extension.entrySet()) {
				IAltraExtension ext = en.getValue();
				ext.destroy();
				ServerLogger.debugf("Shutting down extension: %s", ext.getClass().getSimpleName());
			}
		}
	}

	public void parseExtensions(Element node) {
		LinkedList<Element> eList = XmlUtility.getChildElements(node);
		for (Element xt : eList) {
			if (xt.getName().equalsIgnoreCase("extension")) {
				int xtId = Integer.parseInt(XmlUtility.getAttribute(xt, "id", ""));
				if (!this.contains(xtId)) {
					String xtClass = XmlUtility.getAttribute(xt, "className", "");
					String strByteCmd = XmlUtility.getAttribute(xt, "byteCmd", "");
					createExtension(xtId, xtClass, strByteCmd);
				} else {
					ServerLogger.errorf("duplicated extension id : %s", xtId);
				}
			}
		}
	}

	private void createExtension(int extensionId, String className, String strByteCmd) {
		// 读取并得到扩展实例
		IAltraExtension ae = loadExtension(extensionId, className);

		strByteCmd = strByteCmd.trim();
		Byte[] arrByteCmd = new Byte[0];
		if (strByteCmd.trim().length() > 0) {
			String[] ss = strByteCmd.split(",");
			arrByteCmd = new Byte[ss.length];
			for (int i = 0; i < ss.length; i++) {
				arrByteCmd[i] = Byte.valueOf(ss[i]);
			}
		}

		if (ae != null) {
			this.add(extensionId, ae);
			for (int i = 0; i < arrByteCmd.length; i++) {
				this.add(arrByteCmd[i], ae);
			}

			// ServerLogger.infof("Zone extension[%s] created", ae.getClass().getSimpleName());

			ae.registerForEvents(zone);

			ae.init();
		}
	}

	public static void loadAdminExtension() {
		try {
			IAltraExtension extensionObj = new AdminExtension();
			Zone adminZone = Zone.adminZone;
			adminZone.extensionManager.add(ConfigData.ADMIN_EXT_ID, extensionObj);
			extensionObj.registerForEvents(adminZone);
			extensionObj.init();
		} catch (Exception e) {
			ServerLogger.error("<<Load Admin Extension>>: Exception - ", e);
		}
	}

	private IAltraExtension loadExtension(int extensionId, String className) {
		IAltraExtension extensionObj = null;
		try {
			File file = new File(ConfigData.JAVA_EXTENSIONS_PATH);
			ClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI().toURL() });
			// 用IAltraExtension会报错，原因不明
			Class<?> extensionClass = classLoader.loadClass(className);
			if (!AbstractExtension.class.isAssignableFrom(extensionClass)) {
				ServerLogger.errorf("Load Extension : Class[%s] does not extend Class[AbstractExtension]", className);
			} else {
				extensionObj = (IAltraExtension) extensionClass.newInstance();
			}
		} catch (Exception e) {
			ServerLogger.error("Load Extension : Exception - ", e);
		}
		return extensionObj;
	}

	public IAltraExtension getExtension(int extensionId) {
		return id_Extension.get(extensionId);		
	}

	public IAltraExtension getExtension(byte extensionByteCmd) {
		return byteCmd_Extension.get(extensionByteCmd);		
	}
}