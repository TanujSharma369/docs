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
 * Premium One-Page Controller with Matte Black Theme
 * Features: AI-style voice input, connection indicator, navigation controls,
 * app launchers, and numeric keypad - all on one sleek screen
 */
public class PremiumControllerFragment extends Fragment {
  private static final String TAG = PremiumControllerFragment.class.getSimpleName();
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
  
  // UI Components
  private View voiceInputIcon;
  private android.widget.ImageView statusIndicator;
  private TextView statusLabel;
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
  private static final int KEY_VOLUME_UP = 65;
  private static final int KEY_VOLUME_DOWN = 66;
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
  
  static {
    System.loadLibrary("TvCastingApp");
  }
  
  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_premium_controller, container, false);
  }
  
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    initializeViews(view);
    checkPermissions();
    updateConnectionStatus();
    setupVoiceInput();
    setupNavigationControls(view);
    setupAppLaunchers(view);
    setupKeypad(view);
    
    // Start monitoring connection status
    startConnectionMonitoring();
  }
  
  private void initializeViews(View view) {
    voiceInputIcon = view.findViewById(R.id.voiceInputIcon);
    statusIndicator = view.findViewById(R.id.statusIndicator);
    statusLabel = view.findViewById(R.id.statusLabel);
  }
  
  private void checkPermissions() {
    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getActivity(),
          new String[]{Manifest.permission.RECORD_AUDIO},
          REQUEST_RECORD_AUDIO_PERMISSION);
    }
  }
  
  private void updateConnectionStatus() {
    boolean connected = ManualCommissioningHelper.hasCommissionedVideoPlayer();
    if (connected) {
      statusIndicator.setImageResource(R.drawable.indicator_connected);
      if (statusLabel != null) statusLabel.setText("Connected");
    } else {
      statusIndicator.setImageResource(R.drawable.indicator_disconnected);
      if (statusLabel != null) statusLabel.setText("Disconnected");
    }
  }
  
  private void startConnectionMonitoring() {
    // Monitor connection status every 2 seconds
    final android.os.Handler handler = new android.os.Handler();
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        updateConnectionStatus();
        handler.postDelayed(this, 2000);
      }
    }, 2000);
  }
  
  // ========== VOICE INPUT ==========
  
  private void setupVoiceInput() {
    if (!SpeechRecognizer.isRecognitionAvailable(getContext())) {
      voiceInputIcon.setEnabled(false);
      voiceInputIcon.setAlpha(0.3f);
      return;
    }
    
    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
    speechRecognizer.setRecognitionListener(new RecognitionListener() {
      @Override
      public void onReadyForSpeech(Bundle params) {
        voiceInputIcon.setAlpha(1.0f);
      }
      
      @Override
      public void onBeginningOfSpeech() {}
      
      @Override
      public void onRmsChanged(float rmsdB) {
        // Animate voice icon based on audio level
        float alpha = 0.5f + (rmsdB / 20.0f);
        voiceInputIcon.setAlpha(Math.max(0.5f, Math.min(1.0f, alpha)));
      }
      
      @Override
      public void onBufferReceived(byte[] buffer) {}
      
      @Override
      public void onEndOfSpeech() {
        voiceInputIcon.setAlpha(0.7f);
      }
      
      @Override
      public void onError(int error) {
        isListening = false;
        voiceInputIcon.setAlpha(0.7f);
      }
      
      @Override
      public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
          String command = matches.get(0);
          processVoiceCommand(command);
        }
        isListening = false;
        voiceInputIcon.setAlpha(0.7f);
        updateConnectionStatus();
      }
      
      @Override
      public void onPartialResults(Bundle partialResults) {}
      
      @Override
      public void onEvent(int eventType, Bundle params) {}
    });
    
    voiceInputIcon.setOnClickListener(v -> toggleVoiceInput());
    voiceInputIcon.setAlpha(0.7f); // Default dimmed state
  }
  
  private void toggleVoiceInput() {
    if (!ManualCommissioningHelper.hasCommissionedVideoPlayer()) {
      Toast.makeText(getContext(), "Connect device first", Toast.LENGTH_SHORT).show();
      return;
    }
    
    if (isListening) {
      stopListening();
    } else {
      startListening();
    }
  }
  
  private void startListening() {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    
    speechRecognizer.startListening(intent);
    isListening = true;
  }
  
  private void stopListening() {
    if (speechRecognizer != null && isListening) {
      speechRecognizer.stopListening();
      isListening = false;
      voiceInputIcon.setAlpha(0.7f);
      updateConnectionStatus();
    }
  }
  
  private void processVoiceCommand(String command) {
    String lowerCommand = command.toLowerCase().trim();
    Log.i(TAG, "Voice command: " + lowerCommand);
    
    // App launch commands
    if (lowerCommand.contains("youtube") || lowerCommand.contains("you tube")) {
      launchApp("YouTube");
    } else if (lowerCommand.contains("netflix")) {
      launchApp("Netflix");
    }
    // Navigation commands
    else if (lowerCommand.contains("up")) {
      sendKey(KEY_UP, "Up");
    } else if (lowerCommand.contains("down")) {
      sendKey(KEY_DOWN, "Down");
    } else if (lowerCommand.contains("left")) {
      sendKey(KEY_LEFT, "Left");
    } else if (lowerCommand.contains("right")) {
      sendKey(KEY_RIGHT, "Right");
    } else if (lowerCommand.contains("select") || lowerCommand.contains("ok") || lowerCommand.contains("enter")) {
      sendKey(KEY_SELECT, "OK");
    } else if (lowerCommand.contains("home") || lowerCommand.contains("menu")) {
      sendKey(KEY_ROOT_MENU, "Home");
    } else if (lowerCommand.contains("back") || lowerCommand.contains("exit")) {
      sendKey(KEY_EXIT, "Back");
    }
    // Volume commands
    else if (lowerCommand.contains("volume up") || lowerCommand.contains("louder")) {
      sendKey(KEY_VOLUME_UP, "Volume Up");
    } else if (lowerCommand.contains("volume down") || lowerCommand.contains("quieter")) {
      sendKey(KEY_VOLUME_DOWN, "Volume Down");
    }
    // Number commands
    else if (lowerCommand.matches(".*\\b(zero|0)\\b.*")) {
      sendKey(KEY_NUMBER_0, "0");
    } else if (lowerCommand.matches(".*\\b(one|1)\\b.*")) {
      sendKey(KEY_NUMBER_1, "1");
    } else if (lowerCommand.matches(".*\\b(two|2)\\b.*")) {
      sendKey(KEY_NUMBER_2, "2");
    } else if (lowerCommand.matches(".*\\b(three|3)\\b.*")) {
      sendKey(KEY_NUMBER_3, "3");
    } else if (lowerCommand.matches(".*\\b(four|4)\\b.*")) {
      sendKey(KEY_NUMBER_4, "4");
    } else if (lowerCommand.matches(".*\\b(five|5)\\b.*")) {
      sendKey(KEY_NUMBER_5, "5");
    } else if (lowerCommand.matches(".*\\b(six|6)\\b.*")) {
      sendKey(KEY_NUMBER_6, "6");
    } else if (lowerCommand.matches(".*\\b(seven|7)\\b.*")) {
      sendKey(KEY_NUMBER_7, "7");
    } else if (lowerCommand.matches(".*\\b(eight|8)\\b.*")) {
      sendKey(KEY_NUMBER_8, "8");
    } else if (lowerCommand.matches(".*\\b(nine|9)\\b.*")) {
      sendKey(KEY_NUMBER_9, "9");
    }
  }
  
  // ========== NAVIGATION CONTROLS ==========
  
  private void setupNavigationControls(View view) {
    view.findViewById(R.id.navUp).setOnClickListener(v -> sendKey(KEY_UP, "Up"));
    view.findViewById(R.id.navDown).setOnClickListener(v -> sendKey(KEY_DOWN, "Down"));
    view.findViewById(R.id.navLeft).setOnClickListener(v -> sendKey(KEY_LEFT, "Left"));
    view.findViewById(R.id.navRight).setOnClickListener(v -> sendKey(KEY_RIGHT, "Right"));
    view.findViewById(R.id.okButton).setOnClickListener(v -> sendKey(KEY_SELECT, "OK"));
    view.findViewById(R.id.homeButton).setOnClickListener(v -> sendKey(KEY_ROOT_MENU, "Home"));
    view.findViewById(R.id.backButton).setOnClickListener(v -> sendKey(KEY_EXIT, "Back"));
    view.findViewById(R.id.volumeUpButton).setOnClickListener(v -> sendKey(KEY_VOLUME_UP, "Volume Up"));
    view.findViewById(R.id.volumeDownButton).setOnClickListener(v -> sendKey(KEY_VOLUME_DOWN, "Volume Down"));
  }
  
  // ========== APP LAUNCHERS ==========
  
  private void setupAppLaunchers(View view) {
    view.findViewById(R.id.youtubeCard).setOnClickListener(v -> launchApp("YouTube"));
    view.findViewById(R.id.netflixCard).setOnClickListener(v -> launchApp("Netflix"));
  }
  
  private void launchApp(String appName) {
    if (!ManualCommissioningHelper.hasCommissionedVideoPlayer()) {
      Toast.makeText(getContext(), "Connect device first", Toast.LENGTH_SHORT).show();
      return;
    }
    
    new Thread(() -> {
      launchAppNative(0, appName);
    }).start();
  }
  
  // ========== KEYPAD ==========
  
  private void setupKeypad(View view) {
    view.findViewById(R.id.key0).setOnClickListener(v -> sendKey(KEY_NUMBER_0, "0"));
    view.findViewById(R.id.key1).setOnClickListener(v -> sendKey(KEY_NUMBER_1, "1"));
    view.findViewById(R.id.key2).setOnClickListener(v -> sendKey(KEY_NUMBER_2, "2"));
    view.findViewById(R.id.key3).setOnClickListener(v -> sendKey(KEY_NUMBER_3, "3"));
    view.findViewById(R.id.key4).setOnClickListener(v -> sendKey(KEY_NUMBER_4, "4"));
    view.findViewById(R.id.key5).setOnClickListener(v -> sendKey(KEY_NUMBER_5, "5"));
    view.findViewById(R.id.key6).setOnClickListener(v -> sendKey(KEY_NUMBER_6, "6"));
    view.findViewById(R.id.key7).setOnClickListener(v -> sendKey(KEY_NUMBER_7, "7"));
    view.findViewById(R.id.key8).setOnClickListener(v -> sendKey(KEY_NUMBER_8, "8"));
    view.findViewById(R.id.key9).setOnClickListener(v -> sendKey(KEY_NUMBER_9, "9"));
  }
  
  private void sendKey(int keyCode, String keyName) {
    if (!ManualCommissioningHelper.hasCommissionedVideoPlayer()) {
      Toast.makeText(getContext(), "Connect device first", Toast.LENGTH_SHORT).show();
      return;
    }
    
    sendKeyToDevice(keyCode);
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
  private native boolean launchAppNative(int catalogVendorId, String applicationId);
}
