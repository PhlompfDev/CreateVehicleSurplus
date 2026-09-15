package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.Drive;
import com.createvehiclesurplus.content.transmission.Role;
import com.createvehiclesurplus.content.transmission.ShiftRules;
import com.createvehiclesurplus.content.transmission.ShiftRules.Refusal;
import com.createvehiclesurplus.content.transmission.ShiftRules.Result;
import com.createvehiclesurplus.content.transmission.ShiftRules.State;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** ShiftRules is pure logic; these tests need no blocks, the template is only there because GameTests need one. */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class ShiftRulesGameTests {
    private static final String TEMPLATE = "empty_5x3x5";

    private static State n(int gear) { return new State(gear, Drive.NEUTRAL); }
    private static State f(int gear) { return new State(gear, Drive.FORWARD); }
    private static State r(int gear) { return new State(gear, Drive.REVERSE); }

    @GameTest(template = TEMPLATE)
    public static void forward_hold_drives_and_release_goes_neutral(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.onSignal(Role.FORWARD, 15, n(0), 0), f(0));
        rules.markShifted(0);
        expectShift(rules.onSignal(Role.FORWARD, 0, f(0), 10), n(0));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void both_direction_faces_mean_neutral(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.onSignal(Role.FORWARD, 15, n(0), 0), f(0));
        rules.markShifted(0);
        expectShift(rules.onSignal(Role.REVERSE, 15, f(0), 10), n(0));
        check(rules.redstoneDrive() == Drive.NEUTRAL, "both faces should ask for neutral");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void drive_change_during_cooldown_lands_on_tick(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        rules.markShifted(0);
        expectNothing(rules.onSignal(Role.REVERSE, 15, n(0), 1));
        expectNothing(rules.tick(n(0), 3));
        expectShift(rules.tick(n(0), 4), r(0));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void up_pulse_shifts_then_repeats_while_held(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.onSignal(Role.UP, 15, n(0), 0), n(1));
        rules.markShifted(0);
        expectNothing(rules.tick(n(1), 7));
        expectShift(rules.tick(n(1), 8), n(2));
        rules.markShifted(8);
        expectNothing(rules.onSignal(Role.UP, 0, n(2), 9));
        expectNothing(rules.tick(n(2), 30));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void down_pulse_shifts_down(GameTestHelper helper) {
        expectShift(new ShiftRules().onSignal(Role.DOWN, 15, f(2), 0), f(1));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ends_refuse_with_limit(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectRefusal(rules.shiftUp(n(3), 0), Refusal.LIMIT);
        expectRefusal(rules.shiftDown(n(0), 0), Refusal.LIMIT);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void held_up_at_top_gear_stays_quiet(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectRefusal(rules.onSignal(Role.UP, 15, n(3), 0), Refusal.LIMIT);
        expectNothing(rules.tick(n(3), 8));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cooldown_blocks_fast_shifts(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        rules.markShifted(100);
        expectRefusal(rules.shiftUp(n(0), 103), Refusal.COOLDOWN);
        expectShift(rules.shiftUp(n(0), 104), n(1));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void computer_drive_holds_without_redstone(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.requestDrive(Drive.FORWARD, n(0), 0), f(0));
        rules.markShifted(0);
        check(rules.wantedDrive() == Drive.FORWARD, "computer drive should hold");
        expectNothing(rules.tick(f(0), 20));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void redstone_overrides_computer_drive(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.onSignal(Role.REVERSE, 15, n(0), 0), r(0));
        rules.markShifted(0);
        expectRefusal(rules.requestDrive(Drive.FORWARD, r(0), 10), Refusal.REDSTONE_OVERRIDE);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void releasing_redstone_hands_back_to_the_computer(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.requestDrive(Drive.FORWARD, n(0), 0), f(0));
        rules.markShifted(0);
        expectShift(rules.onSignal(Role.REVERSE, 15, f(0), 10), r(0));
        rules.markShifted(10);
        expectShift(rules.onSignal(Role.REVERSE, 0, r(0), 20), f(0));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void request_for_current_gear_is_accepted_unchanged(GameTestHelper helper) {
        Result result = new ShiftRules().requestGear(1, n(1), 0);
        check(result.accepted() && result.target() == null && result.refusal() == null, "expected UNCHANGED, got " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void repropagation_waits_for_cooldown(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.requestDrive(Drive.FORWARD, n(1), 0), f(1));
        rules.markShifted(0);
        rules.requestRepropagate();
        expectNothing(rules.tick(f(1), 3));
        expectShift(rules.tick(f(1), 4), f(1));
        rules.markShifted(4);
        expectNothing(rules.tick(f(1), 20));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void a_shift_clears_pending_repropagation(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        expectShift(rules.requestDrive(Drive.FORWARD, n(1), 0), f(1));
        rules.requestRepropagate();
        rules.markShifted(0);
        expectNothing(rules.tick(f(1), 10));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void saved_state_survives_reload(GameTestHelper helper) {
        ShiftRules rules = new ShiftRules();
        rules.onSignal(Role.UP, 15, n(0), 0);
        rules.requestDrive(Drive.REVERSE, n(1), 0);
        ShiftRules loaded = new ShiftRules();
        loaded.read(rules.write());
        expectNothing(loaded.onSignal(Role.UP, 15, n(1), 50));
        check(loaded.computerDrive() == Drive.REVERSE, "computer drive lost: " + loaded.computerDrive());
        helper.succeed();
    }

    private static void expectShift(Result result, State state) {
        check(result.accepted() && state.equals(result.target()), "expected shift to " + state + ", got " + result);
    }

    private static void expectNothing(Result result) {
        check(result.target() == null && result.refusal() == null, "expected no change, got " + result);
    }

    private static void expectRefusal(Result result, Refusal refusal) {
        check(!result.accepted() && result.refusal() == refusal, "expected refusal " + refusal + ", got " + result);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new GameTestAssertException(message);
    }
}
