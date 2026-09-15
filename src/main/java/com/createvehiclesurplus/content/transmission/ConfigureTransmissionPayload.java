package com.createvehiclesurplus.content.transmission;

import com.createvehiclesurplus.CreateVehicleSurplus;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Client to server: the four gear speeds set in a Transmission's screen, sent when it closes. */
public record ConfigureTransmissionPayload(BlockPos pos, List<Integer> speeds) implements CustomPacketPayload {
    public static final Type<ConfigureTransmissionPayload> TYPE = new Type<>(CreateVehicleSurplus.rl("configure_transmission"));
    public static final StreamCodec<ByteBuf, ConfigureTransmissionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ConfigureTransmissionPayload::pos,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(GearSpeeds.GEARS)), ConfigureTransmissionPayload::speeds,
            ConfigureTransmissionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureTransmissionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> apply(payload, context.player()));
    }

    /** The checks Create's BlockEntityConfigurationPacket makes: not a spectator or adventurer, loaded, in reach. Values are clamped by the block entity. */
    public static void apply(ConfigureTransmissionPayload payload, Player player) {
        if (player.isSpectator() || !player.mayBuild())
            return;
        Level level = player.level();
        if (!level.isLoaded(payload.pos()) || !player.canInteractWithBlock(payload.pos(), 4))
            return;
        if (level.getBlockEntity(payload.pos()) instanceof TransmissionBlockEntity be)
            be.setGearSpeeds(payload.speeds().stream().mapToInt(Integer::intValue).toArray());
    }
}
