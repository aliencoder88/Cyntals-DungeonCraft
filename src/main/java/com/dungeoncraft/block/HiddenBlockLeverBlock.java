package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.HiddenBlockLeverBlockEntity;
import com.dungeoncraft.config.DeviceSignalMode;
import com.dungeoncraft.config.HiddenLeverOutputFace;
import com.dungeoncraft.config.HiddenPanelWiringMode;
import com.dungeoncraft.network.CodingToolNetworking;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * First Stonekeep concealed-lever implementation.
 *
 * The lower front panel opens only from the authored front face. Once fully
 * open, the centered pull-bar can be flipped. The Coding Tool controls output
 * routing plus three concealed-panel wiring modes and lock-signal polarity.
 */
public class HiddenBlockLeverBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty PANEL_OPEN =
            BooleanProperty.create("panel_open");
    public static final BooleanProperty POWERED =
            BooleanProperty.create("powered");
    /**
     * True only after the panel has finished closing. While true, Minecraft's
     * normal chunk renderer draws the full Aged Limestone Bricks cube so its
     * lighting and ambient occlusion match neighboring blocks exactly.
     */
    public static final BooleanProperty RENDER_CLOSED =
            BooleanProperty.create("render_closed");

    public static final MapCodec<HiddenBlockLeverBlock> CODEC =
            simpleCodec(HiddenBlockLeverBlock::new);

    private static final double LOWER_PANEL_MAX_Y = 0.5D;
    private static final double CLOSE_REGION_MIN_Y = 0.55D;
    private static final double FRONT_APERTURE_MAX_Y = 0.53D;
    private static final double LEVER_MIN_Y = 0.08D;
    private static final double LEVER_MAX_Y = 0.58D;
    private static final double LEVER_MIN_HORIZONTAL = 0.18D;
    private static final double LEVER_MAX_HORIZONTAL = 0.82D;
    private static final double LEVER_MIN_DEPTH = 0.28D;
    private static final double LEVER_MAX_DEPTH = 0.82D;
    private static final double RAY_EPSILON = 1.0E-7D;

    public HiddenBlockLeverBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(PANEL_OPEN, false)
                        .setValue(POWERED, false)
                        .setValue(RENDER_CLOSED, true)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide()) {
            updatePanelFromWiring(level, pos, state);
        }
    }

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
            updatePanelFromWiring(level, pos, state);
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, PANEL_OPEN, POWERED, RENDER_CLOSED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        /*
         * The blockstate model renders either the fully closed cube or the
         * stationary open shell. The block-entity renderer is now reserved
         * for the moving panel and lever only.
         */
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return this.handleInteraction(
                ItemStack.EMPTY,
                state,
                level,
                pos,
                player,
                hitResult
        );
    }

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
         * The Coding Tool is detected after the front-only ray reaches the
         * exposed lever. Other held items use the same panel and lever controls
         * as an empty hand.
         */
        return this.handleInteraction(
                stack,
                state,
                level,
                pos,
                player,
                hitResult
        );
    }

    private InteractionResult handleInteraction(
            ItemStack heldStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        Direction facing = state.getValue(FACING);
        double localY = hitResult.getLocation().y - pos.getY();

        /*
         * A closed concealed panel is still opened only by clicking its real
         * lower front face. Side, top, bottom, and back clicks remain available
         * for normal building interactions.
         */
        if (!state.getValue(PANEL_OPEN)) {
            if (hitResult.getDirection() != facing
                    || localY > LOWER_PANEL_MAX_Y
                    || !this.canOpenPanel(state, level, pos, player)) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide()) {
                setPanelOpen(level, pos, state, true);
            }

            return InteractionResult.SUCCESS;
        }

        FrontRay frontRay = getFrontRay(state, pos, player);

        /*
         * Once open, every mechanism interaction must come through the authored
         * front plane. Requiring the player's eye to be in front of that plane
         * prevents reaching the lever through a solid side or from behind.
         */
        if (frontRay == null) {
            return InteractionResult.PASS;
        }

        /*
         * Use the sight ray rather than Minecraft's reported clicked face for
         * the upper close region. At steep angles the full-block hit result can
         * report a side or top even though the crosshair is visibly on the
         * upper front half.
         */
        if (frontRay.frontY() >= CLOSE_REGION_MIN_Y) {
            if (!level.isClientSide()
                    && !isPanelAutomaticallyControlled(level, pos)) {
                setPanelOpen(level, pos, state, false);
            }

            return InteractionResult.SUCCESS;
        }

        /*
         * The lower opening is an interaction-only cavity. Consume front-cavity
         * clicks even when the lever itself was missed so held blocks cannot be
         * placed into or over the mechanism by accident.
         */
        if (frontRay.frontY() > FRONT_APERTURE_MAX_Y) {
            return InteractionResult.SUCCESS;
        }

        if (!isLeverTargeted(frontRay)) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof HiddenBlockLeverBlockEntity blockEntity)
                || !blockEntity.isPanelFullyOpen()) {
            return InteractionResult.SUCCESS;
        }

        /*
         * The Coding Tool opens this device's page only when it is used
         * directly on the exposed pull bar. It does not flip the lever.
         */
        if (heldStack.is(DungeonCraft.CODING_TOOL)) {
            if (!level.isClientSide()
                    && player instanceof ServerPlayer serverPlayer) {
                CodingToolNetworking.openScreen(
                        serverPlayer,
                        pos,
                        blockEntity
                );
            }

            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide()) {
            toggleLever(level, pos, state);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Called by the BlockItem mixin when vanilla gives crouched block placement
     * priority over the block's normal use callback. A non-PASS result cancels
     * placement and performs the same front-only panel or lever interaction.
     */
    public InteractionResult handleBlockItemPlacementBypass(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return this.handleInteraction(ItemStack.EMPTY, state, level, pos, player, hitResult);
    }

    /**
     * Checks the configured panel wiring before a manual open attempt.
     *
     * Locked / Unlocked mode blocks opening while locked but still permits a
     * player to close an already-open panel. The automatic mode ignores manual
     * opening and closing because the panel state follows the wiring directly.
     */
    protected boolean canOpenPanel(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof HiddenBlockLeverBlockEntity blockEntity)) {
            return true;
        }

        HiddenPanelWiringMode wiringMode =
                blockEntity.getPanelWiringMode();

        if (wiringMode == HiddenPanelWiringMode.UNLOCKED) {
            return true;
        }

        if (wiringMode
                == HiddenPanelWiringMode.LOCKED_CLOSED_UNLOCKED_OPEN) {
            return false;
        }

        boolean authorizationSignalPresent =
                hasPanelAuthorizationSignal(
                        level,
                        pos,
                        state,
                        blockEntity
                );

        return !blockEntity.isPanelLocked(authorizationSignalPresent);
    }

    private static boolean isPanelAutomaticallyControlled(
            Level level,
            BlockPos pos
    ) {
        return level.getBlockEntity(pos)
                instanceof HiddenBlockLeverBlockEntity blockEntity
                && blockEntity.getPanelWiringMode()
                == HiddenPanelWiringMode.LOCKED_CLOSED_UNLOCKED_OPEN;
    }

    /**
     * Re-evaluates the configured panel input and applies automatic behavior.
     *
     * Regular redstone is active in this checkpoint. Verified authorization is
     * saved, including its required key, but remains inactive until the verified
     * signal network is implemented.
     */
    public static void updatePanelFromWiring(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        if (level.isClientSide()
                || !(level.getBlockEntity(pos)
                instanceof HiddenBlockLeverBlockEntity blockEntity)) {
            return;
        }

        if (blockEntity.getPanelWiringMode()
                != HiddenPanelWiringMode.LOCKED_CLOSED_UNLOCKED_OPEN) {
            return;
        }

        boolean authorizationSignalPresent =
                hasPanelAuthorizationSignal(
                        level,
                        pos,
                        state,
                        blockEntity
                );
        boolean locked = blockEntity.isPanelLocked(
                authorizationSignalPresent
        );
        boolean shouldOpen = !locked;

        if (state.getValue(PANEL_OPEN) != shouldOpen) {
            setPanelOpen(level, pos, state, shouldOpen);
        }
    }

    private static boolean hasPanelAuthorizationSignal(
            Level level,
            BlockPos pos,
            BlockState state,
            HiddenBlockLeverBlockEntity blockEntity
    ) {
        if (blockEntity.getPanelSignalMode()
                == DeviceSignalMode.VERIFIED_SIGNAL) {
            /*
             * Reserved for the provenance-preserving verified network. An empty
             * or unmatched key will never be treated as a wildcard.
             */
            return false;
        }

        Direction facing = state.getValue(FACING);
        int inputFaceMask = blockEntity.getPanelInputFaceMask();

        for (HiddenLeverOutputFace inputFace
                : HiddenLeverOutputFace.values()) {
            if (!inputFace.isEnabled(inputFaceMask)) {
                continue;
            }

            Direction worldDirection =
                    inputFace.getWorldOutputDirection(facing);
            BlockPos neighborPos = pos.relative(worldDirection);

            if (level.getSignal(neighborPos, worldDirection) > 0) {
                return true;
            }
        }

        return false;
    }

    private static @Nullable FrontRay getFrontRay(
            BlockState state,
            BlockPos pos,
            Player player
    ) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Direction facing = state.getValue(FACING);

        double worldX = eye.x - pos.getX();
        double worldY = eye.y - pos.getY();
        double worldZ = eye.z - pos.getZ();

        double horizontalOrigin;
        double horizontalDirection;
        double depthOrigin;
        double depthDirection;

        switch (facing) {
            case NORTH -> {
                horizontalOrigin = worldX;
                horizontalDirection = view.x;
                depthOrigin = worldZ;
                depthDirection = view.z;
            }
            case SOUTH -> {
                horizontalOrigin = worldX;
                horizontalDirection = view.x;
                depthOrigin = 1.0D - worldZ;
                depthDirection = -view.z;
            }
            case WEST -> {
                horizontalOrigin = worldZ;
                horizontalDirection = view.z;
                depthOrigin = worldX;
                depthDirection = view.x;
            }
            case EAST -> {
                horizontalOrigin = worldZ;
                horizontalDirection = view.z;
                depthOrigin = 1.0D - worldX;
                depthDirection = -view.x;
            }
            default -> {
                return null;
            }
        }

        /*
         * The eye must be outside or directly on the front plane and looking
         * inward. This is the front-only access guarantee for angled rays.
         */
        if (depthOrigin > RAY_EPSILON || depthDirection <= RAY_EPSILON) {
            return null;
        }

        double frontT = -depthOrigin / depthDirection;
        if (frontT < 0.0D) {
            return null;
        }

        double frontHorizontal = horizontalOrigin
                + horizontalDirection * frontT;
        double frontY = worldY + view.y * frontT;

        if (frontHorizontal < 0.0D || frontHorizontal > 1.0D
                || frontY < 0.0D || frontY > 1.0D) {
            return null;
        }

        return new FrontRay(
                horizontalOrigin,
                worldY,
                depthOrigin,
                horizontalDirection,
                view.y,
                depthDirection,
                frontT,
                frontHorizontal,
                frontY
        );
    }

    private static boolean isLeverTargeted(FrontRay ray) {
        return rayIntersectsLeverVolume(
                ray.horizontalOrigin(),
                ray.yOrigin(),
                ray.depthOrigin(),
                ray.horizontalDirection(),
                ray.yDirection(),
                ray.depthDirection(),
                ray.frontT()
        );
    }

    private record FrontRay(
            double horizontalOrigin,
            double yOrigin,
            double depthOrigin,
            double horizontalDirection,
            double yDirection,
            double depthDirection,
            double frontT,
            double frontHorizontal,
            double frontY
    ) {
    }

    private static boolean rayIntersectsLeverVolume(
            double horizontalOrigin,
            double yOrigin,
            double depthOrigin,
            double horizontalDirection,
            double yDirection,
            double depthDirection,
            double minimumT
    ) {
        double[] interval = {minimumT, Double.POSITIVE_INFINITY};

        if (!clipRayAxis(
                horizontalOrigin,
                horizontalDirection,
                LEVER_MIN_HORIZONTAL,
                LEVER_MAX_HORIZONTAL,
                interval
        )) {
            return false;
        }

        if (!clipRayAxis(
                yOrigin,
                yDirection,
                LEVER_MIN_Y,
                LEVER_MAX_Y,
                interval
        )) {
            return false;
        }

        return clipRayAxis(
                depthOrigin,
                depthDirection,
                LEVER_MIN_DEPTH,
                LEVER_MAX_DEPTH,
                interval
        );
    }

    private static boolean clipRayAxis(
            double origin,
            double direction,
            double minimum,
            double maximum,
            double[] interval
    ) {
        if (Math.abs(direction) < RAY_EPSILON) {
            return origin >= minimum && origin <= maximum;
        }

        double first = (minimum - origin) / direction;
        double second = (maximum - origin) / direction;

        if (first > second) {
            double swap = first;
            first = second;
            second = swap;
        }

        interval[0] = Math.max(interval[0], first);
        interval[1] = Math.min(interval[1], second);
        return interval[1] >= interval[0];
    }

    private static void setPanelOpen(
            Level level,
            BlockPos pos,
            BlockState state,
            boolean open
    ) {
        /*
         * Hide the baked full-cube model before either animation starts.
         * The stationary open shell remains chunk-rendered while the moving
         * panel is handled by the block-entity renderer.
         */
        BlockState updatedState = state
                .setValue(PANEL_OPEN, open)
                .setValue(RENDER_CLOSED, false);

        level.setBlock(
                pos,
                updatedState,
                Block.UPDATE_ALL
        );

        level.playSound(
                null,
                pos,
                open ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND,
                SoundSource.BLOCKS,
                0.45F,
                open ? 0.9F : 0.8F
        );
    }

    private static void toggleLever(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        BlockState newState = state.cycle(POWERED);

        level.setBlock(pos, newState, Block.UPDATE_ALL);
        notifyConfiguredOutputNeighbors(level, pos, newState);

        level.playSound(
                null,
                pos,
                SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                0.3F,
                newState.getValue(POWERED) ? 0.6F : 0.5F
        );
    }

    public static void notifyConfiguredOutputNeighbors(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        Direction facing = state.getValue(FACING);

        /*
         * Notify every face that can be configured, not only the currently
         * selected faces. This clears stale power immediately when the Coding
         * Tool removes an output from a face.
         */
        level.updateNeighborsAt(pos, state.getBlock());

        for (HiddenLeverOutputFace outputFace
                : HiddenLeverOutputFace.values()) {
            level.updateNeighborsAt(
                    pos.relative(
                            outputFace.getWorldOutputDirection(facing)
                    ),
                    state.getBlock()
            );
        }
    }

    private static boolean emitsRegularSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        boolean physicallyActivated = state.getValue(POWERED);

        if (level.getBlockEntity(pos) instanceof HiddenBlockLeverBlockEntity blockEntity) {
            return blockEntity.isElectricallyOn(physicallyActivated)
                    && blockEntity.getSignalMode()
                    == DeviceSignalMode.REGULAR_REDSTONE;
        }

        return physicallyActivated;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        if (!emitsRegularSignal(state, level, pos)) {
            return 0;
        }

        int outputFaceMask = HiddenLeverOutputFace.defaultMask();

        if (level.getBlockEntity(pos)
                instanceof HiddenBlockLeverBlockEntity blockEntity) {
            outputFaceMask = blockEntity.getOutputFaceMask();
        }

        Direction facing = state.getValue(FACING);

        for (HiddenLeverOutputFace outputFace
                : HiddenLeverOutputFace.values()) {
            if (!outputFace.isEnabled(outputFaceMask)) {
                continue;
            }

            /*
             * Minecraft asks the source from the direction pointing from the
             * receiving neighbor back toward this block. That is the opposite
             * of the physical direction in which power leaves the lever.
             */
            if (outputFace
                    .getWorldOutputDirection(facing)
                    .getOpposite() == direction) {
                return 15;
            }
        }

        return 0;
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return this.getSignal(state, level, pos, direction);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        notifyConfiguredOutputNeighbors(level, pos, state);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return createTickerHelper(
                type,
                DungeonCraft.HIDDEN_BLOCK_LEVER_BLOCK_ENTITY,
                HiddenBlockLeverBlockEntity::tick
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HiddenBlockLeverBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
