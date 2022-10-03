package cn.noryea.manhunt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TeamArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public class ManhuntCommand {
    @SuppressWarnings("unused")
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dis, CommandRegistryAccess reg, CommandManager.RegistrationEnvironment env) {

        dis.register(CommandManager.literal("mh")
            .then(CommandManager.literal("join")
                .then(CommandManager.argument("team", TeamArgumentType.team())
                    .executes((ctx) -> executeJoin(ctx.getSource(), TeamArgumentType.getTeam(ctx, "team")))))
            .then(CommandManager.literal("cure").requires((src) -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .executes((ctx) -> executeCure(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
            .then(CommandManager.literal("freeze").requires((src) -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 120))
                    .executes((ctx) -> executeFreeze(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds")))))
            .then(CommandManager.literal("compassDelay").requires((src) -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0, 120))
                    .executes((ctx) -> executeCompassDelay(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds"))))));
    }

    private static int executeJoin(ServerCommandSource source, Team team) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        scoreboard.addPlayerToTeam(source.getPlayer().getName().getString(), team);
        source.sendFeedback(Text.translatable("commands.team.join.success.single", source.getPlayer().getName(), team.getFormattedName()), true);

        return 1;
    }

    private static int executeCompassDelay(ServerCommandSource source, Integer delay) {
        Manhunt.delay = delay;
        source.sendFeedback(Text.of("Set delay to: " + delay + " seconds"), true);

        return 1;
    }

    private static int executeCure(ServerCommandSource source, Collection<? extends Entity> targets) {
        for (Entity target : targets) {
            ServerPlayerEntity player = (ServerPlayerEntity) target;

            player.clearStatusEffects();
            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);
            player.getHungerManager().setSaturationLevel(8.5F);

        }
        source.sendFeedback(Text.of("Cured " + targets.size() + " targets"), true);
        return targets.size();
    }

    private static int executeFreeze(ServerCommandSource source, int time) throws CommandSyntaxException {
        MinecraftServer server = source.getEntityOrThrow().getServer();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            if (player.isTeamPlayer(server.getScoreboard().getTeam("hunters"))) {

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

        source.sendFeedback(Text.of("\u00a7fHunters will be frozen for \u00a7b" + time + "\u00a7f seconds"), true);

        return 1;
    }
}
