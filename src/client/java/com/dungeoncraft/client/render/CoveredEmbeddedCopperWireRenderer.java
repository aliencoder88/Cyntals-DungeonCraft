package com.dungeoncraft.client.render;

import com.dungeoncraft.block.entity.CoveredEmbeddedCopperWireBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/*
 * Renders the BlockState stored by the covered Embedded Copper Wire.
 *
 * Minecraft 26.1 no longer provides a direct block submission helper.
 * The stored BlockState is first resolved into a BlockModelRenderState
 * during extractRenderState(), then that prepared state is submitted
 * during submit().
 */
public class CoveredEmbeddedCopperWireRenderer
        implements BlockEntityRenderer<
                CoveredEmbeddedCopperWireBlockEntity,
                CoveredEmbeddedCopperWireRenderState
        > {

    /*
     * General display context for a block model rendered by a block-entity
     * renderer.
     */
    private static final BlockDisplayContext
    BLOCK_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;

    public CoveredEmbeddedCopperWireRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.blockModelResolver =
                context.blockModelResolver();
    }

    @Override
    public CoveredEmbeddedCopperWireRenderState
    createRenderState() {
        return new CoveredEmbeddedCopperWireRenderState();
    }

    @Override
    public void extractRenderState(
            CoveredEmbeddedCopperWireBlockEntity blockEntity,
            CoveredEmbeddedCopperWireRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            @Nullable
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity,
                state,
                tickProgress,
                cameraPos,
                crumblingOverlay
        );

        /*
         * Resolve the stored covering BlockState into the model data that
         * Minecraft 26.1's feature renderer expects.
         */
        /*
         * Keep both forms:
         *
         * - coverModel renders the ordinary stored block;
         * - coverState identifies the baked model used by the crack
         *   animation.
         */
        state.setCoverState(
                blockEntity.getCoverState()
        );

        this.blockModelResolver.update(
                state.coverModel,
                state.getCoverState(),
                BLOCK_DISPLAY_CONTEXT
        );

        /*
         * A normal full block is rendered in the chunk mesh and receives
         * lighting from the neighboring position beside each visible face.
         *
         * This faux cover is rendered as a block entity, so Minecraft's
         * base extraction gives it one packed light value sampled at the
         * wrapper's own position. That usually looks acceptable under
         * broad skylight, but local torch light can make the wrapper look
         * darker than the identical blocks around it.
         *
         * Use the strongest block-light and sky-light values found at
         * the wrapper and its six direct neighbors. This better matches
         * the light available to the visible faces of an ordinary block.
         */
        state.lightCoords =
                getNeighborMatchedLight(
                        blockEntity,
                        state.lightCoords
                );
    }

    private static int getNeighborMatchedLight(
            CoveredEmbeddedCopperWireBlockEntity blockEntity,
            int fallbackLight
    ) {
        var level = blockEntity.getLevel();

        if (level == null) {
            return fallbackLight;
        }

        /*
         * Packed Minecraft light uses:
         *
         * - block light in bits beginning at bit 4;
         * - sky light in bits beginning at bit 20.
         *
         * Read the fallback components directly so this renderer does not
         * depend on a client light helper class whose mapped name changed
         * in Minecraft 26.1.2.
         */
        int strongestBlockLight =
                (fallbackLight >> 4) & 0xF;

        int strongestSkyLight =
                (fallbackLight >> 20) & 0xF;

        var blockPos =
                blockEntity.getBlockPos();

        /*
         * Include the wrapper's own position.
         */
        strongestBlockLight =
                Math.max(
                        strongestBlockLight,
                        level.getBrightness(
                                LightLayer.BLOCK,
                                blockPos
                        )
                );

        strongestSkyLight =
                Math.max(
                        strongestSkyLight,
                        level.getBrightness(
                                LightLayer.SKY,
                                blockPos
                        )
                );

        /*
         * Include all six face-adjacent positions.
         */
        for (Direction direction : Direction.values()) {
            var samplePos =
                    blockPos.relative(
                            direction
                    );

            strongestBlockLight =
                    Math.max(
                            strongestBlockLight,
                            level.getBrightness(
                                    LightLayer.BLOCK,
                                    samplePos
                            )
                    );

            strongestSkyLight =
                    Math.max(
                            strongestSkyLight,
                            level.getBrightness(
                                    LightLayer.SKY,
                                    samplePos
                            )
                    );
        }

        /*
         * Repack the two 0-15 light components into the integer expected
         * by BlockModelRenderState.submit(...).
         */
        return (
                (strongestBlockLight & 0xF) << 4
        ) | (
                (strongestSkyLight & 0xF) << 20
        );
    }

    @Override
    public void submit(
            CoveredEmbeddedCopperWireRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState
    ) {
        /*
         * BlockModelRenderState owns the correct 26.1 submission path.
         */
        state.coverModel.submit(
                matrices,
                queue,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );

        /*
         * BlockEntityRenderer.super.extractRenderState(...) copies the
         * current mining stage into state.breakProgress.
         *
         * Submit the crack texture against the STORED cover's baked model,
         * not the invisible internal wrapper model.
         */
        if (state.breakProgress != null) {
            var breakingModel =
                    Minecraft.getInstance()
                            .getModelManager()
                            .getBlockStateModelSet()
                            .get(
                                    state.getCoverState()
                            );

            queue.order(0)
                    .submitBreakingBlockModel(
                            matrices,
                            breakingModel,
                            state.getCoverState()
                                    .getSeed(
                                            state.blockPos
                                    ),
                            state.breakProgress.progress()
                    );
        }
    }
}
