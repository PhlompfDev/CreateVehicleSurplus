package com.createvehiclesurplus.content.gimbal;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * The Gimbal Controller without any physics: kinetic state, the redstone switch, the lean the
 * server last reported (for goggles), and whether the block currently sits on a Simulated
 * vehicle. The Sable subclass in {@code compat/sable} fills in the last two.
 */
public class GimbalControllerBlockEntity extends KineticBlockEntity {

    public enum Status {
        BALANCING("balancing"), IDLE("idle"), OFF("off"), NOT_ON_SHIP("no_ship");

        private final String key;

        Status(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    /** Client packets carry the lean when it moved this much or more... */
    private static final float SYNC_THRESHOLD_DEGREES = 1;
    /** ...but never more often than this. */
    private static final int SYNC_INTERVAL_TICKS = 10;

    private float leanDegrees;
    private float lastSentLean;
    private int ticksSinceSync;
    private boolean onShip;

    public GimbalControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    /** Turning and not switched off. Whether that does anything depends on being on a vehicle. */
    public boolean isActive() {
        return getSpeed() != 0 && !getBlockState().getValue(GimbalControllerBlock.POWERED);
    }

    public Status status() {
        if (getBlockState().getValue(GimbalControllerBlock.POWERED))
            return Status.OFF;
        if (getSpeed() == 0)
            return Status.IDLE;
        return onShip ? Status.BALANCING : Status.NOT_ON_SHIP;
    }

    public float leanDegrees() {
        return leanDegrees;
    }

    public boolean isOnShip() {
        return onShip;
    }

    /** Server side: the subclass reports the lean the controller measured. Synced by {@link #tick()}. */
    public void setLean(double degrees) {
        leanDegrees = (float) degrees;
    }

    /** Server side: refreshed every tick by the subclass; stays false without Sable. */
    public void setOnShip(boolean onShip) {
        if (this.onShip != onShip) {
            this.onShip = onShip;
            if (level != null && !level.isClientSide)
                sendData();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        ticksSinceSync++;
        if (ticksSinceSync >= SYNC_INTERVAL_TICKS && Math.abs(leanDegrees - lastSentLean) >= SYNC_THRESHOLD_DEGREES) {
            lastSentLean = leanDegrees;
            ticksSinceSync = 0;
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) {
            tag.putFloat("Lean", leanDegrees);
            tag.putBoolean("OnShip", onShip);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) {
            leanDegrees = tag.getFloat("Lean");
            onShip = tag.getBoolean("OnShip");
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Status status = status();
        LangBuilder line = new LangBuilder(CreateVehicleSurplus.ID);
        if (status == Status.BALANCING)
            line.translate("gui.goggles.gimbal." + status.key(), String.format(Locale.ROOT, "%.0f", Math.abs(leanDegrees)));
        else
            line.translate("gui.goggles.gimbal." + status.key());
        line.style(status == Status.BALANCING ? ChatFormatting.GREEN : ChatFormatting.GRAY).forGoggles(tooltip);
        return true;
    }
}
