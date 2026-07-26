package com.dungeoncraft.config;

/**
 * Determines which physical lever position is treated as electrically ON.
 *
 * ACTIVATED_IS_ON preserves normal lever behavior. RESTING_IS_ON inverts the
 * electrical logic without automatically moving the visible lever.
 */
public enum LeverPowerMode {
    ACTIVATED_IS_ON("activated_is_on"),
    RESTING_IS_ON("resting_is_on");

    private final String serializedName;

    LeverPowerMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public boolean isElectricallyOn(boolean physicallyActivated) {
        return this == ACTIVATED_IS_ON
                ? physicallyActivated
                : !physicallyActivated;
    }

    public static LeverPowerMode fromSerializedName(String name) {
        for (LeverPowerMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }

        return ACTIVATED_IS_ON;
    }
}
