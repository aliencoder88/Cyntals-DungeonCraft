package com.dungeoncraft.config;

import net.minecraft.core.Direction;

/**
 * Configurable output faces for the Hidden Block Lever.
 *
 * LEFT and RIGHT are defined from the viewpoint of a player looking directly
 * at the opening panel. FRONT is intentionally excluded because that face is
 * occupied by the moving panel and exposed mechanism.
 */
public enum HiddenLeverOutputFace {
    TOP(1 << 0, "Top"),
    BOTTOM(1 << 1, "Bottom"),
    LEFT(1 << 2, "Left"),
    RIGHT(1 << 3, "Right"),
    BACK(1 << 4, "Back");

    private final int mask;
    private final String displayName;

    HiddenLeverOutputFace(int mask, String displayName) {
        this.mask = mask;
        this.displayName = displayName;
    }

    public int getMask() {
        return this.mask;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean isEnabled(int outputFaceMask) {
        return (sanitizeMask(outputFaceMask) & this.mask) != 0;
    }

    /**
     * Converts this local face into the physical world direction leaving the
     * block. The supplied facing is the authored front/opening direction.
     */
    public Direction getWorldOutputDirection(Direction facing) {
        return switch (this) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case BACK -> facing.getOpposite();
        };
    }

    public static int defaultMask() {
        return BACK.mask;
    }

    public static int allMask() {
        int mask = 0;

        for (HiddenLeverOutputFace face : values()) {
            mask |= face.mask;
        }

        return mask;
    }

    public static int sanitizeMask(int mask) {
        return mask & allMask();
    }
}
