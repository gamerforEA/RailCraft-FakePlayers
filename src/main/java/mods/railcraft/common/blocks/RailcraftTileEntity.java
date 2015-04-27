/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.blocks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mods.railcraft.api.core.INetworkedObject;
import mods.railcraft.api.core.IOwnable;
import mods.railcraft.common.plugins.forge.LocalizationPlugin;
import mods.railcraft.common.plugins.forge.PlayerPlugin;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.util.misc.AdjacentTileCache;
import mods.railcraft.common.util.misc.MiscTools;
import mods.railcraft.common.util.network.PacketBuilder;
import mods.railcraft.common.util.network.PacketTileEntity;
import mods.railcraft.common.util.network.RailcraftPacket;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import com.gamerforea.railcraft.FakePlayerUtils;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;

public abstract class RailcraftTileEntity extends TileEntity implements INetworkedObject, IOwnable
{
	protected final AdjacentTileCache tileCache = new AdjacentTileCache(this);
	protected int clock = MiscTools.getRand().nextInt();
	private GameProfile owner = new GameProfile(null, "[Railcraft]");
	private boolean sendClientUpdate = false;

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
	// TODO gamerforEA code end

	public static boolean isUseableByPlayerHelper(TileEntity tile, EntityPlayer player)
	{
		if (tile.isInvalid()) return false;
		if (tile.getWorldObj().getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile) return false;
		return player.getDistanceSq(tile.xCoord, tile.yCoord, tile.zCoord) <= 64;
	}

	public AdjacentTileCache getTileCache()
	{
		return tileCache;
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();
		clock++;

		if (sendClientUpdate)
		{
			sendClientUpdate = false;
			PacketBuilder.instance().sendTileEntityPacket(this);
		}
	}

	@Override
	public FMLProxyPacket getDescriptionPacket()
	{
		//        System.out.println("Sending Tile Packet");
		RailcraftPacket packet = new PacketTileEntity(this);
		return packet.getPacket();
	}

	@Override
	public void writePacketData(DataOutputStream data) throws IOException
	{
		//        data.writeUTF(owner);
	}

	@Override
	public void readPacketData(DataInputStream data) throws IOException
	{
		//        owner = data.readUTF();
	}

	public void markBlockForUpdate()
	{
		//        System.out.println("updating");
		if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}

	public void notifyBlocksOfNeighborChange()
	{
		if (worldObj != null) WorldPlugin.notifyBlocksOfNeighborChange(worldObj, xCoord, yCoord, zCoord, getBlockType());
	}

	public void sendUpdateToClient()
	{
		if (canUpdate()) sendClientUpdate = true;
		else PacketBuilder.instance().sendTileEntityPacket(this);
	}

	public void onBlockPlacedBy(EntityLivingBase entityliving, ItemStack stack)
	{
		if (entityliving instanceof EntityPlayer)
		{
			owner = ((EntityPlayer) entityliving).getGameProfile();
			// TODO gamerforEA code start
			this.ownerUUID = this.owner.getId();
			this.ownerName = this.owner.getName();
			// TODO gamerforEA code end
		}
	}

	public void onNeighborBlockChange(Block id)
	{
		tileCache.onNeighborChange();
	}

	@Override
	public void invalidate()
	{
		tileCache.purge();
		super.invalidate();
	}

	@Override
	public void validate()
	{
		tileCache.purge();
		super.validate();
	}

	public final int getDimension()
	{
		if (worldObj == null) return 0;
		return worldObj.provider.dimensionId;
	}

	@Override
	public final GameProfile getOwner()
	{
		return owner;
	}

	public boolean isOwner(GameProfile player)
	{
		return PlayerPlugin.isSamePlayer(owner, player);
	}

	public String getName()
	{
		return LocalizationPlugin.translate(getLocalizationTag());
	}

	public abstract String getLocalizationTag();

	public List<String> getDebugOutput()
	{
		List<String> debug = new ArrayList<String>();
		debug.add("Railcraft Tile Entity Data Dump");
		debug.add("Object: " + this);
		debug.add(String.format("Coordinates: d=%d, %d,%d,%d", worldObj.provider.dimensionId, xCoord, yCoord, zCoord));
		debug.add("Owner: " + (owner == null ? "null" : owner.getName()));
		debug.addAll(tileCache.getDebugOutput());
		return debug;
	}

	@Override
	public void writeToNBT(NBTTagCompound data)
	{
		super.writeToNBT(data);
		if (owner.getName() != null) data.setString("owner", owner.getName());
		if (owner.getId() != null) data.setString("ownerId", owner.getId().toString());
		// TODO gamerforEA code start
		if (this.ownerUUID != null) data.setString("ownerUUID", this.ownerUUID.toString());
		if (!Strings.isNullOrEmpty(this.ownerName)) data.setString("ownerName", this.ownerName);
		// TODO gamerforEA code end
	}

	@Override
	public void readFromNBT(NBTTagCompound data)
	{
		super.readFromNBT(data);
		owner = PlayerPlugin.readOwnerFromNBT(data);
		// TODO gamerforEA code start
		String s = data.getString("ownerUUID");
		if (!Strings.isNullOrEmpty(s)) this.ownerUUID = UUID.fromString(s);
		s = data.getString("ownerName");
		if (!Strings.isNullOrEmpty(s)) this.ownerName = s;
		// TODO gamerforEA code end
	}

	public final int getX()
	{
		return xCoord;
	}

	public final int getY()
	{
		return yCoord;
	}

	public final int getZ()
	{
		return zCoord;
	}

	@Override
	public final World getWorld()
	{
		return worldObj;
	}

	public abstract short getId();
}