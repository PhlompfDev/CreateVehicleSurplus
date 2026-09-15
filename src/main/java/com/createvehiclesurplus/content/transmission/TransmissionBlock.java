package com.createvehiclesurplus.content.transmission;

import com.createvehiclesurplus.VehicleSurplusBlockEntities;
import com.createvehiclesurplus.client.TransmissionScreen;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * An inline gearbox for vehicle drivelines. Shafts on the two {@code AXIS} ends; the four long
 * faces each have a {@link Role}, taken from a ring of faces around the axis rotated by
 * {@link #ROLES}. The current gear and drive are the {@link #GEAR} and {@link #DRIVE} properties
 * so clients get them for free.
 */
public class TransmissionBlock extends AbstractEncasedShaftBlock implements IBE<TransmissionBlockEntity> {
    /** Quarter-turns of the role layout around the shaft. */
    public static final IntegerProperty ROLES = IntegerProperty.create("roles", 0, 3);
    /** Index (0-based) of the current gear; its speed lives in the block entity's {@link GearSpeeds}. */
    public static final IntegerProperty GEAR = IntegerProperty.create("gear", 0, GearSpeeds.GEARS - 1);
    /** Which way the output turns, or neutral. */
    public static final EnumProperty<Drive> DRIVE = EnumProperty.create("drive", Drive.class);

    // Consecutive faces around each axis; ROLES = 0 puts Up on the first one.
    private static final Direction[] RING_X = {Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH};
    private static final Direction[] RING_Y = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final Direction[] RING_Z = {Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST};

    public TransmissionBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ROLES, 0).setValue(GEAR, 0).setValue(DRIVE, Drive.NEUTRAL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROLES, GEAR, DRIVE);
        super.createBlockStateDefinition(builder);
    }

    public static Direction[] ring(Axis axis) {
        return switch (axis) {
            case X -> RING_X;
            case Y -> RING_Y;
            case Z -> RING_Z;
        };
    }

    public static Direction faceOf(BlockState state, Role role) {
        return ring(state.getValue(AXIS))[(role.ordinal() + state.getValue(ROLES)) % 4];
    }

    @Nullable
    public static Role roleAt(BlockState state, Direction face) {
        for (Role role : Role.VALUES)
            if (faceOf(state, role) == face)
                return role;
        return null;
    }

    /**
     * The "up" direction of a long face: its first frequency slot sits on that side of the second,
     * and its glyph and gear wheel are drawn upright along it. On a wall it is world up, like a
     * wall-mounted Redstone Link. On the top and bottom faces of a horizontal shaft it points across
     * the shaft (south on top of an X shaft, north underneath, as Create's floor and ceiling links do).
     */
    public static Direction faceUp(Axis axis, Direction face) {
        if (axis == Axis.Y || face.getAxis().isHorizontal())
            return Direction.UP;
        boolean top = face == Direction.UP;
        if (axis == Axis.X)
            return top ? Direction.SOUTH : Direction.NORTH;
        return top ? Direction.WEST : Direction.EAST;
    }

    public static ShiftRules.State stateOf(BlockState state) {
        return new ShiftRules.State(state.getValue(GEAR), state.getValue(DRIVE));
    }

    /** Axis as Create's encased shafts pick it; Up faces the sky on a horizontal shaft, the player on a vertical one. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Axis axis = state.getValue(AXIS);
        Direction upFace = axis == Axis.Y ? context.getHorizontalDirection().getOpposite() : Direction.UP;
        Direction[] ring = ring(axis);
        int roles = 0;
        for (int i = 0; i < ring.length; i++)
            if (ring[i] == upFace)
                roles = i;
        return state.setValue(ROLES, roles).setValue(GEAR, 0).setValue(DRIVE, Drive.NEUTRAL);
    }

    /** Wrench on a shaft end turns the roles around the shaft; on a long face Create's axis rotation applies. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getClickedFace().getAxis() != state.getValue(AXIS))
            return super.onWrenched(state, context);
        Level level = context.getLevel();
        if (!level.isClientSide) {
            rotateRoles(level, context.getClickedPos(), state);
            IWrenchable.playRotateSound(level, context.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }

    /** Shift-right-click with an empty hand opens the gear speed screen. Plain clicks stay free for the link slots. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown())
            return InteractionResult.PASS;
        // Same client-only hop as Create's Sequenced Gearshift.
        CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> withBlockEntityDo(level, pos, be -> displayScreen(be, player)));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @OnlyIn(Dist.CLIENT)
    private void displayScreen(TransmissionBlockEntity be, Player player) {
        if (player instanceof LocalPlayer)
            TransmissionScreen.open(be);
    }

    /** One quarter-turn of the role layout. Frequencies belong to roles, so they move with it. */
    public void rotateRoles(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.cycle(ROLES), Block.UPDATE_ALL);
        withBlockEntityDo(level, pos, TransmissionBlockEntity::updateWiredSignals);
    }

    /**
     * Change gear or drive the way Create's Gearshift flips POWERED: detach, change the state,
     * re-attach next tick. Called with the current state it just re-propagates (a gear speed edit).
     */
    public void shift(Level level, BlockPos pos, BlockState state, ShiftRules.State target) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic)
            RotationPropagator.handleRemoved(level, pos, kinetic);
        level.setBlock(pos, state.setValue(GEAR, target.gear()).setValue(DRIVE, target.drive()), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, 1, TickPriority.EXTREMELY_HIGH);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic)
            RotationPropagator.handleAdded(level, pos, kinetic);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide)
            withBlockEntityDo(level, pos, TransmissionBlockEntity::updateWiredSignals);
    }

    @Override
    public Class<TransmissionBlockEntity> getBlockEntityClass() {
        return TransmissionBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TransmissionBlockEntity> getBlockEntityType() {
        return VehicleSurplusBlockEntities.TRANSMISSION.get();
    }
}
