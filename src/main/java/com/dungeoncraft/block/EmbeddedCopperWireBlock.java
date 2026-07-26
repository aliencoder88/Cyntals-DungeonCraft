package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.CoveredEmbeddedCopperWireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/*
 * Creative-only exposed Embedded Copper Wire.
 *
 * It inherits the complete Copper Wire electrical network and interaction
 * box. Its resource models use Rick's exact 0.01 / 0.015 embedded profile.
 *
 * Phase 3.1 adds the faux "place a block over the wire" interaction.
 */
public class EmbeddedCopperWireBlock extends CopperWireBlock {

    public EmbeddedCopperWireBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
    }

    /*
     * Right-clicking exposed Embedded Copper Wire with a supported full
     * block replaces the exposed wire with a hidden-wire wrapper.
     *
     * The wrapper stores the selected block state in a block entity while
     * retaining all Copper Wire network properties in its own BlockState.
     */
    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState wireState,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }

        Block coverBlock =
                blockItem.getBlock();

        /*
         * The first prototype accepts only uncomplicated full blocks.
         *
         * Block-entity blocks such as chests and furnaces need additional
         * inventory/data transfer behavior and are intentionally rejected.
         */
        if (
                coverBlock instanceof EntityBlock
                || coverBlock instanceof CopperWireBlock
        ) {
            return InteractionResult.PASS;
        }

        BlockPlaceContext placementContext =
                new BlockPlaceContext(
                        player,
                        hand,
                        stack,
                        hitResult
                );

        BlockState coverState =
                coverBlock.getStateForPlacement(
                        placementContext
                );

        if (coverState == null) {
            coverState =
                    coverBlock.defaultBlockState();
        }

        /*
         * Only full collision cubes are supported during the first stage.
         * This excludes stairs, slabs, doors, torches, plants, and similar
         * shapes without maintaining a hard-coded block list.
         */
        if (
                coverState.isAir()
                || coverState.hasBlockEntity()
                || !Block.isShapeFullBlock(
                        coverState.getCollisionShape(
                                level,
                                pos
                        )
                )
        ) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState coveredWireState =
                copyWireProperties(
                        wireState,
                        DungeonCraft
                                .COVERED_EMBEDDED_COPPER_WIRE
                                .defaultBlockState()
                );

        level.setBlock(
                pos,
                coveredWireState,
                Block.UPDATE_ALL
        );

        if (
                level.getBlockEntity(pos)
                        instanceof CoveredEmbeddedCopperWireBlockEntity
                        coveredWireEntity
        ) {
            coveredWireEntity.setCoverState(
                    coverState
            );
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        level.scheduleTick(
                pos,
                DungeonCraft.COVERED_EMBEDDED_COPPER_WIRE,
                1
        );

        return InteractionResult.SUCCESS;
    }

    /*
     * Copies every state value used by the Copper Wire controller.
     *
     * This is used when moving between:
     *
     * exposed Embedded Copper Wire
     *          <->
     * covered Embedded Copper Wire
     *
     * Keeping these values avoids a visible or electrical reset during the
     * transition. The normal scheduled network update still verifies them.
     */
    public static BlockState copyWireProperties(
            BlockState source,
            BlockState target
    ) {
        return target
                .setValue(
                        LINK_NORTH,
                        source.getValue(LINK_NORTH)
                )
                .setValue(
                        LINK_EAST,
                        source.getValue(LINK_EAST)
                )
                .setValue(
                        LINK_SOUTH,
                        source.getValue(LINK_SOUTH)
                )
                .setValue(
                        LINK_WEST,
                        source.getValue(LINK_WEST)
                )
                .setValue(
                        UP_MASK,
                        source.getValue(UP_MASK)
                )
                .setValue(
                        POWER,
                        source.getValue(POWER)
                )
                .setValue(
                        VISUAL_REFRESH_REQUIRED,
                        source.getValue(
                                VISUAL_REFRESH_REQUIRED
                        )
                );
    }
}
