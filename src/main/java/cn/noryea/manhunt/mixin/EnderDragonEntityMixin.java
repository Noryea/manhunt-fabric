package cn.noryea.manhunt.mixin;

import cn.noryea.manhunt.ManhuntConfig;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonEntity.class)
public class EnderDragonEntityMixin {

  //End the game when runners kill the enderdragon
  @Inject(method = "updatePostDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
  private void runnersWon(CallbackInfo ci) {
    EnderDragonEntity dragon = ((EnderDragonEntity)(Object) this);
    MinecraftServer server = dragon.getServer();
    if(ManhuntConfig.INSTANCE.isRunnersWinOnDragonDeath() && !server.getScoreboard().getTeam("runners").getPlayerList().isEmpty() && dragon.ticksSinceDeath == 1) {
      server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent().withLevel(2), "title @a subtitle {\"translate\":\"manhunt.win.runners.subtitle\",\"color\":\"white\"}");
      server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent().withLevel(2), "title @a title {\"translate\":\"manhunt.win.runners.title\",\"color\":\"white\"}");
    }
  }
}
