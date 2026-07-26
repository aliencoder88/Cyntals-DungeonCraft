package com.dungeoncraft.client.render;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.HiddenBlockLeverBlock;
import com.dungeoncraft.block.entity.HiddenBlockLeverBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Renders the fixed Stonekeep concealed-lever geometry.
 *
 * A fully closed block uses the ordinary Aged Limestone Bricks cube model so
 * every outside face is exact. Once the panel begins moving, the renderer
 * switches to the authored shell, sliding panel, fixed mount, and pull bar.
 */
public class HiddenBlockLeverRenderer implements BlockEntityRenderer<
        HiddenBlockLeverBlockEntity,
        HiddenBlockLeverRenderState
        > {
    private static final BlockDisplayContext MODEL_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    private static final float PANEL_RETRACT_FRACTION = 1.0F / 6.0F;
    private static final float PANEL_RETRACT_DISTANCE = 1.0F / 16.0F;
    private static final float PANEL_RISE_DISTANCE = 8.0F / 16.0F;
    /**
     * Keeps the renderer panel just behind the baked closed face during the
     * handoff from block-entity rendering to chunk rendering. This prevents a
     * one-frame transparent gap without allowing the two faces to z-fight.
     */
    private static final float CLOSED_PANEL_HANDOFF_INSET = 0.001F;

    private static final float LEVER_PIVOT_X = 8.0F / 16.0F;
    private static final float LEVER_PIVOT_Y = 4.0F / 16.0F;
    private static final float LEVER_PIVOT_Z = 11.5F / 16.0F;
    private static final float LEVER_UP_ANGLE = -45.0F;
    private static final float LEVER_PULL_ANGLE = -90.0F;

    private final BlockModelResolver blockModelResolver;

    public HiddenBlockLeverRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public HiddenBlockLeverRenderState createRenderState() {
        return new HiddenBlockLeverRenderState();
    }

    @Override
    public void extractRenderState(
            HiddenBlockLeverBlockEntity blockEntity,
            HiddenBlockLeverRenderState renderState,
            float tickProgress,
            Vec3 cameraPosition,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity,
                renderState,
                tickProgress,
                cameraPosition,
                crumblingOverlay
        );

        BlockState blockState = blockEntity.getBlockState();

        renderState.setFacing(
                blockState.getValue(HiddenBlockLeverBlock.FACING)
        );
        renderState.setRenderClosedModel(
                blockState.getValue(HiddenBlockLeverBlock.RENDER_CLOSED)
        );
        renderState.setPanelProgress(blockEntity.getPanelProgress(tickProgress));
        renderState.setLeverProgress(blockEntity.getLeverProgress(tickProgress));

        this.blockModelResolver.update(
                renderState.panelModel,
                DungeonCraft.HIDDEN_BLOCK_LEVER_PANEL_MODEL.defaultBlockState(),
                MODEL_DISPLAY_CONTEXT
        );
        this.blockModelResolver.update(
                renderState.mountModel,
                DungeonCraft.HIDDEN_BLOCK_LEVER_MOUNT_MODEL.defaultBlockState(),
                MODEL_DISPLAY_CONTEXT
        );
        this.blockModelResolver.update(
                renderState.barModel,
                DungeonCraft.HIDDEN_BLOCK_LEVER_BAR_MODEL.defaultBlockState(),
                MODEL_DISPLAY_CONTEXT
        );
    }

    @Override
    public void submit(
            HiddenBlockLeverRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState
    ) {
        /*
         * The normal chunk renderer handles the exact full cube once the
         * panel has finished closing. The block-state update and chunk rebuild
         * can arrive one or two frames before/after the client animation, so
         * the panel remains as a fallback during that handoff. When the baked
         * cube is active, the fallback panel is moved a tiny distance inward
         * and is hidden behind the cube's front face.
         */
        poseStack.pushPose();
        rotateToFacing(poseStack, renderState.getFacing());

        if (!renderState.shouldRenderClosedModel()) {
            renderState.mountModel.submit(
                    poseStack,
                    submitNodeCollector,
                    renderState.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );

            submitLeverBar(renderState, poseStack, submitNodeCollector);
        }

        submitPanel(renderState, poseStack, submitNodeCollector);

        poseStack.popPose();
    }

    private static void submitPanel(
            HiddenBlockLeverRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector
    ) {
        float progress = renderState.getPanelProgress();
        float retractProgress = Math.min(
                progress / PANEL_RETRACT_FRACTION,
                1.0F
        );
        float riseProgress = progress <= PANEL_RETRACT_FRACTION
                ? 0.0F
                : (progress - PANEL_RETRACT_FRACTION)
                / (1.0F - PANEL_RETRACT_FRACTION);

        poseStack.pushPose();
        float closedHandoffInset = renderState.shouldRenderClosedModel()
                ? CLOSED_PANEL_HANDOFF_INSET
                : 0.0F;

        poseStack.translate(
                0.0F,
                riseProgress * PANEL_RISE_DISTANCE,
                retractProgress * PANEL_RETRACT_DISTANCE
                        + closedHandoffInset
        );

        BlockState panelState =
                DungeonCraft.HIDDEN_BLOCK_LEVER_PANEL_MODEL.defaultBlockState();

        renderState.panelModel.submit(
                poseStack,
                submitNodeCollector,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        submitBreakingOverlay(
                renderState,
                poseStack,
                submitNodeCollector,
                panelState
        );

        poseStack.popPose();
    }

    private static void submitLeverBar(
            HiddenBlockLeverRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector
    ) {
        float angle = LEVER_UP_ANGLE
                + renderState.getLeverProgress() * LEVER_PULL_ANGLE;

        poseStack.pushPose();
        poseStack.translate(LEVER_PIVOT_X, LEVER_PIVOT_Y, LEVER_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(angle));
        poseStack.translate(-LEVER_PIVOT_X, -LEVER_PIVOT_Y, -LEVER_PIVOT_Z);

        renderState.barModel.submit(
                poseStack,
                submitNodeCollector,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );

        poseStack.popPose();
    }

    private static void rotateToFacing(PoseStack poseStack, Direction facing) {
        float rotation = switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };

        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    private static void submitBreakingOverlay(
            HiddenBlockLeverRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            BlockState modelState
    ) {
        if (renderState.breakProgress == null) {
            return;
        }

        BlockStateModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getBlockStateModelSet()
                        .get(modelState);

        submitNodeCollector.submitBreakingBlockModel(
                poseStack,
                model,
                modelState.getSeed(renderState.blockPos),
                renderState.breakProgress.progress()
        );
    }
}
