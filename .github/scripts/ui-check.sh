#!/usr/bin/env bash
# Opens every screen of the app on an emulator, saves screenshots, records the
# guided demo, and fails if the app crashed at any point.
set -u
APK="$1"
PKG=com.aeromaintenance.ai
OUT=ui-check
mkdir -p "$OUT"

adb wait-for-device
adb shell settings put global window_animation_scale 1
adb install -r -g "$APK" || exit 1
adb logcat -c

shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; }
open_screen() { adb shell am start -S -W -n "$PKG/.MainActivity" "$@" > /dev/null; }
scroll() { adb shell input swipe 540 1900 540 "${1:-700}" 600; }

open_screen --es screen dashboard;                                   shot 01-dashboard 4
scroll;                                                              shot 02-dashboard-scrolled
scroll;                                                              shot 03-dashboard-bottom
open_screen --es screen aircraft;                                    shot 04-aircraft
open_screen --es screen detail --es aircraft AERO-101;               shot 05-aircraft-detail 3
scroll;                                                              shot 06-aircraft-detail-scrolled
open_screen --es screen monitoring --es aircraft AERO-101 --es condition normal;   shot 07-monitoring-normal 4
open_screen --es screen monitoring --es aircraft AERO-101 --es condition abnormal; shot 08-monitoring-abnormal 4
scroll 500;                                                          shot 09-monitoring-charts 3
scroll 500;                                                          shot 10-monitoring-charts-2 3
open_screen --es screen analysis --es aircraft AERO-101 --es condition abnormal --ez run_analysis true
shot 11-ai-running 2
shot 12-ai-result 6
scroll 600;                                                          shot 13-ai-result-2 3
scroll 600;                                                          shot 14-ai-result-3 3
scroll 600;                                                          shot 15-ai-result-4 3
open_screen --es screen analysis --es aircraft AERO-101 --es condition normal --ez run_analysis true
shot 16-ai-normal 8
open_screen --es screen predictive --es aircraft AERO-101 --es condition abnormal --ei tab 0; shot 17-predictive 3
scroll 600;                                                          shot 18-predictive-2 3
open_screen --es screen planning --ei tab 1;                         shot 19-work-orders 3
open_screen --es screen alerts;                                      shot 20-alerts 3
open_screen --es screen reports --es aircraft AERO-101 --es condition abnormal --ez report true; shot 21-report 4
scroll 500;                                                          shot 22-report-2 2
open_screen --es screen insights;                                    shot 23-insights 3
open_screen --es screen faculty;                                     shot 24-faculty 3
open_screen --es screen about;                                       shot 25-about 3

# Guided demo, recorded end to end.
open_screen --es screen dashboard
sleep 2
adb shell screenrecord --bit-rate 6000000 --time-limit 95 /sdcard/demo.mp4 &
REC=$!
sleep 1
adb shell am start -n "$PKG/.MainActivity" --ez demo true > /dev/null
for i in $(seq -w 1 12); do shot "demo-$i" 7; done
wait $REC
adb pull /sdcard/demo.mp4 "$OUT/demo.mp4" || true

adb logcat -d > "$OUT/logcat.txt"
if grep -E "FATAL EXCEPTION|AndroidRuntime: Process: $PKG" "$OUT/logcat.txt"; then
  grep -A40 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -120 > "$OUT/crash.txt"
  echo "App crashed during the UI check"
  exit 1
fi
echo "UI check finished without crashes"
