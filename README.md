**AUTOMUTE**

AutoMute is an Android application that automatically manages the phone’s sound profile by enabling and disabling Do Not Disturb (DND) based on scheduled time rules.

The goal of this project is to reduce manual effort in managing silent mode and help users avoid interruptions during important periods such as meetings, classes, or sleep.

---

Features

- Automatically enables Do Not Disturb at scheduled times  
- Automatically disables Do Not Disturb when the schedule ends  
- Uses Android system services for background scheduling  
- Simple and lightweight user interface  
- Runs without requiring user interaction after setup  

---

Tech Stack

- Platform: Android  
- Language: Java  
- Android APIs:
  - AlarmManager (for scheduling tasks)
  - Notification / DND permission APIs
  - BroadcastReceiver  
- IDE: Android Studio  

---

How It Works

1. User sets a start and end time inside the app.
2. The app schedules alarms using AlarmManager.
3. When the alarm triggers, a BroadcastReceiver runs in the background.
4. The app automatically switches the phone to Do Not Disturb mode.
5. At the end time, another alarm disables DND.

---

Setup & Run

1. Clone this repository.
2. Open the project in Android Studio.
3. Grant required permissions (DND access).
4. Build and run the app on a real Android device.

---

Learning Outcomes

- Working with Android system services
- Scheduling background tasks
- Handling permissions securely
- Designing simple automation logic
- Debugging real-device behavior

---

Future Improvements

- Multiple schedules support  
- Location-based automation  
- UI enhancements  
- Battery optimization  
- Cloud backup for schedules  

