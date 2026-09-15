package com.createvehiclesurplus;

import com.createvehiclesurplus.client.DifferentialRenderer;
import com.createvehiclesurplus.client.DifferentialVisual;
import com.createvehiclesurplus.client.LongFuelTankRenderer;
import com.createvehiclesurplus.client.TransmissionRenderer;
import com.createvehiclesurplus.client.TransmissionVisual;
import com.createvehiclesurplus.content.differential.DifferentialBlockEntity;
import com.createvehiclesurplus.content.fuel_tank.FuelTankBlockEntity;
import com.createvehiclesurplus.content.gimbal.GimbalBlockEntityFactory;
import com.createvehiclesurplus.content.gimbal.GimbalControllerBlockEntity;
import com.createvehiclesurplus.content.long_fuel_tank.LongFuelTankBlockEntity;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class VehicleSurplusBlockEntities {
    private static final CreateRegistrate REGISTRATE = CreateVehicleSurplus.REGISTRATE;

    public static final BlockEntityEntry<FuelTankBlockEntity> FUEL_TANK = REGISTRATE
            .blockEntity("fuel_tank", FuelTankBlockEntity::new)
            .validBlocks(VehicleSurplusBlocks.FUEL_TANK)
            // Create's fluid tank renderer only needs a FluidTankBlockEntity; reuse it as-is.
            .renderer(() -> FluidTankRenderer::new)
            .register();

    public static final BlockEntityEntry<LongFuelTankBlockEntity> LONG_FUEL_TANK = REGISTRATE
            .blockEntity("long_fuel_tank", LongFuelTankBlockEntity::new)
            .validBlocks(VehicleSurplusBlocks.LONG_FUEL_TANK)
            // Create's renderer draws the fluid column upward; the lying tank needs its own.
            .renderer(() -> LongFuelTankRenderer::new)
            .register();

    public static final BlockEntityEntry<DifferentialBlockEntity> DIFFERENTIAL = REGISTRATE
            .blockEntity("differential", DifferentialBlockEntity::new)
            // Flywheel draws the shafts; the block model itself renders normally (false = not through the visual).
            .visual(() -> DifferentialVisual::new, false)
            .validBlocks(VehicleSurplusBlocks.DIFFERENTIAL)
            .renderer(() -> DifferentialRenderer::new)
            .register();

    public static final BlockEntityEntry<TransmissionBlockEntity> TRANSMISSION = REGISTRATE
            .blockEntity("transmission", TransmissionBlockEntity::new)
            // The visual draws the half shafts, pinions and corner cogs at their own speeds.
            // true = the block-entity renderer keeps running under Flywheel (it draws the frequency items there).
            .visual(() -> TransmissionVisual::new, true)
            .validBlocks(VehicleSurplusBlocks.TRANSMISSION)
            .renderer(() -> TransmissionRenderer::new)
            .register();

    public static final BlockEntityEntry<GimbalControllerBlockEntity> GIMBAL_CONTROLLER = REGISTRATE
            .blockEntity("gimbal_controller", GimbalBlockEntityFactory::create)
            // The block model has no shaft; Create's own shaft partial spins through it (Flywheel), ShaftRenderer is the fallback.
            .visual(() -> SingleAxisRotatingVisual.<GimbalControllerBlockEntity>of(AllPartialModels.SHAFT), false)
            .validBlocks(VehicleSurplusBlocks.GIMBAL_CONTROLLER)
            .renderer(() -> ShaftRenderer::new)
            .register();

    public static void register() {
        // Forces class init so the Registrate entries above are queued before registry events fire.
    }
}
