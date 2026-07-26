package com.dungeoncraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;

/*
 * IMPORT MANAGEMENT NOTE
 *
 * Compare these imports with those already present in your file.
 * Do not add duplicate imports.
 * Remove duplicate or unused imports after adapting this class.
 *
 * Duplicate, unused, or conflicting imports may be highlighted by
 * IntelliJ and can make the file harder to maintain.
 */
public class ArchedWindowBlock extends Block {

    /*
     * Direction the front of the Arched Window faces.
     */
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    /*
     * Whether this block position contains the lower or upper half.
     */
    public static final EnumProperty<DoubleBlockHalf> HALF =
            BlockStateProperties.DOUBLE_BLOCK_HALF;

    /*
     * Which broad sides currently contain packed dirt.
     *
     * Possible saved values:
     *
     * clear
     * front
     * back
     * both
     */
    public static final EnumProperty<ArchedWindowBuried> BURIED =
            EnumProperty.create(
                    "buried",
                    ArchedWindowBuried.class
            );

    public ArchedWindowBlock(Properties properties) {
        super(properties);

        /*
         * A newly created Arched Window begins as:
         *
         * - facing north;
         * - the lower half;
         * - completely clear of packed dirt.
         */
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                HALF,
                                DoubleBlockHalf.LOWER
                        )
                        .setValue(
                                BURIED,
                                ArchedWindowBuried.CLEAR
                        )
        );
    }

    /*
     * Direct block placement defaults to the clear lower half.
     *
     * Normal item placement will be handled by ArchedWindowItem,
     * which places both halves together.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                                .getOpposite()
                )
                .setValue(
                        HALF,
                        DoubleBlockHalf.LOWER
                )
                .setValue(
                        BURIED,
                        ArchedWindowBuried.CLEAR
                );
    }

    /*
     * Packs dirt into the broad side of the Arched Window that the
     * player clicked.
     *
     * The clicked face determines whether the front or back side is
     * The clicked face determines whether the front or back side is
     * being buried. Both the lower and upper halves are updated together.
     */
    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        /*
         * Only coarse dirt performs the burial interaction.
         *
         * Other held items continue their normal behavior.
         */
        if (stack.getItem() != Items.COARSE_DIRT) {
            return InteractionResult.PASS;
        }

        Direction clickedFace =
                hitResult.getDirection();

        Direction frontFace =
                state.getValue(FACING);

        Direction backFace =
                frontFace.getOpposite();

        boolean clickedFront =
                clickedFace == frontFace;

        boolean clickedBack =
                clickedFace == backFace;

        /*
         * Dirt can be packed only through one of the two broad window
         * faces. Clicking the top, bottom, or narrow side does not alter
         * the window.
         */
        if (!clickedFront && !clickedBack) {
            return InteractionResult.PASS;
        }

        BlockPos lowerPos =
                getLowerPosition(
                        pos,
                        state
                );

        BlockPos upperPos =
                lowerPos.above();

        BlockState lowerState =
                level.getBlockState(lowerPos);

        BlockState upperState =
                level.getBlockState(upperPos);

        /*
         * Confirm that this is still one matching two-block Arched Window.
         *
         * This prevents a damaged or command-created partial window from
         * changing an unrelated neighboring block.
         */
        if (
                !lowerState.is(this)
                        || !upperState.is(this)
                        || lowerState.getValue(HALF)
                        != DoubleBlockHalf.LOWER
                        || upperState.getValue(HALF)
                        != DoubleBlockHalf.UPPER
                        || lowerState.getValue(FACING)
                        != upperState.getValue(FACING)
        ) {
            return InteractionResult.PASS;
        }

        ArchedWindowBuried currentBuried =
                lowerState.getValue(BURIED);

        ArchedWindowBuried newBuried =
                clickedFront
                        ? currentBuried.addFrontDirt()
                        : currentBuried.addBackDirt();

        /* This side is already filled with packed coarse dirt.
         * Do not intercept the interaction. Returning PASS allows the held
         * Coarse Dirt block to perform its normal placement behavior in the
         * adjacent space beside the window.
         */
        if (newBuried == currentBuried) {
            return InteractionResult.PASS;
        }

        /*
         * Let the client display a successful interaction, but make the
         * actual block and inventory changes only on the server.
         */
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        level.setBlock(
                lowerPos,
                lowerState.setValue(
                        BURIED,
                        newBuried
                ),
                Block.UPDATE_ALL
        );

        level.setBlock(
                upperPos,
                upperState.setValue(
                        BURIED,
                        newBuried
                ),
                Block.UPDATE_ALL
        );

        /*
         * Consume one dirt block outside Creative mode.
         *
         * One dirt item represents packing one complete side of the
         * two-block-tall window.
         */
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /*
     * Adds gravel reinforcement to an existing packed Coarse Dirt side.
     *
     * Gravel must be applied directly from the dirt-facing side. It cannot
     * be applied by reaching through the iron bars from the open side.
     */
    private InteractionResult reinforceBuriedSide(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        Direction clickedFace =
                hitResult.getDirection();

        Direction frontFace =
                state.getValue(FACING);

        Direction backFace =
                frontFace.getOpposite();

        boolean clickedFront =
                clickedFace == frontFace;

        boolean clickedBack =
                clickedFace == backFace;

        /*
         * Reinforcement can be added only through one of the two broad
         * window faces.
         */
        if (!clickedFront && !clickedBack) {
            return InteractionResult.PASS;
        }

        BlockPos lowerPos =
                getLowerPosition(
                        pos,
                        state
                );

        BlockPos upperPos =
                lowerPos.above();

        BlockState lowerState =
                level.getBlockState(lowerPos);

        BlockState upperState =
                level.getBlockState(upperPos);

        /*
         * Verify that both halves belong to the same complete window.
         */
        if (
                !lowerState.is(this)
                        || !upperState.is(this)
                        || lowerState.getValue(HALF)
                        != DoubleBlockHalf.LOWER
                        || upperState.getValue(HALF)
                        != DoubleBlockHalf.UPPER
                        || lowerState.getValue(FACING)
                        != upperState.getValue(FACING)
        ) {
            return InteractionResult.PASS;
        }

        ArchedWindowBuried buried =
                lowerState.getValue(BURIED);

        boolean clickedSideHasDirt =
                clickedFront
                        ? buried.hasFrontDirt()
                        : buried.hasBackDirt();

        /*
         * The clicked side is not buried.
         *
         * Returning PASS allows Gravel to perform its normal placement
         * behavior beside the window.
         */
        if (!clickedSideHasDirt) {
            return InteractionResult.PASS;
        }

        /*
         * Consume one Gravel outside Creative mode.
         */
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /*
     * Breaking either half removes the matching partner.
     *
     * The partner is removed with drops disabled so the complete
     * window produces only one Arched Window item.
     */
    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        DoubleBlockHalf half =
                state.getValue(HALF);

        BlockPos otherPos =
                half == DoubleBlockHalf.LOWER
                        ? pos.above()
                        : pos.below();

        BlockState otherState =
                level.getBlockState(otherPos);

        /*
         * Verify that the neighboring block is:
         *
         * 1. Another Arched Window block.
         * 2. The opposite half.
         * 3. Facing the same direction.
         *
         * We intentionally do not compare BURIED here. If the halves
         * become temporarily mismatched, breaking one should still
         * remove the complete window.
         */
        if (
                otherState.is(this)
                        && otherState.getValue(HALF) != half
                        && otherState.getValue(FACING)
                        == state.getValue(FACING)
        ) {
            level.removeBlock(
                    otherPos,
                    false
            );
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }

    /*
     * Returns the lower position when given either half.
     */
    public static BlockPos getLowerPosition(
            BlockPos pos,
            BlockState state
    ) {
        return state.getValue(HALF)
                == DoubleBlockHalf.LOWER
                ? pos
                : pos.below();
    }

    /*
     * Returns the upper position when given either half.
     */
    public static BlockPos getUpperPosition(
            BlockPos pos,
            BlockState state
    ) {
        return getLowerPosition(
                pos,
                state
        ).above();
    }

    /*
     * Registers every property stored by an Arched Window block state.
     */
    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                HALF,
                BURIED
        );
    }
}