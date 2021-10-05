package cn.noryea.manhunt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;

public class Main implements ModInitializer {
    @Override
    public void onInitialize() {

        //基础队伍
        ServerTickEvents.START_WORLD_TICK.register((world) -> {
            Scoreboard scoreboard = world.getServer().getScoreboard();
            if (scoreboard.getTeam("hunters") == null) {
                Team team = scoreboard.addTeam("hunters");
                team.setColor(Formatting.DARK_AQUA);
            }

            if (scoreboard.getTeam("runners") == null) {
                Team team= scoreboard.addTeam("runners");
                team.setColor(Formatting.RED);
            }
        });

        //命令注册
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            ManhuntCommand.registerCommands(dispatcher);
        });
    }
}
