package com.dungeoncraft.block;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Needed for door animation
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

public class IronPocketDoorBlock extends Block {

    // Which direction the door is facing.
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    // Whether this block is the lower half or upper half of the two-block door.
    public static final EnumProperty<DoubleBlockHalf> HALF =
            BlockStateProperties.DOUBLE_BLOCK_HALF;

    // Whether the door is open or closed.
    public static final BooleanProperty OPEN =
            BlockStateProperties.OPEN;

    // Remembers whether redstone was powering the door on the previous check.
    //
    // OPEN and POWERED must remain separate:
    // - OPEN controls the visible/manual door position.
    // - POWERED records the redstone input state.
    //
    // This prevents an unpowered manually opened door from being immediately
    // forced closed by a neighbor update between the two door-half updates.
    public static final BooleanProperty POWERED =
            BlockStateProperties.POWERED;

    // Whether the door slides left when opening.
    // false = slides right
    // true = slides left
    public static final BooleanProperty SLIDES_LEFT =
            BooleanProperty.create("slides_left");

    // Which animation frame the sliding door is currently showing.
    // 0 = fully closed,  1 = slightly open,  2 = halfway open
    // 3 = mostly open,  4 = fully open
    public static final IntegerProperty SLIDE_STEP =
            IntegerProperty.create("slide_step", 0, 4);

    // Closed door blocks the doorway.
    // North/South door: thin panel across Z 7-9, full X width.
    private static final VoxelShape CLOSED_NORTH_SOUTH =
            Block.box(0.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);

