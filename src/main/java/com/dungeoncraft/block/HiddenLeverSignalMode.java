package com.dungeoncraft.block;

/**
 * Selects which electrical system receives the concealed lever's output.
 *
 * The first implementation uses REGULAR_REDSTONE. VERIFIED_SIGNAL is kept
 * here now so the later verified network can be added without replacing the
 * lever's saved configuration format.
 */
public enum HiddenLeverSignalMode {
    REGULAR_REDSTONE("regular_redstone"),
    VERIFIED_SIGNAL("verified_signal");

    private final String serializedName;

    HiddenLeverSignalMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static HiddenLeverSignalMode fromSerializedName(String name) {
        for (HiddenLeverSignalMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }

        return REGULAR_REDSTONE;
    }
}
