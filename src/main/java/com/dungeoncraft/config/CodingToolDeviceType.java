package com.dungeoncraft.config;

/**
 * Device categories supported by the shared Coding Tool protocol.
 *
 * Future devices such as the verified power source, doors, trapdoors,
 * portals, and viewing devices can be added without replacing the
 * screen-opening protocol.
 */
public enum CodingToolDeviceType {
    HIDDEN_BLOCK_LEVER("hidden_block_lever"),
    IRON_LEVER("iron_lever"),
    POWER_DIVERTER("power_diverter");

    private final String serializedName;

    CodingToolDeviceType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static CodingToolDeviceType fromSerializedName(String name) {
        for (CodingToolDeviceType type : values()) {
            if (type.serializedName.equals(name)) {
                return type;
            }
        }

        return HIDDEN_BLOCK_LEVER;
    }
}
