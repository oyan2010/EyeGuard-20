# EyeGuard 20 — 20-20-20 Eye Strain Protection Plan

EyeGuard 20 helps users prevent digital eye strain through the clinically recommended 20-20-20 rule. After a configurable screen usage period (default 20 minutes), the app initiates a mandatory eye break (default 5 minutes), locking the phone and displaying a high-contrast calming full-screen prompt instructing the user to look away into the distance (~6 meters) or out a window.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification and will govern the implementation:
>
> 1. **Alert Experience**: Subtle gentle chime with vibration at break start and completion.
> 2. **Break Enforcement Tone**: Uncompromising focus on forcing the user away from the mobile screen. The device policy lock triggers, and any screen unlock during the break immediately surfaces the immersive BreakActivity, discouraging mobile interaction while keeping emergency calls accessible.

---

## 1. Overview & Core Concept

- **What It Does**: Monitors active unlocked screen time in a robust Foreground Service (`EyeGuardService`). When the usage threshold is reached, it triggers a device lock and presents an immersive `BreakActivity` with full-screen countdown, rotating eye-relaxation tips, and relaxation guidance.
- **Target Persona**: Professionals, students, and mobile users spending extended hours on their smartphones who experience dry eyes, headaches, or visual fatigue.
- **Key Value**: Enforces healthy ergonomic habits automatically with zero manual timer management, persistent status via chronometer notifications, and safe emergency dialer access.

---

## 2. User Experience & Visual Design

### Aesthetic Direction & Visual Identity
- **Palette**: Nature-inspired, calming eye-friendly tones.
  - Light Theme: Background `#F4F8F7`, Surface `#FFFFFF`, Primary Deep Teal `#1F9D8B`, Secondary Soft Mint `#7BC8A4`, Warning Amber `#F2B84B`, Dark Teal Typography `#1B2B2A`.
  - Dark Theme: Background `#0E1A19`, Surface `#162625`, Primary Mint Teal `#4CC3B0`, Soft Slate Typography `#E6F2F0`.
- **Typography & Elevation**: Rounded M3 shapes (24dp cards, 28dp pill buttons), generous 16–24dp spacing, high-contrast readable timer displays, and edge-to-edge system insets handling.

### Screen Walkthrough

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             USER EXPERIENCE FLOW                            │
└─────────────────────────────────────────────────────────────────────────────┘
  First Launch
      │
      ▼
┌───────────────┐      Device Admin + Perms      ┌───────────────┐
│  Onboarding   │ ─────────────────────────────▶ │   Home View   │
│  (4 Pagers)   │                                │  (Active/Off) │
└───────────────┘                                └───────┬───────┘
                                                         │
                                        Screen Time Reaches Usage Target
                                                         │
                                                         ▼
                                                 ┌───────────────┐
                                                 │ BreakActivity │
                                                 │ (Lock + Tips) │
                                                 └───────┬───────┘
                                                         │
                                       Break Finishes / 5s Hold Skip
                                                         │
                                                         ▼
                                                 ┌───────────────┐
                                                 │ Next Cycle or │
                                                 │ Home Session  │
                                                 └───────────────┘
```

1. **Onboarding Screen (HorizontalPager, 4 steps)**:
   - *Slide 1*: Explanation of the 20-20-20 rule and vision health benefits.
   - *Slide 2*: Device Administrator permission card with one-click "Grant" button and live status checkmark.
   - *Slide 3*: Notification, full-screen intent, and battery optimization permissions.
   - *Slide 4*: Quick selection for default usage (20m) and break (5m) duration with disabled-until-admin "Start Protecting" button.
2. **Home Screen**:
   - Status chip ("Protection Active" / "Disabled") and 220dp animated circular progress ring with countdown and amber color-shift in the final 60 seconds.
   - Master ON/OFF Switch card with haptic feedback.
   - Quick navigation cards ("Usage: 20m", "Break: 5m") and "Today" stats (breaks completed, total rest minutes).
   - "Take a break now" button for immediate manual eye relief.
3. **Break Screen (`BreakActivity`)**:
   - Fullscreen immersive layout (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`).
   - Prominent instruction: "Look away from your screen: focus on an object 6 meters away or look out of a window".
   - MM:SS countdown inside a thin circular ring.
   - Eye-strain tips smoothly cross-fading every 20 seconds.
   - Always-visible Emergency Call button (`ACTION_DIAL`).
   - "Skip break" button requiring a 5-second continuous press-and-hold (hidden when Strict Mode is enabled).
