package com.dungeoncraft.mixin.client;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.block.StonekeepChestFacing;
import com.dungeoncraft.block.StonekeepLowDungeonChestBlock;
import com.dungeoncraft.block.StonekeepLowDungeonChestShapes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Replaces Minecraft's detailed final destruction effect for diagonal
 * Stonekeep chests with a small fixed particle count across all three cells.
 *
 * This targets ClientLevel.addDestroyBlockEffect directly because that method
 * is called by LevelEventHandler, not by MultiPlayerGameMode.destroyBlock.
 */
@Mixin(ClientLevel.class)
public abstract class StonekeepChestDiagonalDestroyParticleMixin {

    /**
     * Final particles emitted in EACH occupied diagonal cell.
     *
     * A diagonal chest has three cells:
     * 4 per cell = about 12 particles total.
     */
    private static final int DIAGONAL_PARTICLES_PER_CELL = 16;

    @Inject(
            method = "addDestroyBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dungeoncraft$replaceDiagonalDestroyEffect(
            BlockPos masterPos,
            BlockState masterState,
            CallbackInfo callbackInfo
    ) {
        if (!masterState.is(DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST)) {
            return;
        }

        StonekeepChestFacing facing =
                masterState.getValue(StonekeepLowDungeonChestBlock.FACING);

        if (!facing.isDiagonal()) {
            return;
        }

        ClientLevel level = (ClientLevel) (Object) this;

        dungeoncraft$addParticlesForShape(
                level,
                masterPos,
                masterState,
                StonekeepLowDungeonChestShapes.forMaster(masterState)
        );

        for (Direction helperDirection : facing.helperDirections()) {
            int offsetX = helperDirection.getStepX();
            int offsetZ = helperDirection.getStepZ();
            BlockPos helperPos = masterPos.relative(helperDirection);

            dungeoncraft$addParticlesForShape(
                    level,
                    helperPos,
                    masterState,
                    StonekeepLowDungeonChestShapes.forCell(
                            facing,
                            offsetX,
                            offsetZ
                    )
            );
        }

        /* Prevent Minecraft from also processing the 64-step master shape. */
        callbackInfo.cancel();
    }

    /** Adds a fixed number of particles inside one local chest-cell shape. */
    private static void dungeoncraft$addParticlesForShape(
            ClientLevel level,
            BlockPos cellPos,
            BlockState particleState,
            VoxelShape shape
    ) {
        List<AABB> boxes = shape.toAabbs();
        if (boxes.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        BlockParticleOption particle =
                new BlockParticleOption(ParticleTypes.BLOCK, particleState);

        for (int particleIndex = 0;
             particleIndex < DIAGONAL_PARTICLES_PER_CELL;
             particleIndex++) {
            AABB box = boxes.get(random.nextInt(boxes.size()));

            double x =
                    cellPos.getX()
                            + dungeoncraft$randomBetween(random, box.minX, box.maxX);

            double y =
                    cellPos.getY()
                            + dungeoncraft$randomBetween(random, box.minY, box.maxY);

            double z =
                    cellPos.getZ()
                            + dungeoncraft$randomBetween(random, box.minZ, box.maxZ);

            double velocityX = (random.nextDouble() - 0.5D) * 0.14D;
            double velocityY = 0.04D + random.nextDouble() * 0.10D;
            double velocityZ = (random.nextDouble() - 0.5D) * 0.14D;

            level.addParticle(
                    particle,
                    x,
                    y,
                    z,
                    velocityX,
                    velocityY,
                    velocityZ
            );
        }
    }

    private static double dungeoncraft$randomBetween(
            ThreadLocalRandom random,
            double minimum,
            double maximum
    ) {
        if (maximum <= minimum) {
            return minimum;
        }

        return minimum
                + random.nextDouble() * (maximum - minimum);
    }
}
