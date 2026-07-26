package com.dungeoncraft.item;

import net.minecraft.world.item.Item;

/*
 * MetalFileItem
 *
 * This tool will later be used to file/sand the center plate
 * of a Rusted Metal Sign.
 *
 * Planned behavior:
 * - clear engraved text;
 * - reduce the sign's remaining usable surface;
 * - eventually break the center plate if overused.
 */
public class MetalFileItem extends Item {

    /*
     * Constructor.
     *
     * DungeonCraft.java passes in the item properties when registering this item.
     */
    public MetalFileItem(
            Item.Properties properties
    ) {
        super(properties);
    }
}