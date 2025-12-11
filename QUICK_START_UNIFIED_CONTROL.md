# Quick Start Guide - Unified Control Interface

## Build and Run

### 1. Build the Android App

```powershell
cd tv-casting-app/android/App
./gradlew assembleDebug
```

### 2. Install on Device/Emulator

```powershell
./gradlew installDebug
```

Or use Android Studio:
- Open: `tv-casting-app/android/App`
- Click: Run ▶️

---

## Testing the Unified Controller

### Step 1: Commission a Device
1. Launch the app
2. Click **"Open Commissioning Window"**
3. Commission from your Matter controller/hub
4. Wait for "✓ Commissioned" status

### Step 2: Access Unified Controller
1. On home screen, click **"📱 ALL-IN-ONE CONTROLLER"**
2. Verify device status shows: **"🟢 Connected: [Device Name]"**

### Step 3: Test Remote Control Tab (Default)
- Press D-pad buttons (Up, Down, Left, Right, Select)
- Test volume buttons (Vol+, Vol-, Mute)
- Try media controls (Play, Pause, Stop)
- Press number buttons
- Click Power button

**Expected**: Toast messages show "✓ [Button Name]" on success

### Step 4: Test App Launcher Tab
1. Swipe/tap to **"📺 Apps"** tab
2. Click **"Launch YouTube"**
3. Wait for confirmation toast
4. Try other apps (Netflix, Prime, Disney+)
5. Test **"Stop"** buttons

**Expected**: 
- Status shows "✓ Launched: [App Name]"
- Toast confirms action

### Step 5: Test Voice Control Tab
1. Swipe/tap to **"🎤 Voice"** tab
2. Grant microphone permission if prompted
3. Click **"🎤 START LISTENING"**
4. Wait for "Listening..." indicator
5. Say a command (e.g., "Launch YouTube")
6. Check "Last command" display

**Expected**:
- Microphone icon animates
- Command is recognized and displayed
- Action is executed
- Toast confirms success

---

## Troubleshooting

### Device Not Connected
**Problem**: Red status "⚪ Not Connected"
**Solution**: 
- Commission device first from home screen
- Check Matter network connectivity
- Restart app if needed

### Voice Control Not Working
**Problem**: "Speech recognition not available"
**Solution**:
- Ensure device has Google Speech Services
- Grant RECORD_AUDIO permission
- Check microphone hardware

### Buttons Not Responding
**Problem**: Commands fail silently
**Solution**:
- Verify device supports KeypadInput cluster
- Check device is online
- Review app logs: `adb logcat | grep VoiceControl`

### Tab Switching Issues
**Problem**: Tabs don't switch
**Solution**:
- Ensure Material Design library is in build.gradle
- Clean and rebuild project
- Check for layout errors

---

## Command Reference

### Voice Commands - Full List

#### Navigation
- "Press left" / "Left"
- "Press right" / "Right"  
- "Press up" / "Up"
- "Press down" / "Down"
- "Press select" / "Select" / "OK" / "Enter"

#### Menu
- "Home" / "Go home" / "Menu"
- "Back" / "Exit"

#### Volume
- "Volume up" / "Louder"
- "Volume down" / "Quieter"
- "Mute"

#### Channels
- "Channel up"
- "Channel down"

#### Media
- "Play"
- "Pause"
- "Stop"
- "Rewind"
- "Fast forward"

#### Apps
- "Launch YouTube" / "Open YouTube"
- "Launch Netflix" / "Open Netflix"
- "Launch Prime" / "Open Amazon"
- "Launch Disney" / "Open Disney"
- "Stop YouTube"
- "Stop Netflix"
- "Close [app name]"

---

## Development Tips

### Adding New Apps
Edit `UnifiedControlFragment.java`:

```java
// In setupAppLauncher() method, add:
view.findViewById(R.id.launchHuluButton).setOnClickListener(v -> 
  launchApplication(0, "Hulu"));
view.findViewById(R.id.stopHuluButton).setOnClickListener(v -> 
  stopApplication(0, "Hulu"));
```

