package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.CoveredEmbeddedCopperWireBlockEntity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.LevelAccessor;
// Lets this class return an empty item-drop list from getDrops().
import java.util.List;

// Lets this class override Minecraft's normal loot-table drop method.
import net.minecraft.world.level.storage.loot.LootParams;

/*
 * Faux solid block placed over Embedded Copper Wire.
 *
 * This block remains a CopperWireBlock subclass, so the existing network
 * controller sees it as normal Copper Wire. A block entity stores the
 * separate solid cover BlockState for rendering and drops.
 */
public class CoveredEmbeddedCopperWireBlock
        extends EmbeddedCopperWireBlock
        implements EntityBlock {

    private static final VoxelShape FALLBACK_FULL_BLOCK =
            Shapes.block();

    public CoveredEmbeddedCopperWireBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new CoveredEmbeddedCopperWireBlockEntity(
                pos,
                state
        );
    }

    /*
     * Phase 3.6 renders the stored cover through Minecraft's ordinary
     * chunk/block-model pipeline.
     *
     * A Fabric dynamic block model reads the immutable cover BlockState
     * supplied by the block entity and emits that cover model's quads.
     *
     * This allows the visible wrapper to receive the same:
     *
     * - per-face lighting;
     * - smooth ambient occlusion;
     * - hidden-face culling;
     * - chunk render-layer handling;
     * - vanilla breaking overlay
     *
     * as the real covering block beside it.
     */
    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    /*
     * Covered wire currently accepts only full-collision cover blocks.
     *
     * Treat the wrapper as a full occluding cube so adjacent terrain
     * blocks cull the faces hidden against it, just as they do beside a
     * normal block.
     *
     * Without this, the wrapper's block-entity-rendered cube and the
     * neighboring terrain block can both keep their shared internal face,
     * producing a dark or doubled seam.
     */
    @Override
    protected VoxelShape getOcclusionShape(
            BlockState state
    ) {
        return FALLBACK_FULL_BLOCK;
    }

    /*
     * Use the full occlusion shape when Minecraft calculates light and
     * ambient-occlusion behavior around this wrapper.
     */
    @Override
    protected boolean useShapeForLightOcclusion(
            BlockState state
    ) {
        return true;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        BlockState coverState =
                getStoredCoverState(
                        level,
                        pos
                );

        if (coverState == null) {
            return FALLBACK_FULL_BLOCK;
        }

        return coverState.getShape(
                level,
                pos,
                context
        );
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        BlockState coverState =
                getStoredCoverState(
                        level,
                        pos
                );

        if (coverState == null) {
            return FALLBACK_FULL_BLOCK;
        }

        return coverState.getCollisionShape(
                level,
                pos,
                context
        );
    }

    /*
     * A covered wire cannot be re-covered or changed in place.
     *
     * EmbeddedCopperWireBlock normally consumes a held full block and
     * stores it as the new cover. This wrapper deliberately blocks that
     * inherited behavior.
     *
     * Returning PASS lets the held BlockItem continue its ordinary
     * placement behavior on the clicked face, so the new block is placed
     * beside the faux cover rather than replacing it.
     *
     * To change the faux cover:
     *
     * 1. Break the current cover.
     * 2. The exposed Embedded Copper Wire remains.
     * 3. Place the replacement cover onto that exposed wire.
     */
    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        return InteractionResult.PASS;
    }

    /*
     * Mine the faux cover at the same speed as the stored covering block.
     *
     * Without this override, Minecraft calculates mining progress from
     * the internal wrapper instead of the block that the player sees.
     */
    @Override
    protected float getDestroyProgress(
            BlockState state,
            Player player,
            BlockGetter level,
            BlockPos pos
    ) {
        BlockState coverState =
                getStoredCoverState(
                        level,
                        pos
                );

        if (coverState == null) {
            return super.getDestroyProgress(
                    state,
                    player,
                    level,
                    pos
            );
        }

        return coverState.getDestroyProgress(
                player,
                level,
                pos
        );
    }

    /*
     * Creative mode does not call playerDestroy(), so mark the block for
     * conversion into exposed Embedded Copper Wire before Minecraft removes
     * the covered wrapper.
     */
    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        BlockState adjustedState =
                super.playerWillDestroy(
                        level,
                        pos,
                        state,
                        player
                );

        if (!player.getAbilities().instabuild) {
            return adjustedState;
        }

        return EmbeddedCopperWireBlock.copyWireProperties(
                state,
                DungeonCraft
                        .EMBEDDED_COPPER_WIRE
                        .defaultBlockState()
        );
    }

    /*
     * Minecraft calls this after removing the original wrapper.
     *
     * When playerWillDestroy() returned an Embedded Copper Wire state, restore
     * that state at the same position instead of leaving air behind.
     */
    @Override
    public void destroy(
            LevelAccessor level,
            BlockPos pos,
            BlockState state
    ) {
        super.destroy(
                level,
                pos,
                state
        );

        if (
                state.is(
                        DungeonCraft.EMBEDDED_COPPER_WIRE
                )
        ) {
            level.setBlock(
                    pos,
                    state,
                    Block.UPDATE_ALL
            );

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.scheduleTick(
                        pos,
                        DungeonCraft.EMBEDDED_COPPER_WIRE,
                        1
                );
            }
        }
    }

    /*
     * Prevent the covered embedded wire wrapper from dropping anything through
     * Minecraft's normal loot-table path.
     *
     * Why this exists:
     *
     * - Directly breaking the faux cover is handled manually in playerDestroy().
     * - Support-loss cleanup is handled manually in getStateAfterSupportLost().
     * - The wrapper block itself should never auto-drop the stored cover block.
     *
     * If we allow normal loot-table drops here, support-loss can accidentally
     * create a duplicate copy of the stored cover block.
     */
    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder lootParams
    ) {
        // Return no automatic drops from the wrapper itself.
        return List.of();
    }

    /*
     * Survival breaking removes only the stored cover.
     *
     * Direct faux-cover break:
     *
     * - drops the stored cover block;
     * - restores exposed Embedded Copper Wire at this position.
     *
     * Support-loss cleanup:
     *
     * - must NOT drop the stored cover block;
     * - must NOT restore exposed wire here;
     * - is handled by getStateAfterSupportLost().
     */
    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool
    ) {
        /*
         * Check whether this covered wire still has valid support.
         *
         * Directly breaking the faux cover should normally still have support.
         * Support-loss cleanup happens when the block below is already gone.
         */
        boolean lostSupport =
                !state.canSurvive(
                        level,
                        pos
                );

        /*
         * Read the stored cover block from the block entity before calling super.
         *
         * After the wrapper is removed, the block entity may no longer be available.
         */
        BlockState coverState =
                blockEntity
                        instanceof CoveredEmbeddedCopperWireBlockEntity
                        coveredWireEntity
                        ? coveredWireEntity.getCoverState()
                        : null;

        /*
         * Let Minecraft run its normal player-destroy bookkeeping.
         *
         * The getDrops() override above makes sure this does not auto-drop
         * the wrapper or the stored cover block.
         */
        super.playerDestroy(
                level,
                player,
                pos,
                state,
                blockEntity,
                tool
        );

        /*
         * Do not run real block-restoration or item-drop logic on the client.
         */
        if (level.isClientSide()) {
            return;
        }

        /*
         * If support was lost, this is not a direct faux-cover break.
         *
         * getStateAfterSupportLost() is responsible for:
         *
         * - dropping the Copper Wire;
         * - replacing this position with the stored cover block.
         *
         * Returning here prevents a duplicate stored-cover item from dropping.
         */
        if (lostSupport) {
            return;
        }

        /*
         * Build the exposed Embedded Copper Wire state.
         *
         * This keeps the wire's route and power properties from the covered wrapper.
         */
        BlockState exposedWireState =
                EmbeddedCopperWireBlock
                        .copyWireProperties(
                                state,
                                DungeonCraft
                                        .EMBEDDED_COPPER_WIRE
                                        .defaultBlockState()
                        );

        /*
         * Restore exposed Embedded Copper Wire where the faux cover was broken.
         */
        level.setBlock(
                pos,
                exposedWireState,
                Block.UPDATE_ALL
        );

        /*
         * Creative mode removes the cover without generating an item.
         * Survival mode returns one copy of the stored full block.
         */
        if (
                !player.getAbilities().instabuild
                        && coverState != null
                        && !coverState.isAir()
        ) {
            Block.popResource(
                    level,
                    pos,
                    new ItemStack(
                            coverState.getBlock()
                    )
            );
        }

        /*
         * Schedule the exposed wire to update itself after being restored.
         */
        level.scheduleTick(
                pos,
                DungeonCraft.EMBEDDED_COPPER_WIRE,
                1
        );
    }

    /*
     * Covered-wire support loss.
     *
     * This method runs when the block below the covered embedded wire is broken
     * and the covered wire can no longer survive.
     *
     * Desired behavior:
     *
     * - The support block below drops from its own normal loot table.
     * - The hidden Copper Wire drops as an item.
     * - The stored cover block is placed back into this position.
     * - The stored cover block is NOT dropped as an item.
     *
     * This is different from directly breaking the faux cover.
     * Direct faux-cover breaking is still handled by playerDestroy().
     */
    @Override
    protected BlockState getStateAfterSupportLost(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        /*
         * Get the stored cover block from the block entity.
         *
         * Example:
         * - If the wire was covered with Limestone Fieldstone,
         *   this should be the Limestone Fieldstone BlockState.
         */
        BlockState storedCoverState =
                getStoredCoverState(
                        level,
                        pos
                );

        /*
         * Only spawn item drops on the server side.
         *
         * The client is only for visuals.
         * The server owns real item drops and world changes.
         */
        if (level instanceof ServerLevel serverLevel) {

            /*
             * Create one normal Copper Wire item stack.
             *
             * This represents the embedded wire being knocked loose
             * when its support block is broken.
             */
            ItemStack wireDrop =
                    new ItemStack(
                            DungeonCraft.COPPER_WIRE_ITEM
                    );

            /*
             * Make sure the wire item is valid before dropping it.
             */
            if (!wireDrop.isEmpty()) {

                /*
                 * Drop the Copper Wire item into the world.
                 *
                 * This should be the only item dropped by the covered wire cleanup.
                 * The stored cover block should be placed back, not dropped.
                 */
                Block.popResource(
                        serverLevel,
                        pos,
                        wireDrop
                );
            }
        }

        /*
         * If the stored cover is missing or corrupted, remove the wrapper.
         *
         * This fallback prevents Minecraft from inventing a fake cover block.
         */
        if (
                storedCoverState == null
                        || storedCoverState.isAir()
        ) {
            return Blocks.AIR.defaultBlockState();
        }

        /*
         * Put the stored cover block back into the world.
         *
         * This is the key line that restores the old behavior:
         *
         * - the covered wire wrapper disappears;
         * - the stored cover block replaces it;
         * - the cover block is not dropped as an item.
         */
        return storedCoverState;
    }

    @Nullable
    private static BlockState getStoredCoverState(
            BlockGetter level,
            BlockPos pos
    ) {
        if (
                level.getBlockEntity(pos)
                        instanceof CoveredEmbeddedCopperWireBlockEntity
                        coveredWireEntity
        ) {
            return coveredWireEntity.getCoverState();
        }
        return null;
    }
}
