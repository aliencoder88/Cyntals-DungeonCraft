package com.dungeoncraft.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/*
 * Holds the information needed to render one Basic Treasure Chest
 * during a frame.
 */
public class BasicTreasureChestRenderState
        extends BlockEntityRenderState {

    private Direction facing =
            Direction.NORTH;

    public Direction getFacing() {
        return this.facing;
    }

    public void setFacing(
            Direction facing
    ) {
        this.facing = facing;
    }

    /*
     * Prepared block-model data for the separate chest lid.
     */
    public final BlockModelRenderState lidModel =
            new BlockModelRenderState();

    /*
     * 0.0F means closed.
     * 1.0F will eventually mean fully open.
     */
    private float lidProgress = 0.0F;

    public float getLidProgress() {
        return lidProgress;
    }

    public void setLidProgress(float lidProgress) {
        this.lidProgress = lidProgress;
    }
}