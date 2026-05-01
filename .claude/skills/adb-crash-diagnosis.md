# ADB Crash Diagnosis Skill

You are responsible for diagnosing Android app crashes on connected devices for the **Expense Analyst** app.

## Connected Devices

This project regularly has two devices connected simultaneously:
- `adb-R5GL13FPFWM-Abcn3B._adb-tls-connect._tcp` — USB/wireless (Samsung physical device)
- `192.168.100.110:39197` — network ADB

Always check `adb devices` first. Use the physical device ID for crash diagnosis (it's the one the user is holding).

## Step-by-step crash diagnosis

### 1. Clear logcat buffer
```bash
adb -s <device> logcat -c
```

### 2. Launch the app
The package name is `com.expenseanalyst`. Use `monkey` to launch it:
```bash
adb -s <device> shell monkey -p com.expenseanalyst 1
```

### 3. Wait for crash (6–8 seconds), then dump log
```bash
sleep 6
adb -s <device> logcat -d 2>/dev/null | grep -A 80 "FATAL EXCEPTION"
```

### 4. One-liner (clear + launch + wait + dump)
```bash
adb -s <device> logcat -c && \
adb -s <device> shell monkey -p com.expenseanalyst 1 && \
sleep 6 && \
adb -s <device> logcat -d | grep -A 80 "FATAL EXCEPTION"
```

## Reading the crash output

Key fields in the stacktrace:
| Line | What it tells you |
|------|-------------------|
| `FATAL EXCEPTION: main` | UI thread crash (most common — Hilt init, Room validation, Compose) |
| `java.lang.IllegalStateException: Migration didn't properly handle` | Room DB schema mismatch — see `room-migration-gotchas.md` |
| `java.lang.RuntimeException: Unable to start activity` | Activity/ViewModel init failure |
| `Caused by:` | The real root cause — always scroll past the wrapper exception |
| `at com.expenseanalyst...` | First project frame — the line that triggered the crash |

## Installing a fixed APK

```bash
# Install on both devices at once
adb -s adb-R5GL13FPFWM-Abcn3B._adb-tls-connect._tcp install -r <path-to-apk> &
adb -s 192.168.100.110:39197 install -r <path-to-apk>
```

APK path after a debug build: `app/build/outputs/apk/debug/app-debug.apk`

## Verifying the fix

After installing, re-run the launch + logcat sequence. If no `FATAL EXCEPTION` appears in 6 seconds, the crash is resolved.

## Note on `am start` vs `monkey`

`adb shell am start -n com.expenseanalyst/.app.MainActivity` fails with "Error type 3" because the fully-qualified activity class is `com.expenseanalyst.app.MainActivity` but the package is `com.expenseanalyst`. Use `monkey -p com.expenseanalyst 1` instead — it resolves the launcher activity automatically.
