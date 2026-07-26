package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.BasicTreasureChestBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.Nullable;

/*
 * The physical Basic Treasure Chest block.
 *
 * This class is responsible for:
 *
 * - existing at a position in the world;
 * - creating its block entity;
 * - telling Minecraft that a custom renderer will draw it;
 * - later handling facing, opening, and double-chest behavior.
 */
public class BasicTreasureChestBlock extends BaseEntityBlock {

    /*
     * Stores which horizontal direction the front of the chest faces.
     */
    public static final Property<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    /*
     * Approximate physical shape of the closed chest.
     *
     * Block.box(...) uses model coordinates from 0 to 16.
     */
    private static final VoxelShape SHAPE =
            box(
                    1.0,
                    0.0,
                    1.0,
                    15.0,
                    13.0,
                    15.0
            );

    /*
     * A codec describes how Minecraft can recreate this
     * block from its registered block properties.
     */
    public static final MapCodec<BasicTreasureChestBlock> CODEC =
            simpleCodec(BasicTreasureChestBlock::new);

    public BasicTreasureChestBlock(
            Properties properties
    ) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
        );
    }

    /*
     * Tells comparators that this block can provide
     * an analog redstone signal.
     */
    @Override
    protected boolean hasAnalogOutputSignal(
            BlockState blockState
    ) {
        return true;
    }

    /*
     * Returns a signal from 0 to 15 based on how full
     * the Basic Treasure Chest is.
     */
    @Override
    protected int getAnalogOutputSignal(
            BlockState blockState,
            Level level,
            BlockPos blockPos,
            Direction direction
    ) {
        if (
                level.getBlockEntity(blockPos)
                        instanceof BasicTreasureChestBlockEntity chest
        ) {
            return AbstractContainerMenu
                    .getRedstoneSignalFromContainer(
                            chest
                    );
        }

        return 0;
    }

    /*
     * Makes the front of the chest face the player
     * who placed it.
     */
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return this.defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                                .getOpposite()
                );
    }

    /*
     * Adds FACING to this block's valid blockstate properties.
     */
    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING
        );
    }

    /*
     * Controls the chest's physical collision.
     */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState blockState,
            BlockGetter blockGetter,
            BlockPos blockPos,
            CollisionContext collisionContext
    ) {
        return SHAPE;
    }

    /*
     * Controls the selection outline and the normal physical
     * collision shape of the chest.
     */
    @Override
    protected VoxelShape getShape(
            BlockState blockState,
            BlockGetter blockGetter,
            BlockPos blockPos,
            CollisionContext collisionContext
    ) {
        return SHAPE;
    }

    /*
     * Receives synchronized block events and forwards the
     * opener count to this chest's block entity.
     */
    @Override
    protected boolean triggerEvent(
            BlockState blockState,
            Level level,
            BlockPos blockPos,
            int eventId,
            int eventData
    ) {
        if (
                eventId == 1
                        && level.getBlockEntity(blockPos)
                        instanceof BasicTreasureChestBlockEntity chest
        ) {
            chest.setOpenPlayerCount(
                    eventData
            );

            return true;
        }

        return super.triggerEvent(
                blockState,
                level,
                blockPos,
                eventId,
                eventData
        );
    }

    /*
     * Opens the Basic Treasure Chest menu when the player
     * right-clicks the block.
     */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState blockState,
            Level level,
            BlockPos blockPos,
            Player player,
            BlockHitResult blockHitResult
    ) {
        if (
                !level.isClientSide()
                        && level.getBlockEntity(blockPos)
                        instanceof BasicTreasureChestBlockEntity chest
        ) {
            player.openMenu(chest);
        }

        return InteractionResult.SUCCESS;
    }

    /*
     * Drops the chest's stored contents when the block is
     * actually replaced by a different block.
     */

    /*
     * Notifies neighboring blocks, such as comparators,
     * after the chest is removed.
     *
     * Container contents are dropped automatically by
     * BlockEntity.preRemoveSideEffects(...) in Minecraft 26.1.2.
     */
    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState blockState,
            ServerLevel level,
            BlockPos blockPos,
            boolean movedByPiston
    ) {
        Containers.updateNeighboursAfterDestroy(
                blockState,
                level,
                blockPos
        );

        super.affectNeighborsAfterRemoval(
                blockState,
                level,
                blockPos,
                movedByPiston
        );
    }

    /*
     * Connects each placed chest block entity to its
     * once-per-tick animation method.
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState blockState,
            BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(
                blockEntityType,
                DungeonCraft.BASIC_TREASURE_CHEST_BLOCK_ENTITY,
                BasicTreasureChestBlockEntity::tick
        );
    }

    /*
     * BaseEntityBlock requires the block to expose its codec.
     */
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /*
     * Minecraft calls this when one Basic Treasure Chest
     * is placed or loaded.
     *
     * The block exists once in the registry, but every placed
     * chest receives its own block-entity object.
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos blockPos,
            BlockState blockState
    ) {
        return new BasicTreasureChestBlockEntity(
                blockPos,
                blockState
        );
    }

    /*
     * INVISIBLE means Minecraft should not render the block
     * using a normal baked block model.
     *
     * Our future block-entity renderer will draw the body
     * and animated lid instead.
     */
    @Override
    protected RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(
            BlockState blockState,
            Rotation rotation
    ) {
        return blockState.setValue(
                FACING,
                rotation.rotate(
                        blockState.getValue(
                                FACING
                        )
                )
        );
    }

    @Override
    protected BlockState mirror(
            BlockState blockState,
            Mirror mirror
    ) {
        return blockState.rotate(
                mirror.getRotation(
                        blockState.getValue(
                                FACING
                        )
                )
        );
    }
}