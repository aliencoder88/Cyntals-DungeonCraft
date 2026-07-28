package com.dungeoncraft.config;

import net.minecraft.core.Direction;

/**
 * The Power Router has four equal horizontal ports.
 *
 * Labels are fixed to world directions so the block does not rotate:
 * 1 = North, 2 = East, 3 = South, and 4 = West. Up and down are not ports.
 */
public enum PowerDiverterPort {
    NORTH(0, Direction.NORTH, "1 (North)"),
    EAST(1, Direction.EAST, "2 (East)"),
    SOUTH(2, Direction.SOUTH, "3 (South)"),
    WEST(3, Direction.WEST, "4 (West)");

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
