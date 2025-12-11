# Commissioning Flow Comparison

## Current Android App (UDC Flow) - Why It Doesn't Work for You

```
┌─────────────────┐                              ┌──────────────────┐
│  Android App    │                              │  Commissioner    │
│  (Commissionee) │                              │  (Your Device)   │
└────────┬────────┘                              └────────┬─────────┘
         │                                                │
         │ 1. Start App                                  │
         │────────────────────────────────>              │
         │                                                │
         │ 2. Discover Commissioners (_matterd._udp)     │
         │───────────────────────────────────────────>   │
         │                                                │
         │ 3. Display list of commissioners              │
         │<───────────────────────────────────────────   │
         │                                                │
         │ ❌ PROBLEM: Your commissioner doesn't         │
         │    advertise as a video player                │
         │    (_matterd._udp) so app can't find it       │
         │                                                │
         X ❌ Flow stops here - can't proceed            X
```

## Linux App (Manual Commissioning) - What Works

```
┌─────────────────┐                              ┌──────────────────┐
│   Linux App     │                              │  Commissioner    │
│  (Commissionee) │                              │  (Your Device)   │
└────────┬────────┘                              └────────┬─────────┘
         │                                                │
         │ 1. Start App                                  │
         │────────────────────────────────>              │
         │                                                │
         │ 2. Open Commissioning Window                  │
         │    Advertise on _matterc._udp                 │
         │<───────────────────────────────               │
         │                                                │
         │              3. Discover Commissionable Nodes │
         │   <───────────────────────────────────────────│
         │                                                │
         │              4. Select Linux App              │
         │   <───────────────────────────────────────────│
         │                                                │
         │              5. Initiate Commissioning        │
         │   <───────────────────────────────────────────│
         │                                                │
         │ 6. PASE (Passcode Auth)                       │
         │<──────────────────────────────────────────────>│
         │                                                │
         │ 7. Commissioning Complete                     │
         │<──────────────────────────────────────────────>│
         │                                                │
         │ ✅ Now can receive commands                   │
         │<──────────────────────────────────────────────│
```

## Android App After Fix (Manual Commissioning) - What You'll Get

```
┌─────────────────┐                              ┌──────────────────┐
│  Android App    │                              │  Commissioner    │
│  (Commissionee) │                              │  (Your Device)   │
└────────┬────────┘                              └────────┬─────────┘
         │                                                │
         │ 1. Start App                                  │
         │────────────────────────────────>              │
         │                                                │
         │ 2. Call ManualCommissioningHelper             │
         │    .openBasicCommissioningWindow()            │
         │────────────────────────────────>              │
         │                                                │
         │ 3. Advertise on _matterc._udp                 │
         │<───────────────────────────────               │
         │                                                │
         │              4. Discover Commissionable Nodes │
         │   <───────────────────────────────────────────│
         │                                                │
         │              5. Select Android App            │
         │   <───────────────────────────────────────────│
         │                                                │
         │              6. Initiate Commissioning        │
         │   <───────────────────────────────────────────│
         │                                                │
         │ 7. PASE (Passcode Auth)                       │
         │<──────────────────────────────────────────────>│
         │                                                │
         │ 8. Commissioning Complete                     │
         │<──────────────────────────────────────────────>│
         │                                                │
         │ ✅ Now can receive commands                   │
         │<──────────────────────────────────────────────│
```

## Key Differences

### Discovery Phase

**UDC (Current Android):**
- App discovers commissioners
- App sends UDC request
- Commissioner discovers app
- ❌ Requires commissioner to support UDC

**Manual Commissioning (Linux & Fixed Android):**
- App advertises itself
- Commissioner discovers app
- Commissioner initiates commissioning
- ✅ Works with any Matter commissioner

### mDNS Services

| Service Type | Purpose | Who Advertises | Who Discovers |
|--------------|---------|----------------|---------------|
| `_matterc._udp` | Commissionable nodes | Devices being commissioned | Commissioners |
| `_matter._tcp` | Commissioned devices (operational) | Commissioned devices | Controllers |
| `_matterd._udp` | Video players / commissioners | Commissioners/TVs | Casting apps (UDC) |

**The Problem:**
- Android app looks for `_matterd._udp` (commissioners)
- Your device doesn't advertise as a commissioner
- Android app never advertises itself on `_matterc._udp`

**The Solution:**
- Make Android app advertise on `_matterc._udp`
- Your commissioner discovers it there
- Commissioning proceeds normally

## Network Discovery Timeline

### Before Fix
```
Time →
0s    Android app starts
1s    App searches for _matterd._udp services
2s    ❌ No commissioners found (your device isn't advertising)
3s    App shows empty list
      ❌ Can't proceed
```

### After Fix
```
Time →
0s    Android app starts
1s    App calls openBasicCommissioningWindow()
2s    ✅ App advertises on _matterc._udp
3s    Your commissioner scans network
4s    ✅ Commissioner finds Android app
5s    Commissioner starts commissioning
10s   ✅ Commissioning complete
11s   ✅ Can send commands
```

## Code Comparison

### Linux App (Automatic)
```cpp
// In main():
CastingApp::Start();  // Starts Matter server
                      // Automatically advertises on _matterc._udp
                      // when commissioning window opens
```

### Android App (Before Fix)
```java
// In InitializationExample:
CastingApp.getInstance().start();
// ❌ Doesn't open commissioning window
// ❌ Doesn't advertise on _matterc._udp
// Only prepares for UDC flow
```

### Android App (After Fix)
```java
// In InitializationExample:
CastingApp.getInstance().start();

// ✅ NEW CODE:
ManualCommissioningHelper.openBasicCommissioningWindow();
// ✅ Now advertises on _matterc._udp
// ✅ Works just like Linux app
```

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| **Discovery** | App finds commissioners | Commissioner finds app |
| **mDNS Service** | None (waits for UDC) | `_matterc._udp` |
| **Commissioning** | Requires UDC support | Standard Matter commissioning |
| **Your Commissioner** | ❌ Can't see app | ✅ Can discover and commission |
| **Behavior** | ≠ Linux app | = Linux app |

The fix makes the Android app behave exactly like the Linux example by explicitly opening a commissioning window and advertising itself as a commissionable Matter device.
