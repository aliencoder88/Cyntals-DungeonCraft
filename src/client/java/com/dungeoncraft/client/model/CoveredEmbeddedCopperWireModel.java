package com.dungeoncraft.client.model;

import com.dungeoncraft.DungeonCraft;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/*
 * Position-aware block model for Covered Embedded Copper Wire.
 *
 * The wrapper BlockState is identical at every covered-wire position, but
 * the visible cover is stored independently in block-entity data.
 *
 * During chunk building, FabricBlockGetter supplies an immutable snapshot
 * of that data. The model then delegates directly to the selected cover's
 * baked BlockStateModel.
 *
 * Because the delegated quads enter the normal terrain mesh, Minecraft
 * applies normal face lighting, smooth ambient occlusion, culling, render
 * layers, and breaking overlays.
 */
public final class CoveredEmbeddedCopperWireModel
        extends WrapperBlockStateModel {

    public CoveredEmbeddedCopperWireModel(
            BlockStateModel wrapped
    ) {
        super(wrapped);
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState wrapperState,
            RandomSource random,
            Predicate<Direction> cullTest
    ) {
        BlockState coverState =
                getCoverState(
                        level,
                        pos
                );

        if (coverState == null) {
            /*
             * The wrapped JSON model is intentionally empty. This fallback
             * avoids unsafe BlockEntity access if render data is temporarily
             * unavailable during loading.
             */
            super.emitQuads(
                    emitter,
                    level,
                    pos,
                    wrapperState,
                    random,
                    cullTest
            );

            return;
        }

        BlockStateModel coverModel =
                getCoverModel(
                        coverState
                );

        random.setSeed(
                coverState.getSeed(
                        pos
                )
        );

        (
                (FabricBlockStateModel)
                        coverModel
        ).emitQuads(
                emitter,
                level,
                pos,
                coverState,
                random,
                cullTest
        );
    }

    @Override
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState wrapperState,
            RandomSource random
    ) {
        BlockState coverState =
                getCoverState(
                        level,
                        pos
                );

        if (coverState == null) {
            return super.createGeometryKey(
                    level,
                    pos,
                    wrapperState,
                    random
            );
        }

        BlockStateModel coverModel =
                getCoverModel(
                        coverState
                );

        random.setSeed(
                coverState.getSeed(
                        pos
                )
        );

        Object delegatedKey =
                (
                        (FabricBlockStateModel)
                                coverModel
                ).createGeometryKey(
                        level,
                        pos,
                        coverState,
                        random
                );

        /*
         * A null key tells the renderer not to cache this context. This is
         * safer than caching an incomplete key for a cover model whose own
         * geometry cannot be represented by a stable key.
         */
        if (delegatedKey == null) {
            return null;
        }

        return new CoverGeometryKey(
                coverState,
                delegatedKey
        );
    }

    @Override
    public int materialFlags(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState wrapperState,
            RandomSource random
    ) {
        BlockState coverState =
                getCoverState(
                        level,
                        pos
                );

        if (coverState == null) {
            return super.materialFlags(
                    level,
                    pos,
                    wrapperState,
                    random
            );
        }

        BlockStateModel coverModel =
                getCoverModel(
                        coverState
                );

        random.setSeed(
                coverState.getSeed(
                        pos
                )
        );

        return (
                (FabricBlockStateModel)
                        coverModel
        ).materialFlags(
                level,
                pos,
                coverState,
                random
        );
    }

    @Nullable
    private static BlockState getCoverState(
            BlockAndTintGetter level,
            BlockPos pos
    ) {
        Object renderData =
                (
                        (FabricBlockGetter)
                                level
                ).getBlockEntityRenderData(
                        pos
                );

        if (
                !(renderData
                        instanceof BlockState coverState)
                || coverState.isAir()
                || coverState.is(
                        DungeonCraft
                                .COVERED_EMBEDDED_COPPER_WIRE
                )
        ) {
            return null;
        }

        return coverState;
    }

    private static BlockStateModel getCoverModel(
            BlockState coverState
    ) {
        return Minecraft
                .getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .get(
                        coverState
                );
    }

    /*
     * Geometry caching must distinguish both the selected cover state and
     * the delegated model's own geometry key.
     */
    private record CoverGeometryKey(
            BlockState coverState,
            Object delegatedKey
    ) {
    }
}
