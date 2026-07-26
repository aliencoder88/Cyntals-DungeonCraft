package com.dungeoncraft.item;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.EmbeddedCopperWireBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/*
 * DungeonCraft Embedding Tool
 *
 * First working prototype:
 *
 * - represented by a rough wide-chisel model;
 * - right-clicks exposed normal Copper Wire;
 * - replaces it with Embedded Copper Wire;
 * - preserves every Copper Wire connection and electrical property;
 * - leaves already embedded and covered wire unchanged.
 *
 * Durability, recipe, sounds, particles, and final artwork can be added
 * after the core interaction has been tested.
 */
public class EmbeddingToolItem extends Item {

    public EmbeddingToolItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        Level level =
                context.getLevel();

        BlockPos wirePos =
                context.getClickedPos();

        BlockState wireState =
                level.getBlockState(
                        wirePos
                );

        /*
         * The first version embeds only exposed normal Copper Wire.
         *
         * Embedded Copper Wire and Covered Embedded Copper Wire are already
         * in their embedded forms and therefore return PASS.
         */
        if (!wireState.is(DungeonCraft.COPPER_WIRE)) {
            return InteractionResult.PASS;
        }

        /*
         * Return success immediately on the client so the hand animation
         * plays. The authoritative block replacement occurs only on the
         * logical server.
         */
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState embeddedState =
                EmbeddedCopperWireBlock.copyWireProperties(
                        wireState,
                        DungeonCraft
                                .EMBEDDED_COPPER_WIRE
                                .defaultBlockState()
                );

        boolean replaced =
                level.setBlock(
                        wirePos,
                        embeddedState,
                        Block.UPDATE_ALL
                );

        if (!replaced) {
            return InteractionResult.FAIL;
        }

        /*
         * Let the inherited Copper Wire controller verify visual links and
         * electrical power one tick after the conversion.
         */
        level.scheduleTick(
                wirePos,
                DungeonCraft.EMBEDDED_COPPER_WIRE,
                1
        );

        return InteractionResult.SUCCESS;
    }
}
