package com.gamerforea.railcraft;

import com.gamerforea.eventhelper.util.FastUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

public final class ModUtils
{
	public static final GameProfile profile = new GameProfile(UUID.fromString("95762508-ece9-11e4-90ec-1681e6b88ec1"), "[RailCraft]");
	private static FakePlayer player = null;

	public static final FakePlayer getModFake(World world)
	{
		if (player == null)
			player = FastUtils.getFake(world, profile);
		else
			player.worldObj = world;

		return player;
	}
}
