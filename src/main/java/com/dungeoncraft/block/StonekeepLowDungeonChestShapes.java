package com.dungeoncraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Collision and interaction shapes for the complete multiblock chest.
 *
 * The rendered chest is a 23 x 10.5-model-unit rectangle rotated around the
 * same authored root pivot used by the block-entity renderer. Cardinal
 * placements reduce to two rectangular cell fragments. Diagonal placements
 * are rasterized into one-model-unit stair steps so the invisible helper
 * blocks do not behave like full cubes in the empty triangular areas.
 */
public final class StonekeepLowDungeonChestShapes {
    private static final int GRID = 64;          // Accurate hitbox
    private static final int PARTICLE_GRID = 8;  // Simpler break effect
    private static final double GRID_TO_MODEL_UNITS = 16.0D / GRID;
    private static final double EPSILON = 1.0E-7D;
    private static final double QUARTER_MODEL_UNIT = 0.25D / 16.0D;

    /*
     * Hitbox measurements are entered in Blockbench model units.
     *
     * One Minecraft block = 16 model units.
     * Quarter-unit adjustments are supported:
     *
     * .00
     * .25
     * .50
     * .75
     */
    private static final double MODEL_UNITS_PER_BLOCK = 16.0D;


    /*
     * North/South cardinal hitbox.
     *
     * Adjusting these values changes NORTH and SOUTH only.
     */
    private static final ModelBounds NORTH_SOUTH_BOUNDS =
            new ModelBounds(
                    -11.50D,   // Minimum X
                    11.50D,   // Maximum X
                    -13.00D,   // Minimum Z
                    -3.50D    // Maximum Z
            );


    /*
     * East/West cardinal hitbox.
     *
     * Adjusting these values changes EAST and WEST only.
     * Begin with the same dimensions as north/south, then tune them separately.
     */
    private static final ModelBounds EAST_WEST_BOUNDS =
            new ModelBounds(
                    -11.25D,   // Minimum X
                    11.25D,   // Maximum X
                    -13.25D,   // Minimum Z
                    -3.25D    // Maximum Z
            );


    /*
     * Diagonal hitbox.
     *
     * Adjusting these values changes only:
     * NORTH_EAST, SOUTH_EAST, SOUTH_WEST, and NORTH_WEST.
     */
    private static final ModelBounds DIAGONAL_BOUNDS =
            new ModelBounds(
                    -11.50D,
                    11.50D,
                    -13.00D,
                    -3.50D
            );


    /*
     * Clean diagonal selection outline.
     *
     * These measurements follow the actual outer edges of the chest's main
     * top and bottom frames. They are intentionally separate from the
     * collision bounds so the accurate 64-step hitbox remains unchanged.
     */
    private static final ModelBounds DIAGONAL_OUTLINE_BOUNDS =
            new ModelBounds(
                    -11.50D,   // Left outer edge
                    11.50D,    // Right outer edge
                    -13.25D,   // Front outer edge
                    -3.25D     // Back outer edge
            );


    private static final double MODEL_HEIGHT = 14.0D;

    /* Every occupied chest cell is at most one block from the master. */
    private static final VoxelShape[][][] SHAPES =
            new VoxelShape[StonekeepChestFacing.values().length][3][3];

