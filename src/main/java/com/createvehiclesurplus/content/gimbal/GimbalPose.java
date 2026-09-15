package com.createvehiclesurplus.content.gimbal;

import net.minecraft.util.Mth;

/**
 * How the gyroscope's three moving parts sit for a given block entity state, in degrees about the
 * model's own axes (the shaft along X, the block built for axis X; the client turns the whole
 * assembly for axis Z). Pure, so it is game-tested and shared by the Flywheel visual, the
 * fallback renderer and the offline preview.
 * <ul>
 * <li>{@code outer}: the outer frame pivots on the shaft axis and counter-rotates the vehicle's
 * lean, so it stays level with the world while the vehicle rolls, as a gyroscope does.</li>
 * <li>{@code inner}: the inner frame pivots on Z and shows the balancing torque as precession: a
 * torque about X on a rotor spinning about +Y tips the spin axis toward +X (a negative rotation
 * about Z), and the other way for a rotor spinning the other way.</li>
 * <li>{@code rotor}: spins about Y at shaft speed with Create's kinetic angle, and stops when the
 * block is switched off by redstone.</li>
 * </ul>
 * While balancing, a slow low-amplitude hunting wobble rides on both frames so the gyroscope never
 * looks frozen.
 */
public record GimbalPose(float outerDegrees, float innerDegrees, float rotorDegrees) {
    /** The outer frame shows at most this much lean either way (the tuning target is 40 degrees). */
    public static final float LEAN_DISPLAY_MAX = 45.0f;
    /** Inner frame precession at full effort. */
    public static final float TILT_MAX = 30.0f;
    /** Hunting wobble on the outer frame while balancing: amplitude in degrees, period in ticks. */
    public static final float WOBBLE_DEGREES = 0.8f;
    public static final float WOBBLE_PERIOD = 34f;
    /** Nutation on the inner frame while balancing: amplitude in degrees, period in ticks. */
    public static final float NUTATION_DEGREES = 2.5f;
    public static final float NUTATION_PERIOD = 48f;
    /** Create's kinetic rotation: degrees per tick per unit of speed (RotatingInstance.SPEED_MULTIPLIER). */
    public static final float DEGREES_PER_TICK_PER_SPEED = 3f / 10f;

    public static final GimbalPose REST = new GimbalPose(0, 0, 0);

    /** The outer frame's angle about the shaft axis for a vehicle leaning {@code leanDegrees} about it. */
    public static float outerDegrees(float leanDegrees) {
        return -Mth.clamp(leanDegrees, -LEAN_DISPLAY_MAX, LEAN_DISPLAY_MAX);
    }

    /** The inner frame's precession about Z for a torque share of {@code effort} (-1..1 of the authority) at {@code speed}. */
    public static float innerDegrees(float effort, float speed) {
        return -TILT_MAX * Mth.clamp(effort, -1, 1) * Math.signum(speed);
    }

    /** The rotor's angle about +Y at {@code time} ticks for a shaft turning at {@code speed}, zero while declutched. */
    public static float rotorDegrees(float time, float speed, boolean active) {
        return active ? (time * speed * DEGREES_PER_TICK_PER_SPEED) % 360f : 0;
    }

    /** The full pose from the block entity's client-side display values at render time. */
    public static GimbalPose of(GimbalControllerBlockEntity be, float partialTicks, float time) {
        float activity = be.displayActivity(partialTicks);
        float speed = be.getSpeed();
        float outer = outerDegrees(be.displayLean(partialTicks))
                + activity * WOBBLE_DEGREES * Mth.sin(time * Mth.TWO_PI / WOBBLE_PERIOD);
        float inner = innerDegrees(be.displayEffort(partialTicks), speed)
                + activity * NUTATION_DEGREES * Mth.sin(time * Mth.TWO_PI / NUTATION_PERIOD + 1.3f);
        return new GimbalPose(outer, inner, rotorDegrees(time, speed, be.isActive()));
    }
}
