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
 * Client -> server packet that saves Coding Tool changes.
 */
public record SaveCodingToolConfigPayload(
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
                    "save_coding_tool_config"
            );

    public static final Type<SaveCodingToolConfigPayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveCodingToolConfigPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SaveCodingToolConfigPayload::pos,
                    ByteBufCodecs.stringUtf8(32),
                    SaveCodingToolConfigPayload::deviceType,
                    ByteBufCodecs.stringUtf8(32),
                    SaveCodingToolConfigPayload::signalMode,
                    ByteBufCodecs.stringUtf8(32),
                    SaveCodingToolConfigPayload::leverPowerMode,
                    ByteBufCodecs.VAR_INT,
                    SaveCodingToolConfigPayload::outputFaceMask,
                    CodingToolAdvancedConfigCodec.CODEC,
                    SaveCodingToolConfigPayload::advancedConfig,
                    SaveCodingToolConfigPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
