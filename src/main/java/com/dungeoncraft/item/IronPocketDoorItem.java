package com.dungeoncraft.item;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.IronPocketDoorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import net.minecraft.world.phys.AABB;

public class IronPocketDoorItem extends BlockItem {

    public IronPocketDoorItem(Properties properties) {
        super(DungeonCraft.IRON_POCKET_DOOR, properties);
    }


    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();

// This is the block position where Minecraft thinks the item should place.
// It changes based on what face you clicked:
// - top of floor block  -> air above the floor
// - side of wall block  -> air beside the wall
// - bottom of ceiling   -> air below the ceiling
        BlockPos bottomPos = context.getClickedPos();

// The top half of the door is always one block above the bottom half.
        BlockPos topPos = bottomPos.above();

// The door must have a solid/supporting block directly below the bottom half.
// This prevents floating doors, but still allows wall-click placement if there is floor below.
        BlockPos supportPos = bottomPos.below();

        if (!level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP)) {
            return InteractionResult.FAIL;
        }

        // The bottom door space must be empty/replaceable.
        if (!level.getBlockState(bottomPos).canBeReplaced(context)) {
            return InteractionResult.FAIL;
        }

        // The top door space must be empty/replaceable.
        if (!level.getBlockState(topPos).canBeReplaced(context)) {
            return InteractionResult.FAIL;
        }

        // Do not place the door inside the player's body.
        if (context.getPlayer() != null) {
            AABB bottomBox = new AABB(bottomPos);
            AABB topBox = new AABB(topPos);

            if (context.getPlayer().getBoundingBox().intersects(bottomBox)
                    || context.getPlayer().getBoundingBox().intersects(topBox)) {
                return InteractionResult.FAIL;
            }
        }

        Direction playerFacing = context.getHorizontalDirection();
        Direction facing = playerFacing.getOpposite();

        // These are left and right from the player's view while placing the door.
        Direction leftDirection = playerFacing.getCounterClockWise();
        Direction rightDirection = playerFacing.getClockWise();

        // This decides the door's slide direction.
        // If you clicked a side wall, the clicked wall wins.
        // If you clicked the ground, it checks for a left/right wall.
        // If neither side is clear, it defaults to right.
        boolean slidesLeft = chooseSlidesLeft(
                context,
                level,
                bottomPos,
                leftDirection,
                rightDirection
        );

        BlockState bottomState = DungeonCraft.IRON_POCKET_DOOR.defaultBlockState()
                .setValue(IronPocketDoorBlock.FACING, facing)
                .setValue(IronPocketDoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(IronPocketDoorBlock.OPEN, false)
                .setValue(IronPocketDoorBlock.POWERED, false)
                .setValue(IronPocketDoorBlock.SLIDES_LEFT, slidesLeft)
                .setValue(IronPocketDoorBlock.SLIDE_STEP, 0);

        BlockState topState = DungeonCraft.IRON_POCKET_DOOR.defaultBlockState()
                .setValue(IronPocketDoorBlock.FACING, facing)
                .setValue(IronPocketDoorBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(IronPocketDoorBlock.OPEN, false)
                .setValue(IronPocketDoorBlock.POWERED, false)
                .setValue(IronPocketDoorBlock.SLIDES_LEFT, slidesLeft)
                .setValue(IronPocketDoorBlock.SLIDE_STEP, 0);

        level.setBlock(bottomPos, bottomState, 3);
        level.setBlock(topPos, topState, 3);

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }


    private boolean chooseSlidesLeft(
            BlockPlaceContext context,
            Level level,
            BlockPos doorBottomPos,
            Direction leftDirection,
            Direction rightDirection
    ) {
        /*
         * Part 1:
         * If the player clicked the side face of a wall block,
         * use that clicked wall to force the slide direction.
         *
         * Example:
         * - Click the left wall beside the doorway  -> left slide setting
         * - Click the right wall beside the doorway -> right slide setting
         */
        Direction clickedFace = context.getClickedFace();

        if (clickedFace.getAxis().isHorizontal()) {
            Direction clickedWallDirection = clickedFace.getOpposite();

            if (clickedWallDirection == leftDirection) {
                return true;
            }

            if (clickedWallDirection == rightDirection) {
                return false;
            }
        }

        /*
         * Part 2:
         * If the player clicked the ground/floor instead of a side wall,
         * automatically check which side has a pocket wall.
         */
        boolean wallOnLeft = hasPocketWall(level, doorBottomPos, leftDirection);
        boolean wallOnRight = hasPocketWall(level, doorBottomPos, rightDirection);

        if (wallOnLeft && !wallOnRight) {
            return true;
        }

        if (wallOnRight && !wallOnLeft) {
            return false;
        }

        /*
         * Part 3:
         * If both sides have walls, or neither side has a wall,
         * use the normal/default direction.
         *
         * false = your default/right-slide setting.
         */
        return false;
    }


    private boolean hasPocketWall(Level level, BlockPos doorBottomPos, Direction sideDirection) {
        BlockPos wallBottomPos = doorBottomPos.relative(sideDirection);
        BlockPos wallTopPos = wallBottomPos.above();

        return level.getBlockState(wallBottomPos).isFaceSturdy(level, wallBottomPos, sideDirection.getOpposite())
                && level.getBlockState(wallTopPos).isFaceSturdy(level, wallTopPos, sideDirection.getOpposite());
    }
}