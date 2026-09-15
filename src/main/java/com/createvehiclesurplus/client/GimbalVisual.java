package com.createvehiclesurplus.client;

import com.createvehiclesurplus.content.gimbal.GimbalControllerBlockEntity;
import com.createvehiclesurplus.content.gimbal.GimbalPose;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Flywheel visual: the two shaft stubs as kinetic rotating instances, and the outer frame, inner
 * frame and rotor as transformed instances posed every frame by {@link GimbalPose}. The pose is a
 * chain, each part rotated in the frame of the one outside it: yaw for the block axis, then the
 * outer frame about X, the inner frame about Z inside it, the rotor about Y inside that.
 */
public class GimbalVisual extends KineticBlockEntityVisual<GimbalControllerBlockEntity> implements SimpleDynamicVisual {
    private final RotatingInstance[] shafts = new RotatingInstance[2];
    private final TransformedInstance outer;
    private final TransformedInstance inner;
    private final TransformedInstance rotor;
    private final List<FlatLit> all = new ArrayList<>(5);

    public GimbalVisual(VisualizationContext context, GimbalControllerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Axis axis = rotationAxis();
        Direction[] ends = Iterate.directionsInAxis(axis);
        for (int i : Iterate.zeroAndOne) {
            shafts[i] = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(GimbalParts.SHAFT_END))
                    .createInstance().rotateToFace(Direction.SOUTH, ends[i]);
            all.add(shafts[i]);
        }
        outer = transformed(GimbalParts.OUTER_FRAME);
        inner = transformed(GimbalParts.INNER_FRAME);
        rotor = transformed(GimbalParts.ROTOR);
        animate(partialTick);
    }

    private TransformedInstance transformed(PartialModel partial) {
        TransformedInstance instance = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(partial)).createInstance();
        all.add(instance);
        return instance;
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
        Axis axis = rotationAxis();
        Vector3f origin = new Vector3f(getVisualPosition().getX(), getVisualPosition().getY(), getVisualPosition().getZ());
        for (RotatingInstance shaft : shafts)
            shaft.setup(blockEntity, axis, blockEntity.getSpeed()).setPosition(origin).setChanged();

        GimbalPose pose = GimbalPose.of(blockEntity, partialTick, AnimationTickHolder.getRenderTime(blockEntity.getLevel()));
        float yaw = GimbalParts.yawDegrees(axis);
        place(outer, origin, yaw).rotateXCenteredDegrees(pose.outerDegrees()).setChanged();
        place(inner, origin, yaw).rotateXCenteredDegrees(pose.outerDegrees()).rotateZCenteredDegrees(pose.innerDegrees()).setChanged();
        place(rotor, origin, yaw).rotateXCenteredDegrees(pose.outerDegrees()).rotateZCenteredDegrees(pose.innerDegrees())
                .rotateYCenteredDegrees(pose.rotorDegrees()).setChanged();
    }

    private static TransformedInstance place(TransformedInstance instance, Vector3f origin, float yaw) {
        return instance.setIdentityTransform().translate(origin.x, origin.y, origin.z).rotateYCenteredDegrees(yaw);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(all.toArray(FlatLit[]::new));
    }

    @Override
    protected void _delete() {
        all.forEach(i -> ((AbstractInstance) i).delete());
        all.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        all.forEach(i -> consumer.accept((Instance) i));
    }
}
