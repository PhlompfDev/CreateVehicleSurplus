package com.createvehiclesurplus.content.transmission;

import java.util.Arrays;
import java.util.List;

/**
 * Everything a computer can do with a Transmission, with no ComputerCraft types in sight: the CC
 * peripheral is a thin wrapper around this, and the GameTests call it directly. Gears are 1-based
 * for Lua. Requests go through the same {@link ShiftRules} as links and wires. Results use Lua
 * conventions: {@code {true}} or {@code {false, reason}}.
 */
public final class TransmissionComputerApi {
    private final TransmissionBlockEntity be;

    public TransmissionComputerApi(TransmissionBlockEntity be) {
        this.be = be;
    }

    public int getGear() {
        return be.gear() + 1;
    }

    /** @throws IllegalArgumentException for a gear outside 1..4 */
    public Object[] setGear(int gear) {
        return toLua(be.requestGear(checkGear(gear) - 1));
    }

    public Object[] shiftUp() {
        return toLua(be.requestShift(true));
    }

    public Object[] shiftDown() {
        return toLua(be.requestShift(false));
    }

    public String getDirection() {
        return be.drive().id();
    }

    /** @throws IllegalArgumentException for anything but "forward", "reverse" or "neutral" */
    public Object[] setDirection(String id) {
        Drive drive = Drive.byId(id);
        if (drive == null)
            throw new IllegalArgumentException("Unknown direction '" + id + "', expected forward, reverse or neutral");
        return toLua(be.requestDrive(drive));
    }

    public List<Integer> getGearSpeeds() {
        return Arrays.stream(be.speeds().toArray()).boxed().toList();
    }

    /** @return the stored (clamped) speed */
    public int setGearSpeed(int gear, int rpm) {
        be.setGearSpeed(checkGear(gear) - 1, rpm);
        return be.speeds().get(gear - 1);
    }

    public double getInputSpeed() {
        return be.inputSpeed();
    }

    public double getOutputSpeed() {
        return be.outputSpeed();
    }

    private static int checkGear(int gear) {
        if (gear < 1 || gear > GearSpeeds.GEARS)
            throw new IllegalArgumentException("Gear must be 1 to " + GearSpeeds.GEARS + ", got " + gear);
        return gear;
    }

    private static Object[] toLua(ShiftRules.Result result) {
        if (result.accepted())
            return new Object[]{true};
        return new Object[]{false, result.refusal() == null ? "unchanged" : result.refusal().id()};
    }
}
