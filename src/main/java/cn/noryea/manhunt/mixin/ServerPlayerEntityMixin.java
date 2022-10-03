package cn.noryea.manhunt.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    @Final @Shadow public MinecraftServer server;
    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Shadow
    public abstract boolean changeGameMode(GameMode gameMode);

    boolean holding;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile, PlayerPublicKey key) {
        super(world, pos, yaw, profile, key);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        if (this.isTeamPlayer(server.getScoreboard().getTeam("hunters")) && this.isAlive()) {
            if (!hasTracker()) {
                NbtCompound nbt = new NbtCompound();
                nbt.putBoolean("Tracker", true);
                nbt.putBoolean("LodestoneTracked", false);
                nbt.putString("LodestoneDimension", "minecraft:overworld");
                nbt.putInt("HideFlags", 1);
                nbt.put("Info", new NbtCompound());
                nbt.put("display", new NbtCompound());
                nbt.getCompound("display").putString("Name", "{\"translate\": \"manhunt.item.tracker\",\"italic\": false,\"color\": \"white\"}");

                ItemStack stack = new ItemStack(Items.COMPASS);
                stack.setNbt(nbt);
                stack.addEnchantment(Enchantments.VANISHING_CURSE, 1);

                this.giveItemStack(stack);
            }


            if (holdingTracker()) {
                holding = true;
                if (this.getMainHandStack().getNbt() != null && this.getMainHandStack().getNbt().getBoolean("Tracker")) {
                    NbtCompound info = this.getMainHandStack().getNbt().getCompound("Info");
                    if (server.getPlayerManager().getPlayer(info.getString("Name")) != null) {
                        showInfo(info);
                    }
                } else if (this.getOffHandStack().getNbt() != null) {
                    NbtCompound info = this.getOffHandStack().getNbt().getCompound("Info");
                    if (server.getPlayerManager().getPlayer(info.getString("Name")) != null) {
                        showInfo(info);
                    }
                }
            } else {
                if (holding) {
                    this.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.of("")));
                    holding = false;
                }
            }

        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    public void onDeath(DamageSource source, CallbackInfo ci) {
        Scoreboard scoreboard = server.getScoreboard();

        if (this.getScoreboardTeam() != null) {
            if (this.getScoreboardTeam().isEqual(scoreboard.getTeam("runners"))) {

                changeGameMode(GameMode.SPECTATOR);
                scoreboard.clearPlayerTeam(this.getName().getString());

                if (server.getScoreboard().getTeam("runners").getPlayerList().isEmpty()) {
                    server.getCommandManager().executeWithPrefix(this.getCommandSource().withSilent().withLevel(2), "title @a subtitle {\"translate\":\"manhunt.win.hunters.subtitle\",\"color\":\"white\"}");
                    server.getCommandManager().executeWithPrefix(this.getCommandSource().withSilent().withLevel(2), "title @a title {\"translate\":\"manhunt.win.hunters.title\",\"color\":\"white\"}");
                }
            }
        }
    }

    private void showInfo(NbtCompound info) {
        String dim = info.getString("Dimension");
        MutableText dimension = Text.literal(dim);
        if (!info.contains("Dimension")) {
            dimension = Text.translatable("manhunt.scoreboard.world.unknown");
        } else if (Objects.equals(dim, "minecraft:overworld")) {
            dimension = Text.translatable("manhunt.scoreboard.world.overworld");
        } else if (Objects.equals(dim, "minecraft:the_nether")) {
            dimension = Text.translatable("manhunt.scoreboard.world.the_nether");
        } else if (Objects.equals(dim, "minecraft:the_end")) {
            dimension = Text.translatable("manhunt.scoreboard.world.the_end");
        }

        this.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.translatable("manhunt.scoreboard.target.text", info.getString("Name"), dimension)));
    }

    private boolean hasTracker() {
        boolean n = false;
        for (ItemStack item : this.getInventory().main) {
            if (item.getItem().equals(Items.COMPASS) && item.getNbt() != null && item.getNbt().getBoolean("Tracker")) {
                n = true;
                break;
            }
        }

        if (this.playerScreenHandler.getCursorStack().getNbt() != null && this.playerScreenHandler.getCursorStack().getNbt().getBoolean("Tracker")) {
            n = true;
        } else if (this.getOffHandStack().getNbt() != null && this.getOffHandStack().getNbt().getBoolean("Tracker")) {
            n = true;
        }

        return n;
    }
    
    private boolean holdingTracker() {
        boolean n = false;
        if (this.getMainHandStack().getNbt() != null && this.getMainHandStack().getNbt().getBoolean("Tracker") && this.getMainHandStack().getNbt().getCompound("Info").contains("Name")) {
            n = true;
        } else if (this.getOffHandStack().getNbt() != null && this.getOffHandStack().getNbt().getBoolean("Tracker") && this.getOffHandStack().getNbt().getCompound("Info").contains("Name")) {
            n = true;
        }
        return n;
    }

    //玩家列表的名字
//    @Inject(method = "getPlayerListName", at = @At("TAIL"), cancellable = true)
//    private void replacePlayerListName(CallbackInfoReturnable<Text> cir) {
//        try {
//            if (this.getScoreboardTeam() != null) {
//
//                Team team = server.getScoreboard().getTeam(this.getScoreboardTeam().getName());
//
//                MutableText mutableText = (new LiteralText("")).append(team.getFormattedName()).append(this.getName());
//
//                Formatting formatting = team.getColor();
//                if (formatting != Formatting.RESET) {
//                    mutableText.formatted(formatting);
//                } else if (team.getName().equals("hunters")) {
//                    mutableText.formatted(Manhunt.huntersColor);
//                } else if (team.getName().equals("runners")) {
//                    mutableText.formatted(Manhunt.runnersColor);
//                }
//
//                cir.setReturnValue(mutableText);
//
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
