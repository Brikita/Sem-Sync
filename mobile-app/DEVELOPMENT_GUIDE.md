# Application Architecture & Data Flow

## Firestore Structure

To ensure both Web and Mobile show the same data, use these exact collection paths in your Android queries.

### 1. Users

- **Path:** `users/{uid}`
- **Fields:** `displayName`, `email`, `role`, `university`

### 2. Personal Tasks

- **Path:** `tasks` (Top level collection)
- **Query:** `.whereEqualTo("userId", uid)`
- **Fields:**
  - `title` (String)
  - `completed` (Boolean)
  - `dueDate` (Timestamp)
  - `priority` (String: "low", "medium", "high")

### 3. Timetable

_Note: Depending on how the web app implemented it, it might be a subcollection._

- **Path Example:** `users/{uid}/timetable/` OR `timetables/{uid}`
- **Check the Firebase Console** to confirm where the Web App is saving data.

## Android Best Practices (Speed Mode)

### View Binding

In `build.gradle (Module: app)`:

```groovy
android {
    ...
    buildFeatures {
        viewBinding true
    }
}
```

**Why?** It prevents app crashes from `findViewById` (NullPointerExceptions).

### Navigation Graph

Use the Navigation Component.

- Create 3 Fragments: `HomeFragment`, `TimetableFragment`, `AiFragment`.
- Connect them to a Bottom Navigation View.

### Handling "The Presentation"

Since you are presenting **Tomorrow**:

1. **Login Persistence:** Ensure that if I close the app and reopen it, I don't have to login again. Use `FirebaseAuth.getInstance().currentUser`.
2. **Offline Support:** Enable Firestore offline persistence:
   ```kotlin
   val settings = FirebaseFirestoreSettings.Builder()
       .setPersistenceEnabled(true)
       .build()
   db.firestoreSettings = settings
   ```
   This ensures that if the demo wifi is bad, the app still shows data.

## Troubleshooting Common Errors

**Issue:** "App crashes on launch"

- **Fix:** Check `AndroidManifest.xml`. Ensure the Internet Permission is there:
  `<uses-permission android:name="android.permission.INTERNET" />`

**Issue:** "Data not loading"

- **Fix:** Check Firestore Security Rules. If they are locked, the mobile app (which might not send the same Auth token headers effectively if custom http is used) might be blocked. For demo day, **Authenticated Users** rules should be open.

**Issue:** "Gemini gives 404"

- **Fix:** Ensure you are using the correct Region in the URL (e.g., `us-central1`).
