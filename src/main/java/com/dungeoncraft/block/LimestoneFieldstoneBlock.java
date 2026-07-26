package com.dungeoncraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class LimestoneFieldstoneBlock extends Block {

    public static final TagKey<Item> IRON_LEVEL_PICKAXES =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath("dungeoncraft", "iron_level_pickaxes")
            );

    public static final TagKey<Item> IRON_LEVEL_BACKUP_AXES =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath("dungeoncraft", "iron_level_backup_axes")
            );

    public static final TagKey<Item> IRON_LEVEL_BACKUP_SWORDS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath("dungeoncraft", "iron_level_backup_swords")
            );

    public static final TagKey<Item> IRON_LEVEL_BACKUP_SHOVELS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath("dungeoncraft", "iron_level_backup_shovels")
            );

    public static final TagKey<Item> IRON_LEVEL_BACKUP_HOES =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath("dungeoncraft", "iron_level_backup_hoes")
            );

    public LimestoneFieldstoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float normalBreakingSpeed = super.getDestroyProgress(state, player, level, pos);

        if (player.isCreative()) {
            return normalBreakingSpeed;
        }

        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.is(IRON_LEVEL_PICKAXES)) {
            return normalBreakingSpeed;
        }

        if (heldItem.is(IRON_LEVEL_BACKUP_AXES)) {
            return normalBreakingSpeed * 0.80F;
        }

        if (heldItem.is(IRON_LEVEL_BACKUP_SWORDS)) {
            return normalBreakingSpeed * 0.40F;
        }

        if (heldItem.is(IRON_LEVEL_BACKUP_SHOVELS)) {
            return normalBreakingSpeed * 0.30F;
        }

        if (heldItem.is(IRON_LEVEL_BACKUP_HOES)) {
            return normalBreakingSpeed * 0.20F;
        }

        return 0.0F;
    }
}