package com.dungeoncraft.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/*
 * Per-frame render information for Covered Embedded Copper Wire.
 *
 * The BlockModelResolver fills coverModel from the BlockState stored in
 * the block entity.
 */
public class CoveredEmbeddedCopperWireRenderState
        extends BlockEntityRenderState {

    public final BlockModelRenderState coverModel =
            new BlockModelRenderState();

    /*
     * The actual BlockState selected as the faux cover.
     *
     * Keeping it in the extracted render state lets submit(...) obtain the
     * matching baked BlockStateModel for the breaking-crack overlay.
     */
    private BlockState coverState =
            Blocks.STONE.defaultBlockState();

    public BlockState getCoverState() {
        return this.coverState;
    }

    public void setCoverState(
            BlockState coverState
    ) {
        this.coverState =
                coverState;
    }
}
