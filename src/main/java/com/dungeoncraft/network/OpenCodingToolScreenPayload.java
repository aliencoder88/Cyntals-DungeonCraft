package com.dungeoncraft.network;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.config.CodingToolAdvancedConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client packet containing the selected device's saved settings.
 */
public record OpenCodingToolScreenPayload(
        BlockPos pos,
        String deviceType,
        String signalMode,
        String leverPowerMode,
        int outputFaceMask,
        CodingToolAdvancedConfig advancedConfig
) implements CustomPacketPayload {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    DungeonCraft.MOD_ID,
                    "open_coding_tool_screen"
            );

    public static final Type<OpenCodingToolScreenPayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCodingToolScreenPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenCodingToolScreenPayload::pos,
                    ByteBufCodecs.stringUtf8(32),
                    OpenCodingToolScreenPayload::deviceType,
                    ByteBufCodecs.stringUtf8(32),
                    OpenCodingToolScreenPayload::signalMode,
                    ByteBufCodecs.stringUtf8(32),
                    OpenCodingToolScreenPayload::leverPowerMode,
                    ByteBufCodecs.VAR_INT,
                    OpenCodingToolScreenPayload::outputFaceMask,
                    CodingToolAdvancedConfigCodec.CODEC,
                    OpenCodingToolScreenPayload::advancedConfig,
                    OpenCodingToolScreenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
