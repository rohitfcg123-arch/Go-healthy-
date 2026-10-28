# Go Healthy — Firebase cloud backup setup

Cloud account and backup code is included, but it stays disabled until you connect your own Firebase project. Do not put service-account keys or private server credentials in this repository.

## 1. Create/configure Firebase
1. Open https://console.firebase.google.com/ and create a project.
2. Add a **Web app** in Project settings and copy its Firebase web configuration.
3. In Authentication → Sign-in method, enable **Email/Password**.
4. In Firestore Database, create a database.
5. Replace the placeholder values in `firebase-config.js` with the web app config.
6. Publish the repository's `firestore.rules` in Firestore → Rules.

## 2. Verify
- Open the Go Healthy site and go to Profile.
- Create an account using email and a password of at least 6 characters.
- Log a test cigarette, refresh the page, and verify the record remains.
- Sign in on another device with the same account and verify the data is restored.

## Data model
Backups are stored under `users/{Firebase Auth UID}/private/appData`. Firestore rules restrict access to the signed-in owner. Passwords are handled by Firebase Authentication and are not saved in the app state.

## Smart reminders limitation
Pattern-based reminders use the most common logged smoking hour and browser notifications while the app is active. Reliable notifications while the Android app is fully closed require a native Android scheduling/notification implementation and permission handling; the current web logic does not promise background delivery.
