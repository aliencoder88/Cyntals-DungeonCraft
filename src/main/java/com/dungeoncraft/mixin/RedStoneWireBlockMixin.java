package com.dungeoncraft.mixin;

import com.dungeoncraft.block.CopperWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 * Phase 4F: Makes vanilla redstone dust recognize Copper Wire in same-level, upward, and downward stair connections, and wakes lower diagonal Copper Wire when upper dust is removed
 * connections toward DungeonCraft Copper Wire.
 *
 * The previous mixin only changed shouldConnectTo(). In 26.1.2 that
 * was sufficient for some same-level checks, but it did not guarantee
 * that the final returned dust blockstate contained the required UP
 * direction for a Copper Wire one block higher.
 */
@Mixin(RedStoneWireBlock.class)
public abstract class RedStoneWireBlockMixin {

    /*
     * Restores the electrical/connectability hooks from Phase 3.1.
     *
     * Phase 3.2 added final SIDE/UP blockstate shaping, but accidentally
     * replaced these earlier hooks instead of keeping both systems.
     * Without them, the dust can look connected while vanilla's internal
     * connection checks still treat Copper Wire as unrelated.
     */

    @Inject(
            method =
                    "shouldConnectTo("
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/core/Direction;"
                    + ")Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void dungeoncraft$connectToCopperWire(
            BlockState state,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (
                state.getBlock()
                        instanceof CopperWireBlock
        ) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method =
                    "shouldConnectTo("
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + ")Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void dungeoncraft$connectUpToCopperWire(
            BlockState state,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (
                state.getBlock()
                        instanceof CopperWireBlock
        ) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "getStateForPlacement",
            at = @At("RETURN"),
            cancellable = true
    )
    private void dungeoncraft$applyCopperConnectionsOnPlacement(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState state =
                cir.getReturnValue();

        if (state == null) {
            return;
        }

        Level level =
                context.getLevel();

        BlockPos pos =
                context.getClickedPos();

        cir.setReturnValue(
                dungeoncraft$applyCopperConnections(
                        state,
                        level,
                        pos
                )
        );
    }

    @Inject(
            method = "updateShape",
            at = @At("RETURN"),
            cancellable = true
    )
    private void dungeoncraft$applyCopperConnectionsAfterNeighborUpdate(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTicks,
            BlockPos pos,
            Direction changedDirection,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random,
            CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState returnedState =
                cir.getReturnValue();

        if (returnedState == null) {
            return;
        }

        /*
         * If this redstone dust is being removed or has lost support,
         * its replacement state is commonly air.
         *
         * A Copper Wire one block outward and one block lower is diagonal,
         * so vanilla's ordinary direct-neighbor updates do not reliably
         * wake it. Mark that Copper state for visual verification and
         * schedule an electrical recalculation explicitly.
         */
        if (
                state.is(Blocks.REDSTONE_WIRE)
                && !returnedState.is(Blocks.REDSTONE_WIRE)
        ) {
            dungeoncraft$wakeLowerDiagonalCopper(
                    level,
                    pos
            );
        }

        cir.setReturnValue(
                dungeoncraft$applyCopperConnections(
                        returnedState,
                        level,
                        pos
                )
        );
    }

    /*
     * Wakes Copper Wire blocks that descended diagonally from the removed
     * upper dust.
     *
     * Layout:
     *
     *   [ removed dust ] [ air ]
     *   [ solid support ] [ Copper Wire ]
     */
    private static void dungeoncraft$wakeLowerDiagonalCopper(
            LevelReader level,
            BlockPos formerDustPos
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos supportPos =
                formerDustPos.below();

        BlockState supportState =
                serverLevel.getBlockState(
                        supportPos
                );

        /*
         * If the support was removed at the same time, a refresh is still
         * harmless and ensures no stale upward link or power survives.
         */
        for (
                Direction direction
                : Direction.Plane.HORIZONTAL
        ) {
            BlockPos lowerCopperPos =
                    formerDustPos
                            .relative(direction)
                            .below();

            BlockState copperState =
                    serverLevel.getBlockState(
                            lowerCopperPos
                    );

            if (
                    !(copperState.getBlock()
                            instanceof CopperWireBlock)
            ) {
                continue;
            }

            BlockState refreshState =
                    copperState.setValue(
                            CopperWireBlock
                                    .VISUAL_REFRESH_REQUIRED,
                            true
                    );

            serverLevel.setBlock(
                    lowerCopperPos,
                    refreshState,
                    2
            );

            serverLevel.scheduleTick(
                    lowerCopperPos,
                    copperState.getBlock(),
                    1
            );
        }
    }

