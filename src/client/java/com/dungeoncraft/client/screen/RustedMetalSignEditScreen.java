package com.dungeoncraft.client.screen;

import com.dungeoncraft.network.SaveRustedMetalSignEditorPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import com.dungeoncraft.DungeonCraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/*
 * RustedMetalSignEditScreen
 *
 * Temporary editor screen for the Rusted Metal Sign.
 *
 * This version visually behaves more like a wooden sign editor:
 * - the metal plate is drawn on the screen;
 * - the text boxes are borderless;
 * - the player appears to engrave directly onto the plate.
 */
public class RustedMetalSignEditScreen extends Screen {

    /*
     * Full editor texture size.
     *
     * This is the full Rusted Metal Sign GUI texture,
     * not just the center writing plate.
     */
    private static final int SIGN_TEXTURE_WIDTH = 200;
    private static final int SIGN_TEXTURE_HEIGHT = 125;

    /*
     * Texture used for the Rusted Metal Sign editor.
     */
    private static final Identifier SIGN_EDITOR_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    DungeonCraft.MOD_ID,
                    "textures/gui/rusted_metal_sign_editor.png"
            );

    /*
     * Temporary line length limit.
     *
     * This keeps the editor closer to what actually fits on the
     * in-world Rusted Metal Sign.
     */
    private static final int MAX_CHARS_PER_LINE = 15;

    /*
     * Editor layout tuning.
     */
    private static final int EDITOR_VERTICAL_OFFSET = -15;

    private static final int TEXT_BOX_WIDTH = 120;
    private static final int TEXT_BOX_HEIGHT = 14;

    private static final int TEXT_LINE_1_Y = 28;
    private static final int TEXT_LINE_2_Y = 48;
    private static final int TEXT_LINE_3_Y = 68;
    private static final int TEXT_LINE_4_Y = 88;

    private static final int HIDDEN_TEXT_BOX_Y_OFFSET = -3;

    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_Y_OFFSET = 25;

    private static final int TITLE_Y_OFFSET = -24;

    /*
     * Editor colors.
     */
    private static final int BACKGROUND_COLOR = 0xAA000000;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFF000000;
    private static final int SELECTED_TEXT_COLOR = 0xFFFFFFFF;
    private static final int SELECTION_COLOR = 0xAA3366FF;
    private static final int CURSOR_COLOR = 0xFF000000;
    private static final int HIDDEN_EDIT_BOX_TEXT_COLOR = 0x00000000;

    private final BlockPos signPos;

    private final String startingLine1;
    private final String startingLine2;
    private final String startingLine3;
    private final String startingLine4;

    private int plateLeft;
    private int plateTop;

    private EditBox line1Box;
    private EditBox line2Box;
    private EditBox line3Box;
    private EditBox line4Box;

    private boolean hasSavedAndClosed = false;

    public RustedMetalSignEditScreen(
            BlockPos signPos,
            String line1,
            String line2,
            String line3,
            String line4
    ) {
        super(
                Component.literal(
                        "Edit Metal Sign"
                )
        );

        this.signPos = signPos;
        this.startingLine1 = line1;
        this.startingLine2 = line2;
        this.startingLine3 = line3;
        this.startingLine4 = line4;
    }

    @Override
    protected void init() {
        int centerX =
                this.width / 2;

        this.plateLeft =
                centerX - SIGN_TEXTURE_WIDTH / 2;

        this.plateTop =
                this.height / 2 - SIGN_TEXTURE_HEIGHT / 2 + EDITOR_VERTICAL_OFFSET;

        int textBoxWidth =
                TEXT_BOX_WIDTH;

        int textBoxX =
                centerX - textBoxWidth / 2;

        int firstLineY =
                this.plateTop + TEXT_LINE_1_Y + HIDDEN_TEXT_BOX_Y_OFFSET;

        this.line1Box =
                new EditBox(
                        this.font,
                        textBoxX,
                        firstLineY,
                        textBoxWidth,
                        TEXT_BOX_HEIGHT,
                        Component.literal("Line 1")
                );

        this.line2Box =
                new EditBox(
                        this.font,
                        textBoxX,
                        firstLineY + 20,
                        textBoxWidth,
                        TEXT_BOX_HEIGHT,
                        Component.literal("Line 2")
                );

        this.line3Box =
                new EditBox(
                        this.font,
                        textBoxX,
                        firstLineY + 40,
                        textBoxWidth,
                        TEXT_BOX_HEIGHT,
                        Component.literal("Line 3")
                );

        this.line4Box =
                new EditBox(
                        this.font,
                        textBoxX,
                        firstLineY + 60,
                        textBoxWidth,
                        TEXT_BOX_HEIGHT,
                        Component.literal("Line 4")
                );

        this.line1Box.setValue(this.startingLine1);
        this.line2Box.setValue(this.startingLine2);
        this.line3Box.setValue(this.startingLine3);
        this.line4Box.setValue(this.startingLine4);

        /*
         * Make the EditBoxes feel like text directly on the metal plate.
         */
        this.line1Box.setBordered(false);
        this.line2Box.setBordered(false);
        this.line3Box.setBordered(false);
        this.line4Box.setBordered(false);

        this.line1Box.setMaxLength(MAX_CHARS_PER_LINE);
        this.line2Box.setMaxLength(MAX_CHARS_PER_LINE);
        this.line3Box.setMaxLength(MAX_CHARS_PER_LINE);
        this.line4Box.setMaxLength(MAX_CHARS_PER_LINE);

        this.line1Box.setTextColor(HIDDEN_EDIT_BOX_TEXT_COLOR);
        this.line2Box.setTextColor(HIDDEN_EDIT_BOX_TEXT_COLOR);
        this.line3Box.setTextColor(HIDDEN_EDIT_BOX_TEXT_COLOR);
        this.line4Box.setTextColor(HIDDEN_EDIT_BOX_TEXT_COLOR);

        /*
         * Turn off the default EditBox text shadow.
         *
         * Without this, the editor text can look doubled or like it has
         * a second offset copy underneath it.
         */
        this.line1Box.setTextShadow(false);
        this.line2Box.setTextShadow(false);
        this.line3Box.setTextShadow(false);
        this.line4Box.setTextShadow(false);

        this.addWidget(this.line1Box);
        this.addWidget(this.line2Box);
        this.addWidget(this.line3Box);
        this.addWidget(this.line4Box);

        /*
         * Done button.
         *
         * This saves the edited sign text and closes the editor.
         */
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Done"),
                                button -> this.saveAndClose()
                        )
                        .bounds(
                                centerX - BUTTON_WIDTH / 2,
                                this.plateTop + SIGN_TEXTURE_HEIGHT + BUTTON_Y_OFFSET,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        this.setInitialFocus(
                this.line1Box
        );
    }

    /*
     * Handles keyboard shortcuts for sign-style editing.
     *
     * Enter:
     * - moves to the next line.
     *
     * Backspace:
     * - if the current line is empty, moves to the previous line.
     */
    @Override
    public boolean keyPressed(
            KeyEvent input
    ) {
        if (
                input.key() == GLFW.GLFW_KEY_ENTER
                        || input.key() == GLFW.GLFW_KEY_KP_ENTER
                        || input.key() == GLFW.GLFW_KEY_DOWN
        ) {
            moveFocusDownOneLine();

            return true;
        }

        if (input.key() == GLFW.GLFW_KEY_UP) {
            moveFocusUpOneLine();

            return true;
        }

        if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
            EditBox focusedBox =
                    getFocusedLineBox();

            if (
                    focusedBox != null
                            && focusedBox.getValue().isEmpty()
            ) {
                /*
                 * If line 1 is empty, Backspace should do nothing.
                 *
                 * Up Arrow can still wrap from line 1 to line 4,
                 * but Backspace should not.
                 */
                if (focusedBox == this.line1Box) {
                    return true;
                }

                moveFocusUpOneLine();

                return true;
            }
        }

        return super.keyPressed(
                input
        );
    }

    /*
     * Returns whichever line EditBox is currently focused.
     */
    private EditBox getFocusedLineBox() {
        if (this.line1Box.isFocused()) {
            return this.line1Box;
        }

        if (this.line2Box.isFocused()) {
            return this.line2Box;
        }

        if (this.line3Box.isFocused()) {
            return this.line3Box;
        }

        if (this.line4Box.isFocused()) {
            return this.line4Box;
        }

        return null;
    }

    /*
     * Returns the screen Y position of the currently focused editor line.
     */
    private int getFocusedLineY() {
        if (this.line1Box.isFocused()) {
            return this.plateTop + TEXT_LINE_1_Y;
        }

        if (this.line2Box.isFocused()) {
            return this.plateTop + TEXT_LINE_2_Y;
        }

        if (this.line3Box.isFocused()) {
            return this.plateTop + TEXT_LINE_3_Y;
        }

        if (this.line4Box.isFocused()) {
            return this.plateTop + TEXT_LINE_4_Y;
        }

        return -1;
    }

    /*
     * Moves typing focus to the next sign line.
     *
     * If line 4 is focused, wrap back around to line 1.
     */
    private void moveFocusDownOneLine() {
        if (this.line1Box.isFocused()) {
            focusLine(
                    this.line2Box
            );

            return;
        }

        if (this.line2Box.isFocused()) {
            focusLine(
                    this.line3Box
            );

            return;
        }

        if (this.line3Box.isFocused()) {
            focusLine(
                    this.line4Box
            );

            return;
        }

        if (this.line4Box.isFocused()) {
            focusLine(
                    this.line1Box
            );
        }
    }

    /*
     * Moves typing focus to the previous sign line.
     *
     * If line 1 is focused, wrap back around to line 4.
     */
    private void moveFocusUpOneLine() {
        if (this.line1Box.isFocused()) {
            focusLine(
                    this.line4Box
            );

            return;
        }

        if (this.line4Box.isFocused()) {
            focusLine(
                    this.line3Box
            );

            return;
        }

        if (this.line3Box.isFocused()) {
            focusLine(
                    this.line2Box
            );

            return;
        }

        if (this.line2Box.isFocused()) {
            focusLine(
                    this.line1Box
            );
        }
    }

    /*
     * Gives focus to one EditBox and removes focus from the others.
     */
    private void focusLine(
            EditBox box
    ) {
        this.line1Box.setFocused(false);
        this.line2Box.setFocused(false);
        this.line3Box.setFocused(false);
        this.line4Box.setFocused(false);

        box.setFocused(true);

        this.setFocused(
                box
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        /*
         * Draw a dark transparent background behind the editor.
         *
         * We use a simple full-screen fill here instead of renderBackground(...)
         * because this screen version uses GuiGraphicsExtractor.
         */
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                BACKGROUND_COLOR
        );

        /*
         * Draw the full metal sign background before drawing widgets.
         *
         * The EditBoxes and buttons are drawn later by super.extractRenderState(...),
         * so they appear on top of the sign instead of behind it.
         */

        /*
         * Draw the full Rusted Metal Sign editor texture.
         *
         * This replaces the temporary rectangle-built sign.
         */
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SIGN_EDITOR_TEXTURE,
                this.plateLeft,
                this.plateTop,
                0,
                0,
                SIGN_TEXTURE_WIDTH,
                SIGN_TEXTURE_HEIGHT,
                SIGN_TEXTURE_WIDTH,
                SIGN_TEXTURE_HEIGHT
        );

        /*
         * Screen title.
         */
        graphics.text(
                this.font,
                this.title,
                this.width / 2 - this.font.width(this.title) / 2,
                this.plateTop + TITLE_Y_OFFSET,
                TITLE_COLOR,
                false
        );

        /*
         * Draw widgets after the sign background.
         *
         * This makes the EditBoxes and buttons appear on top.
         */
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        /*
         * Draw the typed sign text manually after the invisible EditBoxes.
         *
         * This makes the editor feel like engraving directly on the metal plate
         * instead of typing into form boxes.
         */
        drawCenteredEditorLine(
                graphics,
                this.line1Box,
                this.plateTop + TEXT_LINE_1_Y
        );

        drawCenteredEditorLine(
                graphics,
                this.line2Box,
                this.plateTop + TEXT_LINE_2_Y
        );

        drawCenteredEditorLine(
                graphics,
                this.line3Box,
                this.plateTop + TEXT_LINE_3_Y
        );

        drawCenteredEditorLine(
                graphics,
                this.line4Box,
                this.plateTop + TEXT_LINE_4_Y
        );

        /*
         * Draw the manual cursor on top of the manually drawn text.
         */
        drawEditorCursor(
                graphics
        );
    }

    /*
     * Draws one centered editor line directly onto the metal plate.
     *
     * This version also draws a blue selection highlight when part of the
     * hidden EditBox text is selected, similar to the vanilla sign editor.
     */
    private void drawCenteredEditorLine(
            GuiGraphicsExtractor graphics,
            EditBox box,
            int y
    ) {
        String text =
                box.getValue();

        if (text == null || text.isEmpty()) {
            return;
        }

        int textLeft =
                this.width / 2 - this.font.width(text) / 2;

        String highlightedText =
                box.getHighlighted();

        if (
                box.isFocused()
                        && highlightedText != null
                        && !highlightedText.isEmpty()
        ) {
            int[] selectionRange =
                    getSelectionRange(
                            box
                    );

            int selectionStart =
                    selectionRange[0];

            int selectionEnd =
                    selectionRange[1];

            int selectionLeft =
                    textLeft + this.font.width(
                            text.substring(
                                    0,
                                    selectionStart
                            )
                    );

            int selectionRight =
                    textLeft + this.font.width(
                            text.substring(
                                    0,
                                    selectionEnd
                            )
                    );

            /*
             * Vanilla-style blue selection box.
             */
            graphics.fill(
                    selectionLeft - 1,
                    y - 1,
                    selectionRight + 1,
                    y + 10,
                    SELECTION_COLOR
            );

            String beforeSelection =
                    text.substring(
                            0,
                            selectionStart
                    );

            String selectedText =
                    text.substring(
                            selectionStart,
                            selectionEnd
                    );

            String afterSelection =
                    text.substring(
                            selectionEnd
                    );

            int drawX =
                    textLeft;

            graphics.text(
                    this.font,
                    beforeSelection,
                    drawX,
                    y,
                    TEXT_COLOR,
                    false
            );

            drawX +=
                    this.font.width(
                            beforeSelection
                    );

            graphics.text(
                    this.font,
                    selectedText,
                    drawX,
                    y,
                    SELECTED_TEXT_COLOR,
                    false
            );

            drawX +=
                    this.font.width(
                            selectedText
                    );

            graphics.text(
                    this.font,
                    afterSelection,
                    drawX,
                    y,
                    TEXT_COLOR,
                    false
            );

            return;
        }

        graphics.text(
                this.font,
                text,
                textLeft,
                y,
                TEXT_COLOR,
                false
        );
    }

    /*
     * Figures out which part of the EditBox text is selected.
     *
     * The EditBox gives us the selected text and the cursor position.
     * This method turns that into start/end indexes so we can draw our
     * own blue highlight box.
     */
    private int[] getSelectionRange(
            EditBox box
    ) {
        String text =
                box.getValue();

        String highlightedText =
                box.getHighlighted();

        if (
                text == null
                        || highlightedText == null
                        || highlightedText.isEmpty()
        ) {
            int cursorPosition =
                    box.getCursorPosition();

            return new int[] {
                    cursorPosition,
                    cursorPosition
            };
        }

        int cursorPosition =
                box.getCursorPosition();

        if (cursorPosition < 0) {
            cursorPosition = 0;
        }

        if (cursorPosition > text.length()) {
            cursorPosition = text.length();
        }

        int highlightedLength =
                highlightedText.length();

        int possibleStartBeforeCursor =
                cursorPosition - highlightedLength;

        if (
                possibleStartBeforeCursor >= 0
                        && text.substring(
                        possibleStartBeforeCursor,
                        cursorPosition
                ).equals(
                        highlightedText
                )
        ) {
            return new int[] {
                    possibleStartBeforeCursor,
                    cursorPosition
            };
        }

        int possibleEndAfterCursor =
                cursorPosition + highlightedLength;

        if (
                possibleEndAfterCursor <= text.length()
                        && text.substring(
                        cursorPosition,
                        possibleEndAfterCursor
                ).equals(
                        highlightedText
                )
        ) {
            return new int[] {
                    cursorPosition,
                    possibleEndAfterCursor
            };
        }

        /*
         * Fallback for unusual cases.
         */
        int fallbackStart =
                text.indexOf(
                        highlightedText
                );

        if (fallbackStart >= 0) {
            return new int[] {
                    fallbackStart,
                    fallbackStart + highlightedLength
            };
        }

        return new int[] {
                cursorPosition,
                cursorPosition
        };
    }

    /*
     * Draws a non-blinking manual cursor for the invisible EditBox system.
     *
     * Later, this can be replaced with a real Engraving Tool cursor texture.
     */
    private void drawEditorCursor(
            GuiGraphicsExtractor graphics
    ) {
        EditBox focusedBox =
                getFocusedLineBox();

        if (focusedBox == null) {
            return;
        }

        if (!focusedBox.getHighlighted().isEmpty()) {
            return;
        }

        String text =
                focusedBox.getValue();

        int lineY =
                getFocusedLineY();

        if (lineY < 0) {
            return;
        }

        int cursorPosition =
                focusedBox.getCursorPosition();

        if (cursorPosition < 0) {
            cursorPosition = 0;
        }

        if (cursorPosition > text.length()) {
            cursorPosition = text.length();
        }

        String textBeforeCursor =
                text.substring(
                        0,
                        cursorPosition
                );

        int textLeft =
                this.width / 2 - this.font.width(text) / 2;

        int cursorX =
                textLeft + this.font.width(textBeforeCursor);

        /*
         * Small dark engraving point cursor.
         */
        graphics.fill(
                cursorX,
                lineY - 1,
                cursorX + 2,
                lineY + 10,
                CURSOR_COLOR
        );
    }

    /*
     * Called when the screen is closed, including pressing Escape.
     *
     * We save instead of cancelling.
     */
    @Override
    public void onClose() {
        saveAndClose();
    }

    /*
     * Sends the edited sign text to the server, then closes the editor.
     */
    private void saveAndClose() {
        if (this.hasSavedAndClosed) {
            super.onClose();

            return;
        }

        this.hasSavedAndClosed = true;

        ClientPlayNetworking.send(
                new SaveRustedMetalSignEditorPayload(
                        this.signPos,
                        cleanLine(
                                this.line1Box.getValue()
                        ),
                        cleanLine(
                                this.line2Box.getValue()
                        ),
                        cleanLine(
                                this.line3Box.getValue()
                        ),
                        cleanLine(
                                this.line4Box.getValue()
                        )
                )
        );

        super.onClose();
    }

    /*
     * Keeps each sign line from getting too long.
     *
     * This is a temporary limit for the metal sign editor.
     */
    private String cleanLine(
            String line
    ) {
        if (line == null) {
            return "";
        }

        if (line.length() > MAX_CHARS_PER_LINE) {
            return line.substring(
                    0,
                    MAX_CHARS_PER_LINE
            );
        }

        return line;
    }
}