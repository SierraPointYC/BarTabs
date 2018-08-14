package org.spyc.bartabs.app;

public interface Acr3xNotifListener {
    void onUUIDAavailable(String uuid);
    void onFirmwareVersionAvailable(String firmwareVersion);
    void onStatusAvailable(int battery, int sleep);
    void onPiccPowerOn(String atr);
    void operationFailure(String error);
}
