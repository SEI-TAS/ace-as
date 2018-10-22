package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

public interface IPairingHandler
{
    /**
     * Code to actually perform the pairing procedure with a client.
     */
    boolean pairIoTDevice(byte[] pskBytes);
}
