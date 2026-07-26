package com.dungeoncraft.block.entity;

// Needed so this block entity can refer to your main DungeonCraft registration class.
import com.dungeoncraft.DungeonCraft;

// Stores the block position in the world.
import net.minecraft.core.BlockPos;

// Newer Minecraft save-data reader.
import net.minecraft.world.level.storage.ValueInput;

// Newer Minecraft save-data writer.
import net.minecraft.world.level.storage.ValueOutput;

// Gives access to the block state this block entity belongs to.
import net.minecraft.world.level.block.state.BlockState;

// Base class for blocks that store extra data.
import net.minecraft.world.level.block.entity.BlockEntity;
import com.dungeoncraft.block.RustedMetalSignBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/*
 * RustedMetalSignBlockEntity
 *
 * This stores the special data for a placed Rusted Metal Sign.
 *
 * Normal blocks can only store simple blockstate values.
 * This sign needs more information:
 *
 * - four lines of engraved text;
 * - whether the text is locked in;
 * - how many times the surface can still be sanded clean;
 * - whether the center plate has broken from too much wear.
 */
public class RustedMetalSignBlockEntity extends BlockEntity {

    /*
     * Returns true if this sign currently has any engraved text.
     */
    public boolean hasText() {
        return !this.line1.isEmpty()
                || !this.line2.isEmpty()
                || !this.line3.isEmpty()
                || !this.line4.isEmpty();
    }

    /*
     * Temporary engraving method.
     *
     * This writes one test inscription only if the sign is blank.
     *
     * Later, the Engraving Tool will open a real text-entry screen.
     */
    public boolean engraveTemporaryDefaultText() {
        /*
         * Do not overwrite existing text.
         *
         * Later, changing existing text will require the workbench/file system.
         */
        if (this.hasText()) {
            return false;
        }

        this.setText(
                "Arm Yourself",
                "Fight Khuul",
                "Khuum",
                "Save Stonekeep"
        );

        return true;
    }

    /*
     * ItemStack data keys.
     *
     * These are used when a Rusted Metal Sign is broken and picked up.
     * The dropped item needs to remember its engraved text and plate condition.
     */
    private static final String STACK_MARKER_KEY = "DungeonCraftRustedMetalSign";
    private static final String STACK_LINE_1_KEY = "Line1";
    private static final String STACK_LINE_2_KEY = "Line2";
    private static final String STACK_LINE_3_KEY = "Line3";
    private static final String STACK_LINE_4_KEY = "Line4";
    private static final String STACK_SURFACE_USES_KEY = "SurfaceUses";
    private static final String STACK_LOCKED_KEY = "Locked";
    private static final String STACK_BROKEN_PLATE_KEY = "BrokenPlate";

