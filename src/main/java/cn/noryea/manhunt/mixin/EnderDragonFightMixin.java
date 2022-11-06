package cn.noryea.manhunt.mixin;

import cn.noryea.manhunt.ManhuntConfig;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public class EnderDragonFightMixin {

  //End the game when runners kill the enderdragon
  @Inject(method = "dragonKilled", at = @At(target = "Lnet/minecraft/entity/boss/ServerBossBar;setPercent(F)V", value = "INVOKE"))
  private void runnersWon(EnderDragonEntity dragon, CallbackInfo ci) {
    MinecraftServer server = dragon.getServer();
    if(ManhuntConfig.INSTANCE.isRunnersWinOnDragonDeath() && !server.getScoreboard().getTeam("runners").getPlayerList().isEmpty()) {
      server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent().withLevel(2), "title @a subtitle {\"translate\":\"manhunt.win.runners.subtitle\",\"color\":\"white\"}");
      server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent().withLevel(2), "title @a title {\"translate\":\"manhunt.win.runners.title\",\"color\":\"white\"}");
    }
  }
}
