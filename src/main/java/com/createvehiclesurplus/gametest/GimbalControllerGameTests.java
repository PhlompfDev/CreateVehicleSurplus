package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.VehicleSurplusBlocks;
import com.createvehiclesurplus.content.gimbal.GimbalControllerBlock;
import com.createvehiclesurplus.content.gimbal.GimbalControllerBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Gimbal Controller as a kinetic block: an inline relay on one horizontal axis with a
 * redstone off switch. Rig: Creative Motor -> shaft -> gimbal -> shaft along X (or Z).
 */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class GimbalControllerGameTests {
    private static final String TEMPLATE = "empty_5x3x5";
    private static final BlockPos MOTOR = new BlockPos(0, 1, 2);
    private static final BlockPos INPUT = new BlockPos(1, 1, 2);
    private static final BlockPos GIMBAL = new BlockPos(2, 1, 2);
    private static final BlockPos OUTPUT = new BlockPos(3, 1, 2);
    private static final BlockPos SIDE = new BlockPos(2, 1, 1);

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void relays_rotation_along_x(GameTestHelper helper) {
        placeRigX(helper);
        helper.runAfterDelay(10, () -> {
            float in = speedAt(helper, INPUT);
            helper.assertTrue(in != 0, "input shaft is not turning");
            helper.assertTrue(speedAt(helper, OUTPUT) == in, "output shaft should turn like the input");
            helper.assertTrue(gimbal(helper).isActive(), "gimbal should be active while turning");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void relays_rotation_along_z(GameTestHelper helper) {
        BlockPos motor = new BlockPos(2, 1, 0);
        BlockPos input = new BlockPos(2, 1, 1);
        BlockPos output = new BlockPos(2, 1, 3);
        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        helper.setBlock(input, shaft(Axis.Z));
        helper.setBlock(GIMBAL, gimbalState(Axis.Z));
        helper.setBlock(output, shaft(Axis.Z));
        helper.runAfterDelay(10, () -> {
            float in = speedAt(helper, input);
            helper.assertTrue(in != 0, "input shaft is not turning");
            helper.assertTrue(speedAt(helper, output) == in, "output shaft should turn like the input");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void side_faces_carry_no_shaft(GameTestHelper helper) {
        placeRigX(helper);
        helper.setBlock(SIDE, shaft(Axis.Z));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(speedAt(helper, SIDE) == 0, "a shaft on a side face must not be driven");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void redstone_switches_it_off(GameTestHelper helper) {
        placeRigX(helper);
        helper.runAfterDelay(5, () -> {
            helper.setBlock(GIMBAL.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(helper.getBlockState(GIMBAL).getValue(GimbalControllerBlock.POWERED), "POWERED should follow the signal");
            helper.assertTrue(!gimbal(helper).isActive(), "a powered gimbal is not active");
            helper.assertTrue(gimbal(helper).status() == GimbalControllerBlockEntity.Status.OFF, "status should be OFF");
            helper.setBlock(GIMBAL.above(), Blocks.AIR.defaultBlockState());
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(!helper.getBlockState(GIMBAL).getValue(GimbalControllerBlock.POWERED), "POWERED should clear");
            helper.assertTrue(gimbal(helper).isActive(), "gimbal should be active again");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void status_reports_idle_off_ship_and_lean(GameTestHelper helper) {
        helper.setBlock(GIMBAL, gimbalState(Axis.X));
        helper.runAfterDelay(5, () -> {
            GimbalControllerBlockEntity be = gimbal(helper);
            helper.assertTrue(be.status() == GimbalControllerBlockEntity.Status.IDLE, "no rotation -> IDLE, got " + be.status());
            helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
            helper.setBlock(INPUT, shaft(Axis.X));
        });
        helper.runAfterDelay(15, () -> {
            GimbalControllerBlockEntity be = gimbal(helper);
            helper.assertTrue(be.status() == GimbalControllerBlockEntity.Status.NOT_ON_SHIP, "turning in the overworld -> NOT_ON_SHIP, got " + be.status());
            be.setOnShip(true);
            be.setLean(12.34);
            helper.assertTrue(be.status() == GimbalControllerBlockEntity.Status.BALANCING, "on a ship and turning -> BALANCING, got " + be.status());
            helper.assertTrue(Math.abs(be.leanDegrees() - 12.34f) < 1e-3, "lean should be stored, got " + be.leanDegrees());
            helper.succeed();
        });
    }

    private static void placeRigX(GameTestHelper helper) {
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        helper.setBlock(INPUT, shaft(Axis.X));
        helper.setBlock(GIMBAL, gimbalState(Axis.X));
        helper.setBlock(OUTPUT, shaft(Axis.X));
    }

    private static BlockState gimbalState(Axis axis) {
        return VehicleSurplusBlocks.GIMBAL_CONTROLLER.getDefaultState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, axis);
    }

    private static BlockState shaft(Axis axis) {
        return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
    }

    private static GimbalControllerBlockEntity gimbal(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(GIMBAL);
        if (!(be instanceof GimbalControllerBlockEntity gimbal))
            throw new GameTestAssertException("no gimbal block entity at " + GIMBAL);
        return gimbal;
    }

    private static float speedAt(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof KineticBlockEntity kinetic))
            throw new GameTestAssertException("no kinetic block entity at " + pos);
        return kinetic.getSpeed();
    }
}
