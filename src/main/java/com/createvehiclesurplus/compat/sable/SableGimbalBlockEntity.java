package com.createvehiclesurplus.compat.sable;

import com.createvehiclesurplus.content.gimbal.GimbalControllerBlockEntity;
import com.createvehiclesurplus.content.gimbal.GimbalTuning;
import com.createvehiclesurplus.content.gimbal.RollController;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

/**
 * The Gimbal Controller on a Simulated vehicle. Sable registers any block entity implementing
 * {@link BlockEntitySubLevelActor} with the vehicle's plot by itself and calls
 * {@link #sable$physicsTick} every physics substep (server thread, before queued forces flush).
 * <p>
 * Frames: the handle's velocities are world frame; the pose's orientation maps local to world;
 * the impulse handed back is vehicle-local, which is why the torque is applied along the
 * block's own axis vector rather than a world vector (Sable's reaction-wheel path does the same).
 * The propeller actor multiplies its thrust by the substep length before queueing it, so the
 * controller's torque is multiplied by {@code timeStep} here too.
 */
public class SableGimbalBlockEntity extends GimbalControllerBlockEntity implements BlockEntitySubLevelActor {
    private int gimbalCount = 1;
    private final Vector3d axis = new Vector3d();
    private final Vector3d angularVelocity = new Vector3d();
    private final Vector3d linearVelocity = new Vector3d();
    private final Vector3d impulse = new Vector3d();
    private final ForceTotal forces = new ForceTotal();

    public SableGimbalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        SubLevel subLevel = Sable.HELPER.getContaining(this);
        setOnShip(subLevel != null);
        if (subLevel instanceof ServerSubLevel server)
            gimbalCount = countActiveGimbals(server);
    }

    /**
     * Counts active Gimbal Controllers sharing this one's horizontal axis, floored at 1. A
     * gimbal on the other horizontal axis controls a different roll axis, so it must not
     * dilute this one's share of the vehicle's balancing torque.
     */
    private int countActiveGimbals(ServerSubLevel subLevel) {
        Axis myAxis = getBlockState().getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);
        int count = 0;
        for (BlockEntitySubLevelActor actor : subLevel.getPlot().getBlockEntityActors())
            if (actor instanceof SableGimbalBlockEntity gimbal && gimbal.isActive()
                    && gimbal.getBlockState().getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS) == myAxis)
                count++;
        return Math.max(1, count);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (!isActive() || !handle.isValid())
            return;
        MassData mass = subLevel.getMassTracker();
        if (mass == null || mass.isInvalid())
            return;

        Axis blockAxis = getBlockState().getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);
        axis.set(blockAxis == Axis.X ? 1 : 0, 0, blockAxis == Axis.Z ? 1 : 0);
        RollController.Output out = RollController.compute(new RollController.Inputs(
                subLevel.logicalPose().orientation(),
                handle.getAngularVelocity(angularVelocity),
                handle.getLinearVelocity(linearVelocity),
                DimensionPhysicsData.getGravity(subLevel.getLevel()),
                axis, mass.getInertiaTensor(), gimbalCount));
        if (out.isNone())
            return;
        setLean(out.leanDegrees());
        setEffort(out.torque() / GimbalTuning.AUTHORITY);
        if (out.torque() == 0)
            return;

        impulse.set(axis).mul(out.torque() * timeStep);
        forces.applyAngularImpulse(impulse);
        handle.applyForcesAndReset(forces);
    }
}
