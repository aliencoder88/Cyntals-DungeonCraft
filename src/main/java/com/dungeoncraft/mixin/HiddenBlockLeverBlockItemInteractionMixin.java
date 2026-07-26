package com.dungeoncraft.mixin;

import com.dungeoncraft.block.HiddenBlockLeverBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Preserves Hidden Block Lever interaction while the player is crouching and
 * holding a placeable block. In that situation the item placement path can run
 * without the block's ordinary held-item callback getting the final say.
 */
@Mixin(BlockItem.class)
public abstract class HiddenBlockLeverBlockItemInteractionMixin {

    @Inject(
            method =
                    "useOn("
                    + "Lnet/minecraft/world/item/context/UseOnContext;"
                    + ")Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dungeoncraft$routeHiddenLeverInteractionBeforePlacement(
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        Player player = context.getPlayer();
        if (player == null) {
            return;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof HiddenBlockLeverBlock hiddenLever)) {
            return;
        }

        BlockHitResult hitResult = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                pos,
                context.isInside()
        );

        InteractionResult result = hiddenLever.handleBlockItemPlacementBypass(
                state,
                level,
                pos,
                player,
                hitResult
        );

        if (result != InteractionResult.PASS) {
            cir.setReturnValue(result);
        }
    }
}
