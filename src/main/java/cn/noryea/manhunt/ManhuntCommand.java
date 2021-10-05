package cn.noryea.manhunt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.TeamArgumentType;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;

public class ManhuntCommand {
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)(LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("mh").requires((source) -> {
            return source.hasPermissionLevel(0);
        })).then(CommandManager.literal("join").then(CommandManager.argument("team", TeamArgumentType.team()).executes((context) -> {
            return executeJoin((ServerCommandSource)context.getSource(), TeamArgumentType.getTeam(context, "team"));
        }))));
    }

    private static int executeJoin(ServerCommandSource source, Team team) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        scoreboard.addPlayerToTeam(source.getEntity().getName().asString(), team);
        source.sendFeedback(new TranslatableText("commands.team.join.success.single", source.getEntity().getName(), team.getFormattedName()), true);

        return 1;
    }
}
