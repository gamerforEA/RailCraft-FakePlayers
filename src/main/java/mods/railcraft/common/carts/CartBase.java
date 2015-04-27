/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.carts;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;

import com.gamerforea.railcraft.FakePlayerUtils;
import com.google.common.base.Strings;

/**
 *
 * It also contains some generic code that most carts will find useful.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public abstract class CartBase extends EntityMinecart implements IRailcraftCart
{
	// TODO gamerforEA code start
	public UUID ownerUUID;
	public String ownerName;
	private FakePlayer ownerFake;

	public FakePlayer getOwnerFake()
	{
		FakePlayer fake = null;
		if (this.ownerFake != null) fake = this.ownerFake;
		else if (this.ownerUUID != null && !Strings.isNullOrEmpty(this.ownerName)) fake = this.ownerFake = FakePlayerUtils.createFakePlayer(this.ownerUUID, this.ownerName, this.worldObj);
		else fake = FakePlayerUtils.getPlayer(this.worldObj);
		return fake;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt)
	{
		super.writeEntityToNBT(nbt);
		if (this.ownerUUID != null) nbt.setString("ownerUUID", this.ownerUUID.toString());
		if (!Strings.isNullOrEmpty(this.ownerName)) nbt.setString("ownerName", this.ownerName);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt)
	{
		super.readEntityFromNBT(nbt);
		String s = nbt.getString("ownerUUID");
		if (!Strings.isNullOrEmpty(s)) this.ownerUUID = UUID.fromString(s);
		s = nbt.getString("ownerName");
		if (!Strings.isNullOrEmpty(s)) this.ownerName = s;
	}
	// TODO gamerforEA code end

	public CartBase(World world)
	{
		super(world);
		renderDistanceWeight = CartConstants.RENDER_DIST_MULTIPLIER;
	}

	public CartBase(World world, double x, double y, double z)
	{
		super(world, x, y, z);
		renderDistanceWeight = CartConstants.RENDER_DIST_MULTIPLIER;
	}

	@Override
	public void initEntityFromItem(ItemStack stack)
	{
	}

	@Override
	public final boolean interactFirst(EntityPlayer player)
	{
		if (MinecraftForge.EVENT_BUS.post(new MinecartInteractEvent(this, player))) return true;
		return doInteract(player);
	}

	public boolean doInteract(EntityPlayer player)
	{
		return true;
	}

	public abstract double getDrag();

	@Override
	public ItemStack getCartItem()
	{
		ItemStack stack = EnumCart.fromCart(this).getCartItem();
		if (hasCustomInventoryName()) stack.setStackDisplayName(getCommandSenderName());
		return stack;
	}

	public List<ItemStack> getItemsDropped()
	{
		List<ItemStack> items = new ArrayList<ItemStack>();
		items.add(getCartItem());
		return items;
	}

	@Override
	public void killMinecart(DamageSource par1DamageSource)
	{
		setDead();
		List<ItemStack> drops = getItemsDropped();
		if (this.func_95999_t() != null) drops.get(0).setStackDisplayName(this.func_95999_t());
		for (ItemStack item : drops)
		{
			entityDropItem(item, 0.0F);
		}
	}

	@Override
	public int getMinecartType()
	{
		return -1;
	}
}