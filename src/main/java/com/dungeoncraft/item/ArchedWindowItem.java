package com.dungeoncraft.item;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.ArchedWindowBlock;
import com.dungeoncraft.block.ArchedWindowBuried;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

public class ArchedWindowItem extends BlockItem {

    /*
     * IMPORT MANAGEMENT NOTE
     *
     * Compare these imports with those already present in your file.
     * Do not add duplicate imports.
     * Remove duplicate or unused imports after adapting this class.
     * Conflicting imports can cause compilation errors.
     */

    public ArchedWindowItem(Properties properties) {
        super(
                DungeonCraft.ARCHED_WINDOW,
                properties
        );
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level =
                context.getLevel();

        BlockPos lowerPos =
                context.getClickedPos();

        BlockPos upperPos =
                lowerPos.above();

        /*
         * Validate both spaces before changing either one.
         */
        if (
                !level.getBlockState(lowerPos)
                        .canBeReplaced(context)
        ) {
            return InteractionResult.FAIL;
        }

        if (
                !level.getBlockState(upperPos)
                        .canBeReplaced(context)
        ) {
            return InteractionResult.FAIL;
        }

        Player player =
                context.getPlayer();

        /*
         * Prevent placement through the player's body.
         */
        if (player != null) {
            AABB lowerBox =
                    new AABB(lowerPos);

            AABB upperBox =
                    new AABB(upperPos);

            if (
                    player.getBoundingBox().intersects(lowerBox)
                            || player.getBoundingBox().intersects(upperBox)
            ) {
                return InteractionResult.FAIL;
            }
        }

        Direction facing =
                context.getHorizontalDirection()
                        .getOpposite();

        BlockState lowerState =
                DungeonCraft.ARCHED_WINDOW
                        .defaultBlockState()
                        .setValue(
                                ArchedWindowBlock.FACING,
                                facing
                        )
                        .setValue(
                                ArchedWindowBlock.HALF,
                                DoubleBlockHalf.LOWER
                        )
                        .setValue(
                                ArchedWindowBlock.BURIED,
                                ArchedWindowBuried.CLEAR
                        );

        BlockState upperState =
                DungeonCraft.ARCHED_WINDOW
                        .defaultBlockState()
                        .setValue(
                                ArchedWindowBlock.FACING,
                                facing
                        )
                        .setValue(
                                ArchedWindowBlock.HALF,
                                DoubleBlockHalf.UPPER
                        )
                        .setValue(
                                ArchedWindowBlock.BURIED,
                                ArchedWindowBuried.CLEAR
                        );

        /*
         * Place the lower half first.
         */
        boolean lowerPlaced =
                level.setBlock(
                        lowerPos,
                        lowerState,
                        3
                );

        if (!lowerPlaced) {
            return InteractionResult.FAIL;
        }

        /*
         * Place the upper half.
         *
         * If this unexpectedly fails, remove the lower half so an
         * incomplete Arched Window is not left behind.
         */
        boolean upperPlaced =
                level.setBlock(
                        upperPos,
                        upperState,
                        3
                );

        if (!upperPlaced) {
            level.removeBlock(
                    lowerPos,
                    false
            );

            return InteractionResult.FAIL;
        }

        /*
         * Consume one item outside Creative mode.
         */
        if (
                player != null
                        && !player.getAbilities().instabuild
        ) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}