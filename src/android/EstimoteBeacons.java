package com.oracle.mx.ux.cordova.estimotebeacons;

import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EstimoteBeacons extends CordovaPlugin {

    // Method names exposed to JavaScript
    public static final String START_MONITORING_BEACONS_IN_REGION = "startMonitoringBeaconsInRegion";
    public static final String STOP_MONITORING_BEACONS_IN_REGION = "stopMonitoringBeaconsInRegion";
    public static final String START_RANGING_BEACONS_IN_REGION = "startRangingBeaconsInRegion";
    public static final String STOP_RANGING_BEACONS_IN_REGION = "stopRangingBeaconsInRegion";
    public static final String GET_BEACONS = "getBeacons";

    // Constants
    private static final String REGION_ID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";

    // Variables
    private BeaconManager beaconManager;
    private Region currentRegion;
    private Utils util = new Utils();
    private List<Beacon> beacons = new ArrayList<Beacon>();
    private int inRegion = 0;

    /**
     * Initializes the plugin.
     * @param cordova The context of the main Activity
     * @param webView The CordovaWebView Cordova is running in
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        beaconManager = new BeaconManager(this.cordova.getActivity().getApplicationContext());
        beaconManager.setBackgroundScanPeriod(1000,0);
        currentRegion = new Region("regionId", REGION_ID, null, null);
    }

    /**
     * Executes the request and returns PluginResult.
     * @param action The action to execute
     * @param args JSONArray of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript
     * @return True if the action was valid, otherwise false
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(EstimoteBeacons.class.toString(), "action -> " + action);

        try {
            if(action.equalsIgnoreCase(START_MONITORING_BEACONS_IN_REGION)) {
                startMonitoringBeaconsInRegion(callbackContext);
                return true;
            }

            if(action.equalsIgnoreCase(STOP_MONITORING_BEACONS_IN_REGION)) {
                stopMonitoringBeaconsInRegion();
                callbackContext.success(callbackContext.getCallbackId());
                return true;
            }

            if(action.equalsIgnoreCase(START_RANGING_BEACONS_IN_REGION)) {
                startRangingBeaconsInRegion();
                callbackContext.success(callbackContext.getCallbackId());
                return true;
            }

            if(action.equalsIgnoreCase(STOP_RANGING_BEACONS_IN_REGION)) {
                stopRangingBeaconsInRegion();
                callbackContext.success(callbackContext.getCallbackId());
                return true;
            }

            if(action.equalsIgnoreCase(GET_BEACONS)) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, listToJSONArray(beacons)));
                return true;
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
            callbackContext.error(e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Starts ranging beacons in region.
     * @throws RemoteException
     */
    private void startRangingBeaconsInRegion() throws RemoteException {
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                if(beacons == null || beacons.isEmpty()) {
                    Log.d("DEBUG", "No beacons");
                } else {
                    EstimoteBeacons.this.beacons = beacons;
                }
            }
        });
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(currentRegion);
                } catch(RemoteException e) {
                    Log.e("DEBUG", "Cannot start ranging", e);
                }
            }
        });
    }

    /**
     * Stops ranging beacons in region.
     * @throws RemoteException
     */
    private void stopRangingBeaconsInRegion() throws RemoteException {
        beaconManager.stopRanging(currentRegion);
    }

    /**
     * Starts monitoring beacons in region.
     * @param callbackContext The callback id used when calling back into JavaScript
     * @throws RemoteException
     */
    private void startMonitoringBeaconsInRegion(final CallbackContext callbackContext) throws RemoteException {
        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onExitedRegion(Region region) {
                EstimoteBeacons.this.inRegion = 0;
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, EstimoteBeacons.this.inRegion);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }

            @Override
            public void onEnteredRegion(Region region, List<Beacon> beacons) {
                EstimoteBeacons.this.inRegion = 1;
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, EstimoteBeacons.this.inRegion);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startMonitoring(currentRegion);
                } catch(RemoteException e) {
                    Log.e("DEBUG", "Cannot start monitoring", e);
                }
            }
        });
    }

    /**
     * Stops monitoring beacons in region.
     * @throws RemoteException
     */
    private void stopMonitoringBeaconsInRegion() throws RemoteException {
        beaconManager.stopMonitoring(currentRegion);
    }

    /**
     * Converts passed list of beacons(@com.estimote.sdk.Beacon) to @JSONArray.
     * @param beacons List of beacons(@com.estimote.sdk.Beacon)
     * @return JSONArray
     * @throws JSONException
     */
    private JSONArray listToJSONArray(List<Beacon> beacons) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for(Beacon beacon : beacons) {
            jsonArray.put(beaconToJSONObject(beacon));
        }
        return jsonArray;
    }

    /**
     * Converts passed beacon(@com.estimote.sdk.Beacon) to @JSONObject.
     * @param beacon beacon(@com.estimote.sdk.Beacon)
     * @return JSONObject
     * @throws JSONException
     */
    private JSONObject beaconToJSONObject(Beacon beacon) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("proximityUUID", beacon.getProximityUUID());
        jsonObject.put("major", beacon.getMajor());
        jsonObject.put("minor", beacon.getMinor());
        jsonObject.put("rssi", beacon.getRssi());
        jsonObject.put("macAddress", beacon.getMacAddress());
        jsonObject.put("measuredPower", beacon.getMeasuredPower());
        jsonObject.put("dist", util.computeAccuracy(beacon));
        return jsonObject;
    }
}
