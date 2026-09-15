package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.ConfigureTransmissionPayload;
import com.createvehiclesurplus.content.transmission.Drive;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

import static com.createvehiclesurplus.gametest.TransmissionGameTests.*;

/** The screen's payload, applied on the server side the way the network handler does. */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class TransmissionPayloadGameTests {

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void payload_sets_clamped_speeds_within_reach(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.moveTo(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
            ConfigureTransmissionPayload.apply(new ConfigureTransmissionPayload(helper.absolutePos(BOX), List.of(40, 80, 300, 0)), player);
            int[] speeds = transmission(helper).speeds().toArray();
            check(Arrays.equals(speeds, new int[]{40, 80, 256, 1}), "speeds are " + Arrays.toString(speeds));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void payload_out_of_reach_is_ignored(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.moveTo(helper.absoluteVec(new Vec3(2.5, 1, 40)));
            ConfigureTransmissionPayload.apply(new ConfigureTransmissionPayload(helper.absolutePos(BOX), List.of(1, 1, 1, 1)), player);
            int[] speeds = transmission(helper).speeds().toArray();
            check(Arrays.equals(speeds, DEFAULT_SPEEDS), "speeds changed to " + Arrays.toString(speeds));
            helper.succeed();
        });
    }
}
