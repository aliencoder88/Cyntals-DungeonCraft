package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.StonekeepLowDungeonChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible footprint part for the Stonekeep chest.
 *
 * It has no item and no loot.  Every part stores the one-step horizontal
 * direction back to the master block.
 */
public class StonekeepLowDungeonChestPartBlock extends Block
        implements WorldlyContainerHolder {
    public static final EnumProperty<Direction> TO_MASTER =
            BlockStateProperties.HORIZONTAL_FACING;

    public StonekeepLowDungeonChestPartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(TO_MASTER, Direction.NORTH)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TO_MASTER);
    }

    public static BlockPos getMasterPos(BlockPos partPos, BlockState partState) {
        return partPos.relative(partState.getValue(TO_MASTER));
    }

    /*
     * Vanilla hopper lookup checks WorldlyContainerHolder before looking for
     * a block entity at this exact position. Redirect that lookup to the one
     * master block entity so hoppers work on every occupied footprint cell.
     */
    @Override
    public WorldlyContainer getContainer(
            BlockState state,
            LevelAccessor level,
            BlockPos pos
    ) {
        BlockPos masterPos = getMasterPos(pos, state);

        if (level.getBlockState(masterPos)
                .is(DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST)
                && level.getBlockEntity(masterPos)
                instanceof StonekeepLowDungeonChestBlockEntity chest) {
            return chest;
        }

        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return StonekeepLowDungeonChestShapes.forPart(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return StonekeepLowDungeonChestShapes.forPart(state, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        BlockPos masterPos = getMasterPos(pos, state);
        BlockState masterState = level.getBlockState(masterPos);

        if (!level.isClientSide()
                && masterState.is(DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST)
                && !StonekeepLowDungeonChestBlock.isLidBlocked(
                level,
                masterPos,
                masterState
        )
                && level.getBlockEntity(masterPos)
                instanceof StonekeepLowDungeonChestBlockEntity chest) {
            player.openMenu(chest);
        }

        return InteractionResult.SUCCESS;
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
        BlockPos masterPos = getMasterPos(pos, state);
        if (level.getBlockEntity(masterPos) instanceof StonekeepLowDungeonChestBlockEntity chest) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(chest);
        }
        return 0;
    }

    /**
     * Breaking any helper destroys the master.  The master's removal hook then
     * clears every remaining helper.  This produces one chest item and one set
     * of inventory drops rather than one drop per occupied cell.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos masterPos = getMasterPos(pos, state);
            if (level.getBlockState(masterPos).is(DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST)) {
                level.destroyBlock(masterPos, !player.isCreative(), player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(TO_MASTER, rotation.rotate(state.getValue(TO_MASTER)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(TO_MASTER)));
    }
}
