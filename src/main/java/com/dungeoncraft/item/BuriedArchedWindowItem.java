package com.dungeoncraft.item;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.ArchedWindowBlock;
import com.dungeoncraft.block.ArchedWindowBuried;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/*
 * IMPORT MANAGEMENT NOTE
 *
 * Compare these imports with imports already present in your file.
 * Do not add duplicate imports.
 * Remove duplicate or unused imports after adapting this file.
 */
public class BuriedArchedWindowItem extends Item {

    /*
     * Determines the buried state applied to both halves when this
     * temporary command item places an Arched Window.
     */
    private final ArchedWindowBuried startingBuriedState;

    public BuriedArchedWindowItem(
            ArchedWindowBuried startingBuriedState,
            Properties properties
    ) {
        super(properties);
        this.startingBuriedState = startingBuriedState;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPlaceContext placeContext =
                new BlockPlaceContext(context);

        Level level =
                placeContext.getLevel();

        BlockPos lowerPos =
                placeContext.getClickedPos();

        BlockPos upperPos =
                lowerPos.above();

        /*
         * Both spaces must be available before either half is placed.
         */
        if (
                !level.getBlockState(lowerPos)
                        .canBeReplaced(placeContext)
        ) {
            return InteractionResult.FAIL;
        }

        if (
                !level.getBlockState(upperPos)
                        .canBeReplaced(placeContext)
        ) {
            return InteractionResult.FAIL;
        }

        Player player =
                placeContext.getPlayer();

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
                placeContext.getHorizontalDirection()
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
                                startingBuriedState
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
                                startingBuriedState
                        );

        boolean lowerPlaced =
                level.setBlock(
                        lowerPos,
                        lowerState,
                        3
                );

        if (!lowerPlaced) {
            return InteractionResult.FAIL;
        }

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

        if (
                player != null
                        && !player.getAbilities().instabuild
        ) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}