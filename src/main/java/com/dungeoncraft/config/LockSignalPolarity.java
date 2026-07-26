package com.dungeoncraft.config;

/**
 * Selects which authorization-input state represents a locked device.
 */
public enum LockSignalPolarity {
    POWER_IS_LOCKED("power_is_locked"),
    NO_POWER_IS_LOCKED("no_power_is_locked");

    private final String serializedName;

    LockSignalPolarity(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public boolean isLocked(boolean signalPresent) {
        return this == POWER_IS_LOCKED
                ? signalPresent
                : !signalPresent;
    }

    public LockSignalPolarity next() {
        return this == POWER_IS_LOCKED
                ? NO_POWER_IS_LOCKED
                : POWER_IS_LOCKED;
    }

    public static LockSignalPolarity fromSerializedName(String name) {
        for (LockSignalPolarity polarity : values()) {
            if (polarity.serializedName.equals(name)) {
                return polarity;
            }
        }

        return NO_POWER_IS_LOCKED;
    }
}
