package com.dungeoncraft.block.entity;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.HiddenBlockLeverBlock;
import com.dungeoncraft.config.CodingToolAdvancedConfig;
import com.dungeoncraft.config.CodingToolConfigurable;
import com.dungeoncraft.config.CodingToolDeviceType;
import com.dungeoncraft.config.DeviceSignalMode;
import com.dungeoncraft.config.HiddenLeverOutputFace;
import com.dungeoncraft.config.HiddenPanelWiringMode;
import com.dungeoncraft.config.LeverPowerMode;
import com.dungeoncraft.config.LockSignalPolarity;
import com.dungeoncraft.config.PowerDiverterConfig;
import com.dungeoncraft.config.VerifiedSignalKey;

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

/**
 * Stores animation, output routing, and concealed-panel wiring configuration
 * for the Stonekeep Hidden Block Lever.
 */
public class HiddenBlockLeverBlockEntity
        extends BlockEntity
        implements CodingToolConfigurable {
    private static final String SIGNAL_MODE_KEY = "signal_mode";
    private static final String LEVER_POWER_MODE_KEY = "lever_power_mode";
    private static final String OUTPUT_FACE_MASK_KEY = "output_face_mask";
    private static final String VERIFIED_OUTPUT_KEY = "verified_output_key";
    private static final String PANEL_WIRING_MODE_KEY = "panel_wiring_mode";
    private static final String LOCK_SIGNAL_POLARITY_KEY =
            "lock_signal_polarity";
    private static final String PANEL_SIGNAL_MODE_KEY = "panel_signal_mode";
    private static final String PANEL_INPUT_FACE_MASK_KEY =
            "panel_input_face_mask";
    private static final String PANEL_REQUIRED_KEY_KEY = "panel_required_key";

    private static final float PANEL_STEP = 1.0F / 12.0F;
    private static final float LEVER_STEP = 1.0F / 6.0F;

    private float previousPanelProgress;
    private float panelProgress;
    private float previousLeverProgress;
    private float leverProgress;

    private DeviceSignalMode signalMode =
            DeviceSignalMode.REGULAR_REDSTONE;
    private LeverPowerMode leverPowerMode =
            LeverPowerMode.ACTIVATED_IS_ON;
    private int outputFaceMask = HiddenLeverOutputFace.defaultMask();

    private String verifiedOutputKey = "";
    private HiddenPanelWiringMode panelWiringMode =
            HiddenPanelWiringMode.UNLOCKED;
    private LockSignalPolarity lockSignalPolarity =
            LockSignalPolarity.NO_POWER_IS_LOCKED;
    private DeviceSignalMode panelSignalMode =
            DeviceSignalMode.REGULAR_REDSTONE;
    private int panelInputFaceMask;
    private String panelRequiredKey = "";
    private boolean panelWiringInitialized;

    public HiddenBlockLeverBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonCraft.HIDDEN_BLOCK_LEVER_BLOCK_ENTITY, pos, state);

        this.panelProgress = state.getValue(HiddenBlockLeverBlock.PANEL_OPEN)
                ? 1.0F
                : 0.0F;
        this.previousPanelProgress = this.panelProgress;

        this.leverProgress = state.getValue(HiddenBlockLeverBlock.POWERED)
                ? 1.0F
                : 0.0F;
        this.previousLeverProgress = this.leverProgress;
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            HiddenBlockLeverBlockEntity blockEntity
    ) {
        blockEntity.previousPanelProgress = blockEntity.panelProgress;
        blockEntity.previousLeverProgress = blockEntity.leverProgress;

        float panelTarget = state.getValue(HiddenBlockLeverBlock.PANEL_OPEN)
                ? 1.0F
                : 0.0F;
        float leverTarget = state.getValue(HiddenBlockLeverBlock.POWERED)
                ? 1.0F
                : 0.0F;

        blockEntity.panelProgress = approach(
                blockEntity.panelProgress,
                panelTarget,
                PANEL_STEP
        );
        blockEntity.leverProgress = approach(
                blockEntity.leverProgress,
                leverTarget,
                LEVER_STEP
        );

        if (!level.isClientSide()) {
            if (!blockEntity.panelWiringInitialized) {
                blockEntity.panelWiringInitialized = true;
                HiddenBlockLeverBlock.updatePanelFromWiring(
                        level,
                        pos,
                        state
                );

                state = level.getBlockState(pos);
            }

            boolean panelOpen = state.getValue(HiddenBlockLeverBlock.PANEL_OPEN);
            boolean renderClosed = state.getValue(
                    HiddenBlockLeverBlock.RENDER_CLOSED
            );

            if (!panelOpen
                    && !renderClosed
                    && blockEntity.panelProgress <= 0.0001F) {
                level.setBlock(
                        pos,
                        state.setValue(
                                HiddenBlockLeverBlock.RENDER_CLOSED,
                                true
                        ),
                        Block.UPDATE_ALL
                );
            } else if (panelOpen && renderClosed) {
                level.setBlock(
                        pos,
                        state.setValue(
                                HiddenBlockLeverBlock.RENDER_CLOSED,
                                false
                        ),
                        Block.UPDATE_ALL
                );
            }
        }
    }

    private static float approach(float current, float target, float step) {
        if (current < target) {
            return Math.min(current + step, target);
        }

        if (current > target) {
            return Math.max(current - step, target);
        }

        return current;
    }

    public float getPanelProgress(float tickProgress) {
        return Mth.lerp(
                tickProgress,
                this.previousPanelProgress,
                this.panelProgress
        );
    }

    public float getLeverProgress(float tickProgress) {
        return Mth.lerp(
                tickProgress,
                this.previousLeverProgress,
                this.leverProgress
        );
    }

    public boolean isPanelFullyOpen() {
        return this.panelProgress >= 0.999F;
    }

    @Override
    public CodingToolDeviceType getCodingToolDeviceType() {
        return CodingToolDeviceType.HIDDEN_BLOCK_LEVER;
    }

    @Override
    public DeviceSignalMode getSignalMode() {
        return this.signalMode;
    }

    @Override
    public LeverPowerMode getLeverPowerMode() {
        return this.leverPowerMode;
    }

    public boolean isElectricallyOn(boolean physicallyActivated) {
        return this.leverPowerMode.isElectricallyOn(physicallyActivated);
    }

    @Override
    public int getOutputFaceMask() {
        return this.outputFaceMask;
    }

    public String getVerifiedOutputKey() {
        return this.verifiedOutputKey;
    }

    public HiddenPanelWiringMode getPanelWiringMode() {
        return this.panelWiringMode;
    }

    public LockSignalPolarity getLockSignalPolarity() {
        return this.lockSignalPolarity;
    }

    public DeviceSignalMode getPanelSignalMode() {
        return this.panelSignalMode;
    }

    public int getPanelInputFaceMask() {
        return this.panelInputFaceMask;
    }

    public String getPanelRequiredKey() {
        return this.panelRequiredKey;
    }

    public boolean isPanelLocked(boolean authorizationSignalPresent) {
        return this.panelWiringMode != HiddenPanelWiringMode.UNLOCKED
                && this.lockSignalPolarity.isLocked(
                        authorizationSignalPresent
                );
    }

    @Override
    public CodingToolAdvancedConfig getAdvancedConfig() {
        return new CodingToolAdvancedConfig(
                this.verifiedOutputKey,
                this.panelWiringMode,
                this.lockSignalPolarity,
                this.panelSignalMode,
                this.panelInputFaceMask,
                this.panelRequiredKey,
                PowerDiverterConfig.defaults()
        );
    }

    @Override
    public void applyCodingToolConfiguration(
            DeviceSignalMode signalMode,
            LeverPowerMode leverPowerMode,
            int outputFaceMask,
            CodingToolAdvancedConfig advancedConfig
    ) {
        int sanitizedOutputFaceMask =
                HiddenLeverOutputFace.sanitizeMask(outputFaceMask);
        CodingToolAdvancedConfig sanitizedAdvancedConfig =
                advancedConfig.sanitizedFor(
                        CodingToolDeviceType.HIDDEN_BLOCK_LEVER
                );

        if (this.signalMode == signalMode
                && this.leverPowerMode == leverPowerMode
                && this.outputFaceMask == sanitizedOutputFaceMask
                && this.getAdvancedConfig().equals(
                        sanitizedAdvancedConfig
                )) {
            return;
        }

        this.signalMode = signalMode;
        this.leverPowerMode = leverPowerMode;
        this.outputFaceMask = sanitizedOutputFaceMask;
        this.verifiedOutputKey =
                sanitizedAdvancedConfig.verifiedOutputKey();
        this.panelWiringMode =
                sanitizedAdvancedConfig.panelWiringMode();
        this.lockSignalPolarity =
                sanitizedAdvancedConfig.lockSignalPolarity();
        this.panelSignalMode =
                sanitizedAdvancedConfig.panelSignalMode();
        this.panelInputFaceMask =
                sanitizedAdvancedConfig.panelInputFaceMask();
        this.panelRequiredKey =
                sanitizedAdvancedConfig.panelRequiredKey();
        this.setChanged();

        if (this.level != null) {
            HiddenBlockLeverBlock.notifyConfiguredOutputNeighbors(
                    this.level,
                    this.worldPosition,
                    this.getBlockState()
            );
            HiddenBlockLeverBlock.updatePanelFromWiring(
                    this.level,
                    this.worldPosition,
                    this.getBlockState()
            );
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putString(
                SIGNAL_MODE_KEY,
                this.signalMode.getSerializedName()
        );
        output.putString(
                LEVER_POWER_MODE_KEY,
                this.leverPowerMode.getSerializedName()
        );
        output.putInt(OUTPUT_FACE_MASK_KEY, this.outputFaceMask);
        output.putString(VERIFIED_OUTPUT_KEY, this.verifiedOutputKey);
        output.putString(
                PANEL_WIRING_MODE_KEY,
                this.panelWiringMode.getSerializedName()
        );
        output.putString(
                LOCK_SIGNAL_POLARITY_KEY,
                this.lockSignalPolarity.getSerializedName()
        );
        output.putString(
                PANEL_SIGNAL_MODE_KEY,
                this.panelSignalMode.getSerializedName()
        );
        output.putInt(
                PANEL_INPUT_FACE_MASK_KEY,
                this.panelInputFaceMask
        );
        output.putString(PANEL_REQUIRED_KEY_KEY, this.panelRequiredKey);

        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.signalMode = DeviceSignalMode.fromSerializedName(
                input.getStringOr(
                        SIGNAL_MODE_KEY,
                        DeviceSignalMode.REGULAR_REDSTONE.getSerializedName()
                )
        );
        this.leverPowerMode = LeverPowerMode.fromSerializedName(
                input.getStringOr(
                        LEVER_POWER_MODE_KEY,
                        LeverPowerMode.ACTIVATED_IS_ON.getSerializedName()
                )
        );
        this.outputFaceMask = HiddenLeverOutputFace.sanitizeMask(
                input.getIntOr(
                        OUTPUT_FACE_MASK_KEY,
                        HiddenLeverOutputFace.defaultMask()
                )
        );
        this.verifiedOutputKey = VerifiedSignalKey.sanitize(
                input.getStringOr(VERIFIED_OUTPUT_KEY, "")
        );
        this.panelWiringMode = HiddenPanelWiringMode.fromSerializedName(
                input.getStringOr(
                        PANEL_WIRING_MODE_KEY,
                        HiddenPanelWiringMode.UNLOCKED.getSerializedName()
                )
        );
        this.lockSignalPolarity = LockSignalPolarity.fromSerializedName(
                input.getStringOr(
                        LOCK_SIGNAL_POLARITY_KEY,
                        LockSignalPolarity.NO_POWER_IS_LOCKED
                                .getSerializedName()
                )
        );
        this.panelSignalMode = DeviceSignalMode.fromSerializedName(
                input.getStringOr(
                        PANEL_SIGNAL_MODE_KEY,
                        DeviceSignalMode.REGULAR_REDSTONE.getSerializedName()
                )
        );
        this.panelInputFaceMask = HiddenLeverOutputFace.sanitizeMask(
                input.getIntOr(
                        PANEL_INPUT_FACE_MASK_KEY,
                        0
                )
        );
        this.panelRequiredKey = VerifiedSignalKey.sanitize(
                input.getStringOr(PANEL_REQUIRED_KEY_KEY, "")
        );
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
}
