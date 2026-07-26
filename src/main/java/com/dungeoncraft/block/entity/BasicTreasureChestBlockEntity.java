package com.dungeoncraft.block.entity;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.menu.BasicTreasureChestMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import org.jetbrains.annotations.Nullable;

/*
 * Stores the Basic Treasure Chest's world data and animation state.
 */
public class BasicTreasureChestBlockEntity
        extends BlockEntity
        implements ImplementedContainer, MenuProvider {

    /*
     * A normal single chest contains 3 rows of 9 slots.
     */
    public static final int CONTAINER_SIZE =
            3 * 9;

    /*
     * The actual stacks stored inside this chest.
     */
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(
                    CONTAINER_SIZE,
                    ItemStack.EMPTY
            );

    /*
     * Lid position from the previous game tick.
     *
     * This lets the renderer smoothly blend between ticks.
     */
    private float previousLidProgress = 0.0F;

    /*
     * The position the lid is trying to reach.
     *
     * false = closed
     * true  = open
     */
    private boolean lidOpen = false;

    /*
     * Number of players currently using this chest.
     *
     * This prepares the chest for multiplayer, where more than
     * one player could have the same inventory open.
     */
    private int openPlayerCount = 0;

    /*
     * Current lid position.
     *
     * 0.0F = fully closed
     * 1.0F = fully open
     */
    private float lidProgress = 0.0F;

    public BasicTreasureChestBlockEntity(
            BlockPos blockPos,
            BlockState blockState
    ) {
        super(
                DungeonCraft.BASIC_TREASURE_CHEST_BLOCK_ENTITY,
                blockPos,
                blockState
        );
    }

    /*
     * Returns a smoothly interpolated lid position.
     *
     * tickProgress describes how far the rendered frame is
     * between the previous tick and the current tick.
     */
    public float getLidProgress(
            float tickProgress
    ) {
        return Mth.lerp(
                tickProgress,
                this.previousLidProgress,
                this.lidProgress
        );
    }

    /*
     * Gives the block entity a new lid position.
     *
     * We keep the old value first so the renderer can blend
     * smoothly from the old position to the new one.
     */
    public void setLidProgress(
            float newLidProgress
    ) {
        this.previousLidProgress =
                this.lidProgress;

        this.lidProgress =
                Mth.clamp(
                        newLidProgress,
                        0.0F,
                        1.0F
                );
    }

    /*
     * Moves the lid toward its requested position.
     */
    public static void tick(
            Level level,
            BlockPos blockPos,
            BlockState blockState,
            BasicTreasureChestBlockEntity chest
    ) {
        /*
         * The animation values used by the renderer are maintained
         * on the client for this interaction test.
         */
        if (!level.isClientSide()) {
            return;
        }

        /*
         * Retain the previous tick's position so the renderer can
         * interpolate smoothly between ticks.
         */
        chest.previousLidProgress =
                chest.lidProgress;

        float animationSpeed = 0.1F;

        if (chest.lidOpen) {
            chest.lidProgress =
                    Math.min(
                            chest.lidProgress + animationSpeed,
                            1.0F
                    );
        } else {
            chest.lidProgress =
                    Math.max(
                            chest.lidProgress - animationSpeed,
                            0.0F
                    );
        }
    }

    /*
     * Records that a player has opened this specific chest.
     */
    public void playerStartedUsing(
            Player player
    ) {
        /*
         * Ignore spectators because they should not affect
         * the physical lid state.
         */
        if (player.isSpectator()) {
            return;
        }

        this.openPlayerCount++;

        this.lidOpen =
                this.openPlayerCount > 0;


        if (this.level != null) {
            this.level.blockEvent(
                    this.worldPosition,
                    this.getBlockState().getBlock(),
                    1,
                    this.openPlayerCount
            );
        }
    }

    /*
     * Records that a player has closed this specific chest.
     */
    public void playerStoppedUsing(
            Player player
    ) {
        if (player.isSpectator()) {
            return;
        }

        int previousOpenPlayerCount =
                this.openPlayerCount;

        this.openPlayerCount =
                Math.max(
                        this.openPlayerCount - 1,
                        0
                );

        this.lidOpen =
                this.openPlayerCount > 0;

        /*
         * Play the closing sound only when the final player
         * stops using the chest.
         */
        if (
                previousOpenPlayerCount > 0
                        && this.openPlayerCount == 0
                        && this.level != null
        ) {
            this.level.playSound(
                    null,
                    this.worldPosition,
                    SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS,
                    0.5F,
                    1.0F
            );
        }

        if (this.level != null) {
            this.level.blockEvent(
                    this.worldPosition,
                    this.getBlockState().getBlock(),
                    1,
                    this.openPlayerCount
            );
        }
    }

    /*
     * Updates the client-side lid target after the block
     * receives an opener-count event.
     */
    public void setOpenPlayerCount(
            int newOpenPlayerCount
    ) {
        int previousOpenPlayerCount =
                this.openPlayerCount;

        this.openPlayerCount =
                Math.max(
                        newOpenPlayerCount,
                        0
                );

        this.lidOpen =
                this.openPlayerCount > 0;

        /*
         * The block event reaches both sides. Only play this
         * locally on the client to avoid duplicate sounds.
         */
        if (
                this.level != null
                        && this.level.isClientSide()
                        && previousOpenPlayerCount == 0
                        && this.openPlayerCount > 0
        ) {
            this.level.playLocalSound(
                    this.worldPosition,
                    SoundEvents.CHEST_OPEN,
                    SoundSource.BLOCKS,
                    0.5F,
                    1.0F,
                    false
            );
        }
    }

    /*
     * Marks the inventory as changed and tells nearby
     * comparators to recalculate their output.
     */
    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level != null) {
            this.level.updateNeighbourForOutputSignal(
                    this.worldPosition,
                    this.getBlockState().getBlock()
            );
        }
    }

    /*
     * Loads the saved chest contents when the world or chunk loads.
     */
    @Override
    protected void loadAdditional(
            ValueInput input
    ) {
        super.loadAdditional(input);

        ContainerHelper.loadAllItems(
                input,
                this.items
        );
    }

    /*
     * Writes the chest contents into the world save.
     */
    @Override
    protected void saveAdditional(
            ValueOutput output
    ) {
        ContainerHelper.saveAllItems(
                output,
                this.items
        );

        super.saveAdditional(output);
    }

    /*
     * Gives the ImplementedContainer helper access to this
     * chest's actual inventory list.
     */
    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

        /*
     * Supplies the title displayed at the top of the chest screen.
     *
     * This uses the same translation key as the chest block.
     */

    /*
     * Creates the server-side menu connected to this chest's
     * real 27-slot inventory.
     */
    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new BasicTreasureChestMenu(
                containerId,
                playerInventory,
                this
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.dungeoncraft.basic_treasure_chest"
        );
    }

    /*
     * Closes the inventory if the player is too far away
     * or if this block entity is no longer present.
     */
    @Override
    public boolean stillValid(
            Player player
    ) {
        return Container.stillValidBlockEntity(
                this,
                player
        );
    }
}