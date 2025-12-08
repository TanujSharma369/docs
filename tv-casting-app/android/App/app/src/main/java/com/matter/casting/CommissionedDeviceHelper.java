/*
 *   Copyright (c) 2024 Project CHIP Authors
 *   All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.matter.casting;

import android.util.Log;

/**
 * Helper class to check for commissioned devices in the fabric table.
 * When the STB commissions this Android app externally, the device info
 * is stored in the fabric table. This helper allows checking for such devices.
 */
public class CommissionedDeviceHelper {
    private static final String TAG = CommissionedDeviceHelper.class.getSimpleName();
    
    /**
     * Check if there are any commissioned devices in the fabric table
     * @return true if at least one commissioned device exists, false otherwise
     */
    public static native boolean hasCommissionedDevice();
    
    /**
     * Get information about commissioned devices from the fabric table
     * @return Array of strings containing fabric info, or null if none exist
     *         Format: "FabricIndex:X,NodeId:0xYYYY,FabricId:0xZZZZ"
     */
    public static native String[] getCommissionedDeviceInfo();
    
    /**
     * Log all commissioned device information
     */
    public static void logCommissionedDevices() {
        String[] devices = getCommissionedDeviceInfo();
        if (devices == null || devices.length == 0) {
            Log.i(TAG, "No commissioned devices found in fabric table");
            return;
        }
        
        Log.i(TAG, "Found " + devices.length + " commissioned device(s):");
        for (String device : devices) {
            Log.i(TAG, "  " + device);
        }
    }
}
