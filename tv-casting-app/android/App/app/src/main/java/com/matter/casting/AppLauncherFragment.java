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
 * on a commissioned device using Matter ApplicationLauncher cluster.
 */
public class AppLauncherFragment extends Fragment {
  private static final String TAG = AppLauncherFragment.class.getSimpleName();
  
  private TextView deviceStatusText;
  private Button launchYouTubeButton;
  private Button stopYouTubeButton;
  private Button launchNetflixButton;
  private Button stopNetflixButton;
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
    launchYouTubeButton = view.findViewById(R.id.launchYouTubeButton);
    stopYouTubeButton = view.findViewById(R.id.stopYouTubeButton);
    launchNetflixButton = view.findViewById(R.id.launchNetflixButton);
    stopNetflixButton = view.findViewById(R.id.stopNetflixButton);
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
      launchYouTubeButton.setEnabled(false);
      stopYouTubeButton.setEnabled(false);
      launchNetflixButton.setEnabled(false);
      stopNetflixButton.setEnabled(false);
    }
  }
  
  private void setupButtons() {
    // YouTube Launch Button
    launchYouTubeButton.setOnClickListener(v -> {
      launchApplication(0, "YouTube");
    });
    
    // YouTube Stop Button
    stopYouTubeButton.setOnClickListener(v -> {
      stopApplication(0, "YouTube");
    });
    
    // Netflix Launch Button
    launchNetflixButton.setOnClickListener(v -> {
      launchApplication(0, "NetflixApp");
    });
    
    // Netflix Stop Button
    stopNetflixButton.setOnClickListener(v -> {
      stopApplication(0, "Netflix");
    });
  }
  
  private void launchApplication(int catalogVendorId, String applicationId) {
    Log.d(TAG, "Launching application: catalogVendorId=" + catalogVendorId + ", appId=" + applicationId);
    statusText.setText("Launching " + applicationId + "...");
    
    new Thread(() -> {
      boolean success = launchApp(catalogVendorId, applicationId);
      
      getActivity().runOnUiThread(() -> {
        if (success) {
          statusText.setText(" Launch command sent for: " + applicationId);
          statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
          Toast.makeText(getContext(), "Application launch command sent", Toast.LENGTH_SHORT).show();
        } else {
          statusText.setText(" Failed to launch: " + applicationId);
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
          statusText.setText(" Stop command sent for: " + applicationId);
          statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
          Toast.makeText(getContext(), "Application stop command sent", Toast.LENGTH_SHORT).show();
        } else {
          statusText.setText(" Failed to stop: " + applicationId);
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
