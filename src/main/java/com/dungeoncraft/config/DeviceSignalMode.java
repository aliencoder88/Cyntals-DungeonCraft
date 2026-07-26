package com.dungeoncraft.config;

/**
 * Selects the electrical system used by a configurable device.
 *
 * VERIFIED_SIGNAL is saved by the Coding Tool now, but the verified network
 * will be implemented after the Power Diverter and dedicated power source.
 */
public enum DeviceSignalMode {
    REGULAR_REDSTONE("regular_redstone"),
    VERIFIED_SIGNAL("verified_signal");

    private final String serializedName;

    DeviceSignalMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static DeviceSignalMode fromSerializedName(String name) {
        for (DeviceSignalMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }

        return REGULAR_REDSTONE;
    }
}
