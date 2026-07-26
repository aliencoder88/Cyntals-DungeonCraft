package com.dungeoncraft.block;

// Used to create the block's position and collision shapes.
import net.minecraft.core.BlockPos;

// Converts custom enum values into names Minecraft can save
// inside block states such as "shaft", "base", "none", and "x".
import net.minecraft.util.StringRepresentable;

// Used by shape methods to examine the world.
import net.minecraft.world.level.BlockGetter;

// Minecraft's base block class.
import net.minecraft.world.level.block.Block;

// Represents the block's current saved properties.
import net.minecraft.world.level.block.state.BlockState;

// Builds the list of properties belonging to this block.
import net.minecraft.world.level.block.state.StateDefinition;

// Property type used for custom enum values.
import net.minecraft.world.level.block.state.properties.EnumProperty;

// Used when Minecraft checks the outline or collision shape.
import net.minecraft.world.phys.shapes.CollisionContext;

// Allows multiple boxes to be combined into one shape.
import net.minecraft.world.phys.shapes.Shapes;

// Minecraft's block outline and collision shape type.
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;

public class FlankedColumnBlock extends Block {

    /*
     * Saves which physical column section this block represents.
     *
     * shaft = normal middle column
     * base  = wider floor base
     * top   = wider ceiling cap
     */
    public static final EnumProperty<ColumnPart> PART =
            EnumProperty.create("part", ColumnPart.class);

    /*
     * Saves which pair of opposite blocks is flanking the column.
     *
     * none:
     * No opposite flanking pair. Only the original column model appears.
     *
     * x:
     * Blocks are east and west of the column.
     * The open viewing sides are north and south.
     *
     * z:
     * Blocks are north and south of the column.
     * The open viewing sides are east and west.
     */
    public static final EnumProperty<FlankAxis> FLANK_AXIS =
            EnumProperty.create("flank_axis", FlankAxis.class);

