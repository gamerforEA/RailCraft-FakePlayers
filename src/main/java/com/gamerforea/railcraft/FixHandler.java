package com.gamerforea.railcraft;

import com.gamerforea.eventhelper.util.EventUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;

public final class FixHandler
{
	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new FixHandler());
	}

	@SubscribeEvent
	public void onMinecartInteract(MinecartInteractEvent event)
	{
		if (EventUtils.cantDamage(event.player, event.minecart))
			event.setCanceled(true);
	}
}
