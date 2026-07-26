package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.StonekeepLowDungeonChestBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Master block for the Stonekeep Low Dungeon Chest.
 *
 * One placed chest owns exactly one inventory/block entity. Technical helper
 * blocks reserve the second cardinal cell or the two additional diagonal
 * cells and redirect interaction back to this master.
 */
public class StonekeepLowDungeonChestBlock extends BaseEntityBlock {
    public static final EnumProperty<StonekeepChestFacing> FACING =
            EnumProperty.create("facing", StonekeepChestFacing.class);

    public static final MapCodec<StonekeepLowDungeonChestBlock> CODEC =
            simpleCodec(StonekeepLowDungeonChestBlock::new);

    public StonekeepLowDungeonChestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(FACING, StonekeepChestFacing.NORTH)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(
                FACING,
                StonekeepChestFacing.fromPlacementYaw(context.getRotation())
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return StonekeepLowDungeonChestShapes.forMaster(state);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return StonekeepLowDungeonChestShapes.forMaster(state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state,
            Level level,
            BlockPos pos,
            Direction direction
    ) {
        if (level.getBlockEntity(pos) instanceof StonekeepLowDungeonChestBlockEntity chest) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(chest);
        }
        return 0;
    }

    /**
     * Returns true when a full, redstone-conducting block is directly above
     * any occupied lid cell.
     *
     * Cardinal chests check the master and one helper cell. Diagonal chests
     * check the master and both helper cells. This mirrors vanilla chest lid
     * blocking while covering the complete custom footprint.
     */
    public static boolean isLidBlocked(
            Level level,
            BlockPos masterPos,
            BlockState masterState
    ) {
        if (isCellBlockedAbove(level, masterPos)) {
            return true;
        }

        StonekeepChestFacing facing = masterState.getValue(FACING);
        for (Direction helperDirection : facing.helperDirections()) {
            if (isCellBlockedAbove(level, masterPos.relative(helperDirection))) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCellBlockedAbove(Level level, BlockPos chestCellPos) {
        BlockPos abovePos = chestCellPos.above();
        return level.getBlockState(abovePos).isRedstoneConductor(level, abovePos);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide()
                && !isLidBlocked(level, pos, state)
                && level.getBlockEntity(pos) instanceof StonekeepLowDungeonChestBlockEntity chest) {
            player.openMenu(chest);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean triggerEvent(
            BlockState state,
            Level level,
            BlockPos pos,
            int eventId,
            int eventData
    ) {
        if (eventId == 1
                && level.getBlockEntity(pos) instanceof StonekeepLowDungeonChestBlockEntity chest) {
            chest.setOpenPlayerCount(eventData);
            return true;
        }
        return super.triggerEvent(state, level, pos, eventId, eventData);
    }

    /** Removes only helper parts that still point back to this master. */
    private void removeHelperParts(ServerLevel level, BlockPos masterPos, BlockState state) {
        StonekeepChestFacing facing = state.getValue(FACING);
        for (Direction offset : facing.helperDirections()) {
            BlockPos helperPos = masterPos.relative(offset);
            BlockState helperState = level.getBlockState(helperPos);
            if (helperState.is(DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_PART)
                    && helperState.getValue(StonekeepLowDungeonChestPartBlock.TO_MASTER)
                    == offset.getOpposite()) {
                level.removeBlock(helperPos, false);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        this.removeHelperParts(level, pos, state);
        Containers.updateNeighboursAfterDestroy(state, level, pos);
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
                DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_BLOCK_ENTITY,
                StonekeepLowDungeonChestBlockEntity::tick
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StonekeepLowDungeonChestBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, state.getValue(FACING).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, state.getValue(FACING).mirror(mirror));
    }
}
