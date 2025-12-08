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
import com.matter.casting.core.CastingPlayer;
import com.matter.casting.core.MatterCastingPlayerDiscovery;
import java.util.List;

/**
 * Monitors for external commissioning completion (when STB commissions this Android app)
 * and provides callbacks when a commissioned CastingPlayer is detected.
 */
public class ManualCommissioningMonitor {
    private static final String TAG = ManualCommissioningMonitor.class.getSimpleName();
    private static ManualCommissioningMonitor instance;
    private CommissioningCompleteListener listener;
    private Thread monitorThread;
    private volatile boolean monitoring = false;

    public interface CommissioningCompleteListener {
        void onCommissioningComplete(CastingPlayer castingPlayer);
    }

    private ManualCommissioningMonitor() {}

    public static synchronized ManualCommissioningMonitor getInstance() {
        if (instance == null) {
            instance = new ManualCommissioningMonitor();
        }
        return instance;
    }

    /**
     * Start monitoring for external commissioning completion
     * @param listener Callback to invoke when commissioning completes
     */
    public void startMonitoring(CommissioningCompleteListener listener) {
        Log.i(TAG, "startMonitoring() called");
        this.listener = listener;
        
        if (monitoring) {
            Log.w(TAG, "Already monitoring");
            return;
        }

        monitoring = true;
        monitorThread = new Thread(() -> {
            Log.i(TAG, "Monitor thread started");
            
            while (monitoring) {
                try {
                    // Check for commissioned CastingPlayers every 2 seconds
                    Thread.sleep(2000);
                    
                    // Get the list of connected/commissioned CastingPlayers
                    List<CastingPlayer> connectedPlayers = getConnectedCastingPlayers();
                    
                    if (connectedPlayers != null && !connectedPlayers.isEmpty()) {
                        Log.i(TAG, "Found " + connectedPlayers.size() + " connected CastingPlayer(s)");
                        
                        // Take the first connected player
                        CastingPlayer player = connectedPlayers.get(0);
                        
                        if (player.getConnectionState() == CastingPlayer.ConnectionState.CASTING_PLAYER_CONNECTED) {
                            Log.i(TAG, "Commissioning complete! CastingPlayer: " + player.getDeviceName());
                            
                            // Stop monitoring
                            monitoring = false;
                            
                            // Notify listener
                            if (listener != null) {
                                listener.onCommissioningComplete(player);
                            }
                            break;
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Log.e(TAG, "Monitor thread interrupted", e);
                    monitoring = false;
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in monitor thread", e);
                }
            }
            
            Log.i(TAG, "Monitor thread stopped");
        });
        
        monitorThread.start();
    }

    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        Log.i(TAG, "stopMonitoring() called");
        monitoring = false;
        
        if (monitorThread != null) {
            monitorThread.interrupt();
            monitorThread = null;
        }
        
        listener = null;
    }

    /**
     * Get list of connected CastingPlayers using native method
     * This calls into the C++ layer to retrieve commissioned players
     */
    private native List<CastingPlayer> getConnectedCastingPlayers();
}
