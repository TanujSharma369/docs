package com.matter.casting.discoverable;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class MdnsAdvertiser {

    private static final String TAG = MdnsAdvertiser.class.getSimpleName();
    private static final String SERVICE_TYPE = "_matterd._udp";
    private static final String SERVICE_NAME = "MatterCastingApp";

    private final Context context;
    private NsdManager nsdManager;
    private NsdManager.RegistrationListener registrationListener;
    private String serviceName;
    private int port;

    public MdnsAdvertiser(Context context) {
        this.context = context;
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void startAdvertising(int port) {
        this.port = port;
        initializeRegistrationListener();
        registerService();
    }

    public void stopAdvertising() {
        if (nsdManager != null && registrationListener != null) {
            nsdManager.unregisterService(registrationListener);
            registrationListener = null;
        }
    }

    private void registerService() {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    private void initializeRegistrationListener() {
        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                serviceName = nsdServiceInfo.getServiceName();
                Log.d(TAG, "Service registered with name: " + serviceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e(TAG, "Service registration failed with error code: " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, "Service unregistered");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e(TAG, "Service unregistration failed with error code: " + errorCode);
            }
        };
    }
}