    // East/West door: thin panel across X 7-9, full Z width.
    private static final VoxelShape CLOSED_EAST_WEST =
            Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 16.0D);


    // Open shapes.
    // These are the thin visible/selection edge where the door has slid into the wall.

    // Open door is visually shifted into the side/pocket.
    // Collision will be empty while open, but the outline can still show a thin edge.
    // These are used when we only have a right side slide
    // private static final VoxelShape OPEN_NORTH_SOUTH = Block.box(0, 0, 7, 2, 16, 9);
    // private static final VoxelShape OPEN_EAST_WEST = Block.box(7, 0, 0, 9, 16, 2);

    private static final VoxelShape OPEN_NORTH_SOUTH_LEFT =
            Block.box(0.0D, 0.0D, 7.0D, 2.0D, 16.0D, 9.0D);

    private static final VoxelShape OPEN_NORTH_SOUTH_RIGHT =
            Block.box(14.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);

    private static final VoxelShape OPEN_EAST_WEST_LEFT =
            Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 2.0D);

    private static final VoxelShape OPEN_EAST_WEST_RIGHT =
            Block.box(7.0D, 0.0D, 14.0D, 9.0D, 16.0D, 16.0D);

    /*
     * Permanent pocket-door frame shapes.
     *
     * The frame remains in place whether the metal door panel is
     * open, closed, or moving.
     *
     * Each half includes the two vertical side posts.
     *
     * The lower half also includes the bottom rail.
     * The upper half also includes the top rail.
     */

    /*
     * Frame used when the door faces north or south.
     *
     * The frame extends across the X direction and is centered
     * around the Z direction.
     */
    private static final VoxelShape LOWER_FRAME_NORTH_SOUTH =
            Shapes.or(
                    // Left vertical frame post.
                    Block.box(
                            0.0D, 0.0D, 4.5D,
                            0.25D, 16.0D, 11.5D
                    ),

                    // Right vertical frame post.
                    Block.box(
                            15.75D, 0.0D, 4.5D,
                            16.0D, 16.0D, 11.5D
                    ),

                    // Bottom frame rail.
                    Block.box(
                            0.0D, 0.0D, 4.5D,
                            16.0D, 0.5D, 11.5D
                    )
            );

    private static final VoxelShape UPPER_FRAME_NORTH_SOUTH =
            Shapes.or(
                    // Left vertical frame post.
                    Block.box(
                            0.0D, 0.0D, 4.5D,
                            0.25D, 16.0D, 11.5D
                    ),

                    // Right vertical frame post.
                    Block.box(
                            15.75D, 0.0D, 4.5D,
                            16.0D, 16.0D, 11.5D
                    ),

                    // Top frame rail.
                    Block.box(
                            0.0D, 15.5D, 4.5D,
                            16.0D, 16.0D, 11.5D
                    )
            );


    /*
     * Rotated frame used when the door faces east or west.
     *
     * The frame extends across the Z direction and is centered
     * around the X direction.
     */
    private static final VoxelShape LOWER_FRAME_EAST_WEST =
            Shapes.or(
                    // First vertical frame post.
                    Block.box(
                            4.5D, 0.0D, 0.0D,
                            11.5D, 16.0D, 0.25D
                    ),

                    // Second vertical frame post.
                    Block.box(
                            4.5D, 0.0D, 15.75D,
                            11.5D, 16.0D, 16.0D
                    ),

                    // Bottom frame rail.
                    Block.box(
                            4.5D, 0.0D, 0.0D,
                            11.5D, 0.5D, 16.0D
                    )
            );

    private static final VoxelShape UPPER_FRAME_EAST_WEST =
            Shapes.or(
                    // First vertical frame post.
                    Block.box(
                            4.5D, 0.0D, 0.0D,
                            11.5D, 16.0D, 0.25D
                    ),

                    // Second vertical frame post.
                    Block.box(
                            4.5D, 0.0D, 15.75D,
                            11.5D, 16.0D, 16.0D
                    ),

                    // Top frame rail.
                    Block.box(
                            4.5D, 15.5D, 0.0D,
                            11.5D, 16.0D, 16.0D
                    )
            );

    public IronPocketDoorBlock(Properties properties) {
        super(properties);

        // Default state for the block using this constructor.
        // The item placement code will set the actual facing, half, and slide direction.
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(SLIDES_LEFT, false)
                .setValue(SLIDE_STEP, 0)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(SLIDES_LEFT, false)
                .setValue(SLIDE_STEP, 0);
    }

    /*
     * Rechecks redstone when either half is placed.
     *
     * This allows a pocket door placed beside an already-powered
     * Iron Lever, Copper Wire, or redstone source to open immediately.
     */
    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );

        if (!level.isClientSide()) {
            updateFromRedstone(
                    level,
                    pos,
                    state
            );
        }
    }

    /*
     * Runs whenever a neighboring block changes its redstone output.
     *
     * Minecraft 26.1.2 passes an Orientation object with modern
     * redstone neighbor updates. The door does not need to read that
     * object; it simply rechecks both door halves for power.
     */
    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block changedBlock,
            Orientation orientation,
            boolean movedByPiston
    ) {
        if (!level.isClientSide()) {
            updateFromRedstone(
                    level,
                    pos,
                    state
            );
        }
    }

    /*
     * Treats the two-block door as one redstone receiver.
     *
     * A signal beside either the lower or upper half controls the
     * complete door when that signal changes:
     *
     * signal turns on  = open
     * signal turns off = closed
     *
     * Manual opening while unpowered remains valid because OPEN and
     * POWERED are tracked separately.
     */
    private void updateFromRedstone(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        BlockPos lowerPos =
                state.getValue(HALF)
                        == DoubleBlockHalf.LOWER
                        ? pos
                        : pos.below();

        BlockPos upperPos =
                lowerPos.above();

        BlockState lowerState =
                level.getBlockState(lowerPos);

        BlockState upperState =
                level.getBlockState(upperPos);

        if (
                !lowerState.is(this)
                && !upperState.is(this)
        ) {
            return;
        }

        boolean powered =
                level.hasNeighborSignal(lowerPos)
                        || level.hasNeighborSignal(upperPos);

        /*
         * Compare the new signal with the stored POWERED value, not OPEN.
         *
         * OPEN may have been changed manually while POWERED remains false.
         * That is valid and must not be undone by an unrelated neighbor update.
         *
         * Check both halves so a newly placed upper half, or a temporarily
         * desynchronized half, is repaired even if the other half already has
         * the correct POWERED value.
         */
        boolean lowerPowerMatches =
                !lowerState.is(this)
                        || lowerState.getValue(POWERED) == powered;

        boolean upperPowerMatches =
                !upperState.is(this)
                        || upperState.getValue(POWERED) == powered;

        if (lowerPowerMatches && upperPowerMatches) {
            return;
        }

        setDoorOpenFromSignal(
                level,
                lowerPos,
                upperPos,
                lowerState,
                upperState,
                powered
        );
    }

    /*
     * Updates both halves together and starts or reverses the existing
     * stepped sliding animation.
     */
    private void setDoorOpenFromSignal(
            Level level,
            BlockPos lowerPos,
            BlockPos upperPos,
            BlockState lowerState,
            BlockState upperState,
            boolean open
    ) {
        /*
         * First update both halves without neighbor notifications.
         * This prevents neighborChanged() from seeing one updated half and one
         * old half in the middle of the operation. Flag 2 still sends the new
         * block states to the client for rendering.
         */
        if (lowerState.is(this)) {
            level.setBlock(
                    lowerPos,
                    lowerState
                            .setValue(OPEN, open)
                            .setValue(POWERED, open),
                    2
            );
        }

        if (upperState.is(this)) {
            level.setBlock(
                    upperPos,
                    upperState
                            .setValue(OPEN, open)
                            .setValue(POWERED, open),
                    2
            );
        }

        // Start or reverse the animation after both halves are synchronized.
        if (lowerState.is(this)) {
            level.scheduleTick(lowerPos, this, 2);
        }

        if (upperState.is(this)) {
            level.scheduleTick(upperPos, this, 2);
        }

        // Notify neighboring blocks only after both halves contain the final state.
        if (lowerState.is(this)) {
            level.updateNeighborsAt(lowerPos, this);
        }

        if (upperState.is(this)) {
            level.updateNeighborsAt(upperPos, this);
        }
    }

    /*
     * useItemOn()
     *
     * Runs when the player right-clicks this block while holding
     * an item or building block.
     *
     * Inward frame surfaces:
     *
     * - Do not open or close the door.
     * - Do not allow the held item to place through the doorway.
     *
     * Exterior frame surfaces:
     *
     * - Front and back surfaces allow normal block placement.
     * - Outside edges allow normal block placement.
     * - Exterior top and bottom surfaces allow normal placement.
     *
     * Metal door panel:
     *
     * - Continues to the normal door interaction so it can
     *   still open or close while an item is held.
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
         * First determine whether the click landed on one of the
         * permanent frame boxes.
         */
        if (isHitInsideShape(
                getFrameShapeForState(state),
                pos,
                hitResult
        )) {
            /*
             * Consume clicks only when the selected surface faces
             * inward toward the open doorway.
             *
             * SUCCESS stops the held block or item from acting.
             */
            if (isInsideFrameFace(
                    state,
                    pos,
                    hitResult
            )) {
                return InteractionResult.SUCCESS;
            }

            /*
             * The player clicked an exterior frame surface.
             *
             * PASS allows the held block or item to perform its
             * normal action against that surface.
             */
            return InteractionResult.PASS;
        }

        /*
         * The click did not land on the permanent frame.
         *
         * Send it to useWithoutItem(), which contains the current
         * metal door opening and closing behavior.
         */
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        /*
         * The permanent frame is part of the same block as the moving
         * door panel, allowing the complete assembly to be mined.
         *
         * Clicking the frame must not toggle the door.
         */
        if (isHitInsideShape(
                getFrameShapeForState(state),
                pos,
                hitResult
        )) {
            return InteractionResult.PASS;
        }

        // Match the Iron Lever pattern: let the server own the actual toggle.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos lowerPos =
                state.getValue(HALF) == DoubleBlockHalf.LOWER
                        ? pos
                        : pos.below();

        BlockPos upperPos =
                lowerPos.above();

        BlockState lowerState =
                level.getBlockState(lowerPos);

        BlockState upperState =
                level.getBlockState(upperPos);

        BlockState referenceState =
                lowerState.is(this)
                        ? lowerState
                        : upperState;

        boolean newOpen =
                !referenceState.getValue(OPEN);

        /*
         * Manual use changes OPEN only. POWERED must remain unchanged because
         * the player did not change the redstone input.
         *
         * Both halves are written with flag 2 before any neighbor notification,
         * so redstone cannot inspect a half-updated door.
         */
        if (lowerState.is(this)) {
            level.setBlock(
                    lowerPos,
                    lowerState.setValue(OPEN, newOpen),
                    2
            );
        }

        if (upperState.is(this)) {
            level.setBlock(
                    upperPos,
                    upperState.setValue(OPEN, newOpen),
                    2
            );
        }

        if (lowerState.is(this)) {
            level.scheduleTick(lowerPos, this, 2);
        }

        if (upperState.is(this)) {
            level.scheduleTick(upperPos, this, 2);
        }

        if (lowerState.is(this)) {
            level.updateNeighborsAt(lowerPos, this);
        }

        if (upperState.is(this)) {
            level.updateNeighborsAt(upperPos, this);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int currentStep = state.getValue(SLIDE_STEP);
        boolean open = state.getValue(OPEN);

        int nextStep = currentStep;

        if (open && currentStep < 4) {
            nextStep = currentStep + 1;
        }

        if (!open && currentStep > 0) {
            nextStep = currentStep - 1;
        }

        if (nextStep == currentStep) {
            return;
        }

        level.setBlock(
                pos,
                state.setValue(SLIDE_STEP, nextStep),
                2
        );

        level.scheduleTick(pos, this, 2);
    }

    // This method runs right before the player destroys this block.
