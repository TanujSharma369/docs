package com.matter.casting;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
  private View powerButton;
  private android.widget.ImageView powerIcon;
  private boolean isPowerOn = false; // Track power state
  private android.app.AlertDialog commissioningDialog;
  private android.app.AlertDialog voiceSessionDialog;
  private SpeechRecognizer speechRecognizer;
  private boolean isListening = false;
  private boolean isVoiceSessionActive = false;
  private TextView voiceCommandText;
  private TextView voiceStatusText;
  private TextView commandHistoryText;
  private View pulseAnimation;
  
  // Haptic feedback
  private Vibrator vibrator;
  private Animation pressAnim;
  private Animation releaseAnim;
  private Animation glowAnim;
  
  // Handler for polling connection status
  private final Handler handler = new Handler(Looper.getMainLooper());
  
  // CEC Key Codes
  private static final int KEY_SELECT = 0;
  private static final int KEY_UP = 1;
  private static final int KEY_DOWN = 2;
  private static final int KEY_LEFT = 3;
  private static final int KEY_RIGHT = 4;
  private static final int KEY_ROOT_MENU = 9;
  private static final int KEY_EXIT = 13;
  private static final int KEY_POWER = 64;
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
  public void onStart() {
    super.onStart();
    Log.i(TAG, "onStart() - attempting reconnection");
    
    // Explicitly trigger reconnection to last player if one exists in cache
    // Do this synchronously before polling to ensure it completes
    try {
      com.matter.casting.support.MatterError err = ManualCommissioningHelper.attemptReconnectToLastPlayer();
      if (err.hasError()) {
        Log.w(TAG, "No cached player to reconnect to: " + err.getErrorMessage());
      } else {
        Log.i(TAG, "Reconnection attempt initiated successfully");
      }
    } catch (Exception e) {
      Log.e(TAG, "Exception during reconnection attempt", e);
    }
    
    // Poll for connection status
    handler.postDelayed(
        new Runnable() {
          private int pollCount = 0;
          private final int MAX_POLLS = 120; // Poll for 60 seconds

          @Override
          public void run() {
            boolean connected = isConnectedToCastingPlayer();
            
            if (pollCount % 10 == 0 || connected) {
              // Log every 5 seconds or when connected
              Log.i(TAG, "onStart poll #" + pollCount + ": connected=" + connected);
            }
            
            if (connected) {
              Log.i(TAG, "onStart: Connected to CastingPlayer after " + (pollCount * 500) + "ms");
              updateConnectionStatus();
              // Stop polling once connected
            } else if (pollCount < MAX_POLLS) {
              pollCount++;
              handler.postDelayed(this, 500); // Poll every 500ms
              updateConnectionStatus();
            } else {
              Log.w(TAG, "onStart: Gave up waiting for connection after " + (MAX_POLLS * 500) + "ms");
              updateConnectionStatus();
            }
          }
        },
        500); // Wait 500ms before starting to poll
  }

  @Override
  public void onResume() {
    super.onResume();
    
    // Only proceed if views are initialized
    if (statusIndicator == null || statusLabel == null) {
      Log.w(TAG, "onResume() called before views initialized, skipping");
      return;
    }
    
    // Update UI to reflect current connection state
    // CastingApp.start() is already called in MainActivity.onCreate() which handles auto-reconnect
    updateConnectionStatus();
    
    // Poll more aggressively for the first 10 seconds after resume to catch auto-reconnect
    final android.os.Handler reconnectHandler = new android.os.Handler();
    final int[] pollCount = {0};
    reconnectHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Aggressive reconnect poll #" + pollCount[0]);
        updateConnectionStatus();
        pollCount[0]++;
        if (pollCount[0] < 10 && !isConnectedToCastingPlayer()) {
          reconnectHandler.postDelayed(this, 1000); // Check every 1 second for 10 seconds
        } else if (isConnectedToCastingPlayer()) {
          Log.i(TAG, "Connection detected, starting foreground service");
          Intent serviceIntent = new Intent(getContext(), MatterKeepAliveService.class);
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getContext().startForegroundService(serviceIntent);
          } else {
            getContext().startService(serviceIntent);
          }
        }
      }
    }, 1000);
    
    // Start foreground service to keep Matter stack alive if already connected
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
    powerButton = view.findViewById(R.id.powerButton);
    powerIcon = view.findViewById(R.id.powerIcon);
    
    // Setup pair button click with haptics
    pairButton.setOnClickListener(v -> {
      hapticFeedback(v, true);
      showCommissioningDialog();
    });
    
    // Setup power button click with haptics and state toggle
    powerButton.setOnClickListener(v -> {
      hapticFeedback(v, true);
      togglePower();
    });
    
    // Initialize power button color (red = off by default)
    updatePowerButtonState();
  }
  
  /**
   * Toggle power state and send power key command to device
   */
  private void togglePower() {
    if (!ManualCommissioningHelper.hasCommissionedVideoPlayer()) {
      Toast.makeText(getContext(), "Connect device first", Toast.LENGTH_SHORT).show();
      return;
    }
    
    // Toggle power state
    isPowerOn = !isPowerOn;
    
    // Update button appearance
    updatePowerButtonState();
    
    // Send power key command to device
    new Thread(() -> {
      boolean success = sendKeyToDevice(KEY_POWER);
      if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
          if (success) {
            String state = isPowerOn ? "ON" : "OFF";
            Toast.makeText(getContext(), "Power " + state, Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Power toggled to: " + state);
          } else {
            // Revert state on failure
            isPowerOn = !isPowerOn;
            updatePowerButtonState();
            Toast.makeText(getContext(), "Failed to send power command", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to send power command");
          }
        });
      }
    }).start();
  }
  
  /**
   * Update power button icon color based on current power state
   * Green = ON, Red = OFF
   */
  private void updatePowerButtonState() {
    if (powerIcon != null) {
      if (isPowerOn) {
        // Green for power ON
        powerIcon.setColorFilter(0xFF30D158); // Green color
      } else {
        // Red for power OFF
        powerIcon.setColorFilter(0xFFFF3B30); // Red color
      }
    }
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
    // Use ManualCommissioningHelper to check if we have a commissioned player
    // This reliably tracks the commissioned state even after discovery stops
    boolean hasCommissioned = ManualCommissioningHelper.hasCommissionedVideoPlayer();
    Log.d(TAG, "isConnectedToCastingPlayer: hasCommissionedVideoPlayer=" + hasCommissioned);
    return hasCommissioned;
  }
  
  // Counter for heartbeat checks
  private int heartbeatCheckCounter = 0;
  private static final int HEARTBEAT_CHECK_INTERVAL = 5; // Check every 5th monitoring cycle (10 seconds)
  
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
        
        // Perform heartbeat verification every HEARTBEAT_CHECK_INTERVAL cycles
        // This detects when TV removes/decommissions the app
        heartbeatCheckCounter++;
        if (heartbeatCheckCounter >= HEARTBEAT_CHECK_INTERVAL && isConnectedToCastingPlayer()) {
          heartbeatCheckCounter = 0;
          performHeartbeatVerification();
        }
        
        handler.postDelayed(this, 2000);
      }
    }, 2000);
  }
  
  /**
   * Performs a heartbeat verification to detect if the TV has removed this device.
   * Runs on a background thread to avoid blocking the UI.
   */
  private void performHeartbeatVerification() {
    new Thread(() -> {
      Log.i(TAG, "Performing heartbeat verification...");
      
      try {
        boolean isAlive = ManualCommissioningHelper.verifyConnectionAlive();
        
        if (getActivity() != null) {
          getActivity().runOnUiThread(() -> {
            if (!isAlive) {
              Log.w(TAG, "Heartbeat verification FAILED - TV may have removed this device");
              handleDisconnectionDetected();
            } else {
              Log.d(TAG, "Heartbeat verification SUCCESS - connection still alive");
            }
          });
        }
      } catch (Exception e) {
        Log.e(TAG, "Exception during heartbeat verification", e);
        if (getActivity() != null) {
          getActivity().runOnUiThread(() -> handleDisconnectionDetected());
        }
      }
    }).start();
  }
  
  /**
   * Called when we detect the TV has disconnected/removed this device.
   * Clears local state and updates UI.
   */
  private void handleDisconnectionDetected() {
    Log.w(TAG, "Disconnection detected! Clearing commissioned player and updating UI.");
    
    // Clear the cached commissioned player
    try {
      ManualCommissioningHelper.clearCommissionedPlayer();
    } catch (Exception e) {
      Log.e(TAG, "Error clearing commissioned player", e);
    }
    
    // Update UI to show disconnected
    updateConnectionStatus();
    
    // Notify user
    Toast.makeText(getContext(), "TV disconnected - device was removed", Toast.LENGTH_LONG).show();
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
        
        // Auto-restart on certain errors if voice session is active
        if (isVoiceSessionActive && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
          new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isVoiceSessionActive) {
              startListening();
            }
          }, 500);
        } else {
          voiceInputIcon.setAlpha(0.7f);
        }
      }
      
      @Override
      public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
          String command = matches.get(0);
          
          // Update current command display
          if (voiceCommandText != null) {
            voiceCommandText.setText("\"" + command + "\"");
            voiceCommandText.setAlpha(1.0f);
          }
          
          // Add to command history with timestamp
          if (commandHistoryText != null) {
            String currentHistory = commandHistoryText.getText().toString();
            if (currentHistory.contains("Command history will appear here")) {
              currentHistory = "";
            }
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
            String newEntry = timestamp + " - Heard: \"" + command + "\"\n" + currentHistory;
            commandHistoryText.setText(newEntry);
            commandHistoryText.setTextColor(0xFFFFFFFF);
          }
          
          processVoiceCommand(command);
          
          // Reset listening state
          isListening = false;
          
          // Auto-restart listening if voice session is still active
          if (isVoiceSessionActive) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
              if (isVoiceSessionActive && !isListening) {
                if (voiceStatusText != null) {
                  voiceStatusText.setText("Listening...");
                }
                if (voiceCommandText != null) {
                  voiceCommandText.setText("Say next command...");
                  voiceCommandText.setAlpha(0.7f);
                }
                startListening();
              }
            }, 1000);
          }
        } else {
          isListening = false;
        }
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
    
    if (isVoiceSessionActive) {
      stopVoiceSession();
    } else {
      startVoiceSession();
    }
  }
  
  private void startVoiceSession() {
    // Create premium dark-themed voice session dialog
    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
    
    // Create custom dark layout
    android.widget.LinearLayout dialogLayout = new android.widget.LinearLayout(requireContext());
    dialogLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
    dialogLayout.setPadding(50, 50, 50, 50);
    dialogLayout.setBackgroundColor(0xFF1C1C1E);
    dialogLayout.setGravity(android.view.Gravity.CENTER);
    
    // Pulsing animation view (microphone indicator)
    pulseAnimation = new View(requireContext());
    android.widget.LinearLayout.LayoutParams pulseParams = new android.widget.LinearLayout.LayoutParams(80, 80);
    pulseParams.gravity = android.view.Gravity.CENTER;
    pulseParams.bottomMargin = 20;
    pulseAnimation.setLayoutParams(pulseParams);
    pulseAnimation.setBackgroundResource(android.R.drawable.ic_btn_speak_now);
    pulseAnimation.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF0A84FF));
    dialogLayout.addView(pulseAnimation);
    
    // Start pulse animation
    android.view.animation.ScaleAnimation scaleUp = new android.view.animation.ScaleAnimation(
        1.0f, 1.3f, 1.0f, 1.3f,
        android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
        android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
    );
    scaleUp.setDuration(800);
    scaleUp.setRepeatCount(android.view.animation.Animation.INFINITE);
    scaleUp.setRepeatMode(android.view.animation.Animation.REVERSE);
    pulseAnimation.startAnimation(scaleUp);
    
    // Status text ("Listening...")
    voiceStatusText = new TextView(requireContext());
    voiceStatusText.setText("Listening...");
    voiceStatusText.setTextSize(20);
    voiceStatusText.setTextColor(0xFF0A84FF);
    voiceStatusText.setGravity(android.view.Gravity.CENTER);
    voiceStatusText.setPadding(0, 0, 0, 20);
    dialogLayout.addView(voiceStatusText);
    
    // Current command text (larger, prominent)
    voiceCommandText = new TextView(requireContext());
    voiceCommandText.setText("Say a command...");
    voiceCommandText.setTextSize(16);
    voiceCommandText.setTextColor(0xFFFFFFFF);
    voiceCommandText.setGravity(android.view.Gravity.CENTER);
    voiceCommandText.setAlpha(0.7f);
    voiceCommandText.setMinHeight(60);
    dialogLayout.addView(voiceCommandText);
    
    // Separator line
    View separator = new View(requireContext());
    android.widget.LinearLayout.LayoutParams sepParams = new android.widget.LinearLayout.LayoutParams(
        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
    );
    sepParams.topMargin = 30;
    sepParams.bottomMargin = 20;
    separator.setLayoutParams(sepParams);
    separator.setBackgroundColor(0xFF3A3A3C);
    dialogLayout.addView(separator);
    
    // Command history scroll view
    android.widget.ScrollView historyScroll = new android.widget.ScrollView(requireContext());
    android.widget.LinearLayout.LayoutParams scrollParams = new android.widget.LinearLayout.LayoutParams(
        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
        300
    );
    historyScroll.setLayoutParams(scrollParams);
    
    commandHistoryText = new TextView(requireContext());
    commandHistoryText.setText("Command history will appear here...");
    commandHistoryText.setTextSize(12);
    commandHistoryText.setTextColor(0xFF9E9E9E);
    commandHistoryText.setPadding(20, 0, 20, 0);
    historyScroll.addView(commandHistoryText);
    dialogLayout.addView(historyScroll);
    
    // Stop button with premium styling
    androidx.cardview.widget.CardView stopButtonCard = new androidx.cardview.widget.CardView(requireContext());
    android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
    );
    cardParams.topMargin = 30;
    cardParams.gravity = android.view.Gravity.CENTER;
    stopButtonCard.setLayoutParams(cardParams);
    stopButtonCard.setCardBackgroundColor(0xFFFF3B30);
    stopButtonCard.setRadius(20);
    stopButtonCard.setCardElevation(0);
    stopButtonCard.setForeground(requireContext().getDrawable(android.R.drawable.list_selector_background));
    stopButtonCard.setClickable(true);
    stopButtonCard.setFocusable(true);
    stopButtonCard.setOnClickListener(v -> stopVoiceSession());
    
    TextView stopButtonText = new TextView(requireContext());
    stopButtonText.setText("Stop Listening");
    stopButtonText.setTextSize(14);
    stopButtonText.setTextColor(0xFFFFFFFF);
    stopButtonText.setTypeface(null, android.graphics.Typeface.BOLD);
    stopButtonText.setPadding(40, 20, 40, 20);
    stopButtonText.setGravity(android.view.Gravity.CENTER);
    stopButtonCard.addView(stopButtonText);
    dialogLayout.addView(stopButtonCard);
    
    builder.setView(dialogLayout);
    builder.setCancelable(true);
    builder.setOnCancelListener(dialog -> stopVoiceSession());
    
    voiceSessionDialog = builder.create();
    if (voiceSessionDialog.getWindow() != null) {
      voiceSessionDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }
    voiceSessionDialog.show();
    
    isVoiceSessionActive = true;
    startListening();
  }
  
  private void stopVoiceSession() {
    isVoiceSessionActive = false;
    stopListening();
    if (voiceSessionDialog != null && voiceSessionDialog.isShowing()) {
      voiceSessionDialog.dismiss();
      voiceSessionDialog = null;
    }
  }
  
  private void startListening() {
    if (isListening) {
      Log.w(TAG, "Already listening, skipping duplicate startListening call");
      return;
    }
    
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
    
    try {
      speechRecognizer.startListening(intent);
      isListening = true;
      Log.d(TAG, "Started listening");
    } catch (Exception e) {
      Log.e(TAG, "Error starting speech recognition", e);
      isListening = false;
    }
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
    // Power commands
    else if (lowerCommand.contains("power") || lowerCommand.contains("turn on") || lowerCommand.contains("turn off")) {
      togglePower();
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
    stopVoiceSession();
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
