package com.dungeoncraft.block;

import net.minecraft.util.StringRepresentable;

/*
 * IMPORT MANAGEMENT NOTE
 *
 * Compare these imports with those already present in your file.
 * Do not add duplicate imports.
 * Remove duplicate or unused imports after adapting this file.
 */
public enum ArchedWindowBuried implements StringRepresentable {

    /*
     * No packed dirt is present.
     */
    CLEAR("clear"),

    /*
     * Packed dirt covers only the front side.
     */
    FRONT("front"),

    /*
     * Packed dirt covers only the back side.
     */
    BACK("back"),

    /*
     * Packed dirt covers both sides.
     */
    BOTH("both");

    private final String serializedName;

    ArchedWindowBuried(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /*
     * Returns true when the front side contains packed dirt.
     */
    public boolean hasFrontDirt() {
        return this == FRONT || this == BOTH;
    }

    /*
     * Returns true when the back side contains packed dirt.
     */
    public boolean hasBackDirt() {
        return this == BACK || this == BOTH;
    }

    /*
     * Adds dirt to the front side.
     */
    public ArchedWindowBuried addFrontDirt() {
        return switch (this) {
            case CLEAR -> FRONT;
            case BACK -> BOTH;
            case FRONT, BOTH -> this;
        };
    }

    /*
     * Adds dirt to the back side.
     */
    public ArchedWindowBuried addBackDirt() {
        return switch (this) {
            case CLEAR -> BACK;
            case FRONT -> BOTH;
            case BACK, BOTH -> this;
        };
    }

    /*
     * Removes dirt from the front side.
     */
    public ArchedWindowBuried removeFrontDirt() {
        return switch (this) {
            case FRONT -> CLEAR;
            case BOTH -> BACK;
            case CLEAR, BACK -> this;
        };
    }

    /*
     * Removes dirt from the back side.
     */
    public ArchedWindowBuried removeBackDirt() {
        return switch (this) {
            case BACK -> CLEAR;
            case BOTH -> FRONT;
            case CLEAR, FRONT -> this;
        };
    }
}