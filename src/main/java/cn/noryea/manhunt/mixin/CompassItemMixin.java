package cn.noryea.manhunt.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CompassItem.class)
public abstract class CompassItemMixin extends Item {

    public CompassItemMixin(Settings settings) {
        super(settings);
    }

    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return !(miner.getMainHandStack().getOrCreateNbt().getBoolean("Tracker") && miner.isCreative());
    }

}
