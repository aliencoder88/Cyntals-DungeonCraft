package com.dungeoncraft.client.screen;

import com.dungeoncraft.config.CodingToolAdvancedConfig;
import com.dungeoncraft.config.CodingToolDeviceType;
import com.dungeoncraft.config.DeviceSignalMode;
import com.dungeoncraft.config.DiverterPortMode;
import com.dungeoncraft.config.HiddenLeverOutputFace;
import com.dungeoncraft.config.HiddenPanelWiringMode;
import com.dungeoncraft.config.LeverPowerMode;
import com.dungeoncraft.config.LockSignalPolarity;
import com.dungeoncraft.config.PowerDiverterConfig;
import com.dungeoncraft.config.PowerDiverterPort;
import com.dungeoncraft.config.VerifiedSignalKey;
import com.dungeoncraft.network.SaveCodingToolConfigPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Shared Coding Tool screen for levers and the four-port Power Router.
 */
public class CodingToolScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 240;
    private static final int BUTTON_WIDTH = 260;
    private static final int HALF_BUTTON_WIDTH = 126;
    private static final int QUARTER_BUTTON_WIDTH = 62;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_HEIGHT = 18;
    private static final int TEXT_BOX_HEIGHT = 16;

    private static final int BACKGROUND_COLOR = 0xB0000000;
    private static final int PANEL_COLOR = 0xFF262626;
    private static final int PANEL_BORDER_COLOR = 0xFF777777;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int MUTED_TEXT_COLOR = 0xFFAAAAAA;
    private static final int WARNING_COLOR = 0xFFFFC65C;

    private enum HiddenLeverPage {
        LEVER_OUTPUT,
        PANEL_WIRING
    }


    private final BlockPos devicePos;
    private final CodingToolDeviceType deviceType;

    private DeviceSignalMode signalMode;
    private LeverPowerMode leverPowerMode;
    private int outputFaceMask;

    private String verifiedOutputKey;
    private HiddenPanelWiringMode panelWiringMode;
    private LockSignalPolarity lockSignalPolarity;
    private DeviceSignalMode panelSignalMode;
    private int panelInputFaceMask;
    private String panelRequiredKey;
    private PowerDiverterConfig powerDiverterConfig;

    private HiddenLeverPage currentHiddenLeverPage =
            HiddenLeverPage.LEVER_OUTPUT;
    private PowerDiverterPort selectedRouteSource =
            PowerDiverterPort.NORTH;

    private final List<AbstractWidget> outputPageWidgets = new ArrayList<>();
    private final List<AbstractWidget> panelPageWidgets = new ArrayList<>();
    private final List<AbstractWidget> diverterPageWidgets =
            new ArrayList<>();

    private final Map<HiddenLeverOutputFace, Button> outputFaceButtons =
            new EnumMap<>(HiddenLeverOutputFace.class);
    private final Map<HiddenLeverOutputFace, Button> inputFaceButtons =
            new EnumMap<>(HiddenLeverOutputFace.class);
    private final Map<PowerDiverterPort, Button> routeDestinationButtons =
            new EnumMap<>(PowerDiverterPort.class);

    private Button firstTabButton;
    private Button secondTabButton;
    private Button routeSourceButton;
    private EditBox verifiedOutputKeyBox;
    private EditBox panelRequiredKeyBox;

    private int panelLeft;
    private int panelTop;
    private boolean saved;

    public CodingToolScreen(
            BlockPos devicePos,
            CodingToolDeviceType deviceType,
            DeviceSignalMode signalMode,
            LeverPowerMode leverPowerMode,
            int outputFaceMask,
            CodingToolAdvancedConfig advancedConfig
    ) {
        super(Component.literal("Coding Tool"));
        this.devicePos = devicePos;
        this.deviceType = deviceType;
        this.signalMode = signalMode;
        this.leverPowerMode = leverPowerMode;
        this.outputFaceMask =
                deviceType == CodingToolDeviceType.HIDDEN_BLOCK_LEVER
                        ? HiddenLeverOutputFace.sanitizeMask(outputFaceMask)
                        : 0;

        CodingToolAdvancedConfig sanitized =
                advancedConfig.sanitizedFor(deviceType);
        this.verifiedOutputKey = sanitized.verifiedOutputKey();
        this.panelWiringMode = sanitized.panelWiringMode();
        this.lockSignalPolarity = sanitized.lockSignalPolarity();
        this.panelSignalMode = sanitized.panelSignalMode();
        this.panelInputFaceMask = sanitized.panelInputFaceMask();
        this.panelRequiredKey = sanitized.panelRequiredKey();
        this.powerDiverterConfig = sanitized.powerDiverterConfig();
    }

    @Override
    protected void init() {
        this.outputPageWidgets.clear();
        this.panelPageWidgets.clear();
        this.diverterPageWidgets.clear();
        this.outputFaceButtons.clear();
        this.inputFaceButtons.clear();
        this.routeDestinationButtons.clear();

        this.panelLeft = this.width / 2 - PANEL_WIDTH / 2;
        this.panelTop = this.height / 2 - PANEL_HEIGHT / 2;

        int buttonX = this.width / 2 - BUTTON_WIDTH / 2;

        if (this.deviceType == CodingToolDeviceType.HIDDEN_BLOCK_LEVER) {
            this.addHiddenLeverTabs(buttonX);
            this.addOutputPageWidgets(buttonX);
            this.addPanelPageWidgets(buttonX);
        } else if (this.deviceType == CodingToolDeviceType.IRON_LEVER) {
            this.addOutputPageWidgets(buttonX);
        } else if (this.deviceType
                == CodingToolDeviceType.POWER_DIVERTER) {
            this.addDiverterRoutingWidgets(buttonX);
        }

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Save Configuration"),
                                button -> this.saveAndClose()
                        )
                        .bounds(
                                buttonX,
                                this.panelTop + 214,
                                HALF_BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Cancel"),
                                button -> super.onClose()
                        )
                        .bounds(
                                buttonX + 134,
                                this.panelTop + 214,
                                HALF_BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        this.updatePageVisibility();
    }

    private void addHiddenLeverTabs(int buttonX) {
        this.firstTabButton = this.addRenderableWidget(
                Button.builder(
                                Component.literal("Lever Output"),
                                button -> {
                                    this.currentHiddenLeverPage =
                                            HiddenLeverPage.LEVER_OUTPUT;
                                    this.updatePageVisibility();
                                }
                        )
                        .bounds(
                                buttonX,
                                this.panelTop + 26,
                                HALF_BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        this.secondTabButton = this.addRenderableWidget(
                Button.builder(
                                Component.literal("Panel Wiring"),
                                button -> {
                                    this.currentHiddenLeverPage =
                                            HiddenLeverPage.PANEL_WIRING;
                                    this.updatePageVisibility();
                                }
                        )
                        .bounds(
                                buttonX + 134,
                                this.panelTop + 26,
                                HALF_BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
    }

    private void addOutputPageWidgets(int buttonX) {
        Button powerModeButton = this.addRenderableWidget(
                Button.builder(
                                getPowerModeButtonText(),
                                button -> {
                                    this.leverPowerMode =
                                            this.leverPowerMode
                                                    == LeverPowerMode.ACTIVATED_IS_ON
                                                    ? LeverPowerMode.RESTING_IS_ON
                                                    : LeverPowerMode.ACTIVATED_IS_ON;
                                    button.setMessage(
                                            getPowerModeButtonText()
                                    );
                                }
                        )
                        .bounds(
                                buttonX,
                                this.panelTop + 48,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
        this.outputPageWidgets.add(powerModeButton);

        Button signalModeButton = this.addRenderableWidget(
                Button.builder(
                                getSignalModeButtonText(),
                                button -> {
                                    this.signalMode =
                                            this.signalMode
                                                    == DeviceSignalMode.REGULAR_REDSTONE
                                                    ? DeviceSignalMode.VERIFIED_SIGNAL
                                                    : DeviceSignalMode.REGULAR_REDSTONE;
                                    button.setMessage(
                                            getSignalModeButtonText()
                                    );
                                }
                        )
                        .bounds(
                                buttonX,
                                this.panelTop + 70,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
        this.outputPageWidgets.add(signalModeButton);

        this.verifiedOutputKeyBox = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        buttonX,
                        this.panelTop + 105,
                        BUTTON_WIDTH,
                        TEXT_BOX_HEIGHT,
                        Component.literal("Verified output key")
                )
        );
        this.verifiedOutputKeyBox.setMaxLength(
                VerifiedSignalKey.MAX_LENGTH
        );
        this.verifiedOutputKeyBox.setValue(this.verifiedOutputKey);
        this.outputPageWidgets.add(this.verifiedOutputKeyBox);

        if (this.deviceType == CodingToolDeviceType.HIDDEN_BLOCK_LEVER) {
            this.addFaceButtons(
                    this.outputPageWidgets,
                    true,
                    buttonX,
                    this.panelTop + 138
            );
        }
    }

    private void addPanelPageWidgets(int buttonX) {
        Button wiringModeButton = this.addRenderableWidget(
                Button.builder(
                                getPanelWiringModeButtonText(),
                                button -> {
                                    this.panelWiringMode =
                                            this.panelWiringMode.next();

                                    if (this.panelWiringMode
                                                    != HiddenPanelWiringMode.UNLOCKED
                                            && this.panelInputFaceMask == 0) {
                                        this.panelInputFaceMask =
                                                HiddenLeverOutputFace.defaultMask();
                                        this.refreshFaceButtons(false);
                                    }

                                    button.setMessage(
                                            getPanelWiringModeButtonText()
                                    );
                                }
                        )
                        .bounds(
                                buttonX,
                                this.panelTop + 48,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
        this.panelPageWidgets.add(wiringModeButton);

        Button polarityButton = this.addRenderableWidget(
                Button.builder(
                                getLockSignalPolarityButtonText(),
                                button -> {
                                    this.lockSignalPolarity =
                                            this.lockSignalPolarity.next();
                                    button.setMessage(
                                            getLockSignalPolarityButtonText()
                                    );
                                }
                        )
                        .bounds(
                                buttonX,
                                this.panelTop + 70,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
        this.panelPageWidgets.add(polarityButton);

        Button panelSignalModeButton = this.addRenderableWidget(
                Button.builder(
                                getPanelSignalModeButtonText(),
                                button -> {
                                    this.panelSignalMode =
                                            this.panelSignalMode
                                                    == DeviceSignalMode.REGULAR_REDSTONE
                                                    ? DeviceSignalMode.VERIFIED_SIGNAL
                                                    : DeviceSignalMode.REGULAR_REDSTONE;
                                    button.setMessage(
                                            getPanelSignalModeButtonText()
                                    );
                                }
                        )
                        .bounds(
                                buttonX,
                                this.panelTop + 92,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
        this.panelPageWidgets.add(panelSignalModeButton);

        this.panelRequiredKeyBox = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        buttonX,
                        this.panelTop + 127,
                        BUTTON_WIDTH,
                        TEXT_BOX_HEIGHT,
                        Component.literal("Required verified key")
                )
        );
        this.panelRequiredKeyBox.setMaxLength(
                VerifiedSignalKey.MAX_LENGTH
        );
        this.panelRequiredKeyBox.setValue(this.panelRequiredKey);
        this.panelPageWidgets.add(this.panelRequiredKeyBox);

        this.addFaceButtons(
                this.panelPageWidgets,
                false,
                buttonX,
                this.panelTop + 160
        );
    }

    private void addDiverterRoutingWidgets(int buttonX) {
        this.routeSourceButton = this.addRenderableWidget(
                Button.builder(
                                this.getRouteSourceButtonText(),
                                button -> {
                                    this.selectedRouteSource =
                                            PowerDiverterPort.fromIndex(
                                                    this.selectedRouteSource
                                                            .getIndex() + 1
                                            );
                                    this.refreshRouteDestinationButtons();
                                }
                        )
                        .bounds(
                                buttonX,
                                this.panelTop + 50,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
        this.diverterPageWidgets.add(this.routeSourceButton);

        PowerDiverterPort[] ports = PowerDiverterPort.values();

        for (int index = 0; index < ports.length; index++) {
            PowerDiverterPort port = ports[index];
            int column = index % 2;
            int row = index / 2;

            Button routeButton = this.addRenderableWidget(
                    Button.builder(
                                    this.getRouteGridButtonText(port),
                                    button -> {
                                        if (port
                                                == this.selectedRouteSource) {
                                            DiverterPortMode nextMode =
                                                    this.powerDiverterConfig
                                                            .getPortMode(port)
                                                            .next();
                                            this.powerDiverterConfig =
                                                    this.powerDiverterConfig
                                                            .withPortMode(
                                                                    port,
                                                                    nextMode
                                                            );
                                        } else {
                                            boolean currentlyEnabled =
                                                    this.powerDiverterConfig
                                                            .routesTo(
                                                                    this.selectedRouteSource,
                                                                    port
                                                            );
                                            this.powerDiverterConfig =
                                                    this.powerDiverterConfig
                                                            .withRouteEnabled(
                                                                    this.selectedRouteSource,
                                                                    port,
                                                                    !currentlyEnabled
                                                            );
                                        }

                                        this.refreshRouteDestinationButtons();
                                    }
                            )
                            .bounds(
                                    buttonX + column * 134,
                                    this.panelTop + 92 + row * 22,
                                    HALF_BUTTON_WIDTH,
                                    BUTTON_HEIGHT
                            )
                            .build()
            );
            this.diverterPageWidgets.add(routeButton);
            this.routeDestinationButtons.put(port, routeButton);
        }

        this.refreshRouteDestinationButtons();
    }

    private void addFaceButtons(
            List<AbstractWidget> pageWidgets,
            boolean output,
            int buttonX,
            int firstRowY
    ) {
        HiddenLeverOutputFace[] firstRowFaces = {
                HiddenLeverOutputFace.TOP,
                HiddenLeverOutputFace.BOTTOM,
                HiddenLeverOutputFace.LEFT,
                HiddenLeverOutputFace.RIGHT
        };

        for (int index = 0; index < firstRowFaces.length; index++) {
            this.addFaceButton(
                    pageWidgets,
                    firstRowFaces[index],
                    output,
                    buttonX + index
                            * (QUARTER_BUTTON_WIDTH + BUTTON_GAP),
                    firstRowY,
                    QUARTER_BUTTON_WIDTH
            );
        }

        this.addFaceButton(
                pageWidgets,
                HiddenLeverOutputFace.BACK,
                output,
                buttonX,
                firstRowY + 22,
                BUTTON_WIDTH
        );
    }

    private void addFaceButton(
            List<AbstractWidget> pageWidgets,
            HiddenLeverOutputFace face,
            boolean output,
            int x,
            int y,
            int width
    ) {
        Button button = this.addRenderableWidget(
                Button.builder(
                                getFaceButtonText(face, output),
                                clickedButton -> {
                                    if (output) {
                                        this.outputFaceMask ^= face.getMask();
                                        this.outputFaceMask =
                                                HiddenLeverOutputFace.sanitizeMask(
                                                        this.outputFaceMask
                                                );
                                    } else {
                                        this.panelInputFaceMask ^=
                                                face.getMask();
                                        this.panelInputFaceMask =
                                                HiddenLeverOutputFace.sanitizeMask(
                                                        this.panelInputFaceMask
                                                );
                                    }

                                    clickedButton.setMessage(
                                            getFaceButtonText(face, output)
                                    );
                                }
                        )
                        .bounds(x, y, width, BUTTON_HEIGHT)
                        .build()
        );
        pageWidgets.add(button);

        if (output) {
            this.outputFaceButtons.put(face, button);
        } else {
            this.inputFaceButtons.put(face, button);
        }
    }

    private void refreshFaceButtons(boolean output) {
        Map<HiddenLeverOutputFace, Button> buttons = output
                ? this.outputFaceButtons
                : this.inputFaceButtons;

        for (Map.Entry<HiddenLeverOutputFace, Button> entry
                : buttons.entrySet()) {
            entry.getValue().setMessage(
                    this.getFaceButtonText(entry.getKey(), output)
            );
        }
    }

    private void refreshRouteDestinationButtons() {
        if (this.routeSourceButton != null) {
            this.routeSourceButton.setMessage(
                    this.getRouteSourceButtonText()
            );
        }

        for (Map.Entry<PowerDiverterPort, Button> entry
                : this.routeDestinationButtons.entrySet()) {
            PowerDiverterPort port = entry.getKey();
            Button button = entry.getValue();
            button.setMessage(this.getRouteGridButtonText(port));
            button.active = true;
        }
    }

    private void updatePageVisibility() {
        boolean outputPageVisible =
                this.deviceType == CodingToolDeviceType.IRON_LEVER
                        || (this.deviceType
                                == CodingToolDeviceType.HIDDEN_BLOCK_LEVER
                                && this.currentHiddenLeverPage
                                == HiddenLeverPage.LEVER_OUTPUT);
        boolean panelPageVisible =
                this.deviceType == CodingToolDeviceType.HIDDEN_BLOCK_LEVER
                        && this.currentHiddenLeverPage
                        == HiddenLeverPage.PANEL_WIRING;
        boolean diverterPageVisible =
                this.deviceType == CodingToolDeviceType.POWER_DIVERTER;

        this.setWidgetsVisible(this.outputPageWidgets, outputPageVisible);
        this.setWidgetsVisible(this.panelPageWidgets, panelPageVisible);
        this.setWidgetsVisible(
                this.diverterPageWidgets,
                diverterPageVisible
        );

        if (this.firstTabButton != null) {
            this.firstTabButton.active = !outputPageVisible;
        }

        if (this.secondTabButton != null) {
            this.secondTabButton.active = !panelPageVisible;
        }

        if (diverterPageVisible) {
            this.refreshRouteDestinationButtons();
        }
    }

    private void setWidgetsVisible(
            List<AbstractWidget> widgets,
            boolean visible
    ) {
        for (AbstractWidget widget : widgets) {
            widget.visible = visible;
            widget.active = visible;
        }
    }

    private Component getPowerModeButtonText() {
        if (this.deviceType == CodingToolDeviceType.HIDDEN_BLOCK_LEVER) {
            return Component.literal(
                    this.leverPowerMode == LeverPowerMode.ACTIVATED_IS_ON
                            ? "Powered Position: Down = ON"
                            : "Powered Position: Up = ON"
            );
        }

        return Component.literal(
                this.leverPowerMode == LeverPowerMode.ACTIVATED_IS_ON
                        ? "Powered Position: Pulled = ON"
                        : "Powered Position: Resting = ON"
        );
    }

    private Component getSignalModeButtonText() {
        return Component.literal(
                this.signalMode == DeviceSignalMode.REGULAR_REDSTONE
                        ? "Output Signal: Regular Redstone"
                        : "Output Signal: Verified (Reserved)"
        );
    }

    private Component getPanelWiringModeButtonText() {
        return Component.literal(
                switch (this.panelWiringMode) {
                    case UNLOCKED -> "Panel: Unlocked";
                    case LOCKED_UNLOCKED ->
                            "Panel: Locked / Unlocked";
                    case LOCKED_CLOSED_UNLOCKED_OPEN ->
                            "Panel: Locked Closed / Unlocked Open";
                }
        );
    }

    private Component getLockSignalPolarityButtonText() {
        return Component.literal(
                this.lockSignalPolarity
                        == LockSignalPolarity.POWER_IS_LOCKED
                        ? "Lock Polarity: Power = Locked"
                        : "Lock Polarity: No Power = Locked"
        );
    }

    private Component getPanelSignalModeButtonText() {
        return Component.literal(
                this.panelSignalMode
                        == DeviceSignalMode.REGULAR_REDSTONE
                        ? "Panel Input: Regular Redstone"
                        : "Panel Input: Verified (Reserved)"
        );
    }

    private Component getFaceButtonText(
            HiddenLeverOutputFace face,
            boolean output
    ) {
        int mask = output
                ? this.outputFaceMask
                : this.panelInputFaceMask;

        return Component.literal(
                face.getDisplayName()
                        + ": "
                        + (face.isEnabled(mask) ? "ON" : "OFF")
        );
    }

    private Component getRouteSourceButtonText() {
        return Component.literal(
                "Signal Arrives From: "
                        + this.selectedRouteSource.getDisplayName()
        );
    }

    private Component getRouteGridButtonText(
            PowerDiverterPort port
    ) {
        if (port == this.selectedRouteSource) {
            return Component.literal(
                    "Mode: "
                            + this.powerDiverterConfig
                                    .getPortMode(port)
                                    .getDisplayName()
            );
        }

        boolean requested = this.powerDiverterConfig.routesTo(
                this.selectedRouteSource,
                port
        );

        if (!requested) {
            return Component.literal(
                    port.getDisplayName() + ": OFF"
            );
        }

        if (this.powerDiverterConfig.isRouteBlocked(
                this.selectedRouteSource,
                port
        )) {
            return Component.literal(
                            port.getDisplayName() + ": ROUTE"
                    )
                    .append(
                            Component.literal(" !")
                                    .withStyle(ChatFormatting.RED)
                    );
        }

        return Component.literal(
                port.getDisplayName() + ": ROUTE"
        );
    }

    private String getDeviceName() {
        return switch (this.deviceType) {
            case HIDDEN_BLOCK_LEVER -> "Hidden Block Lever";
            case IRON_LEVER -> "Iron Lever";
            case POWER_DIVERTER -> "Power Router";
        };
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        graphics.fill(
                this.panelLeft - 1,
                this.panelTop - 1,
                this.panelLeft + PANEL_WIDTH + 1,
                this.panelTop + PANEL_HEIGHT + 1,
                PANEL_BORDER_COLOR
        );
        graphics.fill(
                this.panelLeft,
                this.panelTop,
                this.panelLeft + PANEL_WIDTH,
                this.panelTop + PANEL_HEIGHT,
                PANEL_COLOR
        );

        String heading = "Coding Tool — " + this.getDeviceName();
        this.drawCenteredText(
                graphics,
                heading,
                this.panelTop + 8,
                TITLE_COLOR,
                true
        );

        if (this.deviceType == CodingToolDeviceType.POWER_DIVERTER) {
            this.drawDiverterRoutingText(graphics);
        } else {
            boolean outputPageVisible =
                    this.deviceType == CodingToolDeviceType.IRON_LEVER
                            || this.currentHiddenLeverPage
                            == HiddenLeverPage.LEVER_OUTPUT;

            if (outputPageVisible) {
                this.drawOutputPageText(graphics);
            } else {
                this.drawPanelPageText(graphics);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawOutputPageText(GuiGraphicsExtractor graphics) {
        graphics.text(
                this.font,
                "Verified output key / code",
                this.panelLeft + 20,
                this.panelTop + 94,
                TEXT_COLOR,
                false
        );

        if (this.deviceType == CodingToolDeviceType.HIDDEN_BLOCK_LEVER) {
            this.drawCenteredText(
                    graphics,
                    "Output faces — left/right while looking at panel",
                    this.panelTop + 126,
                    TEXT_COLOR,
                    false
            );
            this.drawCenteredText(
                    graphics,
                    "Front stays reserved for the panel and lever.",
                    this.panelTop + 184,
                    MUTED_TEXT_COLOR,
                    false
            );
        } else {
            this.drawCenteredText(
                    graphics,
                    "Iron Lever keeps its existing power-flow direction.",
                    this.panelTop + 136,
                    MUTED_TEXT_COLOR,
                    false
            );
        }

        if (this.signalMode == DeviceSignalMode.VERIFIED_SIGNAL) {
            String warning = this.verifiedOutputKeyBox != null
                            && this.verifiedOutputKeyBox.getValue().isBlank()
                    ? "Verified output is reserved; blank keys transmit nothing."
                    : "Verified output and its key are saved, but inactive yet.";
            this.drawCenteredText(
                    graphics,
                    warning,
                    this.panelTop + 198,
                    WARNING_COLOR,
                    false
            );
        }
    }

    private void drawPanelPageText(GuiGraphicsExtractor graphics) {
        graphics.text(
                this.font,
                "Required verified key / code",
                this.panelLeft + 20,
                this.panelTop + 116,
                TEXT_COLOR,
                false
        );

        this.drawCenteredText(
                graphics,
                "Panel input faces — left/right looking at panel",
                this.panelTop + 148,
                TEXT_COLOR,
                false
        );

        String message;
        int messageColor;

        if (this.panelWiringMode != HiddenPanelWiringMode.UNLOCKED
                && this.panelInputFaceMask == 0) {
            message = "Locked modes require an input; Back will be restored.";
            messageColor = WARNING_COLOR;
        } else if (this.panelSignalMode
                == DeviceSignalMode.VERIFIED_SIGNAL) {
            message = this.panelRequiredKeyBox != null
                            && this.panelRequiredKeyBox.getValue().isBlank()
                    ? "Verified input is reserved; a blank key matches nothing."
                    : "Verified authorization and key are saved, but inactive yet.";
            messageColor = WARNING_COLOR;
        } else if (this.panelWiringMode != HiddenPanelWiringMode.UNLOCKED
                && (this.outputFaceMask & this.panelInputFaceMask) != 0) {
            message = "Ordinary input/output overlap may feed power back.";
            messageColor = WARNING_COLOR;
        } else if (this.panelWiringMode
                == HiddenPanelWiringMode.UNLOCKED) {
            message = "Unlocked mode ignores panel input and lock polarity.";
            messageColor = MUTED_TEXT_COLOR;
        } else {
            message = "The selected input and polarity control the panel lock.";
            messageColor = MUTED_TEXT_COLOR;
        }

        this.drawCenteredText(
                graphics,
                message,
                this.panelTop + 204,
                messageColor,
                false
        );
    }

    private void drawDiverterRoutingText(
            GuiGraphicsExtractor graphics
    ) {
        this.drawCenteredText(
                graphics,
                "Routing and port modes",
                this.panelTop + 32,
                TEXT_COLOR,
                false
        );
        this.drawCenteredText(
                graphics,
                "Send that incoming signal to:",
                this.panelTop + 78,
                TEXT_COLOR,
                false
        );

        DiverterPortMode sourceMode = this.powerDiverterConfig
                .getPortMode(this.selectedRouteSource);
        String sourceStatus = sourceMode.acceptsInput()
                ? "Selected source accepts incoming power."
                : "Selected source cannot receive power.";
        int sourceColor = sourceMode.acceptsInput()
                ? MUTED_TEXT_COLOR
                : WARNING_COLOR;

        this.drawCenteredText(
                graphics,
                sourceStatus,
                this.panelTop + 148,
                sourceColor,
                false
        );
        this.drawCenteredText(
                graphics,
                "Red ! = requested route cannot carry power.",
                this.panelTop + 165,
                MUTED_TEXT_COLOR,
                false
        );
        this.drawCenteredText(
                graphics,
                "Check port modes or reverse-route feedback.",
                this.panelTop + 182,
                MUTED_TEXT_COLOR,
                false
        );
        this.drawCenteredText(
                graphics,
                "Disabled ports clear connected routes when saved.",
                this.panelTop + 199,
                MUTED_TEXT_COLOR,
                false
        );
    }

    private void drawCenteredText(
            GuiGraphicsExtractor graphics,
            String text,
            int y,
            int color,
            boolean shadow
    ) {
        graphics.text(
                this.font,
                text,
                this.width / 2 - this.font.width(text) / 2,
                y,
                color,
                shadow
        );
    }

    private void saveAndClose() {
        if (this.saved) {
            super.onClose();
            return;
        }

        this.saved = true;

        this.verifiedOutputKey = VerifiedSignalKey.sanitize(
                this.verifiedOutputKeyBox == null
                        ? this.verifiedOutputKey
                        : this.verifiedOutputKeyBox.getValue()
        );
        this.panelRequiredKey = VerifiedSignalKey.sanitize(
                this.panelRequiredKeyBox == null
                        ? this.panelRequiredKey
                        : this.panelRequiredKeyBox.getValue()
        );

        if (this.deviceType == CodingToolDeviceType.POWER_DIVERTER) {
            this.powerDiverterConfig =
                    this.powerDiverterConfig.normalizedForSave();
        }

        CodingToolAdvancedConfig advancedConfig =
                new CodingToolAdvancedConfig(
                        this.verifiedOutputKey,
                        this.panelWiringMode,
                        this.lockSignalPolarity,
                        this.panelSignalMode,
                        this.panelInputFaceMask,
                        this.panelRequiredKey,
                        this.powerDiverterConfig
                ).sanitizedFor(this.deviceType);

        ClientPlayNetworking.send(
                new SaveCodingToolConfigPayload(
                        this.devicePos,
                        this.deviceType.getSerializedName(),
                        this.signalMode.getSerializedName(),
                        this.leverPowerMode.getSerializedName(),
                        this.outputFaceMask,
                        advancedConfig
                )
        );
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