// We use it to make the two-block Iron Pocket Door behave like one object.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {

        // This will store the position of the other half of the door.
        BlockPos otherPos;

        // This checks whether the block being broken is the lower half of the door.
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {

            // If the broken block is the lower half, the other half is above it.
            otherPos = pos.above();

            // If the broken block is not the lower half, then it must be the upper half.
        } else {

            // If the broken block is the upper half, the other half is below it.
            otherPos = pos.below();
        }

        // This checks whether the other half is actually still an Iron Pocket Door block.
        if (level.getBlockState(otherPos).is(this)) {

            // This checks whether the player broke the upper half of the door.
            if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {

                // This prevents item drops from happening in Creative mode.
                if (!player.isCreative()) {

                    // This manually drops exactly one Iron Pocket Door item.
                    // We do this because the upper half's loot table is supposed to drop nothing.
                    popResource(level, pos, new ItemStack(this.asItem()));
                }
            }

            // This removes the other half of the door.
            // The false value means "do not move this block; just remove it."
            // Because we are removing the other half directly, its loot table will not create a second drop.
            level.removeBlock(otherPos, false);
        }

        // This lets Minecraft continue its normal block-breaking behavior.
        // If the lower half was broken, your lower-half loot table will drop the door item.
        // If the upper half was broken, the loot table drops nothing, but we already manually dropped one item above.
        return super.playerWillDestroy(level, pos, state, player);
    }


    /*
     * Controls the outline that appears when the player points
     * at the pocket door.
     *
     * The outline includes both:
     *
     * - The permanent door frame.
     * - The visible position of the moving door panel.
     *
     * This allows the assembly to be mined by targeting either
     * the frame or the metal door.
     */
    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.or(
                getFrameShapeForState(state),
                getVisualShapeForState(state)
        );
    }


    /*
     * Controls the physical collision for the pocket door.
     *
     * The frame always retains collision.
     *
     * The metal door panel blocks the doorway while it is closed
     * or moving. Once the animation reaches fully open, only the
     * permanent frame remains as collision.
     */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        // Get the permanent frame for this direction and door half.
        VoxelShape frameShape =
                getFrameShapeForState(state);

        /*
         * At animation step 4, the metal door panel is fully inside
         * its pocket.
         *
         * The doorway is open, but the frame still has collision.
         */
        if (state.getValue(SLIDE_STEP) >= 4) {
            return frameShape;
        }

        /*
         * While the door is closed or moving, preserve the existing
         * full-panel collision.
         */
        Direction facing = state.getValue(FACING);

        VoxelShape doorShape;

        if (facing == Direction.NORTH
                || facing == Direction.SOUTH) {

            doorShape = CLOSED_NORTH_SOUTH;

        } else {

            doorShape = CLOSED_EAST_WEST;
        }

        // Combine the permanent frame and metal door panel.
        return Shapes.or(
                frameShape,
                doorShape
        );
    }

    /*
     * Returns the permanent frame shape matching:
     *
     * - The direction the door faces.
     * - Whether this block is the lower or upper half.
     */
    private static VoxelShape getFrameShapeForState(
            BlockState state
    ) {
        Direction facing =
                state.getValue(FACING);

        DoubleBlockHalf half =
                state.getValue(HALF);

        boolean northSouth =
                facing == Direction.NORTH
                        || facing == Direction.SOUTH;

        boolean lowerHalf =
                half == DoubleBlockHalf.LOWER;

        if (northSouth) {
            return lowerHalf
                    ? LOWER_FRAME_NORTH_SOUTH
                    : UPPER_FRAME_NORTH_SOUTH;
        }

        return lowerHalf
                ? LOWER_FRAME_EAST_WEST
                : UPPER_FRAME_EAST_WEST;
    }

    /*
     * isInsideFrameFace()
     *
     * Determines whether a frame click landed on a surface facing
     * inward toward the doorway.
     *
     * Only these inward surfaces prevent held-item use.
     *
     * Front, back, and exterior frame surfaces return false so
     * normal block placement can continue.
     */
    /*
     * isInsideFrameFace()
     *
     * Determines whether a frame click landed on a surface facing
     * inward toward the doorway.
     *
     * Block.box() dimensions use 0–16 model coordinates.
     * BlockHitResult locations use normalized 0.0–1.0 coordinates.
     *
     * Therefore, all frame measurements must be divided by 16.
     */
    private static boolean isInsideFrameFace(
            BlockState state,
            BlockPos pos,
            BlockHitResult hitResult
    ) {
        Direction facing =
                state.getValue(FACING);

        DoubleBlockHalf half =
                state.getValue(HALF);

        Direction hitDirection =
                hitResult.getDirection();

        /*
         * Convert the world-space hit location into coordinates inside
         * this individual block.
         *
         * Each resulting value normally ranges from 0.0 to 1.0.
         */
        double localX =
                hitResult.getLocation().x - pos.getX();

        double localY =
                hitResult.getLocation().y - pos.getY();

        double localZ =
                hitResult.getLocation().z - pos.getZ();

        final double tolerance =
                1.0E-4D;

        /*
         * Convert the frame measurements from Block.box() model units
         * into normalized block coordinates.
         */
        final double firstPostInnerEdge =
                0.25D / 16.0D;

        final double secondPostInnerEdge =
                15.75D / 16.0D;

        final double bottomRailTop =
                0.5D / 16.0D;

        final double topRailBottom =
                15.5D / 16.0D;

        boolean northSouth =
                facing == Direction.NORTH
                        || facing == Direction.SOUTH;

        /*
         * North/South-oriented door.
         *
         * The frame posts are located at the X edges.
         */
        if (northSouth) {

            /*
             * Post at X 0.0–0.25:
             * its EAST face points inward.
             */
            boolean leftPostInsideFace =
                    hitDirection == Direction.EAST
                            && localX <= firstPostInnerEdge + tolerance;

            /*
             * Post at X 15.75–16.0:
             * its WEST face points inward.
             */
            boolean rightPostInsideFace =
                    hitDirection == Direction.WEST
                            && localX >= secondPostInnerEdge - tolerance;

            if (leftPostInsideFace || rightPostInsideFace) {
                return true;
            }
        }

        /*
         * East/West-oriented door.
         *
         * The frame posts are located at the Z edges.
         */
        if (!northSouth) {

            /*
             * Post at Z 0.0–0.25:
             * its SOUTH face points inward.
             */
            boolean firstPostInsideFace =
                    hitDirection == Direction.SOUTH
                            && localZ <= firstPostInnerEdge + tolerance;

            /*
             * Post at Z 15.75–16.0:
             * its NORTH face points inward.
             */
            boolean secondPostInsideFace =
                    hitDirection == Direction.NORTH
                            && localZ >= secondPostInnerEdge - tolerance;

            if (firstPostInsideFace || secondPostInsideFace) {
                return true;
            }
        }

        /*
         * The upper surface of the lower-half bottom rail points into
         * the doorway.
         */
        if (half == DoubleBlockHalf.LOWER) {
            boolean bottomRailInsideFace =
                    hitDirection == Direction.UP
                            && localY <= bottomRailTop + tolerance;

            if (bottomRailInsideFace) {
                return true;
            }
        }

        /*
         * The lower surface of the upper-half top rail points into
         * the doorway.
         */
        if (half == DoubleBlockHalf.UPPER) {
            boolean topRailInsideFace =
                    hitDirection == Direction.DOWN
                            && localY >= topRailBottom - tolerance;

            if (topRailInsideFace) {
                return true;
            }
        }

        /*
         * The click was on a front, back, or exterior frame surface.
         */
        return false;
    }

    /*
     * Determines whether the player's click landed within one
     * of the boxes forming the permanent door frame.
     *
     * The BlockHitResult contains world coordinates, so they are
     * converted into coordinates local to this one block.
     */
    private static boolean isHitInsideShape(
            VoxelShape shape,
            BlockPos pos,
            BlockHitResult hitResult
    ) {
        double localX =
                hitResult.getLocation().x - pos.getX();

        double localY =
                hitResult.getLocation().y - pos.getY();

        double localZ =
                hitResult.getLocation().z - pos.getZ();

        /*
         * This small tolerance includes clicks that land exactly
         * along the edge of a frame box.
         */
        final double tolerance =
                1.0E-4D;

        for (AABB box : shape.toAabbs()) {

            boolean insideX =
                    localX >= box.minX - tolerance
                            && localX <= box.maxX + tolerance;

            boolean insideY =
                    localY >= box.minY - tolerance
                            && localY <= box.maxY + tolerance;

            boolean insideZ =
                    localZ >= box.minZ - tolerance
                            && localZ <= box.maxZ + tolerance;

            if (insideX && insideY && insideZ) {
                return true;
            }
        }

        return false;
    }

    private VoxelShape getVisualShapeForState(BlockState state) {
        Direction facing = state.getValue(FACING);
        boolean open = state.getValue(OPEN);
        boolean slidesLeft = state.getValue(SLIDES_LEFT);

        if (!open) {
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                return CLOSED_NORTH_SOUTH;
            }

            return CLOSED_EAST_WEST;
        }

        return switch (facing) {
            case NORTH -> slidesLeft ? OPEN_NORTH_SOUTH_RIGHT : OPEN_NORTH_SOUTH_LEFT;
            case SOUTH -> slidesLeft ? OPEN_NORTH_SOUTH_LEFT : OPEN_NORTH_SOUTH_RIGHT;
            case EAST -> slidesLeft ? OPEN_EAST_WEST_RIGHT : OPEN_EAST_WEST_LEFT;
            case WEST -> slidesLeft ? OPEN_EAST_WEST_LEFT : OPEN_EAST_WEST_RIGHT;
            default -> OPEN_NORTH_SOUTH_LEFT;
        };
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN, POWERED, SLIDES_LEFT, SLIDE_STEP);
    }
}