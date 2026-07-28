package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.PowerDiverterBlockEntity;
import com.dungeoncraft.config.PowerDiverterPort;
import com.dungeoncraft.network.CodingToolNetworking;
import com.mojang.serialization.MapCodec;

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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Four-port Power Router configured with the Coding Tool.
 *
 * Ports 1 through 4 are fixed to North, East, South, and West. Up and down are
 * structural faces and never transmit or receive routed power. The registered
 * block ID remains power_diverter so existing worlds keep their placed blocks.
 */
public class PowerDiverterBlock extends BaseEntityBlock {
    public static final MapCodec<PowerDiverterBlock> CODEC =
            simpleCodec(PowerDiverterBlock::new);
    private static final int RECALCULATION_DELAY = 1;
    private static final VoxelShape SHAPE =
            Block.box(0, 0, 0, 16, 8, 16);

    public PowerDiverterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
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
            scheduleRecalculation(level, pos);
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
            scheduleRecalculation(level, pos);
        }
    }

    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (level.getBlockEntity(pos)
                instanceof PowerDiverterBlockEntity blockEntity) {
            blockEntity.recalculate(level, pos, state);
        }
    }

    public static void scheduleRecalculation(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            level.scheduleTick(
                    pos,
                    level.getBlockState(pos).getBlock(),
                    RECALCULATION_DELAY
            );
        }
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
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos)
                instanceof PowerDiverterBlockEntity blockEntity) {
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
        return InteractionResult.PASS;
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
        PowerDiverterPort outputPort =
                PowerDiverterPort.fromDirection(direction.getOpposite());

        if (outputPort == null
                || !(level.getBlockEntity(pos)
                instanceof PowerDiverterBlockEntity blockEntity)) {
            return 0;
        }

        return blockEntity.getOutputStrength(outputPort);
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

    public static void notifyAllPorts(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        level.updateNeighborsAt(pos, state.getBlock());

        for (PowerDiverterPort port : PowerDiverterPort.values()) {
            level.updateNeighborsAt(
                    pos.relative(port.getDirection()),
                    state.getBlock()
            );
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        notifyAllPorts(level, pos, state);
        super.affectNeighborsAfterRemoval(
                state,
                level,
                pos,
                movedByPiston
        );
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(
                type,
                DungeonCraft.POWER_DIVERTER_BLOCK_ENTITY,
                PowerDiverterBlockEntity::serverTick
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new PowerDiverterBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
