package com.dungeoncraft.network;

import com.dungeoncraft.config.PowerDiverterConfig;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Packet codec for the compact Power Router port and route settings. */
public final class PowerDiverterConfigCodec {
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerDiverterConfig> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    PowerDiverterConfig::portModeBits,
                    ByteBufCodecs.VAR_INT,
                    PowerDiverterConfig::routeBits,
                    ByteBufCodecs.VAR_INT,
                    PowerDiverterConfig::feedbackBlockedRouteBits,
                    PowerDiverterConfig::new
            );

    private PowerDiverterConfigCodec() {
    }
}
