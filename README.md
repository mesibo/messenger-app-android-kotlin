## Mesibo Messenger for Android - Kotlin 
Note: 
- This kotlin version is created by converting java code using Android Studio
- If this is the first time you are using kotlin in your project, Android Studio might display a warning that Kotlin is not configured in the project. Configure Kotlin by clicking Configure either in the upper right corner of the editor or in the event log alert that pops up in the lower-right corner. Refer to the official documentation [here](https://developer.android.com/kotlin/add-kotlin#add)

Mesibo Messenger is an open-source app with real-time messaging, voice and video call features. This repo contains the source code for Mesibo Messenger App for Android. The GitHub repository for iOS version is [here](https://github.com/mesibo/messenger-app-ios).

### Features
- One-on-one messaging and Group chat
- High quality voice and video calling
- Video and Voice Conferencing
- Rich messaging (text, picture, video, audio, other files)
- End-to-End Encryption 
- Location sharing
- Message status and typing indicators
- Online status (presence) and real-time profile update
- Push notifications

### Generating OTP for the demo
The messenger App requires a valid phone number and OTP to login. Note that, we do not send OTP for App login. Instead, you can generate OTP for any number from the [mesibo console](https://console.mesibo.com)

Note that, all the users are private to your app/account. The demo app can not see or communicate with users from other apps. Also, the demo app uses contact synchronization to find other users of your app and hence ensure to use the correct phone number and other users are in your phone book. 

### Documentation
The documentation for the messenger is available here - [A fully featured WhatsApp clone using mesibo](https://docs.mesibo.com/tutorials/open-source-whatsapp-clone/)

It describes 
- Compilation Instructions (Trivial though)
- Download [backend source code](https://github.com/mesibo/messenger-app-backend) and host it on your server (the default one is located on the mesibo server)
- Hosting entire real-time messaging and call server in your premise
- Other Customizations and Rebranding

## Downloading the Source Code

### Clone the Repository (Recommended)
If you have git installed, this is a recommended approach as you can quickly sync and stay up to date with the latest version. This is also a preferred way of downloading the code if you decide to contribute to the project. 

To download, open a terminal and issue following commands:

    $ mkdir Messenger
    $ cd Messenger
    $ git clone https://github.com/mesibo/messenger-app-android.git

### Stay Up-to-date
Whatever approach you take to download the code, it is important to stay up-to-date with the latest changes, new features, fixes etc. Ensure to **Star(*)** the project on GitHub to get notified whenever the source code is updated. 

## Build and Run

Before we dive into building and running a fully featured Messenger for Android, ensure that you've read the following.

 - Latest Android Studio Installed
 - An Android Device

Building the code is as simple as:

 1. Launch Android Studio
 2. Open the project from the folder where you have downloaded the code using menu `File -> Open`
 3. Build using menu `Build -> Rebuild Project`
 4. It may take a while to build the project for the first time. 
 5. Once the build is over, run on the device using menu `Run -> Run (app)`
 6. That's it, you should see the welcome screen like below.

Login using your phone number and OTP from the mesibo console. You can even start using the app you've just built to communicate with your family and friends.

## Key SDKs user in this project

These apps use following [Mesibo SDKs](https://mesibo.com).

- Mesibo SDK
- Mesibo Messaging UI Module
- Mesibo Call UI Module

These apps also use following third party libraries/services.

- [Google Maps](https://developers.google.com/maps/documentation/) and [Google Places](https://cloud.google.com/maps-platform/places/) SDKs for Geolocation integration 

# Backend
The backend code is here https://github.com/mesibo/messenger-app-backend

## Documentation & Tutorials

- [Mesibo Documentation](https://docs.mesibo.com) 
- [Mesibo Get Started Guide](https://docs.mesibo.com/tutorials/get-started/)
- Tutorial - [A fully featured WhatsApp clone using mesibo](https://docs.mesibo.com/tutorials/open-source-whatsapp-clone/)

