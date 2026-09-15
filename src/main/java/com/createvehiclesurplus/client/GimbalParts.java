package com.createvehiclesurplus.client;

import com.createvehiclesurplus.CreateVehicleSurplus;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction.Axis;

/**
 * The moving parts of the Gimbal Controller: the two shaft stubs (modelled toward +Z like Create's
 * half shaft, so {@code partialFacing} / {@code rotateToFace(SOUTH, end)} orient them), and the
 * outer frame, inner frame and rotor, all modelled in block coordinates for axis X and posed about
 * the block centre by {@link com.createvehiclesurplus.content.gimbal.GimbalPose}. Geometry comes
 * from tools/gen_gimbal_assets.py. Created before models bake: {@link #init()} is called from
 * client setup, as Create does with AllPartialModels.
 */
public final class GimbalParts {
    public static final PartialModel SHAFT_END = PartialModel.of(CreateVehicleSurplus.rl("block/gimbal_controller/shaft_end"));
    public static final PartialModel OUTER_FRAME = PartialModel.of(CreateVehicleSurplus.rl("block/gimbal_controller/outer_frame"));
    public static final PartialModel INNER_FRAME = PartialModel.of(CreateVehicleSurplus.rl("block/gimbal_controller/inner_frame"));
    public static final PartialModel ROTOR = PartialModel.of(CreateVehicleSurplus.rl("block/gimbal_controller/rotor"));

    private GimbalParts() {
    }

    public static void init() {
        // Loads the class, which creates the partials above.
    }

    /**
     * How far to turn the axis-X assembly about Y for a block on {@code axis}: the blockstate's
     * {@code y: 90} for axis Z is a -90 degree rotation about +Y (vanilla rotates by minus the
     * declared angle), which takes model +X to world +Z.
     */
    public static float yawDegrees(Axis axis) {
        return axis == Axis.Z ? -90 : 0;
    }
}
