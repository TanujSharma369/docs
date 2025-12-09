package com.matter.casting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.R;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;

/**
 * Unified Control Fragment - Combines Virtual Remote, App Launcher, and Voice Control
 * into a single convenient interface with tabs.
 */
public class UnifiedControlFragment extends Fragment {
  private static final String TAG = UnifiedControlFragment.class.getSimpleName();
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
  
  // UI Components
  private TextView deviceStatusText;
  private TabLayout controlTabs;
  private View remoteControlSection;
  private View appLauncherSection;
  private View voiceControlSection;
  
  // Voice Control Components
  private TextView listeningStatusText;
  private TextView lastCommandText;
  private Button startListeningButton;
  private Button stopListeningButton;
  private SpeechRecognizer speechRecognizer;
  private boolean isListening = false;
  
  // App Launcher Components
  private TextView appLauncherStatusText;
  
  // CEC Key Codes
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
  
  static {
    System.loadLibrary("TvCastingApp");
  }
  
  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_unified_control, container, false);
  }
  
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    initializeViews(view);
    checkDeviceConnection();
    setupTabs();
    setupRemoteControl(view);
    setupAppLauncher(view);
    setupVoiceControl(view);
  }
  
  private void initializeViews(View view) {
    deviceStatusText = view.findViewById(R.id.unifiedDeviceStatusText);
    controlTabs = view.findViewById(R.id.controlTabs);
    remoteControlSection = view.findViewById(R.id.remoteControlSection);
    appLauncherSection = view.findViewById(R.id.appLauncherSection);
    voiceControlSection = view.findViewById(R.id.voiceControlSection);
    
    // Voice Control Views
    listeningStatusText = view.findViewById(R.id.listeningStatusText);
    lastCommandText = view.findViewById(R.id.lastCommandText);
    startListeningButton = view.findViewById(R.id.startListeningButton);
    stopListeningButton = view.findViewById(R.id.stopListeningButton);
    
    // App Launcher Views
    appLauncherStatusText = view.findViewById(R.id.appLauncherStatusText);
  }
  
  private void checkDeviceConnection() {
    if (ManualCommissioningHelper.hasCommissionedVideoPlayer()) {
      String deviceInfo = ManualCommissioningHelper.getCommissionedVideoPlayerInfo();
      deviceStatusText.setText("🟢 Connected: " + deviceInfo);
      deviceStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    } else {
      deviceStatusText.setText("⚪ Not Connected - Please commission a device first");
      deviceStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
      disableAllControls();
    }
  }
  
  private void disableAllControls() {
    remoteControlSection.setAlpha(0.5f);
    appLauncherSection.setAlpha(0.5f);
    voiceControlSection.setAlpha(0.5f);
    startListeningButton.setEnabled(false);
  }
  
  private void setupTabs() {
    controlTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      @Override
      public void onTabSelected(TabLayout.Tab tab) {
        int position = tab.getPosition();
        showSection(position);
      }
      
      @Override
      public void onTabUnselected(TabLayout.Tab tab) {
        // Stop listening if leaving voice control tab
        if (tab.getPosition() == 2 && isListening) {
          stopListening();
        }
      }
      
      @Override
      public void onTabReselected(TabLayout.Tab tab) {}
    });
    
    // Show first tab by default
    showSection(0);
  }
  
  private void showSection(int position) {
    remoteControlSection.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
    appLauncherSection.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
    voiceControlSection.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
  }
  
  // ========== REMOTE CONTROL SETUP ==========
  
  private void setupRemoteControl(View view) {
    if (!ManualCommissioningHelper.hasCommissionedVideoPlayer()) return;
    
    // Navigation buttons (D-pad)
    setupRemoteButton(view, R.id.btnUp, KEY_UP, "Up");
    setupRemoteButton(view, R.id.btnDown, KEY_DOWN, "Down");
    setupRemoteButton(view, R.id.btnLeft, KEY_LEFT, "Left");
    setupRemoteButton(view, R.id.btnRight, KEY_RIGHT, "Right");
    setupRemoteButton(view, R.id.btnSelect, KEY_SELECT, "Select");
    
    // Menu buttons
    setupRemoteButton(view, R.id.btnHome, KEY_ROOT_MENU, "Home");
    setupRemoteButton(view, R.id.btnBack, KEY_EXIT, "Back");
    setupRemoteButton(view, R.id.btnMenu, KEY_SETUP_MENU, "Menu");
    
    // Volume buttons
    setupRemoteButton(view, R.id.btnVolumeUp, KEY_VOLUME_UP, "Vol+");
    setupRemoteButton(view, R.id.btnVolumeDown, KEY_VOLUME_DOWN, "Vol-");
    setupRemoteButton(view, R.id.btnMute, KEY_MUTE, "Mute");
    
    // Channel buttons
    setupRemoteButton(view, R.id.btnChannelUp, KEY_CHANNEL_UP, "CH+");
    setupRemoteButton(view, R.id.btnChannelDown, KEY_CHANNEL_DOWN, "CH-");
    
    // Media control buttons
    setupRemoteButton(view, R.id.btnPlay, KEY_PLAY, "Play");
    setupRemoteButton(view, R.id.btnPause, KEY_PAUSE, "Pause");
    setupRemoteButton(view, R.id.btnStop, KEY_STOP, "Stop");
    setupRemoteButton(view, R.id.btnRewind, KEY_REWIND, "⏪");
    setupRemoteButton(view, R.id.btnFastForward, KEY_FAST_FORWARD, "⏩");
    
    // Number buttons
    setupRemoteButton(view, R.id.btn0, KEY_NUMBER_0, "0");
    setupRemoteButton(view, R.id.btn1, KEY_NUMBER_1, "1");
    setupRemoteButton(view, R.id.btn2, KEY_NUMBER_2, "2");
    setupRemoteButton(view, R.id.btn3, KEY_NUMBER_3, "3");
    setupRemoteButton(view, R.id.btn4, KEY_NUMBER_4, "4");
    setupRemoteButton(view, R.id.btn5, KEY_NUMBER_5, "5");
    setupRemoteButton(view, R.id.btn6, KEY_NUMBER_6, "6");
    setupRemoteButton(view, R.id.btn7, KEY_NUMBER_7, "7");
    setupRemoteButton(view, R.id.btn8, KEY_NUMBER_8, "8");
    setupRemoteButton(view, R.id.btn9, KEY_NUMBER_9, "9");
    
    // Power button
    setupRemoteButton(view, R.id.btnPower, KEY_POWER, "Power");
  }
  
  private void setupRemoteButton(View parentView, int buttonId, int keyCode, String keyName) {
    Button button = parentView.findViewById(buttonId);
    if (button != null) {
      button.setOnClickListener(v -> sendKeyCommand(keyCode, keyName));
    }
  }
  
  private void sendKeyCommand(int keyCode, String keyName) {
    Log.d(TAG, "Sending key command: " + keyName + " (code: " + keyCode + ")");
    boolean success = sendKeyToDevice(keyCode);
    
    if (success) {
      Toast.makeText(getContext(), "✓ " + keyName, Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(getContext(), "✗ Failed: " + keyName, Toast.LENGTH_SHORT).show();
    }
  }
  
  // ========== APP LAUNCHER SETUP ==========
  
  private void setupAppLauncher(View view) {
    if (!ManualCommissioningHelper.hasCommissionedVideoPlayer()) return;
    
    // YouTube
    view.findViewById(R.id.launchYouTubeButton).setOnClickListener(v -> 
      launchApplication(0, "YouTube"));
    view.findViewById(R.id.stopYouTubeButton).setOnClickListener(v -> 
      stopApplication(0, "YouTube"));
    
    // Netflix
    view.findViewById(R.id.launchNetflixButton).setOnClickListener(v -> 
      launchApplication(0, "Netflix"));
    view.findViewById(R.id.stopNetflixButton).setOnClickListener(v -> 
      stopApplication(0, "Netflix"));
    
    // Prime Video
    view.findViewById(R.id.launchPrimeButton).setOnClickListener(v -> 
      launchApplication(0, "Prime Video"));
    view.findViewById(R.id.stopPrimeButton).setOnClickListener(v -> 
      stopApplication(0, "Prime Video"));
    
    // Disney+
    view.findViewById(R.id.launchDisneyButton).setOnClickListener(v -> 
      launchApplication(0, "Disney+"));
    view.findViewById(R.id.stopDisneyButton).setOnClickListener(v -> 
      stopApplication(0, "Disney+"));
  }
  
  private void launchApplication(int catalogVendorId, String applicationId) {
    Log.d(TAG, "Launching: " + applicationId);
    appLauncherStatusText.setText("🚀 Launching " + applicationId + "...");
    appLauncherStatusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
    
    new Thread(() -> {
      boolean success = launchApp(catalogVendorId, applicationId);
      
      if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
          if (success) {
            appLauncherStatusText.setText("✓ Launched: " + applicationId);
            appLauncherStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            Toast.makeText(getContext(), "✓ " + applicationId + " launched", Toast.LENGTH_SHORT).show();
          } else {
            appLauncherStatusText.setText("✗ Failed to launch: " + applicationId);
            appLauncherStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            Toast.makeText(getContext(), "✗ Failed to launch " + applicationId, Toast.LENGTH_LONG).show();
          }
        });
      }
    }).start();
  }
  
  private void stopApplication(int catalogVendorId, String applicationId) {
    Log.d(TAG, "Stopping: " + applicationId);
    appLauncherStatusText.setText("⏹️ Stopping " + applicationId + "...");
    appLauncherStatusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
    
    new Thread(() -> {
      boolean success = stopApp(catalogVendorId, applicationId);
      
      if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
          if (success) {
            appLauncherStatusText.setText("✓ Stopped: " + applicationId);
            appLauncherStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            Toast.makeText(getContext(), "✓ " + applicationId + " stopped", Toast.LENGTH_SHORT).show();
          } else {
            appLauncherStatusText.setText("✗ Failed to stop: " + applicationId);
            appLauncherStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            Toast.makeText(getContext(), "✗ Failed to stop " + applicationId, Toast.LENGTH_LONG).show();
          }
        });
      }
    }).start();
  }
  
  // ========== VOICE CONTROL SETUP ==========
  
  private void setupVoiceControl(View view) {
    checkPermissions();
    
    if (!ManualCommissioningHelper.hasCommissionedVideoPlayer()) {
      startListeningButton.setEnabled(false);
      return;
    }
    
    startListeningButton.setOnClickListener(v -> startListening());
    stopListeningButton.setOnClickListener(v -> stopListening());
    stopListeningButton.setEnabled(false);
    
    initializeSpeechRecognizer();
  }
  
  private void checkPermissions() {
    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getActivity(),
          new String[]{Manifest.permission.RECORD_AUDIO},
          REQUEST_RECORD_AUDIO_PERMISSION);
    }
  }
  
  private void initializeSpeechRecognizer() {
    if (!SpeechRecognizer.isRecognitionAvailable(getContext())) {
      Toast.makeText(getContext(), "Speech recognition not available", Toast.LENGTH_LONG).show();
      startListeningButton.setEnabled(false);
      return;
    }
    
    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
    speechRecognizer.setRecognitionListener(new RecognitionListener() {
      @Override
      public void onReadyForSpeech(Bundle params) {
        listeningStatusText.setText("🎤 Listening... Speak now!");
        listeningStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
      }
      
      @Override
      public void onBeginningOfSpeech() {
        Log.d(TAG, "Speech detected");
      }
      
      @Override
      public void onRmsChanged(float rmsdB) {}
      
      @Override
      public void onBufferReceived(byte[] buffer) {}
      
      @Override
      public void onEndOfSpeech() {
        listeningStatusText.setText("⏳ Processing...");
      }
      
      @Override
      public void onError(int error) {
        String errorMsg = getErrorMessage(error);
        listeningStatusText.setText("❌ Error: " + errorMsg);
        listeningStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        isListening = false;
        startListeningButton.setEnabled(true);
        stopListeningButton.setEnabled(false);
        Log.e(TAG, "Speech recognition error: " + errorMsg);
      }
      
      @Override
      public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
          String spokenText = matches.get(0);
          Log.i(TAG, "Recognized speech: " + spokenText);
          processVoiceCommand(spokenText);
        }
        isListening = false;
        startListeningButton.setEnabled(true);
        stopListeningButton.setEnabled(false);
      }
      
      @Override
      public void onPartialResults(Bundle partialResults) {}
      
      @Override
      public void onEvent(int eventType, Bundle params) {}
    });
  }
  
  private void startListening() {
    if (isListening) return;
    
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    
    listeningStatusText.setText("🎤 Starting...");
    listeningStatusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
    
    speechRecognizer.startListening(intent);
    isListening = true;
    startListeningButton.setEnabled(false);
    stopListeningButton.setEnabled(true);
    
    Log.i(TAG, "Started listening for voice commands");
  }
  
  private void stopListening() {
    if (speechRecognizer != null && isListening) {
      speechRecognizer.stopListening();
      isListening = false;
      listeningStatusText.setText("⏹️ Stopped");
      listeningStatusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
      startListeningButton.setEnabled(true);
      stopListeningButton.setEnabled(false);
      Log.i(TAG, "Stopped listening");
    }
  }
  
  private void processVoiceCommand(String command) {
    String lowerCommand = command.toLowerCase().trim();
    lastCommandText.setText("Heard: \"" + command + "\"");
    
    Log.i(TAG, "Processing voice command: " + lowerCommand);
    
    // Check for application launch commands first
    if (lowerCommand.contains("launch") || lowerCommand.contains("open") || lowerCommand.contains("start")) {
      handleApplicationCommand(lowerCommand, true);
      return;
    }
    
    if (lowerCommand.contains("stop") || lowerCommand.contains("close")) {
      handleApplicationCommand(lowerCommand, false);
      return;
    }
    
    // Check for keypad commands
    handleKeypadCommand(lowerCommand);
  }
  
  private void handleApplicationCommand(String command, boolean isLaunch) {
    String appName = null;
    
    if (command.contains("youtube")) {
      appName = "YouTube";
    } else if (command.contains("netflix")) {
      appName = "Netflix";
    } else if (command.contains("prime") || command.contains("amazon")) {
      appName = "Prime Video";
    } else if (command.contains("disney")) {
      appName = "Disney+";
    }
    
    if (appName != null) {
      final String finalAppName = appName;
      listeningStatusText.setText("✓ " + (isLaunch ? "Launching" : "Stopping") + " " + appName);
      listeningStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
      
      new Thread(() -> {
        boolean success;
        if (isLaunch) {
          success = launchApp(0, finalAppName);
        } else {
          success = stopApp(0, finalAppName);
        }
        
        if (getActivity() != null) {
          getActivity().runOnUiThread(() -> {
            if (success) {
              Toast.makeText(getContext(), 
                "✓ " + (isLaunch ? "Launched" : "Stopped") + ": " + finalAppName, 
                Toast.LENGTH_SHORT).show();
            } else {
              Toast.makeText(getContext(), 
                "✗ Failed to " + (isLaunch ? "launch" : "stop") + " " + finalAppName, 
                Toast.LENGTH_LONG).show();
            }
          });
        }
      }).start();
    } else {
      listeningStatusText.setText("❓ Unknown app in command");
      listeningStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
    }
  }
  
  private void handleKeypadCommand(String command) {
    Integer keyCode = null;
    String keyName = null;
    
    // Navigation keys
    if (command.contains("left")) {
      keyCode = KEY_LEFT;
      keyName = "Left";
    } else if (command.contains("right")) {
      keyCode = KEY_RIGHT;
      keyName = "Right";
    } else if (command.contains("up")) {
      keyCode = KEY_UP;
      keyName = "Up";
    } else if (command.contains("down")) {
      keyCode = KEY_DOWN;
      keyName = "Down";
    } else if (command.contains("select") || command.contains("ok") || command.contains("enter")) {
      keyCode = KEY_SELECT;
      keyName = "Select";
    }
    // Menu keys
    else if (command.contains("home") || command.contains("menu")) {
      keyCode = KEY_ROOT_MENU;
      keyName = "Home/Menu";
    } else if (command.contains("back") || command.contains("exit")) {
      keyCode = KEY_EXIT;
      keyName = "Back";
    }
    // Volume keys
    else if (command.contains("volume up") || command.contains("louder")) {
      keyCode = KEY_VOLUME_UP;
      keyName = "Volume Up";
    } else if (command.contains("volume down") || command.contains("quieter")) {
      keyCode = KEY_VOLUME_DOWN;
      keyName = "Volume Down";
    } else if (command.contains("mute")) {
      keyCode = KEY_MUTE;
      keyName = "Mute";
    }
    // Media keys
    else if (command.contains("play")) {
      keyCode = KEY_PLAY;
      keyName = "Play";
    } else if (command.contains("pause")) {
      keyCode = KEY_PAUSE;
      keyName = "Pause";
    } else if (command.contains("rewind")) {
      keyCode = KEY_REWIND;
      keyName = "Rewind";
    } else if (command.contains("forward") || command.contains("fast forward")) {
      keyCode = KEY_FAST_FORWARD;
      keyName = "Fast Forward";
    }
    
    if (keyCode != null) {
      final String finalKeyName = keyName;
      listeningStatusText.setText("✓ Sending: " + keyName);
      listeningStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
      
      boolean success = sendKeyToDevice(keyCode);
      
      if (success) {
        Toast.makeText(getContext(), "✓ " + finalKeyName, Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(getContext(), "✗ Failed: " + finalKeyName, Toast.LENGTH_LONG).show();
      }
    } else {
      listeningStatusText.setText("❓ Command not recognized");
      listeningStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
      Toast.makeText(getContext(), "Try: 'launch youtube', 'press left', etc.", 
        Toast.LENGTH_LONG).show();
    }
  }
  
  private String getErrorMessage(int error) {
    switch (error) {
      case SpeechRecognizer.ERROR_AUDIO:
        return "Audio recording error";
      case SpeechRecognizer.ERROR_CLIENT:
        return "Client error";
      case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
        return "Insufficient permissions";
      case SpeechRecognizer.ERROR_NETWORK:
        return "Network error";
      case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
        return "Network timeout";
      case SpeechRecognizer.ERROR_NO_MATCH:
        return "No speech match";
      case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
        return "Recognition service busy";
      case SpeechRecognizer.ERROR_SERVER:
        return "Server error";
      case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
        return "No speech input";
      default:
        return "Unknown error";
    }
  }
  
  @Override
  public void onDestroy() {
    super.onDestroy();
    if (speechRecognizer != null) {
      speechRecognizer.destroy();
    }
  }
  
  // Native methods
  private native boolean sendKeyToDevice(int keyCode);
  private native boolean launchApp(int catalogVendorId, String applicationId);
  private native boolean stopApp(int catalogVendorId, String applicationId);
}
