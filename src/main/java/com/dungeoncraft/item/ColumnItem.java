package com.dungeoncraft.item;

// Main mod class, used so this item can access:
//
// COLUMN
// COLUMN_BASE
// COLUMN_TOP
// FLANKED_COLUMN
import com.dungeoncraft.DungeonCraft;

// Block position class used to identify:
//
// - The block the player clicked.
// - The position where the column will be placed.
// - The blocks directly above and below the placement location.
import net.minecraft.core.BlockPos;

// Direction identifies which face of a block was clicked.
//
// It is also used when checking whether a nearby block has
// a sturdy face pointing toward the new column.
import net.minecraft.core.Direction;

// Result returned after the player uses the item.
//
// SUCCESS means the column was placed.
// FAIL means placement was prevented.
import net.minecraft.world.InteractionResult;

// Represents the player using the Column item.
//
// This is used to check whether the player is holding Shift
// and whether the player is in Creative mode.
import net.minecraft.world.entity.player.Player;

// Base item class.
//
// ColumnItem extends Item rather than BlockItem because this
// single item can choose between three different registered blocks.
import net.minecraft.world.item.Item;

// Context used to perform Minecraft's normal replaceability check.
import net.minecraft.world.item.context.BlockPlaceContext;

// Contains information about the player's right-click action:
//
// - Clicked block
// - Clicked face
// - Player
// - World
// - Held item stack
import net.minecraft.world.item.context.UseOnContext;

// Represents the Minecraft world where placement occurs.
import net.minecraft.world.level.Level;

// Represents the exact state of a block being examined or placed.
import net.minecraft.world.level.block.state.BlockState;


/*
 * ColumnItem
 *
 * This is the single inventory item used to place:
 *
 * - COLUMN      = regular middle shaft
 * - COLUMN_BASE = wider floor base
 * - COLUMN_TOP  = wider ceiling cap
 *
 * Smart-placement rules:
 *
 * 1. Holding Shift:
 *    Always places a regular Column shaft.
 *
 * 2. Clicking an existing column piece:
 *    Places another regular shaft.
 *
 * 3. Clicking the top face of a non-column block:
 *    Places a Column Base.
 *
 * 4. Clicking the underside of a non-column block:
 *    Places a Column Top.
 *
 * 5. Clicking the side of a wall:
 *    - Sturdy non-column block below the placement space -> Base
 *    - Sturdy non-column block above the placement space -> Top
 *    - Neither support -> Regular shaft
 *    - Both supports -> Regular shaft
 *
 * The selected form is checked only during placement.
 *
 * After placement:
 *
 * - Breaking the floor beneath a base does not change it.
 * - Breaking the ceiling above a top does not change it.
 * - Adding a floor beneath a shaft does not turn it into a base.
 * - Adding a ceiling above a shaft does not turn it into a top.
 */
public class ColumnItem extends Item {

    /*
     * Constructor
     *
     * Receives the Item.Properties created in DungeonCraft.java
     * and passes them to Minecraft's normal Item constructor.
     */
    public ColumnItem(Properties properties) {
        super(properties);
    }


    /*
     * useOn()
     *
     * Runs when the player right-clicks a block while holding
     * the regular Column item.
     *
     * This method:
     *
     * 1. Finds the intended placement position.
     * 2. Verifies that the destination can be replaced.
     * 3. Checks Shift and existing column pieces.
     * 4. Selects shaft, base, or top.
     * 5. Places the chosen registered block.
     * 6. Consumes one item outside Creative mode.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {

        // Get the world in which the item is being used.
        Level level = context.getLevel();

        // Get the player using the item.
        //
        // The player can theoretically be null when an automated
        // system invokes item behavior, so null checks are retained.
        Player player = context.getPlayer();

        // Determine which face of the block was clicked.
        Direction clickedFace = context.getClickedFace();

        // Get the world position of the block that was clicked.
        BlockPos clickedPos = context.getClickedPos();

        /*
         * Determine where the new column block will be placed.
         *
         * Examples:
         *
         * Click UP:
         * - Place above the clicked block.
         *
         * Click DOWN:
         * - Place below the clicked block.
         *
         * Click NORTH, SOUTH, EAST, or WEST:
         * - Place beside the clicked block.
         */
        BlockPos placePos =
                clickedPos.relative(clickedFace);

        /*
         * Create Minecraft's standard placement context.
         *
         * This allows us to use the same replaceability check
         * that normal BlockItem placement uses.
         */
        BlockPlaceContext placeContext =
                new BlockPlaceContext(context);

        /*
         * Stop if the intended location contains a block that
         * cannot be replaced.
         */
        if (!level.getBlockState(placePos).canBeReplaced(placeContext)) {
            return InteractionResult.FAIL;
        }

        // Read the state of the block directly clicked by the player.
        BlockState clickedState =
                level.getBlockState(clickedPos);

        /*
         * Determine whether the clicked block belongs to either
         * the regular or Flanked Column family.
         *
         * Clicking an existing column piece should continue the
         * structure with a regular shaft.
         */
        boolean clickedColumnPiece =
                isColumnBlock(clickedState);

        /*
         * Holding Shift is the manual override.
         *
         * Shift always forces the regular shaft, regardless of:
         *
         * - Clicked face
         * - Floor beneath the destination
         * - Ceiling above the destination
         */
        boolean forceRegularColumn =
                player != null && player.isShiftKeyDown();

