package com.dungeoncraft.config;

import net.minecraft.core.Direction;

/**
 * The Power Diverter has four equal horizontal ports.
 *
 * Port names are world-cardinal directions so a symmetric block does not need
 * a privileged front face. Up and down are not ports in this first version.
 */
public enum PowerDiverterPort {
    NORTH(0, Direction.NORTH, "North"),
    EAST(1, Direction.EAST, "East"),
    SOUTH(2, Direction.SOUTH, "South"),
    WEST(3, Direction.WEST, "West");

    private final int index;
    private final Direction direction;
    private final String displayName;

    PowerDiverterPort(
            int index,
            Direction direction,
            String displayName
    ) {
        this.index = index;
        this.direction = direction;
        this.displayName = displayName;
    }

    public int getIndex() {
        return this.index;
    }

    public int getMask() {
        return 1 << this.index;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static PowerDiverterPort fromDirection(Direction direction) {
        for (PowerDiverterPort port : values()) {
            if (port.direction == direction) {
                return port;
            }
        }

        return null;
    }

    public static PowerDiverterPort fromIndex(int index) {
        int normalizedIndex = Math.floorMod(index, values().length);

        for (PowerDiverterPort port : values()) {
            if (port.index == normalizedIndex) {
                return port;
            }
        }

        return NORTH;
    }
}
