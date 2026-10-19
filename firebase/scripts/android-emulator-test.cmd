@echo off
rem Runs the core:firebase on-device tests against the local Firebase emulators (demo project).
rem Usage, from the repo's firebase\ folder:
rem   firebase emulators:exec --only auth,firestore --project demo-pbb scripts\android-emulator-test.cmd
rem The connected phone reaches the emulators on this PC through adb reverse.
adb reverse tcp:8080 tcp:8080 || exit /b 1
adb reverse tcp:9099 tcp:9099 || exit /b 1
call ..\gradlew.bat -p .. :core:firebase:connectedDebugAndroidTest --console=plain -q
set RESULT=%ERRORLEVEL%
adb reverse --remove tcp:8080
adb reverse --remove tcp:9099
exit /b %RESULT%
