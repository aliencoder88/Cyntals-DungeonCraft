package com.dungeoncraft;

import net.fabricmc.api.ModInitializer;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

// Custom Tab imports
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

// DungeonCraft creation imports
import com.dungeoncraft.block.AgedLimestoneBricksBlock;
import com.dungeoncraft.block.LimestoneFieldstoneBlock;
import com.dungeoncraft.block.AgedLimestoneBlock;
import com.dungeoncraft.block.ColumnBlock;
import com.dungeoncraft.block.ColumnBaseBlock;
import com.dungeoncraft.block.ColumnTopBlock;
import com.dungeoncraft.block.FlankedColumnBlock;
import com.dungeoncraft.item.FlankedColumnItem;
import com.dungeoncraft.block.ArchedWindowBlock;
import com.dungeoncraft.item.ArchedWindowItem;
import com.dungeoncraft.block.ArchedWindowBuried;
import com.dungeoncraft.item.BuriedArchedWindowItem;
import com.dungeoncraft.block.IronPocketDoorBlock;
import com.dungeoncraft.item.IronPocketDoorItem;
import com.dungeoncraft.block.IronLeverBlock;
import com.dungeoncraft.block.HiddenBlockLeverBlock;
import com.dungeoncraft.block.PowerDiverterBlock;
import com.dungeoncraft.block.entity.HiddenBlockLeverBlockEntity;
import com.dungeoncraft.block.entity.IronLeverBlockEntity;
import com.dungeoncraft.block.entity.PowerDiverterBlockEntity;
import com.dungeoncraft.block.CopperWireBlock;
import com.dungeoncraft.block.EmbeddedCopperWireBlock;
import com.dungeoncraft.block.CoveredEmbeddedCopperWireBlock;
import com.dungeoncraft.block.entity.CoveredEmbeddedCopperWireBlockEntity;
import com.dungeoncraft.menu.BasicTreasureChestMenu;
import com.dungeoncraft.block.BasicTreasureChestBlock;
import com.dungeoncraft.block.entity.BasicTreasureChestBlockEntity;
import com.dungeoncraft.block.StonekeepLowDungeonChestBlock;
import com.dungeoncraft.block.StonekeepLowDungeonChestPartBlock;
import com.dungeoncraft.block.entity.StonekeepLowDungeonChestBlockEntity;
import com.dungeoncraft.item.StonekeepLowDungeonChestItem;
import com.dungeoncraft.menu.StonekeepLowDungeonChestMenu;
import com.dungeoncraft.item.ColumnItem;
import com.dungeoncraft.item.EmbeddingToolItem;
import com.dungeoncraft.block.RustedMetalSignBlock;
import com.dungeoncraft.block.entity.RustedMetalSignBlockEntity;
import com.dungeoncraft.item.RustedMetalSignItem;
import com.dungeoncraft.item.EngravingToolItem;
import com.dungeoncraft.item.CodingToolItem;
//import com.dungeoncraft.item.MetalFileItem;
import com.dungeoncraft.network.OpenRustedMetalSignEditorPayload;
import com.dungeoncraft.network.OpenCodingToolScreenPayload;
import com.dungeoncraft.network.SaveCodingToolConfigPayload;
import com.dungeoncraft.config.CodingToolDeviceType;
import com.dungeoncraft.config.DeviceSignalMode;
import com.dungeoncraft.config.LeverPowerMode;
import com.dungeoncraft.config.CodingToolConfigurable;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import com.dungeoncraft.network.SaveRustedMetalSignEditorPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DungeonCraft implements ModInitializer {

    public static final String MOD_ID = "dungeoncraft";

    // //////////////////////////////////////////////////
    // This is where we start our block declarations

    // Aged Limestone Bricks Info
    public static final Identifier AGED_LIMESTONE_BRICKS_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "aged_limestone_bricks");

    public static final ResourceKey<Block> AGED_LIMESTONE_BRICKS_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, AGED_LIMESTONE_BRICKS_ID);

    public static final ResourceKey<Item> AGED_LIMESTONE_BRICKS_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, AGED_LIMESTONE_BRICKS_ID);

        public static final Block AGED_LIMESTONE_BRICKS = new AgedLimestoneBricksBlock(
            BlockBehaviour.Properties.of()
                    .setId(AGED_LIMESTONE_BRICKS_BLOCK_KEY)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
    );

    // Individual Aged Limestone Brick crafting ingredient.
    // This is an individual brick item, similar to a vanilla Brick or a metal ingot. It is not a placeable block.
    public static final Identifier AGED_LIMESTONE_ASHLAR_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "aged_limestone_ashlar"
            );

    public static final ResourceKey<Item> AGED_LIMESTONE_ASHLAR_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    AGED_LIMESTONE_ASHLAR_ID
            );

    // Limestone Fieldston Info
    public static final Identifier LIMESTONE_FIELDSTONE_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "limestone_fieldstone");

    public static final ResourceKey<Block> LIMESTONE_FIELDSTONE_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, LIMESTONE_FIELDSTONE_ID);

    public static final ResourceKey<Item> LIMESTONE_FIELDSTONE_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, LIMESTONE_FIELDSTONE_ID   );

    public static final Block LIMESTONE_FIELDSTONE = new LimestoneFieldstoneBlock(
            BlockBehaviour.Properties.of()
                    .setId(LIMESTONE_FIELDSTONE_BLOCK_KEY)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
    );

    // Aged Limestone Info
    public static final Identifier AGED_LIMESTONE_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "aged_limestone");

    public static final ResourceKey<Block> AGED_LIMESTONE_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, AGED_LIMESTONE_ID);

    public static final ResourceKey<Item> AGED_LIMESTONE_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, AGED_LIMESTONE_ID   );

    public static final Block AGED_LIMESTONE = new AgedLimestoneBlock(
            BlockBehaviour.Properties.of()
                    .setId(AGED_LIMESTONE_BLOCK_KEY)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
    );

    // Ledged Door Info
    public static final Identifier LEDGED_DOOR_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "ledged_door");

    public static final ResourceKey<Block> LEDGED_DOOR_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, LEDGED_DOOR_ID);

    public static final ResourceKey<Item> LEDGED_DOOR_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, LEDGED_DOOR_ID);

    public static final Block LEDGED_DOOR = new DoorBlock(
            BlockSetType.ACACIA,
            BlockBehaviour.Properties.of()
                    .setId(LEDGED_DOOR_BLOCK_KEY)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
    );

    // Iron Pocket Door Info
    public static final Identifier IRON_POCKET_DOOR_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "iron_pocket_door");

    public static final ResourceKey<Block> IRON_POCKET_DOOR_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, IRON_POCKET_DOOR_ID);

    public static final ResourceKey<Item> IRON_POCKET_DOOR_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, IRON_POCKET_DOOR_ID);

    public static final Block IRON_POCKET_DOOR = new IronPocketDoorBlock(
            BlockBehaviour.Properties.of()
                    .setId(IRON_POCKET_DOOR_BLOCK_KEY)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.IRON)
                    .noOcclusion()
    );

    // Rusted Metal Sign Info

    // Creates the registry ID for the Rusted Metal Sign.
