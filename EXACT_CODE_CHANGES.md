# Exact Code Changes Required

## File to Modify
`tv-casting-app/android/App/app/src/main/java/com/matter/casting/InitializationExample.java`

## Step 1: Add Import Statement

Add this import at the top of the file with the other imports:

```java
import com.matter.casting.ManualCommissioningHelper;
```

## Step 2: Modify the initAndStart Method

Find the `initAndStart` method (around line 93-119) and modify it as shown below.

### Original Code (Lines ~93-119):
```java
  /**
   * @param applicationContext Given android.content.Context, initialize and start the CastingApp
   */
  public static MatterError initAndStart(Context applicationContext) {
    // Create an AppParameters object to pass in global casting parameters to the SDK
    AppParameters appParameters =
        new AppParameters(
            applicationContext,
            new DataProvider<ConfigurationManager>() {
              @Override
              public ConfigurationManager get() {
                return new PreferencesConfigurationManager(
                    applicationContext, "chip.platform.ConfigurationManager");
              }
            },
            rotatingDeviceIdUniqueIdProvider,
            commissionableDataProvider,
            dacProvider);

    // Initialize the SDK using the appParameters and check if it returns successfully
    MatterError err = CastingApp.getInstance().initialize(appParameters);
    if (err.hasError()) {
      Log.e(TAG, "Failed to initialize Matter CastingApp");
      return err;
    }

    err = CastingApp.getInstance().start();
    if (err.hasError()) {
      Log.e(TAG, "Failed to start Matter CastingApp");
      return err;
    }
    return err;
  }
```

### Modified Code (ADD THE NEW LINES):
```java
  /**
   * @param applicationContext Given android.content.Context, initialize and start the CastingApp
   */
  public static MatterError initAndStart(Context applicationContext) {
    // Create an AppParameters object to pass in global casting parameters to the SDK
    AppParameters appParameters =
        new AppParameters(
            applicationContext,
            new DataProvider<ConfigurationManager>() {
              @Override
              public ConfigurationManager get() {
                return new PreferencesConfigurationManager(
                    applicationContext, "chip.platform.ConfigurationManager");
              }
            },
            rotatingDeviceIdUniqueIdProvider,
            commissionableDataProvider,
            dacProvider);

    // Initialize the SDK using the appParameters and check if it returns successfully
    MatterError err = CastingApp.getInstance().initialize(appParameters);
    if (err.hasError()) {
      Log.e(TAG, "Failed to initialize Matter CastingApp");
      return err;
    }

    err = CastingApp.getInstance().start();
    if (err.hasError()) {
      Log.e(TAG, "Failed to start Matter CastingApp");
      return err;
    }
    
    // ============== NEW CODE START ==============
    // Open commissioning window for manual commissioning (similar to Linux app)
    // This makes the app discoverable by Matter commissioners via mDNS (_matterc._udp)
    Log.i(TAG, "Opening commissioning window to enable manual commissioning");
    MatterError commissioningErr = ManualCommissioningHelper.openBasicCommissioningWindow();
    if (commissioningErr.hasError()) {
      Log.e(TAG, "Failed to open commissioning window: " + commissioningErr);
      // We don't return error here as the app can still function
      // The commissioning window just won't be open
    } else {
      Log.i(TAG, "✓ Commissioning window opened successfully");
      Log.i(TAG, "✓ App is now discoverable on network");
      Log.i(TAG, "Setup Passcode: " + commissionableDataProvider.get().getSetupPasscode());
      Log.i(TAG, "Discriminator: " + commissionableDataProvider.get().getDiscriminator());
      Log.i(TAG, "Commissioning window will remain open for 3 minutes (180 seconds)");
    }
    // ============== NEW CODE END ==============
    
    return err;
  }
```

## What This Does

1. **After the app starts**, it immediately opens a commissioning window
2. **Advertises the app** on the network as a commissionable Matter device
3. **Logs commissioning parameters** so you can see them in logcat
4. **Makes the app discoverable** by your custom commissioner

## Alternative: Conditional Opening

If you want to make it optional (controlled by a flag), use this version instead:

```java
    // Open commissioning window for manual commissioning (optional - controlled by flag)
    boolean enableManualCommissioning = true; // Set to false to disable
    if (enableManualCommissioning) {
      Log.i(TAG, "Opening commissioning window to enable manual commissioning");
      MatterError commissioningErr = ManualCommissioningHelper.openBasicCommissioningWindow();
      if (commissioningErr.hasError()) {
        Log.e(TAG, "Failed to open commissioning window: " + commissioningErr);
      } else {
        Log.i(TAG, "✓ Commissioning window opened successfully");
        Log.i(TAG, "Setup Passcode: " + commissionableDataProvider.get().getSetupPasscode());
        Log.i(TAG, "Discriminator: " + commissionableDataProvider.get().getDiscriminator());
      }
    }
```

## Complete File After Changes

The complete import section should look like:
```java
package com.matter.casting;

import android.content.Context;
import android.util.Log;
import chip.platform.ConfigurationManager;
import com.matter.casting.core.CastingApp;
import com.matter.casting.support.AppParameters;
import com.matter.casting.support.CommissionableData;
import com.matter.casting.support.DACProvider;
import com.matter.casting.support.DataProvider;
import com.matter.casting.support.MatterError;
import com.matter.casting.ManualCommissioningHelper;  // ← NEW IMPORT

public class InitializationExample {
  // ... rest of the file
}
```

## Expected Log Output

After making these changes and running the app, you should see in logcat:

```
InitializationExample: Opening commissioning window to enable manual commissioning
ManualCommissioningHelper: openBasicCommissioningWindow() called
ManualCommissioningHelper: Successfully opened commissioning window for 180 seconds
ConfigurationManager: Setup discriminator: 3874
ConfigurationManager: Setup passcode: 20202021
InitializationExample: ✓ Commissioning window opened successfully
InitializationExample: ✓ App is now discoverable on network
InitializationExample: Setup Passcode: 20202021
InitializationExample: Discriminator: 3874
InitializationExample: Commissioning window will remain open for 3 minutes (180 seconds)
```

## Verification

After rebuilding and installing:

1. **Check logs:**
   ```bash
   adb logcat | grep -E "InitializationExample|ManualCommissioning|CommissioningWindow"
   ```

2. **Discover from commissioner:**
   Your commissioner should now see the Android app when scanning for commissionable devices

3. **Commission the device:**
   Use passcode `20202021` and discriminator `3874`

## Troubleshooting

**If you see "Failed to open commissioning window":**
- Make sure CastingApp.start() succeeded
- Check that no commissioning window is already open
- Look for detailed error in logcat

**If commissioner still can't see the app:**
- Verify devices are on the same WiFi network
- Check firewall isn't blocking mDNS/multicast
- Use `avahi-browse -rt _matterc._udp` (Linux) to verify mDNS advertisement
- Try from a different device to rule out network issues

## That's It!

This single change to `InitializationExample.java` is the minimum required modification to enable manual commissioning in the Android app, making it behave like the Linux example.
