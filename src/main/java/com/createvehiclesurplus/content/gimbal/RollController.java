package com.createvehiclesurplus.content.gimbal;

import org.joml.Matrix3dc;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * The balancing law, free of Minecraft and Sable types so it can be tested by itself.
 * <p>
 * Frames: {@code orientation} maps vehicle-local to world; velocities and gravity are world;
 * {@code rollAxisLocal} and {@code inertiaLocal} are vehicle-local. The output torque is a scalar
 * about {@code rollAxisLocal} (right-hand rule), which is the same axis in the world frame.
 * <p>
 * The target is "apparent up": world up plus the centripetal acceleration of the current turn
 * ({@code angularVelocity x linearVelocity}), so the vehicle leans into corners and stands
 * upright at rest. Only the component around the roll axis is controlled; pitch and yaw are
 * left to the physics.
 */
public final class RollController {

    public record Inputs(Quaterniondc orientation, Vector3dc angularVelocity, Vector3dc linearVelocity,
                         Vector3dc gravity, Vector3dc rollAxisLocal, Matrix3dc inertiaLocal, int gimbalCount) {
    }

    /** {@code torque} is this gimbal's share, already clamped. Lean angles in degrees, signed about the roll axis. */
    public record Output(double torque, double leanDegrees, double targetLeanDegrees) {
        static final Output NONE = new Output(0, 0, 0);

        /** True when the controller produced no real result (the {@link #NONE} sentinel): every field is zero. */
        public boolean isNone() {
            return torque == 0 && leanDegrees == 0 && targetLeanDegrees == 0;
        }
    }

    private RollController() {
    }

    public static Output compute(Inputs in) {
        double gravityLength = in.gravity().length();
        if (!(gravityLength > GimbalTuning.EPSILON))
            return Output.NONE;

        Vector3d axis = in.orientation().transform(new Vector3d(in.rollAxisLocal())).normalize();
        Vector3d worldUp = new Vector3d(in.gravity()).div(-gravityLength);
        Vector3d centripetal = new Vector3d(in.angularVelocity()).cross(in.linearVelocity());
        Vector3d apparentUp = new Vector3d(worldUp).mul(gravityLength).add(centripetal);
        Vector3d vehicleUp = in.orientation().transform(new Vector3d(0, 1, 0));

        Vector3d worldUpP = projectOntoPlane(worldUp, axis);
        Vector3d apparentUpP = projectOntoPlane(apparentUp, axis);
        Vector3d vehicleUpP = projectOntoPlane(vehicleUp, axis);
        if (worldUpP == null || apparentUpP == null || vehicleUpP == null)
            return Output.NONE;

        double leanMax = Math.toRadians(GimbalTuning.LEAN_MAX_DEGREES);
        double target = clamp(signedAngle(worldUpP, apparentUpP, axis), -leanMax, leanMax);
        double lean = signedAngle(worldUpP, vehicleUpP, axis);
        double error = wrap(target - lean);
        double rollRate = in.angularVelocity().dot(axis);

        Vector3d local = new Vector3d(in.rollAxisLocal()).normalize();
        double inertia = in.inertiaLocal().transform(new Vector3d(local)).dot(local);
        if (!(inertia > GimbalTuning.EPSILON))
            return Output.NONE;

        double demand = inertia * (GimbalTuning.KP * error - GimbalTuning.KD * rollRate);
        demand = clamp(demand, -inertia * GimbalTuning.ACCEL_MAX, inertia * GimbalTuning.ACCEL_MAX);
        double share = clamp(demand / Math.max(1, in.gimbalCount()), -GimbalTuning.AUTHORITY, GimbalTuning.AUTHORITY);

        double leanDegrees = Math.toDegrees(lean);
        double targetDegrees = Math.toDegrees(target);
        if (!Double.isFinite(share) || !Double.isFinite(leanDegrees) || !Double.isFinite(targetDegrees))
            return Output.NONE;
        return new Output(share, leanDegrees, targetDegrees);
    }

    /** {@code v} minus its component along the unit {@code axis}, normalised; null if nothing is left. */
    private static Vector3d projectOntoPlane(Vector3dc v, Vector3dc axis) {
        Vector3d out = new Vector3d(v).fma(-v.dot(axis), axis);
        double length = out.length();
        if (!(length > GimbalTuning.EPSILON))
            return null;
        return out.div(length);
    }

    /** Signed angle from {@code from} to {@code to}, both unit and perpendicular to the unit {@code axis}. */
    private static double signedAngle(Vector3dc from, Vector3dc to, Vector3dc axis) {
        double sin = new Vector3d(from).cross(to).dot(axis);
        double cos = from.dot(to);
        return Math.atan2(sin, cos);
    }

    private static double wrap(double radians) {
        return Math.atan2(Math.sin(radians), Math.cos(radians));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
