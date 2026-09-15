package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.Drive;
import com.createvehiclesurplus.content.transmission.GearSpeeds;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

/** GearSpeeds is pure maths; the template is only there because GameTests need one. */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class GearSpeedsGameTests {
    private static final String TEMPLATE = "empty_5x3x5";

    @GameTest(template = TEMPLATE)
    public static void defaults_are_32_64_128_256(GameTestHelper helper) {
        check(Arrays.equals(new GearSpeeds().toArray(), new int[]{32, 64, 128, 256}), "defaults " + Arrays.toString(new GearSpeeds().toArray()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void set_clamps_to_1_and_256(GameTestHelper helper) {
        GearSpeeds speeds = new GearSpeeds();
        speeds.setAll(new int[]{0, 300, 50, -5});
        check(Arrays.equals(speeds.toArray(), new int[]{1, 256, 50, 1}), "clamped " + Arrays.toString(speeds.toArray()));
        check(!speeds.set(2, 50), "setting the same value should report no change");
        check(speeds.set(2, 51), "setting a new value should report a change");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void target_caps_at_server_max(GameTestHelper helper) {
        GearSpeeds speeds = new GearSpeeds();
        check(speeds.target(3, 128) == 128, "4th gear should cap at 128");
        check(speeds.target(0, 128) == 32, "1st gear is under the cap");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void modifier_hits_the_target(GameTestHelper helper) {
        check(GearSpeeds.modifier(64, Drive.FORWARD, 16) == 4f, "64 from 16 forward");
        check(GearSpeeds.modifier(64, Drive.REVERSE, 16) == -4f, "64 from 16 reverse");
        check(-16 * GearSpeeds.modifier(64, Drive.FORWARD, -16) == -64f, "forward follows a negative input");
        check(-16 * GearSpeeds.modifier(64, Drive.REVERSE, -16) == 64f, "reverse opposes a negative input");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void modifier_is_zero_in_neutral_or_without_input(GameTestHelper helper) {
        check(GearSpeeds.modifier(64, Drive.NEUTRAL, 16) == 0, "neutral");
        check(GearSpeeds.modifier(64, Drive.FORWARD, 0) == 0, "no input");
        helper.succeed();
    }

    /** Create breaks a block conveying more than the max speed, so float error must never round up. */
    @GameTest(template = TEMPLATE)
    public static void modifier_never_overshoots(GameTestHelper helper) {
        for (int target : new int[]{1, 32, 64, 100, 128, 200, 256})
            for (int input = 1; input <= 256; input++) {
                float out = input * GearSpeeds.modifier(target, Drive.FORWARD, input);
                check(out <= target && out > target - 0.01f, "input " + input + " target " + target + " gave " + out);
                float back = input * GearSpeeds.modifier(target, Drive.REVERSE, input);
                check(-back <= target && -back > target - 0.01f, "reverse input " + input + " target " + target + " gave " + back);
            }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void read_clamps_saved_values_and_keeps_defaults_when_missing(GameTestHelper helper) {
        GearSpeeds speeds = new GearSpeeds();
        speeds.read(new int[]{999, 10});
        check(Arrays.equals(speeds.toArray(), new int[]{256, 10, 128, 256}), "read " + Arrays.toString(speeds.toArray()));
        helper.succeed();
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new GameTestAssertException(message);
    }
}
