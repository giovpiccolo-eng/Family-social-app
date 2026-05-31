# 🪺 FamilyNest

A private, aspirational social space for one family — parents and kids — to share
**ideas, events, plans, images, and ongoing projects**. It blends a warm, closed
social feed with a light task-manager: events, plans, and projects carry a status,
a target date, and a checklist, so the family can dream things up *and* follow
through together.

> Built with **Kotlin + Jetpack Compose (Material 3)**, **MVVM**, and **Firebase**
> (Auth · Cloud Firestore · Storage). Google Sign-In, real-time sync across phones.

---

## ✨ What it does

- **Google sign-in** — everyone signs in with their Google account.
- **Create or join a family** — a parent creates the "nest" and gets a 6-character
  invite code; kids join with that code.
- **Shared feed** — five kinds of posts:
  - 💡 **Idea** — aspirational, free-form thoughts (the heart of the app)
  - 📅 **Event** · 🗺️ **Plan** · 🛠️ **Project** — these are *actionable*: they have a
    status (Dreaming → Planned → In progress → Done), an optional target date, and a checklist
  - 🖼️ **Photo** — shared images/memories
- **Plans view** — a task-manager board of every actionable item grouped by status.
- **Calendar view** — every dated item (events, plans, project deadlines) on a
  chronological agenda grouped by month, with past items dimmed.
- **Comments** — a conversation thread on every post.
- **Assign checklist steps** — give any step to a specific family member.
- **Push notifications** — everyone gets pinged when something new is shared (via a
  Cloud Function — see *Push notifications* below).
- **Cheers** — lightweight likes to encourage each other.
- **Family view** — members, roles, and a shareable invite code.

---

## 📲 Get the APK

There are three ways to get an installable APK:

1. **GitHub Actions (easiest):** every push to the default / feature branch runs the
   `Android CI` workflow and uploads a **`FamilyNest-debug-apk`** artifact. Open the run
   under the repo's **Actions** tab and download it.
2. **Build locally:** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
3. **Release build:** `./gradlew assembleRelease` (signed with the debug key for now).

Then copy the APK to an Android phone and open it. You'll need to allow
"Install unknown apps" for your file manager/browser the first time.

> ⚠️ **The APK builds and installs, but sign-in and sync won't work until you connect
> your own Firebase project** (next section). This repo ships with a *placeholder*
> `app/google-services.json` so the project compiles out of the box.

---

## 🔥 Connect Firebase (one-time, ~10 minutes)

To make Google login and cross-device sync actually work, create a free Firebase
project and drop your config into the app.

### 1. Create the project
1. Go to <https://console.firebase.google.com> → **Add project**.
2. Name it (e.g. *FamilyNest*). Analytics is optional.

### 2. Register the Android app
1. In the project, click the **Android** icon → **Add app**.
2. **Package name:** `com.familynest.app` (must match exactly).
3. **Add a SHA-1 fingerprint** (required for Google Sign-In). Get it with:
   ```bash
   # Debug keystore (used by assembleDebug and assembleRelease in this repo):
   keytool -list -v \
     -keystore ~/.android/debug.keystore \
     -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```
   Paste the SHA-1 into the Firebase app settings. (Add the SHA-256 too if you like.)
4. **Download `google-services.json`** and replace the placeholder at
   **`app/google-services.json`** in this repo with it.

### 3. Turn on the services
In the Firebase console:
- **Build → Authentication → Sign-in method →** enable **Google**.
- **Build → Firestore Database →** create a database (start in *test mode* while
  developing, then lock down with rules — see below).
- **Build → Storage →** enable it (for shared photos).

### 4. Rebuild
```bash
./gradlew assembleDebug
```
Install the new APK and sign in. 🎉

> If you build APKs via **GitHub Actions** and want sign-in to work there too, make
> sure the real `google-services.json` is committed (a Firebase config is safe to
> commit) and that the **SHA-1 of the keystore used by CI** is registered in Firebase.
> The default CI build uses the standard Android debug keystore.

