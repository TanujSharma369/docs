# Troubleshooting Manual Commissioning

## Issue: Linux TV App Cannot Discover Android Device

The Linux commissioner is sending PBKDF param requests but receiving no response, indicating the Android app either:
1. Doesn't have the commissioning window open
2. Isn't advertising on the network correctly
3. Has network/firewall issues

## Diagnostic Steps

### 1. Verify Commissioning Window is Open

On the Android device, check logcat for commissioning window status:

```bash
adb logcat | grep -i "commission\|window\|manual"
```

Look for logs like:
- `Successfully opened commissioning window`
- `Failed to open commissioning window`
- `ManualCommissioningHelper::openBasicCommissioningWindow`

### 2. Check mDNS Advertisement

On the same network, use `avahi-browse` or `dns-sd` to verify the Android app is advertising:

```bash
# Linux
avahi-browse -r _matterc._udp

# macOS
dns-sd -B _matterc._udp
```

You should see the Android casting app listed. If not, the commissioning window isn't open or advertising failed.

### 3. Verify the Button Was Pressed

In the Android app UI:
1. Open the app
2. Look for the "Manual Commissioning" section at the top of the home screen
3. Click "Open Commissioning Window" button
4. Status should change to "✓ Commissioning window is open"
5. QR code should be displayed: `MT:-24J0AFN00YZ.548G00`

### 4. Check Android Logs During Window Open

```bash
adb logcat -s SVR:* AppServer:* DIS:* DiscoveryExampleFragment:*
```

Expected logs when opening window:
```
I ManualCommissioningHelper: Opening basic commissioning window
I AppServer: Successfully opened commissioning window
```

### 5. Verify Network Connectivity

Both devices must be on the same network and multicast must be enabled:

```bash
# On Linux, check multicast route
netstat -rn | grep 224.0.0.251

# Ping multicast address
ping -c 3 224.0.0.251
```

### 6. Check Firewall Rules

Android device may need mDNS port 5353 open for discovery:

```bash
# Check if mDNS traffic is blocked
adb shell netstat -an | grep 5353
```

## Common Issues & Solutions

### Issue: Button doesn't work / Status stays "Not open"

**Cause:** JNI methods not loaded or commissioning window open failed

**Solution:** Check logcat for errors:
```bash
adb logcat | grep -E "FATAL|UnsatisfiedLinkError|commission"
```

If you see `UnsatisfiedLinkError`, the native library didn't load - rebuild the app.

### Issue: mDNS advertisement not visible

**Cause:** Commissioning window opened but mDNS not advertising

**Solution:** 
1. Check if multicast lock is acquired:
   ```bash
   adb logcat | grep "MulticastLock\|mdns"
   ```
2. Ensure WiFi is connected (not mobile data)
3. Restart the Android app

### Issue: Commissioner sends requests but no response

**Cause:** Network packets not reaching Android app (shown in your logs)

**Solutions:**
1. **Verify same subnet:** Both devices must be on same WiFi network
2. **Check discriminator:** Commissioner must use discriminator `3874` (0x0F22)
3. **Check passcode:** Commissioner must use passcode `20202021`
4. **Verify port:** Android app should listen on UDP port 5540
5. **Check Android app is in foreground:** Background apps may not respond

### Issue: Wrong credentials

Your commissioner needs to use these exact values:
- **Discriminator:** 3874 (or hex 0x0F22)
- **Passcode:** 20202021
- **QR Code:** `MT:-24J0AFN00YZ.548G00`

Verify in your commissioner command:
```bash
# Example commissioner command
chip-tool pairing code <node-id> MT:-24J0AFN00YZ.548G00

# Or with manual parameters
chip-tool pairing code <node-id> 20202021 3874
```

## Debug: Capture Network Traffic

To see if Android is responding at all:

```bash
# On Linux machine running commissioner
sudo tcpdump -i <interface> udp port 5540 -w commissioning.pcap

# Then open commissioning window on Android and try to commission
# Analyze with Wireshark to see if packets reach/leave Android
```

## Expected Flow

1. **Android App Startup**
   - App loads native libraries
   - CastingApp initializes
   - Matter stack starts

2. **User Opens Commissioning Window**
   - Click "Open Commissioning Window" button
   - Native JNI calls `openBasicCommissioningWindow()`
   - Commissioning window opens for 3 minutes
   - mDNS advertises on `_matterc._udp`

3. **Commissioner Discovery**
   - Linux TV app browses `_matterc._udp`
   - Finds Android device
   - Sends PBKDFParamRequest to Android IP:5540

4. **PASE Session Establishment**
   - Android responds with PBKDFParamResponse
   - PASE handshake completes
   - Commissioner sends commissioning commands

5. **Commissioning Complete**
   - Commissioning window closes automatically
   - Device is commissioned

## Quick Check Commands

```bash
# 1. Check if app is running
adb shell ps | grep com.chip.casting

# 2. Check if commissioning window open attempt succeeded
adb logcat -d | grep -i "opened commissioning window"

# 3. Check for network activity on port 5540
adb shell netstat -an | grep 5540

# 4. Verify mDNS multicast lock
adb logcat -d | grep "chipBrowseMulticastLock"

# 5. Check for any errors
adb logcat -d | grep -E "ERROR|FATAL|Exception" | tail -20
```

## Still Not Working?

If none of the above helps, gather full debug logs:

```bash
# Start logging
adb logcat -c  # clear logs
adb logcat > android_full_log.txt &

# In another terminal, start app and open commissioning window
# Then try to commission from Linux

# Stop logging (Ctrl+C) and share android_full_log.txt
```

Also check if the Linux example app works (as a baseline):
```bash
cd ~/Desktop/workspace/matter/connectedhomeip/examples/tv-casting-app/linux
./tv-casting-app
# This should be discoverable by your commissioner
```
