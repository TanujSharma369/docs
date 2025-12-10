/*
 *   Copyright (c) 2023 Project CHIP Authors
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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.R;
import com.matter.casting.core.CastingPlayer;
import com.matter.casting.core.CastingPlayerDiscovery;
import com.matter.casting.core.MatterCastingPlayerDiscovery;
import com.matter.casting.support.MatterError;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiscoveryExampleFragment extends Fragment {
  private static final String TAG = DiscoveryExampleFragment.class.getSimpleName();
  // 35 represents device type of Matter Casting Player
  private static final Long DISCOVERY_TARGET_DEVICE_TYPE = 35L;
  private static final int DISCOVERY_RUNTIME_SEC = 15;
  private static final List<CastingPlayer> castingPlayerList = new ArrayList<>();
  private static ArrayAdapter<CastingPlayer> arrayAdapter;
  
  // Handler for periodic connection status updates
  private android.os.Handler connectionStatusHandler;
  private Runnable connectionStatusUpdateRunnable;

  // Get a singleton instance of the MatterCastingPlayerDiscovery
  private static final CastingPlayerDiscovery matterCastingPlayerDiscovery =
      MatterCastingPlayerDiscovery.getInstance();

  /**
   * Implementation of a CastingPlayerChangeListener used to listen to changes in the discovered
   * CastingPlayers
   */
  private static final CastingPlayerDiscovery.CastingPlayerChangeListener
      castingPlayerChangeListener =
          new CastingPlayerDiscovery.CastingPlayerChangeListener() {
            private final String TAG =
                CastingPlayerDiscovery.CastingPlayerChangeListener.class.getSimpleName();

            @Override
            public void onAdded(CastingPlayer castingPlayer) {
              Log.i(
                  TAG,
                  "DiscoveryExampleFragment onAdded() Discovered CastingPlayer deviceId: "
                      + castingPlayer.getDeviceId());
              // Display CastingPlayer info on the screen
              new Handler(Looper.getMainLooper())
                  .post(
                      () -> {
                        arrayAdapter.add(castingPlayer);
                      });
            }

            @Override
            public void onChanged(CastingPlayer castingPlayer) {
              Log.i(
                  TAG,
                  "DiscoveryExampleFragment onChanged() Discovered changes to CastingPlayer with deviceId: "
                      + castingPlayer.getDeviceId());
              // Update the CastingPlayer on the screen
              new Handler(Looper.getMainLooper())
                  .post(
                      () -> {
                        final Optional<CastingPlayer> playerInList =
                            castingPlayerList
                                .stream()
                                .filter(node -> castingPlayer.equals(node))
                                .findFirst();
                        if (playerInList.isPresent()) {
                          Log.d(
                              TAG,
                              "onChanged() Updating existing CastingPlayer entry "
                                  + playerInList.get().getDeviceId()
                                  + " in castingPlayerList list");
                          arrayAdapter.remove(playerInList.get());
                        }
                        arrayAdapter.add(castingPlayer);
                      });
            }

            @Override
            public void onRemoved(CastingPlayer castingPlayer) {
              Log.i(
                  TAG,
                  "DiscoveryExampleFragment onRemoved() Removed CastingPlayer with deviceId: "
                      + castingPlayer.getDeviceId());
              // Remove CastingPlayer from the screen
              new Handler(Looper.getMainLooper())
                  .post(
                      () -> {
                        final Optional<CastingPlayer> playerInList =
                            castingPlayerList
                                .stream()
                                .filter(node -> castingPlayer.equals(node))
                                .findFirst();
                        if (playerInList.isPresent()) {
                          Log.d(
                              TAG,
                              "onRemoved() Removing existing CastingPlayer entry "
                                  + playerInList.get().getDeviceId()
                                  + " in castingPlayerList list");
                          arrayAdapter.remove(playerInList.get());
                        }
                      });
            }
          };

  public static DiscoveryExampleFragment newInstance() {
    Log.i(TAG, "newInstance() called");
    return new DiscoveryExampleFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.i(TAG, "onCreate() called");
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.i(TAG, "onCreateView() called");
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_matter_discovery_example, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.i(TAG, "onViewCreated() called");

    Log.d(TAG, "onViewCreated() creating callbacks");

    // Update connection status indicator
    updateConnectionStatus();
    
    // Setup manual commissioning section
    setupManualCommissioningSection();
    
    // Setup navigation buttons
    setupNavigationButtons();
  }

  @Override
  public void onResume() {
    Log.i(TAG, "onResume() called");
    super.onResume();
    // Update connection status when returning to this screen
    updateConnectionStatus();
    // Start periodic connection status monitoring
    startConnectionStatusMonitoring();
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i(TAG, "DiscoveryExampleFragment onPause() called");
    // Stop periodic connection status monitoring
    stopConnectionStatusMonitoring();
  }

  /** Interface for notifying the host. */
  public interface Callback {
    /** Notifies listener of Connection Button click. */
    void handleConnectionButtonClicked(
        CastingPlayer castingPlayer, boolean useCommissionerGeneratedPasscode);
  }

  /**
   * Updates the connection status indicator at the top of the screen
   */
  private void updateConnectionStatus() {
    if (getView() == null) return;
    
    TextView connectionStatusIndicator = getView().findViewById(R.id.connectionStatusIndicator);
    if (connectionStatusIndicator == null) return;
    
    new Thread(() -> {
      boolean isConnected = ManualCommissioningHelper.hasCommissionedVideoPlayer();
      String deviceInfo = isConnected ? ManualCommissioningHelper.getCommissionedVideoPlayerInfo() : "";
      
      if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
          if (isConnected) {
            connectionStatusIndicator.setText("🟢 Connected: " + deviceInfo);
            connectionStatusIndicator.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            connectionStatusIndicator.setTextColor(getResources().getColor(android.R.color.black));
          } else {
            connectionStatusIndicator.setText("⚪ Not Connected");
            connectionStatusIndicator.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            connectionStatusIndicator.setTextColor(getResources().getColor(android.R.color.white));
          }
        });
      }
    }).start();
  }

  /**
   * Starts periodic monitoring of connection status (checks every 2 seconds)
   */
  private void startConnectionStatusMonitoring() {
    if (connectionStatusHandler == null) {
      connectionStatusHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }
    
    connectionStatusUpdateRunnable = new Runnable() {
      @Override
      public void run() {
        updateConnectionStatus();
        // Schedule next update in 2 seconds
        if (connectionStatusHandler != null) {
          connectionStatusHandler.postDelayed(this, 2000);
        }
      }
    };
    
    // Start the periodic updates
    connectionStatusHandler.post(connectionStatusUpdateRunnable);
    Log.i(TAG, "Started connection status monitoring");
  }

  /**
   * Stops periodic monitoring of connection status
   */
  private void stopConnectionStatusMonitoring() {
    if (connectionStatusHandler != null && connectionStatusUpdateRunnable != null) {
      connectionStatusHandler.removeCallbacks(connectionStatusUpdateRunnable);
      Log.i(TAG, "Stopped connection status monitoring");
    }
  }

  /**
   * Sets up the manual commissioning section with QR code, commissioning info,
   * and button to open commissioning window
   * AUTO-STARTS commissioning window for 10 minutes on launch
   */
  private void setupManualCommissioningSection() {
    TextView commissioningInfoTextView = getView().findViewById(R.id.commissioningInfoTextView);
    TextView commissioningStatusTextView = getView().findViewById(R.id.commissioningStatusTextView);
    Button openCommissioningWindowButton = getView().findViewById(R.id.openCommissioningWindowButton);

    // Display commissioning parameters
    long setupPasscode = InitializationExample.commissionableDataProvider.get().getSetupPasscode();
    int discriminator = InitializationExample.commissionableDataProvider.get().getDiscriminator();
    
    String infoText = "Passcode: " + setupPasscode + " | Discriminator: " + discriminator;
    commissioningInfoTextView.setText(infoText);

    // AUTO-START: Open commissioning window for 10 minutes immediately
    autoStartCommissioningWindow(commissioningStatusTextView, openCommissioningWindowButton);

    // Setup button click listener for manual re-open
    openCommissioningWindowButton.setOnClickListener(v -> {
      Log.i(TAG, "Manually opening commissioning window");
      commissioningStatusTextView.setText("Opening commissioning window...");
      openCommissioningWindowButton.setEnabled(false);

      new Thread(() -> {
        // Open for 10 minutes (600 seconds)
        MatterError err = ManualCommissioningHelper.openBasicCommissioningWindowWithTimeout(600);
        
        getActivity().runOnUiThread(() -> {
          if (err.hasNoError()) {
            commissioningStatusTextView.setText(
              "✓ Commissioning window opened!\n" +
              "App is now discoverable for 10 minutes.\n"
            );
            Log.i(TAG, "Successfully opened commissioning window for 10 minutes");
            
            // Show toast notification
            android.widget.Toast.makeText(
              getContext(),
              "✓ Commissioning Window Opened (10 min)\nWaiting for device...",
              android.widget.Toast.LENGTH_LONG
            ).show();
            
            // Start monitoring for commissioned connection
            startMonitoringForCommissionedPlayer();
          } else {
            commissioningStatusTextView.setText(
              "✗ Failed to open commissioning window: " + err.getErrorMessage()
            );
            openCommissioningWindowButton.setEnabled(true);
            Log.e(TAG, "Failed to open commissioning window: " + err);
          }
        });
      }).start();
    });
  }

  /**
   * AUTO-START: Automatically opens commissioning window for 10 minutes on app launch
   */
  private void autoStartCommissioningWindow(TextView statusTextView, Button button) {
    Log.i(TAG, "AUTO-START: Opening commissioning window for 10 minutes...");
    statusTextView.setText("🔄 Auto-starting commissioning window (10 min)...");
    button.setEnabled(false);

    new Thread(() -> {
      // Open for 10 minutes (600 seconds)
      MatterError err = ManualCommissioningHelper.openBasicCommissioningWindowWithTimeout(600);
      
      if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
          if (err.hasNoError()) {
            statusTextView.setText(
              "✓ AUTO-STARTED: Commissioning window open!\n" +
              "Ready for pairing (10 minutes)\n" +
              "Use Matter controller to commission this device."
            );
            Log.i(TAG, "AUTO-START: Successfully opened commissioning window for 10 minutes");
            
            // Show prominent toast
            android.widget.Toast.makeText(
              getContext(),
              "✓ Ready for Pairing (10 min window)",
              android.widget.Toast.LENGTH_LONG
            ).show();
            
            // Start monitoring for commissioned connection
            startMonitoringForCommissionedPlayer();
            
            // Re-enable button after a delay (allow manual restart)
            new android.os.Handler().postDelayed(() -> {
              button.setEnabled(true);
              button.setText("Restart Commissioning Window");
            }, 5000); // Enable after 5 seconds
            
          } else {
            statusTextView.setText(
              "✗ AUTO-START FAILED: " + err.getErrorMessage() + "\n" +
              "Click button to try manually."
            );
            button.setEnabled(true);
            Log.e(TAG, "AUTO-START: Failed to open commissioning window: " + err);
            
            android.widget.Toast.makeText(
              getContext(),
              "✗ Auto-start failed. Try manual button.",
              android.widget.Toast.LENGTH_LONG
            ).show();
          }
        });
      }
    }).start();
  }

  /**
   * Sets up the "Check for Commissioned Device" button to detect if this app
   * was commissioned externally and navigate to command interface
   */
  private void setupNavigationButtons() {
    Button virtualRemoteButton = getView().findViewById(R.id.virtualRemoteButton);
    Button appLauncherButton = getView().findViewById(R.id.appLauncherButton);
    Button voiceControlButton = getView().findViewById(R.id.voiceControlButton);
    TextView commissioningStatusTextView = getView().findViewById(R.id.commissioningStatusTextView);
    
    // Virtual Remote button - navigate to RemoteControlFragment
    virtualRemoteButton.setOnClickListener(v -> {
      Log.i(TAG, "Virtual Remote button clicked");
      
      // Check if device is commissioned
      boolean hasPlayer = ManualCommissioningHelper.hasCommissionedVideoPlayer();
      if (!hasPlayer) {
        commissioningStatusTextView.setText(
          "✗ No commissioned device found.\n\n" +
          "Please open commissioning window and commission from your device first."
        );
        return;
      }
      
      // Navigate to RemoteControlFragment
      if (getActivity() != null) {
        getActivity().getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.main_fragment_container, new RemoteControlFragment())
          .addToBackStack(null)
          .commit();
      }
    });
    
    // Application Launcher button - navigate to AppLauncherFragment
    appLauncherButton.setOnClickListener(v -> {
      Log.i(TAG, "Application Launcher button clicked");
      
      // Check if device is commissioned
      boolean hasPlayer = ManualCommissioningHelper.hasCommissionedVideoPlayer();
      if (!hasPlayer) {
        commissioningStatusTextView.setText(
          "✗ No commissioned device found.\n\n" +
          "Please open commissioning window and commission from your device first."
        );
        return;
      }
      
      // Navigate to AppLauncherFragment
      if (getActivity() != null) {
        getActivity().getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.main_fragment_container, new AppLauncherFragment())
          .addToBackStack(null)
          .commit();
      }
    });
    
    // Voice Control button - navigate to VoiceControlFragment
    voiceControlButton.setOnClickListener(v -> {
      Log.i(TAG, "Voice Control button clicked");
      
      // Check if device is commissioned
      boolean hasPlayer = ManualCommissioningHelper.hasCommissionedVideoPlayer();
      if (!hasPlayer) {
        commissioningStatusTextView.setText(
          "✗ No commissioned device found.\n\n" +
          "Please open commissioning window and commission from your device first."
        );
        return;
      }
      
      // Navigate to VoiceControlFragment
      if (getActivity() != null) {
        getActivity().getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.main_fragment_container, new VoiceControlFragment())
          .addToBackStack(null)
          .commit();
      }
    });
  }

  /**
   * Monitors for externally commissioned CastingPlayers and automatically navigates
   * to the ActionSelector when one is found
   */
  private void startMonitoringForCommissionedPlayer() {
    Log.i(TAG, "Starting to monitor for commissioned CastingPlayer");
    
    // Monitor for newly commissioned players in the castingPlayerList
    final int[] previousSize = {castingPlayerList.size()};
    
    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    Runnable checkForNewPlayer = new Runnable() {
      @Override
      public void run() {
        // Check if a new player was added and is connected
        if (castingPlayerList.size() > previousSize[0]) {
          CastingPlayer newPlayer = castingPlayerList.get(castingPlayerList.size() - 1);
          
          if (newPlayer.getConnectionState() == CastingPlayer.ConnectionState.CONNECTED) {
            Log.i(TAG, "Detected newly commissioned CastingPlayer: " + newPlayer.getDeviceName());
            
            // Update connection status indicator
            updateConnectionStatus();
            
            // Show success toast
            if (getActivity() != null) {
              getActivity().runOnUiThread(() -> {
                android.widget.Toast.makeText(
                  getContext(),
                  "✓ Connection Successful!\nConnected to: " + newPlayer.getDeviceName(),
                  android.widget.Toast.LENGTH_LONG
                ).show();
              });
            }
            
            TextView commissioningStatusTextView = getView().findViewById(R.id.commissioningStatusTextView);
            commissioningStatusTextView.setText(
              "✓ Successfully commissioned!\n" +
              "Connected to: " + newPlayer.getDeviceName() + "\n" +
              "You can now use Virtual Remote or Application Launcher."
            );
            
            // Navigate to ActionSelector after a brief delay
            handler.postDelayed(() -> {
              Callback callback = (Callback) getActivity();
              if (callback != null) {
                callback.handleConnectionButtonClicked(newPlayer, false);
              }
            }, 1500); // 1.5 second delay to show the message
            
            return; // Stop monitoring
          }
        }
        
        previousSize[0] = castingPlayerList.size();
        
        // Check again in 2 seconds
        handler.postDelayed(this, 2000);
      }
    };
    
    // Start checking after 2 seconds
    handler.postDelayed(checkForNewPlayer, 2000);
  }
}

