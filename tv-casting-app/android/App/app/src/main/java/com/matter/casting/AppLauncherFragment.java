package com.matter.casting;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.R;

/**
 * Application Launcher Fragment - Provides interface to launch and stop applications
 * on a commissioned STB/TV device using Matter ApplicationLauncher cluster.
 */
public class AppLauncherFragment extends Fragment {
  private static final String TAG = AppLauncherFragment.class.getSimpleName();
  
  private TextView deviceStatusText;
  private EditText catalogVendorIdInput;
  private EditText applicationIdInput;
  private Button launchAppButton;
  private Button stopAppButton;
  private TextView statusText;
  
  static {
    System.loadLibrary("TvCastingApp");
  }
  
  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_app_launcher, container, false);
  }
  
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    deviceStatusText = view.findViewById(R.id.appLauncherDeviceStatusText);
    catalogVendorIdInput = view.findViewById(R.id.catalogVendorIdInput);
    applicationIdInput = view.findViewById(R.id.applicationIdInput);
    launchAppButton = view.findViewById(R.id.launchAppButton);
    stopAppButton = view.findViewById(R.id.stopAppButton);
    statusText = view.findViewById(R.id.appLauncherStatusText);
    
    checkDeviceConnection();
    setupButtons();
  }
  
  private void checkDeviceConnection() {
    if (ManualCommissioningHelper.hasCommissionedVideoPlayer()) {
      String deviceInfo = ManualCommissioningHelper.getCommissionedVideoPlayerInfo();
      deviceStatusText.setText("Connected to: " + deviceInfo);
      deviceStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    } else {
      deviceStatusText.setText("No commissioned device found. Please commission a device first.");
      deviceStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
      launchAppButton.setEnabled(false);
      stopAppButton.setEnabled(false);
    }
  }
  
  private void setupButtons() {
    // Set default values
    catalogVendorIdInput.setText("0");
    applicationIdInput.setText("YouTube");
    
    launchAppButton.setOnClickListener(v -> {
      String catalogVendorIdStr = catalogVendorIdInput.getText().toString().trim();
      String applicationId = applicationIdInput.getText().toString().trim();
      
      if (applicationId.isEmpty()) {
        Toast.makeText(getContext(), "Please enter Application ID", Toast.LENGTH_SHORT).show();
        return;
      }
      
      int catalogVendorId = 0;
      try {
        catalogVendorId = Integer.parseInt(catalogVendorIdStr);
      } catch (NumberFormatException e) {
        Toast.makeText(getContext(), "Invalid Catalog Vendor ID", Toast.LENGTH_SHORT).show();
        return;
      }
      
      launchApplication(catalogVendorId, applicationId);
    });
    
    stopAppButton.setOnClickListener(v -> {
      String catalogVendorIdStr = catalogVendorIdInput.getText().toString().trim();
      String applicationId = applicationIdInput.getText().toString().trim();
      
      if (applicationId.isEmpty()) {
        Toast.makeText(getContext(), "Please enter Application ID", Toast.LENGTH_SHORT).show();
        return;
      }
      
      int catalogVendorId = 0;
      try {
        catalogVendorId = Integer.parseInt(catalogVendorIdStr);
      } catch (NumberFormatException e) {
        Toast.makeText(getContext(), "Invalid Catalog Vendor ID", Toast.LENGTH_SHORT).show();
        return;
      }
      
      stopApplication(catalogVendorId, applicationId);
    });
  }
  
  private void launchApplication(int catalogVendorId, String applicationId) {
    Log.d(TAG, "Launching application: catalogVendorId=" + catalogVendorId + ", appId=" + applicationId);
    statusText.setText("Launching " + applicationId + "...");
    
    new Thread(() -> {
      boolean success = launchApp(catalogVendorId, applicationId);
      
      getActivity().runOnUiThread(() -> {
        if (success) {
          statusText.setText("✓ Launch command sent for: " + applicationId);
          statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
          Toast.makeText(getContext(), "Application launch command sent", Toast.LENGTH_SHORT).show();
        } else {
          statusText.setText("✗ Failed to launch: " + applicationId);
          statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
          Toast.makeText(getContext(), "Failed to send launch command", Toast.LENGTH_LONG).show();
        }
      });
    }).start();
  }
  
  private void stopApplication(int catalogVendorId, String applicationId) {
    Log.d(TAG, "Stopping application: catalogVendorId=" + catalogVendorId + ", appId=" + applicationId);
    statusText.setText("Stopping " + applicationId + "...");
    
    new Thread(() -> {
      boolean success = stopApp(catalogVendorId, applicationId);
      
      getActivity().runOnUiThread(() -> {
        if (success) {
          statusText.setText("✓ Stop command sent for: " + applicationId);
          statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
          Toast.makeText(getContext(), "Application stop command sent", Toast.LENGTH_SHORT).show();
        } else {
          statusText.setText("✗ Failed to stop: " + applicationId);
          statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
          Toast.makeText(getContext(), "Failed to send stop command", Toast.LENGTH_LONG).show();
        }
      });
    }).start();
  }
  
  /**
   * Native method to launch an application via ApplicationLauncher cluster
   * @param catalogVendorId Catalog Vendor ID
   * @param applicationId Application ID string
   * @return true if command was sent successfully
   */
  private native boolean launchApp(int catalogVendorId, String applicationId);
  
  /**
   * Native method to stop an application via ApplicationLauncher cluster
   * @param catalogVendorId Catalog Vendor ID
   * @param applicationId Application ID string
   * @return true if command was sent successfully
   */
  private native boolean stopApp(int catalogVendorId, String applicationId);
}
