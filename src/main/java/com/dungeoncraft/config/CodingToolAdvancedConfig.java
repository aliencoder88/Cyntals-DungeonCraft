package com.dungeoncraft.config;

/**
 * Extended Coding Tool settings shared by current and future configurable
 * devices.
 *
 * Lever devices use the verified-output and concealed-panel fields. The Power
 * Diverter uses powerDiverterConfig. Fields that do not belong to the selected
 * device are reset to safe defaults during server-side sanitization.
 */
public record CodingToolAdvancedConfig(
        String verifiedOutputKey,
        HiddenPanelWiringMode panelWiringMode,
        LockSignalPolarity lockSignalPolarity,
        DeviceSignalMode panelSignalMode,
        int panelInputFaceMask,
        String panelRequiredKey,
        PowerDiverterConfig powerDiverterConfig
) {
    public CodingToolAdvancedConfig {
        verifiedOutputKey = VerifiedSignalKey.sanitize(verifiedOutputKey);
        panelWiringMode = panelWiringMode == null
                ? HiddenPanelWiringMode.UNLOCKED
                : panelWiringMode;
        lockSignalPolarity = lockSignalPolarity == null
                ? LockSignalPolarity.NO_POWER_IS_LOCKED
                : lockSignalPolarity;
        panelSignalMode = panelSignalMode == null
                ? DeviceSignalMode.REGULAR_REDSTONE
                : panelSignalMode;
        panelInputFaceMask = HiddenLeverOutputFace.sanitizeMask(
                panelInputFaceMask
        );
        panelRequiredKey = VerifiedSignalKey.sanitize(panelRequiredKey);
        powerDiverterConfig = PowerDiverterConfig.sanitized(
                powerDiverterConfig
        );
    }

    public static CodingToolAdvancedConfig hiddenLeverDefaults() {
        return new CodingToolAdvancedConfig(
                "",
                HiddenPanelWiringMode.UNLOCKED,
                LockSignalPolarity.NO_POWER_IS_LOCKED,
                DeviceSignalMode.REGULAR_REDSTONE,
                0,
                "",
                PowerDiverterConfig.defaults()
        );
    }

    public static CodingToolAdvancedConfig ironLeverDefaults() {
        return new CodingToolAdvancedConfig(
                "",
                HiddenPanelWiringMode.UNLOCKED,
                LockSignalPolarity.NO_POWER_IS_LOCKED,
                DeviceSignalMode.REGULAR_REDSTONE,
                0,
                "",
                PowerDiverterConfig.defaults()
        );
    }

    public static CodingToolAdvancedConfig powerDiverterDefaults() {
        return new CodingToolAdvancedConfig(
                "",
                HiddenPanelWiringMode.UNLOCKED,
                LockSignalPolarity.NO_POWER_IS_LOCKED,
                DeviceSignalMode.REGULAR_REDSTONE,
                0,
                "",
                PowerDiverterConfig.defaults()
        );
    }

    public CodingToolAdvancedConfig sanitizedFor(
            CodingToolDeviceType deviceType
    ) {
        if (deviceType == CodingToolDeviceType.IRON_LEVER) {
            return new CodingToolAdvancedConfig(
                    this.verifiedOutputKey,
                    HiddenPanelWiringMode.UNLOCKED,
                    LockSignalPolarity.NO_POWER_IS_LOCKED,
                    DeviceSignalMode.REGULAR_REDSTONE,
                    0,
                    "",
                    PowerDiverterConfig.defaults()
            );
        }

        if (deviceType == CodingToolDeviceType.POWER_DIVERTER) {
            return new CodingToolAdvancedConfig(
                    "",
                    HiddenPanelWiringMode.UNLOCKED,
                    LockSignalPolarity.NO_POWER_IS_LOCKED,
                    DeviceSignalMode.REGULAR_REDSTONE,
                    0,
                    "",
                    this.powerDiverterConfig
            );
        }

        int sanitizedInputMask = this.panelInputFaceMask;

        /*
         * A wired lock with no selected input could permanently close the
         * concealed panel and prevent the Coding Tool from reaching the lever.
         * Back is therefore restored as the safe input when a locked mode is
         * selected without any input face. Unlocked mode may keep no inputs.
         */
        if (this.panelWiringMode != HiddenPanelWiringMode.UNLOCKED
                && sanitizedInputMask == 0) {
            sanitizedInputMask = HiddenLeverOutputFace.defaultMask();
        }

        return new CodingToolAdvancedConfig(
                this.verifiedOutputKey,
                this.panelWiringMode,
                this.lockSignalPolarity,
                this.panelSignalMode,
                sanitizedInputMask,
                this.panelRequiredKey,
                PowerDiverterConfig.defaults()
        );
    }
}