class CastingPlayerArrayAdapter extends ArrayAdapter<CastingPlayer> {
  private final List<CastingPlayer> playerList;
  private final Context context;
  private LayoutInflater inflater;
  private static final String TAG = CastingPlayerArrayAdapter.class.getSimpleName();

  public CastingPlayerArrayAdapter(Context context, List<CastingPlayer> playerList) {
    super(context, 0, playerList);
    Log.i(TAG, "CastingPlayerArrayAdapter() constructor called");
    this.context = context;
    this.playerList = playerList;
    inflater = (LayoutInflater.from(context));
  }

  @Override
  public View getView(int i, View view, ViewGroup viewGroup) {
    view = inflater.inflate(R.layout.commissionable_player_list_item, null);
    String buttonText = getCastingPlayerButtonText(playerList.get(i));
    Button playerDescription = view.findViewById(R.id.commissionable_player_description);
    playerDescription.setText(buttonText);

    // OnClickListener for the CastingPLayer button, to be used for the Commissionee-Generated
    // passcode commissioning flow.
    View.OnClickListener clickListener =
        v -> {
          CastingPlayer castingPlayer = playerList.get(i);
          Log.d(
              TAG,
              "OnClickListener.onClick() called for CastingPlayer with deviceId: "
                  + castingPlayer.getDeviceId());
          DiscoveryExampleFragment.Callback onClickCallback =
              (DiscoveryExampleFragment.Callback) context;
          onClickCallback.handleConnectionButtonClicked(castingPlayer, false);
        };
    playerDescription.setOnClickListener(clickListener);

    // OnLongClickListener for the CastingPLayer button, to be used for the Commissioner-Generated
    // passcode commissioning flow.
    View.OnLongClickListener longClickListener =
        v -> {
          CastingPlayer castingPlayer = playerList.get(i);
          if (!castingPlayer.getSupportsCommissionerGeneratedPasscode()) {
            Log.e(
                TAG,
                "OnLongClickListener.onLongClick() called for CastingPlayer with deviceId "
                    + castingPlayer.getDeviceId()
                    + ". This CastingPlayer does not support Commissioner-Generated passcode commissioning.");
            // Manual commissioning mode - no error display needed
            return true;
          }
          Log.d(
              TAG,
              "OnLongClickListener.onLongClick() called for CastingPlayer with deviceId "
                  + castingPlayer.getDeviceId()
                  + ", attempting the Commissioner-Generated passcode commissioning flow.");
          DiscoveryExampleFragment.Callback onClickCallback =
              (DiscoveryExampleFragment.Callback) context;
          onClickCallback.handleConnectionButtonClicked(castingPlayer, true);
          return true;
        };
    playerDescription.setOnLongClickListener(longClickListener);
    return view;
  }

  private String getCastingPlayerButtonText(CastingPlayer player) {
    String main = player.getDeviceName() != null ? player.getDeviceName() : "";
    String aux = "" + (player.getDeviceId() != null ? "Device ID: " + player.getDeviceId() : "");
    aux +=
        player.getProductId() > 0
            ? (aux.isEmpty() ? "" : ", ") + "Product ID: " + player.getProductId()
            : "";
    aux +=
        player.getVendorId() > 0
            ? (aux.isEmpty() ? "" : ", ") + "Vendor ID: " + player.getVendorId()
            : "";
    aux +=
        player.getDeviceType() > 0
            ? (aux.isEmpty() ? "" : ", ") + "Device Type: " + player.getDeviceType()
            : "";
    aux += (aux.isEmpty() ? "" : ", ") + "Resolved IP?: " + (player.getIpAddresses().size() > 0);
    aux +=
        (aux.isEmpty() ? "" : ", ")
            + "Supports Commissioner-Generated Passcode: "
            + (player.getSupportsCommissionerGeneratedPasscode());

    aux = aux.isEmpty() ? aux : "\n" + aux;
    return main + aux;
  }
}
