# StorageCrypt #
StorageCrypt is an Android and desktop app which provides strong encryption for your files stored on the cloud.

## Description ##
  * Open source
  * Android version : Compatible with android 4.4+
  * Desktop version : Compatible with Java 8 with Unlimited Strength Java Cryptographic Extension (JCE)
  * Strong encryption : AES 256
  * Encryption keys are stored in a password protected keystore
  * Only you can decrypt your files, unless you share your keys
  * Encrypted files are stored locally, with optional synchronization on Google Drive, Dropbox, Box, HubiC or OneDrive
  * You can use different keys to encrypt different files
  * Import/export your keys to be able to decrypt your files on multiple devices

## Credits ##
* Code : Pierre Sagne
* Application icon : Tattman

## Android version ##
You can install the Android version from [Google Play](https://play.google.com/store/apps/details?id=fr.petrus.tools.storagecrypt)

## Desktop version ##
The desktop version is available here : [v0.23.0](https://github.com/petrus-dev/storagecrypt/raw/master/storagecrypt-desktop/distributions/StorageCrypt-0.23.0.zip). It runs on Windows, Linux, and MacOS X, with Java 8.

## Build it ##
The sources are available here, so you can build it yourself if you like.
But please note that in order to synchronize your files on the cloud, it needs a set of API keys, which are not included here.

### API keys ###
1. You will need to get your own from Google, Dropbox, Box, HubiC, or OneDrive if you want to access any of these.
2. Then place these keys in template_keys.json files and rename them keys.json:
* [storagecrypt-android/src/main/res/raw/template_keys.json](storagecrypt-android/src/main/res/raw/template_keys.json)
* [storagecrypt-desktop/src/main/resources/res/json/template_keys.json](storagecrypt-desktop/src/main/resources/res/json/template_keys.json)

### Build the Android version ###
1. Build filepickerlib if you want to build the Android version
2. Get the sources
```bash
git clone https://github.com/petrus-dev/storagecrypt.git
```
3. Import the project in Android Studio
4. Run it on your device from Android Studio, by running the storagecrypt-android run task.

### Build the Desktop version ###
1. Get the sources
```bash
git clone https://github.com/petrus-dev/storagecrypt.git
```
2. Compile the java version
```bash
cd storagecrypt/storagecrypt-android
../gradlew clean archiveZip
```
3. Unzip the created zip file, read the instructions and run it.