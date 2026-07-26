package com.dungeoncraft.item;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.FlankedColumnBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FlankedColumnItem extends Item {

    public FlankedColumnItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        Direction clickedFace = context.getClickedFace();
        BlockPos clickedPos = context.getClickedPos();

        // The new block is placed beside the face that was clicked.
        BlockPos placePos = clickedPos.relative(clickedFace);

        BlockPlaceContext placeContext =
                new BlockPlaceContext(context);

        // Stop if the destination cannot be replaced.
        if (!level.getBlockState(placePos).canBeReplaced(placeContext)) {
            return InteractionResult.FAIL;
        }

        BlockState clickedState =
                level.getBlockState(clickedPos);

        /*
         * Checks whether the block directly clicked by the player is part
         * of either the ordinary or Flanked Column family.
         *
         * Clicking an existing column piece continues the column using
         * a regular shaft instead of creating another base or top.
         */
        boolean clickedColumnPiece =
                isColumnBlock(clickedState);

        /*
         * Holding Shift overrides every automatic base/top check.
         * It always places a regular shaft.
         */
        boolean forceShaft =
                player != null && player.isShiftKeyDown();

        FlankedColumnBlock.ColumnPart part;

        if (forceShaft || clickedColumnPiece) {

            /*
             * Shift placement or placement against an existing column
             * always creates the regular shaft.
             */
            part = FlankedColumnBlock.ColumnPart.SHAFT;

        } else if (clickedFace == Direction.UP) {

            /*
             * The player directly clicked the top face of a non-column
             * block, so place a base.
             */
            part = FlankedColumnBlock.ColumnPart.BASE;

        } else if (clickedFace == Direction.DOWN) {

            /*
             * The player directly clicked the underside of a non-column
             * block, so place a top.
             */
            part = FlankedColumnBlock.ColumnPart.TOP;

        } else {

            /*
             * The player clicked the side of a wall or another block.
             *
             * Check the block underneath and above the destination to
             * determine whether this should be a base or top.
             */
            boolean hasBaseSupport =
                    hasNonColumnSupport(
                            level,
                            placePos.below(),
                            Direction.UP
                    );

            boolean hasTopSupport =
                    hasNonColumnSupport(
                            level,
                            placePos.above(),
                            Direction.DOWN
                    );

            if (hasBaseSupport && !hasTopSupport) {

                // A sturdy non-column block is directly beneath it.
                part = FlankedColumnBlock.ColumnPart.BASE;

            } else if (hasTopSupport && !hasBaseSupport) {

                // A sturdy non-column block is directly above it.
                part = FlankedColumnBlock.ColumnPart.TOP;

            } else {

                /*
                 * Use a regular shaft when:
                 *
                 * - neither support exists; or
                 * - both a floor and ceiling touch this one block space.
                 *
                 * Shift can also always be used to force this result.
                 */
                part = FlankedColumnBlock.ColumnPart.SHAFT;
            }
        }

        FlankedColumnBlock.FlankAxis flankAxis =
                FlankedColumnBlock.detectFlankAxis(
                        level,
                        placePos
                );

        BlockState stateToPlace =
                DungeonCraft.FLANKED_COLUMN.defaultBlockState()
                        .setValue(FlankedColumnBlock.PART, part)
                        .setValue(
                                FlankedColumnBlock.FLANK_AXIS,
                                flankAxis
                        );

        boolean placed =
                level.setBlock(placePos, stateToPlace, 3);

        if (!placed) {
            return InteractionResult.FAIL;
        }

        // Consume one item outside Creative mode.
        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /*
     * Returns true when the supplied state belongs to either column
     * family.
     *
     * These blocks must not trigger automatic base or top placement.
     */
    private static boolean isColumnBlock(BlockState state) {
        return state.is(DungeonCraft.COLUMN)
                || state.is(DungeonCraft.COLUMN_BASE)
                || state.is(DungeonCraft.COLUMN_TOP)
                || state.is(DungeonCraft.FLANKED_COLUMN);
    }

    /*
     * Checks whether the specified neighboring block provides a
     * sturdy supporting face toward the placement location.
     *
     * It must also be a non-column block.
     *
     * For the block below:
     * faceTowardColumn is UP.
     *
     * For the block above:
     * faceTowardColumn is DOWN.
     */
    private static boolean hasNonColumnSupport(
            Level level,
            BlockPos supportPos,
            Direction faceTowardColumn
    ) {
        BlockState supportState =
                level.getBlockState(supportPos);

        if (isColumnBlock(supportState)) {
            return false;
        }

        return supportState.isFaceSturdy(
                level,
                supportPos,
                faceTowardColumn
        );
    }
}