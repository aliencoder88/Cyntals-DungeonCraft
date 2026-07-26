package com.dungeoncraft.block.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/*
 * Supplies the ordinary Container methods for block entities
 * that store their items in a NonNullList.
 */
public interface ImplementedContainer
        extends Container {

    /*
     * Each implementing block entity must return the same
     * item-list instance every time.
     */
    NonNullList<ItemStack> getItems();

    @Override
    default int getContainerSize() {
        return this.getItems().size();
    }

    @Override
    default boolean isEmpty() {
        for (ItemStack stack : this.getItems()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    default ItemStack getItem(
            int slot
    ) {
        return this.getItems().get(slot);
    }

    @Override
    default ItemStack removeItem(
            int slot,
            int amount
    ) {
        ItemStack removedStack =
                ContainerHelper.removeItem(
                        this.getItems(),
                        slot,
                        amount
                );

        if (!removedStack.isEmpty()) {
            this.setChanged();
        }

        return removedStack;
    }

    @Override
    default ItemStack removeItemNoUpdate(
            int slot
    ) {
        return ContainerHelper.takeItem(
                this.getItems(),
                slot
        );
    }

    @Override
    default void setItem(
            int slot,
            ItemStack stack
    ) {
        this.getItems().set(
                slot,
                stack
        );

        stack.limitSize(
                this.getMaxStackSize(stack)
        );

        this.setChanged();
    }

    @Override
    default void clearContent() {
        this.getItems().clear();
        this.setChanged();
    }
}