    /*
     * Regular shaft shape.
     *
     * These measurements are copied from ColumnBlock so the
     * Flanked Column shaft has the same outline and collision.
     */
    private static final VoxelShape SHAFT_SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 16, 12),
            Block.box(5, 0, 3, 11, 16, 4),
            Block.box(5, 0, 12, 11, 16, 13),
            Block.box(3, 0, 5, 4, 16, 11),
            Block.box(12, 0, 5, 13, 16, 11)
    );

    /*
     * Column base shape.
     *
     * These measurements are copied from ColumnBaseBlock.
     */
    private static final VoxelShape BASE_SHAPE = Shapes.or(
            // Wider base section: Y 0 through 4.
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 4.0D, 13.0D),
            Block.box(4.0D, 0.0D, 2.0D, 12.0D, 4.0D, 3.0D),
            Block.box(4.0D, 0.0D, 13.0D, 12.0D, 4.0D, 14.0D),
            Block.box(2.0D, 0.0D, 4.0D, 3.0D, 4.0D, 12.0D),
            Block.box(13.0D, 0.0D, 4.0D, 14.0D, 4.0D, 12.0D),

            // Regular shaft section above the base.
            Block.box(4.0D, 4.0D, 4.0D, 12.0D, 16.0D, 12.0D),
            Block.box(5.0D, 4.0D, 3.0D, 11.0D, 16.0D, 4.0D),
            Block.box(5.0D, 4.0D, 12.0D, 11.0D, 16.0D, 13.0D),
            Block.box(3.0D, 4.0D, 5.0D, 4.0D, 16.0D, 11.0D),
            Block.box(12.0D, 4.0D, 5.0D, 13.0D, 16.0D, 11.0D)
    );

    /*
     * Column top shape.
     *
     * These measurements are copied from ColumnTopBlock.
     */
    private static final VoxelShape TOP_SHAPE = Shapes.or(
            // Wider cap section: Y 12 through 16.
            Block.box(3.0D, 12.0D, 3.0D, 13.0D, 16.0D, 13.0D),
            Block.box(4.0D, 12.0D, 2.0D, 12.0D, 16.0D, 3.0D),
            Block.box(4.0D, 12.0D, 13.0D, 12.0D, 16.0D, 14.0D),
            Block.box(2.0D, 12.0D, 4.0D, 3.0D, 16.0D, 12.0D),
            Block.box(13.0D, 12.0D, 4.0D, 14.0D, 16.0D, 12.0D),

            // Regular shaft section below the cap.
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D),
            Block.box(5.0D, 0.0D, 3.0D, 11.0D, 12.0D, 4.0D),
            Block.box(5.0D, 0.0D, 12.0D, 11.0D, 12.0D, 13.0D),
            Block.box(3.0D, 0.0D, 5.0D, 4.0D, 12.0D, 11.0D),
            Block.box(12.0D, 0.0D, 5.0D, 13.0D, 12.0D, 11.0D)
    );

    public FlankedColumnBlock(Properties properties) {
        super(properties);

        /*
         * A newly created state defaults to:
         *
         * - regular shaft
         * - no flanking overlay
         *
         * FlankedColumnItem will change these values during placement.
         */
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(PART, ColumnPart.SHAFT)
                        .setValue(FLANK_AXIS, FlankAxis.NONE)
        );
    }

    /*
     * Gives the block the correct outline shape based on whether
     * it is currently a shaft, base, or top.
     */
    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShapeForPart(state.getValue(PART));
    }

    /*
     * Gives the block the same collision shape as its visible
     * shaft, base, or top section.
     */
    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShapeForPart(state.getValue(PART));
    }

    /*
     * Rechecks the horizontal neighbors whenever an adjacent block
     * is placed, removed, or changed.
     *
     * The column part remains fixed as shaft, base, or top.
     * Only FLANK_AXIS is updated.
     */
    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTickAccess,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        /*
         * Floor and ceiling changes do not affect flanking.
         * Only north, south, east, and west changes need to be checked.
         */
        if (direction.getAxis() != Direction.Axis.Y) {
            FlankAxis detectedAxis =
                    detectFlankAxis(level, pos);

            if (state.getValue(FLANK_AXIS) != detectedAxis) {
                state = state.setValue(
                        FLANK_AXIS,
                        detectedAxis
                );
            }
        }

        return super.updateShape(
                state,
                level,
                scheduledTickAccess,
                pos,
                direction,
                neighborPos,
                neighborState,
                random
        );
    }

    /*
     * Shows the inward-facing fill only when there are exactly
     * two sturdy horizontal neighboring faces and those two faces
     * are directly opposite one another.
     *
     * Valid arrangements:
     * - East + West  -> X
     * - North + South -> Z
     *
     * Invalid arrangements:
     * - Zero or one sturdy side
     * - Two adjacent sides
     * - Three sturdy sides
     * - Four sturdy sides
     */
    public static FlankAxis detectFlankAxis(
            LevelReader level,
            BlockPos pos
    ) {
        boolean sturdyEastFace =
                hasSturdyFaceTowardColumn(
                        level,
                        pos.east(),
                        Direction.WEST
                );

        boolean sturdyWestFace =
                hasSturdyFaceTowardColumn(
                        level,
                        pos.west(),
                        Direction.EAST
                );

        boolean sturdyNorthFace =
                hasSturdyFaceTowardColumn(
                        level,
                        pos.north(),
                        Direction.SOUTH
                );

        boolean sturdySouthFace =
                hasSturdyFaceTowardColumn(
                        level,
                        pos.south(),
                        Direction.NORTH
                );

        int sturdySideCount = 0;

        if (sturdyEastFace) {
            sturdySideCount++;
        }

        if (sturdyWestFace) {
            sturdySideCount++;
        }

        if (sturdyNorthFace) {
            sturdySideCount++;
        }

        if (sturdySouthFace) {
            sturdySideCount++;
        }

        /*
         * The inward-facing fill is allowed only when there are
         * exactly two sturdy horizontal sides.
         */
        if (sturdySideCount != 2) {
            return FlankAxis.NONE;
        }

        /*
         * Exactly two sides exist and they are east and west.
         */
        if (sturdyEastFace && sturdyWestFace) {
            return FlankAxis.X;
        }

        /*
         * Exactly two sides exist and they are north and south.
         */
        if (sturdyNorthFace && sturdySouthFace) {
            return FlankAxis.Z;
        }

        /*
         * Exactly two sides exist, but they form a corner rather
         * than an opposite flanking pair.
         */
        return FlankAxis.NONE;
    }

    /*
     * Checks the exact face of the neighboring block that touches
     * the Flanked Column.
     *
     * Examples:
     * - An east neighbor must have a sturdy west face.
     * - A north neighbor must have a sturdy south face.
     */
    private static boolean hasSturdyFaceTowardColumn(
            LevelReader level,
            BlockPos neighborPos,
            Direction faceTowardColumn
    ) {
        BlockState neighborState =
                level.getBlockState(neighborPos);

        return neighborState.isFaceSturdy(
                level,
                neighborPos,
                faceTowardColumn
        );
    }

    /*
     * Selects the correct existing column measurements.
     *
     * The inward-facing texture overlays will be visual only during
     * the first version, so they are not added to the collision shape.
     */
    private static VoxelShape getShapeForPart(ColumnPart part) {
        return switch (part) {
            case BASE -> BASE_SHAPE;
            case TOP -> TOP_SHAPE;
            case SHAFT -> SHAFT_SHAPE;
        };
    }

    /*
     * Registers the two saved block-state properties.
     */
    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(PART, FLANK_AXIS);
    }

    /*
     * The three column forms represented by this one block.
     */
    public enum ColumnPart implements StringRepresentable {
        SHAFT("shaft"),
        BASE("base"),
        TOP("top");

        private final String serializedName;

        ColumnPart(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /*
     * The orientation of the opposite flanking blocks.
     */
    public enum FlankAxis implements StringRepresentable {
        NONE("none"),
        X("x"),
        Z("z");

        private final String serializedName;

        FlankAxis(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}