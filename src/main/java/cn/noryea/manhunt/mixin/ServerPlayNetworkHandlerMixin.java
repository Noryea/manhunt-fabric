package cn.noryea.manhunt.mixin;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        //清理追踪器掉落物
        this.player.getWorld().getServer().getCommandManager().execute(this.player.getWorld().getServer().getCommandSource().withSilent(), "kill @e[type=item,nbt={Item:{tag:{Tracker:1b}}}]");

        if (this.player.isTeamPlayer(this.player.getWorld().getServer().getScoreboard().getTeam("hunters")) && this.player.isAlive() && !this.player.isSpectator()) {
            //给予猎人追踪器
            if (!hasTracker(this.player)) {
                NbtCompound nbt = new NbtCompound();
                nbt.putBoolean("Tracker", true);
                nbt.putBoolean("LodestoneTracked", false);
                nbt.putString("LodestoneDimension", "minecraft:overworld");
                nbt.putInt("HideFlags", 1);
                nbt.put("display", new NbtCompound());
                nbt.put("Info", new NbtCompound());
                nbt.getCompound("display").putString("Name", "{\"text\": \"追踪器\",\"italic\": false,\"color\": \"white\"}");

                List<ServerPlayerEntity> players = player.getWorld().getPlayers();
                int i;
                for(i = 0; i < players.size(); ++i) {
                    if (players.get(i).isTeamPlayer(player.getWorld().getServer().getScoreboard().getTeam("runners"))) {
                        nbt.getCompound("Info").putString("Name", players.get(i).getName().asString());
                    }
                }

                ItemStack compass = new ItemStack(Items.COMPASS);
                compass.setNbt(nbt);
                compass.addEnchantment(Enchantments.VANISHING_CURSE, 1);
                player.giveItemStack(compass);
            }

            //对猎人显示Info
            if (this.player.getMainHandStack().getOrCreateNbt().getBoolean("Tracker") && this.player.getMainHandStack().getOrCreateNbt().getCompound("Info").contains("Name")) {
                NbtCompound info = this.player.getMainHandStack().getOrCreateNbt().getCompound("Info");
                this.player.networkHandler.sendPacket(new OverlayMessageS2CPacket(new LiteralText("目标: \u00a7c" + info.getString("Name"))));
            }

            if (this.player.getOffHandStack().getOrCreateNbt().getBoolean("Tracker") && this.player.getOffHandStack().getOrCreateNbt().getCompound("Info").contains("Name")) {
                NbtCompound info = this.player.getOffHandStack().getOrCreateNbt().getCompound("Info");
                this.player.networkHandler.sendPacket(new OverlayMessageS2CPacket(new LiteralText("目标: \u00a7c" + info.getString("Name"))));
            }
        }
    }

    private boolean hasTracker(@NotNull ServerPlayerEntity player) {
        boolean has = false;
        for (ItemStack item : player.getInventory().main) {
            if (item.getItem().equals(Items.COMPASS) && item.getOrCreateNbt().getBoolean("Tracker")) {
                has = true;
            }
        }
        if (player.playerScreenHandler.getCursorStack().getOrCreateNbt().getBoolean("Tracker")) {
            has = true;
        }
        if (player.getOffHandStack().getOrCreateNbt().getBoolean("Tracker")) {
            has = true;
        }
        return has;
    }
}
