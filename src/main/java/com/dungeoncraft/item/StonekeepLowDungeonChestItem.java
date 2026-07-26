package com.dungeoncraft.item;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.StonekeepChestFacing;
import com.dungeoncraft.block.StonekeepLowDungeonChestBlock;
import com.dungeoncraft.block.StonekeepLowDungeonChestPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Places the master and its direction-dependent footprint atomically. */
public class StonekeepLowDungeonChestItem extends BlockItem {
    public StonekeepLowDungeonChestItem(Properties properties) {
        super(DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos masterPos = context.getClickedPos();
        Player player = context.getPlayer();

        StonekeepChestFacing facing =
                StonekeepChestFacing.fromPlacementYaw(context.getRotation());

        if (!level.getBlockState(masterPos).canBeReplaced(context)) {
            return InteractionResult.FAIL;
        }

        Direction[] helperDirections = facing.helperDirections();

        for (Direction direction : helperDirections) {
            BlockPos helperPos = masterPos.relative(direction);
            if (!level.getBlockState(helperPos).canBeReplaced(context)) {
                return InteractionResult.FAIL;
            }
        }

        if (player != null) {
            if (player.getBoundingBox().intersects(new AABB(masterPos))) {
                return InteractionResult.FAIL;
            }
            for (Direction direction : helperDirections) {
                if (player.getBoundingBox().intersects(new AABB(masterPos.relative(direction)))) {
                    return InteractionResult.FAIL;
                }
            }
        }

        BlockState masterState = DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST
                .defaultBlockState()
                .setValue(StonekeepLowDungeonChestBlock.FACING, facing);

        if (!level.setBlock(masterPos, masterState, 3)) {
            return InteractionResult.FAIL;
        }

        for (Direction direction : helperDirections) {
            BlockPos helperPos = masterPos.relative(direction);
            BlockState helperState = DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_PART
                    .defaultBlockState()
                    .setValue(
                            StonekeepLowDungeonChestPartBlock.TO_MASTER,
                            direction.getOpposite()
                    );
            level.setBlock(helperPos, helperState, 3);
        }

        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
