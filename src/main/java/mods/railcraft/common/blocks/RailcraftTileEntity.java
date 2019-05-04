/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.blocks;

import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.railcraft.ModUtils;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import mods.railcraft.api.core.INetworkedObject;
import mods.railcraft.api.core.IOwnable;
import mods.railcraft.common.blocks.machine.TileMultiBlock;
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class RailcraftTileEntity extends TileEntity implements INetworkedObject, IOwnable
{
	protected final AdjacentTileCache tileCache = new AdjacentTileCache(this);
	protected int clock = MiscTools.getRand().nextInt();
	private GameProfile owner = new GameProfile(null, "[Railcraft]");
	private boolean sendClientUpdate = false;
	private UUID uuid;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = ModUtils.NEXUS_FACTORY.wrapFake(this);
	// TODO gamerforEA code end

	public static boolean isUseableByPlayerHelper(TileEntity tile, EntityPlayer player)
	{
		if (tile.isInvalid())
			return false;
		if (tile.getWorldObj().getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile)
			return false;

		boolean validDistance = player.getDistanceSq(tile.xCoord, tile.yCoord, tile.zCoord) <= 64;

		// TODO gamerforEA code start
		if (validDistance && tile instanceof TileMultiBlock && !((TileMultiBlock) tile).isStructureValid())
			return false;
		// TODO gamerforEA code end

		return validDistance;
	}

	public UUID getUUID()
	{
		if (this.uuid == null)
			this.uuid = UUID.randomUUID();
		return this.uuid;
	}

	public AdjacentTileCache getTileCache()
	{
		return this.tileCache;
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();
		this.clock++;

		if (this.sendClientUpdate)
		{
			this.sendClientUpdate = false;
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
		if (this.worldObj != null)
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
	}

	public void notifyBlocksOfNeighborChange()
	{
		if (this.worldObj != null)
			WorldPlugin.notifyBlocksOfNeighborChange(this.worldObj, this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
	}

	public void sendUpdateToClient()
	{
		if (this.canUpdate())
			this.sendClientUpdate = true;
		else
			PacketBuilder.instance().sendTileEntityPacket(this);
	}

	public void onBlockPlacedBy(EntityLivingBase entityliving, ItemStack stack)
	{
		if (entityliving instanceof EntityPlayer)
		{
			this.owner = ((EntityPlayer) entityliving).getGameProfile();

			// TODO gamerforEA code start
			this.fake.setProfile(this.owner);
			// TODO gamerforEA code end
		}
	}

	public void onNeighborBlockChange(Block id)
	{
		this.tileCache.onNeighborChange();
	}

	@Override
	public void invalidate()
	{
		this.tileCache.purge();
		super.invalidate();
	}

	@Override
	public void validate()
	{
		this.tileCache.purge();
		super.validate();
	}

	public final int getDimension()
	{
		if (this.worldObj == null)
			return 0;
		return this.worldObj.provider.dimensionId;
	}

	@Override
	public final GameProfile getOwner()
	{
		return this.owner;
	}

	public boolean isOwner(GameProfile player)
	{
		return PlayerPlugin.isSamePlayer(this.owner, player);
	}

	public String getName()
	{
		return LocalizationPlugin.translate(this.getLocalizationTag());
	}

	@Override
	public abstract String getLocalizationTag();

	public List<String> getDebugOutput()
	{
		List<String> debug = new ArrayList<String>();
		debug.add("Railcraft Tile Entity Data Dump");
		debug.add("Object: " + this);
		debug.add(String.format("Coordinates: d=%d, %d,%d,%d", this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord));
		debug.add("Owner: " + (this.owner == null ? "null" : this.owner.getName()));
		debug.addAll(this.tileCache.getDebugOutput());
		return debug;
	}

	@Override
	public void writeToNBT(NBTTagCompound data)
	{
		super.writeToNBT(data);
		if (this.owner.getName() != null)
			data.setString("owner", this.owner.getName());
		if (this.owner.getId() != null)
			data.setString("ownerId", this.owner.getId().toString());

		MiscTools.writeUUID(data, "uuid", this.uuid);

		// TODO gamerforEA code start
		this.fake.writeToNBT(data);
		// TODO gamerforEA code end
	}

	@Override
	public void readFromNBT(NBTTagCompound data)
	{
		super.readFromNBT(data);
		this.owner = PlayerPlugin.readOwnerFromNBT(data);
		this.uuid = MiscTools.readUUID(data, "uuid");

		// TODO gamerforEA code start
		this.fake.readFromNBT(data);
		// TODO gamerforEA code end
	}

	public final int getX()
	{
		return this.xCoord;
	}

	public final int getY()
	{
		return this.yCoord;
	}

	public final int getZ()
	{
		return this.zCoord;
	}

	@Override
	public final World getWorld()
	{
		return this.worldObj;
	}

	public abstract short getId();
}