    /*
     * Saves this sign's data into an ItemStack.
     *
     * This will be used when the placed sign is broken.
     * The dropped item can then remember:
     * - engraved text;
     * - remaining surface uses;
     * - locked state;
     * - broken plate state.
     */
    public void saveToItemStack(
            ItemStack stack
    ) {
        /*
         * CompoundTag is a small data container.
         *
         * Here we are creating custom item data for this specific dropped sign.
         */
        CompoundTag tag =
                new CompoundTag();

        /*
         * Marker value.
         *
         * This lets us later check whether an item stack actually contains
         * Rusted Metal Sign data.
         */
        tag.putBoolean(
                STACK_MARKER_KEY,
                true
        );

        /*
         * Save the four engraved text lines.
         */
        tag.putString(
                STACK_LINE_1_KEY,
                this.line1
        );

        tag.putString(
                STACK_LINE_2_KEY,
                this.line2
        );

        tag.putString(
                STACK_LINE_3_KEY,
                this.line3
        );

        tag.putString(
                STACK_LINE_4_KEY,
                this.line4
        );

        /*
         * Save the plate/surface condition.
         */
        tag.putInt(
                STACK_SURFACE_USES_KEY,
                this.surfaceUses
        );

        tag.putBoolean(
                STACK_LOCKED_KEY,
                this.locked
        );

        tag.putBoolean(
                STACK_BROKEN_PLATE_KEY,
                this.brokenPlate
        );

        /*
         * Attach the custom data to the item stack.
         */
        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(
                        tag
                )
        );
    }

    /*
     * Loads Rusted Metal Sign data from an ItemStack.
     *
     * This will be used when a saved sign item is placed back into the world.
     */
    public void loadFromItemStack(
            ItemStack stack
    ) {
        /*
         * Read custom item data from the stack.
         */
        CustomData customData =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        /*
         * If the item has no custom data, it is just a blank sign.
         */
        if (customData == null) {
            return;
        }

        /*
         * Copy the custom data into a tag we can read.
         */
        CompoundTag tag =
                customData.copyTag();

        /*
         * If this marker is missing, the custom data is not ours.
         */
        if (
                !tag.getBooleanOr(
                        STACK_MARKER_KEY,
                        false
                )
        ) {
            return;
        }

        /*
         * Restore text.
         */
        this.line1 =
                tag.getStringOr(
                        STACK_LINE_1_KEY,
                        ""
                );

        this.line2 =
                tag.getStringOr(
                        STACK_LINE_2_KEY,
                        ""
                );

        this.line3 =
                tag.getStringOr(
                        STACK_LINE_3_KEY,
                        ""
                );

        this.line4 =
                tag.getStringOr(
                        STACK_LINE_4_KEY,
                        ""
                );

        /*
         * Restore surface condition.
         */
        this.surfaceUses =
                tag.getIntOr(
                        STACK_SURFACE_USES_KEY,
                        3
                );

        this.locked =
                tag.getBooleanOr(
                        STACK_LOCKED_KEY,
                        false
                );

        this.brokenPlate =
                tag.getBooleanOr(
                        STACK_BROKEN_PLATE_KEY,
                        false
                );

        /*
         * Sync both the saved block entity data and the visible plate condition.
         */
        this.markChangedAndSync();
        this.syncVisibleSurfaceState();
    }

    // First engraved text line.
    private String line1 = "";

    // Second engraved text line.
    private String line2 = "";

    // Third engraved text line.
    private String line3 = "";

    // Fourth engraved text line.
    private String line4 = "";

    // How many times the surface can still be sanded clean.
    // New signs start with 3 remaining surface uses.
    private int surfaceUses = 3;

    // True once text has been engraved and locked in.
    private boolean locked = false;

    // True if the center plate has broken from being sanded too many times.
    private boolean brokenPlate = false;

    /*
     * Constructor.
     *
     * Minecraft calls this when creating the block entity for the placed sign.
     */
    public RustedMetalSignBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                DungeonCraft.RUSTED_METAL_SIGN_BLOCK_ENTITY,
                pos,
                state
        );
    }

    /*
     * Returns the first text line.
     */
    public String getLine1() {
        return line1;
    }

    /*
     * Returns the second text line.
     */
    public String getLine2() {
        return line2;
    }

    /*
     * Returns the third text line.
     */
    public String getLine3() {
        return line3;
    }

    /*
     * Returns the fourth text line.
     */
    public String getLine4() {
        return line4;
    }

    /*
     * Returns how many sanding/resurfacing uses remain.
     */
    public int getSurfaceUses() {
        return surfaceUses;
    }

    /*
     * Returns whether the sign is currently locked/written.
     */
    public boolean isLocked() {
        return locked;
    }

    /*
     * Returns whether the center metal plate is broken.
     */
    public boolean hasBrokenPlate() {
        return brokenPlate;
    }

    /*
     * Sets all four text lines.
     *
     * This will later be called after the player uses the Engraving Tool
     * and confirms the text entry screen.
     */
    public void setText(
            String line1,
            String line2,
            String line3,
            String line4
    ) {
        // Stores the first engraved line.
        this.line1 = line1;

        // Stores the second engraved line.
        this.line2 = line2;

        // Stores the third engraved line.
        this.line3 = line3;

        // Stores the fourth engraved line.
        this.line4 = line4;

        // Once text is engraved, the sign becomes locked.
        this.locked = hasText();

        // Tell Minecraft this block entity data changed and needs saving.
        markChangedAndSync();
        syncVisibleSurfaceState();
    }

    /*
     * Clears the engraved text and spends one surface use.
     *
     * This will later be called by the sanding/file tool.
     */
    public void sandClean() {

        // If the center plate is already broken, sanding cannot help.
        if (brokenPlate) {
            return;
        }

        // If there are surface uses left, clear the sign and spend one.
        if (surfaceUses > 0) {

            // Clears the first text line.
            line1 = "";

            // Clears the second text line.
            line2 = "";

            // Clears the third text line.
            line3 = "";

            // Clears the fourth text line.
            line4 = "";

            // Makes the sign editable again.
            locked = false;

            // Spends one remaining clean surface use.
            surfaceUses--;

            // Marks the block entity as changed so Minecraft saves it.
            setChanged();
            syncVisibleSurfaceState();

            // Stops here because sanding succeeded.
            return;
        }

        // If the player sands after all surface uses are gone,
        // the center plate breaks.
        brokenPlate = true;

        // Broken plate destroys the first text line.
        line1 = "";

        // Broken plate destroys the second text line.
        line2 = "";

        // Broken plate destroys the third text line.
        line3 = "";

        // Broken plate destroys the fourth text line.
        line4 = "";

        // Keep it locked because a broken plate cannot be engraved again.
        locked = true;

        // Marks the block entity as changed so Minecraft saves it.
        setChanged();
        syncVisibleSurfaceState();
    }

    /*
     * Updates the visible blockstate surface value so the model/texture
     * matches the data stored in this block entity.
     *
     * The block entity stores the real information.
     * The blockstate stores the visual surface level Minecraft's model system can read.
     */
    private void syncVisibleSurfaceState() {
        /*
         * Do nothing if this block entity is not currently in a world.
         *
         * level can be null while the block entity is being created,
         * loaded, or handled outside of an active world.
         */
        if (this.level == null) {
            return;
        }

        /*
         * Start with the clean surface.
         *
         * 0 = clean
         * 1 = scuffed
         * 2 = worn
         * 3 = fragile
         * 4 = broken
         */
        int visibleSurface = 0;

        /*
         * A broken plate always overrides the normal surfaceUses value.
         */
        if (this.brokenPlate) {
            visibleSurface = 4;
        } else if (this.surfaceUses >= 3) {
            visibleSurface = 0;
        } else if (this.surfaceUses == 2) {
            visibleSurface = 1;
        } else if (this.surfaceUses == 1) {
            visibleSurface = 2;
        } else {
            visibleSurface = 3;
        }

        /*
         * Gets the current blockstate at this block entity's position.
         */
        BlockState currentState =
                this.level.getBlockState(this.worldPosition);

        /*
         * Only update the world if this block is actually a Rusted Metal Sign.
         *
         * This avoids errors if something strange happens while loading/unloading.
         */
        if (!(currentState.getBlock() instanceof RustedMetalSignBlock)) {
            return;
        }

        /*
         * If the visible surface already matches, do not update the block again.
         *
         * This prevents unnecessary block updates.
         */
        if (currentState.getValue(RustedMetalSignBlock.SURFACE) == visibleSurface) {
            return;
        }

        /*
         * Updates only the SURFACE property.
         *
         * The FACING property stays unchanged.
         */
        this.level.setBlock(
                this.worldPosition,
                currentState.setValue(
                        RustedMetalSignBlock.SURFACE,
                        visibleSurface
                ),
                3
        );
    }

    /*
     * Temporary test method.
     *
     * This cycles the Rusted Metal Sign through each visible surface state:
     *
     * 0 = clean
     * 1 = scuffed
     * 2 = worn
     * 3 = fragile
     * 4 = broken
     *
     * Later, this will be replaced by real sanding / filing behavior.
     */
    public void cycleSurfaceForTesting() {
        /*
         * Do nothing if this block entity is not currently in a world.
         */
        if (this.level == null) {
            return;
        }

        /*
         * Gets the current blockstate at this sign's position.
         */
        BlockState currentState =
                this.level.getBlockState(
                        this.worldPosition
                );

        /*
         * Make sure this block entity is still attached to a Rusted Metal Sign.
         */
        if (!(currentState.getBlock() instanceof RustedMetalSignBlock)) {
            return;
        }

        /*
         * Read the current visible surface state from the blockstate.
         */
        int currentSurface =
                currentState.getValue(
                        RustedMetalSignBlock.SURFACE
                );

        /*
         * Move to the next surface state.
         *
         * After broken, loop back to clean for testing.
         */
        int nextSurface =
                currentSurface + 1;

        if (nextSurface > 4) {
            nextSurface = 0;
        }

        /*
         * Update the saved block entity data so it roughly matches
         * the visible state we are testing.
         */
        if (nextSurface == 0) {
            this.surfaceUses = 3;
            this.brokenPlate = false;
            this.locked = false;
        } else if (nextSurface == 1) {
            this.surfaceUses = 2;
            this.brokenPlate = false;
        } else if (nextSurface == 2) {
            this.surfaceUses = 1;
            this.brokenPlate = false;
        } else if (nextSurface == 3) {
            this.surfaceUses = 0;
            this.brokenPlate = false;
        } else {
            this.surfaceUses = 0;
            this.brokenPlate = true;
            this.locked = this.hasText();
        }

        /*
         * Mark the block entity as changed so Minecraft knows to save it.
         */
        this.setChanged();

        /*
         * Update the visible blockstate.
         *
         * This changes only SURFACE.
         * FACING stays the same.
         */
        this.level.setBlock(
                this.worldPosition,
                currentState.setValue(
                        RustedMetalSignBlock.SURFACE,
                        nextSurface
                ),
                3
        );
    }

    /*
     * Creates the block entity update packet.
     *
     * This packet lets the server send this sign's saved data to the client.
     * The client-side renderer needs that data so it can draw Line1-Line4.
     */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /*
     * Creates the block entity update tag.
     *
     * This tells Minecraft which block entity data should be sent to the client
     * when the chunk loads or when an update packet is sent.
     */
    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registryLookup
    ) {
        return this.saveWithoutMetadata(
                registryLookup
        );
    }

    /*
     * Marks this block entity as changed and asks Minecraft to sync
     * the updated block entity data to the client.
     *
     * This is important for the Rusted Metal Sign renderer because the
     * stored text lives in the block entity, but the visible text is drawn
     * on the client side.
     */
    private void markChangedAndSync() {
        /*
         * Mark the block entity dirty so Minecraft knows it needs to be saved.
         */
        this.setChanged();

        /*
         * If the block entity is not currently in a level, there is nothing
         * to sync yet.
         */
        if (this.level == null) {
            return;
        }

        /*
         * Get the current blockstate at this sign's position.
         */
        BlockState currentState =
                this.level.getBlockState(
                        this.worldPosition
                );

        /*
         * Tell Minecraft that this block position changed.
         *
         * The old and new blockstates are the same because the text changed
         * inside the block entity, not in the blockstate itself.
         */
        this.level.sendBlockUpdated(
                this.worldPosition,
                currentState,
                currentState,
                3
        );
    }

    /*
     * Saves this block entity's custom data.
     *
     * This lets the sign remember its text and condition when the world saves.
     *
     * Your current Minecraft version uses ValueOutput here.
     */
    @Override
    protected void saveAdditional(
            ValueOutput output
    ) {
        // Lets the base BlockEntity save its normal data first.
        super.saveAdditional(output);

        // Saves the first engraved text line.
        output.putString(
                "Line1",
                line1
        );

        // Saves the second engraved text line.
        output.putString(
                "Line2",
                line2
        );

        // Saves the third engraved text line.
        output.putString(
                "Line3",
                line3
        );

        // Saves the fourth engraved text line.
        output.putString(
                "Line4",
                line4
        );

        // Saves how many sanding uses remain.
        output.putInt(
                "SurfaceUses",
                surfaceUses
        );

        // Saves whether the sign is locked/written.
        output.putBoolean(
                "Locked",
                locked
        );

        // Saves whether the center plate is broken.
        output.putBoolean(
                "BrokenPlate",
                brokenPlate
        );
    }

    /*
     * Temporary engraving test.
     *
     * This lets the Engraving Tool change the sign's stored text
     * before we build the real editing screen.
     *
     * Each use cycles to the next test inscription.
     */
    public String cycleTemporaryInscription() {
        /*
         * If the sign is blank, add the first test message.
         */
        if (
                this.line1.isEmpty()
                        && this.line2.isEmpty()
                        && this.line3.isEmpty()
                        && this.line4.isEmpty()
        ) {
            this.setText(
                    "Arm Yourself",
                    "Fight Khuul",
                    "Khuum",
                    "Save Stonekeep"
            );

            return "Engraved: Arm Yourself / Fight Khuul Khuum / Save Stonekeep";
        }

        /*
         * Second test message.
         */
        if (this.line1.equals("Arm Yourself")) {
            this.setText(
                    "Beware",
                    "The Lower",
                    "Halls",
                    ""
            );

            return "Engraved: Beware / The Lower Halls";
        }

        /*
         * Third test message.
         */
        if (this.line1.equals("Beware")) {
            this.setText(
                    "The Gate",
                    "Accepts",
                    "No Cowards",
                    ""
            );

            return "Engraved: The Gate Accepts No Cowards";
        }

        /*
         * Fourth use clears the sign again.
         *
         * This is temporary. Later, clearing text will cost surface condition
         * and will happen through the proper cleanup/restoration workflow.
         */
        this.line1 = "";
        this.line2 = "";
        this.line3 = "";
        this.line4 = "";
        this.locked = false;

        markChangedAndSync();

        return "Engraving cleared.";
    }

    /*
     * Loads this block entity's custom data.
     *
     * This restores the sign after the world is reopened.
     *
     * Your current Minecraft version uses ValueInput here.
     */
    @Override
    protected void loadAdditional(
            ValueInput input
    ) {
        // Lets the base BlockEntity load its normal data first.
        super.loadAdditional(input);

        // Loads the first engraved text line.
        // If the saved value is missing, use an empty string.
        line1 = input.getStringOr(
                "Line1",
                ""
        );

        // Loads the second engraved text line.
        // If the saved value is missing, use an empty string.
        line2 = input.getStringOr(
                "Line2",
                ""
        );

        // Loads the third engraved text line.
        // If the saved value is missing, use an empty string.
        line3 = input.getStringOr(
                "Line3",
                ""
        );

        // Loads the fourth engraved text line.
        // If the saved value is missing, use an empty string.
        line4 = input.getStringOr(
                "Line4",
                ""
        );

        // Loads remaining sanding uses.
        // If missing, default to 3 for a new/old blank sign.
        surfaceUses = input.getIntOr(
                "SurfaceUses",
                3
        );

        // Loads whether the sign is locked.
        // If missing, default to false.
        locked = input.getBooleanOr(
                "Locked",
                false
        );

        // Loads whether the center plate is broken.
        // If missing, default to false.
        brokenPlate = input.getBooleanOr(
                "BrokenPlate",
                false
        );
    }
}