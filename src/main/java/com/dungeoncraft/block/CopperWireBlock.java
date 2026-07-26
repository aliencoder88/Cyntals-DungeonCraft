package com.dungeoncraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * DungeonCraft Copper Wire — Phase 4H Support-Break Behavior
 *
 * This version replaces the delayed Phase 2.1 controller.
 *
 * Design goals:
 *
 * 1. Copper Wire and vanilla redstone dust can power one another.
 *
 * 2. Removing a genuine source cannot leave Dust and Copper Wire
 *    powering one another forever.
 *
 * 3. Model connections appear immediately when added.
 *
 * 4. A possible model disconnection is verified one game tick later.
 *
 * 5. Electrical recalculation never scans or changes LINK_* properties.
 *
 * 6. Visual verification happens before power is changed, so piston
 *    movement caused by the new power state cannot be mistaken for a
 *    permanent lost connection.
 *
 * Future converter boundary:
 *
 * Redstone Dust -> Upgrading Converter -> Copper Wire
 * Copper Wire   -> Downgrading Converter -> Redstone Dust
 */
public class CopperWireBlock extends Block {

    /*
     * Completed model-selection properties.
     */
    public static final BooleanProperty LINK_NORTH =
            BooleanProperty.create("link_north");

    public static final BooleanProperty LINK_EAST =
            BooleanProperty.create("link_east");

    public static final BooleanProperty LINK_SOUTH =
            BooleanProperty.create("link_south");

    public static final BooleanProperty LINK_WEST =
            BooleanProperty.create("link_west");

    /*
     * Bit mask describing every branch that climbs an adjacent wall.
     *
     * north = 1
     * east  = 2
     * south = 4
     * west  = 8
     *
     * Values combine normally:
     *
     * 3  = north + east
     * 5  = north + south
     * 7  = north + east + south
     * 15 = all four directions
     */
    public static final IntegerProperty UP_MASK =
            IntegerProperty.create("up_mask", 0, 15);

    public static final IntegerProperty POWER =
            BlockStateProperties.POWER;

    /*
     * Selection / interaction shapes
     * ------------------------------
     *
     * These are intentionally larger than the rendered wire so the player
     * can still target and break it comfortably.
     *
     * They are much smaller than a full block, leaving the surrounding
     * floor and neighboring block faces easier to target.
     *
     * Coordinates use Minecraft's 0-16 block model scale.
     */
    private static final VoxelShape COIL_INTERACTION_SHAPE =
            Block.box(
                    4.5,
                    0.0,
                    4.5,
                    11.5,
                    2.5,
                    11.5
            );

    private static final VoxelShape CENTER_INTERACTION_SHAPE =
            Block.box(
                    5.5,
                    0.0,
                    5.5,
                    10.5,
                    2.25,
                    10.5
            );

    private static final VoxelShape NORTH_INTERACTION_ARM =
            Block.box(
                    6.5,
                    0.0,
                    0.0,
                    9.5,
                    2.25,
                    8.0
            );

    private static final VoxelShape EAST_INTERACTION_ARM =
            Block.box(
                    8.0,
                    0.0,
                    6.5,
                    16.0,
                    2.25,
                    9.5
            );

    private static final VoxelShape SOUTH_INTERACTION_ARM =
            Block.box(
                    6.5,
                    0.0,
                    8.0,
                    9.5,
                    2.25,
                    16.0
            );

    private static final VoxelShape WEST_INTERACTION_ARM =
            Block.box(
                    0.0,
                    0.0,
                    6.5,
                    8.0,
                    2.25,
                    9.5
            );

    private static final VoxelShape NORTH_UP_INTERACTION_ARM =
            Block.box(
                    6.5,
                    0.0,
                    0.0,
                    9.5,
                    16.0,
                    2.25
            );

    private static final VoxelShape EAST_UP_INTERACTION_ARM =
            Block.box(
                    13.75,
                    0.0,
                    6.5,
                    16.0,
                    16.0,
                    9.5
            );

    private static final VoxelShape SOUTH_UP_INTERACTION_ARM =
            Block.box(
                    6.5,
                    0.0,
                    13.75,
                    9.5,
                    16.0,
                    16.0
            );

    private static final VoxelShape WEST_UP_INTERACTION_ARM =
            Block.box(
                    0.0,
                    0.0,
                    6.5,
                    2.25,
                    16.0,
                    9.5
            );

    /*
     * Precomputed shapes for all 16 combinations of N/E/S/W links.
     *
     * Bit values:
     * north = 1
     * east  = 2
     * south = 4
     * west  = 8
     */
    private static final VoxelShape[] INTERACTION_SHAPES =
            createInteractionShapes();

    /*
     * Marks a possible visual disconnection for one-tick verification.
     *
     * The unused Phase 2 power/network timer properties were removed here
     * before adding UP_MASK. Keeping them would multiply the state space to
     * 65,536 states per Copper Wire block.
     */
    public static final BooleanProperty VISUAL_REFRESH_REQUIRED =
            BooleanProperty.create("visual_refresh_required");

