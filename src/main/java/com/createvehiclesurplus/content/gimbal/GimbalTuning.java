package com.createvehiclesurplus.content.gimbal;

/** Every number the balancing behaviour depends on. Playtest tunables; nothing else carries a magic number. */
public final class GimbalTuning {
    /** Proportional gain in 1/s^2: about a 1 Hz return to the target lean. */
    public static final double KP = 40.0;
    /** Derivative gain in 1/s, critically damped for KP. */
    public static final double KD = 2 * Math.sqrt(KP);
    /** Largest angular acceleration the controller asks for, rad/s^2, whatever the error. */
    public static final double ACCEL_MAX = 20.0;
    /** The vehicle never targets more lean than this. */
    public static final double LEAN_MAX_DEGREES = 40.0;
    /** Torque one gimbal can supply (Sable mass units * block^2 / s^2). A ~30-block bike needs ~600 at full error. */
    public static final double AUTHORITY = 2000.0;
    /** Magnitudes at or below this count as zero: gravity, inertia about the roll axis, a projected up vector. */
    public static final double EPSILON = 1e-6;

    private GimbalTuning() {
    }
}
