package com.createvehiclesurplus.client.ponder;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.VehicleSurplusBlocks;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Ponder entry point. Registered once on the client from {@link com.createvehiclesurplus.client.VehicleSurplusClient};
 * the scenes themselves live in {@link FuelTankScenes}, {@link DifferentialScenes}, {@link TransmissionScenes} and
 * {@link GimbalScenes}, their structures in {@code assets/createvehiclesurplus/ponder/} (written by the local
 * generator script) and their text in the lang file under {@code createvehiclesurplus.ponder.<scene>.*}.
 */
public class VehicleSurplusPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CreateVehicleSurplus.ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        HELPER.forComponents(VehicleSurplusBlocks.FUEL_TANK)
                .addStoryBoard("fuel_tank/feeding", FuelTankScenes::feeding, AllCreatePonderTags.FLUIDS);
        HELPER.forComponents(VehicleSurplusBlocks.LONG_FUEL_TANK)
                .addStoryBoard("fuel_tank/long", FuelTankScenes::longTank, AllCreatePonderTags.FLUIDS)
                .addStoryBoard("fuel_tank/feeding", FuelTankScenes::feeding);
        HELPER.forComponents(VehicleSurplusBlocks.DIFFERENTIAL)
                .addStoryBoard("differential", DifferentialScenes::differential, AllCreatePonderTags.KINETIC_RELAYS);
        HELPER.forComponents(VehicleSurplusBlocks.TRANSMISSION)
                .addStoryBoard("transmission", TransmissionScenes::transmission, AllCreatePonderTags.KINETIC_RELAYS);
        HELPER.forComponents(VehicleSurplusBlocks.GIMBAL_CONTROLLER)
                .addStoryBoard("gimbal_controller", GimbalScenes::gimbalController, AllCreatePonderTags.KINETIC_APPLIANCES);
    }
}
