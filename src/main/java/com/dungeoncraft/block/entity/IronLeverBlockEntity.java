package com.dungeoncraft.block.entity;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.IronLeverBlock;
import com.dungeoncraft.config.CodingToolAdvancedConfig;
import com.dungeoncraft.config.CodingToolConfigurable;
import com.dungeoncraft.config.CodingToolDeviceType;
import com.dungeoncraft.config.DeviceSignalMode;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Saves Coding Tool settings for an individual Iron Lever.
 */
public class IronLeverBlockEntity
        extends BlockEntity
        implements CodingToolConfigurable {
    private static final String SIGNAL_MODE_KEY = "signal_mode";
    private static final String LEVER_POWER_MODE_KEY = "lever_power_mode";
    private static final String VERIFIED_OUTPUT_KEY = "verified_output_key";

    private DeviceSignalMode signalMode = DeviceSignalMode.REGULAR_REDSTONE;
    private LeverPowerMode leverPowerMode = LeverPowerMode.ACTIVATED_IS_ON;
    private String verifiedOutputKey = "";

    public IronLeverBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonCraft.IRON_LEVER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public CodingToolDeviceType getCodingToolDeviceType() {
        return CodingToolDeviceType.IRON_LEVER;
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
        return 0;
    }

    @Override
    public CodingToolAdvancedConfig getAdvancedConfig() {
        return new CodingToolAdvancedConfig(
                this.verifiedOutputKey,
                HiddenPanelWiringMode.UNLOCKED,
                LockSignalPolarity.NO_POWER_IS_LOCKED,
                DeviceSignalMode.REGULAR_REDSTONE,
                0,
                "",
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
        CodingToolAdvancedConfig sanitizedAdvancedConfig =
                advancedConfig.sanitizedFor(
                        CodingToolDeviceType.IRON_LEVER
                );

        if (this.signalMode == signalMode
                && this.leverPowerMode == leverPowerMode
                && this.verifiedOutputKey.equals(
                        sanitizedAdvancedConfig.verifiedOutputKey()
                )) {
            return;
        }

        this.signalMode = signalMode;
        this.leverPowerMode = leverPowerMode;
        this.verifiedOutputKey =
                sanitizedAdvancedConfig.verifiedOutputKey();
        this.setChanged();

        if (this.level != null) {
            IronLeverBlock.notifyConfiguredOutputNeighbors(
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
        output.putString(VERIFIED_OUTPUT_KEY, this.verifiedOutputKey);

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
        this.verifiedOutputKey = VerifiedSignalKey.sanitize(
                input.getStringOr(VERIFIED_OUTPUT_KEY, "")
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