    private static BlockState dungeoncraft$applyCopperConnections(
            BlockState state,
            LevelReader level,
            BlockPos dustPos
    ) {
        /*
         * updateShape() may legitimately return air when the redstone dust
         * is being broken or loses support. Never apply redstone-only
         * EnumProperties to that replacement state.
         */
        if (!state.is(Blocks.REDSTONE_WIRE)) {
            return state;
        }

        BlockState updatedState = state;

        for (
                Direction direction
                : Direction.Plane.HORIZONTAL
        ) {
            BlockPos sidePos =
                    dustPos.relative(direction);

            BlockState sideState =
                    level.getBlockState(sidePos);

            /*
             * Same-level Copper Wire.
             */
            if (
                    sideState.getBlock()
                            instanceof CopperWireBlock
            ) {
                updatedState =
                        dungeoncraft$setConnection(
                                updatedState,
                                direction,
                                RedstoneSide.SIDE
                        );

                continue;
            }

            /*
             * Copper Wire one block below and outward from this dust.
             *
             * Layout:
             *
             *   [ redstone dust ] [ air ]
             *   [ solid support ] [ Copper Wire ]
             *
             * Vanilla uses SIDE for a descending dust arm. The Copper
             * model supplies the visible vertical wall segment.
             */
            BlockPos lowerTargetPos =
                    sidePos.below();

            BlockState lowerTargetState =
                    level.getBlockState(
                            lowerTargetPos
                    );

            boolean canDescend =
                    lowerTargetState.getBlock()
                            instanceof CopperWireBlock
                            && !sideState.isRedstoneConductor(
                                    level,
                                    sidePos
                            );

            if (canDescend) {
                updatedState =
                        dungeoncraft$setConnection(
                                updatedState,
                                direction,
                                RedstoneSide.SIDE
                        );

                continue;
            }

            /*
             * Copper Wire one block above a solid neighboring support.
             *
             * Layout:
             *
             *   [ Copper Wire ] [ air ]
             *   [ solid block ] [ redstone dust ]
             */
            BlockPos upperTargetPos =
                    sidePos.above();

            BlockState upperTargetState =
                    level.getBlockState(
                            upperTargetPos
                    );

            BlockPos aboveDustPos =
                    dustPos.above();

            BlockState aboveDustState =
                    level.getBlockState(
                            aboveDustPos
                    );

            boolean canClimb =
                    upperTargetState.getBlock()
                            instanceof CopperWireBlock
                            && sideState.isRedstoneConductor(
                                    level,
                                    sidePos
                            )
                            && !aboveDustState.isRedstoneConductor(
                                    level,
                                    aboveDustPos
                            );

            if (canClimb) {
                updatedState =
                        dungeoncraft$setConnection(
                                updatedState,
                                direction,
                                RedstoneSide.UP
                        );
            }
        }

        return updatedState;
    }

    private static BlockState dungeoncraft$setConnection(
            BlockState state,
            Direction direction,
            RedstoneSide connection
    ) {
        /*
         * Defensive second guard. This prevents a crash even if another
         * future call reaches this helper with a replacement state such as
         * air, water, or a different block.
         */
        if (!state.is(Blocks.REDSTONE_WIRE)) {
            return state;
        }

        return switch (direction) {
            case NORTH ->
                    state.setValue(
                            BlockStateProperties.NORTH_REDSTONE,
                            connection
                    );

            case EAST ->
                    state.setValue(
                            BlockStateProperties.EAST_REDSTONE,
                            connection
                    );

            case SOUTH ->
                    state.setValue(
                            BlockStateProperties.SOUTH_REDSTONE,
                            connection
                    );

            case WEST ->
                    state.setValue(
                            BlockStateProperties.WEST_REDSTONE,
                            connection
                    );

            default -> state;
        };
    }
}
