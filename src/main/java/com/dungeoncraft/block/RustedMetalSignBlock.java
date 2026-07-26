package com.dungeoncraft.block;

// Gives access to the Rusted Metal Sign block entity class.
import com.dungeoncraft.block.entity.RustedMetalSignBlockEntity;
// Allows nullable return values for block entities.
import org.jetbrains.annotations.Nullable;
// Stores a block position in the world.
import net.minecraft.core.BlockPos;
// Stores directions like NORTH, SOUTH, EAST, and WEST.
import net.minecraft.core.Direction;
// Used when Minecraft creates the block entity for this block.
import net.minecraft.world.level.block.entity.BlockEntity;
// Marks this block as a block that can have a block entity.
import net.minecraft.world.level.block.EntityBlock;
// The base Minecraft block class.
import net.minecraft.world.level.block.Block;
// Gives this block its setup properties, such as strength and sound.
import net.minecraft.world.level.block.state.BlockBehaviour;
// Stores the current blockstate and its properties.
import net.minecraft.world.level.block.state.BlockState;
// Lets this block define which blockstate properties it uses.
import net.minecraft.world.level.block.state.StateDefinition;
// Gives access to common Minecraft blockstate properties, including horizontal facing.
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
// Used when the player places the block.
import net.minecraft.world.item.context.BlockPlaceContext;
// Lets this block check nearby blocks for support.
import net.minecraft.world.level.LevelReader;
// Lets this block access the world without changing it.
import net.minecraft.world.level.BlockGetter;
// Used for collision/outline shape checks.
import net.minecraft.world.phys.shapes.CollisionContext;
// Represents the sign's outline/collision shape.
import net.minecraft.world.phys.shapes.VoxelShape;
// Lets this block store an enum-style blockstate property.
// Direction is an enum, so FACING can be stored as EnumProperty<Direction>.
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import com.dungeoncraft.DungeonCraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.redstone.Orientation;
import com.dungeoncraft.network.OpenRustedMetalSignEditorPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/*
 * RustedMetalSignBlock
 *
 * This is the placed Rusted Metal Sign block.
 *
 * Phase 1 goals:
 *
 * - place as a thin wall-mounted metal sign;
 * - remember which direction it faces;
 * - create a RustedMetalSignBlockEntity;
 * - prepare for stored text, surface wear, and broken plate behavior later.
 *
 * This class does NOT open the engraving screen yet.
 * That will come after the block and block entity are safely registered.
 */