    public static final TagKey<Block> COPPER_WIRE_CONNECTABLE =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(
                            "dungeoncraft",
                            "copper_wire_connectable"
                    )
            );

    /*
     * Suppresses Copper Wire output while genuine external sources are
     * being measured.
     */
    private static boolean shouldEmitSignal = true;

    /*
     * Prevents controller-written state updates from recursively starting
     * another copy of the same calculation.
     */
    private static boolean internalUpdate = false;

    private static VoxelShape[] createInteractionShapes() {
        VoxelShape[] shapes =
                new VoxelShape[16];

        /*
         * Zero links uses the larger centered coil target.
         */
        shapes[0] =
                COIL_INTERACTION_SHAPE;

        for (int mask = 1; mask < shapes.length; mask++) {
            VoxelShape shape =
                    CENTER_INTERACTION_SHAPE;

            if ((mask & 1) != 0) {
                shape =
                        Shapes.or(
                                shape,
                                NORTH_INTERACTION_ARM
                        );
            }

            if ((mask & 2) != 0) {
                shape =
                        Shapes.or(
                                shape,
                                EAST_INTERACTION_ARM
                        );
            }

            if ((mask & 4) != 0) {
                shape =
                        Shapes.or(
                                shape,
                                SOUTH_INTERACTION_ARM
                        );
            }

            if ((mask & 8) != 0) {
                shape =
                        Shapes.or(
                                shape,
                                WEST_INTERACTION_ARM
                        );
            }

            shapes[mask] =
                    shape;
        }

        return shapes;
    }

    public CopperWireBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(LINK_NORTH, false)
                        .setValue(LINK_EAST, false)
                        .setValue(LINK_SOUTH, false)
                        .setValue(LINK_WEST, false)
                        .setValue(UP_MASK, 0)
                        .setValue(POWER, 0)
                        .setValue(
                                VISUAL_REFRESH_REQUIRED,
                                false
                        )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                LINK_NORTH,
                LINK_EAST,
                LINK_SOUTH,
                LINK_WEST,
                UP_MASK,
                POWER,
                VISUAL_REFRESH_REQUIRED
        );
    }

    @Override
    protected boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        return Block.canSupportCenter(
                level,
                pos.below(),
                Direction.UP
        );
    }

    /*
     * Returns the smaller player-selection shape.
     *
     * This controls the outline used when aiming at, breaking, or
     * interacting with Copper Wire.
     */
    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        int shapeIndex = 0;

        if (state.getValue(LINK_NORTH)) {
            shapeIndex |= 1;
        }

        if (state.getValue(LINK_EAST)) {
            shapeIndex |= 2;
        }

        if (state.getValue(LINK_SOUTH)) {
            shapeIndex |= 4;
        }

        if (state.getValue(LINK_WEST)) {
            shapeIndex |= 8;
        }

        VoxelShape shape =
                INTERACTION_SHAPES[
                        shapeIndex
                ];

        int upMask = state.getValue(UP_MASK);

        if ((upMask & 1) != 0) {
            shape = Shapes.or(
                    shape,
                    NORTH_UP_INTERACTION_ARM
            );
        }

        if ((upMask & 2) != 0) {
            shape = Shapes.or(
                    shape,
                    EAST_UP_INTERACTION_ARM
            );
        }

        if ((upMask & 4) != 0) {
            shape = Shapes.or(
                    shape,
                    SOUTH_UP_INTERACTION_ARM
            );
        }

        if ((upMask & 8) != 0) {
            shape = Shapes.or(
                    shape,
                    WEST_UP_INTERACTION_ARM
            );
        }

        return shape;
    }

    /*
     * Copper Wire is too thin to block movement.
     *
     * Selection remains available through getShape(), but entities and
     * players can walk over the wire normally.
     */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    /*
     * When Copper Wire is placed above the top of a redstone stair, the
     * lower dust is diagonally adjacent and does not always receive a
     * normal direct-neighbor shape update.
     *
     * Refresh that one directional dust property explicitly so the vanilla
     * dust immediately renders its UP arm toward Copper Wire.
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
            refreshLowerDiagonalDustStates(
                    level,
                    pos,
                    true
            );

            refreshDiagonalCopperVisuals(
                    level,
                    pos
            );
        }
    }

    /*
     * In 26.1, affectNeighborsAfterRemoval() is the removal-side hook used
     * after the block has already left the world.
     *
     * Clear any UP arm that existed only because this Copper Wire occupied
     * the upper stair position.
     */
    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        super.affectNeighborsAfterRemoval(
                state,
                level,
                pos,
                movedByPiston
        );

        refreshLowerDiagonalDustStates(
                level,
                pos,
                false
        );

        refreshDiagonalCopperVisuals(
                level,
                pos
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockState placedState =
                addActualLinks(
                        defaultBlockState(),
                        level,
                        pos
                )
                        .setValue(POWER, 0)
                        .setValue(
                                VISUAL_REFRESH_REQUIRED,
                                false
                        );

        if (!level.isClientSide()) {
            level.scheduleTick(
                    pos,
                    this,
                    1
            );
        }

        return placedState;
    }

    /*
     * Handles the moment the block below this wire disappears.
     *
     * Default behavior is used by:
     *
     * - normal Copper Wire;
     * - exposed Embedded Copper Wire.
     *
     * Both become air and drop their own registered BlockItem, matching
     * the support-loss behavior expected from redstone-like floor wiring.
     *
     * CoveredEmbeddedCopperWireBlock overrides this method because its
     * stored cover must remain in the world.
     */
    protected BlockState getStateAfterSupportLost(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        return Blocks.AIR.defaultBlockState();
    }

    /*
     * Connections that appear are shown immediately.
     *
     * Connections that appear to disappear are preserved and marked for
     * one specific verification on the next scheduled tick. Ordinary
     * electrical updates do not trigger a general model scan.
     */
    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTicks,
            BlockPos pos,
            Direction changedDirection,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        if (
                changedDirection == Direction.DOWN
                && !state.canSurvive(level, pos)
        ) {
            return getStateAfterSupportLost(
                    state,
                    level,
                    pos
            );
        }

        if (internalUpdate) {
            return state;
        }

        BlockState updatedState = state;

        if (
                changedDirection.getAxis().isHorizontal()
                || changedDirection == Direction.UP
                || changedDirection == Direction.DOWN
        ) {
            BlockState actualState =
                    addActualLinks(
                            state,
                            level,
                            pos
                    );

            boolean movingPistonTransition =
                    neighborState.is(
                            Blocks.MOVING_PISTON
                    );

            boolean possibleRemoval = false;

            for (
                    Direction direction
                    : Direction.Plane.HORIZONTAL
            ) {
                boolean oldLink =
                        getLinkValue(
                                state,
                                direction
                        );

                boolean actualLink =
                        getLinkValue(
                                actualState,
                                direction
                        );

                if (!oldLink && actualLink) {
                    updatedState =
                            setLinkValue(
                                    updatedState,
                                    direction,
                                    true
                            );
                } else if (
                        oldLink
                        && !actualLink
                        && !(
                                movingPistonTransition
                                && direction == changedDirection
                        )
                ) {
                    possibleRemoval = true;
                }
            }

            int oldUpMask =
                    state.getValue(UP_MASK);

            int actualUpMask =
                    actualState.getValue(UP_MASK);

            /*
             * New upward branches appear immediately. Suspected removals
             * remain visible until the normal delayed verification tick,
             * matching the existing piston-stability behavior.
             */
            int displayedUpMask =
                    oldUpMask | actualUpMask;

            updatedState =
                    updatedState.setValue(
                            UP_MASK,
                            displayedUpMask
                    );

            if ((oldUpMask & ~actualUpMask) != 0) {
                possibleRemoval = true;
            }

            if (possibleRemoval) {
                updatedState =
                        updatedState.setValue(
                                VISUAL_REFRESH_REQUIRED,
                                true
                        );
            }
        }

        if (
                level instanceof Level actualLevel
                && !actualLevel.isClientSide()
        ) {
            actualLevel.scheduleTick(
                    pos,
                    this,
                    1
            );
        }

        return updatedState;
    }

    /*
     * Responds to redstone-output changes that do not replace the
     * neighboring blockstate.
     *
     * A wall-mounted Iron Lever strongly powers its support block. The
     * support block remains the same stone block when the lever toggles,
     * so updateShape() alone does not detect the electrical change.
     *
     * Vanilla redstone components notify nearby blocks through
     * neighborChanged(). Scheduling a Copper Wire tick here lets the
     * connected network immediately re-read:
     *
     * - a directly adjacent lever;
     * - a powered support block;
     * - buttons, repeaters, comparators, observers, and similar sources.
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
        if (
                level.isClientSide()
                || internalUpdate
        ) {
            return;
        }

        if (changedBlock == Blocks.REDSTONE_WIRE) {
            BlockState currentState =
                    level.getBlockState(pos);

            if (isCopperWireState(currentState)) {
                BlockState refreshedState =
                        refreshRedstoneStairVisualLinks(
                                currentState,
                                level,
                                pos
                        );

                if (!refreshedState.equals(currentState)) {
                    internalUpdate = true;

                    try {
                        level.setBlock(
                                pos,
                                refreshedState,
                                2
                        );
                    } finally {
                        internalUpdate = false;
                    }
                }
            }
        }

        level.scheduleTick(
                pos,
                this,
                1
        );
    }

    /*
     * One scheduled update first verifies any specifically pending visual
     * disconnection, then recalculates the connected Copper/Dust network.
     *
     * Electrical work never performs a general LINK_* scan.
     */
    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        BlockState currentState =
                level.getBlockState(pos);

        if (!isCopperWireState(currentState)) {
            return;
        }

        /*
         * Resolve a suspected lost visual connection before power changes
         * notify or move the piston.
         */
        refreshPendingVisual(
                level,
                pos
        );

        currentState =
                level.getBlockState(pos);

        if (!isCopperWireState(currentState)) {
            return;
        }

        Set<BlockPos> network =
                collectMixedWireNetwork(
                        level,
                        pos
                );

        if (network.isEmpty()) {
            return;
        }

        NetworkResult result =
                calculateNetwork(
                        level,
                        network
                );

        applyCopperPower(
                level,
                network,
                result.powerByPosition()
        );

        if (
                result.anyCalculatedPower()
                || result.anyOriginalDustPower()
        ) {
            /*
             * Keep one controller tick alive while this Copper network is
             * powered, even if the last connected dust block was removed.
             *
             * A powered dust block may be removed while its solid support
             * is still receiving power. The first Copper recalculation can
             * therefore correctly remain on. If the real source is turned
             * off afterward, no dust remains in the network to produce a
             * second notification.
             *
             * Rechecking one representative Copper position per powered
             * network guarantees that the later source loss is detected.
             * The scan still suppresses Copper output and temporarily
             * zeros connected dust, so Copper cannot sustain itself.
             */
            level.scheduleTick(
                    pos,
                    this,
                    1
            );
        }
    }

    @Override
    protected boolean isSignalSource(
            BlockState state
    ) {
        return true;
    }

    @Override
    protected int getSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        if (!shouldEmitSignal) {
            return 0;
        }

        return state.getValue(POWER);
    }

    /*
     * Redstone signal directions are queried from the receiving block's
     * point of view. Therefore Direction.UP here means this Copper Wire is
     * being asked whether it strongly powers the solid block directly
     * beneath it.
     *
     * Vanilla redstone dust strongly powers its support block. The lower
     * redstone stair in this layout depends on that support block carrying
     * the signal:
     *
     *   [ Copper Wire ] [ air ]
     *   [ solid block ] [ redstone dust ]
     *
     * Without a direct signal, the stair can look connected while the
     * lower dust remains electrically off.
     */
    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        if (!shouldEmitSignal) {
            return 0;
        }

        return direction == Direction.UP
                ? state.getValue(POWER)
                : 0;
    }

    /*
     * Calculates one same-level cardinal network containing Copper Wire
     * and vanilla redstone dust.
     *
     * Vanilla dust is temporarily set to power 0 with update flag 0.
     * This state is never sent to the client and is restored before final
     * Copper power is applied.
     *
     * With Dust at 0 and Copper output suppressed, getBestNeighborSignal()
     * can only see genuine non-wire power sources.
     */
    private NetworkResult calculateNetwork(
            ServerLevel level,
            Set<BlockPos> network
    ) {
        Map<BlockPos, BlockState> savedDustStates =
                new HashMap<>();

        boolean anyOriginalDustPower = false;
        boolean containsDust = false;

        for (BlockPos wirePos : network) {
            BlockState wireState =
                    level.getBlockState(
                            wirePos
                    );

            if (
                    wireState.is(
                            Blocks.REDSTONE_WIRE
                    )
            ) {
                containsDust = true;
                savedDustStates.put(
                        wirePos,
                        wireState
                );

                if (
                        wireState.getValue(
                                BlockStateProperties.POWER
                        ) > 0
                ) {
                    anyOriginalDustPower = true;
                }
            }
        }

        Map<BlockPos, Integer> powerByPosition;
        boolean anyCalculatedPower;

        internalUpdate = true;
        shouldEmitSignal = false;

        try {
            for (
                    Map.Entry<BlockPos, BlockState> entry
                    : savedDustStates.entrySet()
            ) {
                BlockState dustState =
                        entry.getValue();

                if (
                        dustState.getValue(
                                BlockStateProperties.POWER
                        ) != 0
                ) {
                    level.setBlock(
                            entry.getKey(),
                            dustState.setValue(
                                    BlockStateProperties.POWER,
                                    0
                            ),
                            0
                    );
                }
            }

            powerByPosition =
                    spreadPowerFromRealSources(
                            level,
                            network
                    );

            anyCalculatedPower =
                    powerByPosition
                            .values()
                            .stream()
                            .anyMatch(
                                    power -> power > 0
                            );
        } finally {
            for (
                    Map.Entry<BlockPos, BlockState> entry
                    : savedDustStates.entrySet()
            ) {
                level.setBlock(
                        entry.getKey(),
                        entry.getValue(),
                        0
                );
            }

            shouldEmitSignal = true;
            internalUpdate = false;
        }

        return new NetworkResult(
                powerByPosition,
                containsDust,
                anyCalculatedPower,
                anyOriginalDustPower
        );
    }

    /*
     * Every Copper or Dust position first receives any genuine external
     * power touching that position. Power then spreads through the combined
     * wire network, decreasing by one per wire block.
     */
    private Map<BlockPos, Integer> spreadPowerFromRealSources(
            ServerLevel level,
            Set<BlockPos> network
    ) {
        Map<BlockPos, Integer> powerByPosition =
                new HashMap<>();

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        for (BlockPos wirePos : network) {
            int externalPower =
                    getExternalPowerAtWire(
                            level,
                            wirePos
                    );

            powerByPosition.put(
                    wirePos,
                    externalPower
            );

            if (externalPower > 0) {
                queue.addLast(wirePos);
            }
        }

        while (!queue.isEmpty()) {
            BlockPos currentPos =
                    queue.removeFirst();

            int currentPower =
                    powerByPosition.getOrDefault(
                            currentPos,
                            0
                    );

            if (currentPower <= 1) {
                continue;
            }

            int nextPower =
                    currentPower - 1;

            for (
                    BlockPos neighborPos
                    : getConnectedNetworkNeighbors(
                            level,
                            currentPos
                    )
            ) {
                if (!network.contains(neighborPos)) {
                    continue;
                }

                int existingPower =
                        powerByPosition.getOrDefault(
                                neighborPos,
                                0
                        );

                if (nextPower > existingPower) {
                    powerByPosition.put(
                            neighborPos,
                            nextPower
                    );

                    queue.addLast(
                            neighborPos
                    );
                }
            }
        }

        return powerByPosition;
    }

    /*
     * Updates Copper Wire power without sending a power-only blockstate
     * change to the client.
     *
     * The Copper Wire models do not use POWER, so the client does not need
     * a new rendered blockstate whenever signal strength changes.
     */
    private void applyCopperPower(
            ServerLevel level,
            Set<BlockPos> network,
            Map<BlockPos, Integer> powerByPosition
    ) {
        Set<BlockPos> changedCopperPositions =
                new HashSet<>();

        internalUpdate = true;

        try {
            for (BlockPos wirePos : network) {
                BlockState wireState =
                        level.getBlockState(
                                wirePos
                        );

                if (!isCopperWireState(wireState)) {
                    continue;
                }

                int oldPower =
                        wireState.getValue(POWER);

                int newPower =
                        powerByPosition.getOrDefault(
                                wirePos,
                                0
                        );

                if (newPower == oldPower) {
                    continue;
                }

                BlockState newState =
                        wireState.setValue(
                                POWER,
                                newPower
                        );

                /*
                 * Flag 0:
                 *
                 * Change the server-side electrical state without sending a
                 * power-only model update to the client and without creating
                 * automatic neighbor-update storms.
                 */
                level.setBlock(
                        wirePos,
                        newState,
                        0
                );

                changedCopperPositions.add(
                        wirePos
                );
            }
        } finally {
            internalUpdate = false;
        }

        /*
         * Notify neighbors once from each Copper Wire block whose power
         * actually changed.
         *
         * This is enough to update pistons, dust, lamps, and neighboring
         * Copper Wire without notifying every block around every boundary
         * position.
         */
        for (
                BlockPos changedPos
                : changedCopperPositions
        ) {
            Block changedWireBlock =
                    level.getBlockState(changedPos)
                            .getBlock();

            level.updateNeighborsAt(
                    changedPos,
                    changedWireBlock
            );

            /*
             * A lower redstone-dust stair connection is not a direct
             * neighbor of the Copper Wire:
             *
             *   [ Copper Wire ] [ air ]
             *   [ solid block ] [ redstone dust ]
             *
             * updateNeighborsAt(changedPos, ...) reaches the support block
             * below Copper, but it does not directly reach the dust beside
             * that support block.
             *
             * Notify from the support position as well whenever at least
             * one valid lower-diagonal dust connection exists. That causes
             * vanilla dust to recalculate its power while preserving the
             * existing anti-feedback network controller.
             */
            if (
                    hasAnyLowerDiagonalRedstone(
                            level,
                            changedPos
                    )
            ) {
                level.updateNeighborsAt(
                        changedPos.below(),
                        changedWireBlock
                );
            }
        }
    }

    /*
     * Verifies only a connection that updateShape suspected had vanished.
     *
     * This method runs before the electrical update. It never performs a
     * network-wide model scan and therefore cannot observe the piston
     * transition created by the power change later in this tick.
     */
    private void refreshPendingVisual(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState wireState =
                level.getBlockState(pos);

        if (
                !isCopperWireState(wireState)
                || !wireState.getValue(
                        VISUAL_REFRESH_REQUIRED
                )
        ) {
            return;
        }

        BlockState visualState =
                addActualLinks(
                        wireState,
                        level,
                        pos
                )
                        .setValue(
                                VISUAL_REFRESH_REQUIRED,
                                false
                        );

        if (visualState.equals(wireState)) {
            return;
        }

        internalUpdate = true;

        try {
            level.setBlock(
                    pos,
                    visualState,
                    2
            );
        } finally {
            internalUpdate = false;
        }
    }

    private Set<BlockPos> collectMixedWireNetwork(
            Level level,
            BlockPos startPos
    ) {
        Set<BlockPos> visited =
                new HashSet<>();

        BlockState startState =
                level.getBlockState(
                        startPos
                );

        if (!isNetworkWire(startState)) {
            return visited;
        }

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        visited.add(startPos);
        queue.addLast(startPos);

        while (!queue.isEmpty()) {
            BlockPos currentPos =
                    queue.removeFirst();

            for (
                    BlockPos neighborPos
                    : getConnectedNetworkNeighbors(
                            level,
                            currentPos
                    )
            ) {
                if (visited.contains(neighborPos)) {
                    continue;
                }

                visited.add(neighborPos);
                queue.addLast(neighborPos);
            }
        }

        return visited;
    }

    /*
     * Returns every Copper Wire or vanilla redstone-dust position that is
     * electrically connected to the supplied position.
     *
     * The previous controller only checked same-level cardinal neighbors.
     * That left the upper dust in a vanilla redstone stair outside the
     * mixed network. Its old POWER value could then be mistaken for a real
     * outside source and keep the Copper/Dust system latched on.
     *
     * This helper follows:
     *
     * - same-level cardinal wire connections;
     * - a dust connection that climbs UP onto a neighboring block;
     * - the matching lower dust when the supplied position is the upper
     *   member of that stair.
     *
     * Using the dust blockstate's RedstoneSide.UP value prevents unrelated
     * diagonal wires from being joined merely because they are spatially
     * close.
     */
    private Set<BlockPos> getConnectedNetworkNeighbors(
            Level level,
            BlockPos pos
    ) {
        Set<BlockPos> connected =
                new HashSet<>();

        BlockState currentState =
                level.getBlockState(pos);

        for (
                Direction direction
                : Direction.Plane.HORIZONTAL
        ) {
            /*
             * Ordinary same-level Copper/Dust connection.
             */
            BlockPos sidePos =
                    pos.relative(direction);

            if (
                    isNetworkWire(
                            level.getBlockState(sidePos)
                    )
            ) {
                connected.add(sidePos);
            }

            /*
             * Copper Wire climbs the wall of the neighboring support block
             * to another Copper Wire one block higher. Embedded and covered
             * variants are included because they share this base class.
             */
            if (
                    isCopperWireState(currentState)
                    && hasUpwardCopperLink(
                            level,
                            pos,
                            direction
                    )
            ) {
                connected.add(
                        sidePos.above()
                );
            }

            /*
             * Lower Copper Wire climbs the same support wall to vanilla
             * redstone dust one block higher.
             *
             * Layout:
             *
             *   [ redstone dust ] [ air ]
             *   [ solid support ] [ Copper Wire ]
             */
            if (
                    isCopperWireState(currentState)
                    && hasUpperDiagonalRedstone(
                            level,
                            pos,
                            direction
                    )
            ) {
                connected.add(
                        sidePos.above()
                );
            }

            /*
             * Lower dust climbs onto the neighboring block.
             *
             * Example:
             *
             * dust at Y=0 --UP--> dust or Copper Wire at Y=1
             */
            if (
                    currentState.is(
                            Blocks.REDSTONE_WIRE
                    )
                    && (
                            getDustConnection(
                                    currentState,
                                    direction
                            ) == RedstoneSide.UP
                            || canDustClimbToNetworkWire(
                                    level,
                                    pos,
                                    direction
                            )
                    )
            ) {
                BlockPos upperPos =
                        sidePos.above();

                if (
                        isNetworkWire(
                                level.getBlockState(
                                        upperPos
                                )
                        )
                ) {
                    connected.add(upperPos);
                }
            }

            /*
             * The supplied position may be the upper member of a stair.
             * Look one block down and one block outward for lower dust
             * whose connection points UP toward this position.
             */
            BlockPos lowerDustPos =
                    sidePos.below();

            BlockState lowerDustState =
                    level.getBlockState(
                            lowerDustPos
                    );

            if (
                    lowerDustState.is(
                            Blocks.REDSTONE_WIRE
                    )
                    && (
                            getDustConnection(
                                    lowerDustState,
                                    direction.getOpposite()
                            ) == RedstoneSide.UP
                            || hasLowerDiagonalRedstone(
                                    level,
                                    pos,
                                    direction
                            )
                    )
            ) {
                connected.add(lowerDustPos);
            }

            if (
                    isCopperWireState(lowerDustState)
                    && hasLowerDiagonalCopperWire(
                            level,
                            pos,
                            direction
                    )
            ) {
                connected.add(lowerDustPos);
            }

            /*
             * The current position may be upper redstone dust with a lower
             * Copper Wire descending from one side of its support block.
             */
            if (
                    currentState.is(
                            Blocks.REDSTONE_WIRE
                    )
                    && isCopperWireState(
                            lowerDustState
                    )
                    && hasLowerDiagonalCopperFromUpperRedstone(
                            level,
                            pos,
                            direction
                    )
            ) {
                connected.add(lowerDustPos);
            }
        }

        return connected;
    }

    /*
     * Returns true when this lower Copper Wire can run across the floor and
     * climb the face of the adjacent support block to a Copper Wire above.
     */
    private boolean hasUpwardCopperLink(
            LevelReader level,
            BlockPos lowerWirePos,
            Direction direction
    ) {
        BlockPos supportPos =
                lowerWirePos.relative(direction);

        BlockState supportState =
                level.getBlockState(supportPos);

        if (
                !supportState.isRedstoneConductor(
                        level,
                        supportPos
                )
        ) {
            return false;
        }

        BlockPos upperWirePos =
                supportPos.above();

        if (
                !isCopperWireState(
                        level.getBlockState(upperWirePos)
                )
        ) {
            return false;
        }

        BlockPos spaceAboveLower =
                lowerWirePos.above();

        BlockState stateAboveLower =
                level.getBlockState(spaceAboveLower);

        return !stateAboveLower.isRedstoneConductor(
                level,
                spaceAboveLower
        );
    }

    /*
     * Same Copper-to-Copper stair viewed from the upper wire.
     *
     * The direction points from the upper wire toward the lower wire.
     */
    private boolean hasLowerDiagonalCopperWire(
            LevelReader level,
            BlockPos upperWirePos,
            Direction direction
    ) {
        BlockPos lowerWirePos =
                upperWirePos
                        .relative(direction)
                        .below();

        if (
                !isCopperWireState(
                        level.getBlockState(lowerWirePos)
                )
        ) {
            return false;
        }

        return hasUpwardCopperLink(
                level,
                lowerWirePos,
                direction.getOpposite()
        );
    }

    /*
     * Returns true when this lower Copper Wire can climb the adjacent
     * support wall to vanilla redstone dust one block higher.
     *
     * Layout:
     *
     *   [ redstone dust ] [ air ]
     *   [ solid support ] [ Copper Wire ]
     *
     * The direction points from the lower Copper Wire toward the support.
     */
    private boolean hasUpperDiagonalRedstone(
            LevelReader level,
            BlockPos lowerWirePos,
            Direction direction
    ) {
        BlockPos supportPos =
                lowerWirePos.relative(
                        direction
                );

        BlockState supportState =
                level.getBlockState(
                        supportPos
                );

        if (
                !supportState.isRedstoneConductor(
                        level,
                        supportPos
                )
        ) {
            return false;
        }

        BlockPos upperDustPos =
                supportPos.above();

        if (
                !level.getBlockState(
                        upperDustPos
                ).is(Blocks.REDSTONE_WIRE)
        ) {
            return false;
        }

        BlockPos spaceAboveLowerWire =
                lowerWirePos.above();

        BlockState stateAboveLowerWire =
                level.getBlockState(
                        spaceAboveLowerWire
                );

        return !stateAboveLowerWire.isRedstoneConductor(
                level,
                spaceAboveLowerWire
        );
    }

    /*
     * Same reverse stair viewed from the upper redstone-dust position.
     *
     * The direction points from the upper dust toward the lower Copper
     * Wire.
     */
    private boolean hasLowerDiagonalCopperFromUpperRedstone(
            LevelReader level,
            BlockPos upperDustPos,
            Direction direction
    ) {
        BlockPos lowerWirePos =
                upperDustPos
                        .relative(direction)
                        .below();

        if (
                !isCopperWireState(
                        level.getBlockState(
                                lowerWirePos
                        )
                )
        ) {
            return false;
        }

        return hasUpperDiagonalRedstone(
                level,
                lowerWirePos,
                direction.getOpposite()
        );
    }

    /*
     * Returns true when this Copper Wire has at least one lower-diagonal
     * redstone stair connection.
     */
    private boolean hasAnyLowerDiagonalRedstone(
            LevelReader level,
            BlockPos upperWirePos
    ) {
        for (
                Direction direction
                : Direction.Plane.HORIZONTAL
        ) {
            if (
                    hasLowerDiagonalRedstone(
                            level,
                            upperWirePos,
                            direction
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    /*
     * Returns true when a lower dust block is geometrically able to climb
     * onto the solid block beneath an upper Copper Wire.
     *
     * Layout:
     *
     *   [ Copper Wire ] [ air ]
     *   [ solid block ] [ redstone dust ]
     *
     * The direction argument points from Copper Wire toward the lower dust.
     */
    private boolean hasLowerDiagonalRedstone(
            LevelReader level,
            BlockPos upperWirePos,
            Direction direction
    ) {
        BlockPos lowerDustPos =
                upperWirePos
                        .relative(direction)
                        .below();

        BlockState lowerDustState =
                level.getBlockState(
                        lowerDustPos
                );

        if (
                !lowerDustState.is(
                        Blocks.REDSTONE_WIRE
                )
        ) {
            return false;
        }

        BlockPos supportPos =
                upperWirePos.below();

        BlockState supportState =
                level.getBlockState(
                        supportPos
                );

        if (
                !supportState.isRedstoneConductor(
                        level,
                        supportPos
                )
        ) {
            return false;
        }

        BlockPos spaceAboveDust =
                upperWirePos.relative(
                        direction
                );

        BlockState stateAboveDust =
                level.getBlockState(
                        spaceAboveDust
                );

        return !stateAboveDust.isRedstoneConductor(
                level,
                spaceAboveDust
        );
    }

    /*
     * Same stair test viewed from the lower dust position.
     */
    private boolean canDustClimbToNetworkWire(
            LevelReader level,
            BlockPos dustPos,
            Direction direction
    ) {
        BlockPos sidePos =
                dustPos.relative(direction);

        BlockPos upperTargetPos =
                sidePos.above();

        BlockState upperTargetState =
                level.getBlockState(
                        upperTargetPos
                );

        if (!isNetworkWire(upperTargetState)) {
            return false;
        }

        BlockState sideState =
                level.getBlockState(
                        sidePos
                );

        if (
                !sideState.isRedstoneConductor(
                        level,
                        sidePos
                )
        ) {
            return false;
        }

        BlockPos aboveDustPos =
                dustPos.above();

        BlockState aboveDustState =
                level.getBlockState(
                        aboveDustPos
                );

        return !aboveDustState.isRedstoneConductor(
                level,
                aboveDustPos
        );
    }

    /*
     * Updates only the model directions affected by lower diagonal
     * redstone stairs.
     *
     * Existing moving-piston links remain preserved so this does not undo
     * the earlier piston flicker correction.
     */
    private BlockState refreshRedstoneStairVisualLinks(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        /*
         * Recalculate the complete Copper visual state so both stair
         * directions update LINK_* and UP_MASK together.
         */
        BlockState refreshedState =
                addActualLinks(
                        state,
                        level,
                        pos
                );

        /*
         * Preserve a connection that is temporarily occupied by a moving
         * piston. This retains the earlier piston-flicker correction.
         */
        for (
                Direction direction
                : Direction.Plane.HORIZONTAL
        ) {
            BlockPos sidePos =
                    pos.relative(direction);

            if (
                    level.getBlockState(sidePos)
                            .is(Blocks.MOVING_PISTON)
                    && getLinkValue(
                            state,
                            direction
                    )
            ) {
                refreshedState =
                        setLinkValue(
                                refreshedState,
                                direction,
                                true
                        );
            }
        }

        return refreshedState;
    }

    /*
     * Recalculates Copper Wire blocks that may be diagonally connected to a
     * newly placed, removed, exposed, embedded, or covered Copper Wire.
     */
    private void refreshDiagonalCopperVisuals(
            Level level,
            BlockPos changedPos
    ) {
        Set<BlockPos> candidates =
                new HashSet<>();

        for (
                Direction direction
                : Direction.Plane.HORIZONTAL
        ) {
            candidates.add(
                    changedPos
                            .relative(direction)
                            .below()
            );

            candidates.add(
                    changedPos
                            .relative(direction)
                            .above()
            );
        }

        internalUpdate = true;

        try {
            for (BlockPos candidatePos : candidates) {
                BlockState candidateState =
                        level.getBlockState(candidatePos);

                if (!isCopperWireState(candidateState)) {
                    continue;
                }

                BlockState refreshedState =
                        addActualLinks(
                                candidateState,
                                level,
                                candidatePos
                        );

                if (!refreshedState.equals(candidateState)) {
                    level.setBlock(
                            candidatePos,
                            refreshedState,
                            2
                    );
                }

                level.scheduleTick(
                        candidatePos,
                        candidateState.getBlock(),
                        1
                );
            }
        } finally {
            internalUpdate = false;
        }
    }

    /*
     * Forces the lower vanilla dust's directional blockstate to agree with
     * the Copper Wire stair geometry when Copper is placed or removed.
     *
     * The mixin performs the same rule during ordinary vanilla dust state
     * calculations. This explicit refresh covers the diagonal placement
     * case where dust may not receive a direct-neighbor shape callback.
     */
    private void refreshLowerDiagonalDustStates(
            Level level,
            BlockPos upperWirePos,
            boolean copperPresent
    ) {
        for (
                Direction direction
                : Direction.Plane.HORIZONTAL
        ) {
            BlockPos lowerDustPos =
                    upperWirePos
                            .relative(direction)
                            .below();

            BlockState dustState =
                    level.getBlockState(
                            lowerDustPos
                    );

            if (
                    !dustState.is(
                            Blocks.REDSTONE_WIRE
                    )
            ) {
                continue;
            }

            Direction dustToCopper =
                    direction.getOpposite();

            RedstoneSide oldConnection =
                    getDustConnection(
                            dustState,
                            dustToCopper
                    );

            RedstoneSide newConnection =
                    copperPresent
                            && hasLowerDiagonalRedstone(
                                    level,
                                    upperWirePos,
                                    direction
                            )
                            ? RedstoneSide.UP
                            : RedstoneSide.NONE;

            if (oldConnection == newConnection) {
                continue;
            }

            BlockState newDustState =
                    setDustConnection(
                            dustState,
                            dustToCopper,
                            newConnection
                    );

            level.setBlock(
                    lowerDustPos,
                    newDustState,
                    3
            );
        }
    }

    private BlockState setDustConnection(
            BlockState dustState,
            Direction direction,
            RedstoneSide connection
    ) {
        return switch (direction) {
            case NORTH ->
                    dustState.setValue(
                            BlockStateProperties.NORTH_REDSTONE,
                            connection
                    );

            case EAST ->
                    dustState.setValue(
                            BlockStateProperties.EAST_REDSTONE,
                            connection
                    );

            case SOUTH ->
                    dustState.setValue(
                            BlockStateProperties.SOUTH_REDSTONE,
                            connection
                    );

            case WEST ->
                    dustState.setValue(
                            BlockStateProperties.WEST_REDSTONE,
                            connection
                    );

            default -> dustState;
        };
    }

    /*
     * Reads the directional RedstoneSide property stored by vanilla dust.
     */
    private RedstoneSide getDustConnection(
            BlockState dustState,
            Direction direction
    ) {
        return switch (direction) {
            case NORTH ->
                    dustState.getValue(
                            BlockStateProperties.NORTH_REDSTONE
                    );

            case EAST ->
                    dustState.getValue(
                            BlockStateProperties.EAST_REDSTONE
                    );

            case SOUTH ->
                    dustState.getValue(
                            BlockStateProperties.SOUTH_REDSTONE
                    );

            case WEST ->
                    dustState.getValue(
                            BlockStateProperties.WEST_REDSTONE
                    );

            default ->
                    RedstoneSide.NONE;
        };
    }

    private boolean isNetworkWire(
            BlockState state
    ) {
        return isCopperWireState(state)
                || state.is(
                        Blocks.REDSTONE_WIRE
                );
    }

    /*
     * Both exposed Copper Wire and Embedded Copper Wire extend this class.
     * Treating them as one family allows power, model links, and mixed
     * Dust/Copper calculations to pass directly between the two variants.
     */
    private boolean isCopperWireState(
            BlockState state
    ) {
        return state.getBlock()
                instanceof CopperWireBlock;
    }

    private BlockState addActualLinks(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        BlockState updatedState = state;
        int upMask = 0;

        for (
                Direction direction
                : Direction.Plane.HORIZONTAL
        ) {
            boolean upwardConnection =
                    hasUpwardCopperLink(
                            level,
                            pos,
                            direction
                    )
                            || hasUpperDiagonalRedstone(
                                    level,
                                    pos,
                                    direction
                            );

            boolean linked =
                    upwardConnection
                            || hasHorizontalLink(
                                    level,
                                    pos,
                                    direction
                            );

            updatedState =
                    setLinkValue(
                            updatedState,
                            direction,
                            linked
                    );

            if (upwardConnection) {
                upMask |=
                        directionBit(direction);
            }
        }

        /*
         * Keep every valid upward branch. A two-, three-, or four-way node
         * therefore stores every corresponding direction bit instead of
         * discarding all but Integer.lowestOneBit(...).
         */
        return updatedState.setValue(
                UP_MASK,
                upMask
        );
    }

    private boolean hasHorizontalLink(
            LevelReader level,
            BlockPos wirePos,
            Direction direction
    ) {
        BlockPos targetPos =
                wirePos.relative(
                        direction
                );

        BlockState targetState =
                level.getBlockState(
                        targetPos
                );

        return isVisibleConnection(
                targetState
        )
                /*
                 * A lower redstone-dust block may climb onto the solid
                 * support block beneath this Copper Wire. From Copper's
                 * position that dust is one block outward and one block
                 * down, so it is not a normal horizontal neighbor.
                 */
                || hasLowerDiagonalRedstone(
                        level,
                        wirePos,
                        direction
                )
                || hasLowerDiagonalCopperWire(
                        level,
                        wirePos,
                        direction
                )
                /*
                 * A wall-mounted Iron Lever powers the solid block it is
                 * attached to. Treat that support block as the visible
                 * Copper Wire terminal even though the lever occupies a
                 * different block position.
                 */
                || hasAttachedIronLever(
                        level,
                        targetPos
                );
    }

    /*
     * Reads ordinary power touching the wire and genuine STRONG power
     * carried through an adjacent solid conductor.
     *
     * The older version used hasNeighborSignal(relayPos). That method also
     * accepts weak power. As a result, merely placing a solid block beside
     * powered Copper could make that block look like a new external source
     * and restart the Copper network.
     *
     * getDirectSignalTo(relayPos) checks only strong/direct power received
     * by the conductor. This preserves intended relay behavior from an
     * attached lever or redstone dust strongly powering its support block,
     * while rejecting a block that is only weakly powered.
     */
    private int getExternalPowerAtWire(
            ServerLevel level,
            BlockPos wirePos
    ) {
        int strongestPower =
                level.getBestNeighborSignal(
                        wirePos
                );

        if (strongestPower >= 15) {
            return strongestPower;
        }

        for (Direction direction : Direction.values()) {
            BlockPos relayPos =
                    wirePos.relative(
                            direction
                    );

            BlockState relayState =
                    level.getBlockState(
                            relayPos
                    );

            if (
                    !relayState.isRedstoneConductor(
                            level,
                            relayPos
                    )
            ) {
                continue;
            }

            int directPower =
                    level.getDirectSignalTo(
                            relayPos
                    );

            if (directPower > strongestPower) {
                strongestPower = directPower;
            }

            if (strongestPower >= 15) {
                break;
            }
        }

        return strongestPower;
    }

    /*
     * Checks all six sides of a candidate support block for one of
     * DungeonCraft's Iron Levers that is actually mounted on that block.
     *
     * The lever may be on the wall, floor, or ceiling side.
     */
    private boolean hasAttachedIronLever(
            LevelReader level,
            BlockPos supportPos
    ) {
        for (Direction direction : Direction.values()) {
            BlockPos leverPos =
                    supportPos.relative(
                            direction
                    );

            BlockState leverState =
                    level.getBlockState(
                            leverPos
                    );

            if (
                    leverState.getBlock()
                            instanceof IronLeverBlock ironLever
                    && ironLever.isAttachedToBlock(
                            leverState,
                            leverPos,
                            supportPos
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    private boolean isVisibleConnection(
            BlockState targetState
    ) {
        return isCopperWireState(targetState)
                || targetState.is(
                        COPPER_WIRE_CONNECTABLE
                );
    }

    private int directionBit(
            Direction direction
    ) {
        return switch (direction) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 4;
            case WEST -> 8;
            default -> 0;
        };
    }

    private boolean getLinkValue(
            BlockState state,
            Direction direction
    ) {
        return switch (direction) {
            case NORTH ->
                    state.getValue(LINK_NORTH);

            case EAST ->
                    state.getValue(LINK_EAST);

            case SOUTH ->
                    state.getValue(LINK_SOUTH);

            case WEST ->
                    state.getValue(LINK_WEST);

            default -> false;
        };
    }

    private BlockState setLinkValue(
            BlockState state,
            Direction direction,
            boolean value
    ) {
        return switch (direction) {
            case NORTH ->
                    state.setValue(
                            LINK_NORTH,
                            value
                    );

            case EAST ->
                    state.setValue(
                            LINK_EAST,
                            value
                    );

            case SOUTH ->
                    state.setValue(
                            LINK_SOUTH,
                            value
                    );

            case WEST ->
                    state.setValue(
                            LINK_WEST,
                            value
                    );

            default -> state;
        };
    }

    private record NetworkResult(
            Map<BlockPos, Integer> powerByPosition,
            boolean containsDust,
            boolean anyCalculatedPower,
            boolean anyOriginalDustPower
    ) {
    }
}
