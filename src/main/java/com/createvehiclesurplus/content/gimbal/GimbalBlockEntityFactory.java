package com.createvehiclesurplus.content.gimbal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/**
 * Builds the block entity for the Gimbal Controller: the Sable-aware subclass when Sable is
 * installed, the physics-free base otherwise. The only class outside {@code compat/sable} that
 * names the compat entry point, and only inside the {@code isLoaded} branch, so nothing from
 * Sable is ever resolved without it.
 */
public final class GimbalBlockEntityFactory {
    public static final String SABLE_ID = "sable";
    private static final boolean SABLE_LOADED = ModList.get().isLoaded(SABLE_ID);

    private GimbalBlockEntityFactory() {
    }

    public static GimbalControllerBlockEntity create(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        // Fully qualified on purpose: the compat class (and Sable's API) only loads when Sable is installed.
        if (SABLE_LOADED)
            return com.createvehiclesurplus.compat.sable.SableCompat.createGimbal(type, pos, state);
        return new GimbalControllerBlockEntity(type, pos, state);
    }
}
