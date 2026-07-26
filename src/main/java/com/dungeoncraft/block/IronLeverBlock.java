package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.IronLeverBlockEntity;
import com.dungeoncraft.config.DeviceSignalMode;
import com.dungeoncraft.network.CodingToolNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Animated iron lever with per-block Coding Tool configuration.
 */
public class IronLeverBlock extends LeverBlock implements EntityBlock {
    /**
     * Stores the Iron Lever's current animation frame.
     * 0 = fully off, 4 = fully on.
     */
    public static final IntegerProperty LEVER_STEP =
            IntegerProperty.create("lever_step", 0, 4);

    private static final int ANIMATION_TICK_DELAY = 1;

    public IronLeverBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.defaultBlockState().setValue(LEVER_STEP, 0)
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVER_STEP);
    }

    @Override
    public void animateTick(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random
    ) {
        // Intentionally empty: suppress vanilla redstone particles.
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
        if (!stack.is(DungeonCraft.CODING_TOOL)) {
            return super.useItemOn(
                    stack,
                    state,
                    level,
                    pos,
                    player,
                    hand,
                    hitResult
            );
        }

        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos)
                instanceof IronLeverBlockEntity blockEntity) {
            CodingToolNetworking.openScreen(
                    serverPlayer,
                    pos,
                    blockEntity
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide()) {
            pull(state, level, pos, player);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void pull(BlockState state, Level level, BlockPos pos, Player player) {
        BlockState newState = state.cycle(POWERED);

        level.setBlock(pos, newState, 2);
        notifyConfiguredOutputNeighbors(level, pos, newState);

        playSound(player, level, pos, newState);

        level.gameEvent(
                player,
                newState.getValue(POWERED)
                        ? GameEvent.BLOCK_ACTIVATE
                        : GameEvent.BLOCK_DEACTIVATE,
                pos
        );

        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, ANIMATION_TICK_DELAY);
        }
    }

    /**
     * Returns true when this lever is physically mounted on the supplied block.
     */
    public boolean isAttachedToBlock(
            BlockState state,
            BlockPos leverPos,
            BlockPos supportPos
    ) {
        BlockPos attachedBlockPos =
                leverPos.relative(
                        getConnectedDirection(state).getOpposite()
                );

        return attachedBlockPos.equals(supportPos);
    }

    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        int currentStep = state.getValue(LEVER_STEP);
        int targetStep = state.getValue(POWERED) ? 4 : 0;

        if (currentStep == targetStep) {
            return;
        }

        int nextStep = currentStep + Integer.compare(targetStep, currentStep);
        BlockState nextState = state.setValue(LEVER_STEP, nextStep);
        level.setBlock(pos, nextState, 2);

        if (nextStep != targetStep) {
            level.scheduleTick(pos, this, ANIMATION_TICK_DELAY);
        }
    }

    private static boolean emitsRegularSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        boolean physicallyActivated = state.getValue(POWERED);

        if (level.getBlockEntity(pos) instanceof IronLeverBlockEntity blockEntity) {
            return blockEntity.isElectricallyOn(physicallyActivated)
                    && blockEntity.getSignalMode()
                    == DeviceSignalMode.REGULAR_REDSTONE;
        }

        return physicallyActivated;
    }

    @Override
    protected int getSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return emitsRegularSignal(state, level, pos) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return emitsRegularSignal(state, level, pos)
                && getConnectedDirection(state) == direction
                ? 15
                : 0;
    }

    public static void notifyConfiguredOutputNeighbors(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(
                pos.relative(getConnectedDirection(state).getOpposite()),
                state.getBlock()
        );
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronLeverBlockEntity(pos, state);
    }
}
