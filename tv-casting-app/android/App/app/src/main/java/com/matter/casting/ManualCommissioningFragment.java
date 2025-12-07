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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.R;
import com.matter.casting.support.MatterError;

/**
 * ManualCommissioningFragment provides a UI for manual commissioning mode.
 * 
 * In manual commissioning mode:
 * 1. The app opens a commissioning window
 * 2. The app advertises itself as a commissionable Matter node
 * 3. Your custom commissioner can discover and commission the app directly
 * 
 * This bypasses the User Directed Commissioning (UDC) flow where the app 
 * discovers and selects a commissioner first.
 */
public class ManualCommissioningFragment extends Fragment {
    private static final String TAG = ManualCommissioningFragment.class.getSimpleName();
    
    private TextView statusTextView;
    private TextView commissioningInfoTextView;
    private Button openWindowButton;
    private Button closeWindowButton;
    private Button showPayloadButton;
    
    public static ManualCommissioningFragment newInstance() {
        Log.i(TAG, "newInstance() called");
        return new ManualCommissioningFragment();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate() called");
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView() called");
        return inflater.inflate(R.layout.fragment_manual_commissioning, container, false);
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onViewCreated() called");
        
        statusTextView = view.findViewById(R.id.manualCommissioningStatusText);
        commissioningInfoTextView = view.findViewById(R.id.manualCommissioningInfoText);
        openWindowButton = view.findViewById(R.id.openCommissioningWindowButton);
        closeWindowButton = view.findViewById(R.id.closeCommissioningWindowButton);
        showPayloadButton = view.findViewById(R.id.showOnboardingPayloadButton);
        
        // Display commissioning parameters
        updateCommissioningInfo();
        
        // Set up button handlers
        openWindowButton.setOnClickListener(v -> openCommissioningWindow());
        closeWindowButton.setOnClickListener(v -> closeCommissioningWindow());
        showPayloadButton.setOnClickListener(v -> showOnboardingPayload());
        
        // Check if window is already open
        updateWindowStatus();
    }
    
    private void updateCommissioningInfo() {
        long setupPasscode = InitializationExample.commissionableDataProvider.get().getSetupPasscode();
        int discriminator = InitializationExample.commissionableDataProvider.get().getDiscriminator();
        
        String info = "Manual Commissioning Parameters:\n\n" +
                     "Setup Passcode: " + setupPasscode + "\n" +
                     "Discriminator: " + discriminator + "\n\n" +
                     "Your commissioner should be able to discover this app over mDNS " +
                     "using service type _matterc._udp after opening the commissioning window.";
        
        commissioningInfoTextView.setText(info);
    }
    
    private void openCommissioningWindow() {
        Log.i(TAG, "openCommissioningWindow() Opening basic commissioning window");
        statusTextView.setText("Opening commissioning window...");
        
        // Open commissioning window on a background thread
        new Thread(() -> {
            MatterError err = ManualCommissioningHelper.openBasicCommissioningWindow();
            
            getActivity().runOnUiThread(() -> {
                if (err.hasNoError()) {
                    statusTextView.setText(
                        "✓ Commissioning window opened!\n\n" +
                        "The app is now discoverable by Matter commissioners on your network.\n\n" +
                        "Use your custom commissioner to discover and commission this device.\n\n" +
                        "The window will remain open for 3 minutes (180 seconds)."
                    );
                    openWindowButton.setEnabled(false);
                    closeWindowButton.setEnabled(true);
                    
                    Log.i(TAG, "Successfully opened commissioning window");
                } else {
                    statusTextView.setText(
                        "✗ Failed to open commissioning window\n\n" +
                        "Error: " + err.getMessage()
                    );
                    Log.e(TAG, "Failed to open commissioning window: " + err);
                }
            });
        }).start();
    }
    
    private void closeCommissioningWindow() {
        Log.i(TAG, "closeCommissioningWindow() Closing commissioning window");
        statusTextView.setText("Closing commissioning window...");
        
        new Thread(() -> {
            MatterError err = ManualCommissioningHelper.closeCommissioningWindow();
            
            getActivity().runOnUiThread(() -> {
                if (err.hasNoError()) {
                    statusTextView.setText("✓ Commissioning window closed");
                    openWindowButton.setEnabled(true);
                    closeWindowButton.setEnabled(false);
                    
                    Log.i(TAG, "Successfully closed commissioning window");
                } else {
                    statusTextView.setText(
                        "✗ Failed to close commissioning window\n\n" +
                        "Error: " + err.getMessage()
                    );
                    Log.e(TAG, "Failed to close commissioning window: " + err);
                }
            });
        }).start();
    }
    
    private void showOnboardingPayload() {
        Log.i(TAG, "showOnboardingPayload() Logging onboarding payload to logcat");
        
        new Thread(() -> {
            ManualCommissioningHelper.logOnboardingPayload();
            
            getActivity().runOnUiThread(() -> {
                statusTextView.setText(
                    "✓ Onboarding payload logged to logcat\n\n" +
                    "Check logcat for QR code data and manual pairing code.\n" +
                    "Filter by tag: CHIP or ConfigurationManager"
                );
            });
        }).start();
    }
    
    private void updateWindowStatus() {
        new Thread(() -> {
            boolean isOpen = ManualCommissioningHelper.isCommissioningWindowOpen();
            
            getActivity().runOnUiThread(() -> {
                if (isOpen) {
                    statusTextView.setText("ℹ A commissioning window is currently open");
                    openWindowButton.setEnabled(false);
                    closeWindowButton.setEnabled(true);
                } else {
                    statusTextView.setText("ℹ No commissioning window is currently open\n\n" +
                                          "Click 'Open Commissioning Window' to make this app discoverable.");
                    openWindowButton.setEnabled(true);
                    closeWindowButton.setEnabled(false);
                }
            });
        }).start();
    }
}
