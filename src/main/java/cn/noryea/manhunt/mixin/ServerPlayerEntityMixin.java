package cn.noryea.manhunt.mixin;

import cn.noryea.manhunt.Manhunt;
import com.mojang.authlib.GameProfile;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    @Shadow public MinecraftServer server;
    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Shadow
    public abstract boolean changeGameMode(GameMode gameMode);

    boolean holding;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        //猎人追踪器功能
        if (this.isTeamPlayer(server.getScoreboard().getTeam("hunters")) && this.isAlive()) {
            if (!hasTracker()) {
                NbtCompound nbt = new NbtCompound();
                nbt.putBoolean("Tracker", true);
                nbt.putBoolean("LodestoneTracked", false);
                nbt.putString("LodestoneDimension", "minecraft:overworld");
                nbt.putInt("HideFlags", 1);
                nbt.put("Info", new NbtCompound());
                nbt.put("display", new NbtCompound());
                nbt.getCompound("display").putString("Name", "{\"text\": \"追踪器\",\"italic\": false,\"color\": \"white\"}");

                ItemStack stack = new ItemStack(Items.COMPASS);
                stack.setNbt(nbt);
                stack.addEnchantment(Enchantments.VANISHING_CURSE, 1);

                this.giveItemStack(stack);  //给予
            }

            //显示信息
            if (holdingTracker()) {
                holding = true;
                if (this.getMainHandStack().getOrCreateNbt().getBoolean("Tracker")) {
                    NbtCompound info = this.getMainHandStack().getOrCreateNbt().getCompound("Info");
                    if (server.getPlayerManager().getPlayer(info.getString("Name")) != null) {
                        showInfo(info);
                    }
                } else {
                    NbtCompound info = this.getOffHandStack().getOrCreateNbt().getCompound("Info");
                    if (server.getPlayerManager().getPlayer(info.getString("Name")) != null) {
                        showInfo(info);
                    }
                }
            } else {
                if (holding) {
                    this.networkHandler.sendPacket(new OverlayMessageS2CPacket(new LiteralText("")));
                    holding = false;
                }
            }

        }
    }

    //逃者死亡事件
    @Inject(method = "onDeath", at = @At("HEAD"))
    public void onDeath(DamageSource source, CallbackInfo ci) {
        Scoreboard scoreboard = server.getScoreboard();

        if (this.getScoreboardTeam().isEqual(scoreboard.getTeam("runners"))) {

            changeGameMode(GameMode.SPECTATOR);
            scoreboard.clearPlayerTeam(this.getName().asString());

            if (server.getScoreboard().getTeam("runners").getPlayerList().isEmpty()) {
                server.getCommandManager().execute(this.getCommandSource().withSilent().withLevel(2), "title @a subtitle {\"text\":\"所有逃者已阵亡\",\"color\":\"white\"}");
                server.getCommandManager().execute(this.getCommandSource().withSilent().withLevel(2), "title @a title {\"text\":\"猎人胜利!\",\"color\":\"red\"}");
            }
        }
    }

    //玩家列表的名字
    @Inject(method = "getPlayerListName", at = @At("TAIL"), cancellable = true)
    private void replacePlayerListName(CallbackInfoReturnable<Text> cir) {
        try {
            if (this.getScoreboardTeam() != null) {

                Team team = server.getScoreboard().getTeam(this.getScoreboardTeam().getName());

                MutableText mutableText = (new LiteralText("")).append(team.getFormattedName()).append(this.getName());

                Formatting formatting = team.getColor();
                if (formatting != Formatting.RESET) {
                    mutableText.formatted(formatting);
                } else if (team.getName().equals("hunters")) {
                    mutableText.formatted(Manhunt.huntersColor);
                } else if (team.getName().equals("runners")) {
                    mutableText.formatted(Manhunt.runnersColor);
                }

                cir.setReturnValue(mutableText);

            }
        } catch (Exception e) {
            e.printStackTrace();
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

        this.networkHandler.sendPacket(new OverlayMessageS2CPacket(new LiteralText(actionbar)));
    }

    private boolean hasTracker() {
        boolean n = false;
        for (ItemStack item : this.getInventory().main) {
            if (item.getItem().equals(Items.COMPASS) && item.getOrCreateNbt().getBoolean("Tracker")) {
                n = true;
                break;
            }
        }

        if (this.playerScreenHandler.getCursorStack().getOrCreateNbt().getBoolean("Tracker")) {
            n = true;
        } else if (this.getOffHandStack().getOrCreateNbt().getBoolean("Tracker")) {
            n = true;
        }

        return n;
    }

    private boolean holdingTracker() {
        boolean n = false;
        if (this.getMainHandStack().getOrCreateNbt().getBoolean("Tracker") && this.getMainHandStack().getOrCreateNbt().getCompound("Info").contains("Name")) {
            n = true;
        } else if (this.getOffHandStack().getOrCreateNbt().getBoolean("Tracker") && this.getOffHandStack().getOrCreateNbt().getCompound("Info").contains("Name")) {
            n = true;
        }
        return n;
    }

}
