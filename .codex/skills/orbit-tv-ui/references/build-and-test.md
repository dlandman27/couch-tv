# Build, install, test

Windows machine. ADB: `C:\Users\dylan\adb\platform-tools\adb.exe`.

## Build

`on-tv/android` has **no Gradle wrapper of its own** — use tv-home's with `-p`:

```powershell
& "C:\Users\dylan\couch-tv\tv-home\android\gradlew.bat" --no-daemon -p "C:\Users\dylan\couch-tv\on-tv\android" assembleRelease
```

- APK: `on-tv/android/app/build/outputs/apk/release/app-release.apk` (~25MB, R8-minified;
  unminified Compose is visibly slower on the TV).
- `local.properties` needs forward slashes (`sdk.dir=C:/Users/dylan/AppData/Local/Android/Sdk`).
- `preBuild` runs `copyWeb`, which needs `remote/dist` built (`cd remote && npm run build`) and
  **deletes + recopies `app/src/main/assets/www`**. Orbit assets go in `assets/orbit/`.
- Unit tests (pure JVM, no device): `... -p ... testReleaseUnitTest`; results in
  `app/build/test-results/testReleaseUnitTest/*.xml` (check `tests=`/`failures=`; a fast
  "BUILD SUCCESSFUL" alone doesn't prove they ran).
- Kotlin gotchas hit before: `AnimatedContent { x -> }` lambda param shadowing a `var` you
  assign; `BoxWithConstraints.maxWidth` read inside a nested Row lambda (capture it first).

## Devices

- **Emulator `emulator-5554`** (AVD `couchtv_tv`, Google TV image, 1920×1080 @ 320dpi — same
  density as the TV). Production image: no `adb root`, release app isn't debuggable (no
  `run-as`), so you can't seed app data — create real state by using the app.
- **Real Fire TV `10.0.0.207:5555`** — Dylan's TV (Fire OS 8.1.8.5, Android 11 / API 30).
  **Install only** (`adb -s 10.0.0.207:5555 install -r <apk>`) and only when asked. Never send
  key events or launch/stop apps there. ADB drops when it sleeps; `adb connect 10.0.0.207:5555`.
- With both connected, always `-s emulator-5554` (or `$env:ANDROID_SERIAL`).

## Emulator loop

```powershell
$adb="C:\Users\dylan\adb\platform-tools\adb.exe"; $s="-s","emulator-5554"
& $adb @s install -r <apk>
& $adb @s shell pm disable-user --user 0 com.google.android.tvlauncher   # once; stops Google TV reclaiming Home
& $adb @s shell am force-stop dev.couchtv.ontv
& $adb @s shell am start -n dev.couchtv.ontv/.HomeAlias
# wait ~35-40s: the emulator is slow to paint and decode the wallpaper
& $adb @s shell input keyevent 20          # 19 up · 20 down · 21 left · 22 right · 23 OK · 4 back
& $adb @s shell input keyevent --longpress 23   # hold OK (app reorder)
& $adb @s shell screencap -p /sdcard/x.png; & $adb @s pull /sdcard/x.png <local>.png
```

- **Never** `adb exec-out screencap -p > file` in PowerShell — it injects a BOM and corrupts the
  PNG. Capture to /sdcard and pull.
- A ~10KB all-black capture = not painted yet; re-grab.
- `NativeHomeActivity` isn't exported — always launch `.HomeAlias`.
- Multiple key codes per `input keyevent` call work for moves, but OK presses fired in a burst
  get dropped — send each OK alone with ~450ms between.
- Type into Orbit search: `scripts/type.ps1 -Text "sci fi movies from the 2010s" [-Clear]`
  (assumes focus on "A"; it routes the D-pad over the key grid).
- First search after launch builds the IPTV index (tens of seconds on the emulator); give it
  time before judging results.
- Build real watch history by opening titles (Movies tab → poster → Play), letting them play,
  fast-forwarding (`input keyevent 90` = +15s each), then Back. Some VOD streams fail to start
  on the emulator — confirm with a screenshot that it's playing.

## Known emulator failures

- Under heavy CPU (e.g. the first 60k-row search index build) `BridgeService` can throw
  `ForegroundServiceDidNotStartInTimeException` and the app dies (black screen). `adb reboot`,
  wait for `sys.boot_completed`, re-disable tvlauncher, relaunch. Worth watching on the real
  Fire TV too.
- `adb` server occasionally dies mid-run: `adb kill-server; adb start-server`.
- `logcat -d -b crash` shows the last crash.

## Fresh install on the TV (permissions to re-grant if needed)

`pm grant dev.couchtv.ontv android.permission.WRITE_SECURE_SETTINGS` and
`appops set dev.couchtv.ontv SYSTEM_ALERT_WINDOW allow` — only with Dylan's go-ahead.
Fire OS blocks `pm enable` of HomeAlias; the app's SetupActivity "Use as home screen" does it.
An accessibility service (`WatchService`) bounces the Home key from Amazon's launcher to Orbit.
