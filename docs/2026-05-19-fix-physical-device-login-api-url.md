# Task Audit - Fix Physical Device Login API URL

## Date
2026-05-19

## Task Summary
Fixed the Android login failure shown in logcat where Firebase authentication succeeded but backend identity sync failed with `Failed to connect to /10.0.2.2:8080` on a physical Samsung device.

## Files Created
- docs/2026-05-19-fix-physical-device-login-api-url.md

## Files Modified
- app/build.gradle.kts
- app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java
- app/src/debug/res/xml/debug_network_security_config.xml
- local.properties

## What Was Done
The API base URL was moved from a hardcoded Java constant to a Gradle-generated `BuildConfig.API_BASE_URL` value. This lets debug builds target either the Android emulator localhost address or a developer machine LAN address without editing Java source.

The local machine debug URL was set to `http://22.10.66.162:8080/api/v1/` in `local.properties`, because physical Android devices cannot reach a PC backend through emulator-only `10.0.2.2`.

The debug network security config was widened for debug builds so local HTTP backend calls can work during MVP development. Release builds remain unaffected because the debug manifest is the only manifest that references this debug network security file.

OkHttp logging was updated to redact the `Authorization` header so Firebase ID tokens are not printed into logcat when debugging login or backend sync failures.

## Architecture Compliance
The change belongs in Android shared networking infrastructure under `core/network`, because login uses Firebase first and then calls the shared Retrofit backend client for `/api/v1/auth/sync`.

The fix keeps the current pragmatic MVVM Android architecture intact. It does not add new auth flows, bypass Firebase, or change the MVP backend contract.

## Code Comments Added
Comments were added around the generated API URL and token log redaction because both decisions affect debugging and security. The comments explain why the settings exist instead of restating the code.

## Validation / Testing
Ran:

```text
.\gradlew.bat --console=plain "-Dorg.gradle.java.home=C:\Program Files\Zulu\zulu-21" :app:assembleDebug
```

The debug build completed successfully and produced `app/build/outputs/apk/debug/app-debug.apk`.

Verified generated debug `BuildConfig.java` contains:

```text
API_BASE_URL = "http://22.10.66.162:8080/api/v1/"
```

Also checked the PC backend port:

```text
Test-NetConnection 127.0.0.1:8080 -> TcpTestSucceeded: False
```

## Risks / Notes
The app cannot complete `/api/v1/auth/sync` until the Spring Boot backend is running and reachable on port `8080`.

The phone and PC must be on the same reachable network, and Windows Firewall must allow inbound connections to port `8080`. If the network changes, update `edulife.apiBaseUrl` in `local.properties` with the new PC IPv4 address.

The pasted logcat included a Firebase bearer token. Future logs should be safer because the app now redacts `Authorization`, but previously captured logs should not be shared publicly.
