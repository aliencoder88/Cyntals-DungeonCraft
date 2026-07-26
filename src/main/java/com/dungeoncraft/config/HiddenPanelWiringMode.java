package com.dungeoncraft.config;

/**
 * Controls how the Hidden Block Lever's concealed panel reacts to its
 * configured authorization input.
 */
public enum HiddenPanelWiringMode {
    /** Existing behavior: the panel always opens and closes manually. */
    UNLOCKED("unlocked"),

    /** Locked blocks opening; unlocked restores normal manual operation. */
    LOCKED_UNLOCKED("locked_unlocked"),

    /** Locked forces closed; unlocked forces open. */
    LOCKED_CLOSED_UNLOCKED_OPEN("locked_closed_unlocked_open");

    private final String serializedName;

    HiddenPanelWiringMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public HiddenPanelWiringMode next() {
        return switch (this) {
            case UNLOCKED -> LOCKED_UNLOCKED;
            case LOCKED_UNLOCKED -> LOCKED_CLOSED_UNLOCKED_OPEN;
            case LOCKED_CLOSED_UNLOCKED_OPEN -> UNLOCKED;
        };
    }

    public static HiddenPanelWiringMode fromSerializedName(String name) {
        for (HiddenPanelWiringMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }

        return UNLOCKED;
    }
}
