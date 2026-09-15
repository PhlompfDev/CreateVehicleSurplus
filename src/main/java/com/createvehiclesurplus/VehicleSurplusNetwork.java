package com.createvehiclesurplus;

import com.createvehiclesurplus.content.transmission.ConfigureTransmissionPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Payload registration (MOD bus). */
public class VehicleSurplusNetwork {
    /** Bump when a payload's wire format changes. */
    private static final String VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION)
                .playToServer(ConfigureTransmissionPayload.TYPE, ConfigureTransmissionPayload.STREAM_CODEC, ConfigureTransmissionPayload::handle);
    }
}
