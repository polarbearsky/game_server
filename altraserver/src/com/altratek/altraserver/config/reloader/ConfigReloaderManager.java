package com.altratek.altraserver.config.reloader;

import java.util.concurrent.ConcurrentHashMap;

public class ConfigReloaderManager
{
	public static final String TYPE_CONFIG_RELOADER = "config";
	public static final String TYPE_EXCONFIG_RELOADER = "exconfig";

	private ConcurrentHashMap<String, IConfigReloader> reloaders;

	public ConfigReloaderManager()
	{
		reloaders = new ConcurrentHashMap<String, IConfigReloader>();
	}

	public void register(String type, IConfigReloader reloader)
	{
		reloaders.put(type, reloader);
	}

	public void unRegister(String type)
	{
		reloaders.remove(type);
	}

	public void reloadConfig(String type, String param)
	{
		IConfigReloader reloader = reloaders.get(type);
		if(reloader != null)
		{
			reloader.reload(param);
		}
	}
}