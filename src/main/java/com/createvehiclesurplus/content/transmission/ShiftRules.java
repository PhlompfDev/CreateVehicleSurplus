package com.createvehiclesurplus.content.transmission;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Decides every change of one Transmission's gear and drive. Pure logic with no world access, so
 * it can be tested directly. The block entity feeds it role signal strengths, ticks and computer
 * requests, applies the {@link State} it returns and reports back with {@link #markShifted}.
 * <p>
 * Forward and Reverse are held: while one is powered the output turns that way, and both powered is
 * neutral. With neither powered the computer's last drive holds (neutral by default). Drive is
 * level-triggered and re-checked every tick, so a change held back by the cooldown still lands.
 * Up/Down shift on a rising edge and repeat every {@link #REPEAT_TICKS} while held.
 */
public final class ShiftRules {
    /** Minimum ticks between shifts. Create destroys a block whose speed changes too often. */
    public static final int COOLDOWN_TICKS = 4;
    /** A held Up or Down signal shifts again this often. */
    public static final int REPEAT_TICKS = 8;
    private static final Role[] SHIFT_ROLES = {Role.UP, Role.DOWN};

    public enum Refusal {
        COOLDOWN("cooldown"),
        LIMIT("limit"),
        REDSTONE_OVERRIDE("redstone_override");

        private final String id;

        Refusal(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** A gear (0 to {@link GearSpeeds#GEARS} - 1) and a drive: everything the block state says about the gearbox. */
    public record State(int gear, Drive drive) {
        public State withGear(int gear) {
            return new State(gear, drive);
        }

        public State withDrive(Drive drive) {
            return new State(gear, drive);
        }
    }

    /**
     * @param accepted false for refusals and for signals that need nothing
     * @param target   the state to switch to, or null when it stays as it is
     * @param refusal  why a request was refused, or null
     */
    public record Result(boolean accepted, @Nullable State target, @Nullable Refusal refusal) {
        public static final Result NOTHING = new Result(false, null, null);
        public static final Result UNCHANGED = new Result(true, null, null);

        static Result shiftTo(State state) {
            return new Result(true, state, null);
        }

        static Result refuse(Refusal refusal) {
            return new Result(false, null, refusal);
        }
    }

    private final int[] strengths = new int[Role.VALUES.length];
    /** Game time at which the held Up (index 0) or Down (1) signal next repeats. */
    private final long[] nextRepeat = new long[SHIFT_ROLES.length];
    private long lastShift = Long.MIN_VALUE / 2;
    private Drive computerDrive = Drive.NEUTRAL;
    private boolean repropagatePending;

    public int strength(Role role) {
        return strengths[role.ordinal()];
    }

    /** The drive the direction faces ask for, or null when neither is powered. */
    @Nullable
    public Drive redstoneDrive() {
        boolean forward = strength(Role.FORWARD) > 0;
        boolean reverse = strength(Role.REVERSE) > 0;
        if (forward && reverse)
            return Drive.NEUTRAL;
        if (forward)
            return Drive.FORWARD;
        if (reverse)
            return Drive.REVERSE;
        return null;
    }

    public Drive computerDrive() {
        return computerDrive;
    }

    /** The drive the gearbox should be in now: redstone when a direction face is powered, else the computer's. */
    public Drive wantedDrive() {
        Drive redstone = redstoneDrive();
        return redstone != null ? redstone : computerDrive;
    }

    /** A role's effective strength (max of its link and its wire) is now {@code strength}. */
    public Result onSignal(Role role, int strength, State current, long now) {
        int previous = strengths[role.ordinal()];
        strengths[role.ordinal()] = strength;
        boolean risingEdge = previous == 0 && strength > 0;
        return switch (role) {
            case UP, DOWN -> {
                if (!risingEdge)
                    yield Result.NOTHING;
                nextRepeat[repeatIndex(role)] = now + REPEAT_TICKS;
                yield pulse(role == Role.UP, current, now);
            }
            case FORWARD, REVERSE -> followDrive(current, now);
        };
    }

    public Result shiftUp(State current, long now) {
        return pulse(true, current, now);
    }

    public Result shiftDown(State current, long now) {
        return pulse(false, current, now);
    }

    /** A computer asks for a specific gear (0-based). */
    public Result requestGear(int gear, State current, long now) {
        if (gear == current.gear())
            return Result.UNCHANGED;
        if (coolingDown(now))
            return Result.refuse(Refusal.COOLDOWN);
        return Result.shiftTo(current.withGear(gear));
    }

    /** A computer asks for a drive. It holds while no direction face is powered; during the cooldown it lands on a later tick. */
    public Result requestDrive(Drive drive, State current, long now) {
        if (redstoneDrive() != null)
            return Result.refuse(Refusal.REDSTONE_OVERRIDE);
        computerDrive = drive;
        if (drive == current.drive() || coolingDown(now))
            return Result.UNCHANGED;
        return Result.shiftTo(current.withDrive(drive));
    }

    /** A speed edit on the gear in use needs the output re-propagated, which waits for the cooldown. */
    public void requestRepropagate() {
        repropagatePending = true;
    }

    /** Called every server tick: lands a pending drive change, then a pending re-propagation, then repeats held shifts. */
    public Result tick(State current, long now) {
        Result drive = followDrive(current, now);
        if (drive.target() != null)
            return drive;
        if (repropagatePending && !coolingDown(now))
            return Result.shiftTo(current);
        for (Role role : SHIFT_ROLES) {
            int i = repeatIndex(role);
            if (strength(role) == 0 || now < nextRepeat[i] || coolingDown(now))
                continue;
            nextRepeat[i] = now + REPEAT_TICKS;
            Result result = pulse(role == Role.UP, current, now);
            if (result.target() != null)
                return result;
        }
        return Result.NOTHING;
    }

    public void markShifted(long now) {
        lastShift = now;
        repropagatePending = false;
    }

    private Result pulse(boolean up, State current, long now) {
        int target = current.gear() + (up ? 1 : -1);
        if (target < 0 || target >= GearSpeeds.GEARS)
            return Result.refuse(Refusal.LIMIT);
        if (coolingDown(now))
            return Result.refuse(Refusal.COOLDOWN);
        return Result.shiftTo(current.withGear(target));
    }

    private Result followDrive(State current, long now) {
        Drive wanted = wantedDrive();
        if (wanted == current.drive() || coolingDown(now))
            return Result.NOTHING;
        return Result.shiftTo(current.withDrive(wanted));
    }

    private static int repeatIndex(Role role) {
        return role == Role.UP ? 0 : 1;
    }

    private boolean coolingDown(long now) {
        return now - lastShift < COOLDOWN_TICKS;
    }

    /** Strengths are saved so a signal held across a reload is not mistaken for a new pulse. */
    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Strengths", strengths.clone());
        tag.putString("ComputerDrive", computerDrive.id());
        return tag;
    }

    public void read(CompoundTag tag) {
        int[] saved = tag.getIntArray("Strengths");
        for (int i = 0; i < strengths.length && i < saved.length; i++)
            strengths[i] = saved[i];
        Drive drive = Drive.byId(tag.getString("ComputerDrive"));
        computerDrive = drive == null ? Drive.NEUTRAL : drive;
    }
}
