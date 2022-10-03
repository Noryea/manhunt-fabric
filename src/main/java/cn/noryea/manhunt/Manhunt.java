package cn.noryea.manhunt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.LinkedList;
import java.util.List;

public class Manhunt implements ModInitializer {

    public static List<ServerPlayerEntity> allPlayers;
    public static List<ServerPlayerEntity> allRunners;

    public static final Formatting huntersColor = Formatting.RED;
    public static final Formatting runnersColor = Formatting.GREEN;
    public static int delay = 0;

    @Override
    public void onInitialize() {

        ServerTickEvents.START_WORLD_TICK.register((world) -> {
            world.getServer().getCommandManager().executeWithPrefix(world.getServer().getCommandSource().withSilent(), "kill @e[type=item,nbt={Item:{tag:{Tracker:1b}}}]");

            Scoreboard scoreboard = world.getServer().getScoreboard();
            if (scoreboard.getTeam("hunters") == null) {
                Team team = scoreboard.addTeam("hunters");
                team.setDisplayName(Text.translatable("manhunt.teams.hunters.name"));
                team.setColor(huntersColor);
            }

            if (scoreboard.getTeam("runners") == null) {
                Team team = scoreboard.addTeam("runners");
                team.setDisplayName(Text.translatable("manhunt.teams.runners.name"));
                team.setColor(runnersColor);
            }

            allPlayers = world.getServer().getPlayerManager().getPlayerList();
            allRunners = new LinkedList<>();

            Team runners = scoreboard.getTeam("runners");
            for (ServerPlayerEntity x : allPlayers) {
                if (x != null) {
                    if (x.isTeamPlayer(runners)) {
                        allRunners.add(x);
                    }
                }
            }

        });

        CommandRegistrationCallback.EVENT.register(ManhuntCommand::registerCommands);

    }
}
