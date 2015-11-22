package com.example.vladimir_notebook.compassapp;

/**
 * Created by vladimir-notebook on 20.11.15.
 */
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;

@BusInterface (name = "com.example.bus.compass")
public interface Compass_service {

    @BusMethod(replySignature = "d")
    public double getCompassAzimuth() throws BusException;

    @BusMethod(replySignature = "d")
    public double getCompassPitch() throws BusException;

    @BusMethod(replySignature = "d")
    public double getCompassRoll() throws BusException;
}
