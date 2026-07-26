package com.dungeoncraft.mixin.client;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.StonekeepChestFacing;
import com.dungeoncraft.block.StonekeepLowDungeonChestBlock;
import com.dungeoncraft.block.StonekeepLowDungeonChestPartBlock;
import com.dungeoncraft.block.StonekeepLowDungeonChestShapes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces only the diagonal Stonekeep chest's 64-step vanilla selection
 * outline with one clean whole-chest outline.
 *
 * Collision, interaction, placement, and ray tracing continue to use the
 * accurate 64-step VoxelShapes. Cardinal chest outlines remain vanilla.
 */
@Mixin(LevelRenderer.class)
public abstract class StonekeepChestDiagonalOutlineMixin {

    /** Matches vanilla's normal translucent-black block-outline color. */
    private static final int NORMAL_OUTLINE_COLOR = ARGB.black(102);

    /** Matches vanilla's high-contrast primary outline color. */
    private static final int HIGH_CONTRAST_OUTLINE_COLOR = 0xFF57FFE1;

    /** Matches vanilla's high-contrast black backing line. */
    private static final int HIGH_CONTRAST_BACKING_COLOR = 0xFF000000;

    /** Matches vanilla's high-contrast backing-line width. */
    private static final float HIGH_CONTRAST_BACKING_WIDTH = 7.0F;

    @Inject(
            method = "renderBlockOutline(" +
                    "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;" +
                    "Lcom/mojang/blaze3d/vertex/PoseStack;" +
                    "Z" +
                    "Lnet/minecraft/client/renderer/state/level/LevelRenderState;" +
                    ")V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dungeoncraft$renderCleanDiagonalChestOutline(
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            boolean translucentPass,
            LevelRenderState renderState,
            CallbackInfo callbackInfo
    ) {
        BlockOutlineRenderState outlineState =
                renderState.blockOutlineRenderState;

        /*
         * LevelRenderer calls this method once for each outline pass. Preserve
         * vanilla's pass filtering so the custom outline is not drawn twice.
         */
        if (outlineState == null
                || outlineState.isTranslucent() != translucentPass) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || renderState.cameraRenderState == null) {
            return;
        }

        BlockPos selectedPos = outlineState.pos();
        BlockState selectedState = level.getBlockState(selectedPos);
        BlockPos masterPos = dungeoncraft$findMasterPos(
                level,
                selectedPos,
                selectedState
        );

        if (masterPos == null) {
            return;
        }

        BlockState masterState = level.getBlockState(masterPos);
        if (!masterState.is(DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST)) {
            return;
        }

        StonekeepChestFacing facing = masterState.getValue(
                StonekeepLowDungeonChestBlock.FACING
        );

        if (!facing.isDiagonal()) {
            return;
        }

        StonekeepLowDungeonChestShapes.DiagonalOutline outline =
                StonekeepLowDungeonChestShapes.diagonalOutline(facing);

        Vec3 cameraPos = renderState.cameraRenderState.pos;

        if (outlineState.highContrast()) {
            VertexConsumer backingConsumer = bufferSource.getBuffer(
                    RenderTypes.secondaryBlockOutline()
            );

            dungeoncraft$drawOutline(
                    poseStack,
                    backingConsumer,
                    masterPos,
                    cameraPos,
                    outline,
                    HIGH_CONTRAST_BACKING_COLOR,
                    HIGH_CONTRAST_BACKING_WIDTH
            );
        }

        VertexConsumer lineConsumer = bufferSource.getBuffer(
                RenderTypes.lines()
        );

        int lineColor = outlineState.highContrast()
                ? HIGH_CONTRAST_OUTLINE_COLOR
                : NORMAL_OUTLINE_COLOR;

        float lineWidth = Minecraft.getInstance()
                .gameRenderer
                .getGameRenderState()
                .windowRenderState
                .appropriateLineWidth;

        dungeoncraft$drawOutline(
                poseStack,
                lineConsumer,
                masterPos,
                cameraPos,
                outline,
                lineColor,
                lineWidth
        );

        /* Vanilla normally performs this flush after rendering its shape. */
        bufferSource.endLastBatch();

