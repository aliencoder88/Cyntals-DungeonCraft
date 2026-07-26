package com.dungeoncraft.network;

import com.dungeoncraft.config.CodingToolConfigurable;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared helper for opening the Coding Tool screen from supported devices.
 */
public final class CodingToolNetworking {
    private CodingToolNetworking() {
    }

    public static void openScreen(
            ServerPlayer player,
            BlockPos pos,
            CodingToolConfigurable configurable
    ) {
        ServerPlayNetworking.send(
                player,
                new OpenCodingToolScreenPayload(
                        pos,
                        configurable.getCodingToolDeviceType()
                                .getSerializedName(),
                        configurable.getSignalMode().getSerializedName(),
                        configurable.getLeverPowerMode().getSerializedName(),
                        configurable.getOutputFaceMask(),
                        configurable.getAdvancedConfig()
                )
        );
    }
}
