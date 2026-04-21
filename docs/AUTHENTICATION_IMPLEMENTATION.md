# Authentication Implementation (Android)

This document explains how authentication is implemented in this Android app, in beginner-friendly terms.

## Goal

Implement email OTP authentication with JWT session handling:

1. User enters email
2. App calls `POST /auth/send-otp`
3. User enters OTP
4. App calls `POST /auth/verify-otp`
5. Backend returns JWT token + user object
6. App saves JWT securely
7. App auto-attaches JWT for protected APIs
8. If token is invalid/expired, user is logged out

Base URL:

- `https://sevasetu-zqa6.onrender.com/`

---

## Backend Contract (What Android expects)

From backend docs:

- `POST /auth/send-otp`
  - Request: `{ "email": "user@example.com" }`
  - Success: `{ "message": "OTP sent" }`
- `POST /auth/verify-otp`
  - Request: `{ "email": "user@example.com", "otp": "123456" }`
  - Success: returns:
    - `token` (JWT)
    - `user.id` (required)
    - `user.email`

Android relies primarily on:

- `token`
- `user.id`

---

## Folder Structure Used

Auth code follows your README structure:

- `data/remote/` -> DTOs and Retrofit API interface
- `data/repository/` -> business logic + error handling + token save
- `network/` -> Retrofit/OkHttp setup + interceptor
- `utils/` -> token/session helpers
- `ui/common/` -> ViewModel + UI state for auth flow

---

## Retrofit Basics (What it is)

Retrofit is an HTTP client wrapper that turns Kotlin interfaces into real API calls.

You define endpoints as Kotlin functions, for example:

- `sendOtp(...)`
- `verifyOtp(...)`

Retrofit handles:

- HTTP request creation
- JSON parsing (via Gson converter)
- response mapping to Kotlin data classes

### Where in project

- `app/src/main/java/com/example/sevasetu/data/remote/api/AuthApi.kt`
- `app/src/main/java/com/example/sevasetu/data/remote/dto/AuthDtos.kt`

---

## Data Models (DTOs)

DTOs are simple Kotlin data classes that mirror JSON.

- `SendOtpRequest`
- `SendOtpResponse`
- `VerifyOtpRequest`
- `AuthResponse`
- `UserDto`

Why important:

- Compile-time safety for request/response shape
- Easy parsing of backend JSON

---

## Network Layer (Retrofit + OkHttp)

`NetworkModule` creates the API client with:

- Base URL
- Gson converter
- OkHttp client
- Interceptors

### Where

- `app/src/main/java/com/example/sevasetu/network/NetworkModule.kt`
- `app/src/main/java/com/example/sevasetu/network/ApiService.kt`

---

## Token Storage (Security)

JWT is stored in `EncryptedSharedPreferences`, backed by Android Keystore via `MasterKey`.

This is better than plain SharedPreferences because token data is encrypted at rest.

### Where

- `app/src/main/java/com/example/sevasetu/utils/TokenManager.kt`

Methods:

- `saveToken(token)`
- `getToken()`
- `clear()`

---

## JWT Attachment for Every Request

`AuthInterceptor` runs before each network request.

What it does:

1. Read token from `TokenManager`
2. If token exists, add header:
   - `Authorization: Bearer <token>`
3. Continue request

### Where

- `app/src/main/java/com/example/sevasetu/network/AuthInterceptor.kt`

---

## Handling Unauthorized / Expired Sessions

If server responds `401` or `403`:

- token is cleared
- unauthorized event is emitted

This makes it easy to trigger a logout flow globally.

### Where

- `app/src/main/java/com/example/sevasetu/network/UnauthorizedEventBus.kt`
- logic inside `AuthInterceptor.kt`

---

## Repository Layer (App-side auth logic)

`AuthRepository` is the single place for auth business logic.

Responsibilities:

- Call Retrofit API
- Map/validate responses
- Save token on successful verify
- Parse backend error messages
- Expose session clear/get token helpers