        /* Suppress the 64-step vanilla lines for this diagonal chest only. */
        callbackInfo.cancel();
    }

    private static BlockPos dungeoncraft$findMasterPos(
            ClientLevel level,
            BlockPos selectedPos,
            BlockState selectedState
    ) {
        if (selectedState.is(DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST)) {
            return selectedPos;
        }

        if (!selectedState.is(
                DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_PART
        )) {
            return null;
        }

        BlockPos masterPos =
                StonekeepLowDungeonChestPartBlock.getMasterPos(
                        selectedPos,
                        selectedState
                );

        return level.getBlockState(masterPos)
                .is(DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST)
                ? masterPos
                : null;
    }

    /**
     * Draws the twelve main outer edges of the closed diagonal chest:
     * four corner verticals, four bottom edges, and four top edges.
     */
    private static void dungeoncraft$drawOutline(
            PoseStack poseStack,
            VertexConsumer consumer,
            BlockPos masterPos,
            Vec3 cameraPos,
            StonekeepLowDungeonChestShapes.DiagonalOutline outline,
            int color,
            float lineWidth
    ) {
        double originX = masterPos.getX() - cameraPos.x;
        double originY = masterPos.getY() - cameraPos.y;
        double originZ = masterPos.getZ() - cameraPos.z;

        StonekeepLowDungeonChestShapes.OutlinePoint backLeft =
                outline.backLeft();
        StonekeepLowDungeonChestShapes.OutlinePoint frontLeft =
                outline.frontLeft();
        StonekeepLowDungeonChestShapes.OutlinePoint frontRight =
                outline.frontRight();
        StonekeepLowDungeonChestShapes.OutlinePoint backRight =
                outline.backRight();

        double bottomY = originY;
        double topY = originY + outline.height();

        /* Four true outer-corner vertical lines. */
        dungeoncraft$vertical(
                poseStack,
                consumer,
                originX,
                originZ,
                bottomY,
                topY,
                backLeft,
                color,
                lineWidth
        );
        dungeoncraft$vertical(
                poseStack,
                consumer,
                originX,
                originZ,
                bottomY,
                topY,
                frontLeft,
                color,
                lineWidth
        );
        dungeoncraft$vertical(
                poseStack,
                consumer,
                originX,
                originZ,
                bottomY,
                topY,
                frontRight,
                color,
                lineWidth
        );
        dungeoncraft$vertical(
                poseStack,
                consumer,
                originX,
                originZ,
                bottomY,
                topY,
                backRight,
                color,
                lineWidth
        );

        /* Four bottom edges and four matching top edges. */
        dungeoncraft$perimeter(
                poseStack,
                consumer,
                originX,
                originZ,
                bottomY,
                backLeft,
                frontLeft,
                frontRight,
                backRight,
                color,
                lineWidth
        );
        dungeoncraft$perimeter(
                poseStack,
                consumer,
                originX,
                originZ,
                topY,
                backLeft,
                frontLeft,
                frontRight,
                backRight,
                color,
                lineWidth
        );
    }

    private static void dungeoncraft$vertical(
            PoseStack poseStack,
            VertexConsumer consumer,
            double originX,
            double originZ,
            double bottomY,
            double topY,
            StonekeepLowDungeonChestShapes.OutlinePoint point,
            int color,
            float lineWidth
    ) {
        dungeoncraft$line(
                poseStack,
                consumer,
                originX + point.x(),
                bottomY,
                originZ + point.z(),
                originX + point.x(),
                topY,
                originZ + point.z(),
                color,
                lineWidth
        );
    }

    private static void dungeoncraft$perimeter(
            PoseStack poseStack,
            VertexConsumer consumer,
            double originX,
            double originZ,
            double y,
            StonekeepLowDungeonChestShapes.OutlinePoint backLeft,
            StonekeepLowDungeonChestShapes.OutlinePoint frontLeft,
            StonekeepLowDungeonChestShapes.OutlinePoint frontRight,
            StonekeepLowDungeonChestShapes.OutlinePoint backRight,
            int color,
            float lineWidth
    ) {
        dungeoncraft$horizontal(
                poseStack,
                consumer,
                originX,
                originZ,
                y,
                backLeft,
                frontLeft,
                color,
                lineWidth
        );
        dungeoncraft$horizontal(
                poseStack,
                consumer,
                originX,
                originZ,
                y,
                frontLeft,
                frontRight,
                color,
                lineWidth
        );
        dungeoncraft$horizontal(
                poseStack,
                consumer,
                originX,
                originZ,
                y,
                frontRight,
                backRight,
                color,
                lineWidth
        );
        dungeoncraft$horizontal(
                poseStack,
                consumer,
                originX,
                originZ,
                y,
                backRight,
                backLeft,
                color,
                lineWidth
        );
    }

    private static void dungeoncraft$horizontal(
            PoseStack poseStack,
            VertexConsumer consumer,
            double originX,
            double originZ,
            double y,
            StonekeepLowDungeonChestShapes.OutlinePoint start,
            StonekeepLowDungeonChestShapes.OutlinePoint end,
            int color,
            float lineWidth
    ) {
        dungeoncraft$line(
                poseStack,
                consumer,
                originX + start.x(),
                y,
                originZ + start.z(),
                originX + end.x(),
                y,
                originZ + end.z(),
                color,
                lineWidth
        );
    }

    private static void dungeoncraft$line(
            PoseStack poseStack,
            VertexConsumer consumer,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            int color,
            float lineWidth
    ) {
        float normalX = (float) (endX - startX);
        float normalY = (float) (endY - startY);
        float normalZ = (float) (endZ - startZ);
        float normalLength = (float) Math.sqrt(
                normalX * normalX
                        + normalY * normalY
                        + normalZ * normalZ
        );

        if (normalLength == 0.0F) {
            return;
        }

        normalX /= normalLength;
        normalY /= normalLength;
        normalZ /= normalLength;

        PoseStack.Pose pose = poseStack.last();

        consumer.addVertex(
                        pose,
                        (float) startX,
                        (float) startY,
                        (float) startZ
                )
                .setColor(color)
                .setNormal(pose, normalX, normalY, normalZ)
                .setLineWidth(lineWidth);

        consumer.addVertex(
                        pose,
                        (float) endX,
                        (float) endY,
                        (float) endZ
                )
                .setColor(color)
                .setNormal(pose, normalX, normalY, normalZ)
                .setLineWidth(lineWidth);
    }
}
