package com.createvehiclesurplus.client;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * The moving parts of the Transmission and where they sit: the layshaft rod and its cluster of
 * five gears in the channel along the body's (+p,+q) edge, and the sliding main-shaft gears, one
 * size per forward gear, that emerge from the body to mesh the selected cluster gear. All partials
 * are modelled along Z, centred on the block, like Create's half shaft, so {@code partialFacing}
 * / {@code rotateToFace(SOUTH, ...)} orient them and the instance position places them. The pixel
 * constants mirror {@code LAYOUT} in tools/gen_transmission_assets.py. Created before models
 * bake: {@link #init()} is called from client setup, as Create does with AllPartialModels.
 */
public final class TransmissionParts {
    public static final PartialModel LAY_ROD = PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_rod"));
    public static final PartialModel LAY_REVERSE = PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_reverse"));
    /** Cluster gears and sliders by size, one per forward gear (1st smallest). Reverse shares the smallest size. */
    public static final PartialModel[] LAY = {
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_1")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_2")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_3")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/lay_4"))};
    public static final PartialModel[] SLIDER = {
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/slider_1")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/slider_2")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/slider_3")),
            PartialModel.of(CreateVehicleSurplus.rl("block/transmission/slider_4"))};

    /** Layshaft centre, p and q, in pixels. */
    public static final float LAY_CENTRE = 12.75f;
    /** Where the slider parks in Neutral: under the socket zone, inside the body. */
    public static final float NEUTRAL_A = 8f;
    /** Layout positions along the shaft, pixels from the negative end: reverse, neutral, then gears 1-4 (GEARS in tools/gen_transmission_assets.py). */
    public static final float[] SLOT_A = {2.0f, NEUTRAL_A, 4.0f, 11.0f, 12.6f, 14.2f};
    /** Cluster gears turn opposite to the slider; this phase interleaves the teeth where speeds match. */
    public static final float MESH_OFFSET = 22.5f;
    /** Engagement slots: forward gears 0..3, then reverse. */
    public static final int SLOTS = TransmissionBlockEntity.ENGAGEMENT_SLOTS;
    public static final int REVERSE_SLOT = SLOTS - 1;

    private TransmissionParts() {
    }

    public static void init() {
        // Loads the class, which creates the partials above.
    }

    /** Engagement slot the gearbox meshes, or -1 in neutral. */
    public static int slotOf(TransmissionBlockEntity be) {
        return TransmissionBlockEntity.engagementSlot(be.gear(), be.drive());
    }

    /** Where an engagement slot's gear sits along the shaft, in pixels. */
    public static float slotA(int slot) {
        return SLOT_A[slot == REVERSE_SLOT ? 0 : slot + 2];
    }

    /** Partial size index (0..3) of a slot's cluster gear and slider; reverse uses the smallest size. */
    public static int sizeOf(int slot) {
        return slot == REVERSE_SLOT ? 0 : slot;
    }

    public static PartialModel clusterGear(int slot) {
        return slot == REVERSE_SLOT ? LAY_REVERSE : LAY[slot];
    }

    /** Where a slot's slider sits, {@code engagement} of the way from its Neutral park to its slot. */
    public static float sliderA(int slot, float engagement) {
        return NEUTRAL_A + (slotA(slot) - NEUTRAL_A) * engagement;
    }

    /** The shaft end the rotation comes in through: the source's side, or the negative end while unpowered. */
    public static Direction inputEnd(TransmissionBlockEntity be) {
        BlockState state = be.getBlockState();
        Axis axis = state.getValue(TransmissionBlock.AXIS);
        if (be.hasSource()) {
            Direction facing = be.getSourceFacing();
            if (facing.getAxis() == axis)
                return facing;
        }
        return Direction.get(AxisDirection.NEGATIVE, axis);
    }

    /** Offset from the block centre, in blocks, of a point on the main shaft {@code a} pixels from the negative end. */
    public static Vector3f onShaft(Axis axis, float a) {
        return world(axis, 0, 0, a - 8).div(16);
    }

    /** Offset from the block centre, in blocks, of a point on the layshaft {@code a} pixels from the negative end. */
    public static Vector3f onLayshaft(Axis axis, float a) {
        return world(axis, LAY_CENTRE - 8, LAY_CENTRE - 8, a - 8).div(16);
    }

    /** (p, q) cross-section coordinates and a along the shaft into world coordinates, as the asset generator does. */
    private static Vector3f world(Axis axis, float p, float q, float a) {
        return switch (axis) {
            case X -> new Vector3f(a, p, q);
            case Y -> new Vector3f(p, a, q);
            case Z -> new Vector3f(p, q, a);
        };
    }
}
