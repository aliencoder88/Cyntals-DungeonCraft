package com.dungeoncraft.block.entity;

import com.dungeoncraft.DungeonCraft;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/*
 * Stores the solid block that visually covers an Embedded Copper Wire.
 *
 * The Copper Wire route and power remain in the wrapper BlockState. Only
 * the faux covering block needs additional per-position storage.
 */
public class CoveredEmbeddedCopperWireBlockEntity
        extends BlockEntity
        implements RenderDataBlockEntity {

    private static final String COVER_STATE_KEY =
            "cover_state";

    private BlockState coverState =
            Blocks.STONE.defaultBlockState();

    public CoveredEmbeddedCopperWireBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                DungeonCraft
                        .COVERED_EMBEDDED_COPPER_WIRE_BLOCK_ENTITY,
                pos,
                state
        );
    }

    public BlockState getCoverState() {
        return this.coverState;
    }

    public void setCoverState(
            BlockState coverState
    ) {
        this.coverState =
                coverState;

        this.setChanged();
    }


    /*
     * Chunk meshing can run on worker threads, so the dynamic block model
     * must not read the mutable BlockEntity instance directly.
     *
     * BlockState is immutable and is therefore safe to expose as Fabric
     * render data.
     */
    @Override
    public Object getRenderData() {
        return this.coverState;
    }

    @Override
    protected void saveAdditional(
            ValueOutput output
    ) {
        output.store(
                COVER_STATE_KEY,
                BlockState.CODEC,
                this.coverState
        );

        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(
            ValueInput input
    ) {
        super.loadAdditional(input);

        /*
         * Keep the previously rendered value so the client can tell whether
         * the block-entity update packet changed the faux cover.
         *
         * When the wrapper block is first placed, the client creates its block
         * entity with the temporary STONE fallback. The server then sends the
         * real stored cover state in a block-entity data packet. Minecraft
         * loads that packet into this block entity, but loading block-entity
         * data alone does not automatically rebuild the surrounding chunk
         * model. Without the explicit client-side model refresh below, the
         * wrapper can stay visibly stuck on the STONE fallback until another
         * unrelated chunk rebuild occurs.
         */
        BlockState previousCoverState =
                this.coverState;

        this.coverState =
                input.read(
                        COVER_STATE_KEY,
                        BlockState.CODEC
                ).orElse(
                        Blocks.STONE.defaultBlockState()
                );

        /*
         * The dynamic cover model reads immutable render data while the chunk
         * mesh is rebuilt. If a client packet changed that render data, mark
         * this position dirty so the chunk is rebuilt immediately with the
         * newly received cover BlockState.
         *
         * ClientLevel.sendBlockUpdated(...) is local rendering work only; it
         * does not send a packet back to the server.
         */
        if (
                this.level != null
                        && this.level.isClientSide()
                        && !previousCoverState.equals(
                                this.coverState
                        )
        ) {
            BlockState wrapperState =
                    this.getBlockState();

            this.level.sendBlockUpdated(
                    this.worldPosition,
                    wrapperState,
                    wrapperState,
                    Block.UPDATE_ALL
            );
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registryLookup
    ) {
        return this.saveWithoutMetadata(
                registryLookup
        );
    }

    @Override
    public Packet<ClientGamePacketListener>
    getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level == null) {
            return;
        }

        BlockState state =
                this.getBlockState();

        this.level.sendBlockUpdated(
                this.worldPosition,
                state,
                state,
                Block.UPDATE_ALL
        );
    }
}
