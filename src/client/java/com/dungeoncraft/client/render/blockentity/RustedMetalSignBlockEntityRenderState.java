package com.dungeoncraft.client.render.blockentity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/*
 * RustedMetalSignBlockEntityRenderState
 *
 * This client-side render state holds the information needed to draw
 * the Rusted Metal Sign's text.
 *
 * The block entity stores the real data.
 * The renderer reads that data into this render state before drawing.
 */
public class RustedMetalSignBlockEntityRenderState
        extends BlockEntityRenderState {

    private String line1 = "";
    private String line2 = "";
    private String line3 = "";
    private String line4 = "";

    private Direction facing = Direction.NORTH;

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getLine3() {
        return line3;
    }

    public void setLine3(String line3) {
        this.line3 = line3;
    }

    public String getLine4() {
        return line4;
    }

    public void setLine4(String line4) {
        this.line4 = line4;
    }

    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }
}