package com.dungeoncraft.item;

// Base class for an item that places a block.
import net.minecraft.world.item.BlockItem;
// Base item settings class.
import net.minecraft.world.item.Item;
// The block this item places.
import net.minecraft.world.level.block.Block;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/*
 * RustedMetalSignItem
 *
 * This is the inventory item for the Rusted Metal Sign.
 *
 * Phase 1:
 * - behaves like a normal BlockItem;
 * - places the Rusted Metal Sign block.
 *
 * Later:
 * - will carry saved engraved text;
 * - will carry remaining surface uses;
 * - will behave like a blank map vs drawn map:
 *   blank signs stack, engraved signs keep unique data.
 */
public class RustedMetalSignItem extends BlockItem {

    private static final String STACK_MARKER_KEY = "DungeonCraftRustedMetalSign";
    private static final String STACK_LINE_1_KEY = "Line1";
    private static final String STACK_LINE_2_KEY = "Line2";
    private static final String STACK_LINE_3_KEY = "Line3";
    private static final String STACK_LINE_4_KEY = "Line4";

    /*
     * Surface/durability keys.
     *
     * Keep these here for later, but surface tooltip display is hidden for now.
     */
    // private static final String STACK_SURFACE_USES_KEY = "SurfaceUses";
    // private static final String STACK_BROKEN_PLATE_KEY = "BrokenPlate";

    /*
     * Constructor.
     *
     * DungeonCraft.java creates this item and passes in:
     *
     * - the block it places;
     * - the item properties.
     */
    public RustedMetalSignItem(
            Block block,
            Item.Properties properties
    ) {
        // Sends the block and properties to the normal BlockItem constructor.
        super(
                block,
                properties
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(
                stack,
                context,
                tooltipDisplay,
                tooltip,
                flag
        );

        CustomData customData =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        if (customData == null) {
            return;
        }

        CompoundTag tag =
                customData.copyTag();

        if (!tag.getBooleanOr(STACK_MARKER_KEY, false)) {
            return;
        }

        String line1 =
                tag.getStringOr(
                        STACK_LINE_1_KEY,
                        ""
                );

        String line2 =
                tag.getStringOr(
                        STACK_LINE_2_KEY,
                        ""
                );

        String line3 =
                tag.getStringOr(
                        STACK_LINE_3_KEY,
                        ""
                );

        String line4 =
                tag.getStringOr(
                        STACK_LINE_4_KEY,
                        ""
                );

        if (
                line1.isEmpty()
                        && line2.isEmpty()
                        && line3.isEmpty()
                        && line4.isEmpty()
        ) {
            return;
        }

        tooltip.accept(
                Component.literal("Engraved Text:")
                        .withStyle(ChatFormatting.GRAY)
        );

        addEngravedLineToTooltip(
                tooltip,
                line1
        );

        addEngravedLineToTooltip(
                tooltip,
                line2
        );

        addEngravedLineToTooltip(
                tooltip,
                line3
        );

        addEngravedLineToTooltip(
                tooltip,
                line4
        );

        /*
         * Surface / durability tooltip.
         *
         * Hidden for now until the sign durability system is ready.
         */
    /*
    int surfaceUses =
            tag.getIntOr(
                    STACK_SURFACE_USES_KEY,
                    3
            );

    boolean brokenPlate =
            tag.getBooleanOr(
                    STACK_BROKEN_PLATE_KEY,
                    false
            );

    tooltip.accept(
            Component.literal(
                            brokenPlate
                                    ? "Surface: Broken"
                                    : "Surface Uses: " + surfaceUses
                    )
                    .withStyle(ChatFormatting.DARK_GRAY)
    );
    */
    }

    private static void addEngravedLineToTooltip(
            Consumer<Component> tooltip,
            String line
    ) {
        if (line == null || line.isEmpty()) {
            return;
        }

        tooltip.accept(
                Component.literal("  " + line)
                        .withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}