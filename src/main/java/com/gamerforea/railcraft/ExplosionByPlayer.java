package com.gamerforea.railcraft;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gamerforea.eventhelper.util.EventUtils;
import com.google.common.collect.Maps;

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

public final class ExplosionByPlayer extends Explosion
{
	private final EntityPlayer player;
	private final World world;
	private final Map field_77288_k = Maps.newHashMap();

	public ExplosionByPlayer(EntityPlayer player, World world, Entity exploder, double x, double y, double z, float size)
	{
		super(world, exploder, x, y, z, size);
		this.world = world;
		this.player = player == null ? exploder instanceof EntityPlayer ? (EntityPlayer) exploder : ModUtils.getModFake(world) : player;
	}

	@Override
	public final void doExplosionA()
	{
		this.affectedBlockPositions.addAll(this.getPositions());
		float size = this.explosionSize;
		this.explosionSize *= 2F;
		int minX = MathHelper.floor_double(this.explosionX - this.explosionSize - 1D);
		int maxX = MathHelper.floor_double(this.explosionX + this.explosionSize + 1D);
		int minY = MathHelper.floor_double(this.explosionY - this.explosionSize - 1D);
		int maxY = MathHelper.floor_double(this.explosionY + this.explosionSize + 1D);
		int minZ = MathHelper.floor_double(this.explosionZ - this.explosionSize - 1D);
		int maxZ = MathHelper.floor_double(this.explosionZ + this.explosionSize + 1D);
		List<Entity> entities = this.world.getEntitiesWithinAABBExcludingEntity(this.exploder, AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
		Vec3 vec3 = Vec3.createVectorHelper(this.explosionX, this.explosionY, this.explosionZ);

		for (int i = 0; i < entities.size(); ++i)
		{
			Entity entity = entities.get(i);
			double distance = entity.getDistance(this.explosionX, this.explosionY, this.explosionZ) / this.explosionSize;

			if (distance <= 1D)
			{
				double distanceX = entity.posX - this.explosionX;
				double distanceY = entity.posY + entity.getEyeHeight() - this.explosionY;
				double distanceZ = entity.posZ - this.explosionZ;
				double distanceSq = MathHelper.sqrt_double(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);

				if (distanceSq != 0D)
				{
					if (EventUtils.cantDamage(this.player, entity))
						continue;

					distanceX /= distanceSq;
					distanceY /= distanceSq;
					distanceZ /= distanceSq;
					double d4 = this.world.getBlockDensity(vec3, entity.boundingBox);
					double d5 = (1D - distance) * d4;
					entity.attackEntityFrom(DamageSource.setExplosionSource(this), (int) ((d5 * d5 + d5) / 2D * 8D * this.explosionSize + 1D));
					double d6 = EnchantmentProtection.func_92092_a(entity, d5);
					entity.motionX += distanceX * d6;
					entity.motionY += distanceY * d6;
					entity.motionZ += distanceZ * d6;

					if (entity instanceof EntityPlayer)
						this.field_77288_k.put(entity, Vec3.createVectorHelper(distanceX * d5, distanceY * d5, distanceZ * d5));
				}
			}
		}

		this.explosionSize = size;
	}

	private final Set<ChunkPosition> getPositions()
	{
		Set<ChunkPosition> set = new HashSet();
		for (int i = 0; i < 16; ++i)
			for (int j = 0; j < 16; ++j)
				for (int k = 0; k < 16; ++k)
					if (i == 0 || i == 15 || j == 0 || j == 15 || k == 0 || k == 15)
					{
						double d0 = i / 30F - 1F;
						double d1 = j / 30F - 1F;
						double d2 = k / 30F - 1F;
						double dX = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
						d0 /= dX;
						d1 /= dX;
						d2 /= dX;
						float size = this.explosionSize * (0.7F + this.world.rand.nextFloat() * 0.6F);
						dX = this.explosionX;
						double dY = this.explosionY;
						double dZ = this.explosionZ;

						for (float f = 0.3F; size > 0F; size -= f * 0.75F)
						{
							int x = MathHelper.floor_double(dX);
							int y = MathHelper.floor_double(dY);
							int z = MathHelper.floor_double(dZ);
							Block block = this.world.getBlock(x, y, z);

							if (block.getMaterial() != Material.air)
							{
								float resistance = this.exploder != null ? this.exploder.func_145772_a(this, this.world, x, y, z, block) : block.getExplosionResistance(this.exploder, this.world, x, y, z, this.explosionX, this.explosionY, this.explosionZ);
								size -= (resistance + 0.3F) * f;
							}

							if (size > 0F && (this.exploder == null || this.exploder.func_145774_a(this, this.world, x, y, z, block, size)))
								if (!EventUtils.cantBreak(this.player, x, y, z))
									set.add(new ChunkPosition(x, y, z));

							dX += d0 * f;
							dY += d1 * f;
							dZ += d2 * f;
						}
					}
		return set;
	}

	@Override
	public final Map func_77277_b()
	{
		return this.field_77288_k;
	}

	public static final ExplosionByPlayer createExplosion(EntityPlayer player, World world, Entity entity, double d0, double d1, double d2, float f, boolean b)
	{
		return newExplosion(player, world, entity, d0, d1, d2, f, false, b);
	}

	public static final ExplosionByPlayer newExplosion(EntityPlayer player, World world, Entity entity, double d0, double d1, double d2, float f, boolean b0, boolean b1)
	{
		ExplosionByPlayer explosion = new ExplosionByPlayer(player, world, entity, d0, d1, d2, f);
		explosion.isFlaming = b0;
		explosion.isSmoking = b1;
		explosion.doExplosionA();
		explosion.doExplosionB(true);
		return explosion;
	}
}
