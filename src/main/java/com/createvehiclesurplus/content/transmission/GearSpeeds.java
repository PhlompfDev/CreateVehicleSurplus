package com.createvehiclesurplus.content.transmission;

/**
 * The four gears' target output speeds in RPM. Pure data and maths with no world access, so the
 * GameTests call it directly. Speeds are stored within 1..{@link #CEILING}; the server also caps
 * them at Create's configured max rotation speed when it computes the ratio.
 */
public final class GearSpeeds {
    public static final int GEARS = 4;
    /** Create's default maxRotationSpeed: the Transmission never targets more. */
    public static final int CEILING = 256;
    private static final int[] DEFAULTS = {32, 64, 128, 256};

    private final int[] speeds = DEFAULTS.clone();

    public int get(int gear) {
        return speeds[gear];
    }

    public int[] toArray() {
        return speeds.clone();
    }

    /** Stores a clamped speed. @return true if the stored value changed */
    public boolean set(int gear, int rpm) {
        int clamped = clamp(rpm);
        if (speeds[gear] == clamped)
            return false;
        speeds[gear] = clamped;
        return true;
    }

    /** Sets gears from {@code rpms}; missing entries keep their value. */
    public void setAll(int[] rpms) {
        for (int i = 0; i < GEARS && i < rpms.length; i++)
            set(i, rpms[i]);
    }

    public static int clamp(int rpm) {
        return Math.max(1, Math.min(CEILING, rpm));
    }

    /** A gear's target, capped at the server's max rotation speed. */
    public int target(int gear, int maxSpeed) {
        return Math.min(speeds[gear], maxSpeed);
    }

    /**
     * Output speed divided by input speed for a target: the input's own direction driving forward,
     * the opposite in reverse, 0 (disconnected, like a Clutch) in neutral or with no input. Rounded
     * down until {@code |input| × ratio} is at most the target, because Create breaks any block
     * that conveys more than the max speed and a float quotient can land a hair above it.
     */
    public static float modifier(int target, Drive drive, float inputSpeed) {
        if (drive == Drive.NEUTRAL || inputSpeed == 0)
            return 0;
        float input = Math.abs(inputSpeed);
        float ratio = target / input;
        while (input * ratio > target)
            ratio = Math.nextDown(ratio);
        return drive.sign() * ratio;
    }

    /** Loads saved speeds, clamping them; gears missing from {@code saved} keep their value. */
    public void read(int[] saved) {
        for (int i = 0; i < GEARS && i < saved.length; i++)
            speeds[i] = clamp(saved[i]);
    }
}
