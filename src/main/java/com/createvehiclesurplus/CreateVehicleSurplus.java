package com.createvehiclesurplus;

import com.createvehiclesurplus.content.fuel_tank.FuelTankBlockEntity;
import com.createvehiclesurplus.content.link.SidedLinkInteractionHandler;
import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CreateVehicleSurplus.ID)
public class CreateVehicleSurplus {
    public static final String ID = "createvehiclesurplus";

    /** Registrate wired the way Create addons do it, so blocks get Create-style item tooltips for free. */
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
            .setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, Palette.STANDARD_CREATE));

    public CreateVehicleSurplus(IEventBus modBus, ModContainer container) {
        // One block does not warrant a creative tab; it sits next to the Fluid Tank in Create's own.
        REGISTRATE.defaultCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey());
        REGISTRATE.registerEventListeners(modBus);

        VehicleSurplusBlocks.register();
        VehicleSurplusBlockEntities.register();

        modBus.addListener(FuelTankBlockEntity::registerCapabilities);
        modBus.addListener(VehicleSurplusNetwork::register);
        NeoForge.EVENT_BUS.addListener(SidedLinkInteractionHandler::onRightClickBlock);
        // Fully qualified on purpose: the compat class (and CC's API) only loads when CC is installed.
        if (ModList.get().isLoaded("computercraft"))
            com.createvehiclesurplus.compat.cc.CcCompat.init(modBus);

        if (FMLEnvironment.dist.isClient())
            com.createvehiclesurplus.client.VehicleSurplusClient.init();
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}
