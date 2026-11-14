/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

import buildcraft.api.BCModules;
import buildcraft.api.core.BCLog;

import buildcraft.lib.expression.ExpressionDebugManager;

@Mod(BCLib.MODID)
public class BCLib {
    public static final String MODID = "buildcraftlib";

    /** Set from the mod container in the constructor (was gradle-replaced "$version" in 1.12). */
    public static String VERSION = "unknown";

    public static final boolean DEV = Boolean.getBoolean("buildcraft.dev");

    public static ModContainer MOD_CONTAINER;

    public BCLib(IEventBus modBus, ModContainer container) {
        MOD_CONTAINER = container;
        VERSION = container.getModInfo().getVersion().toString();

        // Was the body of FMLPreInitializationEvent. Registration (blocks, items, etc.) must now go through
        // DeferredRegisters attached to modBus here, instead of happening imperatively in pre-init.
        logStartup();

        ExpressionDebugManager.logger = BCLog.logger::info;
        // TODO(port): ExpressionCompat.setup();

        // TODO(port): BCLibRegistries.fmlPreInit();
        // TODO(port): BCLibProxy.getProxy().fmlPreInit(); -> split into client-only class + RegisterClientReloadListenersEvent etc.
        // TODO(port): BCLibItems.fmlPreInit(); -> DeferredRegister.Items, registered on modBus
        // TODO(port): BuildCraftObjectCaches.fmlPreInit();
        // TODO(port): IGuiHandler -> MenuType registry + IMenuTypeExtension.create(...)
        // TODO(port): NeoForge.EVENT_BUS.register(BCLibEventDist.class / MigrationManager / FluidManager)
        // TODO(port): ForgeChunkManager tickets -> TicketController + RegisterTicketControllersEvent (quarry chunk loading)
        // TODO(port): MessageManager -> RegisterPayloadHandlersEvent + CustomPacketPayload / StreamCodec

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);
    }

    private static void logStartup() {
        BCLog.logger.info("");
        BCLog.logger.info("Starting BuildCraft " + VERSION);
        BCLog.logger.info("Copyright (c) the BuildCraft team, 2011-2018");
        BCLog.logger.info("https://www.mod-buildcraft.com");
        BCLog.logger.info("");
        BCLog.logger.info("Loaded Modules:");
        for (BCModules module : BCModules.getLoadedModules()) {
            BCLog.logger.info("  - " + module.lowerCaseName);
        }
        BCLog.logger.info("Missing Modules:");
        for (BCModules module : BCModules.getMissingModules()) {
            BCLog.logger.info("  - " + module.lowerCaseName);
        }
        BCLog.logger.info("");
    }

    /** Was FMLInitializationEvent. */
    private void commonSetup(FMLCommonSetupEvent event) {
        // TODO(port): BCLibProxy.getProxy().fmlInit();
        // TODO(port): BCLibRegistries.fmlInit();
        // TODO(port): VanillaListHandlers / VanillaPaintHandlers / VanillaRotationHandlers .fmlInit();
        // Ore dictionary is gone: RegistrationHelper.registerOredictEntries() becomes item/block tags in data/.
    }

    /** Was FMLPostInitializationEvent. */
    private void loadComplete(FMLLoadCompleteEvent event) {
        // TODO(port): ReloadableRegistryManager.loadAll(); -> AddReloadListenerEvent (datapack reload)
        // TODO(port): BCLibProxy.getProxy().fmlPostInit();
        // TODO(port): BuildCraftObjectCaches.fmlPostInit();
        // TODO(port): VanillaListHandlers.fmlPostInit();
        // TODO(port): MarkerCache.postInit();
        // TODO(port): MessageManager.fmlPostInit();
    }

    // TODO(port): /buildcraft command -> RegisterCommandsEvent + Brigadier (was CommandBuildCraft)

    // TODO(port): the static TagManager block registering guide / guide_note / debugger. TagManager mostly
    // existed to drive 1.12 registry names, model locations and translation keys; on 1.21.1 those come from
    // the DeferredRegister id, so TagManager should shrink to a translation-key/model helper (or go away).

    public static Error throwBadClass(Error e, Class<?> cls) throws Error {
        throw new Error(
            "Bad " + cls + " loaded from " + cls.getClassLoader() + " domain: " + cls.getProtectionDomain(), e
        );
    }
}
