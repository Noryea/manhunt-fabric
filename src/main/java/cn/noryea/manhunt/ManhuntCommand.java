package cn.noryea.manhunt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TeamArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

import java.util.Collection;
import java.util.Iterator;

public class ManhuntCommand {
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)(LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("mh").requires((source) -> {
            return source.hasPermissionLevel(0);
        })).then(CommandManager.literal("join").then(CommandManager.argument("team", TeamArgumentType.team()).executes((context) -> {
            return executeJoin((ServerCommandSource)context.getSource(), TeamArgumentType.getTeam(context, "team"));
        })))).then(CommandManager.literal("cure").then(CommandManager.argument("targets", EntityArgumentType.players()).executes((context) -> {
            return executeCure((ServerCommandSource)context.getSource(), EntityArgumentType.getPlayers(context, "targets"));
        }))).then(CommandManager.literal("freezeAllHunters").then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 100)).executes((context) -> {
            return executeFreeze((ServerCommandSource)context.getSource(), IntegerArgumentType.getInteger(context, "seconds"));
        }))));

    }

    private static int executeJoin(ServerCommandSource source, Team team) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        scoreboard.addPlayerToTeam(source.getPlayer().getName().asString(), team);
        source.sendFeedback(new TranslatableText("commands.team.join.success.single", source.getPlayer().getName(), team.getFormattedName()), true);

        return 1;
    }

    private static int executeCure(ServerCommandSource source, Collection<? extends Entity> targets) {
        if (source.hasPermissionLevel(2)) {
            Iterator var3 = targets.iterator();

            while (var3.hasNext()) {
                ServerPlayerEntity player = (ServerPlayerEntity) var3.next();

                player.clearStatusEffects();
                player.setHealth(player.getMaxHealth());
                player.getHungerManager().setFoodLevel(20);
                player.getHungerManager().setSaturationLevel(8.5F);

            }
            source.sendFeedback(new LiteralText("已治愈" + targets.size() + "名玩家"), true);
            return targets.size();

        } else {
            source.sendFeedback(new LiteralText("\u00a7c宁不配"), false);
            return 0;
        }
    }

    private static int executeFreeze(ServerCommandSource source, int time) throws CommandSyntaxException {
        if (source.hasPermissionLevel(2)) {

            MinecraftServer server = source.getEntityOrThrow().getServer();
            Iterator<ServerPlayerEntity> vec3 = server.getPlayerManager().getPlayerList().listIterator();

            while (vec3.hasNext()) {

                ServerPlayerEntity player = vec3.next();

                if (player.isTeamPlayer( server.getScoreboard().getTeam("hunters") )) {

                    player.clearStatusEffects();
                    player.setHealth(player.getMaxHealth());
                    player.getHungerManager().setFoodLevel(20);
                    player.getHungerManager().setSaturationLevel(8.5F);

                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, time * 20, 255, false, true));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, time * 20, 255, false, false));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, time * 20, 248, false, false));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, (time - 1) * 20, 255, false, false));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, time * 20, 255, false, false));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, time * 20, 255, false, false));

                }
            }

            source.sendFeedback(new LiteralText("\u00a7f猎人将在\u00a7b" + time + "\u00a7f秒内原地不动"), true);

            return 1;

        } else {
            source.sendFeedback(new LiteralText("\u00a7c宁不配"), false);
            return 0;
        }
    }
}
