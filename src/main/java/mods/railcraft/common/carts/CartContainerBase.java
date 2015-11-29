/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.carts;

import java.util.List;

import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.eventhelper.fake.FakePlayerContainerEntity;
import com.gamerforea.railcraft.ModUtils;

import mods.railcraft.api.carts.IItemCart;
import mods.railcraft.common.blocks.tracks.EnumTrackMeta;
import mods.railcraft.common.plugins.forge.LocalizationPlugin;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;

/**
 * It also contains some generic code that most carts will find useful.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public abstract class CartContainerBase extends EntityMinecartContainer implements IRailcraftCart, IItemCart
{
	private final ForgeDirection[] travelDirectionHistory = new ForgeDirection[2];
	protected ForgeDirection travelDirection = ForgeDirection.UNKNOWN;
	protected ForgeDirection verticalTravelDirection = ForgeDirection.UNKNOWN;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = new FakePlayerContainerEntity(ModUtils.profile, this);

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt)
	{
		super.writeEntityToNBT(nbt);
		this.fake.writeToNBT(nbt);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt)
	{
		super.readEntityFromNBT(nbt);
		this.fake.readFromNBT(nbt);
	}
	// TODO gamerforEA code end

	public CartContainerBase(World world)
	{
		super(world);
		this.renderDistanceWeight = CartConstants.RENDER_DIST_MULTIPLIER;
	}

	public CartContainerBase(World world, double x, double y, double z)
	{
		super(world, x, y, z);
		this.renderDistanceWeight = CartConstants.RENDER_DIST_MULTIPLIER;
	}

	public abstract ICartType getCartType();

	@Override
	public void initEntityFromItem(ItemStack stack)
	{
	}

	@Override
	public final boolean interactFirst(EntityPlayer player)
	{
		if (MinecraftForge.EVENT_BUS.post(new MinecartInteractEvent(this, player)))
			return true;
		return this.doInteract(player);
	}

	public boolean doInteract(EntityPlayer player)
	{
		return true;
	}

	public double getDrag()
	{
		return CartConstants.STANDARD_DRAG;
	}

	@Override
	public ItemStack getCartItem()
	{
		ItemStack stack = EnumCart.fromCart(this).getCartItem();
		if (this.hasCustomInventoryName())
			stack.setStackDisplayName(this.getCommandSenderName());
		return stack;
	}

	public abstract List<ItemStack> getItemsDropped();

	@Override
	public String getInventoryName()
	{
		return LocalizationPlugin.translate(this.getCartType().getTag());
	}

	@Override
	public void setDead()
	{
		if (Game.isNotHost(this.worldObj))
			for (int slot = 0; slot < this.getSizeInventory(); slot++)
				this.setInventorySlotContents(slot, null);
		super.setDead();
	}

	@Override
	public void killMinecart(DamageSource par1DamageSource)
	{
		this.setDead();
		List<ItemStack> drops = this.getItemsDropped();
		if (this.func_95999_t() != null)
			drops.get(0).setStackDisplayName(this.func_95999_t());
		for (ItemStack item : drops)
			this.entityDropItem(item, 0.0F);
	}

	@Override
	public int getMinecartType()
	{
		return -1;
	}

	protected void updateTravelDirection(int trackX, int trackY, int trackZ, int meta)
	{
		EnumTrackMeta trackMeta = EnumTrackMeta.fromMeta(meta);
		if (trackMeta != null)
		{
			ForgeDirection forgeDirection = this.determineTravelDirection(trackMeta);
			ForgeDirection previousForgeDirection = this.travelDirectionHistory[1];
			if (previousForgeDirection != ForgeDirection.UNKNOWN && this.travelDirectionHistory[0] == previousForgeDirection)
			{
				this.travelDirection = forgeDirection;
				this.verticalTravelDirection = this.determineVerticalTravelDirection(trackMeta);
			}
			this.travelDirectionHistory[0] = previousForgeDirection;
			this.travelDirectionHistory[1] = forgeDirection;
		}
	}

	private ForgeDirection determineTravelDirection(EnumTrackMeta trackMeta)
	{
		if (trackMeta.isStraightTrack())
		{
			if (this.posX - this.prevPosX > 0)
				return ForgeDirection.EAST;
			if (this.posX - this.prevPosX < 0)
				return ForgeDirection.WEST;
			if (this.posZ - this.prevPosZ > 0)
				return ForgeDirection.SOUTH;
			if (this.posZ - this.prevPosZ < 0)
				return ForgeDirection.NORTH;
		}
		else
			switch (trackMeta)
			{
				case EAST_SOUTH_CORNER:
					if (this.prevPosZ > this.posZ)
						return ForgeDirection.EAST;
					else
						return ForgeDirection.SOUTH;
				case WEST_SOUTH_CORNER:
					if (this.prevPosZ > this.posZ)
						return ForgeDirection.WEST;
					else
						return ForgeDirection.SOUTH;
				case WEST_NORTH_CORNER:
					if (this.prevPosZ > this.posZ)
						return ForgeDirection.NORTH;
					else
						return ForgeDirection.WEST;
				case EAST_NORTH_CORNER:
					if (this.prevPosZ > this.posZ)
						return ForgeDirection.NORTH;
					else
						return ForgeDirection.EAST;
			}
		return ForgeDirection.UNKNOWN;
	}

	private ForgeDirection determineVerticalTravelDirection(EnumTrackMeta trackMeta)
	{
		if (trackMeta.isSlopeTrack())
			return this.prevPosY < this.posY ? ForgeDirection.UP : ForgeDirection.DOWN;
		return ForgeDirection.UNKNOWN;
	}

	@Override
	public boolean canPassItemRequests()
	{
		return false;
	}

	@Override
	public boolean canAcceptPushedItem(EntityMinecart requester, ItemStack stack)
	{
		return false;
	}

	@Override
	public boolean canProvidePulledItem(EntityMinecart requester, ItemStack stack)
	{
		return false;
	}
}
