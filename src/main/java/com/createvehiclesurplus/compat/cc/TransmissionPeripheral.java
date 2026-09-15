package com.createvehiclesurplus.compat.cc;

import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.createvehiclesurplus.content.transmission.TransmissionComputerApi;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The {@code transmission} peripheral: Lua bindings over {@link TransmissionComputerApi}. */
public class TransmissionPeripheral implements IPeripheral {
    private final TransmissionBlockEntity be;
    private final TransmissionComputerApi api;

    public TransmissionPeripheral(TransmissionBlockEntity be) {
        this.be = be;
        this.api = new TransmissionComputerApi(be);
    }

    @Override
    public String getType() {
        return "transmission";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof TransmissionPeripheral peripheral && peripheral.be == be;
    }

    @Override
    public Object getTarget() {
        return be;
    }

    @LuaFunction(mainThread = true)
    public final int getGear() {
        return api.getGear();
    }

    @LuaFunction(mainThread = true)
    public final MethodResult setGear(int gear) throws LuaException {
        try {
            return MethodResult.of(api.setGear(gear));
        } catch (IllegalArgumentException e) {
            throw new LuaException(e.getMessage());
        }
    }

    @LuaFunction(mainThread = true)
    public final MethodResult shiftUp() {
        return MethodResult.of(api.shiftUp());
    }

    @LuaFunction(mainThread = true)
    public final MethodResult shiftDown() {
        return MethodResult.of(api.shiftDown());
    }

    @LuaFunction(mainThread = true)
    public final String getDirection() {
        return api.getDirection();
    }

    @LuaFunction(mainThread = true)
    public final MethodResult setDirection(String direction) throws LuaException {
        try {
            return MethodResult.of(api.setDirection(direction));
        } catch (IllegalArgumentException e) {
            throw new LuaException(e.getMessage());
        }
    }

    @LuaFunction(mainThread = true)
    public final List<Integer> getGearSpeeds() {
        return api.getGearSpeeds();
    }

    @LuaFunction(mainThread = true)
    public final int setGearSpeed(int gear, int rpm) throws LuaException {
        try {
            return api.setGearSpeed(gear, rpm);
        } catch (IllegalArgumentException e) {
            throw new LuaException(e.getMessage());
        }
    }

    @LuaFunction(mainThread = true)
    public final double getInputSpeed() {
        return api.getInputSpeed();
    }

    @LuaFunction(mainThread = true)
    public final double getOutputSpeed() {
        return api.getOutputSpeed();
    }
}
