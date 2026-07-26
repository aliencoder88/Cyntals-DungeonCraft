package com.dungeoncraft.block;

// Minecraft's base block class.
import net.minecraft.world.level.block.Block;

// Gives access to the world/block reader used by shape methods.
import net.minecraft.world.level.BlockGetter;

// Block position class used by shape methods.
import net.minecraft.core.BlockPos;

// BlockState tells Minecraft which state of the block is being checked.
import net.minecraft.world.level.block.state.BlockState;

// CollisionContext tells Minecraft what is colliding/checking the shape.
import net.minecraft.world.phys.shapes.CollisionContext;

// VoxelShape is Minecraft's shape/hitbox/collision shape type.
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class ColumnBlock extends Block {

    // This is the column's physical shape.
    // Minecraft block model coordinates go from 0 to 16.
    // This shape starts 3 pixels in from each side and ends 3 pixels before the far side.
    // So the player collides with the visible column area instead of the full block space.
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 16, 12),  // center core
            Block.box(5, 0, 3, 11, 16, 4),   // north step
            Block.box(5, 0, 12, 11, 16, 13), // south step
            Block.box(3, 0, 5, 4, 16, 11),   // west step
            Block.box(12, 0, 5, 13, 16, 11)  // east step
    );

    public ColumnBlock(Properties properties) {
        super(properties);

        // Column design notes:
        // First version is a simple decorative column block.
        // Current collision should match the visible column model, not the full block space.

        // Future version:
        // Add base/top visual states.
        // Add wall-column or half-column variants.
        // Add placement logic for base/top depending on clicked face.
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