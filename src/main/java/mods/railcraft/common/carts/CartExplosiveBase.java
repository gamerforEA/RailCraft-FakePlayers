/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.carts;

import com.gamerforea.railcraft.ExplosionByPlayer;
import mods.railcraft.api.carts.CartTools;
import mods.railcraft.api.carts.IExplosiveCart;
import mods.railcraft.common.gui.EnumGui;
import mods.railcraft.common.gui.GuiHandler;
import mods.railcraft.common.items.firestone.ItemFirestoneRefined;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.network.IGuiReturnHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class CartExplosiveBase extends CartBase implements IExplosiveCart, IGuiReturnHandler
{
	private static final byte FUSE_DATA_ID = 25;
	private static final byte BLAST_DATA_ID = 26;
	private static final byte PRIMED_DATA_ID = 27;
	private static final float BLAST_RADIUS_BYTE_MULTIPLIER = 0.5f;
	private static final float BLAST_RADIUS_MIN = 2;
	private static final float BLAST_RADIUS_MAX = 6;
	private static final float BLAST_RADIUS_MAX_BONUS = 5;
	public static final short MAX_FUSE = 500;
	public static final short MIN_FUSE = 0;
	private boolean isExploding;

	public CartExplosiveBase(World world)
	{
		super(world);

	}

	public CartExplosiveBase(World world, double x, double y, double z)
	{
		super(world, x, y, z);
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();

		this.dataWatcher.addObject(FUSE_DATA_ID, Short.valueOf((short) 80));
		this.dataWatcher.addObject(BLAST_DATA_ID, Byte.valueOf((byte) 8));
		this.dataWatcher.addObject(PRIMED_DATA_ID, Byte.valueOf((byte) 0));
	}

	@Override
	public Block func_145820_n()
	{
		return Blocks.tnt;
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if (this.isCollidedHorizontally)
		{
			double d0 = this.motionX * this.motionX + this.motionZ * this.motionZ;

			if (d0 >= 0.01)
				this.explode(this.getBlastRadiusWithSpeedModifier());
		}

		if (this.isPrimed())
		{
			this.setFuse((short) (this.getFuse() - 1));
			if (this.getFuse() <= 0)
				this.explode();
			else
				this.worldObj.spawnParticle("smoke", this.posX, this.posY + 0.8D, this.posZ, 0.0D, 0.0D, 0.0D);
		}
	}

	@Override
	public void explode()
	{
		this.explode(this.getBlastRadius());
	}

	protected void explode(float blastRadius)
	{
		this.isExploding = true;
		if (Game.isHost(this.getWorld()))
		{
			// TODO gamerforEA use ExplosionByPlayer
			ExplosionByPlayer.createExplosion(this.fake.get(), this.worldObj, this, this.posX, this.posY, this.posZ, blastRadius, true);

			this.setDead();
		}
	}

	@Override
	public void killMinecart(DamageSource damageSource)
	{
		if (this.isDead || this.isExploding)
			return;
		double speedSq = this.motionX * this.motionX + this.motionZ * this.motionZ;
		if (damageSource.isFireDamage() || damageSource.isExplosion() || speedSq >= 0.01D)
			this.explode(this.getBlastRadiusWithSpeedModifier());
		else
			super.killMinecart(damageSource);
	}

	protected float getBlastRadiusWithSpeedModifier()
	{
		double blast = Math.min(CartTools.getCartSpeedUncapped(this), this.getMaxBlastRadiusBonus());
		return (float) (this.getBlastRadius() + this.rand.nextDouble() * 1.5 * blast);
	}

	protected float getBlastRadiusWithFallModifier(float distance)
	{
		double blast = Math.min(distance / 10.0, this.getMaxBlastRadiusBonus());
		return (float) (this.getBlastRadius() + this.rand.nextDouble() * 1.5 * blast);
	}

	@Override
	public void onActivatorRailPass(int x, int y, int z, boolean powered)
	{
		this.setPrimed(powered);
	}

	@Override
	protected void fall(float distance)
	{
		if (distance >= 3.0F)
			this.explode(this.getBlastRadiusWithFallModifier(distance));

		super.fall(distance);
	}

	@Override
	public boolean doInteract(EntityPlayer player)
	{
		ItemStack stack = player.inventory.getCurrentItem();
		if (stack != null)
			if (stack.getItem() == Items.flint_and_steel || stack.getItem() instanceof ItemFirestoneRefined)
			{
				this.setPrimed(true);
				stack.damageItem(1, player);
			}
			else if (stack.getItem() == Items.string)
			{
				player.inventory.decrStackSize(player.inventory.currentItem, 1);
				GuiHandler.openGui(EnumGui.CART_TNT_FUSE, player, this.worldObj, this);
			}
			else if (stack.getItem() == Items.gunpowder)
			{
				player.inventory.decrStackSize(player.inventory.currentItem, 1);
				GuiHandler.openGui(EnumGui.CART_TNT_BLAST, player, this.worldObj, this);
			}
		return true;
	}

	@Override
	public boolean canBeRidden()
	{
		return false;
	}

	@Override
	public boolean isPrimed()
	{
		return this.dataWatcher.getWatchableObjectByte(PRIMED_DATA_ID) != 0;
	}

	@Override
	public void setPrimed(boolean primed)
	{
		if (Game.isHost(this.worldObj) && this.isPrimed() != primed)
		{
			if (primed)
				this.worldObj.playSoundAtEntity(this, "random.fuse", 1.0F, 1.0F);
			this.dataWatcher.updateObject(PRIMED_DATA_ID, primed ? Byte.valueOf((byte) 1) : Byte.valueOf((byte) 0));
		}
	}

	@Override
	public int getFuse()
	{
		return this.dataWatcher.getWatchableObjectShort(FUSE_DATA_ID);
	}

	@Override
	public void setFuse(int f)
	{
		f = (short) Math.max(f, MIN_FUSE);
		f = (short) Math.min(f, MAX_FUSE);
		this.dataWatcher.updateObject(FUSE_DATA_ID, (short) f);
	}

	protected float getMinBlastRadius()
	{
		return BLAST_RADIUS_MIN;
	}

	protected float getMaxBlastRadius()
	{
		return BLAST_RADIUS_MAX;
	}

	protected float getMaxBlastRadiusBonus()
	{
		return BLAST_RADIUS_MAX_BONUS;
	}

	@Override
	public float getBlastRadius()
	{
		return this.dataWatcher.getWatchableObjectByte(BLAST_DATA_ID) * BLAST_RADIUS_BYTE_MULTIPLIER;
	}

	@Override
	public void setBlastRadius(float b)
	{
		b = Math.max(b, this.getMinBlastRadius());
		b = Math.min(b, this.getMaxBlastRadius());
		b /= BLAST_RADIUS_BYTE_MULTIPLIER;
		this.dataWatcher.updateObject(BLAST_DATA_ID, Byte.valueOf((byte) b));
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound data)
	{
		super.writeEntityToNBT(data);
		data.setShort("Fuse", (short) this.getFuse());
		data.setByte("blastRadius", this.dataWatcher.getWatchableObjectByte(BLAST_DATA_ID));
		data.setBoolean("Primed", this.isPrimed());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound data)
	{
		super.readEntityFromNBT(data);
		this.setFuse(data.getShort("Fuse"));
		this.setBlastRadius(data.getByte("blastRadius"));
		this.setPrimed(data.getBoolean("Primed"));
	}

	@Override
	public void writeGuiData(DataOutputStream data) throws IOException
	{
		data.writeShort(this.getFuse());
		data.writeByte(this.dataWatcher.getWatchableObjectByte(BLAST_DATA_ID));
	}

	@Override
	public void readGuiData(DataInputStream data, EntityPlayer sender) throws IOException
	{
		this.setFuse(data.readShort());
		this.setBlastRadius(data.readByte());
	}

	@Override
	public World getWorld()
	{
		return this.worldObj;
	}

	@Override
	public double getDrag()
	{
		return CartConstants.STANDARD_DRAG;
	}
}
