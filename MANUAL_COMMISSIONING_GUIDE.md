# Manual Commissioning Mode - Implementation Guide

## Overview
This guide shows you how to enable manual commissioning mode in the Android TV Casting app, allowing your custom Matter commissioner to discover and commission the app without User Directed Commissioning (UDC).

## What Was Created

### 1. ManualCommissioningHelper.java
**Location**: `tv-casting-app/android/App/app/src/main/java/com/matter/casting/ManualCommissioningHelper.java`

A Java helper class that provides methods to:
- Open a basic commissioning window
- Check if a commissioning window is open
- Close a commissioning window
- Log onboarding payload for debugging

### 2. ManualCommissioningHelper-JNI.cpp
**Location**: `tv-casting-app/android/App/app/src/main/jni/cpp/support/ManualCommissioningHelper-JNI.cpp`

The JNI (Java Native Interface) implementation that bridges Java calls to the Matter SDK's C++ APIs.

### 3. ManualCommissioningFragment.java
**Location**: `tv-casting-app/android/App/app/src/main/java/com/matter/casting/ManualCommissioningFragment.java`

A ready-to-use UI fragment that provides:
- Button to open commissioning window
- Button to close commissioning window
- Button to show onboarding payload
- Display of commissioning parameters (passcode, discriminator)
- Status messages

### 4. fragment_manual_commissioning.xml
**Location**: `tv-casting-app/android/App/app/src/main/res/layout/fragment_manual_commissioning.xml`

The UI layout for the manual commissioning fragment.

## How to Integrate

### Option A: Launch Manual Commissioning Fragment Directly (Recommended)

**Modify MainActivity.java** to launch the Manual Commissioning Fragment instead of DiscoveryExampleFragment:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Log.i(TAG, "ChipCastingSimplified = " + GlobalCastingConstants.ChipCastingSimplified);
    boolean ret =
        GlobalCastingConstants.ChipCastingSimplified
            ? InitializationExample.initAndStart(this.getApplicationContext()).hasNoError()
            : initJni();
    if (!ret) {
      Log.e(TAG, "Failed to initialize Matter TV casting library");
      return;
    }

    Fragment fragment = null;
    if (GlobalCastingConstants.ChipCastingSimplified) {
      // CHANGE THIS LINE to use ManualCommissioningFragment
      fragment = ManualCommissioningFragment.newInstance();
    } else {
      fragment = CommissionerDiscoveryFragment.newInstance(tvCastingApp);
    }
    getSupportFragmentManager()
        .beginTransaction()
        .add(R.id.main_fragment_container, fragment, fragment.getClass().getSimpleName())
        .commit();
}
```

### Option B: Add Button in DiscoveryExampleFragment

Add a button in the discovery screen to navigate to manual commissioning mode:

**1. Update fragment_matter_discovery_example.xml:**
```xml
<Button
    android:id="@+id/manualCommissioningModeButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Manual Commissioning Mode" />
```

**2. Update DiscoveryExampleFragment.java:**
```java
@Override
public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    // ... existing code ...
    
    Button manualCommissioningButton = view.findViewById(R.id.manualCommissioningModeButton);
    manualCommissioningButton.setOnClickListener(v -> {
        Fragment fragment = ManualCommissioningFragment.newInstance();
        getActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.main_fragment_container, fragment, fragment.getClass().getSimpleName())
            .addToBackStack(null)
            .commit();
    });
}
```

### Option C: Programmatically Open Commissioning Window

If you want to automatically open the commissioning window when the app starts (like Linux example):

**Modify InitializationExample.java:**
```java
public static MatterError initAndStart(Context applicationContext) {
    // ... existing initialization code ...
    
    err = CastingApp.getInstance().start();
    if (err.hasError()) {
      Log.e(TAG, "Failed to start Matter CastingApp");
      return err;
    }
    
    // NEW CODE: Automatically open commissioning window for manual commissioning
    Log.i(TAG, "Opening commissioning window for manual commissioning");
    err = ManualCommissioningHelper.openBasicCommissioningWindow();
    if (err.hasError()) {
      Log.e(TAG, "Failed to open commissioning window: " + err);
      return err;
    }
    
    return err;
}
```

## Building the Project

### 1. Add the JNI Source to Build

You need to add the new JNI file to the build system. Update your `CMakeLists.txt` or build configuration to include:

```cmake
cpp/support/ManualCommissioningHelper-JNI.cpp
```

### 2. Rebuild the App

```bash
cd tv-casting-app/android/App
./gradlew clean
./gradlew assembleDebug
```

## Testing Manual Commissioning

### 1. Start the App
Launch the app on your Android device.

### 2. Open Commissioning Window
- If using the UI: Tap "Open Commissioning Window" button
- If using auto-open: The window opens automatically on app start

### 3. Discover from Your Commissioner
On your custom Matter commissioner, scan for commissionable devices:

```bash
# Example with chip-tool (Linux)
chip-tool discover commissionables