// Final in-game ID: dungeoncraft:rusted_metal_sign
    public static final Identifier RUSTED_METAL_SIGN_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "rusted_metal_sign"
            );

    // Creates the typed block registry key for the Rusted Metal Sign block.
// Newer Minecraft versions want the block ID assigned before the block is constructed.
    public static final ResourceKey<Block> RUSTED_METAL_SIGN_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    RUSTED_METAL_SIGN_ID
            );

    // Creates the typed item registry key for the Rusted Metal Sign item.
    public static final ResourceKey<Item> RUSTED_METAL_SIGN_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    RUSTED_METAL_SIGN_ID
            );

    // Creates the actual Rusted Metal Sign block object.
    public static final Block RUSTED_METAL_SIGN =
            new RustedMetalSignBlock(
                    BlockBehaviour.Properties.of()
                            // Assigns the block registry key to this block.
                            .setId(RUSTED_METAL_SIGN_BLOCK_KEY)

                            // Gives the sign a metal-like strength.
                            .strength(1.5F, 6.0F)

                            // Makes the sign use metal sounds.
                            .sound(SoundType.METAL)

                            // Prevents Minecraft from treating the thin sign as a full opaque cube.
                            .noOcclusion()
            );

    // Column Info
    public static final Identifier COLUMN_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "column");

    public static final ResourceKey<Block> COLUMN_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, COLUMN_ID);

    public static final ResourceKey<Item> COLUMN_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, COLUMN_ID);

    public static final Block COLUMN = new ColumnBlock(
            BlockBehaviour.Properties.of()
                    .setId(COLUMN_BLOCK_KEY)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
    );

    // Column Base Info
    public static final Identifier COLUMN_BASE_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "column_base");

    public static final ResourceKey<Block> COLUMN_BASE_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, COLUMN_BASE_ID);

    // This declares the Column Base item
    // public static final ResourceKey<Item> COLUMN_BASE_ITEM_KEY =
    //        ResourceKey.create(Registries.ITEM, COLUMN_BASE_ID);

    public static final Block COLUMN_BASE = new ColumnBaseBlock(
            BlockBehaviour.Properties.of()
                    .setId(COLUMN_BASE_BLOCK_KEY)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
    );

    // Column Top Info
    public static final Identifier COLUMN_TOP_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "column_top");

    public static final ResourceKey<Block> COLUMN_TOP_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, COLUMN_TOP_ID);

    // This declares the Column Top item
    // public static final ResourceKey<Item> COLUMN_TOP_ITEM_KEY =
    //        ResourceKey.create(Registries.ITEM, COLUMN_TOP_ID);

    public static final Block COLUMN_TOP = new ColumnTopBlock(
            BlockBehaviour.Properties.of()
                    .setId(COLUMN_TOP_BLOCK_KEY)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
    );

    // Flanked Column Info
    public static final Identifier FLANKED_COLUMN_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "flanked_column"
            );

    public static final ResourceKey<Block> FLANKED_COLUMN_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    FLANKED_COLUMN_ID
            );

    public static final ResourceKey<Item> FLANKED_COLUMN_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    FLANKED_COLUMN_ID
            );

    public static final Block FLANKED_COLUMN =
            new FlankedColumnBlock(
                    BlockBehaviour.Properties.of()
                            .setId(FLANKED_COLUMN_BLOCK_KEY)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
                            .noOcclusion()
            );

    // Stonekeep Hidden Block Lever
    public static final Identifier HIDDEN_BLOCK_LEVER_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "hidden_block_lever");

    public static final ResourceKey<Block> HIDDEN_BLOCK_LEVER_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, HIDDEN_BLOCK_LEVER_ID);

    public static final ResourceKey<Item> HIDDEN_BLOCK_LEVER_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, HIDDEN_BLOCK_LEVER_ID);

    public static final Block HIDDEN_BLOCK_LEVER =
            new HiddenBlockLeverBlock(
                    BlockBehaviour.Properties.of()
                            .setId(HIDDEN_BLOCK_LEVER_BLOCK_KEY)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
                            .noOcclusion()
            );

    /* Renderer-only models. None of these receives a BlockItem. */
    public static final Identifier HIDDEN_BLOCK_LEVER_CLOSED_MODEL_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "hidden_block_lever_closed_model");
    public static final ResourceKey<Block> HIDDEN_BLOCK_LEVER_CLOSED_MODEL_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, HIDDEN_BLOCK_LEVER_CLOSED_MODEL_ID);
    public static final Block HIDDEN_BLOCK_LEVER_CLOSED_MODEL =
            new Block(
                    BlockBehaviour.Properties.of()
                            .setId(HIDDEN_BLOCK_LEVER_CLOSED_MODEL_BLOCK_KEY)
                            .noCollision()
                            .noOcclusion()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
            );

    public static final Identifier HIDDEN_BLOCK_LEVER_SHELL_MODEL_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "hidden_block_lever_shell_model");
    public static final ResourceKey<Block> HIDDEN_BLOCK_LEVER_SHELL_MODEL_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, HIDDEN_BLOCK_LEVER_SHELL_MODEL_ID);
    public static final Block HIDDEN_BLOCK_LEVER_SHELL_MODEL =
            new Block(
                    BlockBehaviour.Properties.of()
                            .setId(HIDDEN_BLOCK_LEVER_SHELL_MODEL_BLOCK_KEY)
                            .noCollision()
                            .noOcclusion()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
            );

    public static final Identifier HIDDEN_BLOCK_LEVER_PANEL_MODEL_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "hidden_block_lever_panel_model");
    public static final ResourceKey<Block> HIDDEN_BLOCK_LEVER_PANEL_MODEL_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, HIDDEN_BLOCK_LEVER_PANEL_MODEL_ID);
    public static final Block HIDDEN_BLOCK_LEVER_PANEL_MODEL =
            new Block(
                    BlockBehaviour.Properties.of()
                            .setId(HIDDEN_BLOCK_LEVER_PANEL_MODEL_BLOCK_KEY)
                            .noCollision()
                            .noOcclusion()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
            );

    public static final Identifier HIDDEN_BLOCK_LEVER_MOUNT_MODEL_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "hidden_block_lever_mount_model");
    public static final ResourceKey<Block> HIDDEN_BLOCK_LEVER_MOUNT_MODEL_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, HIDDEN_BLOCK_LEVER_MOUNT_MODEL_ID);
    public static final Block HIDDEN_BLOCK_LEVER_MOUNT_MODEL =
            new Block(
                    BlockBehaviour.Properties.of()
                            .setId(HIDDEN_BLOCK_LEVER_MOUNT_MODEL_BLOCK_KEY)
                            .noCollision()
                            .noOcclusion()
                            .strength(0.5F, 0.5F)
                            .sound(SoundType.METAL)
            );

    public static final Identifier HIDDEN_BLOCK_LEVER_BAR_MODEL_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "hidden_block_lever_bar_model");
    public static final ResourceKey<Block> HIDDEN_BLOCK_LEVER_BAR_MODEL_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, HIDDEN_BLOCK_LEVER_BAR_MODEL_ID);
    public static final Block HIDDEN_BLOCK_LEVER_BAR_MODEL =
            new Block(
                    BlockBehaviour.Properties.of()
                            .setId(HIDDEN_BLOCK_LEVER_BAR_MODEL_BLOCK_KEY)
                            .noCollision()
                            .noOcclusion()
                            .strength(0.5F, 0.5F)
                            .sound(SoundType.METAL)
            );

    // Iron Lever Info
    public static final Identifier IRON_LEVER_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "iron_lever");

    public static final ResourceKey<Block> IRON_LEVER_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, IRON_LEVER_ID);

    public static final ResourceKey<Item> IRON_LEVER_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, IRON_LEVER_ID);

    public static final Block IRON_LEVER = new IronLeverBlock(
            BlockBehaviour.Properties.of()
                    .setId(IRON_LEVER_BLOCK_KEY)
                    .strength(0.5F, 0.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );


    // Four-port Power Diverter
    public static final Identifier POWER_DIVERTER_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "power_diverter");

    public static final ResourceKey<Block> POWER_DIVERTER_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, POWER_DIVERTER_ID);

    public static final ResourceKey<Item> POWER_DIVERTER_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, POWER_DIVERTER_ID);

    public static final Block POWER_DIVERTER = new PowerDiverterBlock(
            BlockBehaviour.Properties.of()
                    .setId(POWER_DIVERTER_BLOCK_KEY)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
    );

    // Stonekeep Hidden Block Lever Item
    public static final Item HIDDEN_BLOCK_LEVER_ITEM =
            new BlockItem(
                    HIDDEN_BLOCK_LEVER,
                    new Item.Properties()
                            .setId(HIDDEN_BLOCK_LEVER_ITEM_KEY)
            );

    // Thin Copper Wire
    public static final Identifier COPPER_WIRE_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "copper_wire"
            );

    // Typed registry key for the placed block.
    // Newer Minecraft versions require this ID to be assigned before the block object is constructed.
    public static final ResourceKey<Block> COPPER_WIRE_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    COPPER_WIRE_ID
            );

    // Typed registry key for the inventory item.
    public static final ResourceKey<Item> COPPER_WIRE_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    COPPER_WIRE_ID
            );

    // Thin Copper Wire
    public static final Block COPPER_WIRE =
            new CopperWireBlock(
                    BlockBehaviour.Properties.of()
                            .setId(COPPER_WIRE_BLOCK_KEY)
                            // Wire does not physically block players or other entities.
                            .noCollision()
                            // Wire breaks immediately when mined.
                            .instabreak()
                            // Prevents Minecraft from treating the wire as an opaque full cube.
                             .noOcclusion()
                            // Copper/metal placement and breaking sound.
                            .sound(SoundType.COPPER)
            );

    // Embedded Copper Wire — creative-only development block
    public static final Identifier EMBEDDED_COPPER_WIRE_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "embedded_copper_wire"
            );

    public static final ResourceKey<Block> EMBEDDED_COPPER_WIRE_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    EMBEDDED_COPPER_WIRE_ID
            );

    public static final ResourceKey<Item> EMBEDDED_COPPER_WIRE_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    EMBEDDED_COPPER_WIRE_ID
            );

    public static final Block EMBEDDED_COPPER_WIRE =
            new EmbeddedCopperWireBlock(
                    BlockBehaviour.Properties.of()
                            .setId(EMBEDDED_COPPER_WIRE_BLOCK_KEY)
                            .noCollision()
                            .instabreak()
                            .noOcclusion()
                            .sound(SoundType.COPPER)
            );

    /*
     * Internal wrapper used when a solid full block is placed over
     * Embedded Copper Wire.
     *
     * It intentionally has no BlockItem and does not appear in creative.
     */
    public static final Identifier
    COVERED_EMBEDDED_COPPER_WIRE_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "covered_embedded_copper_wire"
            );

    public static final ResourceKey<Block>
    COVERED_EMBEDDED_COPPER_WIRE_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    COVERED_EMBEDDED_COPPER_WIRE_ID
            );

    public static final Block
    COVERED_EMBEDDED_COPPER_WIRE =
            new CoveredEmbeddedCopperWireBlock(
                    BlockBehaviour.Properties.of()
                            .setId(
                                    COVERED_EMBEDDED_COPPER_WIRE_BLOCK_KEY
                            )
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
            );

    // Coding Tool
    public static final Identifier CODING_TOOL_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "coding_tool");

    public static final ResourceKey<Item> CODING_TOOL_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, CODING_TOOL_ID);

    public static final Identifier EMBEDDING_TOOL_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "embedding_tool"
            );

    public static final ResourceKey<Item> EMBEDDING_TOOL_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    EMBEDDING_TOOL_ID
            );

    // Engraving Tool
    // Creates the registry ID for the Engraving Tool.
    // Final in-game ID: dungeoncraft:engraving_tool
    public static final Identifier ENGRAVING_TOOL_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "engraving_tool"
            );

    // Creates the typed item registry key for the Engraving Tool.
    public static final ResourceKey<Item> ENGRAVING_TOOL_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    ENGRAVING_TOOL_ID
            );

    // Basic Treasure Chest
    public static final Identifier BASIC_TREASURE_CHEST_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "basic_treasure_chest"
            );

    public static final ResourceKey<Block>
            BASIC_TREASURE_CHEST_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    BASIC_TREASURE_CHEST_ID
            );

    public static final Block BASIC_TREASURE_CHEST =
            new BasicTreasureChestBlock(
                    BlockBehaviour.Properties.of()
                            .setId(
                                    BASIC_TREASURE_CHEST_BLOCK_KEY
                            )
                            .strength(
                                    2.5F,
                                    6.0F
                            )
                            .sound(
                                    SoundType.WOOD
                            )
                            .noOcclusion()
            );

    /*
     * Internal model block used only by the Basic Treasure Chest renderer.
     *
     * It has no BlockItem, does not appear in creative, and is never
     * intended to be placed in the world.
     */
    public static final Identifier
            BASIC_TREASURE_CHEST_LID_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "basic_treasure_chest_lid"
            );

    public static final ResourceKey<Block>
            BASIC_TREASURE_CHEST_LID_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    BASIC_TREASURE_CHEST_LID_ID
            );

    public static final Block
            BASIC_TREASURE_CHEST_LID =
            new Block(
                    BlockBehaviour.Properties.of()
                            .setId(
                                    BASIC_TREASURE_CHEST_LID_BLOCK_KEY
                            )
                            .noCollision()
                            .noOcclusion()
                            .strength(
                                    2.5F,
                                    6.0F
                            )
                            .sound(
                                    SoundType.WOOD
                            )
            );

    // Stonekeep Low Dungeon Chest
    public static final Identifier STONEKEEP_LOW_DUNGEON_CHEST_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "stonekeep_low_dungeon_chest");

    public static final ResourceKey<Block> STONEKEEP_LOW_DUNGEON_CHEST_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, STONEKEEP_LOW_DUNGEON_CHEST_ID);

    public static final Block STONEKEEP_LOW_DUNGEON_CHEST =
            new StonekeepLowDungeonChestBlock(
                    BlockBehaviour.Properties.of()
                            .setId(STONEKEEP_LOW_DUNGEON_CHEST_BLOCK_KEY)
                            .strength(2.5F, 6.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    /* Invisible footprint part: no item and no independent loot. */
    public static final Identifier STONEKEEP_LOW_DUNGEON_CHEST_PART_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "stonekeep_low_dungeon_chest_part");

    public static final ResourceKey<Block> STONEKEEP_LOW_DUNGEON_CHEST_PART_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, STONEKEEP_LOW_DUNGEON_CHEST_PART_ID);

    public static final Block STONEKEEP_LOW_DUNGEON_CHEST_PART =
            new StonekeepLowDungeonChestPartBlock(
                    BlockBehaviour.Properties.of()
                            .setId(STONEKEEP_LOW_DUNGEON_CHEST_PART_BLOCK_KEY)
                            .strength(2.5F, 6.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            );

    /* Renderer-only baked body geometry. */
    public static final Identifier STONEKEEP_LOW_DUNGEON_CHEST_BODY_MODEL_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "stonekeep_low_dungeon_chest_body_model");

    public static final ResourceKey<Block> STONEKEEP_LOW_DUNGEON_CHEST_BODY_MODEL_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, STONEKEEP_LOW_DUNGEON_CHEST_BODY_MODEL_ID);

    public static final Block STONEKEEP_LOW_DUNGEON_CHEST_BODY_MODEL =
            new Block(
                    BlockBehaviour.Properties.of()
                            .setId(STONEKEEP_LOW_DUNGEON_CHEST_BODY_MODEL_BLOCK_KEY)
                            .noCollision()
                            .noOcclusion()
                            .strength(2.5F, 6.0F)
                            .sound(SoundType.WOOD)
            );

    /* Renderer-only baked lid geometry. */
    public static final Identifier STONEKEEP_LOW_DUNGEON_CHEST_LID_MODEL_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "stonekeep_low_dungeon_chest_lid_model");

    public static final ResourceKey<Block> STONEKEEP_LOW_DUNGEON_CHEST_LID_MODEL_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, STONEKEEP_LOW_DUNGEON_CHEST_LID_MODEL_ID);

    public static final Block STONEKEEP_LOW_DUNGEON_CHEST_LID_MODEL =
            new Block(
                    BlockBehaviour.Properties.of()
                            .setId(STONEKEEP_LOW_DUNGEON_CHEST_LID_MODEL_BLOCK_KEY)
                            .noCollision()
                            .noOcclusion()
                            .strength(2.5F, 6.0F)
                            .sound(SoundType.WOOD)
            );

    // Arched Window Info
    public static final Identifier ARCHED_WINDOW_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "arched_window"
            );

    public static final ResourceKey<Block> ARCHED_WINDOW_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    ARCHED_WINDOW_ID
            );

    public static final ResourceKey<Item> ARCHED_WINDOW_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    ARCHED_WINDOW_ID
            );

    public static final Block ARCHED_WINDOW =
            new ArchedWindowBlock(
                    BlockBehaviour.Properties.of()
                            .setId(ARCHED_WINDOW_BLOCK_KEY)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
                            .noOcclusion()
            );

    /* TEMPORARY BURIED ARCHED WINDOW COMMAND ITEMS
     * These are items only. They do not register additional blocks.
     * Both place the registered dungeoncraft:arched_window block:
     * buried_arched_window_1 -> buried=back
     * buried_arched_window_2 -> buried=both
     */

    public static final Identifier BURIED_ARCHED_WINDOW_1_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "buried_arched_window_1"
            );

    public static final ResourceKey<Item>
            BURIED_ARCHED_WINDOW_1_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    BURIED_ARCHED_WINDOW_1_ID
            );

    public static final Identifier BURIED_ARCHED_WINDOW_2_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "buried_arched_window_2"
            );

    public static final ResourceKey<Item>
            BURIED_ARCHED_WINDOW_2_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    BURIED_ARCHED_WINDOW_2_ID
            );

    // //////////////////////////////////////////////////
    // This is where we start our inventory item declarations

    // Aged Limestone Bricks Info
    public static final Item AGED_LIMESTONE_BRICKS_ITEM = new BlockItem(
            AGED_LIMESTONE_BRICKS,
            new Item.Properties()
                    .setId(AGED_LIMESTONE_BRICKS_ITEM_KEY)
    );

    // Individual Aged Limestone Brick crafting ingredient
    public static final Item AGED_LIMESTONE_ASHLAR_ITEM =
            new Item(
                    new Item.Properties()
                            .setId(AGED_LIMESTONE_ASHLAR_ITEM_KEY)
            );

    // Limestone Fieldstone Info
    public static final Item LIMESTONE_FIELDSTONE_ITEM = new BlockItem(
            LIMESTONE_FIELDSTONE,
            new Item.Properties()
                    .setId(LIMESTONE_FIELDSTONE_ITEM_KEY)
    );

    // Aged Limestone Info
    public static final Item AGED_LIMESTONE_ITEM = new BlockItem(
            AGED_LIMESTONE,
            new Item.Properties()
                    .setId(AGED_LIMESTONE_ITEM_KEY)
    );

    // Ledged Door Info
    public static final Item LEDGED_DOOR_ITEM = new DoubleHighBlockItem(
            LEDGED_DOOR,
            new Item.Properties()
                    .setId(LEDGED_DOOR_ITEM_KEY)
    );

    // Iron Pocket Door Info
    public static final Item IRON_POCKET_DOOR_ITEM = new IronPocketDoorItem(
            new Item.Properties()
                    .setId(IRON_POCKET_DOOR_ITEM_KEY)
    );

    // Column Block
    public static final Item COLUMN_ITEM = new ColumnItem(
            new Item.Properties()
                    .setId(COLUMN_ITEM_KEY)
    );

    // Flanked Column Block
    public static final Item FLANKED_COLUMN_ITEM =
            new FlankedColumnItem(
                    new Item.Properties()
                            .setId(FLANKED_COLUMN_ITEM_KEY)
            );

    // Iron Lever Block
    public static final Item IRON_LEVER_ITEM = new BlockItem(
            IRON_LEVER,
            new Item.Properties()
                    .setId(IRON_LEVER_ITEM_KEY)
    );


    // Four-port Power Diverter Item
    public static final Item POWER_DIVERTER_ITEM = new BlockItem(
            POWER_DIVERTER,
            new Item.Properties()
                    .setId(POWER_DIVERTER_ITEM_KEY)
    );

    // Coding Tool
    public static final Item CODING_TOOL =
            new CodingToolItem(
                    new Item.Properties()
                            .setId(CODING_TOOL_ITEM_KEY)
                            .stacksTo(1)
            );

    // Embedding Tool
    // Phase 1 is reusable and non-damageable so the conversion behavior can
    // be tested before a durability value and recipe are finalized.
    public static final Item EMBEDDING_TOOL =
            new EmbeddingToolItem(
                    new Item.Properties()
                            .setId(EMBEDDING_TOOL_ITEM_KEY)
                            .stacksTo(1)
            );

    // Engraving Tool Item
    // Creates the Engraving Tool item.
    // It is stack size 1 because it is a tool, not a material.
    public static final Item ENGRAVING_TOOL =
            new EngravingToolItem(
                    new Item.Properties()
                            // Assigns the item registry key to this item.
                            .setId(ENGRAVING_TOOL_ITEM_KEY)

                            // Makes only one Engraving Tool fit in a stack.
                            .stacksTo(1)
            );

    public static final Identifier METAL_FILE_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "metal_file");

    public static final ResourceKey<Item> METAL_FILE_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, METAL_FILE_ID);

    //public static final Item METAL_FILE =
    //        new MetalFileItem(
    //                new Item.Properties()
    //                        .setId(METAL_FILE_ITEM_KEY)
    //                        .stacksTo(1)
    //        );

    // Rusted Metal Sign Item
    // Creates the inventory item for the Rusted Metal Sign.
    // This uses a custom item class so we can later store text, surface wear,
    // and tooltip information on the item.
    public static final Item RUSTED_METAL_SIGN_ITEM =
            new RustedMetalSignItem(
                    RUSTED_METAL_SIGN,
                    new Item.Properties()
                            // Assigns the item registry key to this item.
                            .setId(RUSTED_METAL_SIGN_ITEM_KEY)
            );

    // Thin Copper Wire
    // RedStoneDustItem extends Minecraft's block-item behavior with redstone-wire-specific placement interaction.
    public static final Item COPPER_WIRE_ITEM =
            new BlockItem(
                    COPPER_WIRE,
                    new Item.Properties()
                            .setId(COPPER_WIRE_ITEM_KEY)
            );

    // Embedded Copper Wire is intentionally creative-only for now.
    // No crafting recipe is provided.
    public static final Item EMBEDDED_COPPER_WIRE_ITEM =
            new BlockItem(
                    EMBEDDED_COPPER_WIRE,
                    new Item.Properties()
                            .setId(EMBEDDED_COPPER_WIRE_ITEM_KEY)
            );

    public static final ResourceKey<Item>
            BASIC_TREASURE_CHEST_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    BASIC_TREASURE_CHEST_ID
            );

    public static final Item BASIC_TREASURE_CHEST_ITEM =
            new BlockItem(
                    BASIC_TREASURE_CHEST,
                    new Item.Properties()
                            .setId(
                                    BASIC_TREASURE_CHEST_ITEM_KEY
                            )
            );

    public static final ResourceKey<Item> STONEKEEP_LOW_DUNGEON_CHEST_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, STONEKEEP_LOW_DUNGEON_CHEST_ID);

    public static final Item STONEKEEP_LOW_DUNGEON_CHEST_ITEM =
            new StonekeepLowDungeonChestItem(
                    new Item.Properties().setId(STONEKEEP_LOW_DUNGEON_CHEST_ITEM_KEY)
            );

    // Arched Window Item
    public static final Item ARCHED_WINDOW_ITEM =
            new ArchedWindowItem(
                    new Item.Properties()
                            .setId(ARCHED_WINDOW_ITEM_KEY)
            );

    /* TEMPORARY COMMAND-ACCESSIBLE BURIED WINDOW ITEMS
     * Keep these declarations permanently.
     * Command availability is controlled by the optional registrations
     * in onInitialize().
     */

    public static final Item BURIED_ARCHED_WINDOW_1_ITEM =
            new BuriedArchedWindowItem(
                    ArchedWindowBuried.BACK,
                    new Item.Properties()
                            .setId(
                                    BURIED_ARCHED_WINDOW_1_ITEM_KEY
                            )
            );

    public static final Item BURIED_ARCHED_WINDOW_2_ITEM =
            new BuriedArchedWindowItem(
                    ArchedWindowBuried.BOTH,
                    new Item.Properties()
                            .setId(
                                    BURIED_ARCHED_WINDOW_2_ITEM_KEY
                            )
            );

    // Column Base allows the following product to be used as an item separate from the full piece
    // public static final Item COLUMN_BASE_ITEM = new BlockItem(
    //        COLUMN_BASE,
    //        new Item.Properties()
    //                .setId(COLUMN_BASE_ITEM_KEY)
    // );

    // Column Top allows the following product to be used as an item separate from the full piece
    // public static final Item COLUMN_TOP_ITEM = new BlockItem(
    //        COLUMN_TOP,
    //        new Item.Properties()
    //                .setId(COLUMN_TOP_ITEM_KEY)
    // );

    // //////////////////////////////////////////////////
    // This is where we start out Block-entity declarations

    // Covered Embedded Wire
    public static final Identifier
            COVERED_EMBEDDED_COPPER_WIRE_BLOCK_ENTITY_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "covered_embedded_copper_wire"
            );

    public static final BlockEntityType<
            CoveredEmbeddedCopperWireBlockEntity
            >
            COVERED_EMBEDDED_COPPER_WIRE_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(
                    CoveredEmbeddedCopperWireBlockEntity::new,
                    COVERED_EMBEDDED_COPPER_WIRE
            ).build();

    // Basic Treasure Chest
    public static final Identifier
            BASIC_TREASURE_CHEST_BLOCK_ENTITY_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "basic_treasure_chest"
            );

    public static final BlockEntityType<
            BasicTreasureChestBlockEntity
            >
            BASIC_TREASURE_CHEST_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(
                    BasicTreasureChestBlockEntity::new,
                    BASIC_TREASURE_CHEST
            ).build();

    public static final Identifier HIDDEN_BLOCK_LEVER_BLOCK_ENTITY_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "hidden_block_lever");

    public static final BlockEntityType<HiddenBlockLeverBlockEntity>
            HIDDEN_BLOCK_LEVER_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(
                    HiddenBlockLeverBlockEntity::new,
                    HIDDEN_BLOCK_LEVER
            ).build();

    public static final Identifier IRON_LEVER_BLOCK_ENTITY_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "iron_lever");

    public static final BlockEntityType<IronLeverBlockEntity>
            IRON_LEVER_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(
                    IronLeverBlockEntity::new,
                    IRON_LEVER
            ).build();


    public static final Identifier POWER_DIVERTER_BLOCK_ENTITY_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "power_diverter");

    public static final BlockEntityType<PowerDiverterBlockEntity>
            POWER_DIVERTER_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(
                    PowerDiverterBlockEntity::new,
                    POWER_DIVERTER
            ).build();

    public static final Identifier STONEKEEP_LOW_DUNGEON_CHEST_BLOCK_ENTITY_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "stonekeep_low_dungeon_chest");

    public static final BlockEntityType<StonekeepLowDungeonChestBlockEntity>
            STONEKEEP_LOW_DUNGEON_CHEST_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(
                    StonekeepLowDungeonChestBlockEntity::new,
                    STONEKEEP_LOW_DUNGEON_CHEST
            ).build();

    // //////////////////////////////////////////////////
    // This is where we create our menus
    /*
     * Identifies the Basic Treasure Chest menu in Minecraft's
     * menu registry.
     */
    public static final Identifier
            BASIC_TREASURE_CHEST_MENU_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "basic_treasure_chest"
            );

    public static final Identifier STONEKEEP_LOW_DUNGEON_CHEST_MENU_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "stonekeep_low_dungeon_chest");

    // Rusted Metal Sign Block Entity

    // Creates the registry ID for the Rusted Metal Sign block entity.
