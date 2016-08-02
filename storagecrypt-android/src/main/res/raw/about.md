# StorageCrypt #
## About ##

### Description ###

With StorageCrypt, encrypt your files before saving them on the cloud.

  * Open source : [https://github.com/petrus-dev/storagecrypt](https://github.com/petrus-dev/storagecrypt) and [https://github.com/petrus-dev/filepickerlib](https://github.com/petrus-dev/filepickerlib)
  * Compatible with android 4.4+
  * Strong encryption : AES 256
  * Encryption keys are stored in a password protected keystore
  * Only you can decrypt your files, unless you share your keys
  * Encrypted files are stored locally, with optional synchronization on Google Drive, Dropbox, Box, HubiC or OneDrive
  * You can use different keys to encrypt different files
  * Import/export your keys to be able to decrypt your files on multiple devices

### Version ###

* 0.17.1

### Changelog ###

* 0.17.1
  * Corrections on the help page to reflect the GUI changes
  * Progress dialog when unlocking the database

* 0.17.0
  * New way to display the current folder and navigate through parents
  * Better material design style for Android 5.0 and up.
  * Support for Android 7.0 (Nougat)

* 0.16.0
  * Bug fix : disabled some unsupported operations for Android < 5.0
  * New application colors.
  * Redesigned dialogs.
  * Cleaner icons for higher resolutions.
  
* 0.15.0
  * Fixed documents synchronization failures count.

* 0.14.0
  * Simplifications in the way the documents synchronization progress is displayed.
  * Bug fix : when keys list changes while context menu is opened, the wrong item was opened.
  * Bug fix : duplicate file when syncing account when uploading file (v0.13.0 did not fix the whole problem).
  * Progress bar on the new account progress dialog.
  * Multiple files selection enhancement.

* 0.13.0
  * Bug fix : duplicate file when syncing account when uploading file.
  * Bug fix : when list elements change while context menu is opened, the wrong item was opened.
  * Account link : indeterminate value progress bar.

* 0.12.0
  * End of beta tests : official release of the app for everyone. Not yet 1.0 but getting close.
  * First key creation or import dialog after keystore creation, to let the user either create a new key or import one.

* 0.11.2
  * HubiC : new method for quota refresh

* 0.11.1 :
  * Bug fix : folders were not successfully recreated on a push after deleting the remote folders.

* 0.11.0 :
  * Crash fixed when starting encryption and decryption services
  * Shadow sync icon fixed in the documents list
  
* 0.10.0 :
  * Some bugs fixes
  * Code refactoring for stability
  * Better encryption, incompatible with previous versions