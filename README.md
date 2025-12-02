# FinWise - Personal Finance Tracker

A comprehensive Android application for tracking personal finances, managing budgets, and analyzing spending patterns.

## Features

### Core Functionality
- **Transaction Management**: Add, edit, and delete income and expense transactions
- **Category Tracking**: Organize transactions by customizable categories
- **Budget Management**: Set and monitor monthly budgets
- **Transaction History**: View and search through all your financial transactions
- **Visual Analytics**: Interactive charts and graphs for expense analysis using MPAndroidChart

### User Management
- **User Authentication**: Secure login and registration system
- **User Profiles**: Personalized user profiles with customizable settings

### Data Management
- **Backup & Restore**: Export and import your financial data
- **Data Migration**: Automated data migration helper for app updates
- **Persistent Storage**: Room database for reliable local data storage

### Notifications & Insights
- **Smart Notifications**: Get notified about important financial events
- **Category Analysis**: Detailed breakdown of expenses by category
- **Monthly Reports**: Visual representation of monthly spending patterns

### Customization
- **Multi-currency Support**: Choose your preferred currency
- **Language Settings**: Customizable app language
- **Theme Support**: Light and dark theme options
- **Calendar Preferences**: Set your preferred first day of the week

## Tech Stack

### Languages & Frameworks
- **Kotlin**: Primary development language
- **XML**: UI layouts and resources

### Architecture & Libraries
- **Room Database**: Local data persistence
  - Version: 2.6.1
  - Room Runtime, KTX extensions, and Compiler
- **Coroutines**: Asynchronous programming
  - kotlinx-coroutines-android: 1.7.3
- **Lifecycle Components**: ViewModel and LiveData
  - lifecycle-viewmodel-ktx: 2.7.0
  - lifecycle-runtime-ktx: 2.7.0

### UI Components
- **Material Design**: Modern UI components
- **ViewBinding**: Type-safe view references
- **RecyclerView**: Efficient list rendering
- **MPAndroidChart**: Data visualization (v3.1.0)

### Utilities
- **Gson**: JSON serialization/deserialization (v2.8.9)
- **AndroidX Core KTX**: Kotlin extensions

## 📋 Requirements

- **Minimum SDK**: Android 5.0 (API level 21)
- **Target SDK**: Android 14 (API level 34)
- **Compile SDK**: Android 15 (API level 35)
- **Java Version**: 11
- **Gradle**: 8.7.3
- **Kotlin**: 1.9.24

## Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- JDK 11 or higher
- Android SDK with API 35 installed

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/nethal17/PersonalFinanceTracker.git
   cd PersonalFinanceTracker
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned repository and select it

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle files
   - Wait for the sync to complete

4. **Add MPAndroidChart Repository**
   - Ensure your `settings.gradle.kts` includes JitPack:
   ```kotlin
   dependencyResolutionManagement {
       repositories {
           google()
           mavenCentral()
           maven { url = uri("https://jitpack.io") }
       }
   }
   ```

5. **Build and Run**
   - Connect an Android device or start an emulator
   - Click the "Run" button or press `Shift + F10`


## Permissions

The app requires the following permissions:
- `POST_NOTIFICATIONS`: For sending financial alerts and reminders

## Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## Author

**Nethal Fernando**
- GitHub: [@nethal17](https://github.com/nethal17)

## Acknowledgments

- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for excellent charting library
- Material Design for UI/UX guidelines
- AndroidX libraries for modern Android development
- Room Database for robust local storage

