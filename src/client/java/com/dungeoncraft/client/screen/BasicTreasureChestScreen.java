package com.dungeoncraft.client.screen;

import com.dungeoncraft.menu.BasicTreasureChestMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/*
 * Draws the Basic Treasure Chest inventory screen.
 *
 * The menu controls the slots and item movement.
 * This class controls the visual appearance.
 */
public class BasicTreasureChestScreen
        extends AbstractContainerScreen<
        BasicTreasureChestMenu
        > {

    /*
     * Vanilla texture used by a normal 3-row chest.
     *
     * We are borrowing this only for the first functional
     * checkpoint. It can later be replaced by a custom
     * DungeonCraft texture.
     */
    private static final Identifier
            CONTAINER_TEXTURE =
            Identifier.withDefaultNamespace(
                    "textures/gui/container/generic_54.png"
            );

    /*
     * Dimensions of the complete source texture.
     */
    private static final int
            BACKGROUND_TEXTURE_WIDTH = 256;

    private static final int
            BACKGROUND_TEXTURE_HEIGHT = 256;

    public BasicTreasureChestScreen(
            BasicTreasureChestMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(
                menu,
                playerInventory,
                title
        );

        /*
         * Center the chest title.
         */
        this.titleLabelX =
                (
                        this.imageWidth
                                - this.font.width(
                                this.title
                        )
                ) / 2;

        /*
         * Position the player inventory label above
         * the player's inventory slots.
         */
        this.inventoryLabelY = 74;
    }

    /*
     * Draws the screen's background texture.
     */
    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractBackground(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        /*
         * Three chest rows:
         *
         * 17 pixels above the slots
         * + 3 rows × 18 pixels
         */
        int chestSectionHeight =
                17 + 3 * 18;

        /*
         * Draw the title area and the three chest rows
         * from the top of generic_54.png.
         */
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CONTAINER_TEXTURE,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                chestSectionHeight,
                BACKGROUND_TEXTURE_WIDTH,
                BACKGROUND_TEXTURE_HEIGHT
        );

        /*
         * Draw the player inventory and hotbar section.
         *
         * In generic_54.png, that section begins at V = 126.
         */
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CONTAINER_TEXTURE,
                this.leftPos,
                this.topPos + chestSectionHeight,
                0.0F,
                126.0F,
                this.imageWidth,
                96,
                BACKGROUND_TEXTURE_WIDTH,
                BACKGROUND_TEXTURE_HEIGHT
        );
    }
}