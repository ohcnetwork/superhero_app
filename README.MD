# Superhero
The Superhero app keeps track of the near real-time location of all the volunteers.
The app sends requests for emergency tasks to nearby volunteers with instructions on how to fulfil them.


## Project Setup

All notifications are received in the app through [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging).
Setup a Firebase project, enable Firebase Cloud Messaging and place the google-services.json in root\app.
Server base url can be found in Constants.kt class.
There is an OTP authentication layer as part of onboarding. Omitting OTP authentication layer is possible and can make app's entry point as LoginActivity.




