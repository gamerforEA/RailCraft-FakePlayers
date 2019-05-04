/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.blocks.machine.alpha;

import com.gamerforea.railcraft.EventConfig;
import com.google.common.collect.MapMaker;
import mods.railcraft.api.core.WorldCoordinate;
import mods.railcraft.api.core.items.IToolCrowbar;
import mods.railcraft.common.blocks.RailcraftTileEntity;
import mods.railcraft.common.blocks.machine.IEnumMachine;
import mods.railcraft.common.blocks.machine.TileMachineItem;
import mods.railcraft.common.blocks.machine.beta.TileSentinel;
import mods.railcraft.common.carts.ItemCartAnchor;
import mods.railcraft.common.core.Railcraft;
import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.core.RailcraftConstants;
import mods.railcraft.common.gui.EnumGui;
import mods.railcraft.common.gui.GuiHandler;
import mods.railcraft.common.plugins.forge.ChatPlugin;
import mods.railcraft.common.plugins.forge.PowerPlugin;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.util.collections.ItemMap;
import mods.railcraft.common.util.effects.EffectManager;
import mods.railcraft.common.util.misc.ChunkManager;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.misc.IAnchor;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import org.apache.logging.log4j.Level;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class TileAnchorWorld extends TileMachineItem implements IAnchor, ISidedInventory
{

	private static final Map<UUID, Ticket> tickets = new MapMaker().makeMap();
	private static final Map<EntityPlayer, WorldCoordinate> sentinelPairingMap = new MapMaker().weakKeys().makeMap();
	private static final int SENTINEL_CHECK = 128;
	private static final byte MAX_CHUNKS = 25;
	private static final byte FUEL_CYCLE = 9;
	private static final byte ANCHOR_RADIUS = 1;
	private static final int[] SLOTS = { 0 };
	private static final int[] SLOTS_NO_ACCESS = {};
	private int xSentinel = -1;
	private int ySentinel = -1;
	private int zSentinel = -1;
	private int prevX, prevY, prevZ;
	private Set<ChunkCoordIntPair> chunks;
	private long fuel;
	private int fuelCycle;
	private boolean hasTicket;
	private boolean refreshTicket;
	private boolean powered;

	public TileAnchorWorld()
	{
		super(1);
	}

	@Override
	public int getSizeInventory()
	{
		return this.needsFuel() ? 1 : 0;
	}

	@Override
	public IEnumMachine getMachineType()
	{
		return EnumMachineAlpha.WORLD_ANCHOR;
	}

	@Override
	public IIcon getIcon(int side)
	{
		if (!this.hasTicket && side < 2)
			return this.getMachineType().getTexture(6);
		return this.getMachineType().getTexture(side);
	}

	@Override
	public boolean blockActivated(EntityPlayer player, int side)
	{
		ItemStack current = player.getCurrentEquippedItem();
		if (current != null && current.getItem() instanceof IToolCrowbar)
		{
			IToolCrowbar crowbar = (IToolCrowbar) current.getItem();
			if (crowbar.canWhack(player, current, this.xCoord, this.yCoord, this.zCoord))
			{
				if (Game.isHost(this.worldObj))
				{
					WorldCoordinate target = sentinelPairingMap.get(player);
					if (target == null)
						setTarget(this, player);
					else if (this.worldObj.provider.dimensionId != target.dimension)
						ChatPlugin.sendLocalizedChatFromServer(player, "railcraft.gui.anchor.pair.fail.dimension", this.getLocalizationTag());
					else if (new WorldCoordinate(this).equals(target))
					{
						removeTarget(player);
						ChatPlugin.sendLocalizedChatFromServer(player, "railcraft.gui.anchor.pair.cancel", this.getLocalizationTag());
					}
					else
						this.setSentinel(player, target);
					crowbar.onWhack(player, current, this.xCoord, this.yCoord, this.zCoord);
				}
				return true;
			}
		}
		return super.blockActivated(player, side);
	}

	public static WorldCoordinate getTarget(EntityPlayer player)
	{
		return sentinelPairingMap.get(player);
	}

	public static void setTarget(RailcraftTileEntity tile, EntityPlayer player)
	{
		sentinelPairingMap.put(player, new WorldCoordinate(tile));
		ChatPlugin.sendLocalizedChatFromServer(player, "railcraft.gui.anchor.pair.start", tile.getLocalizationTag());
	}

	public static void removeTarget(EntityPlayer player)
	{
		sentinelPairingMap.remove(player);
	}

	@Override
	public boolean openGui(EntityPlayer player)
	{
		if (this.needsFuel())
		{
			GuiHandler.openGui(EnumGui.WORLD_ANCHOR, player, this.worldObj, this.xCoord, this.yCoord, this.zCoord);
			return true;
		}
		return false;
	}

	public int getMaxSentinelChunks()
	{
		Ticket ticket = this.getTicket();

		/* TODO gamerforEA code replace, old code:
		if (ticket == null)
			return MAX_CHUNKS;
		return Math.min(ticket.getMaxChunkListDepth(), MAX_CHUNKS); */
		if (ticket == null)
			return EventConfig.worldAnchorMaxSentinelChunks;
		return Math.min(ticket.getMaxChunkListDepth(), EventConfig.worldAnchorMaxSentinelChunks);
		// TODO gamerforEA code end
	}

	public static TileEntity getTargetAt(EntityPlayer player, RailcraftTileEntity searcher, WorldCoordinate coord)
	{
		if (!WorldPlugin.blockExists(searcher.getWorldObj(), coord.x, coord.y, coord.z))
		{
			ChatPlugin.sendLocalizedChatFromServer(player, "railcraft.gui.anchor.pair.fail.unloaded", searcher.getLocalizationTag());
			return null;
		}
		return WorldPlugin.getBlockTile(searcher.getWorldObj(), coord.x, coord.y, coord.z);
	}

	public boolean setSentinel(EntityPlayer player, WorldCoordinate coord)
	{
		TileEntity tile = getTargetAt(player, this, coord);
		if (tile == null)
			return false;

		if (tile instanceof TileSentinel)
		{
			int xChunk = this.xCoord >> 4;
			int zChunk = this.zCoord >> 4;

			int xSentinelChunk = tile.xCoord >> 4;
			int zSentinelChunk = tile.zCoord >> 4;

			if (xChunk != xSentinelChunk && zChunk != zSentinelChunk)
			{
				ChatPlugin.sendLocalizedChatFromServer(player, "railcraft.gui.anchor.pair.fail.alignment", this.getLocalizationTag(), ((TileSentinel) tile).getLocalizationTag());
				return false;
			}

			int max = this.getMaxSentinelChunks();
			if (Math.abs(xChunk - xSentinelChunk) >= max || Math.abs(zChunk - zSentinelChunk) >= max)
			{
				ChatPlugin.sendLocalizedChatFromServer(player, "railcraft.gui.anchor.pair.fail.distance", this.getLocalizationTag(), ((TileSentinel) tile).getLocalizationTag());
				return false;
			}

			this.xSentinel = tile.xCoord;
			this.ySentinel = tile.yCoord;
			this.zSentinel = tile.zCoord;

			this.requestTicket();
			this.sendUpdateToClient();
			removeTarget(player);
			ChatPlugin.sendLocalizedChatFromServer(player, "railcraft.gui.anchor.pair.success", this.getLocalizationTag());
			return true;
		}
		ChatPlugin.sendLocalizedChatFromServer(player, "railcraft.gui.anchor.pair.fail.invalid", this.getLocalizationTag());
		return false;
	}

	public void clearSentinel()
	{
		if (!this.hasSentinel())
			return;

		this.xSentinel = -1;
		this.ySentinel = -1;
		this.zSentinel = -1;

		this.requestTicket();
		this.sendUpdateToClient();
	}

	public boolean hasSentinel()
	{
		return this.ySentinel != -1;
	}

	public boolean hasFuel()
	{
		return this.fuel > 0;
	}

	@Override
	public ArrayList<ItemStack> getDrops(int fortune)
	{
		ArrayList<ItemStack> items = new ArrayList<ItemStack>();
		ItemStack drop = this.getMachineType().getItem();
		if (this.needsFuel() && this.hasFuel())
		{
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setLong("fuel", this.fuel);
			drop.setTagCompound(nbt);
		}
		items.add(drop);
		return items;
	}

	@Override
	public void initFromItem(ItemStack stack)
	{
		super.initFromItem(stack);
		if (this.needsFuel())
			this.fuel = ItemCartAnchor.getFuel(stack);
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();
		if (Game.isNotHost(this.worldObj))
		{
			if (this.chunks != null)
				EffectManager.instance.chunkLoaderEffect(this.worldObj, this, this.chunks);
			return;
		}

		if (RailcraftConfig.deleteAnchors())
		{
			this.releaseTicket();
			this.worldObj.setBlock(this.xCoord, this.yCoord, this.zCoord, Blocks.obsidian);
			return;
		}

		if (this.xCoord != this.prevX || this.yCoord != this.prevY || this.zCoord != this.prevZ)
		{
			this.releaseTicket();
			this.prevX = this.xCoord;
			this.prevY = this.yCoord;
			this.prevZ = this.zCoord;
		}

		if (this.hasActiveTicket() && (this.getTicket().world != this.worldObj || this.refreshTicket || this.powered))
			this.releaseTicket();

		if (this.needsFuel())
		{
			this.fuelCycle++;
			if (this.fuelCycle >= FUEL_CYCLE)
			{
				this.fuelCycle = 0;
				if (this.chunks != null && this.hasActiveTicket() && this.fuel > 0)
					this.fuel -= this.chunks.size();
				if (this.fuel <= 0)
				{
					ItemStack stack = this.getStackInSlot(0);
					if (stack == null || stack.stackSize <= 0)
					{
						this.setInventorySlotContents(0, null);
						this.releaseTicket();
					}
					else if (this.getFuelMap().containsKey(stack))
					{
						this.decrStackSize(0, 1);
						this.fuel = (long) (this.getFuelMap().get(stack) * RailcraftConstants.TICKS_PER_HOUR);
					}
				}
			}
		}

		if (this.clock % SENTINEL_CHECK == 0 && this.hasSentinel())
		{
			TileEntity tile = this.worldObj.getTileEntity(this.xSentinel, this.ySentinel, this.zSentinel);
			if (!(tile instanceof TileSentinel))
				this.clearSentinel();
		}

		if (!this.hasActiveTicket())
			this.requestTicket();

		if (RailcraftConfig.printAnchorDebug() && this.hasActiveTicket())
			if (this.clock % 64 == 0)
			{
				int numChunks = this.chunks == null ? 0 : this.chunks.size();
				ChatPlugin.sendLocalizedChatToAllFromServer(this.worldObj, "%s has loaded %d chunks and is ticking at <%d,%d,%d> in dim:%d - logged on tick %d", this.getName(), numChunks, this.xCoord, this.yCoord, this.zCoord, this.worldObj.provider.dimensionId, this.worldObj.getWorldTime());
				Game.log(Level.DEBUG, "{0} has loaded {1} chunks and is ticking at <{2},{3},{4}> in dim:{5} - logged on tick {6}", this.getName(), numChunks, this.xCoord, this.yCoord, this.zCoord, this.worldObj.provider.dimensionId, this.worldObj.getWorldTime());
			}
	}

	@Override
	public void onBlockRemoval()
	{
		super.onBlockRemoval();
		this.releaseTicket();
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		this.refreshTicket = true;
	}

	@Override
	public void validate()
	{
		super.validate();
		this.refreshTicket = true;
	}

	protected void releaseTicket()
	{
		this.refreshTicket = false;
		this.setTicket(null);
	}

	protected void requestTicket()
	{
		if (this.meetsTicketRequirements())
		{
			Ticket chunkTicket = this.getTicketFromForge();
			if (chunkTicket != null)
			{
				this.setTicketData(chunkTicket);
				this.forceChunkLoading(chunkTicket);
			}
		}
	}

	public boolean needsFuel()
	{
		return !this.getFuelMap().isEmpty();
	}

	@Override
	public ItemMap<Float> getFuelMap()
	{
		return RailcraftConfig.anchorFuelWorld;
	}

	protected boolean meetsTicketRequirements()
	{
		return !this.powered && (this.hasFuel() || !this.needsFuel());
	}

	protected Ticket getTicketFromForge()
	{
		return ForgeChunkManager.requestTicket(Railcraft.getMod(), this.worldObj, Type.NORMAL);
	}

	protected void setTicketData(Ticket chunkTicket)
	{
		chunkTicket.getModData().setInteger("xCoord", this.xCoord);
		chunkTicket.getModData().setInteger("yCoord", this.yCoord);
		chunkTicket.getModData().setInteger("zCoord", this.zCoord);
		chunkTicket.getModData().setString("type", this.getMachineType().getTag());
	}

	public boolean hasActiveTicket()
	{
		return this.getTicket() != null;
	}

	public Ticket getTicket()
	{
		return tickets.get(this.getUUID());
	}

	public void setTicket(Ticket t)
	{
		boolean changed = false;
		Ticket ticket = this.getTicket();
		if (ticket != t)
		{
			if (ticket != null)
			{
				if (ticket.world == this.worldObj)
				{
					for (ChunkCoordIntPair chunk : ticket.getChunkList())
					{
						if (ForgeChunkManager.getPersistentChunksFor(this.worldObj).keys().contains(chunk))
							ForgeChunkManager.unforceChunk(ticket, chunk);
					}
					ForgeChunkManager.releaseTicket(ticket);
				}
				tickets.remove(this.getUUID());
			}
			changed = true;
		}
		this.hasTicket = t != null;
		if (this.hasTicket)
			tickets.put(this.getUUID(), t);
		if (changed)
			this.sendUpdateToClient();
	}

	public void forceChunkLoading(Ticket ticket)
	{
		this.setTicket(ticket);

		this.setupChunks();

		if (this.chunks != null)
			for (ChunkCoordIntPair chunk : this.chunks)
			{
				ForgeChunkManager.forceChunk(ticket, chunk);
			}
	}

	public void setupChunks()
	{
		if (!this.hasTicket)
			this.chunks = null;
		else if (this.hasSentinel())
			this.chunks = ChunkManager.getInstance().getChunksBetween(this.xCoord >> 4, this.zCoord >> 4, this.xSentinel >> 4, this.zSentinel >> 4, this.getMaxSentinelChunks());
		else
			// TODO gamerforEA code replace, old code:
			// this.chunks = ChunkManager.getInstance().getChunksAround(this.xCoord >> 4, this.zCoord >> 4, ANCHOR_RADIUS);
			this.chunks = ChunkManager.getInstance().getChunksAround(this.xCoord >> 4, this.zCoord >> 4, EventConfig.worldAnchorRadius);
		// TODO gamerforEA code end
	}

	public boolean isPowered()
	{
		return this.powered;
	}

	public void setPowered(boolean power)
	{
		this.powered = power;
	}

	@Override
	public void onNeighborBlockChange(Block block)
	{
		super.onNeighborBlockChange(block);
		if (Game.isNotHost(this.getWorld()))
			return;
		boolean newPower = PowerPlugin.isBlockBeingPowered(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
		if (this.powered != newPower)
			this.powered = newPower;
	}

	@Override
	public void writePacketData(DataOutputStream data) throws IOException
	{
		super.writePacketData(data);

		data.writeBoolean(this.hasTicket);

		data.writeInt(this.xSentinel);
		data.writeInt(this.ySentinel);
		data.writeInt(this.zSentinel);
	}

	@Override
	public void readPacketData(DataInputStream data) throws IOException
	{
		super.readPacketData(data);

		boolean tick = data.readBoolean();
		if (this.hasTicket != tick)
		{
			this.hasTicket = tick;
			this.markBlockForUpdate();
		}

		this.xSentinel = data.readInt();
		this.ySentinel = data.readInt();
		this.zSentinel = data.readInt();

		this.setupChunks();
	}

	@Override
	public void writeToNBT(NBTTagCompound data)
	{
		super.writeToNBT(data);

		data.setLong("fuel", this.fuel);

		data.setBoolean("powered", this.powered);

		data.setInteger("xSentinel", this.xSentinel);
		data.setInteger("ySentinel", this.ySentinel);
		data.setInteger("zSentinel", this.zSentinel);

		data.setInteger("prevX", this.prevX);
		data.setInteger("prevY", this.prevY);
		data.setInteger("prevZ", this.prevZ);
	}

	@Override
	public void readFromNBT(NBTTagCompound data)
	{
		super.readFromNBT(data);

		if (this.needsFuel())
			this.fuel = data.getLong("fuel");

		this.powered = data.getBoolean("powered");

		this.xSentinel = data.getInteger("xSentinel");
		this.ySentinel = data.getInteger("ySentinel");
		this.zSentinel = data.getInteger("zSentinel");

		this.prevX = data.getInteger("prevX");
		this.prevY = data.getInteger("prevY");
		this.prevZ = data.getInteger("prevZ");
	}

	@Override
	public float getResistance(Entity exploder)
	{
		return 60f;
	}

	@Override
	public float getHardness()
	{
		return 20;
	}

	@Override
	public long getAnchorFuel()
	{
		return this.fuel;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int var1)
	{
		if (RailcraftConfig.anchorsCanInteractWithPipes())
			return SLOTS;
		return SLOTS_NO_ACCESS;
	}

	@Override
	public boolean canInsertItem(int i, ItemStack itemstack, int j)
	{
		return RailcraftConfig.anchorsCanInteractWithPipes();
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int j)
	{
		return false;
	}
}
