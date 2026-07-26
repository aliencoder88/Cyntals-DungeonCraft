package com.dungeoncraft.item;

import net.minecraft.world.item.Item;

/**
 * Opens a supported device's configuration screen.
 *
 * The device block validates the exact interaction point. For example, the
 * Hidden Block Lever only accepts this tool on its exposed pull bar while the
 * panel is fully open.
 */
public class CodingToolItem extends Item {
    public CodingToolItem(Properties properties) {
        super(properties);
    }
}
