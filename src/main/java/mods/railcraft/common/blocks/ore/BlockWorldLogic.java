/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.blocks.ore;

import com.google.common.base.Objects;
import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.plugins.forge.CreativePlugin;
import mods.railcraft.common.plugins.forge.RailcraftRegistry;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.util.misc.MiscTools;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.EnumSet;
import java.util.Random;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class BlockWorldLogic extends Block
{

	private static BlockWorldLogic instance;

	public static BlockWorldLogic getBlock()
	{
		return instance;
	}

	public static void registerBlock()
	{
		if (instance == null && RailcraftConfig.isBlockEnabled("worldlogic"))
		{
			instance = new BlockWorldLogic();
			RailcraftRegistry.register(instance);
		}
	}

	public BlockWorldLogic()
	{
		super(Material.rock);
		this.setBlockName("railcraft.worldlogic");
		this.setResistance(6000000.0F);
		this.setBlockUnbreakable();
		this.setStepSound(Block.soundTypeStone);
		this.setCreativeTab(CreativePlugin.RAILCRAFT_TAB);
		this.disableStats();

		this.setTickRandomly(true);
	}

	@Override
	public void registerBlockIcons(IIconRegister iconRegister)
	{
	}

	@Override
	public IIcon getIcon(int side, int meta)
	{
		return Blocks.bedrock.getIcon(side, meta);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z)
	{
		super.onBlockAdded(world, x, y, z);
		world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
	}

	// TODO gamerforEA code start
	private final ThreadLocal<Integer> updateTickCounter = new ThreadLocal<>();
	// TODO gamerforEA code end

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand)
	{
		// TODO gamerforEA code replace, old code:
		// world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
		Integer counter = this.updateTickCounter.get();
		if (counter != null && counter > 10)
			return;
		this.updateTickCounter.set(Objects.firstNonNull(counter, 0) + 1);
		try
		{
			world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
		}
		finally
		{
			this.updateTickCounter.set(counter);
		}
		// TODO gamerforEA code end

		if (MiscTools.getRand().nextInt(32) != 0)
			return;
		BlockOre blockOre = BlockOre.getBlock();
		if (blockOre == null || !EnumOre.SALTPETER.isEnabled() || !RailcraftConfig.isWorldGenEnabled("saltpeter"))
			return;
		int surfaceY = world.getTopSolidOrLiquidBlock(x, z) - 2;

		if (surfaceY < 50 || surfaceY > 100)
			return;

		Block block = WorldPlugin.getBlock(world, x, surfaceY, z);
		if (block != Blocks.sand)
			return;

		Block above = WorldPlugin.getBlock(world, x, surfaceY + 1, z);
		if (above != Blocks.sand)
			return;

		Block below = WorldPlugin.getBlock(world, x, surfaceY - 1, z);
		if (below != Blocks.sand && below != Blocks.sandstone)
			return;

		int airCount = 0;
		Block ore = BlockOre.getBlock();
		for (ForgeDirection side : EnumSet.of(ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST))
		{
			boolean isAir = world.isAirBlock(MiscTools.getXOnSide(x, side), MiscTools.getYOnSide(surfaceY, side), MiscTools.getZOnSide(z, side));
			if (isAir)
				airCount++;

			if (airCount > 1)
				return;

			if (isAir)
				continue;

			block = WorldPlugin.getBlockOnSide(world, x, surfaceY, z, side);
			if (block != Blocks.sand && block != Blocks.sandstone && block != ore)
				return;
		}

		world.setBlock(x, surfaceY, z, ore, EnumOre.SALTPETER.ordinal(), 3);
		//        System.out.println("saltpeter spawned");
	}

	@Override
	public int tickRate(World world)
	{
		return 6000;
	}

}