# You should see your Android app listed with:
# - Device Name: MatterCastingApp (or similar)
# - Discriminator: 3874 (or your configured value)
# - Vendor ID, Product ID, etc.
```

### 4. Commission the Device
Use your commissioner to commission the discovered device:

```bash
# Example with chip-tool
chip-tool pairing onnetwork <node-id> 20202021

# Where:
# - <node-id> is a number you assign (e.g., 1)
# - 20202021 is the setup passcode from InitializationExample
```

### 5. Send Commands
After commissioning, you can send commands to the app:

```bash
# Example: Read application basic cluster
chip-tool applicationbasic read vendor-id <node-id> 1

# Example: Launch content
chip-tool contentlauncher launch-url "https://www.example.com" "Test Video" <node-id> 1
```

## Debugging

### Check if Commissioning Window is Open

Add this to your code to check status:
```java
boolean isOpen = ManualCommissioningHelper.isCommissioningWindowOpen();
Log.i(TAG, "Commissioning window is open: " + isOpen);
```

### View Onboarding Payload

Use logcat to see the QR code and manual pairing code:
```bash
adb logcat | grep -i "config\|qrcode\|setup"
```

Or tap the "Show Onboarding Payload" button in the UI.

### Verify mDNS Advertisement

From a Linux machine on the same network:
```bash
# Using avahi-browse
avahi-browse -rt _matterc._udp

# Using dns-sd
dns-sd -B _matterc._udp

# You should see your Android app listed
```

### Common Issues

**1. App not discoverable:**
- Ensure commissioning window is open
- Check if devices are on the same network
- Verify multicast is not blocked by firewall
- Check Android network permissions in manifest

**2. Commissioning fails:**
- Verify passcode matches (20202021 by default)
- Check discriminator value (3874 by default)
- Ensure commissioning window hasn't timed out (3 minutes)

**3. Build errors:**
- Make sure JNI file is added to build system
- Clean and rebuild the project
- Check NDK version compatibility

## Key Differences from Linux App

| Aspect | Linux App | Android App (After Fix) |
|--------|-----------|-------------------------|
| Discovery | Auto-advertises on start | Manual open commissioning window |
| mDNS Service | Automatic via Matter SDK | Via Matter SDK + explicit window opening |
| Commissioning Flow | Opens window in main() | Opens window via helper method |
| UI | Command-line based | Fragment-based UI |

## Next Steps

1. **Choose integration option** (A, B, or C above)
2. **Build and test** the app
3. **Verify discovery** using your commissioner
4. **Commission and send commands** to validate functionality

## Support

If you encounter issues:
1. Check logcat for error messages
2. Verify network connectivity
3. Ensure commissioning window is open
4. Check that your commissioner supports Matter commissioning
5. Verify passcode and discriminator match between app and commissioner

## Comparison with Linux Example

The Linux app (`simple-app.cpp`) does this automatically:
```cpp
// Linux app automatically:
CastingApp::Start()  // Starts Matter server
OpenBasicCommissioningWindow()  // Advertises as commissionable
// Now discoverable on _matterc._udp
```

With these changes, the Android app can do the same thing programmatically.
