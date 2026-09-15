package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.Drive;
import com.createvehiclesurplus.content.transmission.TransmissionComputerApi;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

import static com.createvehiclesurplus.gametest.TransmissionGameTests.*;

/** What a computer sees through the {@code transmission} peripheral. */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class TransmissionComputerGameTests {

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void computer_sets_and_reads_the_gear(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(10, () -> {
            TransmissionComputerApi api = api(helper);
            check(Arrays.equals(api.setGear(2), new Object[]{true}), "setGear refused");
            // Inside the cooldown: accepted now, lands on a later tick.
            check(Arrays.equals(api.setDirection("forward"), new Object[]{true}), "setDirection refused");
        });
        helper.runAfterDelay(25, () -> {
            TransmissionComputerApi api = api(helper);
            check(api.getGear() == 2, "getGear returned " + api.getGear());
            check(api.getDirection().equals("forward"), "getDirection returned " + api.getDirection());
            check(api.getInputSpeed() != 0, "input speed is 0");
            check(Math.abs(api.getOutputSpeed() - Math.signum(api.getInputSpeed()) * 64) < 0.01, "output " + api.getOutputSpeed());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void computer_gets_refusal_reasons(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(10, () -> {
            check(Arrays.equals(api(helper).shiftDown(), new Object[]{false, "limit"}), "expected limit");
            helper.setBlock(REVERSE_FACE, Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(20, () -> {
            TransmissionComputerApi api = api(helper);
            check(api.getDirection().equals("reverse"), "getDirection returned " + api.getDirection());
            check(Arrays.equals(api.setDirection("forward"), new Object[]{false, "redstone_override"}), "expected redstone_override");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void bad_arguments_are_errors(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            TransmissionComputerApi api = api(helper);
            expectError(() -> api.setGear(5), "setGear(5)");
            expectError(() -> api.setGear(0), "setGear(0)");
            expectError(() -> api.setDirection("sideways"), "setDirection(sideways)");
            expectError(() -> api.setGearSpeed(9, 100), "setGearSpeed(9, 100)");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void computer_sets_gear_speeds(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            TransmissionComputerApi api = api(helper);
            check(api.setGearSpeed(1, 300) == 256, "setGearSpeed should clamp to 256");
            check(api.getGearSpeeds().equals(List.of(256, 64, 128, 256)), "getGearSpeeds returned " + api.getGearSpeeds());
            helper.succeed();
        });
    }

    private static void expectError(Runnable call, String what) {
        try {
            call.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        check(false, what + " should throw");
    }

    private static TransmissionComputerApi api(GameTestHelper helper) {
        return new TransmissionComputerApi(transmission(helper));
    }
}
