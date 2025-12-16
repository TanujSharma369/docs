package com.matter.casting;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.R;
import java.util.ArrayList;
import java.util.List;

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
  private View pairButton;
  private android.app.AlertDialog commissioningDialog;
  private SpeechRecognizer speechRecognizer;
  private boolean isListening = false;
  
  // Haptic feedback
  private Vibrator vibrator;
  private Animation pressAnim;
  private Animation releaseAnim;
  private Animation glowAnim;
  
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
    
    // Initialize haptic feedback and animations
    vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
    pressAnim = AnimationUtils.loadAnimation(getContext(), R.anim.button_press);
    releaseAnim = AnimationUtils.loadAnimation(getContext(), R.anim.button_release);
    glowAnim = AnimationUtils.loadAnimation(getContext(), R.anim.glow_pulse);
    
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

  @Override
  public void onResume() {
    super.onResume();
    
    // Only proceed if views are initialized
    if (statusIndicator == null || statusLabel == null) {
      Log.w(TAG, "onResume() called before views initialized, skipping reconnect logic");
      return;
    }
    
    // Trigger auto-reconnect to previously commissioned CastingPlayer
    // CastingApp.start() will check if there's a cached player and reconnect automatically
    new Thread(() -> {
      Log.i(TAG, "Calling CastingApp.start() to trigger auto-reconnect");
      com.matter.casting.support.MatterError err = com.matter.casting.core.CastingApp.getInstance().start();
      if (err.hasError()) {
        Log.e(TAG, "CastingApp.start() failed: " + err.getErrorMessage());
      } else {
        Log.i(TAG, "CastingApp.start() succeeded - auto-reconnect initiated if cached player exists");
        // Don't update UI immediately - let the connection monitoring detect the actual state
      }
    }).start();
    
    // Start foreground service to keep Matter stack alive in background if connected
    if (isConnectedToCastingPlayer()) {
      Intent serviceIntent = new Intent(getContext(), MatterKeepAliveService.class);
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        getContext().startForegroundService(serviceIntent);
      } else {
        getContext().startService(serviceIntent);
      }
      Log.i(TAG, "Started MatterKeepAliveService to maintain connection");
    }
  }
  
  private void initializeViews(View view) {
    voiceInputIcon = view.findViewById(R.id.voiceInputIcon);
    statusIndicator = view.findViewById(R.id.statusIndicator);
    statusLabel = view.findViewById(R.id.statusLabel);
    pairButton = view.findViewById(R.id.pairButton);
    
    // Setup pair button click with haptics
    pairButton.setOnClickListener(v -> {
      hapticFeedback(v, true);
      showCommissioningDialog();
    });
  }
  
  /**
   * Provide haptic feedback and animation for button press
   * @param view The button being pressed
   * @param isPrimary True for primary buttons (OK, Pair) to add glow effect
   */
  private void hapticFeedback(View view, boolean isPrimary) {
    // Vibrate
    if (vibrator != null && vibrator.hasVibrator()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
      } else {
        vibrator.vibrate(20);
      }
    }
    
    // Scale animation
    view.startAnimation(pressAnim);
    view.postDelayed(() -> view.startAnimation(releaseAnim), 100);
    
    // Glow effect for primary buttons
    if (isPrimary) {
      view.startAnimation(glowAnim);
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
  
  private void updateConnectionStatus() {
    // Guard against null views
    if (statusIndicator == null || statusLabel == null) {
      Log.w(TAG, "updateConnectionStatus() called but views not initialized yet");
      return;
    }
    
    boolean connected = isConnectedToCastingPlayer();
    if (connected) {
      statusIndicator.setImageResource(R.drawable.indicator_connected);
      statusLabel.setText("Connected");
      Log.i(TAG, "UI updated: Connected to CastingPlayer");
    } else {
      statusIndicator.setImageResource(R.drawable.indicator_disconnected);
      statusLabel.setText("Disconnected");
      Log.i(TAG, "UI updated: Disconnected from CastingPlayer");
    }
  }
  
  private boolean isConnectedToCastingPlayer() {
    try {
      List<com.matter.casting.core.CastingPlayer> players = 
          ManualCommissioningMonitor.getInstance().getConnectedCastingPlayers();
      if (players != null && !players.isEmpty()) {
        for (com.matter.casting.core.CastingPlayer player : players) {
          if (player.getConnectionState() == com.matter.casting.core.CastingPlayer.ConnectionState.CONNECTED) {
            return true;
          }
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Error checking connection status", e);
    }
    return false;
  }
  
  private void startConnectionMonitoring() {
    // Monitor connection status every 2 seconds
    final android.os.Handler handler = new android.os.Handler();
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        updateConnectionStatus();
        // Auto-close commissioning dialog if connection established
        if (commissioningDialog != null && commissioningDialog.isShowing() 
            && isConnectedToCastingPlayer()) {
          commissioningDialog.dismiss();
          Toast.makeText(getContext(), "Device paired successfully!", Toast.LENGTH_SHORT).show();
        }
        handler.postDelayed(this, 2000);
      }
    }, 2000);
  }

  private void showCommissioningDialog() {
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_commissioning_code, null);
    TextView codeText = dialogView.findViewById(R.id.commissioningCodeText);
    TextView statusText = dialogView.findViewById(R.id.commissioningStatusText);
    
    // Create and show dialog
    commissioningDialog = new android.app.AlertDialog.Builder(getContext())
        .setView(dialogView)
        .setCancelable(true)
        .create();
    
    if (commissioningDialog.getWindow() != null) {
      commissioningDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }
    
    commissioningDialog.show();
    
    // Open commissioning window in background thread
    new Thread(() -> {
      Log.i(TAG, "Opening commissioning window for pairing");
      com.matter.casting.support.MatterError err = 
          ManualCommissioningHelper.openBasicCommissioningWindowWithTimeout(600);
      
      getActivity().runOnUiThread(() -> {
        if (err.hasNoError()) {
          Log.i(TAG, "Commissioning window opened successfully");
          // Get and display the pairing code
          ManualCommissioningHelper.logOnboardingPayload();
          codeText.setText("MT:-24J0AFN00YZ.548G00");
          statusText.setText("Waiting for TV to connect...");
        } else {
          Log.e(TAG, "Failed to open commissioning window: " + err.getErrorMessage());
          statusText.setText("Error: " + err.getErrorMessage());
          statusText.setTextColor(0xFFFF3B30);
        }
      });
    }).start();
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
    view.findViewById(R.id.navUp).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_UP, "Up"); });
    view.findViewById(R.id.navDown).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_DOWN, "Down"); });
    view.findViewById(R.id.navLeft).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_LEFT, "Left"); });
    view.findViewById(R.id.navRight).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_RIGHT, "Right"); });
    view.findViewById(R.id.okButton).setOnClickListener(v -> { hapticFeedback(v, true); sendKey(KEY_SELECT, "OK"); });
    view.findViewById(R.id.homeButton).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_ROOT_MENU, "Home"); });
    view.findViewById(R.id.backButton).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_EXIT, "Back"); });
    view.findViewById(R.id.volumeUpButton).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_VOLUME_UP, "Volume Up"); });
    view.findViewById(R.id.volumeDownButton).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_VOLUME_DOWN, "Volume Down"); });
  }
  
  // ========== APP LAUNCHERS ==========
  
  private void setupAppLaunchers(View view) {
    view.findViewById(R.id.youtubeCard).setOnClickListener(v -> { hapticFeedback(v, false); launchApp("YouTube"); });
    view.findViewById(R.id.netflixCard).setOnClickListener(v -> { hapticFeedback(v, false); launchApp("Netflix"); });
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
    view.findViewById(R.id.key0).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_0, "0"); });
    view.findViewById(R.id.key1).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_1, "1"); });
    view.findViewById(R.id.key2).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_2, "2"); });
    view.findViewById(R.id.key3).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_3, "3"); });
    view.findViewById(R.id.key4).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_4, "4"); });
    view.findViewById(R.id.key5).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_5, "5"); });
    view.findViewById(R.id.key6).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_6, "6"); });
    view.findViewById(R.id.key7).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_7, "7"); });
    view.findViewById(R.id.key8).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_8, "8"); });
    view.findViewById(R.id.key9).setOnClickListener(v -> { hapticFeedback(v, false); sendKey(KEY_NUMBER_9, "9"); });
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
    if (commissioningDialog != null && commissioningDialog.isShowing()) {
      commissioningDialog.dismiss();
    }
    // Stop foreground service if fragment is destroyed
    try {
      Intent serviceIntent = new Intent(getContext(), MatterKeepAliveService.class);
      getContext().stopService(serviceIntent);
      Log.i(TAG, "Stopped MatterKeepAliveService");
    } catch (Exception e) {
      Log.e(TAG, "Error stopping service: " + e.getMessage());
    }
  }
  
  // Native methods
  private native boolean sendKeyToDevice(int keyCode);
  private native boolean launchAppNative(int catalogVendorId, String applicationId);
}
