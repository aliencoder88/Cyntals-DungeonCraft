package com.dungeoncraft.block.entity;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.PowerDiverterBlock;
import com.dungeoncraft.config.CodingToolAdvancedConfig;
import com.dungeoncraft.config.CodingToolConfigurable;
import com.dungeoncraft.config.CodingToolDeviceType;
import com.dungeoncraft.config.DeviceSignalMode;
import com.dungeoncraft.config.LeverPowerMode;
import com.dungeoncraft.config.PowerDiverterConfig;
import com.dungeoncraft.config.PowerDiverterPort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores four equal port modes, source-to-destination routes, and the current
 * routed redstone strengths for the Power Router.
 *
 * Regular redstone is active in this checkpoint. A lightweight route trace is
 * also carried across directly connected Power Routers so a signal cannot
 * circulate forever through a closed diverter-only loop. The future verified
 * network will carry the full key/provenance data through wire and devices.
 */
public class PowerDiverterBlockEntity
        extends BlockEntity
        implements CodingToolConfigurable {
    private static final String PORT_MODE_BITS_KEY = "port_mode_bits";
    private static final String ROUTE_BITS_KEY = "route_bits";
    private static final String FEEDBACK_BLOCKED_ROUTE_BITS_KEY =
            "feedback_blocked_route_bits";
    private static final String ROUTING_DATA_VERSION_KEY =
            "routing_data_version";
    private static final int CURRENT_ROUTING_DATA_VERSION = 2;
    private static final int MAX_DIVERTER_HOPS = 32;

    private PowerDiverterConfig config = PowerDiverterConfig.defaults();
    private final int[] inputStrengths = new int[4];
    private final int[] outputStrengths = new int[4];
    private final RoutedSignal[] inputSignals = new RoutedSignal[4];
    private final RoutedSignal[] outputSignals = new RoutedSignal[4];
    private boolean recalculating;
    private boolean initialRecalculationPending = true;

    public PowerDiverterBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonCraft.POWER_DIVERTER_BLOCK_ENTITY, pos, state);
    }

    /**
     * Runs only until the first server-side calculation after creation/load.
     * Scheduled block ticks handle later neighbor changes.
     */
    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            PowerDiverterBlockEntity blockEntity
    ) {
        if (level.isClientSide()
                || !blockEntity.initialRecalculationPending) {
            return;
        }

        blockEntity.recalculate(level, pos, state);
    }

    public PowerDiverterConfig getPowerDiverterConfig() {
        return this.config;
    }

    public int getInputStrength(PowerDiverterPort port) {
        return this.inputStrengths[port.getIndex()];
    }

    public int getOutputStrength(PowerDiverterPort port) {
        return this.outputStrengths[port.getIndex()];
    }

    /**
     * Used only by an adjacent Power Router. Vanilla redstone consumers still
     * receive the ordinary strength through PowerDiverterBlock#getSignal.
     */
    public RoutedSignal getRoutedOutput(PowerDiverterPort port) {
        return this.outputSignals[port.getIndex()];
    }

    public void recalculate(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || this.recalculating) {
            return;
        }

        this.recalculating = true;
        this.initialRecalculationPending = false;

        try {
            RoutedSignal[] newInputs = new RoutedSignal[4];
            RoutedSignal[] newOutputs = new RoutedSignal[4];
            int[] newInputStrengths = new int[4];
            int[] newOutputStrengths = new int[4];

            for (PowerDiverterPort inputPort
                    : PowerDiverterPort.values()) {
                if (!this.config.canAcceptInput(inputPort)) {
                    continue;
                }

                RoutedSignal incoming = this.readIncomingSignal(
                        level,
                        pos,
                        inputPort
                );
                newInputs[inputPort.getIndex()] = incoming;
                newInputStrengths[inputPort.getIndex()] =
                        incoming == null ? 0 : incoming.strength();
            }

            for (PowerDiverterPort destination
                    : PowerDiverterPort.values()) {
                if (!this.config.canEmitOutput(destination)) {
                    continue;
                }

                RoutedSignal strongest = null;

                for (PowerDiverterPort source
                        : PowerDiverterPort.values()) {
                    if (source == destination
                            || !this.config.isRouteEffective(
                                    source,
                                    destination
                            )) {
                        continue;
                    }

                    RoutedSignal incoming = newInputs[source.getIndex()];
                    if (incoming == null || incoming.strength() <= 0) {
                        continue;
                    }

                    RoutedSignal routed = incoming.through(
                            this.worldPosition.asLong(),
                            MAX_DIVERTER_HOPS
                    );
                    strongest = chooseStronger(strongest, routed);
                }

                newOutputs[destination.getIndex()] = strongest;
                newOutputStrengths[destination.getIndex()] =
                        strongest == null ? 0 : strongest.strength();
            }

            boolean inputChanged = !arraysEqual(
                    this.inputStrengths,
                    newInputStrengths
            );
            boolean outputChanged = !arraysEqual(
                    this.outputStrengths,
                    newOutputStrengths
            );

            System.arraycopy(
                    newInputStrengths,
                    0,
                    this.inputStrengths,
                    0,
                    this.inputStrengths.length
            );
            System.arraycopy(
                    newOutputStrengths,
                    0,
                    this.outputStrengths,
                    0,
                    this.outputStrengths.length
            );
            System.arraycopy(
                    newInputs,
                    0,
                    this.inputSignals,
                    0,
                    this.inputSignals.length
            );
            System.arraycopy(
                    newOutputs,
                    0,
                    this.outputSignals,
                    0,
                    this.outputSignals.length
            );

            if (inputChanged || outputChanged) {
                /* Runtime strengths are server-side routing state. */
                super.setChanged();
            }

            if (outputChanged) {
                PowerDiverterBlock.notifyAllPorts(level, pos, state);
            }
        } finally {
            this.recalculating = false;
        }
    }

    private RoutedSignal readIncomingSignal(
            Level level,
            BlockPos pos,
            PowerDiverterPort inputPort
    ) {
        BlockPos neighborPos = pos.relative(inputPort.getDirection());

        if (level.getBlockEntity(neighborPos)
                instanceof PowerDiverterBlockEntity adjacentDiverter) {
            PowerDiverterPort adjacentOutputPort =
                    PowerDiverterPort.fromDirection(
                            inputPort.getDirection().getOpposite()
                    );
            RoutedSignal adjacentSignal =
                    adjacentDiverter.getRoutedOutput(adjacentOutputPort);

            if (adjacentSignal == null
                    || adjacentSignal.strength() <= 0
                    || adjacentSignal.visitedDiverters().contains(
                            this.worldPosition.asLong()
                    )
                    || adjacentSignal.remainingHops() <= 0) {
                return null;
            }

            return adjacentSignal;
        }

        int strength = Mth.clamp(
                level.getSignal(
                        neighborPos,
                        inputPort.getDirection()
                ),
                0,
                15
        );

        return strength <= 0
                ? null
                : RoutedSignal.external(strength, MAX_DIVERTER_HOPS);
    }

    private static RoutedSignal chooseStronger(
            RoutedSignal current,
            RoutedSignal candidate
    ) {
        if (candidate == null) {
            return current;
        }

        if (current == null
                || candidate.strength() > current.strength()) {
            return candidate;
        }

        if (candidate.strength() == current.strength()
                && candidate.visitedDiverters().size()
                < current.visitedDiverters().size()) {
            return candidate;
        }

        return current;
    }

    private static boolean arraysEqual(int[] first, int[] second) {
        if (first.length != second.length) {
            return false;
        }

        for (int index = 0; index < first.length; index++) {
            if (first[index] != second[index]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public CodingToolDeviceType getCodingToolDeviceType() {
        return CodingToolDeviceType.POWER_DIVERTER;
    }

    @Override
    public DeviceSignalMode getSignalMode() {
        return DeviceSignalMode.REGULAR_REDSTONE;
    }

    @Override
    public LeverPowerMode getLeverPowerMode() {
        return LeverPowerMode.ACTIVATED_IS_ON;
    }

    @Override
    public int getOutputFaceMask() {
        return 0;
    }

    @Override
    public CodingToolAdvancedConfig getAdvancedConfig() {
        return new CodingToolAdvancedConfig(
                "",
                null,
                null,
                null,
                0,
                "",
                this.config
        );
    }

    @Override
    public void applyCodingToolConfiguration(
            DeviceSignalMode signalMode,
            LeverPowerMode leverPowerMode,
            int outputFaceMask,
            CodingToolAdvancedConfig advancedConfig
    ) {
        CodingToolAdvancedConfig sanitized =
                advancedConfig.sanitizedFor(
                        CodingToolDeviceType.POWER_DIVERTER
                );
        PowerDiverterConfig updatedConfig =
                sanitized.powerDiverterConfig().normalizedForSave();

        if (this.config.equals(updatedConfig)) {
            return;
        }

        this.config = updatedConfig;
        this.setChanged();

        if (this.level != null) {
            this.recalculate(
                    this.level,
                    this.worldPosition,
                    this.getBlockState()
            );
            PowerDiverterBlock.notifyAllPorts(
                    this.level,
                    this.worldPosition,
                    this.getBlockState()
            );
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putInt(PORT_MODE_BITS_KEY, this.config.portModeBits());
        output.putInt(ROUTE_BITS_KEY, this.config.routeBits());
        output.putInt(
                FEEDBACK_BLOCKED_ROUTE_BITS_KEY,
                this.config.feedbackBlockedRouteBits()
        );
        output.putInt(
                ROUTING_DATA_VERSION_KEY,
                CURRENT_ROUTING_DATA_VERSION
        );
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int storedRouteBits = input.getIntOr(
                ROUTE_BITS_KEY,
                PowerDiverterConfig.defaults().routeBits()
        );
        int routingDataVersion = input.getIntOr(
                ROUTING_DATA_VERSION_KEY,
                0
        );
        int loadedRouteBits = routingDataVersion
                < CURRENT_ROUTING_DATA_VERSION
                ? PowerDiverterConfig.migrateLegacyDefaultRoutes(
                        storedRouteBits
                )
                : storedRouteBits;

        this.config = new PowerDiverterConfig(
                input.getIntOr(
                        PORT_MODE_BITS_KEY,
                        PowerDiverterConfig.defaults().portModeBits()
                ),
                loadedRouteBits,
                input.getIntOr(
                        FEEDBACK_BLOCKED_ROUTE_BITS_KEY,
                        PowerDiverterConfig.defaults()
                                .feedbackBlockedRouteBits()
                )
        );
        this.initialRecalculationPending = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return this.saveWithoutMetadata(registryLookup);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level == null) {
            return;
        }

        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(
                this.worldPosition,
                state,
                state,
                Block.UPDATE_ALL
        );
    }

    /**
     * In-memory route metadata for directly connected diverters.
     *
     * The verified network will later extend this concept with signal type,
     * assigned key/code, source identity, previous port, and a stable signal ID.
     */
    public record RoutedSignal(
            int strength,
            Set<Long> visitedDiverters,
            int remainingHops
    ) {
        public RoutedSignal {
            strength = Mth.clamp(strength, 0, 15);
            visitedDiverters = Set.copyOf(visitedDiverters);
            remainingHops = Math.max(0, remainingHops);
        }

        public static RoutedSignal external(
                int strength,
                int maximumHops
        ) {
            return new RoutedSignal(
                    strength,
                    Set.of(),
                    maximumHops
            );
        }

        public RoutedSignal through(
                long diverterPosition,
                int maximumHops
        ) {
            if (this.visitedDiverters.contains(diverterPosition)
                    || this.remainingHops <= 0) {
                return null;
            }

            Set<Long> updatedVisited =
                    new HashSet<>(this.visitedDiverters);
            updatedVisited.add(diverterPosition);

            return new RoutedSignal(
                    this.strength,
                    updatedVisited,
                    Math.min(maximumHops, this.remainingHops - 1)
            );
        }
    }
}
