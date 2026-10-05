# HostelFix - Home/Dashboard UI (Jetpack Compose)

This repository now contains an initial Jetpack Compose Home/Dashboard UI scaffold for the HostelFix Android app.

What I added
- app/src/main/java/com/example/hostelfix/MainActivity.kt — Main activity wiring the Compose content
- app/src/main/java/com/example/hostelfix/ui/home/HomeScreen.kt — Dashboard UI (cards + list) and sample data
- app/src/main/java/com/example/hostelfix/ui/theme/Theme.kt — Material theme wrapper
- app/src/main/java/com/example/hostelfix/ui/theme/Color.kt
- app/src/main/java/com/example/hostelfix/ui/theme/Type.kt
- app/src/main/java/com/example/hostelfix/ui/theme/Shape.kt
- app/src/main/AndroidManifest.xml
- README.md — how to open/use these files and next steps
- .gitignore

Notes
- I added only UI Kotlin files (Compose) and a minimal AndroidManifest; I did NOT change Gradle files. You can either create a new Android Studio project and copy these files into the app module (package com.example.hostelfix) or I can add full Gradle configuration on request.
- The Compose code targets modern Compose APIs — make sure your project uses a Compose-compatible AGP/Kotlin/Compose versions (Android Studio Electric Eel / Giraffe or newer recommended).

Next steps I can take for you
- Add Gradle files and project scaffold so you can open directly in Android Studio (I can do that now if you want).
- Implement Report / Details screens and Room persistence.
- Integrate AI chatbot screen later.

If you'd like me to push a full Android Studio project (Gradle files + settings), tell me and I will add them.
