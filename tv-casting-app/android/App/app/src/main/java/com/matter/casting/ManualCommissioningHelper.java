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
import com.matter.casting.core.CastingApp;
import com.matter.casting.support.MatterError;

/**
 * ManualCommissioningHelper provides utility methods to enable manual commissioning mode
 * for the TV casting app. This allows commissioners to discover and commission the app
 * without using User Directed Commissioning (UDC).
 * 
 * This is useful when:
 * - The commissioner doesn't support UDC
 * - You want to skip the discovery/selection flow
 * - Testing with custom commissioners that directly discover commissionable nodes
 */
public class ManualCommissioningHelper {
    private static final String TAG = ManualCommissioningHelper.class.getSimpleName();
    
    /**
     * Opens a basic commissioning window to advertise the app as a commissionable Matter node.
     * This makes the app discoverable over mDNS using the _matterc._udp service type.
     * 
     * When called, the app will:
     * 1. Open a commissioning window
     * 2. Advertise itself on the network as a commissionable node
     * 3. Be discoverable by Matter commissioners
     * 
     * The commissioning window will stay open for the duration specified (default: 3 minutes).
     * During this time, any Matter commissioner on the network can discover and commission the app.
     * 
     * @return MatterError.NO_ERROR if successful, specific error code otherwise
     */
    public static native MatterError openBasicCommissioningWindow();
    
    /**
     * Opens a basic commissioning window with a custom timeout.
     * 
     * @param timeoutSeconds How long to keep the commissioning window open (minimum 180 seconds)
     * @return MatterError.NO_ERROR if successful, specific error code otherwise
     */
    public static native MatterError openBasicCommissioningWindowWithTimeout(int timeoutSeconds);
    
    /**
     * Checks if a commissioning window is currently open
     * 
     * @return true if a commissioning window is open, false otherwise
     */
    public static native boolean isCommissioningWindowOpen();
    
    /**
     * Closes any open commissioning window
     * 
     * @return MatterError.NO_ERROR if successful, specific error code otherwise
     */
    public static native MatterError closeCommissioningWindow();
    
    /**
     * Logs the current device's onboarding payload (QR code data and manual pairing code)
     * This is useful for debugging and manual commissioning scenarios.
     */
    public static native void logOnboardingPayload();
}
