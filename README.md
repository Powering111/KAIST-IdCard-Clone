# KAIST IdCard Clone
This is an Android application. It is considered to be clone coding of KAIST IdCard.

It's very simple and clean. It registers one service to support NFC.

## Advantages
It works even if the main activity is not running. Actually, main activity is not necessary and only debug purpose, but I just left it. The service starts when OS detects NFC tagging, and be destroyed after its job done. So there is a minimum footprint.

Best thing is that you do not need internet to access your id card any more.

By the way, this application cannot be used to prove your identity, but you can still use it for access card at the entrance.


## Usage
Just build with android studio and that'll work. The minimum SDK is set to Android SDK 35, but it should support lower versions by only configuration change to the `build.gradle.kts`.

In order to use this, you have to know the card key number. It is 13 digit hex string. You can get that information by inspecting original KAIST ID Card app using adb. Use the following command and login to the original KAIST IdCard application. Then, you will see your card key number.

```
$ adb logcat -e "insert into card_info.*"
```

Replace 7th letter to `9`. Input the outcome to the text input in app.

You may disable NFC for the original KAIST IdCard app to avoid conflict. It's in the setting.
