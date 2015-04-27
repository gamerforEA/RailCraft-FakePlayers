package com.gamerforea.railcraft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class ExplosionByPlayer extends Explosion
{
	private EntityPlayer ownerPlayer;
	private World worldObj;
	private Map field_77288_k = new HashMap();

	public ExplosionByPlayer(EntityPlayer player, World p_i1948_1_, Entity p_i1948_2_, double p_i1948_3_, double p_i1948_5_, double p_i1948_7_, float p_i1948_9_)
	{
		super(p_i1948_1_, p_i1948_2_, p_i1948_3_, p_i1948_5_, p_i1948_7_, p_i1948_9_);
		this.worldObj = p_i1948_1_;
		this.ownerPlayer = player;
	}

	@Override
	public void doExplosionA()
	{
		this.affectedBlockPositions.addAll(this.getPositions());
		float size = this.explosionSize;
		this.explosionSize *= 2.0F;
		int i0 = MathHelper.floor_double(this.explosionX - (double) this.explosionSize - 1.0D);
		int i1 = MathHelper.floor_double(this.explosionX + (double) this.explosionSize + 1.0D);
		int j0 = MathHelper.floor_double(this.explosionY - (double) this.explosionSize - 1.0D);
		int j1 = MathHelper.floor_double(this.explosionY + (double) this.explosionSize + 1.0D);
		int k0 = MathHelper.floor_double(this.explosionZ - (double) this.explosionSize - 1.0D);
		int k1 = MathHelper.floor_double(this.explosionZ + (double) this.explosionSize + 1.0D);
		List<Entity> entities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this.exploder, AxisAlignedBB.getBoundingBox((double) i0, (double) j0, (double) k0, (double) i1, (double) j1, (double) k1));
		Vec3 vec3 = Vec3.createVectorHelper(this.explosionX, this.explosionY, this.explosionZ);

		for (int i = 0; i < entities.size(); ++i)
		{
			Entity entity = entities.get(i);
			double distance = entity.getDistance(this.explosionX, this.explosionY, this.explosionZ) / (double) this.explosionSize;

			if (distance <= 1.0D)
			{
				double d0 = entity.posX - this.explosionX;
				double d1 = entity.posY + (double) entity.getEyeHeight() - this.explosionY;
				double d2 = entity.posZ - this.explosionZ;
				double d3 = (double) MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);

				if (d3 != 0.0D)
				{
					d0 /= d3;
					d1 /= d3;
					d2 /= d3;
					double d4 = (double) this.worldObj.getBlockDensity(vec3, entity.boundingBox);
					double d5 = (1.0D - distance) * d4;
					if (!FakePlayerUtils.callEntityDamageByEntityEvent(this.getOwnerPlayer(), entity, DamageCause.ENTITY_ATTACK, 1D).isCancelled()) entity.attackEntityFrom(DamageSource.setExplosionSource(this), (float) ((int) ((d5 * d5 + d5) / 2.0D * 8.0D * (double) this.explosionSize + 1.0D)));
					double d6 = EnchantmentProtection.func_92092_a(entity, d5);
					entity.motionX += d0 * d6;
					entity.motionY += d1 * d6;
					entity.motionZ += d2 * d6;

					if (entity instanceof EntityPlayer)
					{
						this.field_77288_k.put((EntityPlayer) entity, Vec3.createVectorHelper(d0 * d5, d1 * d5, d2 * d5));
					}
				}
			}
		}

		this.explosionSize = size;
	}

	private Set<ChunkPosition> getPositions()
	{
		Set<ChunkPosition> set = new HashSet();
		for (int i = 0; i < 16; ++i)
		{
			for (int j = 0; j < 16; ++j)
			{
				for (int k = 0; k < 16; ++k)
				{
					if (i == 0 || i == 15 || j == 0 || j == 15 || k == 0 || k == 15)
					{
						double d0 = (double) ((float) i / 30.0F - 1.0F);
						double d1 = (double) ((float) j / 30.0F - 1.0F);
						double d2 = (double) ((float) k / 30.0F - 1.0F);
						double dX = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
						d0 /= dX;
						d1 /= dX;
						d2 /= dX;
						float size = this.explosionSize * (0.7F + this.worldObj.rand.nextFloat() * 0.6F);
						dX = this.explosionX;
						double dY = this.explosionY;
						double dZ = this.explosionZ;

						for (float f = 0.3F; size > 0.0F; size -= f * 0.75F)
						{
							int x = MathHelper.floor_double(dX);
							int y = MathHelper.floor_double(dY);
							int z = MathHelper.floor_double(dZ);
							Block block = this.worldObj.getBlock(x, y, z);

							if (block.getMaterial() != Material.air)
							{
								float resistance = this.exploder != null ? this.exploder.func_145772_a(this, this.worldObj, x, y, z, block) : block.getExplosionResistance(this.exploder, worldObj, x, y, z, explosionX, explosionY, explosionZ);
								size -= (resistance + 0.3F) * f;
							}

							if (size > 0.0F && (this.exploder == null || this.exploder.func_145774_a(this, this.worldObj, x, y, z, block, size)))
							{
								if (!FakePlayerUtils.callBlockBreakEvent(x, y, z, this.getOwnerPlayer()).isCancelled()) set.add(new ChunkPosition(x, y, z));
							}

							dX += d0 * (double) f;
							dY += d1 * (double) f;
							dZ += d2 * (double) f;
						}
					}
				}
			}
		}
		return set;
	}

	@Override
	public Map func_77277_b()
	{
		return this.field_77288_k;
	}

	public EntityPlayer getOwnerPlayer()
	{
		EntityPlayer player = null;
		if (this.ownerPlayer != null) player = this.ownerPlayer;
		else player = FakePlayerUtils.getPlayer(this.worldObj);
		return player;
	}

	public static ExplosionByPlayer createExplosion(EntityPlayer player, World world, Entity entity, double d0, double d1, double d2, float f, boolean b)
	{
		return newExplosion(player, world, entity, d0, d1, d2, f, false, b);
	}

	public static ExplosionByPlayer newExplosion(EntityPlayer player, World world, Entity entity, double d0, double d1, double d2, float f, boolean b0, boolean b1)
	{
		ExplosionByPlayer explosion = new ExplosionByPlayer(player, world, entity, d0, d1, d2, f);
		explosion.isFlaming = b0;
		explosion.isSmoking = b1;
		explosion.doExplosionA();
		explosion.doExplosionB(true);
		return explosion;
	}
}