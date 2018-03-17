/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.fluids;

import com.gamerforea.railcraft.ExplosionByPlayer;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.railcraft.client.particles.EntityDropParticleFX;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.Random;

/**
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public class BlockRailcraftFluid extends BlockFluidClassic
{
	protected float particleRed;
	protected float particleGreen;
	protected float particleBlue;
	@SideOnly(Side.CLIENT)
	protected IIcon[] theIcon;
	protected boolean flammable;
	protected int flammability = 0;
	private boolean hasFlowIcon = true;

	public BlockRailcraftFluid(Fluid fluid, Material material)
	{
		super(fluid, material);
		this.setDensity(fluid.getDensity());
	}

	public BlockRailcraftFluid setNoFlow()
	{
		this.hasFlowIcon = false;
		return this;
	}

	@Override
	public boolean canDrain(World world, int x, int y, int z)
	{
		return true;
	}

	@Override
	public Fluid getFluid()
	{
		return FluidRegistry.getFluid(this.fluidName);
	}

	@Override
	public float getFilledPercentage(World world, int x, int y, int z)
	{
		return 1;
	}

	@Override
	public IIcon getIcon(int side, int meta)
	{
		return side != 0 && side != 1 ? this.theIcon[1] : this.theIcon[0];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister)
	{
		IIcon still = iconRegister.registerIcon("railcraft:fluids/" + this.fluidName + "_still");
		IIcon flowing = still;
		if (this.hasFlowIcon)
			flowing = iconRegister.registerIcon("railcraft:fluids/" + this.fluidName + "_flow");
		this.theIcon = new IIcon[] { still, flowing };
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
	{
		super.onNeighborBlockChange(world, x, y, z, block);
		if (this.flammable && world.provider.dimensionId == -1)
		{
			// TODO gamerforEA use ExplosionByPlayer
			ExplosionByPlayer.newExplosion(null, world, null, x, y, z, 4F, true, true);
			world.setBlockToAir(x, y, z);
		}
	}

	public BlockRailcraftFluid setFlammable(boolean flammable)
	{
		this.flammable = flammable;
		return this;
	}

	public BlockRailcraftFluid setFlammability(int flammability)
	{
		this.flammability = flammability;
		return this;
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face)
	{
		return this.flammable ? 300 : 0;
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face)
	{
		return this.flammability;
	}

	@Override
	public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face)
	{
		return this.flammable;
	}

	@Override
	public boolean isFireSource(World world, int x, int y, int z, ForgeDirection side)
	{
		return this.flammable && this.flammability == 0;
	}

	public BlockRailcraftFluid setParticleColor(float particleRed, float particleGreen, float particleBlue)
	{
		this.particleRed = particleRed;
		this.particleGreen = particleGreen;
		this.particleBlue = particleBlue;
		return this;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random rand)
	{
		super.randomDisplayTick(world, x, y, z, rand);

		if (rand.nextInt(10) == 0 && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z) && !world.getBlock(x, y - 2, z).getMaterial().blocksMovement())
		{
			double px = x + rand.nextFloat();
			double py = y - 1.05D;
			double pz = z + rand.nextFloat();

			EntityFX fx = new EntityDropParticleFX(world, px, py, pz, this.particleRed, this.particleGreen, this.particleBlue);
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
		}
	}
}