Update layout `fragment_unified_control.xml`:
```xml
<Button
    android:id="@+id/launchHuluButton"
    android:text="▶️ Launch Hulu"/>
<Button
    android:id="@+id/stopHuluButton"
    android:text="⏹ Stop Hulu"/>
```

### Customizing Tab Order
In `fragment_unified_control.xml`, reorder `TabItem` elements:
```xml
<com.google.android.material.tabs.TabLayout>
    <com.google.android.material.tabs.TabItem
        android:text="🎤 Voice"/> <!-- Now first -->
    <com.google.android.material.tabs.TabItem
        android:text="🎮 Remote"/>
    <com.google.android.material.tabs.TabItem
        android:text="📺 Apps"/>
</com.google.android.material.tabs.TabLayout>
```

Update `showSection(0)` in `setupTabs()` to match.

### Adding Custom Voice Commands
Edit `handleKeypadCommand()` in `UnifiedControlFragment.java`:

```java
// Add custom command
else if (command.contains("search")) {
    keyCode = KEY_SEARCH;
    keyName = "Search";
}
```

---

## Performance Optimization

### Memory Management
- Voice recognizer is destroyed in `onDestroy()`
- Listeners are cleaned up automatically
- No memory leaks in tab switching

### Battery Optimization
- Voice recognition stops automatically after command
- No background processes
- Efficient tab visibility handling

### Network Efficiency
- Commands sent only when needed
- Async operations for app launching
- Proper error handling prevents retries

---

## Accessibility Features

✅ **Large touch targets** - Buttons sized for easy tapping
✅ **Color coding** - Different colors for different functions
✅ **Clear labels** - Descriptive button text with emojis
✅ **Voice alternative** - All functions accessible via voice
✅ **Visual feedback** - Toast messages and status indicators
✅ **Scrollable** - Works on small screens

---

## FAQ

**Q: Can I use keyboard instead of voice?**
A: No, but the Remote tab provides all button controls.

**Q: Does it work offline?**
A: Yes, once commissioned. Voice requires internet for speech recognition.

**Q: Can I control multiple devices?**
A: Currently one active device at a time. Switch via commissioning.

**Q: What's the difference from old fragments?**
A: Same functionality, but unified in one screen with tabs.

**Q: Can I remove the legacy buttons?**
A: Yes, edit `fragment_matter_discovery_example.xml` and remove the individual control buttons.

**Q: Does it support custom apps?**
A: Yes, edit the code to add more apps (see Development Tips above).

---

## Support and Logs

### View Logs
```powershell
# All logs
adb logcat | Select-String "UnifiedControl"

# Voice only
adb logcat | Select-String "Voice"

# App launcher only
adb logcat | Select-String "Launching|Stopping"

# Remote control only
adb logcat | Select-String "Sending key"
```

### Common Log Messages

✅ **Success**:
```
Voice: KeypadInput command succeeded
Voice: ApplicationLauncher::LaunchApp succeeded
```

❌ **Errors**:
```
No active target video player found
No endpoint found with KeypadInput cluster
Speech recognition error: No speech match
```

---

## Next Steps

1. ✅ Build and install the app
2. ✅ Commission a Matter device
3. ✅ Test unified controller
4. ✅ Try all three tabs
5. ✅ Test voice commands
6. 🎯 Customize for your needs
7. 🎯 Add your favorite apps
8. 🎯 Share feedback!

---

## Files Modified/Created

### New Files ✨
- `UnifiedControlFragment.java` - Main controller logic
- `fragment_unified_control.xml` - UI layout
- `UNIFIED_CONTROL_IMPLEMENTATION.md` - This guide
- `UNIFIED_CONTROL_UI_GUIDE.md` - Visual reference

### Modified Files 🔧
- `DiscoveryExampleFragment.java` - Added button handler
- `fragment_matter_discovery_example.xml` - Added new button

### Unchanged Files ✔️
- All JNI files (RemoteControl-JNI.cpp, etc.)
- Original fragments still work
- No breaking changes

---

**Ready to test!** 🚀

Run the app and enjoy your unified control experience!
