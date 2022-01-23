package cn.noryea.manhunt.mixin;

import cn.noryea.manhunt.Manhunt;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
	@Shadow
	protected ServerPlayerEntity player;


	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"), cancellable = true)
	public void processBlockBreakingAction(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, CallbackInfo ci) {
		if (action.equals(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK)) {
			cycleTrackedPlayer(this.player, this.player.getMainHandStack().getOrCreateNbt());
		}
	}

	@Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
	public void tryBreakBlock(BlockPos pos, CallbackInfoReturnable<ActionResult> ci) {
		cycleTrackedPlayer(this.player, this.player.getMainHandStack().getOrCreateNbt());
	}

	@Inject(method = "interactItem(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
	public void interactItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cbi) {
		if(stack.getOrCreateNbt().getBoolean("Tracker") && !player.isSpectator() && player.isTeamPlayer(world.getScoreboard().getTeam("hunters"))) {
			if (!stack.getOrCreateNbt().contains("Info")) {
				stack.getOrCreateNbt().put("Info",new NbtCompound());
			}
			NbtCompound info = stack.getOrCreateNbt().getCompound("Info");

			if (!info.contains("Name", NbtElement.STRING_TYPE) && !Manhunt.allRunners.isEmpty()) {
				info.putString("Name", Manhunt.allRunners.get(0).getName().asString());
			}

			ServerPlayerEntity trackedPlayer = world.getServer().getPlayerManager().getPlayer(info.getString("Name"));

			if (trackedPlayer != null) {
				player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 0.85f ,0.95f));
				updateCompass(player, stack.getOrCreateNbt(), trackedPlayer);
			}
		}
	}

	//循环选择目标
	private void cycleTrackedPlayer(ServerPlayerEntity player, NbtCompound stackNbt) {
		if (stackNbt.getBoolean("Tracker") && player.isTeamPlayer(player.getServer().getScoreboard().getTeam("hunters"))) {

			if (!stackNbt.contains("Info")) {
				stackNbt.put("Info",new NbtCompound());
			}

			int next;
			int previous = -1;
			NbtCompound info = stackNbt.getCompound("Info");

			if (Manhunt.allRunners.isEmpty()) { player.sendMessage(new LiteralText("\u00a7c目标队伍成员为空"), MessageType.GAME_INFO, Util.NIL_UUID); }
			else {
				//旧的目标序号
				for (int i = 0; i < Manhunt.allRunners.size(); i++) {
					ServerPlayerEntity x = Manhunt.allRunners.get(i);
					if (x != null) {
						if (Objects.equals(x.getName().asString(), info.getString("Name"))) {
							previous = i;
						}
					}
				}

				//切换目标
				if (previous + 1 >= Manhunt.allRunners.size()) {
					next = 0;
				} else {
					next = previous + 1;
				}

				if (previous != next) {
					updateCompass(player, stackNbt, Manhunt.allRunners.get(next));
					player.sendMessage(new LiteralText("\u00a7a目标切换至: " + Manhunt.allRunners.get(next).getName().asString()), MessageType.CHAT, Util.NIL_UUID);
				}

			}

		}
	}

	//更新指南针
	private void updateCompass(ServerPlayerEntity player, NbtCompound nbt, ServerPlayerEntity trackedPlayer) {
		nbt.remove("LodestonePos");
		nbt.remove("LodestoneDimension");
		nbt.put("Info", new NbtCompound());

		if (Objects.equals(trackedPlayer.getScoreboardTeam().getName(), "runners")) {
			NbtCompound playerTag = trackedPlayer.writeNbt(new NbtCompound());
			NbtList positions = playerTag.getList("Positions", 10);
			int i;
			for (i = 0; i < positions.size(); ++i) {
				NbtCompound compound = positions.getCompound(i);
				if (Objects.equals(compound.getString("LodestoneDimension"), player.writeNbt(new NbtCompound()).getString("Dimension"))) {
					nbt.copyFrom(compound);
					break;
				}
			}

			NbtCompound info = nbt.getCompound("Info");
			info.putLong("LastUpdateTime", player.getWorld().getTime());
			info.putString("Name", trackedPlayer.getEntityName());
			info.putString("Dimension", playerTag.getString("Dimension"));
		}

	}
}