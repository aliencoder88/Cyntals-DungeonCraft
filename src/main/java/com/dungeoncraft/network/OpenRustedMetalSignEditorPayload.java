package com.dungeoncraft.network;

import com.dungeoncraft.DungeonCraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/*
 * OpenRustedMetalSignEditorPayload
 *
 * Server -> client packet.
 *
 * The server sends this when the player uses the Engraving Tool
 * on a Rusted Metal Sign.
 *
 * The client receives it and opens the temporary sign editor screen.
 */
public record OpenRustedMetalSignEditorPayload(
        BlockPos pos,
        String line1,
        String line2,
        String line3,
        String line4
) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    DungeonCraft.MOD_ID,
                    "open_rusted_metal_sign_editor"
            );

    public static final Type<OpenRustedMetalSignEditorPayload> TYPE =
            new Type<>(
                    ID
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRustedMetalSignEditorPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenRustedMetalSignEditorPayload::pos,
                    ByteBufCodecs.STRING_UTF8,
                    OpenRustedMetalSignEditorPayload::line1,
                    ByteBufCodecs.STRING_UTF8,
                    OpenRustedMetalSignEditorPayload::line2,
                    ByteBufCodecs.STRING_UTF8,
                    OpenRustedMetalSignEditorPayload::line3,
                    ByteBufCodecs.STRING_UTF8,
                    OpenRustedMetalSignEditorPayload::line4,
                    OpenRustedMetalSignEditorPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}