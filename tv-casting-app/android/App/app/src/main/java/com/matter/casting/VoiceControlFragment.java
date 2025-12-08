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
import java.util.ArrayList;

/**
 * Voice Control Fragment - Provides voice command interface to control device
 * Supports both KeypadInput commands and ApplicationLauncher commands
 */
public class VoiceControlFragment extends Fragment {
  private static final String TAG = VoiceControlFragment.class.getSimpleName();
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
  
  private TextView deviceStatusText;
  private TextView listeningStatusText;
  private TextView lastCommandText;
  private Button startListeningButton;
  private Button stopListeningButton;
  
  private SpeechRecognizer speechRecognizer;
  private boolean isListening = false;
  
  // CEC Key Codes
  private static final int KEY_SELECT = 0;
  private static final int KEY_UP = 1;
  private static final int KEY_DOWN = 2;
  private static final int KEY_LEFT = 3;
  private static final int KEY_RIGHT = 4;
  private static final int KEY_ROOT_MENU = 9;
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
    return inflater.inflate(R.layout.fragment_voice_control, container, false);
  }
  
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    deviceStatusText = view.findViewById(R.id.voiceDeviceStatusText);
    listeningStatusText = view.findViewById(R.id.listeningStatusText);
    lastCommandText = view.findViewById(R.id.lastCommandText);
    startListeningButton = view.findViewById(R.id.startListeningButton);
    stopListeningButton = view.findViewById(R.id.stopListeningButton);
    
    checkDeviceConnection();
    checkPermissions();
    setupButtons();
    initializeSpeechRecognizer();
  }
  
  private void checkDeviceConnection() {
    if (ManualCommissioningHelper.hasCommissionedVideoPlayer()) {
      String deviceInfo = ManualCommissioningHelper.getCommissionedVideoPlayerInfo();
      deviceStatusText.setText("🟢 Connected: " + deviceInfo);
      deviceStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    } else {
      deviceStatusText.setText("⚪ Not Connected - Please commission a device first");
      deviceStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
      startListeningButton.setEnabled(false);
    }
  }
  
  private void checkPermissions() {
    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getActivity(),
          new String[]{Manifest.permission.RECORD_AUDIO},
          REQUEST_RECORD_AUDIO_PERMISSION);
    }
  }
  
  private void setupButtons() {
    startListeningButton.setOnClickListener(v -> startListening());
    stopListeningButton.setOnClickListener(v -> stopListening());
    stopListeningButton.setEnabled(false);
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
      public void onRmsChanged(float rmsdB) {
        // Volume level changed
      }
      
      @Override
      public void onBufferReceived(byte[] buffer) {
        // Partial results
      }
      
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
      public void onPartialResults(Bundle partialResults) {
        // Partial results available
      }
      
      @Override
      public void onEvent(int eventType, Bundle params) {
        // Reserved for future events
      }
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
    
    // Channel keys
    else if (command.contains("channel up")) {
      keyCode = KEY_CHANNEL_UP;
      keyName = "Channel Up";
    } else if (command.contains("channel down")) {
      keyCode = KEY_CHANNEL_DOWN;
      keyName = "Channel Down";
    }
    
    // Media keys
    else if (command.contains("play")) {
      keyCode = KEY_PLAY;
      keyName = "Play";
    } else if (command.contains("pause")) {
      keyCode = KEY_PAUSE;
      keyName = "Pause";
    } else if (command.contains("stop")) {
      keyCode = KEY_STOP;
      keyName = "Stop";
    } else if (command.contains("rewind")) {
      keyCode = KEY_REWIND;
      keyName = "Rewind";
    } else if (command.contains("forward") || command.contains("fast forward")) {
      keyCode = KEY_FAST_FORWARD;
      keyName = "Fast Forward";
    }
    
    // Number keys
    else if (command.contains("zero") || command.matches(".*\\b0\\b.*")) {
      keyCode = KEY_NUMBER_0;
      keyName = "0";
    } else if (command.contains("one") || command.matches(".*\\b1\\b.*")) {
      keyCode = KEY_NUMBER_1;
      keyName = "1";
    } else if (command.contains("two") || command.matches(".*\\b2\\b.*")) {
      keyCode = KEY_NUMBER_2;
      keyName = "2";
    } else if (command.contains("three") || command.matches(".*\\b3\\b.*")) {
      keyCode = KEY_NUMBER_3;
      keyName = "3";
    } else if (command.contains("four") || command.matches(".*\\b4\\b.*")) {
      keyCode = KEY_NUMBER_4;
      keyName = "4";
    } else if (command.contains("five") || command.matches(".*\\b5\\b.*")) {
      keyCode = KEY_NUMBER_5;
      keyName = "5";
    } else if (command.contains("six") || command.matches(".*\\b6\\b.*")) {
      keyCode = KEY_NUMBER_6;
      keyName = "6";
    } else if (command.contains("seven") || command.matches(".*\\b7\\b.*")) {
      keyCode = KEY_NUMBER_7;
      keyName = "7";
    } else if (command.contains("eight") || command.matches(".*\\b8\\b.*")) {
      keyCode = KEY_NUMBER_8;
      keyName = "8";
    } else if (command.contains("nine") || command.matches(".*\\b9\\b.*")) {
      keyCode = KEY_NUMBER_9;
      keyName = "9";
    } else if (command.contains("power")) {
      keyCode = KEY_POWER;
      keyName = "Power";
    }
    
    if (keyCode != null) {
      final String finalKeyName = keyName;
      listeningStatusText.setText("✓ Sending: " + keyName);
      listeningStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
      
      boolean success = sendKeyToDevice(keyCode);
      
      if (success) {
        Toast.makeText(getContext(), "✓ Sent: " + finalKeyName, Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(getContext(), "✗ Failed to send: " + finalKeyName, Toast.LENGTH_LONG).show();
      }
    } else {
      listeningStatusText.setText("❓ Command not recognized");
      listeningStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
      Toast.makeText(getContext(), "Command not recognized. Try: 'press left', 'launch youtube', etc.", 
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
