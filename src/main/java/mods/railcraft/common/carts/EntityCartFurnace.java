/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.carts;

import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.eventhelper.fake.FakePlayerContainerEntity;
import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.railcraft.ModUtils;
import cpw.mods.fml.relauncher.ReflectionHelper;
import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.plugins.forge.FuelPlugin;
import mods.railcraft.common.util.inventory.InvTools;
import net.minecraft.entity.item.EntityMinecartFurnace;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class EntityCartFurnace extends EntityMinecartFurnace
{
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

	public EntityCartFurnace(World world)
	{
		super(world);
	}

	public EntityCartFurnace(World world, double x, double y, double z)
	{
		super(world, x, y, z);
	}

	public List<ItemStack> getItemsDropped()
	{
		List<ItemStack> items = new ArrayList<ItemStack>();
		if (RailcraftConfig.doCartsBreakOnDrop())
		{
			items.add(new ItemStack(Items.minecart));
			items.add(new ItemStack(Blocks.furnace));
		}
		else
			items.add(this.getCartItem());
		return items;
	}

	@Override
	public void killMinecart(DamageSource par1DamageSource)
	{
		this.setDead();
		List<ItemStack> drops = this.getItemsDropped();
		if (this.func_95999_t() != null)
			drops.get(0).setStackDisplayName(this.func_95999_t());
		for (ItemStack item : drops)
		{
			this.entityDropItem(item, 0.0F);
		}
	}

	@Override
	public ItemStack getCartItem()
	{
		ItemStack stack = new ItemStack(Items.furnace_minecart);
		if (this.hasCustomInventoryName())
			stack.setStackDisplayName(this.getCommandSenderName());
		return stack;
	}

	public double getDrag()
	{
		return DRAG_FACTOR;
	}

	@Override
	public boolean interactFirst(EntityPlayer player)
	{
		// TODO gamerforEA code start
		if (EventUtils.cantDamage(player, this))
			return false;
		// TODO gamerforEA code end

		Integer fuel = ReflectionHelper.getPrivateValue(EntityMinecartFurnace.class, this, 0);
		if (fuel <= 0)
		{
			ItemStack stack = player.inventory.getCurrentItem();
			if (stack != null)
			{
				int burnTime = FuelPlugin.getBurnTime(stack);

				if (burnTime > 0)
				{
					if (!player.capabilities.isCreativeMode)
						player.inventory.setInventorySlotContents(player.inventory.currentItem, InvTools.depleteItem(stack));
					fuel += burnTime;
					ReflectionHelper.setPrivateValue(EntityMinecartFurnace.class, this, fuel, 0);

					this.pushX = this.posX - player.posX;
					this.pushZ = this.posZ - player.posZ;
				}
			}
		}

		return true;
	}

	private static final double DRAG_FACTOR = 0.99;
	private static final double PUSH_FACTOR = 0.1D;
}