### Where

- `app/src/main/java/com/example/sevasetu/data/repository/AuthRepository.kt`
- `app/src/main/java/com/example/sevasetu/data/repository/AuthContainer.kt`

Why repository pattern helps:

- UI does not depend directly on Retrofit
- Easy to test and evolve
- Cleaner separation of concerns

---

## ViewModel Layer (UI state + actions)

`AuthViewModel` handles user actions and updates `AuthUiState`.

Main actions:

- `onEmailChanged(...)`
- `onOtpChanged(...)`
- `sendOtp()`
- `verifyOtp()`
- `restoreSession()`
- `logout()`

`AuthUiState` tracks:

- loading status
- email/otp values
- otp sent status
- auth success status
- info/error messages

### Where

- `app/src/main/java/com/example/sevasetu/ui/common/AuthViewModel.kt`
- `app/src/main/java/com/example/sevasetu/ui/common/AuthUiState.kt`
- `app/src/main/java/com/example/sevasetu/ui/common/AuthViewModelFactory.kt`

---

## Login Screen Flow (Current behavior)

In `Login.kt`:

- On app start, `restoreSession()` checks token.
- If token exists, user is sent to dashboard.
- Email tab allows OTP send/verify.
- Phone tab currently shows disabled message.

### Where

- `app/src/main/java/com/example/sevasetu/ui/screen/Login/Login.kt`

---

## Manifest and Dependencies

### Manifest

- Internet permission required:
  - `<uses-permission android:name="android.permission.INTERNET" />`

Launcher activity is set to login flow.

### Gradle dependencies used

- Retrofit
- Gson converter
- OkHttp + logging interceptor
- AndroidX Security Crypto
- Lifecycle ViewModel KTX

### Where

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/AndroidManifest.xml`

---

## End-to-End Sequence (Simple)

1. User enters email
2. `AuthViewModel.sendOtp()`
3. `AuthRepository.sendOtp()`
4. `AuthApi.sendOtp()` -> backend sends OTP
5. User enters OTP
6. `AuthViewModel.verifyOtp()`
7. `AuthRepository.verifyOtp()`
8. `AuthApi.verifyOtp()` -> returns JWT
9. Repository saves JWT with `TokenManager`
10. Future API calls include bearer token via `AuthInterceptor`

---

## Common Debug Tips

1. If OTP send fails:
- Check email format
- Check backend logs/email provider config
- Verify base URL and endpoint path

2. If verify fails:
- OTP must be 6 digits
- OTP expires (backend says ~5 minutes)
- Ensure same email used in send + verify

3. If protected routes fail with 401/403:
- Token may be expired/invalid
- Interceptor auto-clears token
- User should login again

4. If app skips login unexpectedly:
- A token already exists in encrypted prefs
- Clear app data or call logout/clear session

---

## Suggested Next Improvements

1. Add a dedicated OTP screen (instead of inline input)
2. Add global unauthorized observer to redirect to Login automatically
3. Add unit tests for `AuthRepository` and `AuthViewModel`
4. Add integration tests using `MockWebServer`
5. Add refresh-token strategy if backend supports it

---

## Quick Reference: Key Files

- `app/src/main/java/com/example/sevasetu/data/remote/api/AuthApi.kt`
- `app/src/main/java/com/example/sevasetu/data/remote/dto/AuthDtos.kt`
- `app/src/main/java/com/example/sevasetu/network/NetworkModule.kt`
- `app/src/main/java/com/example/sevasetu/network/AuthInterceptor.kt`
- `app/src/main/java/com/example/sevasetu/network/UnauthorizedEventBus.kt`
- `app/src/main/java/com/example/sevasetu/utils/TokenManager.kt`
- `app/src/main/java/com/example/sevasetu/data/repository/AuthRepository.kt`
- `app/src/main/java/com/example/sevasetu/ui/common/AuthViewModel.kt`
- `app/src/main/java/com/example/sevasetu/ui/screen/Login/Login.kt`

