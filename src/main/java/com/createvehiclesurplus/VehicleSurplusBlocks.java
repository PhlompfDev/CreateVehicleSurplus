package com.createvehiclesurplus;

import com.createvehiclesurplus.client.FuelTankModel;
import com.createvehiclesurplus.client.LongFuelTankModel;
import com.createvehiclesurplus.content.differential.DifferentialBlock;
import com.createvehiclesurplus.content.fuel_tank.FuelTankBlock;
import com.createvehiclesurplus.content.fuel_tank.FuelTankItem;
import com.createvehiclesurplus.content.gimbal.GimbalControllerBlock;
import com.createvehiclesurplus.content.long_fuel_tank.LongFuelTankBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.fluids.tank.FluidTankMovementBehavior;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;

public class VehicleSurplusBlocks {
    private static final CreateRegistrate REGISTRATE = CreateVehicleSurplus.REGISTRATE;

    public static final BlockEntry<FuelTankBlock> FUEL_TANK = REGISTRATE.block("fuel_tank", FuelTankBlock::new)
            .initialProperties(SharedProperties::copperMetal)
            .properties(p -> p.noOcclusion().isRedstoneConductor((state, level, pos) -> true))
            // Connected-texture model with our own sprites; same culling rules as Create's tank.
            .onRegister(CreateRegistrate.blockModel(() -> FuelTankModel::new))
            // On a Create contraption (train, bearing, ...) the tank is plain fluid storage.
            // Create's own mounted storage accepts any FluidTankBlockEntity, which we are.
            .transform(MountedFluidStorageType.mountedFluidStorage(AllMountedStorageTypes.FLUID_TANK))
            .onRegister(MovementBehaviour.movementBehaviour(new FluidTankMovementBehavior()))
            .addLayer(() -> RenderType::cutoutMipped)
            .item(FuelTankItem::new)
            .build()
            .register();

    /** The same tank lying down: a 1x1 tube along X or Z. Shares models, textures and feeding code. */
    public static final BlockEntry<LongFuelTankBlock> LONG_FUEL_TANK = REGISTRATE.block("long_fuel_tank", LongFuelTankBlock::new)
            .initialProperties(SharedProperties::copperMetal)
            .properties(p -> p.noOcclusion().isRedstoneConductor((state, level, pos) -> true))
            .onRegister(CreateRegistrate.blockModel(() -> LongFuelTankModel::new))
            .transform(MountedFluidStorageType.mountedFluidStorage(AllMountedStorageTypes.FLUID_TANK))
            .onRegister(MovementBehaviour.movementBehaviour(new FluidTankMovementBehavior()))
            .addLayer(() -> RenderType::cutoutMipped)
            .simpleItem()
            .register();

    /**
     * The Gearbox that keeps every output turning the input's way. Registered the way Create
     * registers its Gearbox: stone-like, no stress impact, brass casing connected textures on the
     * two free-axis faces so it blends into brass casing floors and walls.
     */
    public static final BlockEntry<DifferentialBlock> DIFFERENTIAL = REGISTRATE.block("differential", DifferentialBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion().mapColor(MapColor.TERRACOTTA_YELLOW))
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.BRASS_CASING)))
            .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, AllSpriteShifts.BRASS_CASING,
                    (state, face) -> face.getAxis() == state.getValue(BlockStateProperties.AXIS))))
            // Relays cost nothing, like the Gearbox. (Unregistered blocks default to 0 too; explicit is clearer.)
            .onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 0))
            .simpleItem()
            .register();

    /** Inline gearbox driven by frequencies on its four long faces. No stress impact, like every relay. */
    public static final BlockEntry<TransmissionBlock> TRANSMISSION = REGISTRATE.block("transmission", TransmissionBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion().mapColor(MapColor.TERRACOTTA_GREEN))
            .onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 0))
            .simpleItem()
            .register();

    /** Balances Simulated vehicles about its shaft axis. Same stress as a Mixer: it is doing work. */
    public static final BlockEntry<GimbalControllerBlock> GIMBAL_CONTROLLER = REGISTRATE.block("gimbal_controller", GimbalControllerBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion().mapColor(MapColor.COLOR_GRAY))
            .onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 4))
            .simpleItem()
            .register();

    public static void register() {
        // Forces class init so the Registrate entries above are queued before registry events fire.
    }
}
