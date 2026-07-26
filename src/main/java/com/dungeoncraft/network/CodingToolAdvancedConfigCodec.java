package com.dungeoncraft.network;

import com.dungeoncraft.config.CodingToolAdvancedConfig;
import com.dungeoncraft.config.DeviceSignalMode;
import com.dungeoncraft.config.HiddenPanelWiringMode;
import com.dungeoncraft.config.LockSignalPolarity;
import com.dungeoncraft.config.VerifiedSignalKey;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Packet codec for the extended Coding Tool settings.
 */
public final class CodingToolAdvancedConfigCodec {
    public static final StreamCodec<RegistryFriendlyByteBuf, CodingToolAdvancedConfig> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(
                            VerifiedSignalKey.MAX_LENGTH
                    ),
                    CodingToolAdvancedConfig::verifiedOutputKey,
                    ByteBufCodecs.stringUtf8(48),
                    config -> config.panelWiringMode().getSerializedName(),
                    ByteBufCodecs.stringUtf8(32),
                    config -> config.lockSignalPolarity().getSerializedName(),
                    ByteBufCodecs.stringUtf8(32),
                    config -> config.panelSignalMode().getSerializedName(),
                    ByteBufCodecs.VAR_INT,
                    CodingToolAdvancedConfig::panelInputFaceMask,
                    ByteBufCodecs.stringUtf8(
                            VerifiedSignalKey.MAX_LENGTH
                    ),
                    CodingToolAdvancedConfig::panelRequiredKey,
                    PowerDiverterConfigCodec.CODEC,
                    CodingToolAdvancedConfig::powerDiverterConfig,
                    (
                            verifiedOutputKey,
                            panelWiringMode,
                            lockSignalPolarity,
                            panelSignalMode,
                            panelInputFaceMask,
                            panelRequiredKey,
                            powerDiverterConfig
                    ) -> new CodingToolAdvancedConfig(
                            verifiedOutputKey,
                            HiddenPanelWiringMode.fromSerializedName(
                                    panelWiringMode
                            ),
                            LockSignalPolarity.fromSerializedName(
                                    lockSignalPolarity
                            ),
                            DeviceSignalMode.fromSerializedName(
                                    panelSignalMode
                            ),
                            panelInputFaceMask,
                            panelRequiredKey,
                            powerDiverterConfig
                    )
            );

    private CodingToolAdvancedConfigCodec() {
    }
}
