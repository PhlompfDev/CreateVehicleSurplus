package com.createvehiclesurplus.content.transmission;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Which way the output turns relative to the input, or not at all. The value of the block's {@code drive} property. */
public enum Drive implements StringRepresentable {
    NEUTRAL(0),
    FORWARD(1),
    REVERSE(-1);

    private final int sign;

    Drive(int sign) {
        this.sign = sign;
    }

    /** 1 forward, -1 reverse, 0 neutral. */
    public int sign() {
        return sign;
    }

    /** Lower-case id used in the block state, NBT, lang keys and the Lua API. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    @Nullable
    public static Drive byId(String id) {
        for (Drive drive : values())
            if (drive.id().equals(id))
                return drive;
        return null;
    }
}
