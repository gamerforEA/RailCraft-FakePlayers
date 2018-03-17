/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.carts;

import mods.railcraft.api.carts.CartTools;
import mods.railcraft.api.tracks.RailTools;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public abstract class CartMaintenanceBase extends CartContainerBase
{
	protected static final double DRAG_FACTOR = 0.9;
	protected static final float MAX_SPEED = 0.1f;
	private static final int BLINK_DURATION = 3;
	private static final int DATA_ID_BLINK = 25;

	public CartMaintenanceBase(World world)
	{
		super(world);
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();
		this.dataWatcher.addObject(DATA_ID_BLINK, (byte) 0);
	}

	@Override
	public int getSizeInventory()
	{
		return 0;
	}

	protected void blink()
	{
		this.dataWatcher.updateObject(DATA_ID_BLINK, (byte) BLINK_DURATION);
	}

	protected void setBlink(byte blink)
	{
		this.dataWatcher.updateObject(DATA_ID_BLINK, blink);
	}

	protected byte getBlink()
	{
		return this.dataWatcher.getWatchableObjectByte(DATA_ID_BLINK);
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();
		if (Game.isNotHost(this.worldObj))
			return;

		if (this.isBlinking())
			this.setBlink((byte) (this.getBlink() - 1));
	}

	@Override
	public List<ItemStack> getItemsDropped()
	{
		List<ItemStack> items = new ArrayList<ItemStack>();
		items.add(this.getCartItem());
		return items;
	}

	@Override
	public boolean canBeRidden()
	{
		return false;
	}

	@Override
	public double getDrag()
	{
		return CartMaintenanceBase.DRAG_FACTOR;
	}

	@Override
	public float getMaxCartSpeedOnRail()
	{
		return MAX_SPEED;
	}

	public boolean isBlinking()
	{
		return this.dataWatcher.getWatchableObjectByte(DATA_ID_BLINK) > 0;
	}

	protected boolean placeNewTrack(int x, int y, int z, int slotStock, int meta)
	{
		ItemStack trackStock = this.getStackInSlot(slotStock);
		if (trackStock != null)
		{
			// TODO gamerforEA code start
			if (this.fake.cantBreak(x, y, z))
				return false;
			// TODO gamerforEA code end

			if (RailTools.placeRailAt(trackStock, this.worldObj, x, y, z))
			{
				this.worldObj.setBlockMetadataWithNotify(x, y, z, meta, 0x02);
				Block block = this.worldObj.getBlock(x, y, z);
				block.onNeighborBlockChange(this.worldObj, x, y, z, block);
				this.worldObj.markBlockForUpdate(x, y, z);
				this.decrStackSize(slotStock, 1);
				this.blink();
				return true;
			}
		}
		return false;
	}

	protected int removeOldTrack(int x, int y, int z, Block block)
	{
		// TODO gamerforEA code start
		if (this.fake.cantBreak(x, y, z))
			return this.worldObj.getBlockMetadata(x, y, z);
		// TODO gamerforEA code end

		List<ItemStack> drops = block.getDrops(this.worldObj, x, y, z, 0, 0);

		for (ItemStack stack : drops)
		{
			CartTools.offerOrDropItem(this, stack);
		}
		int meta = this.worldObj.getBlockMetadata(x, y, z);
		if (((BlockRailBase) block).isPowered())
			meta = meta & 7;
		this.worldObj.setBlockToAir(x, y, z);
		return meta;
	}

}
