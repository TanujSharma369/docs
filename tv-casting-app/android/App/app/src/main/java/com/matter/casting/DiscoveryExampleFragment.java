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
  private TextView matterDiscoveryMessageTextView;
  public static TextView matterDiscoveryErrorMessageTextView;
  private static final List<CastingPlayer> castingPlayerList = new ArrayList<>();
  private static ArrayAdapter<CastingPlayer> arrayAdapter;

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

    matterDiscoveryMessageTextView =
        getActivity().findViewById(R.id.matterDiscoveryMessageTextView);
    matterDiscoveryMessageTextView.setText(
        getString(R.string.matter_discovery_message_initializing_text));

    matterDiscoveryErrorMessageTextView =
        getActivity().findViewById(R.id.matterDiscoveryErrorTextView);
    matterDiscoveryErrorMessageTextView.setText(
        getString(R.string.matter_discovery_error_message_initial));

    arrayAdapter = new CastingPlayerArrayAdapter(getActivity(), castingPlayerList);
    final ListView list = getActivity().findViewById(R.id.castingPlayerList);
    list.setAdapter(arrayAdapter);

    Log.d(TAG, "onViewCreated() creating callbacks");

    // Setup manual commissioning section
    setupManualCommissioningSection();
    
    // Setup check commissioned device button
    setupCheckCommissionedDeviceButton();

    Button startDiscoveryButton = getView().findViewById(R.id.startDiscoveryButton);
    startDiscoveryButton.setOnClickListener(
        v -> {
          Log.i(
              TAG, "onViewCreated() startDiscoveryButton button clicked. Calling startDiscovery()");
          if (!startDiscovery()) {
            Log.e(TAG, "onViewCreated() startDiscovery() call Failed");
          }
        });

    Button stopDiscoveryButton = getView().findViewById(R.id.stopDiscoveryButton);
    stopDiscoveryButton.setOnClickListener(
        v -> {
          Log.i(TAG, "onViewCreated() stopDiscoveryButton button clicked. Calling stopDiscovery()");
          stopDiscovery();
        });

    Button clearDiscoveryResultsButton = getView().findViewById(R.id.clearDiscoveryResultsButton);
    clearDiscoveryResultsButton.setOnClickListener(
        v -> {
          Log.i(
              TAG, "onViewCreated() clearDiscoveryResultsButton button clicked. Clearing results");
          arrayAdapter.clear();
          matterDiscoveryErrorMessageTextView.setText(
              getString(R.string.matter_discovery_error_message_initial));
        });
  }

  @Override
  public void onResume() {
    Log.i(TAG, "onResume() called");
    super.onResume();
    MatterError err =
        matterCastingPlayerDiscovery.removeCastingPlayerChangeListener(castingPlayerChangeListener);
    if (err.hasError()) {
      Log.e(TAG, "onResume() removeCastingPlayerChangeListener() err: " + err);
    }
    if (!startDiscovery()) {
      Log.e(TAG, "onResume() Warning: startDiscovery() call Failed");
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i(TAG, "DiscoveryExampleFragment onPause() called, calling stopDiscovery()");
    // Stop discovery when leaving the fragment, for example, while displaying the
    // ConnectionExampleFragment.
    stopDiscovery();
  }

  /** Interface for notifying the host. */
  public interface Callback {
    /** Notifies listener of Connection Button click. */
    void handleConnectionButtonClicked(
        CastingPlayer castingPlayer, boolean useCommissionerGeneratedPasscode);
  }

  private boolean startDiscovery() {
    Log.i(TAG, "startDiscovery() called");
    matterDiscoveryErrorMessageTextView.setText(
        getString(R.string.matter_discovery_error_message_initial));

    arrayAdapter.clear();

    // Add the implemented CastingPlayerChangeListener to listen to changes in the discovered
    // CastingPlayers
    MatterError err =
        matterCastingPlayerDiscovery.addCastingPlayerChangeListener(castingPlayerChangeListener);
    if (err.hasError()) {
      Log.e(TAG, "startDiscovery() addCastingPlayerChangeListener() called, err Add: " + err);
      matterDiscoveryErrorMessageTextView.setText(
          getString(R.string.matter_discovery_error_message_stop_before_starting) + err);
      return false;
    }
    // Start discovery
    Log.i(TAG, "startDiscovery() calling CastingPlayerDiscovery.startDiscovery()");
    err = matterCastingPlayerDiscovery.startDiscovery(DISCOVERY_TARGET_DEVICE_TYPE);
    if (err.hasError()) {
      Log.e(TAG, "startDiscovery() startDiscovery() called, err Start: " + err);
      matterDiscoveryErrorMessageTextView.setText(
          getString(R.string.matter_discovery_error_message_start_error) + err);
      return false;
    }

    Log.i(TAG, "startDiscovery() started discovery");

    matterDiscoveryMessageTextView.setText(
        getString(R.string.matter_discovery_message_discovering_text));

    return true;
  }

  private void stopDiscovery() {
    Log.i(TAG, "DiscoveryExampleFragment stopDiscovery() called");
    matterDiscoveryErrorMessageTextView.setText(
        getString(R.string.matter_discovery_error_message_initial));

    // Stop discovery
    MatterError err = matterCastingPlayerDiscovery.stopDiscovery();
    if (err.hasError()) {
      Log.e(
          TAG,
          "stopDiscovery() MatterCastingPlayerDiscovery.stopDiscovery() called, err Stop: " + err);
      matterDiscoveryErrorMessageTextView.setText(
          getString(R.string.matter_discovery_error_message_stop_error) + err);
    } else {
      Log.d(TAG, "stopDiscovery() MatterCastingPlayerDiscovery.stopDiscovery() success");
    }

    matterDiscoveryMessageTextView.setText(
        getString(R.string.matter_discovery_message_stopped_text));
    Log.d(
        TAG,
        "stopDiscovery() text set to: "
            + getString(R.string.matter_discovery_message_stopped_text));

    // Remove the CastingPlayerChangeListener
    Log.i(TAG, "stopDiscovery() removing CastingPlayerChangeListener");
    err =
        matterCastingPlayerDiscovery.removeCastingPlayerChangeListener(castingPlayerChangeListener);
    if (err.hasError()) {
      Log.e(
          TAG,
          "stopDiscovery() matterCastingPlayerDiscovery.removeCastingPlayerChangeListener() called, err Remove: "
              + err);
      matterDiscoveryErrorMessageTextView.setText(
          getString(R.string.matter_discovery_error_message_stop_error) + err);
    }
  }

  /**
   * Sets up the manual commissioning section with QR code, commissioning info,
   * and button to open commissioning window
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

    // Setup button click listener
    openCommissioningWindowButton.setOnClickListener(v -> {
      Log.i(TAG, "Opening commissioning window for manual commissioning");
      commissioningStatusTextView.setText("Opening commissioning window...");
      openCommissioningWindowButton.setEnabled(false);

      new Thread(() -> {
        MatterError err = ManualCommissioningHelper.openBasicCommissioningWindow();
        
        getActivity().runOnUiThread(() -> {
          if (err.hasNoError()) {
            commissioningStatusTextView.setText(
              "✓ Commissioning window opened!\n" +
              "App is now discoverable for 3 minutes.\n" +
              "Your commissioner can discover and commission this app.\n\n" +
              "Once commissioned, your STB can send commands to this app.\n" +
              "The app will detect the connection automatically."
            );
            Log.i(TAG, "Successfully opened commissioning window");
            
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

    // Check if window is already open
    new Thread(() -> {
      boolean isOpen = ManualCommissioningHelper.isCommissioningWindowOpen();
      getActivity().runOnUiThread(() -> {
        if (isOpen) {
          commissioningStatusTextView.setText("ℹ Commissioning window is already open");
          openCommissioningWindowButton.setEnabled(false);
          // Also start monitoring since window is open
          startMonitoringForCommissionedPlayer();
        }
      });
    }).start();
  }

  /**
   * Sets up the "Check for Commissioned Device" button to detect if this app
   * was commissioned externally (e.g., by an STB) and navigate to command interface
   */
  private void setupCheckCommissionedDeviceButton() {
    Button checkCommissionedDeviceButton = getView().findViewById(R.id.checkCommissionedDeviceButton);
    TextView commissioningStatusTextView = getView().findViewById(R.id.commissioningStatusTextView);
    
    checkCommissionedDeviceButton.setOnClickListener(v -> {
      Log.i(TAG, "Checking for commissioned devices in fabric table");
      commissioningStatusTextView.setText("Checking fabric table for commissioned devices...");
      
      new Thread(() -> {
        // Check fabric table directly for commissioned devices
        boolean hasCommissioned = CommissionedDeviceHelper.hasCommissionedDevice();
        String[] deviceInfo = CommissionedDeviceHelper.getCommissionedDeviceInfo();
        
        getActivity().runOnUiThread(() -> {
          if (!hasCommissioned || deviceInfo == null || deviceInfo.length == 0) {
            commissioningStatusTextView.setText(
              "✗ No commissioned devices found in fabric table.\n\n" +
              "Make sure your STB has successfully commissioned this app.\n" +
              "Check STB logs for 'Commissioned successfully' message."
            );
            Log.i(TAG, "No commissioned devices found in fabric table");
            return;
          }
          
          // Log all commissioned devices
          Log.i(TAG, "Found " + deviceInfo.length + " commissioned device(s) in fabric table:");
          for (String info : deviceInfo) {
            Log.i(TAG, "  " + info);
          }
          
          // Parse first device info
          String firstDevice = deviceInfo[0];
          commissioningStatusTextView.setText(
            "✓ Found commissioned device in fabric table!\n" +
            deviceInfo.length + " device(s) commissioned\n" +
            "Info: " + firstDevice + "\n\n" +
            "Checking for connected CastingPlayer..."
          );
          
          // Now try to find matching CastingPlayer
          List<CastingPlayer> allPlayers = matterCastingPlayerDiscovery.getCastingPlayers();
          CastingPlayer commissionedPlayer = null;
          
          if (allPlayers != null && !allPlayers.isEmpty()) {
            for (CastingPlayer player : allPlayers) {
              Log.d(TAG, "Checking CastingPlayer: " + player.getDeviceName() + 
                    ", State: " + player.getConnectionState());
              
              if (player.getConnectionState() == CastingPlayer.ConnectionState.CONNECTED) {
                commissionedPlayer = player;
                break;
              }
            }
          }
          
          if (commissionedPlayer != null) {
            final CastingPlayer finalPlayer = commissionedPlayer;
            commissioningStatusTextView.setText(
              "✓ Found commissioned & connected device!\n" +
              "Device: " + finalPlayer.getDeviceName() + "\n" +
              "Device ID: " + finalPlayer.getDeviceId() + "\n\n" +
              "Navigating to command interface..."
            );
            
            Log.i(TAG, "Found connected CastingPlayer: " + finalPlayer.getDeviceName());
            
            // Navigate to ActionSelector after a brief delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
              Callback callback = (Callback) getActivity();
              if (callback != null) {
                callback.handleConnectionButtonClicked(finalPlayer, false);
              }
            }, 1500);
          } else {
            commissioningStatusTextView.setText(
              "⚠ Device is commissioned but not yet in CastingPlayer list.\n\n" +
              "The STB commissioned this app successfully.\n" +
              "To send commands to STB:\n" +
              "1. Click 'Start Discovery' above\n" +
              "2. Wait for STB to appear in the list\n" +
              "3. Click on the STB to connect and send commands"
            );
            Log.i(TAG, "Commissioned device found but no matching connected CastingPlayer. User needs to discover STB.");
          }
        });
      }).start();
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
            
            TextView commissioningStatusTextView = getView().findViewById(R.id.commissioningStatusTextView);
            commissioningStatusTextView.setText(
              "✓ Successfully commissioned!\n" +
              "Connected to: " + newPlayer.getDeviceName() + "\n" +
              "Navigating to command interface..."
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

            DiscoveryExampleFragment.matterDiscoveryErrorMessageTextView.setText(
                "The selected Casting Player does not support Commissioner-Generated passcode commissioning");
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
