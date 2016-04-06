/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.carts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gamerforea.eventhelper.util.EventUtils;

import mods.railcraft.api.carts.CartTools;
import mods.railcraft.common.blocks.aesthetics.post.ItemPost;
import mods.railcraft.common.blocks.tracks.EnumTrackMeta;
import mods.railcraft.common.blocks.tracks.TrackTools;
import mods.railcraft.common.gui.EnumGui;
import mods.railcraft.common.gui.GuiHandler;
import mods.railcraft.common.plugins.forge.LocalizationPlugin;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.sounds.SoundHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class EntityCartUndercutter extends CartMaintenancePatternBase
{
	public static final Set<Block> EXCLUDED_BLOCKS = new HashSet<Block>();
	private static final int SLOT_EXIST_UNDER_A = 0;
	private static final int SLOT_EXIST_UNDER_B = 1;
	private static final int SLOT_EXIST_SIDE_A = 2;
	private static final int SLOT_EXIST_SIDE_B = 3;
	private static final int SLOT_REPLACE_UNDER = 4;
	private static final int SLOT_REPLACE_SIDE = 5;
	public static final int SLOT_STOCK_UNDER = 0;
	public static final int SLOT_STOCK_SIDE = 1;
	private static final int[] SLOTS = InvTools.buildSlotArray(0, 2);

	static
	{
		EXCLUDED_BLOCKS.add(Blocks.sand);
	}

	public static boolean isValidBallast(ItemStack stack)
	{
		if (stack == null)
			return false;
		Block block = InvTools.getBlockFromStack(stack);
		if (block == null)
			return false;
		if (EntityCartUndercutter.EXCLUDED_BLOCKS.contains(block))
			return false;
		if (block.isOpaqueCube())
			return true;
		if (stack.getItem() instanceof ItemPost)
			return true;
		return false;
	}

	public EntityCartUndercutter(World world)
	{
		super(world);
	}

	public EntityCartUndercutter(World world, double d, double d1, double d2)
	{
		this(world);
		this.setPosition(d, d1 + this.yOffset, d2);
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.prevPosX = d;
		this.prevPosY = d1;
		this.prevPosZ = d2;
	}

	@Override
	public ICartType getCartType()
	{
		return EnumCart.UNDERCUTTER;
	}

	@Override
	public int getSizeInventory()
	{
		return 2;
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();
		if (Game.isNotHost(this.worldObj))
			return;

		this.stockItems(SLOT_REPLACE_UNDER, SLOT_STOCK_UNDER);
		this.stockItems(SLOT_REPLACE_SIDE, SLOT_STOCK_SIDE);

		int x = MathHelper.floor_double(this.posX);
		int y = MathHelper.floor_double(this.posY);
		int z = MathHelper.floor_double(this.posZ);
		if (TrackTools.isRailBlockAt(this.worldObj, x, y - 1, z))
			--y;

		Block block = this.worldObj.getBlock(x, y, z);

		if (TrackTools.isRailBlock(block))
		{
			EnumTrackMeta trackMeta = EnumTrackMeta.fromMeta(((BlockRailBase) block).getBasicRailMetadata(this.worldObj, this, x, y, z));
			y--;

			boolean slotANull = true;
			boolean slotBNull = true;
			if (this.patternInv.getStackInSlot(SLOT_EXIST_UNDER_A) != null)
			{
				this.replaceUnder(x, y, z, SLOT_EXIST_UNDER_A);
				slotANull = false;
			}
			if (this.patternInv.getStackInSlot(SLOT_EXIST_UNDER_B) != null)
			{
				this.replaceUnder(x, y, z, SLOT_EXIST_UNDER_B);
				slotBNull = false;
			}

			if (slotANull && slotBNull)
				this.replaceUnder(x, y, z, SLOT_EXIST_UNDER_A);

			slotANull = true;
			slotBNull = true;
			if (this.patternInv.getStackInSlot(SLOT_EXIST_SIDE_A) != null)
			{
				this.replaceSide(x, y, z, SLOT_EXIST_SIDE_A, trackMeta);
				slotANull = false;
			}
			if (this.patternInv.getStackInSlot(SLOT_EXIST_SIDE_B) != null)
			{
				this.replaceSide(x, y, z, SLOT_EXIST_SIDE_B, trackMeta);
				slotBNull = false;
			}

			if (slotANull && slotBNull)
				this.replaceSide(x, y, z, SLOT_EXIST_SIDE_A, trackMeta);
		}
	}

	private void replaceUnder(int x, int y, int z, int slotExist)
	{
		this.replaceWith(x, y, z, slotExist, SLOT_STOCK_UNDER);
	}

	private void replaceSide(int x, int y, int z, int slotExist, EnumTrackMeta trackMeta)
	{
		if (trackMeta.isEastWestTrack())
		{
			this.replaceWith(x, y, z + 1, slotExist, SLOT_STOCK_SIDE);
			this.replaceWith(x, y, z - 1, slotExist, SLOT_STOCK_SIDE);
		}
		else if (trackMeta.isNorthSouthTrack())
		{
			this.replaceWith(x + 1, y, z, slotExist, SLOT_STOCK_SIDE);
			this.replaceWith(x - 1, y, z, slotExist, SLOT_STOCK_SIDE);
		}
	}

	private void replaceWith(int x, int y, int z, int slotExist, int slotStock)
	{
		ItemStack exist = this.patternInv.getStackInSlot(slotExist);
		ItemStack stock = this.getStackInSlot(slotStock);

		if (!isValidBallast(stock))
			return;

		Block blockToReplace = this.worldObj.getBlock(x, y, z);
		int oldMeta = this.worldObj.getBlockMetadata(x, y, z);

		if (blockToReplace == null || !this.blockMatches(blockToReplace, oldMeta, exist))
			return;

		if (this.safeToReplace(x, y, z))
		{
			// TODO gamerforEA code start
			if (EventUtils.cantBreak(this.fake.getPlayer(), x, y, z))
				return;
			// TODO gamerforEA code end

			Block stockBlock = InvTools.getBlockFromStack(stock);
			List<ItemStack> drops = blockToReplace.getDrops(this.worldObj, x, y, z, oldMeta, 0);
			ItemBlock item = (ItemBlock) stock.getItem();
			int newMeta = 0;
			if (item.getHasSubtypes())
				newMeta = item.getMetadata(stock.getItemDamage());
			if (this.worldObj.setBlock(x, y, z, stockBlock, newMeta, 3))
			{
				SoundHelper.playBlockSound(this.worldObj, x, y, z, stockBlock.stepSound.func_150496_b(), (1f + 1.0F) / 2.0F, 1f * 0.8F, stockBlock, newMeta);
				this.decrStackSize(slotStock, 1);
				for (ItemStack stack : drops)
					CartTools.offerOrDropItem(this, stack);
				this.blink();
			}
		}
	}

	private boolean blockMatches(Block block, int meta, ItemStack stack)
	{
		if (stack == null)
			return true;

		if (stack.getItem() instanceof ItemBlock)
		{
			ItemBlock existItem = (ItemBlock) stack.getItem();
			int existMeta = OreDictionary.WILDCARD_VALUE;
			if (existItem.getHasSubtypes())
				existMeta = existItem.getMetadata(stack.getItemDamage());
			Block stackBlock = InvTools.getBlockFromStack(stack);
			return stackBlock == block && (existMeta == OreDictionary.WILDCARD_VALUE || meta == existMeta) || stackBlock == Blocks.dirt && stackBlock == Blocks.grass;
		}
		return false;
	}

	private boolean safeToReplace(int x, int y, int z)
	{
		if (this.worldObj.isAirBlock(x, y, z))
			return false;

		Block block = this.worldObj.getBlock(x, y, z);

		if (block.getMaterial().isLiquid())
			return false;

		if (block.getBlockHardness(this.worldObj, x, y, z) < 0)
			return false;

		return !block.isReplaceable(this.worldObj, x, y, z);
	}

	@Override
	public boolean doInteract(EntityPlayer player)
	{
		if (Game.isHost(this.worldObj))
			GuiHandler.openGui(EnumGui.CART_UNDERCUTTER, player, this.worldObj, this);
		return true;
	}

	@Override
	public String getInventoryName()
	{
		return LocalizationPlugin.translate(EnumCart.UNDERCUTTER.getTag());
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		return SLOTS;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack)
	{
		if (slot == SLOT_REPLACE_UNDER)
		{
			ItemStack trackReplace = this.patternInv.getStackInSlot(SLOT_REPLACE_UNDER);
			return InvTools.isItemEqual(stack, trackReplace);
		}
		if (slot == SLOT_REPLACE_SIDE)
		{
			ItemStack trackReplace = this.patternInv.getStackInSlot(SLOT_REPLACE_SIDE);
			return InvTools.isItemEqual(stack, trackReplace);
		}
		return false;
	}

}