public class RustedMetalSignBlock
        extends Block
        implements EntityBlock {

    /*
     * SURFACE controls the visible condition of the center plate.
     *
     * 0 = clean
     * 1 = scuffed
     * 2 = worn
     * 3 = fragile
     * 4 = broken
     *
     * The block entity will still store the real saved data.
     * This blockstate value is mainly for model/texture switching.
     */
    public static final IntegerProperty SURFACE =
            IntegerProperty.create("surface", 0, 4);

    // FACING stores which horizontal direction the front of the sign points.
    // Direction.NORTH, SOUTH, EAST, and WEST are the allowed values.
    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    /*
     * Shape for a sign facing north.
     *
     * The sign is mounted against the south side of its block space.
     *
     * This matches the thinner model:
     * - X 1.0 to 15.0 covers the full sign width.
     * - Y 3.5 to 12.5 covers the full sign height.
     * - Z 15.25 to 16.0 covers the thinned plaque depth, including rivets.
     */
    private static final VoxelShape SHAPE_NORTH =
            Block.box(
                    1.0D, 3.5D, 15.50D,
                    15.0D, 12.5D, 16.0D
            );

    /*
     * Shape for a sign facing south.
     *
     * This is the opposite side of the block space from SHAPE_NORTH.
     */
    private static final VoxelShape SHAPE_SOUTH =
            Block.box(
                    1.0D, 3.5D, 0.0D,
                    15.0D, 12.5D, 0.50D
            );

    /*
     * Shape for a sign facing east.
     *
     * This is the rotated version of the north/south plaque depth.
     */
    private static final VoxelShape SHAPE_EAST =
            Block.box(
                    0.0D, 3.5D, 1.0D,
                    0.50D, 12.5D, 15.0D
            );

    /*
     * Shape for a sign facing west.
     *
     * This is the opposite side from SHAPE_EAST.
     */
    private static final VoxelShape SHAPE_WEST =
            Block.box(
                    15.50D, 3.5D, 1.0D,
                    16.0D, 12.5D, 15.0D
            );

    /*
     * Constructor.
     *
     * DungeonCraft.java creates this block and passes in the block properties.
     */
    public RustedMetalSignBlock(
            BlockBehaviour.Properties properties
    ) {
        // Sends the block properties to the base Block class.
        super(properties);

        /*
         * Sets the default blockstate.
         *
         * The actual direction will be changed when the player places it,
         * but Minecraft needs a valid default state.
         */
        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(SURFACE, 0)
        );
    }

    /*
     * Engraving Tool interaction.
     *
     * Right-clicking the Rusted Metal Sign with the Engraving Tool asks the
     * client to open the Rusted Metal Sign editor screen.
     *
     * The current text is sent to the client so the editor can start with
     * the sign's existing message.
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
        /*
         * Only the Engraving Tool opens the Rusted Metal Sign editor.
         *
         * Other items pass through normally.
         */
        if (!stack.is(DungeonCraft.ENGRAVING_TOOL)) {
            return InteractionResult.PASS;
        }

        /*
         * On the client side, return success so the hand animation feels right.
         *
         * The real editor-opening packet is sent from the server side below.
         */
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        /*
         * Get the block entity at this sign's position.
         */
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        /*
         * If this is our Rusted Metal Sign block entity,
         * ask the client to open the editor screen.
         */
        if (blockEntity instanceof RustedMetalSignBlockEntity signBlockEntity) {
            /*
             * Only a server player can receive the editor-opening packet.
             */
            if (player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(
                        serverPlayer,
                        new OpenRustedMetalSignEditorPayload(
                                pos,
                                signBlockEntity.getLine1(),
                                signBlockEntity.getLine2(),
                                signBlockEntity.getLine3(),
                                signBlockEntity.getLine4()
                        )
                );
            }

            return InteractionResult.SUCCESS;
        }

        /*
         * If something unexpected is here, do not handle the interaction.
         */
        return InteractionResult.PASS;
    }

    /*
     * Creates the block entity for this sign.
     *
     * Minecraft calls this when a Rusted Metal Sign is placed in the world.
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        // Creates and returns the block entity that stores this sign's data.
        return new RustedMetalSignBlockEntity(
                pos,
                state
        );
    }

    /*
     * Controls how the block is placed.
     *
     * This first version only allows placement on side faces,
     * not the floor or ceiling.
     */
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        // Gets the face of the block that the player clicked.
        Direction clickedFace =
                context.getClickedFace();

        /*
         * Do not place if the player clicked the top or bottom of a block.
         *
         * This keeps Phase 1 as a wall-mounted sign only.
         */
        if (
                clickedFace == Direction.UP
                        || clickedFace == Direction.DOWN
        ) {
            return null;
        }

        /*
         * Start every newly placed Rusted Metal Sign with a clean surface.
         *
         * SURFACE values:
         * 0 = clean
         * 1 = scuffed
         * 2 = worn
         * 3 = fragile
         * 4 = broken
         */
        BlockState placedState =
                this.defaultBlockState()
                        .setValue(
                                FACING,
                                clickedFace
                        )
                        .setValue(
                                SURFACE,
                                0
                        );

        /*
         * Only allow placement if the sign has a sturdy block behind it.
         */
        if (
                !placedState.canSurvive(
                        context.getLevel(),
                        context.getClickedPos()
                )
        ) {
            return null;
        }

        // Returns the final blockstate Minecraft should place.
        return placedState;
    }

    /*
     * Checks whether the sign has a valid wall/support block behind it.
     */
    @Override
    protected boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        // Gets the direction the front of the sign faces.
        Direction facing =
                state.getValue(
                        FACING
                );

        /*
         * The support block is behind the sign.
         *
         * If the sign faces NORTH, the support block is SOUTH of it.
         */
        BlockPos supportPos =
                pos.relative(
                        facing.getOpposite()
                );

        /*
         * Checks if the support block has a sturdy face toward the sign.
         *
         * This prevents the sign from floating in midair.
         */
        return level
                .getBlockState(
                        supportPos
                )
                .isFaceSturdy(
                        level,
                        supportPos,
                        facing
                );
    }

    /*
     * Controls the outline shape when the player looks at the sign.
     */
    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        // Gets the direction the sign is facing.
        Direction facing =
                state.getValue(
                        FACING
                );

        // Returns the correct thin plate shape for the sign direction.
        return switch (facing) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    /*
     * Collision shape for the Rusted Metal Sign.
     *
     * This returns an empty collision shape so the player can walk along the wall
     * through the sign, like vanilla wooden signs.
     *
     * The outline/selection shape still comes from getShape(), so the sign can
     * still be targeted and broken normally.
     */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }


    /*
     * Adds the FACING property to this block.
     *
     * Without this, Minecraft would not know that the block can face
     * north, south, east, or west.
     */
    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        // Adds the FACING property to the blockstate definition.
        builder.add(
                FACING, SURFACE
        );
    }

    /*
     * Called after this block is placed in the world.
     *
     * This restores saved Rusted Metal Sign data from the placed ItemStack.
     * That lets a written/worn sign keep its text and plate condition after
     * being broken and placed again.
     */
    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        // Let the normal Block behavior run first.
        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        // Gets the block entity at the newly placed sign position.
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        /*
         * If this is our Rusted Metal Sign block entity,
         * load saved text/condition data from the item stack.
         */
        if (blockEntity instanceof RustedMetalSignBlockEntity signBlockEntity) {
            signBlockEntity.loadFromItemStack(
                    stack
            );
        }
    }

    /*
     * Called when the player destroys this block.
     *
     * We use this instead of the normal loot-table drop because the dropped
     * Rusted Metal Sign item needs to carry its block entity data.
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
         * Only create the custom drop on the server side.
         *
         * Also do not drop anything if the player directly breaks the sign in creative mode.
         */
        if (
                !level.isClientSide()
                        && !player.getAbilities().instabuild
        ) {
            dropSavedSignItem(
                    level,
                    pos,
                    blockEntity
            );
        }

        /*
         * Do NOT call super.playerDestroy(...) here.
         *
         * The normal super method would use the loot table and could drop
         * a second blank Rusted Metal Sign.
         */
    }

    /*
     * Called when a neighboring block changes.
     *
     * This catches the case where the wall/support block behind the sign is broken.
     * If the sign no longer has support, it drops itself with saved data and removes
     * the floating placed block.
     */
    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston
    ) {
        /*
         * Let the normal Block behavior run first.
         */
        super.neighborChanged(
                state,
                level,
                pos,
                neighborBlock,
                orientation,
                movedByPiston
        );

        /*
         * If the sign still has a valid support block behind it,
         * do nothing.
         */
        if (state.canSurvive(level, pos)) {
            return;
        }

        /*
         * Only handle dropping/removing on the server side.
         */
        if (level.isClientSide()) {
            return;
        }

        /*
         * Get the block entity before removing the sign.
         *
         * This is important because the block entity contains the engraved text
         * and plate condition that need to be saved into the dropped item.
         */
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        /*
         * Drop the Rusted Metal Sign item with its saved text/condition data.
         */
        dropSavedSignItem(
                level,
                pos,
                blockEntity
        );

        /*
         * Remove the unsupported sign block without normal loot-table drops.
         *
         * The saved item was already dropped above.
         */
        level.removeBlock(
                pos,
                false
        );
    }

    /*
     * Drops a Rusted Metal Sign item that remembers this sign's block entity data.
     *
     * This is used by:
     * - normal player breaking;
     * - support block breaking.
     */
    private void dropSavedSignItem(
            Level level,
            BlockPos pos,
            @Nullable BlockEntity blockEntity
    ) {
        /*
         * Only drop items on the server side.
         */
        if (level.isClientSide()) {
            return;
        }

        /*
         * Create the Rusted Metal Sign item that will drop.
         */
        ItemStack droppedSign =
                new ItemStack(
                        this.asItem()
                );

        /*
         * If this sign has block entity data, save that data into the item.
         */
        if (blockEntity instanceof RustedMetalSignBlockEntity signBlockEntity) {
            signBlockEntity.saveToItemStack(
                    droppedSign
            );
        }

        /*
         * Drop the saved sign item into the world.
         */
        popResource(
                level,
                pos,
                droppedSign
        );
    }
}