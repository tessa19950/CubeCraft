package net.mcreator.cubecraft.block;

import com.google.common.collect.Lists;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class CubeletMachineTile extends TileEntity implements ITickableTileEntity {

    public long cubeletInsertTime;
    public ArmorStandEntity armorStand;
    public RabbitEntity[] rabbits = new RabbitEntity[2];

    public CubeletMachineTile() {
        super(CubeletMachineElement.tileEntityType);
    }

    @Override
    public void tick() {
        float animTime = (float)(world.getGameTime() - this.cubeletInsertTime);
        int openTime = 127;
        if(animTime > 1 && animTime < openTime) {
            //Spawn armor stand if it hasn't yet
            if(armorStand == null) {
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                this.armorStand = new ArmorStandEntity(EntityType.ARMOR_STAND, this.world);
                this.armorStand.setItemStackToSlot(EquipmentSlotType.HEAD, head);
                this.armorStand.setInvisible(true);
                world.addEntity(armorStand);
            }
            //Calculate the position of the armor stand
            float y= animTime > 20 ? MathHelper.sin((animTime - 20) * 0.1F) * 0.1F : -1 + (animTime/20);
            float yaw = animTime > 20 ? (animTime - 20) * 5 : 0;
            this.armorStand.setPositionAndRotation(this.pos.getX() + 0.5D, this.pos.getY() + y, this.pos.getZ() + 0.5D, yaw, 0);
        }
        if(animTime > 20 && animTime < openTime) {
            for (int i = 0; i < 2; i++) {
                boolean spawnPoof = false;
                //Spawn rabbit if it hasn't yet
                if (rabbits[i] == null) {
                    rabbits[i] = new RabbitEntity(EntityType.RABBIT, world);
                    rabbits[i].setNoGravity(true);
                    rabbits[i].setNoAI(true);
                    rabbits[i].setRabbitType(i);
                    world.addEntity(rabbits[i]);
                    spawnPoof = true;
                }
                //Calculate the position of the rabbit
                float circleSpeed = 0.06F;
                float circleRadius = MathHelper.cos((animTime - 20) * 0.015F) * 2.5F;
                double x = MathHelper.sin(animTime * circleSpeed + ((float) Math.PI * i)) * circleRadius;
                double z = MathHelper.cos(animTime * circleSpeed + ((float) Math.PI * i)) * circleRadius;
                float jumpSpeed = 0.15F;
                double y = MathHelper.sin((animTime * jumpSpeed) % (float) Math.PI) * 1.5D;
                float lastJumpTime = (float)Math.PI * 5.5F;
                if(animTime * jumpSpeed < lastJumpTime) {
                    y += (animTime - 20) * 0.03D;
                } else {
                    y += 2.5D - ((animTime * jumpSpeed) - lastJumpTime);
                }
                float yaw = 180F + (circleRadius / 2.5F * 90F) + (180 * i) + (-animTime * circleSpeed * 180F / (float) Math.PI);
                rabbits[i].setPositionAndRotation(this.pos.getX() + 0.5D + x, this.pos.getY() + y, this.pos.getZ() + 0.5D + z, yaw, 0);
                rabbits[i].setRotationYawHead(yaw);
                //Spawn the poof particle if the rabbit was spawned this tick
                if(spawnPoof) {
                    for(int j = 0; j < 8; ++j) {
                        double x1 = (world.rand.nextDouble() - 0.5D) * 0.3D;
                        double y1 = (world.rand.nextDouble() - 0.5D) * 0.3D;
                        double z1 = (world.rand.nextDouble() - 0.5D) * 0.3D;
                        world.addParticle(ParticleTypes.POOF, this.pos.getX() + 0.5D + x + x1, this.pos.getY() + y + y1, this.pos.getZ() + 0.5D + z + z1, 0, 0, 0);
                    }
                }
                //Spawn the trail particle
                double x1 = (world.rand.nextDouble() - 0.5D) * 0.01D;
                double y1 = (world.rand.nextDouble() - 0.5D) * 0.01D;
                double z1 = (world.rand.nextDouble() - 0.5D) * 0.01D;
                if(animTime % 2 == 0) {
                    world.addParticle(ParticleTypes.DRAGON_BREATH, this.pos.getX() + 0.5D + x, this.pos.getY() + y + 0.3D, this.pos.getZ() + 0.5D + z, x1, y1, z1);
                } else {
                    world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.pos.getX() + 0.5D + x, this.pos.getY() + y, this.pos.getZ() + 0.5D + z, x1, -0.02D, z1);
                }
            }
        }
        if(animTime == openTime) {
            //Remove the rabbits and the armor stand
            for (RabbitEntity rabbit : rabbits) {
                if (rabbit != null)
                    rabbit.remove();
            }
            if (armorStand != null)
                armorStand.remove();
            armorStand = null;
            rabbits = new RabbitEntity[2];

            //Spawn the firework particles
            int[] colors = new int[]{DyeColor.GREEN.getFireworkColor(), DyeColor.BLUE.getFireworkColor(), DyeColor.ORANGE.getFireworkColor(), DyeColor.RED.getFireworkColor(), DyeColor.MAGENTA.getFireworkColor()};
            for (int color : colors) {
                ItemStack itemstack = new ItemStack(Items.FIREWORK_ROCKET);
                //Create the explosion nbt
                CompoundNBT explosion = itemstack.getOrCreateChildTag("Explosion");
                explosion.putIntArray("Colors", Lists.newArrayList(color));
                //Create the explosion list nbt
                ListNBT explosionlist = new ListNBT();
                explosionlist.add(explosion);
                //Create the firework nbt
                CompoundNBT fireworks = itemstack.getOrCreateChildTag("Fireworks");
                fireworks.put("Explosions", explosionlist);
                this.world.makeFireworks(this.pos.getX() + 0.5D, this.pos.getY() + 2D, this.pos.getZ() + 0.5D, 0, 0, 0, fireworks);
            }
        }
    }

    public ITextComponent getDefaultName() {
        return new StringTextComponent("cubelet_machine");
    }

    public ITextComponent getDisplayName() {
        return new StringTextComponent("Cubelet Machine");
    }
}
