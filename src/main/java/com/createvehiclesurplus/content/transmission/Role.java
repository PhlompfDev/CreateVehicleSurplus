package com.createvehiclesurplus.content.transmission;

/**
 * What a long face of the Transmission does. Declared in ring order around the shaft
 * (Up, Forward, Down, Reverse), so Up is opposite Down and Forward opposite Reverse.
 */
public enum Role {
    UP("up"),
    FORWARD("forward"),
    DOWN("down"),
    REVERSE("reverse");

    public static final Role[] VALUES = values();

    private final String id;

    Role(String id) {
        this.id = id;
    }

    /** Lower-case id used in NBT keys, lang keys and behaviour type names. */
    public String id() {
        return id;
    }
}
