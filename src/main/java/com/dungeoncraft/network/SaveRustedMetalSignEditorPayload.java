package com.dungeoncraft.network;

import com.dungeoncraft.DungeonCraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/*
 * SaveRustedMetalSignEditorPayload
 *
 * Client -> server packet.
 *
 * The client sends this when the player presses Done in the
 * Rusted Metal Sign editor screen.
 *
 * The server receives it and updates the actual block entity.
 */
public record SaveRustedMetalSignEditorPayload(
        BlockPos pos,
        String line1,
        String line2,
        String line3,
        String line4
) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    DungeonCraft.MOD_ID,
                    "save_rusted_metal_sign_editor"
            );

    public static final Type<SaveRustedMetalSignEditorPayload> TYPE =
            new Type<>(
                    ID
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveRustedMetalSignEditorPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SaveRustedMetalSignEditorPayload::pos,
                    ByteBufCodecs.STRING_UTF8,
                    SaveRustedMetalSignEditorPayload::line1,
                    ByteBufCodecs.STRING_UTF8,
                    SaveRustedMetalSignEditorPayload::line2,
                    ByteBufCodecs.STRING_UTF8,
                    SaveRustedMetalSignEditorPayload::line3,
                    ByteBufCodecs.STRING_UTF8,
                    SaveRustedMetalSignEditorPayload::line4,
                    SaveRustedMetalSignEditorPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}