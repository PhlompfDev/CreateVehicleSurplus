package com.createvehiclesurplus.content.gimbal;

import com.createvehiclesurplus.VehicleSurplusBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;

/**
 * Balances a Simulated vehicle about the axis its shaft runs along. Kinetically it is a plain
 * inline relay on one horizontal axis (shafts on the two axis faces, nothing on the others);
 * redstone power switches the balancing off. The physics lives in the Sable subclass of
 * {@link GimbalControllerBlockEntity}, chosen by {@link GimbalBlockEntityFactory}.
 */
public class GimbalControllerBlock extends HorizontalAxisKineticBlock implements IBE<GimbalControllerBlockEntity> {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public GimbalControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.PUSH_ONLY;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide)
            return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered)
            // Flag 2: sync to clients, no further neighbour updates. Same block => block entity survives.
            level.setBlock(pos, state.setValue(POWERED, powered), 2);
    }

    @Override
    public Class<GimbalControllerBlockEntity> getBlockEntityClass() {
        return GimbalControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GimbalControllerBlockEntity> getBlockEntityType() {
        return VehicleSurplusBlockEntities.GIMBAL_CONTROLLER.get();
    }
}
