package cn.noryea.manhunt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TeamArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

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
        }))));

    }

    private static int executeJoin(ServerCommandSource source, Team team) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        scoreboard.addPlayerToTeam(source.getEntity().getName().asString(), team);
        source.sendFeedback(new TranslatableText("commands.team.join.success.single", source.getEntity().getName(), team.getFormattedName()), true);

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
}
