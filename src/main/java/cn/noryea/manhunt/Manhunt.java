package cn.noryea.manhunt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class Manhunt implements ModInitializer {

  public static List<ServerPlayerEntity> allPlayers;
  public static List<ServerPlayerEntity> allRunners;

  public static Logger LOGGER = LoggerFactory.getLogger("manhunt");

  @Override
  public void onInitialize() {
    ManhuntConfig config = ManhuntConfig.INSTANCE;
    config.load();
    ServerTickEvents.START_WORLD_TICK.register((world) -> {
      world.getServer().getCommandManager().executeWithPrefix(world.getServer().getCommandSource().withSilent(), "kill @e[type=item,nbt={Item:{tag:{Tracker:1b}}}]");

      Scoreboard scoreboard = world.getServer().getScoreboard();
      if (scoreboard.getTeam("hunters") == null) {
        Team team = scoreboard.addTeam("hunters");
        team.setDisplayName(Text.translatable("manhunt.teams.hunters.name"));
        team.setCollisionRule(AbstractTeam.CollisionRule.ALWAYS);
        team.setShowFriendlyInvisibles(false);
      }
      scoreboard.getTeam("hunters").setColor(config.getHuntersColor());

      if (scoreboard.getTeam("runners") == null) {
        Team team = scoreboard.addTeam("runners");
        team.setDisplayName(Text.translatable("manhunt.teams.runners.name"));
        team.setCollisionRule(AbstractTeam.CollisionRule.ALWAYS);
        team.setShowFriendlyInvisibles(false);
      }
      scoreboard.getTeam("runners").setColor(config.getRunnersColor());

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
