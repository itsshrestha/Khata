# Khata — Native Android Shop Credit & Ledger Manager

**Khata** is an offline-first native Android application built with **Kotlin**, **Jetpack Compose**, **Room SQLite**, and **Supabase Cloud Sync**. It helps shopkeepers track customer credit sales, payments, outstanding balances, and detailed itemized receipts across multiple devices.

---

## Key Features

- **Offline-First Architecture**: All customer data, credit entries, and payment receipts are saved locally in Room SQLite first. The app is 100% functional without internet connectivity.
- **Detailed Itemized Receipts**: Add detailed credit entries with items, quantities, unit rates (e.g. 5 packets @ Rs. 100/packet), and totals.
- **Interactive Transaction Breakdown**: Tap any credit or payment entry in the statement or global transactions log to view:
  - Full itemized breakdown table (Item Name, Qty, Rate, Total).
  - Exact creation timestamp (e.g., `Oct 04, 2026 at 09:39 PM`).
  - Added by Device Name (e.g., `Xiaomi Redmi Note 10`).
  - Payment method (Cash, Fonepay, Cheque, Bank Transfer, QR).
  - Description / notes.
- **Supabase Cloud Synchronization**:
  - Securely syncs local credit records across multiple Android devices using Supabase PostgreSQL & Auth.
  - Multi-tenant isolation using Row Level Security (RLS) policies (`shop_id = auth.uid()`).
- **Precision Financial Calculations**:
  - Money values are stored as integer minor units (paisa) to guarantee zero floating-point rounding errors.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose + Material 3
- **Database**: Room SQLite (Version 3 with automated migrations)
- **Backend / Cloud Sync**: Supabase Kotlin SDK 3.0.2 (Auth + Postgrest + Realtime)
- **Asynchronous Processing**: Kotlin Coroutines, Flow, WorkManager
- **Build System**: Gradle with Kotlin DSL & KSP

---

## Running the Application (Without Android Studio)

You do **not** need Android Studio installed. Command-line build and deployment scripts are provided in `./scripts/`:

### 1. Build Debug APK
```bash
./scripts/build-apk.sh
```
*Outputs APK at `app/build/outputs/apk/debug/app-debug.apk`.*

### 2. Run & View Live on Connected Android Phone
1. Connect your Android phone to your PC via USB cable.
2. Enable **USB Debugging** on your phone (*Settings -> Developer Options -> USB Debugging*).
3. Execute:
```bash
./scripts/run-on-phone.sh
```
*This automatically checks connected ADB devices, builds the APK if needed, installs/updates the app on your phone, and launches `MainActivity`.*

---

## Cloud Backend Setup

To configure Supabase cloud database tables, authentication providers, and RLS security policies, follow the complete step-by-step guide in [`SUPABASE_SETUP.md`](file:///home/delip/Khata/SUPABASE_SETUP.md).

---

## Running Unit Tests

To run the complete automated test suite:
```bash
./gradlew test
```
