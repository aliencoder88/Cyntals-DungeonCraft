package com.dungeoncraft;

import com.dungeoncraft.client.model.CoveredEmbeddedCopperWireModel;
import com.dungeoncraft.client.render.BasicTreasureChestRenderer;
import com.dungeoncraft.client.render.StonekeepLowDungeonChestRenderer;
import com.dungeoncraft.client.render.HiddenBlockLeverRenderer;
import com.dungeoncraft.client.screen.BasicTreasureChestScreen;
import com.dungeoncraft.client.screen.StonekeepLowDungeonChestScreen;
import com.dungeoncraft.client.screen.CodingToolScreen;
import com.dungeoncraft.config.CodingToolDeviceType;
import com.dungeoncraft.config.DeviceSignalMode;
import com.dungeoncraft.config.LeverPowerMode;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.gui.screens.MenuScreens;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import com.dungeoncraft.client.render.blockentity.RustedMetalSignBlockEntityRenderer;
import com.dungeoncraft.client.screen.RustedMetalSignEditScreen;
import com.dungeoncraft.network.OpenRustedMetalSignEditorPayload;
import com.dungeoncraft.network.OpenCodingToolScreenPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/*
 * Client-only DungeonCraft registration.
 *
 * Covered Embedded Copper Wire is now rendered as an ordinary chunk model,
 * rather than through a block-entity renderer.
 */
public class DungeonCraftClient
        implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(
                pluginContext ->
                        pluginContext
                                .modifyBlockModelAfterBake()
                                .register(
                                        (
                                                model,
                                                modificationContext
                                        ) -> {
                                            if (
                                                    modificationContext
                                                            .state()
                                                            .is(
                                                                    DungeonCraft
                                                                            .COVERED_EMBEDDED_COPPER_WIRE
                                                            )
                                            ) {
                                                return new
                                                        CoveredEmbeddedCopperWireModel(
                                                                model
                                                        );
                                            }

                                            return model;
                                        }
                                )
        );

        // Register the renderer
        BlockEntityRenderers.register(
                DungeonCraft.BASIC_TREASURE_CHEST_BLOCK_ENTITY,
                BasicTreasureChestRenderer::new
        );

        BlockEntityRenderers.register(
                DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_BLOCK_ENTITY,
                StonekeepLowDungeonChestRenderer::new
        );

        BlockEntityRenderers.register(
                DungeonCraft.HIDDEN_BLOCK_LEVER_BLOCK_ENTITY,
                HiddenBlockLeverRenderer::new
        );

        MenuScreens.register(
                DungeonCraft.STONEKEEP_LOW_DUNGEON_CHEST_MENU,
                StonekeepLowDungeonChestScreen::new
        );

        /*
         * Connects the registered Basic Treasure Chest menu
         * to its client-side screen.
         */
        MenuScreens.register(
                DungeonCraft.BASIC_TREASURE_CHEST_MENU,
                BasicTreasureChestScreen::new
        );

        BlockEntityRenderers.register(
                DungeonCraft.RUSTED_METAL_SIGN_BLOCK_ENTITY,
                RustedMetalSignBlockEntityRenderer::new
        );

        /*
         * Opens the Rusted Metal Sign editor screen when the server asks this client
         * to edit a specific sign.
         */
        ClientPlayNetworking.registerGlobalReceiver(
                OpenRustedMetalSignEditorPayload.TYPE,
                (
                        payload,
                        context
                ) -> {
                    Minecraft client =
                            context.client();

                    client.execute(
                            () -> client.setScreen(
                                    new RustedMetalSignEditScreen(
                                            payload.pos(),
                                            payload.line1(),
                                            payload.line2(),
                                            payload.line3(),
                                            payload.line4()
                                    )
                            )
                    );
                }
        );

        /*
         * Opens the shared Coding Tool screen with the server-authoritative
         * settings for the clicked device.
         */
        ClientPlayNetworking.registerGlobalReceiver(
                OpenCodingToolScreenPayload.TYPE,
                (
                        payload,
                        context
                ) -> {
                    Minecraft client = context.client();

                    client.execute(
                            () -> client.setScreen(
                                    new CodingToolScreen(
                                            payload.pos(),
                                            CodingToolDeviceType.fromSerializedName(
                                                    payload.deviceType()
                                            ),
                                            DeviceSignalMode.fromSerializedName(
                                                    payload.signalMode()
                                            ),
                                            LeverPowerMode.fromSerializedName(
                                                    payload.leverPowerMode()
                                            ),
                                            payload.outputFaceMask(),
                                            payload.advancedConfig()
                                    )
                            )
                    );
                }
        );
    }
}
