package com.dungeoncraft.block;

// Minecraft's base block class.
import net.minecraft.world.level.block.Block;

// Used by the shape methods to read the world/block area.
import net.minecraft.world.level.BlockGetter;

// Represents the position of the block in the world.
import net.minecraft.core.BlockPos;

// Represents the current block state.
import net.minecraft.world.level.block.state.BlockState;

// Used for collision/selection context.
import net.minecraft.world.phys.shapes.CollisionContext;

// Minecraft's shape type for collision and outline boxes.
import net.minecraft.world.phys.shapes.VoxelShape;

// Allows combining multiple box shapes into one stepped shape.
import net.minecraft.world.phys.shapes.Shapes;

public class ColumnBaseBlock extends Block {

    // The base is wider than the normal column.
    // This shape gives the base a stepped/octagonal-ish collision shape instead of a full cube.
    private static final VoxelShape SHAPE = Shapes.or(
            // Lower wider base section: y 0 to 4
            // This matches the extended base model pieces.
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 4.0D, 13.0D),
            Block.box(4.0D, 0.0D, 2.0D, 12.0D, 4.0D, 3.0D),
            Block.box(4.0D, 0.0D, 13.0D, 12.0D, 4.0D, 14.0D),
            Block.box(2.0D, 0.0D, 4.0D, 3.0D, 4.0D, 12.0D),
            Block.box(13.0D, 0.0D, 4.0D, 14.0D, 4.0D, 12.0D),

            // Upper column section: y 4 to 16
            // This matches the normal column model shape.
            Block.box(4.0D, 4.0D, 4.0D, 12.0D, 16.0D, 12.0D),
            Block.box(5.0D, 4.0D, 3.0D, 11.0D, 16.0D, 4.0D),
            Block.box(5.0D, 4.0D, 12.0D, 11.0D, 16.0D, 13.0D),
            Block.box(3.0D, 4.0D, 5.0D, 4.0D, 16.0D, 11.0D),
            Block.box(12.0D, 4.0D, 5.0D, 13.0D, 16.0D, 11.0D)
    );

    public ColumnBaseBlock(Properties properties) {
        super(properties);

        // Column Base design notes:
        // First version is a separate base block.
        // It is wider than the normal column.
        // It does not auto-detect placement yet.
        //
        // Future version:
        // A smarter column block may decide base/top/middle based on placement.
        // If the player places on top of a floor block, it can choose base.
        // If the player places under a ceiling block, it can choose top.
        // Once chosen, the visual state should be saved and not automatically removed
        // if the floor or ceiling block is later broken.
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}