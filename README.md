# 🌾 Farm Manager App

An offline-first Android farm management application built with Kotlin and Jetpack Compose.

## Features

- **📊 Dashboard** — Live farm snapshot: birds alive, monthly profit/loss, alerts
- **🌱 Crop Manager** — Track fields, activities (fertilizing, spraying, weeding), and harvests
- **🐔 Poultry Manager** — Manage broiler/layer flocks, record mortality, vaccinations, feed, egg counts
- **📦 Inventory** — Track supplies with low-stock alerts and days-remaining calculation
- **💰 Finance** — Income/expense tracking with monthly profit-loss summary
- **📚 Pest & Disease Guide** — 15+ pests and diseases bundled offline, searchable

## 100% Offline — No Internet Required

All data is stored locally using Room Database (SQLite). The app works in areas with zero connectivity.

## Building the APK via GitHub Actions

1. **Fork or push this project to your GitHub repository**
2. Go to the **Actions** tab in your repository
3. Click **"Build Farm App APK"** workflow
4. Click **"Run workflow"** → **"Run workflow"**
5. Wait ~5-10 minutes for the build to complete
6. Download the APK from the **Artifacts** section of the completed workflow run
7. Transfer the APK to your Android device and install it

> **Note:** You may need to enable "Install from unknown sources" in your Android settings to install the APK.

## Tech Stack

| Component | Library |
|-----------|---------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| Dependency Injection | Hilt (Dagger) |
| Background Tasks | WorkManager |
| Image Loading | Coil |
| Navigation | Navigation Compose |
| Data Serialization | Kotlinx Serialization |

## Architecture

**MVVM + Clean Architecture**

```
UI Layer (Compose Screens + ViewModels)
    ↕
Data Layer (Repositories)
    ↕
Local Data Source (Room DAOs)
    ↕
SQLite Database (on-device)
```

## Minimum Requirements

- Android 8.0 (API 26) or higher
- ~50MB storage space

## Project Structure

```
app/src/main/java/com/farmapp/
├── data/
│   ├── local/
│   │   ├── dao/          # All Room DAOs
│   │   ├── entity/       # All database entities
│   │   ├── converters/   # Type converters
│   │   └── FarmDatabase.kt
│   └── repository/       # Repository classes
├── di/                   # Hilt DI modules
├── ui/
│   ├── dashboard/        # Dashboard screen
│   ├── crop/             # Crop manager
│   ├── poultry/          # Poultry manager
│   ├── inventory/        # Inventory tracker
│   ├── finance/          # Finance tracker
│   ├── guide/            # Pest guide
│   ├── navigation/       # NavHost & routes
│   └── theme/            # Colors & theme
├── worker/               # WorkManager workers
├── FarmApplication.kt
└── MainActivity.kt
```

## Adding More Pests to the Guide

Edit `app/src/main/assets/pest_guide.json` and add entries following the existing format:

```json
{
  "id": "unique_pest_id",
  "name": "Pest Name",
  "localName": "Local language name (optional)",
  "affectedCrop": "Crop Name",
  "symptoms": "Describe what the farmer will see...",
  "treatment": "Step-by-step treatment instructions...",
  "prevention": "How to prevent it...",
  "severity": "HIGH | MEDIUM | LOW"
}
```

---

Built for smallholder farmers. Works without electricity (once downloaded). Works without internet. Always.
