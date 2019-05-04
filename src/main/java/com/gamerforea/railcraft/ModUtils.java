package com.gamerforea.railcraft;

import com.gamerforea.eventhelper.config.ConfigUtils;
import com.gamerforea.eventhelper.nexus.ModNexus;
import com.gamerforea.eventhelper.nexus.ModNexusFactory;
import com.gamerforea.eventhelper.nexus.NexusUtils;

@ModNexus(name = "RailCraft", uuid = "95762508-ece9-11e4-90ec-1681e6b88ec1")
public final class ModUtils
{
	public static final ModNexusFactory NEXUS_FACTORY = NexusUtils.getFactory();

	public static void init()
	{
		ConfigUtils.readConfig(EventConfig.class);
		FixHandler.init();
	}
}
