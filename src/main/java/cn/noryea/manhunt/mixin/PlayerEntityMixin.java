package cn.noryea.manhunt.mixin;

import com.mojang.serialization.DataResult;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

  NbtList positions = new NbtList();

  protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
    super(entityType, world);
  }

  @Inject(method = "tick", at = @At("HEAD"))
  public void tick(CallbackInfo ci) {

    DataResult<NbtElement> var10000 = World.CODEC.encodeStart(NbtOps.INSTANCE, world.getRegistryKey());
    Logger logger = LoggerFactory.getLogger("Manhunt");
    Objects.requireNonNull(logger);
    var10000.resultOrPartial(logger::error).ifPresent((dimension) -> {
      for (int i = 0; i < positions.size(); ++i) {
        NbtCompound compound = positions.getCompound(i);
        if (Objects.equals(compound.getString("LodestoneDimension"), dimension.asString())) {
          positions.remove(compound);
        }
      }
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.put("LodestonePos", NbtHelper.fromBlockPos(this.getBlockPos()));
      nbtCompound.put("LodestoneDimension", dimension);
      positions.add(nbtCompound);
    });
  }

  @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
  public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo cbi) {
    nbt.putBoolean("manhuntModded", true);
    nbt.put("Positions", positions);
  }

  @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
  public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo cbi) {
    this.positions = nbt.getList("Positions", 10);
  }
}
