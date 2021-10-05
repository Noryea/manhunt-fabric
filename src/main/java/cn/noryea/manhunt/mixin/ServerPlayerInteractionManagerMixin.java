package cn.noryea.manhunt.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
	@Shadow
	public ServerPlayerEntity player;

	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"), cancellable = true)
	public void processBlockBreakingAction(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, CallbackInfo ci) {
		if (action.equals(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK)) {
			selectTrackedPlayer(this.player, this.player.getMainHandStack().getOrCreateNbt());
		}
	}

	@Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
	public void tryBreakBlock(BlockPos pos, CallbackInfoReturnable<ActionResult> ci) {
		if (this.player.isCreative())  {	//创造测试用
			selectTrackedPlayer(this.player, this.player.getMainHandStack().getOrCreateNbt());
		}
	}

	private void selectTrackedPlayer(ServerPlayerEntity player, NbtCompound mainHand) {
		if (mainHand.getBoolean("Tracker") && player.isTeamPlayer(player.getWorld().getServer().getScoreboard().getTeam("hunters"))) {
			if (!mainHand.contains("Info")) {
				mainHand.put("Info",new NbtCompound());
			}
			String old = mainHand.getCompound("Info").getString("Name");

			List<ServerPlayerEntity> players = player.getWorld().getPlayers();
			int i;
			for(i = 0; i < players.size(); ++i) {
				if (players.get(i).isTeamPlayer(player.getServer().getScoreboard().getTeam("runners"))) {
					String x = players.get(i).getName().asString();
					if (!Objects.equals(x, old)) {
						mainHand.getCompound("Info").putString("Name", x);
						updateCompass(player, mainHand, players.get(i));
						player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 0.75f ,0.8f));
						break;
					}
				}
			}
		}
	}

	@Inject(method = "interactItem(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/World;" + "Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
	public void interactItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cbi) {
		if(stack.getOrCreateNbt().getBoolean("Tracker") && !player.isSpectator() && player.isTeamPlayer(world.getScoreboard().getTeam("hunters"))) {
			Iterator<ServerPlayerEntity> players = player.getWorld().getServer().getPlayerManager().getPlayerList().iterator();
			while(players.hasNext()){
				if (hand.equals(Hand.OFF_HAND)) {
					players.next().networkHandler.sendPacket(new EntityAnimationS2CPacket(player, EntityAnimationS2CPacket.SWING_OFF_HAND));
				} else {
					players.next().networkHandler.sendPacket(new EntityAnimationS2CPacket(player, EntityAnimationS2CPacket.SWING_MAIN_HAND));
				}
			}
			player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 0.85f ,0.95f));
			ServerPlayerEntity trackedPlayer = world.getServer().getPlayerManager().getPlayer(stack.getOrCreateNbt().getCompound("Info").getString("Name"));
			if (trackedPlayer != null) {
				updateCompass(player, stack.getOrCreateNbt(), trackedPlayer);	//更新指南针
			}
		}
	}

	private void updateCompass(ServerPlayerEntity player, NbtCompound nbt, ServerPlayerEntity trackedPlayer) {
		nbt.remove("LodestonePos");
		nbt.remove("LodestoneDimension");
		nbt.putBoolean("LodestoneTracked", false);
		NbtCompound playerTag = trackedPlayer.writeNbt(new NbtCompound());
		NbtList positions = playerTag.getList("Positions",10);
		int i;
		for(i = 0; i < positions.size(); ++i) {
			NbtCompound compound = positions.getCompound(i);
			if (Objects.equals(compound.getString("LodestoneDimension"), player.writeNbt(new NbtCompound()).getString("Dimension"))) {
				nbt.copyFrom(compound);
				break;
			}
		}
		nbt.put("Info", new NbtCompound());
		NbtCompound info = nbt.getCompound("Info");
		info.putString("Name", trackedPlayer.getEntityName());
		info.putFloat("Health", trackedPlayer.getHealth());
		info.putLong("LastUpdateTime", player.getWorld().getTime());
	}
}