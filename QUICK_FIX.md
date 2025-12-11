# Quick Fix: Enable Manual Commissioning in Android App

## Problem
Your Android TV casting app is not discoverable by your custom Matter commissioner because it doesn't open a commissioning window automatically like the Linux example does.

## Solution
Add code to automatically open a commissioning window when the app starts, making it discoverable via mDNS.

## Quick Implementation (Simplest Approach)

### Step 1: Add the ManualCommissioningHelper-JNI.cpp to Your Build

Create file: `tv-casting-app/android/App/app/src/main/jni/cpp/support/ManualCommissioningHelper-JNI.cpp`

Use the ManualCommissioningHelper-JNI.cpp file that was created in this repository.

### Step 2: Update your CMakeLists.txt or Android.mk

Add the new JNI file to your build:
```cmake
# In your CMakeLists.txt, add this to your source files list:
cpp/support/ManualCommissioningHelper-JNI.cpp
```

### Step 3: Add the Java Helper Class

Create file: `tv-casting-app/android/App/app/src/main/java/com/matter/casting/ManualCommissioningHelper.java`

Use the ManualCommissioningHelper.java file from this repository.

### Step 4: Modify InitializationExample.java

Open: `tv-casting-app/android/App/app/src/main/java/com/matter/casting/InitializationExample.java`

Add this import at the top:
```java
import com.matter.casting.ManualCommissioningHelper;
```

Then modify the `initAndStart` method to open commissioning window after starting:

**BEFORE:**
```java
public static MatterError initAndStart(Context applicationContext) {
    // ... existing code ...
    
    err = CastingApp.getInstance().start();
    if (err.hasError()) {
      Log.e(TAG, "Failed to start Matter CastingApp");
      return err;
    }
    return err;
}
```

**AFTER:**
```java
public static MatterError initAndStart(Context applicationContext) {
    // ... existing code ...
    
    err = CastingApp.getInstance().start();
    if (err.hasError()) {
      Log.e(TAG, "Failed to start Matter CastingApp");
      return err;
    }
    
    // NEW: Open commissioning window for manual commissioning (like Linux app)
    Log.i(TAG, "Opening commissioning window to make app discoverable");
    MatterError commissioningErr = ManualCommissioningHelper.openBasicCommissioningWindow();
    if (commissioningErr.hasError()) {
      Log.e(TAG, "Failed to open commissioning window: " + commissioningErr);
      // Note: We don't return error here, as the app can still function
      // The commissioning window just won't be open
    } else {
      Log.i(TAG, "Commissioning window opened successfully - app is now discoverable");
    }
    
    return err;
}
```

### Step 5: Rebuild and Test

```bash
cd tv-casting-app/android/App
./gradlew clean
./gradlew assembleDebug
./gradlew installDebug
```

## Verification

After installing the app:

1. **Check logcat:**
```bash
adb logcat | grep -E "CommissioningWindow|ManualCommissioning"
```

You should see:
```
InitializationExample: Opening commissioning window to make app discoverable
ManualCommissioningHelper: openBasicCommissioningWindow() called
ManualCommissioningHelper: Successfully opened commissioning window for 180 seconds
ConfigurationManager: Setup discriminator: 3874
ConfigurationManager: Setup passcode: 20202021
```

2. **Discover from your commissioner:**
```bash
# Your commissioner should now be able to discover the app
# The app will be visible as a commissionable Matter device
```

3. **Commission the device:**
```bash
# Use your custom commissioner to commission the app
# with passcode: 20202021
# and discriminator: 3874
```

## That's It!

With these changes, your Android app will behave like the Linux example:
- ✅ Automatically opens commissioning window on startup
- ✅ Advertises itself as a commissionable Matter node
- ✅ Discoverable by your custom commissioner
- ✅ Can be commissioned manually without UDC

## Alternative: Use UI Button Instead

If you prefer to manually control when the commissioning window opens (instead of auto-opening), you can:

1. Add a button to your UI
2. Call `ManualCommissioningHelper.openBasicCommissioningWindow()` when clicked
3. Display status to user

See `ManualCommissioningFragment.java` for a complete UI example.

## Troubleshooting

**Build Error: Cannot find ManualCommissioningHelper**
- Make sure you added the JNI cpp file to CMakeLists.txt
- Clean and rebuild: `./gradlew clean assembleDebug`

**App crashes when calling openBasicCommissioningWindow**
- Ensure CastingApp.start() was called first
- Check logcat for native crash logs

**Commissioner can't discover the app**
- Verify commissioning window is open (check logs)
- Ensure devices are on the same WiFi network
- Check if multicast/mDNS is blocked by firewall
- Try from a different device/network

**Commissioning fails**
- Verify passcode (20202021) and discriminator (3874) match
- Check if commissioning window timed out (3 minutes)
- Reopen the window and try again
