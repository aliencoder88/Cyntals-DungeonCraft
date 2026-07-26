package com.dungeoncraft.menu;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.entity.BasicTreasureChestBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/*
 * Connects the Basic Treasure Chest's 27 slots
 * to the player's inventory.
 */
public class BasicTreasureChestMenu
        extends AbstractContainerMenu {

    /*
     * The chest uses 3 rows of 9 slots.
     */
    private static final int CHEST_ROWS = 3;
    private static final int CHEST_COLUMNS = 9;
    private static final int CHEST_SLOT_COUNT =
            CHEST_ROWS * CHEST_COLUMNS;

    /*
     * Menu slot-index ranges.
     *
     * Chest slots:
     * 0 through 26
     *
     * Player inventory and hotbar:
     * 27 through 62
     */
    private static final int CHEST_START = 0;
    private static final int CHEST_END =
            CHEST_SLOT_COUNT;

    private static final int PLAYER_INVENTORY_START =
            CHEST_END;

    private static final int PLAYER_INVENTORY_END =
            PLAYER_INVENTORY_START
                    + Inventory.INVENTORY_SIZE;

    /*
     * GUI positions measured in screen pixels.
     */
    private static final int CHEST_START_X = 8;
    private static final int CHEST_START_Y = 18;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 85;

    /*
     * The real server-side chest container, or an empty
     * synced client-side placeholder.
     */
    private final Container container;

    /*
     * Client-side constructor.
     *
     * The client initially creates an empty container.
     * Minecraft then synchronizes the real server contents
     * into the menu slots.
     */
    public BasicTreasureChestMenu(
            int containerId,
            Inventory playerInventory
    ) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(
                        CHEST_SLOT_COUNT
                )
        );
    }

    /*
     * Server-side constructor.
     *
     * The block entity passes itself as the real Container.
     */
    public BasicTreasureChestMenu(
            int containerId,
            Inventory playerInventory,
            Container container
    ) {
        super(
                DungeonCraft.BASIC_TREASURE_CHEST_MENU,
                containerId
        );

        checkContainerSize(
                container,
                CHEST_SLOT_COUNT
        );

        this.container = container;

        /*
         * Only the real server-side block entity controls
         * the physical lid.
         *
         * The client-side SimpleContainer does nothing here.
         */
        if (
                this.container
                        instanceof BasicTreasureChestBlockEntity chest
        ) {
            chest.playerStartedUsing(
                    playerInventory.player
            );
        }

        /*
         * Add the chest's 27 slots.
         */
        this.addChestSlots();

        /*
         * Add the player's inventory and hotbar.
         */
        this.addStandardInventorySlots(
                playerInventory,
                PLAYER_INVENTORY_X,
                PLAYER_INVENTORY_Y
        );
    }

    /*
     * Adds three rows of nine chest slots.
     */
    private void addChestSlots() {
        for (
                int row = 0;
                row < CHEST_ROWS;
                row++
        ) {
            for (
                    int column = 0;
                    column < CHEST_COLUMNS;
                    column++
            ) {
                int slotIndex =
                        column
                                + row
                                * CHEST_COLUMNS;

                int slotX =
                        CHEST_START_X
                                + column
                                * SLOT_SIZE;

                int slotY =
                        CHEST_START_Y
                                + row
                                * SLOT_SIZE;

                this.addSlot(
                        new Slot(
                                this.container,
                                slotIndex,
                                slotX,
                                slotY
                        )
                );
            }
        }
    }

    /*
     * Handles shift-clicking between the chest and
     * the player's inventory.
     */
    @Override
    public ItemStack quickMoveStack(
            Player player,
            int slotIndex
    ) {
        Slot slot =
                this.slots.get(
                        slotIndex
                );

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack originalStack =
                slot.getItem();

        ItemStack copiedStack =
                originalStack.copy();

        /*
         * Shift-clicking a chest slot moves the stack
         * into the player inventory.
         */
        if (slotIndex < CHEST_END) {
            if (!this.moveItemStackTo(
                    originalStack,
                    PLAYER_INVENTORY_START,
                    PLAYER_INVENTORY_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            /*
             * Shift-clicking a player inventory slot moves
             * the stack into the chest.
             */
            if (!this.moveItemStackTo(
                    originalStack,
                    CHEST_START,
                    CHEST_END,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }

        /*
         * Clear the source slot if the entire stack moved.
         * Otherwise, notify the slot that its count changed.
         */
        if (originalStack.isEmpty()) {
            slot.setByPlayer(
                    ItemStack.EMPTY
            );
        } else {
            slot.setChanged();
        }

        return copiedStack;
    }

    /*
     * Continually checks whether the player may keep
     * using this chest.
     */
    @Override
    public boolean stillValid(
            Player player
    ) {
        return this.container.stillValid(
                player
        );
    }

    /*
     * Notify the container when the menu closes.
     */
    @Override
    public void removed(
            Player player
    ) {
        super.removed(
                player
        );

        if (
                this.container
                        instanceof BasicTreasureChestBlockEntity chest
        ) {
            chest.playerStoppedUsing(
                    player
            );
        }
    }
}