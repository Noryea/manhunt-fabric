package cn.noryea.manhunt.mixin;

import cn.noryea.manhunt.Manhunt;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

import static cn.noryea.manhunt.Manhunt.delay;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
	@Final @Shadow protected ServerPlayerEntity player;

	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"))
	public void processBlockBreakingAction(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
		if (action.equals(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK)) {
			cycleTrackedPlayer(this.player, this.player.getMainHandStack().getNbt());
		}
	}

	@Inject(method = "tryBreakBlock", at = @At("HEAD"))
	public void tryBreakBlock(BlockPos pos, CallbackInfoReturnable<ActionResult> ci) {
		cycleTrackedPlayer(this.player, this.player.getMainHandStack().getNbt());
	}

	@Inject(
			method = "interactItem",
			at = @At(
					target = "Lnet/minecraft/item/ItemStack;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;",
					value = "INVOKE"
			))
	public void interactItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cbi) {
		player.getItemCooldownManager().set(stack.getItem(), delay * 20);
		if(stack.getNbt() != null && stack.getNbt().getBoolean("Tracker") && !player.isSpectator() && player.isTeamPlayer(world.getScoreboard().getTeam("hunters"))) {
			if (!stack.getOrCreateNbt().contains("Info")) {
				stack.getOrCreateNbt().put("Info",new NbtCompound());
			}
			NbtCompound info = stack.getOrCreateNbt().getCompound("Info");

			if (!info.contains("Name", NbtElement.STRING_TYPE) && !Manhunt.allRunners.isEmpty()) {
				info.putString("Name", Manhunt.allRunners.get(0).getName().getString());
			}

			ServerPlayerEntity trackedPlayer = world.getServer().getPlayerManager().getPlayer(info.getString("Name"));

			if (trackedPlayer != null) {
				player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 0.85f, 0.95f, 0));
				updateCompass(player, stack.getOrCreateNbt(), trackedPlayer);
			}
		}
	}

	private void cycleTrackedPlayer(ServerPlayerEntity player, @Nullable NbtCompound stackNbt) {
		if (stackNbt != null && stackNbt.getBoolean("Tracker") && player.isTeamPlayer(player.getServer().getScoreboard().getTeam("hunters"))) {
			if (!stackNbt.contains("Info")) {
				stackNbt.put("Info",new NbtCompound());
			}

			int next;
			int previous = -1;
			NbtCompound info = stackNbt.getCompound("Info");

			if (Manhunt.allRunners.isEmpty()) { player.sendMessage(Text.of("\u00a7cNo runners")); }
			else {
				for (int i = 0; i < Manhunt.allRunners.size(); i++) {
					ServerPlayerEntity x = Manhunt.allRunners.get(i);
					if (x != null) {
						if (Objects.equals(x.getName().getString(), info.getString("Name"))) {
							previous = i;
						}
					}
				}

				if (previous + 1 >= Manhunt.allRunners.size()) {
					next = 0;
				} else {
					next = previous + 1;
				}

				if (previous != next) {
					updateCompass(player, stackNbt, Manhunt.allRunners.get(next));
					player.sendMessage(Text.of("\u00a7aSwitched runner to: " + Manhunt.allRunners.get(next).getName().getString()));
				}

			}

		}
	}

	private void updateCompass(ServerPlayerEntity player, NbtCompound nbt, ServerPlayerEntity trackedPlayer) {
		nbt.remove("LodestonePos");
		nbt.remove("LodestoneDimension");

		nbt.put("Info", new NbtCompound());
		if (trackedPlayer.getScoreboardTeam() != null && Objects.equals(trackedPlayer.getScoreboardTeam().getName(), "runners")) {
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
