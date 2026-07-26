package com.dungeoncraft.config;

/**
 * Shared configuration contract used by the Coding Tool.
 */
public interface CodingToolConfigurable {
    CodingToolDeviceType getCodingToolDeviceType();

    DeviceSignalMode getSignalMode();

    LeverPowerMode getLeverPowerMode();

    /**
     * Device-specific output routing data.
     *
     * The Hidden Block Lever uses a five-face bit mask. Devices that do not
     * currently expose configurable routing return zero and ignore this value.
     */
    int getOutputFaceMask();

    CodingToolAdvancedConfig getAdvancedConfig();

    void applyCodingToolConfiguration(
            DeviceSignalMode signalMode,
            LeverPowerMode leverPowerMode,
            int outputFaceMask,
            CodingToolAdvancedConfig advancedConfig
    );
}
