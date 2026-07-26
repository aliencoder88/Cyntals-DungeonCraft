package com.dungeoncraft.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/** Data copied from the live concealed lever for one rendered frame. */
public class HiddenBlockLeverRenderState extends BlockEntityRenderState {
    public final BlockModelRenderState panelModel = new BlockModelRenderState();
    public final BlockModelRenderState mountModel = new BlockModelRenderState();
    public final BlockModelRenderState barModel = new BlockModelRenderState();

    private Direction facing = Direction.NORTH;
    private boolean renderClosedModel;
    private float panelProgress;
    private float leverProgress;

    public Direction getFacing() {
        return this.facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }

    public boolean shouldRenderClosedModel() {
        return this.renderClosedModel;
    }

    public void setRenderClosedModel(boolean renderClosedModel) {
        this.renderClosedModel = renderClosedModel;
    }

    public float getPanelProgress() {
        return this.panelProgress;
    }

    public void setPanelProgress(float panelProgress) {
        this.panelProgress = panelProgress;
    }

    public float getLeverProgress() {
        return this.leverProgress;
    }

    public void setLeverProgress(float leverProgress) {
        this.leverProgress = leverProgress;
    }
}
