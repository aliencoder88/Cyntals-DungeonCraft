package com.dungeoncraft.client.render.blockentity;

import com.dungeoncraft.block.RustedMetalSignBlock;
import com.dungeoncraft.block.entity.RustedMetalSignBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/*
 * RustedMetalSignBlockEntityRenderer
 *
 * This renderer draws the stored text lines on the front of the
 * Rusted Metal Sign.
 *
 * This is still a temporary renderer. Later, we can adjust:
 * - exact text color;
 * - text scale;
 * - text position;
 * - in-world language/rune rendering;
 * - engraved/recessed effect.
 */
public class RustedMetalSignBlockEntityRenderer
        implements BlockEntityRenderer<
        RustedMetalSignBlockEntity,
        RustedMetalSignBlockEntityRenderState
        > {

    private final Font font;

    public RustedMetalSignBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.font = context.font();
    }

    @Override
    public RustedMetalSignBlockEntityRenderState createRenderState() {
        return new RustedMetalSignBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(
            RustedMetalSignBlockEntity blockEntity,
            RustedMetalSignBlockEntityRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity,
                state,
                tickProgress,
                cameraPos,
                crumblingOverlay
        );

        /*
         * Copy the saved sign text from the block entity into the render state.
         */
        state.setLine1(blockEntity.getLine1());
        state.setLine2(blockEntity.getLine2());
        state.setLine3(blockEntity.getLine3());
        state.setLine4(blockEntity.getLine4());

        /*
         * Copy the facing direction so the text can rotate with the sign.
         */
        BlockState blockState =
                blockEntity.getBlockState();

        state.setFacing(
                blockState.getValue(
                        RustedMetalSignBlock.FACING
                )
        );
    }

    @Override
    public void submit(
            RustedMetalSignBlockEntityRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState
    ) {
        matrices.pushPose();

        /*
         * Move to the middle of the block space.
         */
        matrices.translate(
                0.5D,
                0.5D,
                0.5D
        );

        /*
         * Rotate the text so it faces the same direction as the sign face.
         *
         * If the text appears backward during testing, we will adjust these
         * four numbers instead of changing the block model.
         */
        switch (state.getFacing()) {
            case NORTH -> matrices.mulPose(
                    Axis.YP.rotationDegrees(180.0F)
            );
            case SOUTH -> matrices.mulPose(
                    Axis.YP.rotationDegrees(0.0F)
            );
            case EAST -> matrices.mulPose(
                    Axis.YP.rotationDegrees(90.0F)
            );
            case WEST -> matrices.mulPose(
                    Axis.YP.rotationDegrees(270.0F)
            );
            default -> matrices.mulPose(
                    Axis.YP.rotationDegrees(180.0F)
            );
        }

        /*
         * Move the text onto the face of the sign.
         *
         * The model's plate/frame face is close to the wall side of the block,
         * so this places the text just in front of that face.
         */
        matrices.translate(
                0.0D,
                0.0D,
                -0.471D
        );

        /*
         * Shrink the Minecraft font down so four lines fit on the plaque.
         *
         * The negative Y scale makes the text draw upright on the vertical face.
         */
        matrices.scale(
                0.008F,
                -0.008F,
                0.008F
        );

        /*
         * Depth-tested readable metal-sign text color.
         *
         * Later this can become darker, recessed, rune-like, or race-language text.
         */
        int textColor =
                0xFF000000;

        drawCenteredLine(
                matrices,
                queue,
                state.getLine1(),
                -21.0F,
                textColor,
                state.lightCoords
        );

        drawCenteredLine(
                matrices,
                queue,
                state.getLine2(),
                -10.0F,
                textColor,
                state.lightCoords
        );

        drawCenteredLine(
                matrices,
                queue,
                state.getLine3(),
                1.0F,
                textColor,
                state.lightCoords
        );

        drawCenteredLine(
                matrices,
                queue,
                state.getLine4(),
                12.0F,
                textColor,
                state.lightCoords
        );

        matrices.popPose();
    }

    private void drawCenteredLine(
            PoseStack matrices,
            SubmitNodeCollector queue,
            String text,
            float y,
            int color,
            int lightCoords
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }

        float width =
                this.font.width(
                        text
                );

        queue.submitText(
                matrices,
                -width / 2.0F,
                y,
                Component.literal(
                        text
                ).getVisualOrderText(),
                false,
                Font.DisplayMode.NORMAL,
                lightCoords,
                color,
                0,
                0
        );
    }
}