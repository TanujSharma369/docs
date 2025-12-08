package com.matter.casting;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.R;
import com.matter.casting.support.CommissionedDeviceHelper;

/**
 * Remote Control Fragment - Provides a virtual TV remote interface to send KeypadInput commands
 * to a commissioned STB/TV device using Matter KeypadInput cluster.
 */
public class RemoteControlFragment extends Fragment {
  private static final String TAG = RemoteControlFragment.class.getSimpleName();
  
  private TextView deviceStatusText;
  private View remoteControlPanel;
  
  // CEC Key Codes from CECKeyCodeEnum in Matter KeypadInput cluster
  private static final int KEY_SELECT = 0;
  private static final int KEY_UP = 1;
  private static final int KEY_DOWN = 2;
  private static final int KEY_LEFT = 3;
  private static final int KEY_RIGHT = 4;
  private static final int KEY_ROOT_MENU = 9;
  private static final int KEY_SETUP_MENU = 10;
  private static final int KEY_EXIT = 13;
  private static final int KEY_NUMBER_0 = 32;
  private static final int KEY_NUMBER_1 = 33;
  private static final int KEY_NUMBER_2 = 34;
  private static final int KEY_NUMBER_3 = 35;
  private static final int KEY_NUMBER_4 = 36;
  private static final int KEY_NUMBER_5 = 37;
  private static final int KEY_NUMBER_6 = 38;
  private static final int KEY_NUMBER_7 = 39;
  private static final int KEY_NUMBER_8 = 40;
  private static final int KEY_NUMBER_9 = 41;
  private static final int KEY_CHANNEL_UP = 48;
  private static final int KEY_CHANNEL_DOWN = 49;
  private static final int KEY_POWER = 64;
  private static final int KEY_VOLUME_UP = 65;
  private static final int KEY_VOLUME_DOWN = 66;
  private static final int KEY_MUTE = 67;
  private static final int KEY_PLAY = 68;
  private static final int KEY_STOP = 69;
  private static final int KEY_PAUSE = 70;
  private static final int KEY_REWIND = 72;
  private static final int KEY_FAST_FORWARD = 73;
  
  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_remote_control, container, false);
  }
  
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    deviceStatusText = view.findViewById(R.id.deviceStatusText);
    remoteControlPanel = view.findViewById(R.id.remoteControlPanel);
    
    checkDeviceConnection();
    setupRemoteButtons(view);
  }
  
  private void checkDeviceConnection() {
    if (CommissionedDeviceHelper.hasCommissionedDevice()) {
      String deviceInfo = CommissionedDeviceHelper.getCommissionedDeviceInfo();
      deviceStatusText.setText("Connected to: " + deviceInfo);
      deviceStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
      remoteControlPanel.setVisibility(View.VISIBLE);
    } else {
      deviceStatusText.setText("No commissioned device found. Please commission a device first.");
      deviceStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
      remoteControlPanel.setVisibility(View.GONE);
    }
  }
  
  private void setupRemoteButtons(View view) {
    // Navigation buttons (D-pad)
    setupButton(view, R.id.btnUp, KEY_UP, "Up");
    setupButton(view, R.id.btnDown, KEY_DOWN, "Down");
    setupButton(view, R.id.btnLeft, KEY_LEFT, "Left");
    setupButton(view, R.id.btnRight, KEY_RIGHT, "Right");
    setupButton(view, R.id.btnSelect, KEY_SELECT, "Select");
    
    // Menu buttons
    setupButton(view, R.id.btnHome, KEY_ROOT_MENU, "Home/Menu");
    setupButton(view, R.id.btnBack, KEY_EXIT, "Back/Exit");
    setupButton(view, R.id.btnMenu, KEY_SETUP_MENU, "Menu");
    
    // Volume buttons
    setupButton(view, R.id.btnVolumeUp, KEY_VOLUME_UP, "Volume Up");
    setupButton(view, R.id.btnVolumeDown, KEY_VOLUME_DOWN, "Volume Down");
    setupButton(view, R.id.btnMute, KEY_MUTE, "Mute");
    
    // Channel buttons
    setupButton(view, R.id.btnChannelUp, KEY_CHANNEL_UP, "Channel Up");
    setupButton(view, R.id.btnChannelDown, KEY_CHANNEL_DOWN, "Channel Down");
    
    // Media control buttons
    setupButton(view, R.id.btnPlay, KEY_PLAY, "Play");
    setupButton(view, R.id.btnPause, KEY_PAUSE, "Pause");
    setupButton(view, R.id.btnStop, KEY_STOP, "Stop");
    setupButton(view, R.id.btnRewind, KEY_REWIND, "Rewind");
    setupButton(view, R.id.btnFastForward, KEY_FAST_FORWARD, "Fast Forward");
    
    // Number buttons
    setupButton(view, R.id.btn0, KEY_NUMBER_0, "0");
    setupButton(view, R.id.btn1, KEY_NUMBER_1, "1");
    setupButton(view, R.id.btn2, KEY_NUMBER_2, "2");
    setupButton(view, R.id.btn3, KEY_NUMBER_3, "3");
    setupButton(view, R.id.btn4, KEY_NUMBER_4, "4");
    setupButton(view, R.id.btn5, KEY_NUMBER_5, "5");
    setupButton(view, R.id.btn6, KEY_NUMBER_6, "6");
    setupButton(view, R.id.btn7, KEY_NUMBER_7, "7");
    setupButton(view, R.id.btn8, KEY_NUMBER_8, "8");
    setupButton(view, R.id.btn9, KEY_NUMBER_9, "9");
    
    // Power button
    setupButton(view, R.id.btnPower, KEY_POWER, "Power");
  }
  
  private void setupButton(View parentView, int buttonId, int keyCode, String keyName) {
    Button button = parentView.findViewById(buttonId);
    if (button != null) {
      button.setOnClickListener(v -> sendKeyCommand(keyCode, keyName));
    }
  }
  
  private void sendKeyCommand(int keyCode, String keyName) {
    Log.d(TAG, "Sending key command: " + keyName + " (code: " + keyCode + ")");
    
    // Use the native method to send KeypadInput command
    boolean success = sendKeyToDevice(keyCode);
    
    if (success) {
      showFeedback("Sent: " + keyName);
    } else {
      showError("Failed to send: " + keyName);
    }
  }
  
  private void showFeedback(String message) {
    if (getActivity() != null) {
      Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
  }
  
  private void showError(String message) {
    if (getActivity() != null) {
      Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }
  }
  
  /**
   * Native method to send KeypadInput SendKey command via CastingServer API.
   * This will be implemented in JNI to call:
   * CastingServer::KeypadInput_SendKey(endpoint, CECKeyCodeEnum, callback)
   * 
   * @param keyCode The CEC key code to send
   * @return true if command was sent successfully
   */
  private native boolean sendKeyToDevice(int keyCode);
}
