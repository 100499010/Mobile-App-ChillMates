# ChillMates

An Android-based roommate management mobile application developed as a university project for an Android Applications course.

The app allows users to create an account, create or join a shared flat, manage shared shopping lists, track and split expenses, organize events, manage household chores, submit repair requests, and manage their flat and profile information. It also includes nearby supermarket selection and mapping integrated via the Google Maps API, reminders and notifications, and AI-assisted repair message generation using the Gemini API.

## Features

### User Authentication
- User registration, login and logout
- Email and password authentication
- Account profile creation with display name
- Password validation and password changes
- Persistent authentication state

### Flat Management
- Create a new shared flat
- Join an existing flat using an invite code
- View flat information and roommates
- Share the flat invite code with other users
- Leave the current flat
- Create or join another flat from the user profile

### Home and Events
- Home page with a shared notice board
- Create notices for the flat
- Create upcoming events with date, time, description and location
- RSVP to event polls
- Accept or decline event participation
- Set and cancel event reminders
- Export events to the device calendar

### Shopping List
- Shared shopping list for the flat
- Add products with quantity and required date
- Assign shopping items to selected flatmates
- Mark items as bought
- Hide or display recently bought items
- Filter shopping items by supermarket
- Add supermarkets to the flat
- Search for nearby places using Google Places
- Select supermarkets and display them on a map

### Expenses Tracker
- Add shared expenses
- Select who paid for each expense
- Split expenses between selected flatmates
- Calculate individual balances
- View the total balance for the user
- Attach ticket images to expenses
- View attached expense tickets

### Chores
- Create and assign household chores
- View chores by week
- Mark chores as completed or not completed
- Reassign chores between flatmates
- Reconfigure the current chore distribution
- Delete chores
- Automatic chore rotation between flatmates
- Set reminders for assigned chores
- Receive chore notifications

### Repairs
- Create repair requests for the flat
- Add repair titles and descriptions
- Attach multiple images to repair requests
- Use AI to review and improve repair messages
- Send repair requests through Gmail
- Track repair request status
- View submitted repair requests and attached images

### User Profile and Settings
- View and edit user information
- Change profile information and password
- Change profile photo
- View current flat and flatmates
- Manage flat membership and invitations
- Toggle dark mode
- Toggle application sounds

### Automated Tests
- Unit tests for ViewModels (ViewModelsUnitTest)
- UI/Integration tests using Espresso (AddEventTest, AddExpenseTest, ChoresTest, ShoppingListTest)

## Setup & Configuration

To run this project locally, you will need to set up your own Firebase and API credentials:

1. Clone this repository.
2. Navigate to the `app/` directory.
3. Locate the `google-services-sample.json` file and rename it to `google-services.json`.
4. Open the newly renamed `google-services.json` file and replace the placeholder values with your actual Firebase project credentials.
5. Ensure you also configure your Google Maps API and Gemini API keys in the project as required.

## Technologies Used

- Kotlin
- Android Studio
- Jetpack Compose
- Material 3
- Android Navigation Compose
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- Google Maps Compose
- Google Maps SDK
- Google Places API
- Google Maps Services
- Android DataStore
- Google Generative AI
- JUnit
- Espresso
- MockK
- Gradle
- Kotlin Coroutines

## Project Structure

```text
ChillMates/
│
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
│
├── app/
│   ├── build.gradle.kts
│   ├── google-services-sample.json
│   ├── proguard-rules.pro
│   │
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/es/uc3m/android/chillmates/
│       │   │   ├── MainActivity.kt
│       │   │   ├── NavGraph.kt
│       │   │   ├── WelcomePage.kt
│       │   │   ├── UIHelpers.kt
│       │   │   │
│       │   │   ├── chores/
│       │   │   │   ├── ChoresPage.kt
│       │   │   │   ├── ChoresViewModel.kt
│       │   │   │   └── ChoreReminderReceiver.kt
│       │   │   │
│       │   │   ├── expensestracker/
│       │   │   │   ├── ExpensesTrackerPage.kt
│       │   │   │   ├── AddExpenseScreen.kt
│       │   │   │   └── ExpensesViewModel.kt
│       │   │   │
│       │   │   ├── home/
│       │   │   │   ├── HomePage.kt
│       │   │   │   ├── HomeViewModel.kt
│       │   │   │   ├── AddEventScreen.kt
│       │   │   │   └── HomeReminderReceiver.kt
│       │   │   │
│       │   │   ├── repairs/
│       │   │   │   ├── RepairPage.kt
│       │   │   │   └── RepairViewModel.kt
│       │   │   │
│       │   │   ├── shoppinglist/
│       │   │   │   ├── ShoppingListPage.kt
│       │   │   │   ├── ShoppingListViewModel.kt
│       │   │   │   └── SelectSupermarketScreen.kt
│       │   │   │
│       │   │   ├── user/
│       │   │   │   ├── UserPage.kt
│       │   │   │   └── UserViewModel.kt
│       │   │   │
│       │   │   ├── settings/
│       │   │   │   ├── SettingsPage.kt
│       │   │   │   ├── SettingsViewModel.kt
│       │   │   │   └── SettingsDataStoreHelper.kt
│       │   │   │
│       │   │   ├── repository/
│       │   │   │   └── FlatRepository.kt
│       │   │   │
│       │   │   ├── model/
│       │   │   │   ├── User.kt
│       │   │   │   ├── Flat.kt
│       │   │   │   ├── Expense.kt
│       │   │   │   ├── ShoppingItem.kt
│       │   │   │   ├── chore.kt
│       │   │   │   ├── UpcomingEvent.kt
│       │   │   │   ├── Notice.kt
│       │   │   │   └── Repair.kt
│       │   │   │
│       │   │   ├── viewmodel/
│       │   │   │   └── AuthViewModel.kt
│       │   │   │
│       │   │   └── ui/theme/
│       │   │       ├── Color.kt
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   │
│       │   └── res/
│       │       ├── drawable/
│       │       ├── mipmap-hdpi/
│       │       ├── mipmap-mdpi/
│       │       ├── mipmap-xhdpi/
│       │       ├── mipmap-xxhdpi/
│       │       ├── mipmap-xxxhdpi/
│       │       ├── values/
│       │       ├── values-ca/
│       │       ├── values-es/
│       │       ├── values-v23/
│       │       └── xml/
│       │
│       ├── test/
│       │   └── java/es/uc3m/android/chillmates/
│       │       └── ViewModelsUnitTest.kt
│       │
│       └── androidTest/
│           └── java/es/uc3m/android/chillmates/
│               ├── AddEventTest.kt
│               ├── AddExpenseTest.kt
│               ├── ChoresTest.kt
│               └── ShoppingListTest.kt
│
└── gradle/
    ├── libs.versions.toml
    └── wrapper/
