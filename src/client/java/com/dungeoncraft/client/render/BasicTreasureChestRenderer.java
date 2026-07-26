package com.dungeoncraft.client.render;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.BasicTreasureChestBlockEntity;
import com.dungeoncraft.block.BasicTreasureChestBlock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/*
 * Draws the Basic Treasure Chest in the world.
 *
 * The renderer will eventually:
 *
 * - draw the stationary chest body;
 * - draw the lid separately;
 * - rotate the lid around its rear hinge;
 * - rotate the entire chest to match its facing;
 * - handle lighting and block-breaking overlays.
 */
public class BasicTreasureChestRenderer
        implements BlockEntityRenderer<
        BasicTreasureChestBlockEntity,
        BasicTreasureChestRenderState
        > {

    /*
     * General display context for the separately rendered lid model.
     */
    private static final BlockDisplayContext
            LID_DISPLAY_CONTEXT =
            BlockDisplayContext.create();

    /*
     * Converts the internal lid BlockState into prepared model data.
     */
    private final BlockModelResolver blockModelResolver;

    /*
     * Minecraft supplies a Context object when it creates
     * the renderer.
     *
     * Context can provide renderer-related tools such as:
     *
     * - the item renderer;
     * - the font renderer;
     * - model sets and other client rendering resources.
     *
     * We do not need to store anything from it yet.
     */
    public BasicTreasureChestRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.blockModelResolver =
                context.blockModelResolver();
    }

    /*
     * Converts a horizontal direction into the Y rotation used by the lid renderer.
     * PoseStack's positive Y rotation runs opposite to the
     * blockstate JSON model rotation for the quarter turns,
     * so EAST and WEST are reversed here.
     */
    private static float getFacingRotation(
            Direction facing
    ) {
        return switch (facing) {
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    /*
     * Creates a reusable render-state object for this renderer.
     *
     * Minecraft fills this state with the information needed
     * to draw one chest during a frame.
     */
    @Override
    public BasicTreasureChestRenderState createRenderState() {
        return new BasicTreasureChestRenderState();
    }

    /*
     * Copies live information from the block entity into
     * the render state.
     *
     * For now, the lid remains closed at 0.0F.
     * Later this method will copy the actual animated
     * lid progress and chest facing.
     */
    @Override
    public void extractRenderState(
            BasicTreasureChestBlockEntity blockEntity,
            BasicTreasureChestRenderState renderState,
            float tickProgress,
            Vec3 cameraPosition,
            @Nullable
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity,
                renderState,
                tickProgress,
                cameraPosition,
                crumblingOverlay
        );

        renderState.setLidProgress(
                blockEntity.getLidProgress(
                        tickProgress
                )
        );

        renderState.setFacing(
                blockEntity.getBlockState()
                        .getValue(
                                BasicTreasureChestBlock.FACING
                        )
        );

        /*
         * Prepare the internal lid block's model for submission.
         */
        this.blockModelResolver.update(
                renderState.lidModel,
                DungeonCraft.BASIC_TREASURE_CHEST_LID
                        .defaultBlockState(),
                LID_DISPLAY_CONTEXT
        );
    }

    /*
     * Called when Minecraft submits this chest for rendering.
     *
     * This method will contain the actual body and lid drawing
     * instructions in the next stages.
     */
    @Override
    public void submit(
            BasicTreasureChestRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState
    ) {
        /*
         * Save the current transformation state so the lid's
         * movement cannot affect another rendered object.
         */
        poseStack.pushPose();

        /*
         * Rotate the whole lid around the center of the block
         * so it matches the chest body's facing.
         */
        poseStack.translate(
                0.5F,
                0.0F,
                0.5F
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        getFacingRotation(
                                renderState.getFacing()
                        )
                )
        );

        poseStack.translate(
                -0.5F,
                0.0F,
                -0.5F
        );

        /*
         * Convert the normalized opening progress into an angle.
         *
         * 0.0F × 47.5F = 0 degrees
         * 1.0F × 47.5F = 47.5 degrees
         */
        float lidAngle =
                renderState.getLidProgress() * 90.0F;

        /*
         * Move the transformation origin to the lid's hinge pivot.
         *
         * Blockbench pivot:
         *
         * X = 8
         * Y = 8
         * Z = 15
         *
         * Minecraft's renderer uses block units from 0.0 to 1.0,
         * so each Blockbench coordinate is divided by 16.
         */
        poseStack.translate(
                8.0F / 16.0F,
                8.0F / 16.0F,
                15.0F / 16.0F
        );

        /*
         * Rotate around the X axis while the origin is positioned
         * at the rear hinge.
         *
         * This fixed angle is only a pivot test.
         */
        poseStack.mulPose(
                Axis.XP.rotationDegrees(lidAngle)
        );

        /*
         * Move the model back from the temporary hinge origin to
         * its normal block-space coordinates.
         */
        poseStack.translate(
                -8.0F / 16.0F,
                -8.0F / 16.0F,
                -15.0F / 16.0F
        );

        /*
         * Draw the prepared lid after applying the hinge rotation.
         */
        renderState.lidModel.submit(
                poseStack,
                submitNodeCollector,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );

        poseStack.popPose();
    }
}