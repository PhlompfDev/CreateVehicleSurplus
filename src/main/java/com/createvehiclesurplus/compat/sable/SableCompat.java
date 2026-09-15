package com.createvehiclesurplus.compat.sable;

import com.createvehiclesurplus.content.gimbal.GimbalControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sable integration entry point. Only ever reached behind {@code ModList.isLoaded("sable")}
 * ({@link com.createvehiclesurplus.content.gimbal.GimbalBlockEntityFactory#SABLE_ID}).
 * Returns the base type on purpose: the caller's bytecode then never mentions the subclass, so
 * the verifier has no reason to load Sable's interfaces when Sable is absent.
 */
public final class SableCompat {

    private SableCompat() {
    }

    public static GimbalControllerBlockEntity createGimbal(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new SableGimbalBlockEntity(type, pos, state);
    }
}
