package com.dungeoncraft.client.render;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.StonekeepLowDungeonChestBlock;
import com.dungeoncraft.block.entity.StonekeepLowDungeonChestBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Renders the body and lid models from the Blockbench geometry.
 *
 * Both baked models share one 128x128 texture. The body remains stationary;
 * the lid is submitted after an additional rotation around the authored hinge.
 */
public class StonekeepLowDungeonChestRenderer implements BlockEntityRenderer<
        StonekeepLowDungeonChestBlockEntity,
        StonekeepLowDungeonChestRenderState
        > {
    private static final BlockDisplayContext MODEL_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    /* Hinge in converted block-model coordinates: original (2, 9, -1.25) + (6, 0, 6). */
    private static final float HINGE_X = 8.0F / 16.0F;
    private static final float HINGE_Y = 9.0F / 16.0F;
    private static final float HINGE_Z = 4.75F / 16.0F;
    private static final float MAX_LID_ANGLE = 70.0F;

    private final BlockModelResolver blockModelResolver;

    public StonekeepLowDungeonChestRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public StonekeepLowDungeonChestRenderState createRenderState() {
        return new StonekeepLowDungeonChestRenderState();
    }

    @Override
    public void extractRenderState(
            StonekeepLowDungeonChestBlockEntity blockEntity,
            StonekeepLowDungeonChestRenderState renderState,
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

        renderState.setFacing(
                blockEntity.getBlockState().getValue(StonekeepLowDungeonChestBlock.FACING)
        );
        renderState.setLidProgress(blockEntity.getLidProgress(tickProgress));

        this.blockModelResolver.update(
                renderState.bodyModel,
                DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_BODY_MODEL.defaultBlockState(),
                MODEL_DISPLAY_CONTEXT
        );
        this.blockModelResolver.update(
                renderState.lidModel,
                DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_LID_MODEL.defaultBlockState(),
                MODEL_DISPLAY_CONTEXT
        );
    }

    @Override
    public void submit(
            StonekeepLowDungeonChestRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState
    ) {
        poseStack.pushPose();

        /*
         * Move the authored root pivot to the correct block corner/boundary for
         * this footprint, rotate in exact 45-degree steps, then return to the
         * model's local root at (8, 0, 8).
         */
        poseStack.translate(
                renderState.getFacing().placementPivotX(),
                0.0F,
                renderState.getFacing().placementPivotZ()
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(renderState.getFacing().modelRotationDegrees())
        );
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        BlockState bodyState =
                DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_BODY_MODEL.defaultBlockState();

        renderState.bodyModel.submit(
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
                bodyState
        );

        poseStack.pushPose();
        poseStack.translate(HINGE_X, HINGE_Y, HINGE_Z);
        poseStack.mulPose(
                Axis.XP.rotationDegrees(renderState.getLidProgress() * MAX_LID_ANGLE)
        );
        poseStack.translate(-HINGE_X, -HINGE_Y, -HINGE_Z);

        BlockState lidState =
                DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_LID_MODEL.defaultBlockState();

        renderState.lidModel.submit(
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
                lidState
        );

        poseStack.popPose();
        poseStack.popPose();
    }

    /**
     * Draws Minecraft's current crack stage over one renderer-only chest model.
     *
     * The master chest block is intentionally RenderShape.INVISIBLE, so the
     * normal terrain-breaking pass cannot draw cracks for it. The block-entity
     * render state still receives the current break stage, allowing the body
     * and moving lid models to submit the overlay here using the same poses as
     * their normal geometry.
     */
    private static void submitBreakingOverlay(
            StonekeepLowDungeonChestRenderState renderState,
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
