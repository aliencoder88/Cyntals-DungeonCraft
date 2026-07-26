package com.dungeoncraft.item;

// Base Minecraft item class.
import net.minecraft.world.item.Item;

/*
 * EngravingToolItem
 *
 * This is the tool that will eventually let players engrave text
 * onto Rusted Metal Signs.
 *
 * Phase 1:
 * - exists as a registered item;
 * - appears in the DungeonCraft creative tab;
 * - has no special behavior yet.
 *
 * Later:
 * - right-clicking a blank Rusted Metal Sign with this tool
 *   will open the engraving/text screen;
 * - the tool may lose durability when text is confirmed.
 */
public class EngravingToolItem extends Item {

    /*
     * Constructor.
     *
     * DungeonCraft.java creates this item and passes in the item properties.
     */
    public EngravingToolItem(
            Item.Properties properties
    ) {
        // Sends the item properties to the base Item class.
        super(
                properties
        );
    }
}