        /*
         * This variable stores the exact shaft, base, or top
         * block state selected by the placement rules.
         */
        BlockState stateToPlace;


        /*
         * Rule 1:
         *
         * Holding Shift always places a regular shaft.
         */
        if (forceRegularColumn) {

            stateToPlace =
                    DungeonCraft.COLUMN.defaultBlockState();


            /*
             * Rule 2:
             *
             * Clicking any existing column piece continues the
             * structure with another regular shaft.
             */
        } else if (clickedColumnPiece) {

            stateToPlace =
                    DungeonCraft.COLUMN.defaultBlockState();


            /*
             * Rule 3:
             *
             * Clicking directly on the top face of a non-column block
             * places a Column Base.
             */
        } else if (clickedFace == Direction.UP) {

            stateToPlace =
                    DungeonCraft.COLUMN_BASE.defaultBlockState();


            /*
             * Rule 4:
             *
             * Clicking directly on the underside of a non-column block
             * places a Column Top.
             */
        } else if (clickedFace == Direction.DOWN) {

            stateToPlace =
                    DungeonCraft.COLUMN_TOP.defaultBlockState();


            /*
             * Rule 5:
             *
             * The player clicked the side of a wall or another block.
             *
             * Check the blocks immediately below and above the
             * destination where the column will appear.
             */
        } else {

            /*
             * Check whether there is a sturdy, non-column block
             * directly below the placement space.
             *
             * The block below must have a sturdy UP face pointing
             * toward the new column.
             */
            boolean hasBaseSupport =
                    hasNonColumnSupport(
                            level,
                            placePos.below(),
                            Direction.UP
                    );

            /*
             * Check whether there is a sturdy, non-column block
             * directly above the placement space.
             *
             * The block above must have a sturdy DOWN face pointing
             * toward the new column.
             */
            boolean hasTopSupport =
                    hasNonColumnSupport(
                            level,
                            placePos.above(),
                            Direction.DOWN
                    );


            /*
             * A sturdy floor exists below, but no sturdy ceiling
             * exists above.
             *
             * Place a Column Base.
             */
            if (hasBaseSupport && !hasTopSupport) {

                stateToPlace =
                        DungeonCraft.COLUMN_BASE.defaultBlockState();


                /*
                 * A sturdy ceiling exists above, but no sturdy floor
                 * exists below.
                 *
                 * Place a Column Top.
                 */
            } else if (hasTopSupport && !hasBaseSupport) {

                stateToPlace =
                        DungeonCraft.COLUMN_TOP.defaultBlockState();


                /*
                 * Place the regular shaft when:
                 *
                 * - No sturdy floor or ceiling exists; or
                 * - Both a floor and ceiling touch this one placement space.
                 *
                 * When both exist, the code avoids arbitrarily choosing
                 * a base over a top or a top over a base.
                 */
            } else {

                stateToPlace =
                        DungeonCraft.COLUMN.defaultBlockState();
            }
        }


        /*
         * Attempt to place the selected registered block.
         *
         * Update flag 3:
         *
         * - Updates the world.
         * - Notifies nearby blocks.
         * - Sends the change to the client.
         */
        boolean placed =
                level.setBlock(
                        placePos,
                        stateToPlace,
                        3
                );

        // Stop if Minecraft could not place the selected block.
        if (!placed) {
            return InteractionResult.FAIL;
        }


        /*
         * Consume one Column item unless the player is in
         * Creative mode.
         */
        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        // Tell Minecraft that the use action completed successfully.
        return InteractionResult.SUCCESS;
    }


    /*
     * isColumnBlock()
     *
     * Checks whether a supplied block state belongs to either
     * column family.
     *
     * Regular Column family:
     *
     * - COLUMN
     * - COLUMN_BASE
     * - COLUMN_TOP
     *
     * Flanked Column family:
     *
     * - FLANKED_COLUMN
     *
     * The Flanked Column uses one registered block and stores
     * shaft, base, or top in its PART block-state property.
     */
    private static boolean isColumnBlock(BlockState state) {
        return state.is(DungeonCraft.COLUMN)
                || state.is(DungeonCraft.COLUMN_BASE)
                || state.is(DungeonCraft.COLUMN_TOP)
                || state.is(DungeonCraft.FLANKED_COLUMN);
    }


    /*
     * hasNonColumnSupport()
     *
     * Checks whether a block directly above or below the intended
     * placement location should trigger a base or top.
     *
     * Requirements:
     *
     * 1. The support block cannot belong to either column family.
     * 2. The support block must have a sturdy face pointing toward
     *    the new column.
     *
     * For a block below the destination:
     *
     * - Check its UP face.
     *
     * For a block above the destination:
     *
     * - Check its DOWN face.
     */
    private static boolean hasNonColumnSupport(
            Level level,
            BlockPos supportPos,
            Direction faceTowardColumn
    ) {

        // Read the block located at the support position.
        BlockState supportState =
                level.getBlockState(supportPos);

        /*
         * Existing regular or Flanked Column pieces do not count
         * as floor or ceiling support.
         */
        if (isColumnBlock(supportState)) {
            return false;
        }

        /*
         * Return true only if the support block's relevant face
         * is sturdy.
         */
        return supportState.isFaceSturdy(
                level,
                supportPos,
                faceTowardColumn
        );
    }
}