    static {
        for (StonekeepChestFacing facing : StonekeepChestFacing.values()) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                    SHAPES[facing.ordinal()][offsetX + 1][offsetZ + 1] =
                            createCellShape(facing, offsetX, offsetZ);
                }
            }
        }
    }

    private StonekeepLowDungeonChestShapes() {
    }

    public static VoxelShape forMaster(BlockState state) {
        return forCell(state.getValue(StonekeepLowDungeonChestBlock.FACING), 0, 0);
    }

    public static VoxelShape forPart(
            BlockState partState,
            BlockGetter level,
            BlockPos partPos
    ) {
        BlockPos masterPos = StonekeepLowDungeonChestPartBlock.getMasterPos(partPos, partState);
        BlockState masterState = level.getBlockState(masterPos);

        if (!(masterState.getBlock() instanceof StonekeepLowDungeonChestBlock)) {
            return Shapes.empty();
        }

        StonekeepChestFacing facing =
                masterState.getValue(StonekeepLowDungeonChestBlock.FACING);

        int offsetX = partPos.getX() - masterPos.getX();
        int offsetZ = partPos.getZ() - masterPos.getZ();
        return forCell(facing, offsetX, offsetZ);
    }

    /**
     * Returns the four true outer corners used by the clean diagonal chest
     * selection outline.
     *
     * The outline traces the twelve main edges of the closed chest:
     * four bottom edges, four top edges, and four corner verticals.
     */
    public static DiagonalOutline diagonalOutline(
            StonekeepChestFacing facing
    ) {
        if (!facing.isDiagonal()) {
            throw new IllegalArgumentException(
                    "A clean diagonal outline requires a diagonal chest facing."
            );
        }

        List<Point> polygon = createWorldPolygon(
                facing,
                DIAGONAL_OUTLINE_BOUNDS
        );

        /*
         * createWorldPolygon() preserves the model rectangle's authored order:
         * front-left, front-right, back-right, back-left.
         */
        Point frontLeft = polygon.get(0);
        Point frontRight = polygon.get(1);
        Point backRight = polygon.get(2);
        Point backLeft = polygon.get(3);

        return new DiagonalOutline(
                new OutlinePoint(backLeft.x(), backLeft.z()),
                new OutlinePoint(frontLeft.x(), frontLeft.z()),
                new OutlinePoint(frontRight.x(), frontRight.z()),
                new OutlinePoint(backRight.x(), backRight.z()),
                MODEL_HEIGHT / MODEL_UNITS_PER_BLOCK
        );
    }

    /** Returns the local shape belonging to one cell of a placed chest. */
    public static VoxelShape forCell(
            StonekeepChestFacing facing,
            int offsetX,
            int offsetZ
    ) {
        if (offsetX < -1 || offsetX > 1 || offsetZ < -1 || offsetZ > 1) {
            return Shapes.empty();
        }
        return SHAPES[facing.ordinal()][offsetX + 1][offsetZ + 1];
    }

    private static VoxelShape createCellShape(
            StonekeepChestFacing facing,
            int cellX,
            int cellZ
    ) {
        List<Point> polygon = createWorldPolygon(facing);
        int[] rowStart = new int[GRID];
        int[] rowEnd = new int[GRID];

        for (int row = 0; row < GRID; row++) {
            rowStart[row] = -1;
            rowEnd[row] = -1;

            double stripMinZ = cellZ + row / (double) GRID;
            double stripMaxZ = cellZ + (row + 1) / (double) GRID;

            List<Point> clipped = clipZ(polygon, stripMinZ, true);
            clipped = clipZ(clipped, stripMaxZ, false);
            if (clipped.isEmpty()) {
                continue;
            }

            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            for (Point point : clipped) {
                minX = Math.min(minX, point.x());
                maxX = Math.max(maxX, point.x());
            }

            double localMin = (minX - cellX) * GRID;
            double localMax = (maxX - cellX) * GRID;
            int start = Math.max(0, (int) Math.floor(localMin + EPSILON));
            int end = Math.min(GRID, (int) Math.ceil(localMax - EPSILON));

            if (end > start) {
                rowStart[row] = start;
                rowEnd[row] = end;
            }
        }

        VoxelShape shape = Shapes.empty();
        int runStartRow = 0;
        int currentStart = rowStart[0];
        int currentEnd = rowEnd[0];

        for (int row = 1; row <= GRID; row++) {
            int nextStart = row < GRID ? rowStart[row] : -1;
            int nextEnd = row < GRID ? rowEnd[row] : -1;

            if (nextStart != currentStart || nextEnd != currentEnd) {
                if (currentStart >= 0) {
                    shape = Shapes.or(
                            shape,
                            Block.box(
                                    currentStart * GRID_TO_MODEL_UNITS,
                                    0.0D,
                                    runStartRow * GRID_TO_MODEL_UNITS,
                                    currentEnd * GRID_TO_MODEL_UNITS,
                                    MODEL_HEIGHT,
                                    row * GRID_TO_MODEL_UNITS
                            )
                    );
                }
                runStartRow = row;
                currentStart = nextStart;
                currentEnd = nextEnd;
            }
        }

        return shape.optimize();
    }

    private static List<Point> createWorldPolygon(
            StonekeepChestFacing facing
    ) {
        ModelBounds bounds = switch (facing) {
            case NORTH, SOUTH ->
                    NORTH_SOUTH_BOUNDS;

            case EAST, WEST ->
                    EAST_WEST_BOUNDS;

            case NORTH_EAST,
                 SOUTH_EAST,
                 SOUTH_WEST,
                 NORTH_WEST ->
                    DIAGONAL_BOUNDS;
        };

        return createWorldPolygon(facing, bounds);
    }

    private static List<Point> createWorldPolygon(
            StonekeepChestFacing facing,
            ModelBounds bounds
    ) {
        /*
         * Convert the easy-to-edit Blockbench model-unit measurements
         * into Minecraft block coordinates.
         */
        double minimumX =
                bounds.minimumX() / MODEL_UNITS_PER_BLOCK;

        double maximumX =
                bounds.maximumX() / MODEL_UNITS_PER_BLOCK;

        double minimumZ =
                bounds.minimumZ() / MODEL_UNITS_PER_BLOCK;

        double maximumZ =
                bounds.maximumZ() / MODEL_UNITS_PER_BLOCK;

        List<Point> local = List.of(
                new Point(minimumX, minimumZ),
                new Point(maximumX, minimumZ),
                new Point(maximumX, maximumZ),
                new Point(minimumX, maximumZ)
        );

        double radians =
                Math.toRadians(facing.modelRotationDegrees());

        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);

        double shapeOffsetX = 0.0D;
        double shapeOffsetZ = 0.0D;

        /*
         * Hitbox-only corrections.
         *
         * These do not change the rendered model or placement pivot.
         * Adjust one axis at a time in quarter-model-unit steps.
         */
        switch (facing) {
            case EAST -> {
                shapeOffsetX = 0.0D;
                shapeOffsetZ = 0.0D;
            }

            case WEST -> {
                shapeOffsetX = 0.0D;
                shapeOffsetZ = 0.0D;
            }

            default -> {
                // North, south, and diagonal shapes remain unchanged.
            }
        }

        List<Point> transformed = new ArrayList<>(4);

        for (Point point : local) {
            double worldX =
                    facing.placementPivotX()
                            + shapeOffsetX
                            + cosine * point.x()
                            + sine * point.z();

            double worldZ =
                    facing.placementPivotZ()
                            + shapeOffsetZ
                            - sine * point.x()
                            + cosine * point.z();

            transformed.add(new Point(worldX, worldZ));
        }

        return transformed;
    }

    private static List<Point> clipZ(
            List<Point> input,
            double boundary,
            boolean keepAbove
    ) {
        if (input.isEmpty()) {
            return input;
        }

        List<Point> output = new ArrayList<>(input.size() + 2);
        Point previous = input.get(input.size() - 1);
        boolean previousInside = isInside(previous.z(), boundary, keepAbove);

        for (Point current : input) {
            boolean currentInside = isInside(current.z(), boundary, keepAbove);

            if (currentInside != previousInside) {
                double difference = current.z() - previous.z();
                if (Math.abs(difference) > EPSILON) {
                    double progress = (boundary - previous.z()) / difference;
                    output.add(
                            new Point(
                                    previous.x() + progress * (current.x() - previous.x()),
                                    boundary
                            )
                    );
                }
            }

            if (currentInside) {
                output.add(current);
            }

            previous = current;
            previousInside = currentInside;
        }

        return output;
    }

    private static boolean isInside(
            double value,
            double boundary,
            boolean keepAbove
    ) {
        return keepAbove
                ? value >= boundary - EPSILON
                : value <= boundary + EPSILON;
    }

    /** One X/Z point in master-block-local coordinates. */
    public record OutlinePoint(double x, double z) {
    }

    /**
     * Clean twelve-edge outline geometry for a diagonal chest.
     *
     * The top and bottom perimeter follows:
     * back-left -> front-left -> front-right -> back-right -> back-left.
     */
    public record DiagonalOutline(
            OutlinePoint backLeft,
            OutlinePoint frontLeft,
            OutlinePoint frontRight,
            OutlinePoint backRight,
            double height
    ) {
    }

    private record ModelBounds(
            double minimumX,
            double maximumX,
            double minimumZ,
            double maximumZ
    ) {
    }

    private record Point(double x, double z) {
    }
}
