package com.dungeoncraft.block;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/**
 * The eight placement-only facings supported by the Stonekeep chest.
 *
 * The ordinal order is intentional: every step advances 45 degrees clockwise
 * from north.  This makes placement yaw and renderer rotation calculations
 * compact and predictable.
 */
public enum StonekeepChestFacing implements StringRepresentable {
    NORTH("north", 0.0F, new Direction[]{Direction.EAST}, 1.0F, 1.0F),
    NORTH_EAST("north_east", 315.0F, new Direction[]{Direction.NORTH, Direction.EAST}, 0.390625F, 0.609375F),
    EAST("east", 270.0F, new Direction[]{Direction.SOUTH}, 0.0F, 1.0F),
    SOUTH_EAST("south_east", 225.0F, new Direction[]{Direction.SOUTH, Direction.EAST}, 0.390625F, 0.390625F),
    SOUTH("south", 180.0F, new Direction[]{Direction.WEST}, 0.0F, 0.0F),
    SOUTH_WEST("south_west", 135.0F, new Direction[]{Direction.SOUTH, Direction.WEST}, 0.609375F, 0.390625F),
    WEST("west", 90.0F, new Direction[]{Direction.NORTH}, 1.0F, 0.0F),
    NORTH_WEST("north_west", 45.0F, new Direction[]{Direction.NORTH, Direction.WEST}, 0.609375F, 0.609375F);

    private static final StonekeepChestFacing[] VALUES = values();

    private final String serializedName;
    private final float modelRotationDegrees;
    private final Direction[] helperDirections;
    private final float placementPivotX;
    private final float placementPivotZ;

    StonekeepChestFacing(
            String serializedName,
            float modelRotationDegrees,
            Direction[] helperDirections,
            float placementPivotX,
            float placementPivotZ
    ) {
        this.serializedName = serializedName;
        this.modelRotationDegrees = modelRotationDegrees;
        this.helperDirections = helperDirections;
        this.placementPivotX = placementPivotX;
        this.placementPivotZ = placementPivotZ;
    }

    /**
     * Converts player yaw into the chest front that faces back toward the
     * placing player.  Minecraft yaw 0 looks south, so the enum's north-first
     * ordering already represents the required opposite direction.
     */
    public static StonekeepChestFacing fromPlacementYaw(float yawDegrees) {
        int index = Mth.floor((yawDegrees + 22.5F) / 45.0F) & 7;
        return VALUES[index];
    }

    public boolean isDiagonal() {
        return (this.ordinal() & 1) == 1;
    }

    /**
     * Cardinal placements return one helper direction; diagonal placements
     * return two.  The unlisted fourth diagonal cell remains completely free.
     */
    public Direction[] helperDirections() {
        return this.helperDirections.clone();
    }

    /** Rotation used by PoseStack for a model authored facing north. */
    public float modelRotationDegrees() {
        return this.modelRotationDegrees;
    }

    /**
     * World-local corner/boundary where the Blockbench root pivot is placed,
     * measured within the master block from 0.0 to 1.0.
     */
    public float placementPivotX() {
        return this.placementPivotX;
    }

    public float placementPivotZ() {
        return this.placementPivotZ;
    }

    public StonekeepChestFacing rotate(Rotation rotation) {
        int quarterTurns = switch (rotation) {
            case CLOCKWISE_90 -> 2;
            case CLOCKWISE_180 -> 4;
            case COUNTERCLOCKWISE_90 -> 6;
            default -> 0;
        };
        return VALUES[(this.ordinal() + quarterTurns) & 7];
    }

    public StonekeepChestFacing mirror(Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> switch (this) {
                case NORTH -> SOUTH;
                case NORTH_EAST -> SOUTH_EAST;
                case EAST -> EAST;
                case SOUTH_EAST -> NORTH_EAST;
                case SOUTH -> NORTH;
                case SOUTH_WEST -> NORTH_WEST;
                case WEST -> WEST;
                case NORTH_WEST -> SOUTH_WEST;
            };
            case FRONT_BACK -> switch (this) {
                case NORTH -> NORTH;
                case NORTH_EAST -> NORTH_WEST;
                case EAST -> WEST;
                case SOUTH_EAST -> SOUTH_WEST;
                case SOUTH -> SOUTH;
                case SOUTH_WEST -> SOUTH_EAST;
                case WEST -> EAST;
                case NORTH_WEST -> NORTH_EAST;
            };
            default -> this;
        };
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
