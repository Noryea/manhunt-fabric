package cn.noryea.manhunt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.LinkedList;
import java.util.List;

public class Manhunt implements ModInitializer {

    public static List<ServerPlayerEntity> allPlayers;
    public static List<ServerPlayerEntity> allRunners;

    @Override
    public void onInitialize() {

        ServerTickEvents.START_WORLD_TICK.register((world) -> {

            //删除追踪器实体
            world.getServer().getCommandManager().execute(world.getServer().getCommandSource().withSilent(),"kill @e[type=item,nbt={Item:{tag:{Tracker:1b}}}]");

            //创建队伍
            Scoreboard scoreboard = world.getServer().getScoreboard();
            if (scoreboard.getTeam("hunters") == null) {
                Team team = scoreboard.addTeam("hunters");
                team.setColor(Formatting.RED);
                team.setDisplayName(new LiteralText("\u00a7l\u00a7c猎人"));
            }

            if (scoreboard.getTeam("runners") == null) {
                Team team= scoreboard.addTeam("runners");
                team.setColor(Formatting.DARK_GREEN);
                team.setDisplayName(new LiteralText("\u00a7l\u00a72逃者"));
            }

            //获取玩家列表
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

        //命令注册
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            ManhuntCommand.registerCommands(dispatcher);
        });

    }
}
