package com.createvehiclesurplus.content.transmission;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.content.link.SidedLinkBehaviour;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Transmission's state machine host. Create's rotation propagator asks
 * {@link #getRotationSpeedModifier} for the ratio on each face; the source face gets 1, the other
 * end {@code sign × target / |input|}, so the output turns at the current gear's target speed
 * whatever the input (0 in neutral, which disconnects it like a Clutch). Signals from wires and
 * links go through {@link ShiftRules}; accepted changes are applied with {@link TransmissionBlock#shift}.
 */
public class TransmissionBlockEntity extends SplitShaftBlockEntity {
    /** One link behaviour type per role, shared by every Transmission. */
    public static final Map<Role, BehaviourType<SidedLinkBehaviour>> LINK_TYPES = Util.make(new EnumMap<>(Role.class), map -> {
        for (Role role : Role.VALUES)
            map.put(role, SidedLinkBehaviour.newType("transmission_link_" + role.id()));
    });
    public static final int ENGAGEMENT_SLOTS = 5;

    private final ShiftRules rules = new ShiftRules();
    private final GearSpeeds speeds = new GearSpeeds();
    private final int[] wired = new int[Role.VALUES.length];
    private final int[] linked = new int[Role.VALUES.length];
    // Client-side only: how far each corner cog has slid in to mesh its pinion (0 parked, 1 engaged), eased on
    // every shift. Slots are the four forward gears then reverse; see engagementSlot.
    private final LerpedFloat[] engagement = new LerpedFloat[ENGAGEMENT_SLOTS];
    private boolean engagementStarted;
    // Set by read() when the saved tag predates GearSpeeds: the old Wired/Linked/ShiftRules slots mean
    // different roles now, so they're left at their zeroed defaults and initialize() resets the state instead.
    private boolean legacySave;

    public TransmissionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Runs inside SmartBlockEntity's constructor, before this class's fields are initialised:
    // don't store the links in a field here, look them up by type instead.
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        for (Role role : Role.VALUES)
            behaviours.add(new SidedLinkBehaviour(this, LINK_TYPES.get(role), role.id(),
                    Component.translatable(CreateVehicleSurplus.ID + ".transmission.role." + role.id()),
                    ValueBoxTransform.Dual.makeSlots(first -> new TransmissionSlot(first, role)),
                    strength -> setLinkedStrength(role, strength)));
    }

    public SidedLinkBehaviour link(Role role) {
        return getBehaviour(LINK_TYPES.get(role));
    }

    public int gear() {
        return getBlockState().getValue(TransmissionBlock.GEAR);
    }

    public Drive drive() {
        return getBlockState().getValue(TransmissionBlock.DRIVE);
    }

    public ShiftRules.State state() {
        return TransmissionBlock.stateOf(getBlockState());
    }

    public GearSpeeds speeds() {
        return speeds;
    }

    /**
     * The current gear's target speed. On the server it is also capped at Create's max rotation
     * speed; clients and the Ponder level have no server config, and only draw with it.
     */
    public int targetSpeed() {
        int max = level instanceof ServerLevel ? AllConfigs.server().kinetics.maxRotationSpeed.get() : GearSpeeds.CEILING;
        return speeds.target(gear(), max);
    }

    /** Output speed divided by input speed right now: 0 in neutral or without input. */
    public float ratio() {
        return GearSpeeds.modifier(targetSpeed(), drive(), getTheoreticalSpeed());
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (!hasSource() || face == getSourceFacing())
            return 1;
        return ratio();
    }

    @Override
    public void initialize() {
        super.initialize();
        // Runs before propagation, so there's nothing to detach yet - just reset the state directly.
        if (legacySave && level != null && !level.isClientSide && (gear() != 0 || drive() != Drive.NEUTRAL))
            level.setBlock(worldPosition, getBlockState().setValue(TransmissionBlock.GEAR, 0).setValue(TransmissionBlock.DRIVE, Drive.NEUTRAL),
                    Block.UPDATE_CLIENTS);
        legacySave = false;
        updateWiredSignals();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;
        if (level.isClientSide) {
            tickEngagement();
            return;
        }
        apply(rules.tick(state(), level.getGameTime()), false);
    }

    /** Slot the gearbox meshes: the gear's own slot driving forward, the reverse slot in reverse, none in neutral. */
    public static int engagementSlot(int gear, Drive drive) {
        return switch (drive) {
            case NEUTRAL -> -1;
            case FORWARD -> gear;
            case REVERSE -> ENGAGEMENT_SLOTS - 1;
        };
    }

    private void tickEngagement() {
        int live = engagementSlot(gear(), drive());
        for (int slot = 0; slot < engagement.length; slot++) {
            float target = slot == live ? 1 : 0;
            if (!engagementStarted) {
                engagement[slot] = LerpedFloat.linear().startWithValue(target);
                continue;
            }
            engagement[slot].chase(target, 0.35, LerpedFloat.Chaser.EXP);
            engagement[slot].tickChaser();
        }
        engagementStarted = true;
    }

    /** 0 while a corner cog is parked, 1 when it meshes its pinion; between while it slides. */
    public float engagement(int slot, float partialTicks) {
        if (!engagementStarted)
            return engagementSlot(gear(), drive()) == slot ? 1 : 0;
        return engagement[slot].getValue(partialTicks);
    }

    /** Re-reads the redstone signal on each role's face. Called on neighbour changes and role rotation. */
    public void updateWiredSignals() {
        if (level == null || level.isClientSide)
            return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof TransmissionBlock))
            return;
        for (Role role : Role.VALUES) {
            Direction face = TransmissionBlock.faceOf(state, role);
            int strength = level.getSignal(worldPosition.relative(face), face);
            if (strength != wired[role.ordinal()]) {
                wired[role.ordinal()] = strength;
                signalChanged(role);
            }
        }
    }

    /** A role's link network strength changed (called by that role's link behaviour). */
    void setLinkedStrength(Role role, int strength) {
        if (level == null || level.isClientSide || strength == linked[role.ordinal()])
            return;
        linked[role.ordinal()] = strength;
        signalChanged(role);
    }

    private void signalChanged(Role role) {
        int effective = Math.max(wired[role.ordinal()], linked[role.ordinal()]);
        apply(rules.onSignal(role, effective, state(), level.getGameTime()), true);
        setChanged();
        sendData();
    }

    /** A computer (or anything else) asks for a specific gear, 0-based. */
    public ShiftRules.Result requestGear(int gear) {
        return apply(rules.requestGear(gear, state(), level.getGameTime()), true);
    }

    public ShiftRules.Result requestShift(boolean up) {
        long now = level.getGameTime();
        return apply(up ? rules.shiftUp(state(), now) : rules.shiftDown(state(), now), true);
    }

    public ShiftRules.Result requestDrive(Drive drive) {
        ShiftRules.Result result = apply(rules.requestDrive(drive, state(), level.getGameTime()), true);
        setChanged();
        return result;
    }

    /** Changes one gear's target speed (clamped). Queues a re-propagation if it is the gear in use. */
    public void setGearSpeed(int gear, int rpm) {
        int previousTarget = targetSpeed();
        speeds.set(gear, rpm);
        afterSpeedEdit(previousTarget);
    }

    /** Changes every gear's target speed (clamped), as the screen sends them. Queues a re-propagation if it is the gear in use. */
    public void setGearSpeeds(int[] rpms) {
        int previousTarget = targetSpeed();
        speeds.setAll(rpms);
        afterSpeedEdit(previousTarget);
    }

    /** Queues a re-propagation instead of shifting immediately: every shift flickers the driveline, and a script editing a speed every tick would destroy it. */
    private void afterSpeedEdit(int previousTarget) {
        if (level == null || level.isClientSide)
            return;
        if (targetSpeed() != previousTarget && drive() != Drive.NEUTRAL)
            rules.requestRepropagate();
        setChanged();
        sendData();
    }

    public float inputSpeed() {
        return getSpeed();
    }

    public float outputSpeed() {
        return getSpeed() * ratio();
    }

    private ShiftRules.Result apply(ShiftRules.Result result, boolean feedback) {
        if (result.target() != null) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof TransmissionBlock block) {
                block.shift(level, worldPosition, state, result.target());
                rules.markShifted(level.getGameTime());
            }
        } else if (feedback && result.refusal() != null && level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.25f, 0.6f);
            server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8,
                    worldPosition.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.01);
        }
        return result;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("ShiftRules", rules.write());
        tag.putIntArray("GearSpeeds", speeds.toArray());
        tag.putIntArray("Wired", wired.clone());
        tag.putIntArray("Linked", linked.clone());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        // A save without GearSpeeds predates the role reorder: its Wired/Linked/ShiftRules slots would load as
        // the wrong roles, so leave them at their defaults and let initialize() reset the block state instead.
        if (!clientPacket && !tag.contains("GearSpeeds")) {
            legacySave = true;
            return;
        }
        rules.read(tag.getCompound("ShiftRules"));
        speeds.read(tag.getIntArray("GearSpeeds"));
        copyInto(tag.getIntArray("Wired"), wired);
        copyInto(tag.getIntArray("Linked"), linked);
    }

    private static void copyInto(int[] from, int[] to) {
        for (int i = 0; i < to.length && i < from.length; i++)
            to[i] = from[i];
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        new LangBuilder(CreateVehicleSurplus.ID).translate("gui.goggles.transmission.gear", gear() + 1, targetSpeed())
                .style(ChatFormatting.GOLD).forGoggles(tooltip);
        new LangBuilder(CreateVehicleSurplus.ID).translate("gui.goggles.transmission.drive." + drive().id())
                .style(ChatFormatting.GRAY).forGoggles(tooltip);
        new LangBuilder(CreateVehicleSurplus.ID).translate("gui.goggles.transmission.speed", rpm(inputSpeed()), rpm(outputSpeed()))
                .style(ChatFormatting.GRAY).forGoggles(tooltip);
        return true;
    }

    private static String rpm(float value) {
        return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.?0+$", "");
    }
}
