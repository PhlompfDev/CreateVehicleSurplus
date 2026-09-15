package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.VehicleSurplusBlocks;
import com.createvehiclesurplus.content.transmission.Drive;
import com.createvehiclesurplus.content.transmission.Role;
import com.createvehiclesurplus.content.transmission.ShiftRules;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

/**
 * Rig: a Creative Motor (16 RPM) at the west end drives shaft -> Transmission -> shaft along X.
 * With AXIS = X and ROLES = 0 the role faces are Up = UP, Forward = SOUTH, Down = DOWN,
 * Reverse = NORTH (TransmissionBlock.ring(X) = UP, SOUTH, DOWN, NORTH).
 */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class TransmissionGameTests {
    static final String TEMPLATE = "empty_5x3x5";
    static final int[] DEFAULT_SPEEDS = {32, 64, 128, 256};

    static final BlockPos MOTOR = new BlockPos(0, 1, 1);
    static final BlockPos INPUT = new BlockPos(1, 1, 1);
    static final BlockPos BOX = new BlockPos(2, 1, 1);
    static final BlockPos OUTPUT = new BlockPos(3, 1, 1);
    static final BlockPos UP_FACE = new BlockPos(2, 2, 1);
    static final BlockPos FORWARD_FACE = new BlockPos(2, 1, 2);
    static final BlockPos DOWN_FACE = new BlockPos(2, 0, 1);
    static final BlockPos REVERSE_FACE = new BlockPos(2, 1, 0);

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void first_gear_outputs_32(GameTestHelper helper) { driveTest(helper, 0); }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void second_gear_outputs_64(GameTestHelper helper) { driveTest(helper, 1); }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void third_gear_outputs_128(GameTestHelper helper) { driveTest(helper, 2); }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void fourth_gear_outputs_256(GameTestHelper helper) { driveTest(helper, 3); }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void neutral_disconnects_output(GameTestHelper helper) {
        placeRig(helper, 1, Drive.NEUTRAL);
        helper.runAfterDelay(10, () -> {
            check(speedAt(helper, INPUT) != 0, "input shaft is not turning");
            checkSpeed(helper, OUTPUT, 0);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void reverse_turns_the_output_backwards(GameTestHelper helper) {
        placeRig(helper, 1, Drive.REVERSE);
        helper.runAfterDelay(10, () -> {
            checkSpeed(helper, OUTPUT, -Math.signum(speedAt(helper, INPUT)) * 64);
            helper.succeed();
        });
    }

    /** Driven from the east, the west shaft is the output. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void target_follows_drive_direction(GameTestHelper helper) {
        helper.setBlock(BOX, box(1, Drive.FORWARD));
        helper.setBlock(FORWARD_FACE, Blocks.REDSTONE_BLOCK);
        helper.setBlock(new BlockPos(1, 1, 1), shaft(Axis.X));
        helper.setBlock(new BlockPos(3, 1, 1), shaft(Axis.X));
        helper.setBlock(new BlockPos(4, 1, 1), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST));
        helper.runAfterDelay(10, () -> {
            float in = speedAt(helper, new BlockPos(3, 1, 1));
            check(in != 0, "input shaft is not turning");
            checkSpeed(helper, new BlockPos(1, 1, 1), Math.signum(in) * 64);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void forward_wire_drives_and_release_goes_neutral(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(10, () -> helper.setBlock(FORWARD_FACE, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(20, () -> {
            checkState(helper, 0, Drive.FORWARD);
            checkSpeed(helper, OUTPUT, Math.signum(speedAt(helper, INPUT)) * 32);
            helper.setBlock(FORWARD_FACE, Blocks.AIR);
        });
        helper.runAfterDelay(30, () -> {
            checkState(helper, 0, Drive.NEUTRAL);
            checkSpeed(helper, OUTPUT, 0);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void both_direction_wires_mean_neutral(GameTestHelper helper) {
        placeRig(helper, 0, Drive.FORWARD);
        helper.runAfterDelay(10, () -> helper.setBlock(REVERSE_FACE, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(20, () -> {
            checkState(helper, 0, Drive.NEUTRAL);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void held_up_wire_climbs_to_top_gear(GameTestHelper helper) {
        placeRig(helper, 0, Drive.FORWARD);
        helper.runAfterDelay(10, () -> helper.setBlock(UP_FACE, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(50, () -> {
            checkState(helper, 3, Drive.FORWARD);
            checkSpeed(helper, OUTPUT, Math.signum(speedAt(helper, INPUT)) * 256);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void output_holds_target_when_input_changes(GameTestHelper helper) {
        placeRig(helper, 0, Drive.FORWARD);
        helper.runAfterDelay(10, () -> motor(helper).generatedSpeed.setValue(64));
        helper.runAfterDelay(40, () -> {
            float in = speedAt(helper, INPUT);
            check(Math.abs(in) == 64, "input is not 64 RPM: " + in);
            checkSpeed(helper, OUTPUT, Math.signum(in) * 32);
            helper.succeed();
        });
    }

    /** A turbocharged Diesel engine turns at 192 RPM: climbing to 256 must never trip Create's max-speed rule. */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void survives_every_shift_at_192_rpm(GameTestHelper helper) {
        placeRig(helper, 0, Drive.FORWARD);
        helper.runAfterDelay(5, () -> motor(helper).generatedSpeed.setValue(192));
        for (int i = 0; i < 3; i++)
            helper.runAfterDelay(20 + i * 8, () -> check(transmission(helper).requestShift(true).accepted(), "shift up refused"));
        helper.runAfterDelay(70, () -> {
            check(helper.getBlockState(BOX).getBlock() instanceof TransmissionBlock, "the Transmission broke");
            checkState(helper, 3, Drive.FORWARD);
            float in = speedAt(helper, INPUT);
            check(Math.abs(in) == 192, "input is not 192 RPM: " + in);
            checkSpeed(helper, OUTPUT, Math.signum(in) * 256);
            helper.succeed();
        });
    }

    /** Rotating the roles once moves Up from the top face to the south face. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void rotated_roles_move_the_up_face(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            BlockPos abs = helper.absolutePos(BOX);
            BlockState state = helper.getLevel().getBlockState(abs);
            ((TransmissionBlock) state.getBlock()).rotateRoles(helper.getLevel(), abs, state);
            check(TransmissionBlock.faceOf(helper.getLevel().getBlockState(abs), Role.UP) == Direction.SOUTH, "Up should face south after one rotation");
        });
        helper.runAfterDelay(10, () -> helper.setBlock(FORWARD_FACE, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(14, () -> {
            checkState(helper, 1, Drive.NEUTRAL);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void requests_respect_cooldown(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(10, () -> {
            TransmissionBlockEntity be = transmission(helper);
            check(be.requestGear(1).accepted(), "first request should be accepted");
            ShiftRules.Result second = be.requestGear(2);
            check(second.refusal() == ShiftRules.Refusal.COOLDOWN, "expected cooldown, got " + second);
        });
        helper.runAfterDelay(20, () -> {
            check(transmission(helper).requestGear(2).accepted(), "request after the cooldown should be accepted");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void gear_speeds_clamp_and_survive_save(GameTestHelper helper) {
        placeRig(helper, 0, Drive.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            TransmissionBlockEntity be = transmission(helper);
            be.setGearSpeeds(new int[]{0, 300, 50, -5});
            int[] expected = {1, 256, 50, 1};
            check(Arrays.equals(be.speeds().toArray(), expected), "clamped to " + Arrays.toString(be.speeds().toArray()));
            CompoundTag saved = be.saveWithoutMetadata(helper.getLevel().registryAccess());
            helper.setBlock(BOX, Blocks.AIR);
            helper.setBlock(BOX, box(0, Drive.NEUTRAL));
            TransmissionBlockEntity fresh = transmission(helper);
            fresh.loadWithComponents(saved, helper.getLevel().registryAccess());
            check(Arrays.equals(fresh.speeds().toArray(), expected), "loaded " + Arrays.toString(fresh.speeds().toArray()));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void editing_the_active_speed_repropagates(GameTestHelper helper) {
        placeRig(helper, 0, Drive.FORWARD);
        helper.runAfterDelay(10, () -> transmission(helper).setGearSpeed(0, 48));
        helper.runAfterDelay(25, () -> {
            checkSpeed(helper, OUTPUT, Math.signum(speedAt(helper, INPUT)) * 48);
            helper.succeed();
        });
    }

    /** A script editing the active gear's speed every tick must not detach/re-attach every tick, or Create destroys the block. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void rapid_speed_edits_keep_the_driveline(GameTestHelper helper) {
        placeRig(helper, 0, Drive.FORWARD);
        for (int tick = 10; tick <= 49; tick++) {
            int rpm = tick % 2 == 0 ? 32 : 48;
            helper.runAfterDelay(tick, () -> transmission(helper).setGearSpeed(0, rpm));
        }
        helper.runAfterDelay(50, () -> transmission(helper).setGearSpeed(0, 40));
        helper.runAfterDelay(70, () -> {
            check(helper.getBlockState(BOX).getBlock() instanceof TransmissionBlock, "the Transmission broke");
            check(helper.getBlockState(OUTPUT).getBlock() == AllBlocks.SHAFT.get(), "the output shaft broke");
            checkSpeed(helper, OUTPUT, Math.signum(speedAt(helper, INPUT)) * 40);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void legacy_save_loads_neutral_in_first_gear(GameTestHelper helper) {
        placeRig(helper, 2, Drive.FORWARD);
        helper.runAfterDelay(10, () -> {
            check(speedAt(helper, OUTPUT) != 0, "output is not spinning up before the legacy reload");
            TransmissionBlockEntity be = transmission(helper);
            CompoundTag tag = be.saveWithoutMetadata(helper.getLevel().registryAccess());
            tag.remove("GearSpeeds");
            tag.putIntArray("Linked", new int[]{0, 0, 0, 15});
            tag.getCompound("ShiftRules").putIntArray("Strengths", new int[]{0, 0, 0, 15});
            // Keep the forward redstone out of the way, or the wire legitimately re-drives the reloaded state.
            helper.setBlock(FORWARD_FACE, Blocks.AIR);
            helper.setBlock(BOX, Blocks.AIR);
            helper.setBlock(BOX, box(2, Drive.NEUTRAL));
            TransmissionBlockEntity fresh = transmission(helper);
            fresh.loadWithComponents(tag, helper.getLevel().registryAccess());
            // The fresh block entity's initialize() already ran when it was placed above, before this legacy
            // tag was loaded into it; GameTestHelper has no way to simulate a real chunk reload (which reads
            // the saved tag before initialize() runs), so call it again here to exercise the reset the same
            // way a genuine world load would.
            fresh.initialize();
        });
        helper.runAfterDelay(30, () -> {
            checkState(helper, 0, Drive.NEUTRAL);
            checkSpeed(helper, OUTPUT, 0);
            helper.succeed();
        });
    }

    // ---- helpers (package-private: later test classes reuse them) ----

    static void driveTest(GameTestHelper helper, int gear) {
        placeRig(helper, gear, Drive.FORWARD);
        helper.runAfterDelay(10, () -> {
            float in = speedAt(helper, INPUT);
            check(in != 0, "input shaft is not turning");
            checkSpeed(helper, OUTPUT, Math.signum(in) * DEFAULT_SPEEDS[gear]);
            helper.succeed();
        });
    }

    /** Places the rig; a forward or reverse drive also gets a redstone block holding its face, or the rules would drop to neutral. */
    static void placeRig(GameTestHelper helper, int gear, Drive drive) {
        helper.setBlock(BOX, box(gear, drive));
        if (drive == Drive.FORWARD)
            helper.setBlock(FORWARD_FACE, Blocks.REDSTONE_BLOCK);
        if (drive == Drive.REVERSE)
            helper.setBlock(REVERSE_FACE, Blocks.REDSTONE_BLOCK);
        helper.setBlock(INPUT, shaft(Axis.X));
        helper.setBlock(OUTPUT, shaft(Axis.X));
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
    }

    static BlockState box(int gear, Drive drive) {
        return VehicleSurplusBlocks.TRANSMISSION.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Axis.X)
                .setValue(TransmissionBlock.ROLES, 0)
                .setValue(TransmissionBlock.GEAR, gear)
                .setValue(TransmissionBlock.DRIVE, drive);
    }

    static BlockState shaft(Axis axis) {
        return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
    }

    static CreativeMotorBlockEntity motor(GameTestHelper helper) {
        if (!(helper.getBlockEntity(MOTOR) instanceof CreativeMotorBlockEntity motor))
            throw new GameTestAssertException("no creative motor");
        return motor;
    }

    static TransmissionBlockEntity transmission(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(BOX);
        if (!(be instanceof TransmissionBlockEntity transmission))
            throw new GameTestAssertException("no Transmission at " + BOX);
        return transmission;
    }

    static float speedAt(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof KineticBlockEntity kinetic))
            throw new GameTestAssertException("no kinetic block entity at " + pos);
        return kinetic.getSpeed();
    }

    /** The ratio is rounded down so float error never overshoots, so allow a hair below. */
    static void checkSpeed(GameTestHelper helper, BlockPos pos, float expected) {
        float actual = speedAt(helper, pos);
        check(Math.abs(actual - expected) < 0.01f, "shaft at " + pos + " turns at " + actual + ", expected " + expected);
    }

    static void checkState(GameTestHelper helper, int gear, Drive drive) {
        TransmissionBlockEntity be = transmission(helper);
        check(be.gear() == gear && be.drive() == drive, "state is gear " + be.gear() + " " + be.drive() + ", expected gear " + gear + " " + drive);
    }

    static void check(boolean condition, String message) {
        if (!condition)
            throw new GameTestAssertException(message);
    }
}
