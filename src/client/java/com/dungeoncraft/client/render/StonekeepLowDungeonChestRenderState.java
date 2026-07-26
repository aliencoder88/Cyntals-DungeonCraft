package com.dungeoncraft.client.render;

import com.dungeoncraft.block.StonekeepChestFacing;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Data copied from the live chest for one rendered frame. */
public class StonekeepLowDungeonChestRenderState extends BlockEntityRenderState {
    public final BlockModelRenderState bodyModel = new BlockModelRenderState();
    public final BlockModelRenderState lidModel = new BlockModelRenderState();

    private StonekeepChestFacing facing = StonekeepChestFacing.NORTH;
    private float lidProgress;

    public StonekeepChestFacing getFacing() {
        return this.facing;
    }

    public void setFacing(StonekeepChestFacing facing) {
        this.facing = facing;
    }

    public float getLidProgress() {
        return this.lidProgress;
    }

    public void setLidProgress(float lidProgress) {
        this.lidProgress = lidProgress;
    }
}
