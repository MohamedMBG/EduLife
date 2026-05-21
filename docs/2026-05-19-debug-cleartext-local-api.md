# Task Audit - Debug Cleartext Local API

## Date
2026-05-19

## Task Summary
Analyzed the Samsung Android 16 logcat and fixed the debug build network security issue blocking local backend HTTP calls.

## Files Created
- app/src/debug/AndroidManifest.xml
- app/src/debug/res/xml/debug_network_security_config.xml
- docs/2026-05-19-debug-cleartext-local-api.md

## Files Modified
- None

## What Was Done
Parsed the exported Android Studio logcat file and identified the real EduLife failure:

```text
java.net.UnknownServiceException: CLEARTEXT communication to 10.0.2.2 not permitted by network security policy
```

Firebase login succeeded, but the Android app could not call `POST http://10.0.2.2:8080/api/v1/auth/sync` because Android blocked cleartext HTTP. Added a debug-only manifest overlay and network security config that permits HTTP only for local development hosts: `10.0.2.2`, `127.0.0.1`, and `localhost`.

## Architecture Compliance
The change stays inside Android debug configuration and does not weaken release security. This supports Sprint 1 identity bridge testing against the local Spring Boot backend while keeping production builds ready for HTTPS-only API usage.

## Code Comments Added
Added an XML comment explaining that cleartext HTTP is allowed only in debug builds for local Spring Boot API development.

## Validation / Testing
Ran:

```text
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Zulu\zulu-21" :app:assembleDebug
```

The debug build completed successfully.

## Risks / Notes
The logcat contains Firebase bearer tokens in OkHttp logging output. Avoid sharing this log publicly. Also, `10.0.2.2` works for the Android emulator. A physical Samsung device usually needs the laptop LAN IP address or a tunnel such as ngrok to reach the backend.