---

## 🔔 Push notifications (Cloud Function)

When someone creates a post, every *other* family member's devices get a push. The
app already registers each device's FCM token on the user's profile; the sending
happens in a Cloud Function in [`functions/`](functions/index.js).

To enable it:
```bash
npm install -g firebase-tools
firebase login
firebase use --add            # pick your Firebase project
cd functions && npm install && cd ..
firebase deploy --only functions
```
> Cloud Functions require the Firebase **Blaze (pay-as-you-go)** plan, which has a
> generous free tier — a single family will almost certainly stay within it.
>
> `firebase deploy` also publishes the Firestore/Storage rules below (they're wired
> up in [`firebase.json`](firebase.json)). Run `firebase deploy` to push everything.

On Android 13+ the app asks for notification permission the first time you sign in.

## 🔒 Suggested Firestore security rules

Start in test mode, then tighten to "only members of your own family can read/write":

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() { return request.auth != null; }

    // A user can read/write their own profile.
    match /users/{uid} {
      allow read: if signedIn();
      allow write: if signedIn() && request.auth.uid == uid;
    }

    // Only members of a family can read it; only members can touch its posts.
    match /families/{familyId} {
      allow read: if signedIn();
      allow create: if signedIn();
      allow update: if signedIn() &&
        (request.auth.uid in resource.data.memberIds ||
         request.auth.uid in request.resource.data.memberIds);

      match /posts/{postId} {
        function isMember() {
          return request.auth.uid in
            get(/databases/$(database)/documents/families/$(familyId)).data.memberIds;
        }
        allow read, create, update, delete: if signedIn() && isMember();

        match /comments/{commentId} {
          allow read, create, update, delete: if signedIn() && isMember();
        }
      }
    }
  }
}
```

Matching Storage rules (members-only is harder there; a simple signed-in rule):
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /families/{familyId}/{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 🏗️ Project structure

```
app/src/main/java/com/familynest/app/
├── FamilyNestApp.kt          # Application — initialises the DI Graph
├── MainActivity.kt           # Single activity, hosts Compose
├── di/Graph.kt               # Tiny manual DI (no kapt/KSP)
├── data/
│   ├── model/Models.kt       # AppUser, Family, Post, TaskItem, Comment + enums
│   └── repository/           # AuthRepository, FamilyRepository, PostRepository
├── notifications/            # FCM service + notification channel/helper
└── ui/
    ├── theme/                # Material 3 theme (twilight + gold on cream)
    ├── SessionViewModel.kt   # Decides: SignedOut / NeedsFamily / Ready
    ├── FamilyNestApp.kt      # Root routing + push-token registration
    ├── auth/                 # Login (Google Sign-In)
    ├── family/               # Create/join family + Family (members) screen
    ├── main/                 # MainScaffold (bottom nav) + Feed
    ├── create/               # Create-post flow
    ├── projects/             # Task-manager "Plans" board
    ├── calendar/             # Chronological calendar/agenda of dated items
    ├── detail/               # Post detail (status, checklist, assignees, comments, cheers)
    └── components/           # PostCard, Avatar, chips, helpers

functions/                    # Cloud Function: push on new post
firestore.rules · storage.rules · firebase.json
```

**Data model in Firestore:**
- `users/{uid}` → profile + `familyId` + `role`
- `families/{familyId}` → name, ownerId, `inviteCode`, `memberIds[]`
- `families/{familyId}/posts/{postId}` → the shared items

---

## 🛠️ Tech / versions

| | |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Build | Android Gradle Plugin 8.7.3, Gradle 8.14.3 |
| Min / target SDK | 26 / 34 |
| Backend | Firebase Auth, Cloud Firestore, Storage |
| Images | Coil |

## Roadmap ideas
- Threaded replies on comments
- Deep-link notification taps straight to the relevant post
- Reminders ahead of an event's target date
- Per-member activity / "what's mine to do" view