// This uses the same path as the block because it belongs to that block.
    public static final Identifier RUSTED_METAL_SIGN_BLOCK_ENTITY_ID =
            Identifier.fromNamespaceAndPath(
                    MOD_ID,
                    "rusted_metal_sign"
            );

    // Creates the BlockEntityType for RustedMetalSignBlockEntity.
// This connects the block entity class to the placed Rusted Metal Sign block.
    public static final BlockEntityType<
            RustedMetalSignBlockEntity
            >
            RUSTED_METAL_SIGN_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(
                    // Constructor reference for the block entity.
                    RustedMetalSignBlockEntity::new,

                    // The block that is allowed to use this block entity.
                    RUSTED_METAL_SIGN
            ).build();

    /*
     * Creates the menu type.
     *
     * BasicTreasureChestMenu::new points to the client-side
     * constructor that receives:
     *
     * - the container ID;
     * - the player's inventory.
     */
    public static final MenuType<BasicTreasureChestMenu>
            BASIC_TREASURE_CHEST_MENU =
            new MenuType<>(
                    BasicTreasureChestMenu::new,
                    FeatureFlagSet.of()
            );

    public static final MenuType<StonekeepLowDungeonChestMenu>
            STONEKEEP_LOW_DUNGEON_CHEST_MENU =
            new MenuType<>(
                    StonekeepLowDungeonChestMenu::new,
                    FeatureFlagSet.of()
            );

    // //////////////////////////////////////////////////
    // Add your items into the creative menu at the end of the Building Blocks tab.
    // Other tabs can be used and blocks placed where you want inside that area.

    // CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
    //    output.insertAfter places your item where you want, otherwise it is placed at the bottom
    //    output.insertAfter(Items.STONE_BRICKS, AGED_LIMESTONE_BRICKS_ITEM);
    //    output.accept(LIMESTONE_FIELDSTONE_ITEM);
    //    output.accept(AGED_LIMESTONE_ITEM);

    //    output.accept(LEDGED_DOOR_ITEM);
    //    }
    // );

    // //////////////////////////////////////////////////
    // This is where we create our new tab and add our blocks and items to it
    public static final CreativeModeTab DUNGEONCRAFT_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MOD_ID, "dungeoncraft_tab"),
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(AGED_LIMESTONE_BRICKS_ITEM))
                    .title(Component.translatable("creativeTab.dungeoncraft"))
                    .displayItems((params, output) -> {
                        output.accept(AGED_LIMESTONE_BRICKS_ITEM);
                        output.accept(LIMESTONE_FIELDSTONE_ITEM);
                        output.accept(AGED_LIMESTONE_ITEM);

                        //output.accept(COLUMN_BASE_ITEM);
                        //output.accept(COLUMN_TOP_ITEM);
                        output.accept(COLUMN_ITEM);
                        output.accept(FLANKED_COLUMN_ITEM);

                        output.accept(ARCHED_WINDOW_ITEM);
                        output.accept(BURIED_ARCHED_WINDOW_1_ITEM);
                        // output.accept(BURIED_ARCHED_WINDOW_2_ITEM);

                        output.accept(AGED_LIMESTONE_ASHLAR_ITEM);

                        output.accept(BASIC_TREASURE_CHEST_ITEM);
                        output.accept(STONEKEEP_LOW_DUNGEON_CHEST_ITEM);

                        output.accept(LEDGED_DOOR_ITEM);
                        output.accept(IRON_POCKET_DOOR_ITEM);

                        output.accept(IRON_LEVER_ITEM);
                        output.accept(HIDDEN_BLOCK_LEVER_ITEM);
                        output.accept(POWER_DIVERTER_ITEM);
                        output.accept(CODING_TOOL);
                        output.accept(RUSTED_METAL_SIGN_ITEM);
                        //output.accept(EMBEDDED_COPPER_WIRE_ITEM);
                        output.accept(COPPER_WIRE_ITEM);
                        output.accept(EMBEDDING_TOOL);
                        output.accept(ENGRAVING_TOOL);
                        //output.accept(METAL_FILE);
                        }
                    )
                    .build()
    );

    // //////////////////////////////////////////////////
    // This is where we register our blocks and items

    @Override
    public void onInitialize() {
        // Aged Limestone Bricks Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                AGED_LIMESTONE_BRICKS_ID,
                AGED_LIMESTONE_BRICKS
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                AGED_LIMESTONE_BRICKS_ID,
                AGED_LIMESTONE_BRICKS_ITEM
        );

        // Individual Aged Limestone Brick crafting ingredient
        Registry.register(
                BuiltInRegistries.ITEM,
                AGED_LIMESTONE_ASHLAR_ID,
                AGED_LIMESTONE_ASHLAR_ITEM
        );

        // Limestone Fieldstone Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                LIMESTONE_FIELDSTONE_ID,
                LIMESTONE_FIELDSTONE
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                LIMESTONE_FIELDSTONE_ID,
                LIMESTONE_FIELDSTONE_ITEM
        );

        // Aged Limestone Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                AGED_LIMESTONE_ID,
                AGED_LIMESTONE
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                AGED_LIMESTONE_ID,
                AGED_LIMESTONE_ITEM
        );

        // Ledged Door Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                LEDGED_DOOR_ID,
                LEDGED_DOOR
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                LEDGED_DOOR_ID,
                LEDGED_DOOR_ITEM
        );

        // Iron Pocket Door Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                IRON_POCKET_DOOR_ID,
                IRON_POCKET_DOOR
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                IRON_POCKET_DOOR_ID,
                IRON_POCKET_DOOR_ITEM
        );

        // Column Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                COLUMN_ID,
                COLUMN
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                COLUMN_ID,
                COLUMN_ITEM
        );

        // Column Base Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                COLUMN_BASE_ID,
                COLUMN_BASE
        );

        // Registry.register(
        //        BuiltInRegistries.ITEM,
        //        COLUMN_BASE_ID,
        //        COLUMN_BASE_ITEM
        // );

        // Column Top Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                COLUMN_TOP_ID,
                COLUMN_TOP
        );

        // Registry.register(
        //        BuiltInRegistries.ITEM,
        //        COLUMN_TOP_ID,
        //        COLUMN_TOP_ITEM
        // );

        // Flanked Column Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                FLANKED_COLUMN_ID,
                FLANKED_COLUMN
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                FLANKED_COLUMN_ID,
                FLANKED_COLUMN_ITEM
        );

        // Stonekeep Hidden Block Lever and renderer-only models
        Registry.register(
                BuiltInRegistries.BLOCK,
                HIDDEN_BLOCK_LEVER_ID,
                HIDDEN_BLOCK_LEVER
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                HIDDEN_BLOCK_LEVER_ID,
                HIDDEN_BLOCK_LEVER_ITEM
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                HIDDEN_BLOCK_LEVER_CLOSED_MODEL_ID,
                HIDDEN_BLOCK_LEVER_CLOSED_MODEL
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                HIDDEN_BLOCK_LEVER_SHELL_MODEL_ID,
                HIDDEN_BLOCK_LEVER_SHELL_MODEL
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                HIDDEN_BLOCK_LEVER_PANEL_MODEL_ID,
                HIDDEN_BLOCK_LEVER_PANEL_MODEL
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                HIDDEN_BLOCK_LEVER_MOUNT_MODEL_ID,
                HIDDEN_BLOCK_LEVER_MOUNT_MODEL
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                HIDDEN_BLOCK_LEVER_BAR_MODEL_ID,
                HIDDEN_BLOCK_LEVER_BAR_MODEL
        );

        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                HIDDEN_BLOCK_LEVER_BLOCK_ENTITY_ID,
                HIDDEN_BLOCK_LEVER_BLOCK_ENTITY
        );

        // Iron Lever Info
        Registry.register(
                BuiltInRegistries.BLOCK,
                IRON_LEVER_ID,
                IRON_LEVER
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                IRON_LEVER_ID,
                IRON_LEVER_ITEM
        );

        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                IRON_LEVER_BLOCK_ENTITY_ID,
                IRON_LEVER_BLOCK_ENTITY
        );


        // Four-port Power Diverter
        Registry.register(
                BuiltInRegistries.BLOCK,
                POWER_DIVERTER_ID,
                POWER_DIVERTER
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                POWER_DIVERTER_ID,
                POWER_DIVERTER_ITEM
        );

        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                POWER_DIVERTER_BLOCK_ENTITY_ID,
                POWER_DIVERTER_BLOCK_ENTITY
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                CODING_TOOL_ID,
                CODING_TOOL
        );

        // Thin Copper Wire
        Registry.register(
                BuiltInRegistries.BLOCK,
                COPPER_WIRE_ID,
                COPPER_WIRE
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                COPPER_WIRE_ID,
                COPPER_WIRE_ITEM
        );

        // Embedding Tool
        Registry.register(
                BuiltInRegistries.ITEM,
                EMBEDDING_TOOL_ID,
                EMBEDDING_TOOL
        );

        //Registry.register(
        //        BuiltInRegistries.ITEM,
        //        METAL_FILE_ID,
        //        METAL_FILE
        //);

        // Embedded Copper Wire — creative-only development block
        Registry.register(
                BuiltInRegistries.BLOCK,
                EMBEDDED_COPPER_WIRE_ID,
                EMBEDDED_COPPER_WIRE
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                EMBEDDED_COPPER_WIRE_ID,
                EMBEDDED_COPPER_WIRE_ITEM
        );

        /*
         * Covered Embedded Copper Wire is an internal block only.
         * It has no item registration and cannot be selected directly.
         */
        Registry.register(
                BuiltInRegistries.BLOCK,
                COVERED_EMBEDDED_COPPER_WIRE_ID,
                COVERED_EMBEDDED_COPPER_WIRE
        );

        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                COVERED_EMBEDDED_COPPER_WIRE_BLOCK_ENTITY_ID,
                COVERED_EMBEDDED_COPPER_WIRE_BLOCK_ENTITY
        );

        // Basic Treasure Chest
        Registry.register(
                BuiltInRegistries.BLOCK,
                BASIC_TREASURE_CHEST_ID,
                BASIC_TREASURE_CHEST
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                BASIC_TREASURE_CHEST_ID,
                BASIC_TREASURE_CHEST_ITEM
        );

        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                BASIC_TREASURE_CHEST_BLOCK_ENTITY_ID,
                BASIC_TREASURE_CHEST_BLOCK_ENTITY
        );

        /*
         * Internal renderer-only lid model block.
         * No item is registered for this block.
         */
        Registry.register(
                BuiltInRegistries.BLOCK,
                BASIC_TREASURE_CHEST_LID_ID,
                BASIC_TREASURE_CHEST_LID
        );

        // Basic Treasure Chest Menu
        Registry.register(
                BuiltInRegistries.MENU,
                BASIC_TREASURE_CHEST_MENU_ID,
                BASIC_TREASURE_CHEST_MENU
        );

        // Stonekeep Low Dungeon Chest and its technical parts/models.
        Registry.register(
                BuiltInRegistries.BLOCK,
                STONEKEEP_LOW_DUNGEON_CHEST_ID,
                STONEKEEP_LOW_DUNGEON_CHEST
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                STONEKEEP_LOW_DUNGEON_CHEST_ID,
                STONEKEEP_LOW_DUNGEON_CHEST_ITEM
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                STONEKEEP_LOW_DUNGEON_CHEST_PART_ID,
                STONEKEEP_LOW_DUNGEON_CHEST_PART
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                STONEKEEP_LOW_DUNGEON_CHEST_BODY_MODEL_ID,
                STONEKEEP_LOW_DUNGEON_CHEST_BODY_MODEL
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                STONEKEEP_LOW_DUNGEON_CHEST_LID_MODEL_ID,
                STONEKEEP_LOW_DUNGEON_CHEST_LID_MODEL
        );

        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                STONEKEEP_LOW_DUNGEON_CHEST_BLOCK_ENTITY_ID,
                STONEKEEP_LOW_DUNGEON_CHEST_BLOCK_ENTITY
        );

        Registry.register(
                BuiltInRegistries.MENU,
                STONEKEEP_LOW_DUNGEON_CHEST_MENU_ID,
                STONEKEEP_LOW_DUNGEON_CHEST_MENU
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                ARCHED_WINDOW_ID,
                ARCHED_WINDOW
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                ARCHED_WINDOW_ID,
                ARCHED_WINDOW_ITEM
        );

        /*
         * OPTIONAL BURIED ARCHED WINDOW COMMAND ITEMS CURRENTLY ENABLED:
         * Leave these two registrations active to allow:
         * /give @s dungeoncraft:buried_arched_window_1
         * /give @s dungeoncraft:buried_arched_window_2
         *
         * FUTURE NORMAL-GAME MODE:
         * Comment out both Registry.register sections to remove these IDs
         * from commands while keeping their classes, declarations, models,
         * and item definitions in the project.
         */

        Registry.register(
                BuiltInRegistries.ITEM,
                BURIED_ARCHED_WINDOW_1_ID,
                BURIED_ARCHED_WINDOW_1_ITEM
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                BURIED_ARCHED_WINDOW_2_ID,
                BURIED_ARCHED_WINDOW_2_ITEM
        );

        // Rusted Metal Sign
        Registry.register(
                BuiltInRegistries.BLOCK,
                RUSTED_METAL_SIGN_ID,
                RUSTED_METAL_SIGN
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                RUSTED_METAL_SIGN_ID,
                RUSTED_METAL_SIGN_ITEM
        );

        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                RUSTED_METAL_SIGN_BLOCK_ENTITY_ID,
                RUSTED_METAL_SIGN_BLOCK_ENTITY
        );

        // Engraving Tool
        Registry.register(
                BuiltInRegistries.ITEM,
                ENGRAVING_TOOL_ID,
                ENGRAVING_TOOL
        );

        /*
         * Registers the server-to-client packet that tells the client
         * to open the Rusted Metal Sign editor screen.
         */
        PayloadTypeRegistry.clientboundPlay().register(
                OpenRustedMetalSignEditorPayload.TYPE,
                OpenRustedMetalSignEditorPayload.CODEC
        );

        /*
         * Registers the client-to-server packet that saves edited
         * Rusted Metal Sign text.
         */
        PayloadTypeRegistry.serverboundPlay().register(
                SaveRustedMetalSignEditorPayload.TYPE,
                SaveRustedMetalSignEditorPayload.CODEC
        );


        PayloadTypeRegistry.clientboundPlay().register(
                OpenCodingToolScreenPayload.TYPE,
                OpenCodingToolScreenPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                SaveCodingToolConfigPayload.TYPE,
                SaveCodingToolConfigPayload.CODEC
        );

        /*
         * Receives edited Rusted Metal Sign text from the client.
         *
         * The server validates that:
         * - the player is close enough;
         * - the block entity is really a Rusted Metal Sign;
         * - the player is still holding the Engraving Tool.
         */
        ServerPlayNetworking.registerGlobalReceiver(
                SaveCodingToolConfigPayload.TYPE,
                (
                        payload,
                        context
                ) -> {
                    var player = context.player();

                    double centerX = payload.pos().getX() + 0.5D;
                    double centerY = payload.pos().getY() + 0.5D;
                    double centerZ = payload.pos().getZ() + 0.5D;

                    if (player.distanceToSqr(centerX, centerY, centerZ) > 64.0D) {
                        return;
                    }

                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();

                    if (!mainHand.is(CODING_TOOL)
                            && !offHand.is(CODING_TOOL)) {
                        return;
                    }

                    CodingToolDeviceType deviceType =
                            CodingToolDeviceType.fromSerializedName(
                                    payload.deviceType()
                            );
                    DeviceSignalMode signalMode =
                            DeviceSignalMode.fromSerializedName(
                                    payload.signalMode()
                            );
                    LeverPowerMode leverPowerMode =
                            LeverPowerMode.fromSerializedName(
                                    payload.leverPowerMode()
                            );

                    BlockEntity blockEntity =
                            player.level().getBlockEntity(payload.pos());

                    if (!(blockEntity instanceof CodingToolConfigurable configurable)
                            || configurable.getCodingToolDeviceType() != deviceType) {
                        return;
                    }

                    if (deviceType == CodingToolDeviceType.HIDDEN_BLOCK_LEVER) {
                        if (!(blockEntity instanceof HiddenBlockLeverBlockEntity hiddenLever)
                                || !hiddenLever.isPanelFullyOpen()
                                || !player.level().getBlockState(payload.pos()).is(HIDDEN_BLOCK_LEVER)) {
                            return;
                        }
                    } else if (deviceType == CodingToolDeviceType.IRON_LEVER
                            && !player.level().getBlockState(payload.pos()).is(IRON_LEVER)) {
                        return;
                    } else if (deviceType == CodingToolDeviceType.POWER_DIVERTER
                            && !player.level().getBlockState(payload.pos()).is(POWER_DIVERTER)) {
                        return;
                    }

                    configurable.applyCodingToolConfiguration(
                            signalMode,
                            leverPowerMode,
                            payload.outputFaceMask(),
                            payload.advancedConfig().sanitizedFor(deviceType)
                    );
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SaveRustedMetalSignEditorPayload.TYPE,
                (
                        payload,
                        context
                ) -> {
                    var player =
                            context.player();

                    /*
                     * Basic distance check.
                     *
                     * This prevents editing a sign from too far away.
                     */
                    double centerX =
                            payload.pos().getX() + 0.5D;

                    double centerY =
                            payload.pos().getY() + 0.5D;

                    double centerZ =
                            payload.pos().getZ() + 0.5D;

                    if (
                            player.distanceToSqr(
                                    centerX,
                                    centerY,
                                    centerZ
                            ) > 64.0D
                    ) {
                        return;
                    }

                    /*
                     * Make sure the player is still holding the Engraving Tool.
                     */
                    ItemStack mainHand =
                            player.getMainHandItem();

                    ItemStack offHand =
                            player.getOffhandItem();

                    if (
                            !mainHand.is(ENGRAVING_TOOL)
                                    && !offHand.is(ENGRAVING_TOOL)
                    ) {
                        return;
                    }

                    /*
                     * Find the block entity at the sign position.
                     */
                    BlockEntity blockEntity =
                            player.level()
                                    .getBlockEntity(
                                            payload.pos()
                                    );

                    /*
                     * Save the edited text into the Rusted Metal Sign block entity.
                     */
                    if (blockEntity instanceof RustedMetalSignBlockEntity signBlockEntity) {
                        signBlockEntity.setText(
                                payload.line1(),
                                payload.line2(),
                                payload.line3(),
                                payload.line4()
                        );
                    }
                }
        );
    }
}