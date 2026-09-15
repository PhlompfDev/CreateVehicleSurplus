package com.createvehiclesurplus.client;

import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Flywheel visual: the two half shafts, the layshaft rod and its five cluster gears turning
 * against the input, and five sliding main-shaft gears of which the selected one has slid out
 * from under the socket zone to mesh its cluster gear and turn with the output. Which end is the
 * input follows the source, so speeds are re-read every frame; it is thirteen instances.
 */
public class TransmissionVisual extends KineticBlockEntityVisual<TransmissionBlockEntity> implements SimpleDynamicVisual {
    private final RotatingInstance[] shafts = new RotatingInstance[2];   // index 0: negative end, 1: positive end
    private final RotatingInstance rod;
    private final RotatingInstance[] cluster = new RotatingInstance[TransmissionParts.SLOTS];
    private final RotatingInstance[] sliders = new RotatingInstance[TransmissionParts.SLOTS];
    private final List<RotatingInstance> all = new ArrayList<>(13);

    public TransmissionVisual(VisualizationContext context, TransmissionBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Axis axis = rotationAxis();
        Direction[] ends = Iterate.directionsInAxis(axis);
        Direction positive = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        for (int i : Iterate.zeroAndOne) {
            shafts[i] = instance(AllPartialModels.SHAFT_HALF).rotateToFace(Direction.SOUTH, ends[i]);
            all.add(shafts[i]);
        }
        rod = instance(TransmissionParts.LAY_ROD).rotateToFace(Direction.SOUTH, positive);
        all.add(rod);
        for (int slot = 0; slot < TransmissionParts.SLOTS; slot++) {
            cluster[slot] = instance(TransmissionParts.clusterGear(slot)).rotateToFace(Direction.SOUTH, positive);
            sliders[slot] = instance(TransmissionParts.SLIDER[TransmissionParts.sizeOf(slot)]).rotateToFace(Direction.SOUTH, positive);
            all.add(cluster[slot]);
            all.add(sliders[slot]);
        }
        animate(partialTick);
    }

    private RotatingInstance instance(PartialModel partial) {
        return instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(partial)).createInstance();
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        animate(ctx.partialTick());
    }

    @Override
    public void update(float partialTick) {
        animate(partialTick);
    }

    private void animate(float partialTick) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof TransmissionBlock))
            return;
        Axis axis = state.getValue(TransmissionBlock.AXIS);
        Direction input = TransmissionParts.inputEnd(blockEntity);
        float in = blockEntity.getSpeed();
        float out = blockEntity.outputSpeed();
        Vector3f origin = new Vector3f(getVisualPosition().getX(), getVisualPosition().getY(), getVisualPosition().getZ());

        for (int i : Iterate.zeroAndOne) {
            Direction end = Iterate.directionsInAxis(axis)[i];
            shafts[i].setup(blockEntity, axis, end == input ? in : out).setPosition(origin).setChanged();
        }
        // The cluster turns against the input, like a layshaft driven off the input pinion.
        place(rod.setup(blockEntity, axis, -in), origin, TransmissionParts.onLayshaft(axis, 8));
        int live = TransmissionParts.slotOf(blockEntity);
        for (int slot = 0; slot < TransmissionParts.SLOTS; slot++) {
            place(cluster[slot].setup(blockEntity, axis, -in).setRotationOffset(cluster[slot].rotationOffset + TransmissionParts.MESH_OFFSET),
                    origin, TransmissionParts.onLayshaft(axis, TransmissionParts.slotA(slot)));
            float engagement = blockEntity.engagement(slot, partialTick);
            float speed = slot == live ? out : 0;
            place(sliders[slot].setup(blockEntity, axis, speed), origin, TransmissionParts.onShaft(axis, TransmissionParts.sliderA(slot, engagement)));
        }
    }

    private static void place(RotatingInstance instance, Vector3f origin, Vector3f offset) {
        instance.setPosition(origin).nudge(offset.x, offset.y, offset.z).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(all.toArray(FlatLit[]::new));
    }

    @Override
    protected void _delete() {
        all.forEach(AbstractInstance::delete);
        all.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        all.forEach(consumer);
    }
}
