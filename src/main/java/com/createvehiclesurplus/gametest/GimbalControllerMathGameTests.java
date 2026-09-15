package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.gimbal.GimbalTuning;
import com.createvehiclesurplus.content.gimbal.RollController;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * The roll controller is pure JOML math, so these are unit tests wearing a GameTest badge: they
 * run in the same headless server as the block tests and need no world. Conventions: gravity is
 * (0, -9.81, 0), inertia is the identity unless said otherwise, one gimbal unless said otherwise.
 */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class GimbalControllerMathGameTests {
    private static final String TEMPLATE = "empty_5x3x5";
    private static final Vector3d GRAVITY = new Vector3d(0, -9.81, 0);
    private static final Vector3d X = new Vector3d(1, 0, 0);
    private static final Vector3d Z = new Vector3d(0, 0, 1);
    private static final double EPS = 1e-6;

    private static RollController.Output run(Quaterniond orientation, Vector3d angular, Vector3d linear, Vector3d axis, Matrix3d inertia, int count) {
        return RollController.compute(new RollController.Inputs(orientation, angular, linear, GRAVITY, axis, inertia, count));
    }

    private static RollController.Output run(Quaterniond orientation, Vector3d angular, Vector3d linear, Vector3d axis) {
        return run(orientation, angular, linear, axis, new Matrix3d(), 1);
    }

    private static Quaterniond rolledAboutX(double degrees) {
        return new Quaterniond().rotateX(Math.toRadians(degrees));
    }

    private static void assertClose(GameTestHelper helper, String what, double actual, double expected) {
        helper.assertTrue(Math.abs(actual - expected) < 1e-3, what + " = " + actual + ", expected " + expected);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void upright_at_rest_needs_no_torque(GameTestHelper helper) {
        RollController.Output out = run(new Quaterniond(), new Vector3d(), new Vector3d(), X);
        assertClose(helper, "torque", out.torque(), 0);
        assertClose(helper, "lean", out.leanDegrees(), 0);
        assertClose(helper, "target", out.targetLeanDegrees(), 0);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void leaning_at_rest_is_pushed_back_proportionally(GameTestHelper helper) {
        RollController.Output out = run(rolledAboutX(10), new Vector3d(), new Vector3d(), X);
        assertClose(helper, "lean", out.leanDegrees(), 10);
        assertClose(helper, "torque", out.torque(), -GimbalTuning.KP * Math.toRadians(10));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void roll_rate_is_damped(GameTestHelper helper) {
        RollController.Output out = run(new Quaterniond(), new Vector3d(1, 0, 0), new Vector3d(), X);
        assertClose(helper, "torque", out.torque(), -GimbalTuning.KD);
        helper.succeed();
    }

    /** Forward along +Z at 5 m/s, yawing +1 rad/s: the turn centre is at +X, so the target leans toward +X. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void turning_targets_a_lean_toward_the_turn_centre(GameTestHelper helper) {
        RollController.Output out = run(new Quaterniond(), new Vector3d(0, 1, 0), new Vector3d(0, 0, 5), Z);
        double expected = -Math.toDegrees(Math.atan2(5, 9.81)); // leaning to +X is a negative rotation about +Z
        assertClose(helper, "target", out.targetLeanDegrees(), expected);
        helper.assertTrue(out.torque() < 0, "torque should push toward the target, got " + out.torque());
        helper.succeed();
    }

    /** Same turn driving the other way along the axis: the centre is now at -X and the lean flips with it. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void forward_direction_does_not_matter(GameTestHelper helper) {
        RollController.Output out = run(new Quaterniond(), new Vector3d(0, 1, 0), new Vector3d(0, 0, -5), Z);
        assertClose(helper, "target", out.targetLeanDegrees(), Math.toDegrees(Math.atan2(5, 9.81)));
        helper.assertTrue(out.torque() > 0, "torque should push toward the target, got " + out.torque());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void target_lean_is_clamped(GameTestHelper helper) {
        RollController.Output out = run(new Quaterniond(), new Vector3d(0, 3, 0), new Vector3d(0, 0, 50), Z);
        assertClose(helper, "target", out.targetLeanDegrees(), -GimbalTuning.LEAN_MAX_DEGREES);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void angular_acceleration_is_capped(GameTestHelper helper) {
        // 80 degrees of error asks for 40 * 1.396 = 55.9 rad/s^2; the cap is 20.
        RollController.Output out = run(rolledAboutX(80), new Vector3d(), new Vector3d(), X);
        assertClose(helper, "torque", out.torque(), -GimbalTuning.ACCEL_MAX);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void heavy_vehicle_hits_the_authority_cap(GameTestHelper helper) {
        Matrix3d heavy = new Matrix3d().scale(1000);
        RollController.Output out = run(rolledAboutX(30), new Vector3d(), new Vector3d(), X, heavy, 1);
        assertClose(helper, "torque", out.torque(), -GimbalTuning.AUTHORITY);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void two_gimbals_share_the_demand(GameTestHelper helper) {
        RollController.Output one = run(rolledAboutX(10), new Vector3d(), new Vector3d(), X, new Matrix3d(), 1);
        RollController.Output two = run(rolledAboutX(10), new Vector3d(), new Vector3d(), X, new Matrix3d(), 2);
        assertClose(helper, "torque", two.torque(), one.torque() / 2);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void inertia_about_the_roll_axis_scales_the_torque(GameTestHelper helper) {
        Matrix3d inertia = new Matrix3d().scaling(3, 1, 1); // 3 about X, 1 about Y and Z
        RollController.Output aboutX = run(rolledAboutX(10), new Vector3d(), new Vector3d(), X, inertia, 1);
        assertClose(helper, "torque", aboutX.torque(), -3 * GimbalTuning.KP * Math.toRadians(10));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void degenerate_inputs_give_no_torque(GameTestHelper helper) {
        RollController.Output nan = run(new Quaterniond(), new Vector3d(), new Vector3d(Double.NaN, 0, 0), X);
        assertClose(helper, "nan torque", nan.torque(), 0);
        RollController.Output noGravity = RollController.compute(new RollController.Inputs(
                new Quaterniond(), new Vector3d(), new Vector3d(), new Vector3d(), X, new Matrix3d(), 1));
        assertClose(helper, "zero-g torque", noGravity.torque(), 0);
        RollController.Output noInertia = run(rolledAboutX(10), new Vector3d(), new Vector3d(), X, new Matrix3d().zero(), 1);
        assertClose(helper, "zero-inertia torque", noInertia.torque(), 0);
        helper.succeed();
    }
}
