package com.dungeoncraft.config;

/**
 * Per-port direction permissions for the Power Router.
 */
public enum DiverterPortMode {
    DISABLED("disabled", "Disabled", false, false),
    INPUT_ONLY("input_only", "Input Only", true, false),
    OUTPUT_ONLY("output_only", "Output Only", false, true),
    BIDIRECTIONAL("bidirectional", "Bidirectional", true, true);

    private final String serializedName;
    private final String displayName;
    private final boolean acceptsInput;
    private final boolean allowsOutput;

    DiverterPortMode(
            String serializedName,
            String displayName,
            boolean acceptsInput,
            boolean allowsOutput
    ) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.acceptsInput = acceptsInput;
        this.allowsOutput = allowsOutput;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean acceptsInput() {
        return this.acceptsInput;
    }

    public boolean allowsOutput() {
        return this.allowsOutput;
    }

    public DiverterPortMode next() {
        return switch (this) {
            case DISABLED -> INPUT_ONLY;
            case INPUT_ONLY -> OUTPUT_ONLY;
            case OUTPUT_ONLY -> BIDIRECTIONAL;
            case BIDIRECTIONAL -> DISABLED;
        };
    }

    public static DiverterPortMode fromPackedValue(int packedValue) {
        return switch (packedValue & 0b11) {
            case 1 -> INPUT_ONLY;
            case 2 -> OUTPUT_ONLY;
            case 3 -> BIDIRECTIONAL;
            default -> DISABLED;
        };
    }
}