4. **Settings Screen**:
   - Usage time (5–60 min) and Break time (1–15 min) sliders with +/- steppers.
   - Strict Mode switch, Sound chime switch, Vibration toggle, and Start on Boot toggle.
   - Live Permission Diagnostic rows (Admin, Notifications, Battery Optimization, Exact Alarms, Full-Screen Intent) with individual "Fix" action triggers.
   - Danger Zone: "Deactivate Admin & Uninstall" flow.
   - Hidden Developer Test card (revealed via 5 taps on app version) for rapid 10s usage / 15s break testing in emulator.
5. **Tips Screen**:
   - Educational cards covering dry eye prevention, proper 20-20-20 technique, screen distance/lighting tips, and medical disclaimer.
   - Full English and Urdu localization support.

---

## 3. Key Product Decisions & Trade-Offs

- **State Machine in Service vs Activity**:
  - *Chosen Approach*: `EyeGuardService` as an independent Foreground Service that owns timestamps based on `SystemClock.elapsedRealtime()`.
  - *Rationale*: Guarantees that app minimization, configuration changes, or process restarts will not corrupt or reset remaining usage/break time.
- **Break Enforcement Mechanism**:
  - *Chosen Approach*: Call `DevicePolicyManager.lockNow()`, show full-screen `BreakActivity`, and register `ACTION_USER_PRESENT` / `ACTION_SCREEN_ON` receivers. If the user unlocks during an active break, immediately re-surface `BreakActivity` and enforce the break.
  - *Rationale*: Android cannot permanently disable hardware lock pins, but this combination reliably discourages phone usage and refocuses the user's attention away from the device.
- **Notification Updates without Battery Drain**:
  - *Chosen Approach*: Use `setUsesChronometer(true)` with `setChronometerCountDown(true)` and exact base timestamp.
  - *Rationale*: The Android System UI natively ticks every second without waking the CPU or spamming notification updates.
- **Data Persistence**:
  - *Chosen Approach*: Jetpack DataStore Preferences for settings, state, and daily statistics (`breaksToday`, `restMinutesToday`).
  - *Rationale*: Lightweight, asynchronous, robust against process death, perfectly aligned with the requirement.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SYSTEM ARCHITECTURE DIAGRAM                         │
└─────────────────────────────────────────────────────────────────────────────┘

       Broadcast Receivers                       UI Layer (Compose)
   ┌───────────────────────────┐            ┌────────────────────────────┐
   │ ScreenReceiver (ON/OFF)   │            │ MainActivity (NavHost)     │
   │ BootReceiver (BOOT_COMPL) │            │ ├─ OnboardingScreen        │
   │ EyeGuardAdminReceiver     │            │ ├─ HomeScreen              │
   └─────────────┬─────────────┘            │ ├─ SettingsScreen          │
                 │                          │ └─ TipsScreen              │
                 │ Intents                  │ BreakActivity (Immersive)  │
                 ▼                          └─────────────┬──────────────┘
   ┌───────────────────────────┐                          │ StateFlow
   │     EyeGuardService       │                          ▼
   │  (Foreground SpecialUse)  │ ◀──────────────▶ ┌──────────────────────┐
   │  - State: IDLE|USING|BREAK│                  │  MainViewModel       │
   │  - ElapsedRealtime Timers │                  └──────────┬───────────┘
   │  - AlarmManager Backup    │                             │
   └─────────────┬─────────────┘                             │
                 │ Flow / Updates                            │
                 ▼                                           ▼
   ┌─────────────────────────────────────────────────────────────────────┐
   │                     EyeGuardRepository (Singleton)                  │
   │                     + Preferences DataStore                         │
   │ (usageMinutes, breakMinutes, strictMode, sound, vibrate, stats...) │
   └─────────────────────────────────────────────────────────────────────┘
```

### Component & State Responsibility Matrix
- `EyeGuardRepository`: Manages DataStore preferences and current service state `StateFlow<ServiceState>`.
- `EyeGuardService`: Listens for screen state changes, increments stats upon break completion, issues notifications with pause/stop actions, and triggers `BreakActivity`.
- `EyeGuardAdminReceiver`: Handles Device Admin activation and removal callbacks.
- `BreakActivity`: Blocks back navigation during active break, handles 5-second press-and-hold skip indicator, launches dialer on emergency tap, and auto-dismisses upon break completion.

---

## 5. Verification & Testing Strategy

1. **Gradle Build Verification**: Ensure build dependencies are clean (using AndroidX, Compose M3, and DataStore), compile with `compile_applet`.
2. **Developer Test Mode**: Use the 5-tap developer card in Settings to execute the 10s usage and 15s break cycle in the emulator.
3. **Permissions Handling**: Verify graceful fallbacks if Device Admin or Full-Screen Intent is not yet granted.
4. **Localization**: Verify strings in English and Urdu (`values/strings.xml` and `values-ur/strings.xml`).
