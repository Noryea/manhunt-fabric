package cn.noryea.manhunt.mixin;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    boolean holding;

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        //清理追踪器掉落物
        player.getServer().getCommandManager().execute(this.player.getWorld().getServer().getCommandSource().withSilent(), "kill @e[type=item,nbt={Item:{tag:{Tracker:1b}}}]");

        if (player.isTeamPlayer(player.getServer().getScoreboard().getTeam("hunters")) && player.isAlive()) {
            //给予猎人追踪器
             if (!hasTracker(player)) {
                NbtCompound nbt = new NbtCompound();
                nbt.putBoolean("Tracker", true);
                nbt.putBoolean("LodestoneTracked", false);
                nbt.putString("LodestoneDimension", "minecraft:overworld");
                nbt.putInt("HideFlags", 1);
                nbt.put("Info", new NbtCompound());
                nbt.put("display", new NbtCompound());
                nbt.getCompound("display").putString("Name", "{\"text\": \"追踪器\",\"italic\": false,\"color\": \"white\"}");
                List<ServerPlayerEntity> allPlayers = player.getServer().getPlayerManager().getPlayerList();
                Team runners = player.getServer().getScoreboard().getTeam("runners");
                int i;
                String sb = "";
                for(i = 0; i < allPlayers.size(); ++i) {
                    ServerPlayerEntity x = allPlayers.get(i);
                    if (x != null && x.isTeamPlayer(runners)) {
                        sb = x.getName().asString();
                    }
                }
                nbt.getCompound("Info").putString("Name", sb);

                ItemStack stack = new ItemStack(Items.COMPASS);
                stack.setNbt(nbt);
                stack.addEnchantment(Enchantments.VANISHING_CURSE, 1);

                player.giveItemStack(stack);
            }

            //显示信息
            if (holdingTracker(player)) {
                holding = true;
                if (player.getMainHandStack().getOrCreateNbt().getBoolean("Tracker")) {
                    showInfo(player.getMainHandStack().getOrCreateNbt().getCompound("Info"));
                } else {
                    showInfo(player.getOffHandStack().getOrCreateNbt().getCompound("Info"));
                }
            } else {
                if (holding) {
                    player.networkHandler.sendPacket(new OverlayMessageS2CPacket(new LiteralText("")));
                    holding = false;
                }
            }

        }
    }

    private void showInfo(NbtCompound info) {
        String text_color = "\u00a7a";

        String actionbar = "目标: ";
        actionbar += text_color + info.getString("Name");
        actionbar += " \u00a7f";
        actionbar += " 维度: ";

        String dimension = info.getString("Dimension");
        if (!info.contains("Dimension")) {
            dimension = "\u00a7e?";
        } else if (Objects.equals(dimension, "minecraft:overworld")) {
            dimension = "主世界";
        } else if (Objects.equals(dimension, "minecraft:the_nether")) {
            dimension = "下界";
        } else if (Objects.equals(dimension, "minecraft:the_end")) {
            dimension = "末地";
        }

        actionbar += text_color + dimension;

        player.networkHandler.sendPacket(new OverlayMessageS2CPacket(new LiteralText(actionbar)));
    }

    private boolean hasTracker(@NotNull ServerPlayerEntity player) {
        boolean n = false;
        for (ItemStack item : player.getInventory().main) {
            if (item.getItem().equals(Items.COMPASS) && item.getOrCreateNbt().getBoolean("Tracker")) {
                n = true;
                break;
            }
        }

        if (player.playerScreenHandler.getCursorStack().getOrCreateNbt().getBoolean("Tracker")) {
            n = true;
        } else if (player.getOffHandStack().getOrCreateNbt().getBoolean("Tracker")) {
            n = true;
        }

        return n;
    }

    private boolean holdingTracker(@NotNull ServerPlayerEntity player) {
        boolean n = false;
        if (player.getMainHandStack().getOrCreateNbt().getBoolean("Tracker") && player.getMainHandStack().getOrCreateNbt().getCompound("Info").contains("Name")) {
            n = true;
        } else if (player.getOffHandStack().getOrCreateNbt().getBoolean("Tracker") && player.getOffHandStack().getOrCreateNbt().getCompound("Info").contains("Name")) {
            n = true;
        }
        return n;
    }
}
