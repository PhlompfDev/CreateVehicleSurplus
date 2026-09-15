package com.createvehiclesurplus.gametest;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.transmission.Gear;
import com.createvehiclesurplus.content.transmission.Role;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.createvehiclesurplus.gametest.TransmissionGameTests.*;

/** The four faces' built-in frequencies, driven by a real Create Redstone Link transmitter. */
@GameTestHolder(CreateVehicleSurplus.ID)
@PrefixGameTestTemplate(false)
public class TransmissionLinkGameTests {
    private static final BlockPos LINK = new BlockPos(0, 0, 3);

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void link_pulse_shifts_up(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        placeTransmitter(helper);
        helper.runAfterDelay(5, () -> tune(helper, Role.UP, Items.IRON_INGOT, Items.GOLD_INGOT));
        helper.runAfterDelay(6, () -> transmitter(helper).transmit(15));
        helper.runAfterDelay(15, () -> {
            checkGear(helper, Gear.QUARTER);
            transmitter(helper).transmit(0);
        });
        helper.runAfterDelay(25, () -> transmitter(helper).transmit(15));
        helper.runAfterDelay(35, () -> {
            checkGear(helper, Gear.HALF);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void link_analog_follows_bands(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        placeTransmitter(helper);
        helper.runAfterDelay(5, () -> tune(helper, Role.ANALOG, Items.COPPER_INGOT, Items.LAPIS_LAZULI));
        helper.runAfterDelay(6, () -> transmitter(helper).transmit(10));
        helper.runAfterDelay(15, () -> {
            checkGear(helper, Gear.HALF);
            transmitter(helper).transmit(4);
        });
        helper.runAfterDelay(25, () -> {
            checkGear(helper, Gear.NEUTRAL);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void frequencies_follow_their_role_when_rotated(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        placeTransmitter(helper);
        helper.runAfterDelay(5, () -> {
            tune(helper, Role.UP, Items.EMERALD, Items.DIAMOND);
            BlockPos abs = helper.absolutePos(BOX);
            BlockState state = helper.getLevel().getBlockState(abs);
            ((TransmissionBlock) state.getBlock()).rotateRoles(helper.getLevel(), abs, state);
            BlockState rotated = helper.getLevel().getBlockState(abs);
            Vec3 slot = transmission(helper).link(Role.UP).getSlot(true).getLocalOffset(helper.getLevel(), abs, rotated);
            // The slot sits ITEM_SINK px inside the face since the 0.7.0 remodel, so "on the south face" is z near 1, not past it.
            check(slot.z > 0.9, "Up's first slot should now be on the south face, got " + slot);
        });
        helper.runAfterDelay(8, () -> transmitter(helper).transmit(15));
        helper.runAfterDelay(18, () -> {
            checkGear(helper, Gear.QUARTER);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void frequencies_survive_save_and_load(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            TransmissionBlockEntity be = transmission(helper);
            be.link(Role.DOWN).setFrequency(true, new ItemStack(Items.COAL));
            be.link(Role.DOWN).setFrequency(false, new ItemStack(Items.QUARTZ));
            CompoundTag saved = be.saveWithoutMetadata(helper.getLevel().registryAccess());
            helper.setBlock(BOX, Blocks.AIR);
            helper.setBlock(BOX, box(Gear.NEUTRAL));
            TransmissionBlockEntity fresh = transmission(helper);
            fresh.loadWithComponents(saved, helper.getLevel().registryAccess());
            check(fresh.link(Role.DOWN).getFrequency(true).getStack().is(Items.COAL), "first frequency lost");
            check(fresh.link(Role.DOWN).getFrequency(false).getStack().is(Items.QUARTZ), "second frequency lost");
            check(fresh.link(Role.UP).getFrequency(true).getStack().isEmpty(), "Up should still be empty");
            helper.succeed();
        });
    }

    /**
     * Slots stack like a Redstone Link's: on a side face the first frequency is the upper slot; on
     * the top face of an X shaft it is the south one, as on Create's floor link.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void first_slot_is_the_upper_one(GameTestHelper helper) {
        placeRig(helper, Gear.NEUTRAL);
        helper.runAfterDelay(5, () -> {
            BlockPos abs = helper.absolutePos(BOX);
            BlockState state = helper.getLevel().getBlockState(abs);
            TransmissionBlockEntity be = transmission(helper);
            Vec3 sideFirst = be.link(Role.ANALOG).getSlot(true).getLocalOffset(helper.getLevel(), abs, state);
            Vec3 sideSecond = be.link(Role.ANALOG).getSlot(false).getLocalOffset(helper.getLevel(), abs, state);
            check(sideFirst.y > sideSecond.y, "side face: first slot " + sideFirst + " should be above " + sideSecond);
            Vec3 topFirst = be.link(Role.UP).getSlot(true).getLocalOffset(helper.getLevel(), abs, state);
            Vec3 topSecond = be.link(Role.UP).getSlot(false).getLocalOffset(helper.getLevel(), abs, state);
            check(topFirst.z > topSecond.z, "top face: first slot " + topFirst + " should be south of " + topSecond);
            check(topFirst.x == 0.5 && topSecond.x == 0.5, "top face slots should be centred along the shaft");
            helper.succeed();
        });
    }

    private static void placeTransmitter(GameTestHelper helper) {
        helper.setBlock(LINK, AllBlocks.REDSTONE_LINK.getDefaultState()
                .setValue(RedstoneLinkBlock.RECEIVER, false)
                .setValue(BlockStateProperties.FACING, Direction.UP));
    }

    /** Sets the same frequency pair on the transmitter and on one role of the Transmission. */
    private static void tune(GameTestHelper helper, Role role, Item first, Item second) {
        LinkBehaviour link = BlockEntityBehaviour.get(transmitter(helper), LinkBehaviour.TYPE);
        link.setFrequency(true, new ItemStack(first));
        link.setFrequency(false, new ItemStack(second));
        transmission(helper).link(role).setFrequency(true, new ItemStack(first));
        transmission(helper).link(role).setFrequency(false, new ItemStack(second));
    }

    private static RedstoneLinkBlockEntity transmitter(GameTestHelper helper) {
        if (!(helper.getBlockEntity(LINK) instanceof RedstoneLinkBlockEntity link))
            throw new GameTestAssertException("no Redstone Link at " + LINK);
        return link;
    }
}
