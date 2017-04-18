/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.blocks.machine.epsilon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.railcraft.api.electricity.IElectricGrid;
import mods.railcraft.api.tracks.ITrackInstance;
import mods.railcraft.api.tracks.ITrackLockdown;
import mods.railcraft.common.blocks.RailcraftBlocks;
import mods.railcraft.common.blocks.machine.IEnumMachine;
import mods.railcraft.common.blocks.machine.TileMachineBase;
import mods.railcraft.common.blocks.tracks.EnumTrack;
import mods.railcraft.common.blocks.tracks.EnumTrackMeta;
import mods.railcraft.common.blocks.tracks.TileTrack;
import mods.railcraft.common.blocks.tracks.TrackForce;
import mods.railcraft.common.blocks.tracks.TrackTools;
import mods.railcraft.common.plugins.forge.PowerPlugin;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.util.effects.EffectManager;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.misc.MiscTools;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class TileForceTrackEmitter extends TileMachineBase implements IElectricGrid
{
	private static final double BASE_DRAW = 22;
	private static final double CHARGE_PER_TRACK = 2;
	private static final int TICKS_PER_ACTION = 4;
	private static final int TICKS_PER_REFRESH = 64;
	public static final int MAX_TRACKS = 64;
	private final ChargeHandler chargeHandler = new ChargeHandler(this, ChargeHandler.ConnectType.BLOCK, 0.0);
	private boolean powered;
	private ForgeDirection facing = ForgeDirection.NORTH;
	private int numTracks;
	private State state = State.RETRACTED;

	private static enum State
	{
		EXTENDED,
		RETRACTED,
		EXTENDING,
		RETRACTING,
		HALTED;

		@SuppressWarnings("incomplete-switch")
		private void doAction(TileForceTrackEmitter emitter)
		{
			switch (this)
			{
				case EXTENDING:
					emitter.extend();
					break;
				case RETRACTING:
					emitter.retract();
					break;
				case EXTENDED:
					emitter.extended();
					break;
			}
		}
	}

	@Override
	public void onNeighborBlockChange(Block block)
	{
		super.onNeighborBlockChange(block);
		this.checkRedstone();
	}

	@Override
	public void onBlockPlacedBy(EntityLivingBase entityliving, ItemStack stack)
	{
		super.onBlockPlacedBy(entityliving, stack);
		this.facing = MiscTools.getHorizontalSideClosestToPlayer(this.worldObj, this.xCoord, this.yCoord, this.zCoord, entityliving);
		this.checkRedstone();
	}

	private void checkRedstone()
	{
		if (Game.isNotHost(this.getWorld()))
			return;
		boolean p = PowerPlugin.isBlockBeingPowered(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
		if (this.powered != p)
		{
			this.powered = p;
			this.sendUpdateToClient();
		}
	}

	@Override
	public void onBlockRemoval()
	{
		super.onBlockRemoval();
		while (this.numTracks > 0)
		{
			int x = this.xCoord + this.numTracks * this.facing.offsetX;
			int y = this.yCoord + 1;
			int z = this.zCoord + this.numTracks * this.facing.offsetZ;
			this.removeTrack(x, y, z);
		}
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void updateEntity()
	{
		super.updateEntity();

		if (Game.isNotHost(this.getWorld()))
			return;

		double draw = getDraw(this.numTracks);
		if (this.powered && this.chargeHandler.removeCharge(draw) >= draw)
			switch (this.state)
			{
				case RETRACTED:
				case RETRACTING:
				case HALTED:
					this.state = State.EXTENDING;
					break;
				case EXTENDED:
					if (this.clock % TICKS_PER_REFRESH == 0)
						this.state = State.EXTENDING;
					break;
			}
		else if (this.state == State.EXTENDED || this.state == State.EXTENDING || this.state == State.HALTED)
			this.state = State.RETRACTING;

		this.state.doAction(this);

		this.chargeHandler.tick();
	}

	private void spawnParticles(int x, int y, int z)
	{
		EffectManager.instance.forceTrackSpawnEffect(this.worldObj, x, y, z);
	}

	private void extended()
	{
		TileEntity tile = this.tileCache.getTileOnSide(ForgeDirection.UP);
		if (tile instanceof TileTrack)
		{
			TileTrack trackTile = (TileTrack) tile;
			ITrackInstance track = trackTile.getTrackInstance();
			if (track instanceof ITrackLockdown)
				((ITrackLockdown) track).releaseCart();
		}
	}

	private void extend()
	{
		if (!this.hasPowerToExtend())
			this.state = State.HALTED;
		if (this.numTracks >= MAX_TRACKS)
			this.state = State.EXTENDED;
		else if (this.clock % TICKS_PER_ACTION == 0)
		{
			int x = this.xCoord + (this.numTracks + 1) * this.facing.offsetX;
			int y = this.yCoord + 1;
			int z = this.zCoord + (this.numTracks + 1) * this.facing.offsetZ;
			if (WorldPlugin.blockExists(this.worldObj, x, y, z))
			{
				Block block = WorldPlugin.getBlock(this.worldObj, x, y, z);
				EnumTrackMeta meta;
				if (this.facing == ForgeDirection.NORTH || this.facing == ForgeDirection.SOUTH)
					meta = EnumTrackMeta.NORTH_SOUTH;
				else
					meta = EnumTrackMeta.EAST_WEST;
				if (!this.placeTrack(x, y, z, block, meta) && !this.claimTrack(x, y, z, block, meta))
					this.state = State.EXTENDED;
			}
			else
				this.state = State.HALTED;
		}
	}

	private boolean placeTrack(int x, int y, int z, Block block, EnumTrackMeta meta)
	{
		if (WorldPlugin.blockIsAir(this.worldObj, x, y, z, block))
		{
			// TODO gamerforEA code start
			if (this.fake.cantBreak(x, y, z))
				return false;
			// TODO gamerforEA code end

			this.spawnParticles(x, y, z);
			TileTrack track = TrackTools.placeTrack(EnumTrack.FORCE.getTrackSpec(), this.worldObj, x, y, z, meta.ordinal());
			((TrackForce) track.getTrackInstance()).setEmitter(this);
			this.numTracks++;
			return true;
		}
		return false;
	}

	private boolean claimTrack(int x, int y, int z, Block block, EnumTrackMeta meta)
	{
		if (block != RailcraftBlocks.getBlockTrack())
			return false;
		if (TrackTools.getTrackMetaEnum(this.worldObj, block, null, x, y, z) != meta)
			return false;
		TileEntity tile = WorldPlugin.getBlockTile(this.worldObj, x, y, z);
		if (!TrackTools.isTrackSpec(tile, EnumTrack.FORCE.getTrackSpec()))
			return false;
		TrackForce track = (TrackForce) ((TileTrack) tile).getTrackInstance();
		TileForceTrackEmitter emitter = track.getEmitter();
		if (emitter == null || emitter == this)
		{
			track.setEmitter(this);
			this.numTracks++;
			return true;
		}
		return false;
	}

	public int getNumberOfTracks()
	{
		return this.numTracks;
	}

	public static double getDraw(int tracks)
	{
		return BASE_DRAW + CHARGE_PER_TRACK * tracks;
	}

	public boolean hasPowerToExtend()
	{
		return this.chargeHandler.getCharge() >= getDraw(this.numTracks + 1);
	}

	private void retract()
	{
		if (this.numTracks <= 0)
			this.state = State.RETRACTED;
		else if (this.clock % TICKS_PER_ACTION == 0)
		{
			int x = this.xCoord + this.numTracks * this.facing.offsetX;
			int y = this.yCoord + 1;
			int z = this.zCoord + this.numTracks * this.facing.offsetZ;
			this.removeTrack(x, y, z);
		}
	}

	private void removeTrack(int x, int y, int z)
	{
		if (WorldPlugin.blockExists(this.worldObj, x, y, z) && TrackTools.isTrackAt(this.worldObj, x, y, z, EnumTrack.FORCE))
		{
			// TODO gamerforEA code start
			if (this.fake.cantBreak(x, y, z))
				return;
			// TODO gamerforEA code end

			this.spawnParticles(x, y, z);
			WorldPlugin.setBlockToAir(this.worldObj, x, y, z);
		}
		this.numTracks--;
	}

	@Override
	public ChargeHandler getChargeHandler()
	{
		return this.chargeHandler;
	}

	@Override
	public TileEntity getTile()
	{
		return this;
	}

	@Override
	public IEnumMachine getMachineType()
	{
		return EnumMachineEpsilon.FORCE_TRACK_EMITTER;
	}

	@Override
	public IIcon getIcon(int side)
	{
		if (side == this.facing.ordinal())
			return this.getMachineType().getTexture(this.powered ? 7 : 8);
		return this.getMachineType().getTexture(this.powered ? 0 : 6);
	}

	@Override
	public boolean rotateBlock(ForgeDirection axis)
	{
		if (Game.isNotHost(this.worldObj))
			return false;
		if (this.state != State.RETRACTED)
			return false;
		if (axis == ForgeDirection.UP || axis == ForgeDirection.DOWN)
			return false;
		if (this.facing == axis)
			this.facing = axis.getOpposite();
		else
			this.facing = axis;
		this.numTracks = 0;
		this.markBlockForUpdate();
		this.notifyBlocksOfNeighborChange();
		return true;
	}

	@Override
	public void writeToNBT(NBTTagCompound data)
	{
		super.writeToNBT(data);
		this.chargeHandler.writeToNBT(data);
		data.setBoolean("powered", this.powered);
		data.setByte("facing", (byte) this.facing.ordinal());
		data.setInteger("numTracks", this.numTracks);
		data.setString("state", this.state.name());
	}

	@Override
	public void readFromNBT(NBTTagCompound data)
	{
		super.readFromNBT(data);
		this.chargeHandler.readFromNBT(data);
		this.powered = data.getBoolean("powered");
		this.facing = ForgeDirection.getOrientation(data.getByte("facing"));
		this.numTracks = data.getInteger("numTracks");
		this.state = State.valueOf(data.getString("state"));
	}

	@Override
	public void writePacketData(DataOutputStream data) throws IOException
	{
		super.writePacketData(data);
		data.writeBoolean(this.powered);
		data.writeByte((byte) this.facing.ordinal());
	}

	@Override
	public void readPacketData(DataInputStream data) throws IOException
	{
		super.readPacketData(data);

		boolean update = false;

		boolean p = data.readBoolean();
		if (this.powered != p)
		{
			this.powered = p;
			update = true;
		}
		byte f = data.readByte();
		if (this.facing != ForgeDirection.getOrientation(f))
		{
			this.facing = ForgeDirection.getOrientation(f);
			update = true;
		}

		if (update)
			this.markBlockForUpdate();
	}

	public ForgeDirection getFacing()
	{
		return this.facing;
	}

}
