# Implementation Checklist

## Pre-Implementation

- [ ] Read README.md to understand the solution
- [ ] Read QUICK_FIX.md for fast implementation path
- [ ] Have Android Studio and NDK set up
- [ ] Have Matter SDK integrated in your project
- [ ] Can successfully build the Android app currently

## Step 1: Copy Files (5 minutes)

### Core Files (Required)
- [ ] Copy `ManualCommissioningHelper.java` to:
      `tv-casting-app/android/App/app/src/main/java/com/matter/casting/`

- [ ] Copy `ManualCommissioningHelper-JNI.cpp` to:
      `tv-casting-app/android/App/app/src/main/jni/cpp/support/`

### Optional UI Files (If using UI fragment)
- [ ] Copy `ManualCommissioningFragment.java` to:
      `tv-casting-app/android/App/app/src/main/java/com/matter/casting/`

- [ ] Copy `fragment_manual_commissioning.xml` to:
      `tv-casting-app/android/App/app/src/main/res/layout/`

## Step 2: Update Build Configuration (2 minutes)

- [ ] Open `CMakeLists.txt` or `Android.mk`
- [ ] Add `cpp/support/ManualCommissioningHelper-JNI.cpp` to source files
- [ ] Save the file

## Step 3: Modify Java Code (3 minutes)

- [ ] Open `InitializationExample.java`
- [ ] Add import: `import com.matter.casting.ManualCommissioningHelper;`
- [ ] Find the `initAndStart()` method
- [ ] Add commissioning window code after `CastingApp.getInstance().start()`
- [ ] Refer to EXACT_CODE_CHANGES.md for exact code
- [ ] Save the file

## Step 4: Build Project (5 minutes)

- [ ] Open terminal in project root
- [ ] Run: `./gradlew clean`
- [ ] Run: `./gradlew assembleDebug`
- [ ] Check for build errors (if any, see Troubleshooting section)
- [ ] Build succeeds without errors

## Step 5: Install and Test (10 minutes)

### Installation
- [ ] Connect Android device via USB
- [ ] Enable USB debugging on device
- [ ] Run: `./gradlew installDebug`
- [ ] App installs successfully

### Initial Testing
- [ ] Launch the app
- [ ] Open logcat: `adb logcat | grep -E "InitializationExample|ManualCommissioning"`
- [ ] Verify you see: "Opening commissioning window to enable manual commissioning"
- [ ] Verify you see: "Successfully opened commissioning window for 180 seconds"
- [ ] Verify you see: "Setup Passcode: 20202021"
- [ ] Verify you see: "Discriminator: 3874"

### Network Testing
From a Linux/Mac machine on the same network:
- [ ] Run: `avahi-browse -rt _matterc._udp` or `dns-sd -B _matterc._udp`
- [ ] Verify Android app appears in the list
- [ ] Note the service details (name, discriminator, etc.)

### Commissioner Testing
- [ ] Start your custom Matter commissioner
- [ ] Scan for commissionable devices
- [ ] Verify Android app is discovered
- [ ] Attempt to commission the device
- [ ] Use passcode: 20202021
- [ ] Commission completes successfully
- [ ] Try sending a command to the app
- [ ] Verify app responds to the command

## Post-Implementation

### Documentation
- [ ] Document your specific setup in your own README
- [ ] Note any custom changes you made
- [ ] Save troubleshooting notes for team

### Optional Enhancements
- [ ] Add UI button to manually open commissioning window (if desired)
- [ ] Customize commissioning window timeout
- [ ] Add status indicator in app UI
- [ ] Implement auto-reopen on timeout

## Troubleshooting Checklist

### If Build Fails
- [ ] Check that all files are in correct locations
- [ ] Verify CMakeLists.txt includes new JNI file
- [ ] Clean project and rebuild
- [ ] Check for typos in import statements
- [ ] Verify NDK is properly configured

### If App Crashes on Launch
- [ ] Check logcat for crash stack trace
- [ ] Verify JNI library loads correctly
- [ ] Ensure CastingApp.start() succeeded
- [ ] Check for missing dependencies

### If Commissioner Can't Discover App
- [ ] Verify commissioning window opened (check logs)
- [ ] Ensure devices on same WiFi network
- [ ] Check if multicast is blocked
- [ ] Try `avahi-browse` to verify mDNS
- [ ] Restart app and try again
- [ ] Check firewall settings

### If Commissioning Fails
- [ ] Verify correct passcode (20202021)
- [ ] Verify correct discriminator (3874)
- [ ] Check if commissioning window timed out
- [ ] Reopen commissioning window
- [ ] Check commissioner error logs
- [ ] Try commissioning from different device

## Success Metrics

### You know it's working when:
- [ ] ✅ App starts without crashes
- [ ] ✅ Logs show "Commissioning window opened successfully"
- [ ] ✅ `avahi-browse` shows the app
- [ ] ✅ Commissioner discovers the app
- [ ] ✅ Commissioning completes successfully
- [ ] ✅ Commands sent to app are successful
- [ ] ✅ App responds to commands as expected

## Time Estimates

| Phase | Estimated Time | Your Time |
|-------|----------------|-----------|
| Reading documentation | 15 min | _____ |
| Copying files | 5 min | _____ |
| Updating build config | 2 min | _____ |
| Modifying Java code | 3 min | _____ |
| Building project | 5 min | _____ |
| Testing and verification | 10 min | _____ |
| **Total** | **~40 min** | _____ |

## Additional Resources

Reference these documents as needed:

- [ ] **README.md** - Overview and file structure
- [ ] **QUICK_FIX.md** - Fast implementation guide
- [ ] **EXACT_CODE_CHANGES.md** - Code to copy-paste
- [ ] **MANUAL_COMMISSIONING_GUIDE.md** - Detailed guide
- [ ] **FLOW_COMPARISON.md** - Visual understanding
- [ ] **ANDROID_DISCOVERY_FIX.md** - Technical details
- [ ] **SUMMARY.md** - Executive summary

## Notes Section

Use this space to track your progress and any issues:

```
Date: _______________

Issues encountered:
_________________________________________________________
_________________________________________________________
_________________________________________________________

Solutions applied:
_________________________________________________________
_________________________________________________________
_________________________________________________________

Custom modifications:
_________________________________________________________
_________________________________________________________
_________________________________________________________

Testing results:
_________________________________________________________
_________________________________________________________
_________________________________________________________

Additional notes:
_________________________________________________________
_________________________________________________________
_________________________________________________________
```

## Final Verification

Before marking complete, verify:

- [ ] All checkboxes above are completed
- [ ] App builds without errors
- [ ] App runs without crashes
- [ ] Commissioner can discover app
- [ ] Commissioning succeeds
- [ ] Commands work properly
- [ ] Solution is documented for team
- [ ] Any custom changes are noted

## Completion

- [ ] **Implementation Complete** ✅
- [ ] **Tested and Verified** ✅
- [ ] **Documented** ✅
- [ ] **Ready for Production/Further Development** ✅

---

Congratulations! You've successfully enabled manual commissioning in your Android TV casting app! 🎉

If you have any issues, refer back to the troubleshooting sections in this checklist and the detailed documentation files.
