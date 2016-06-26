# StorageCrypt #
## About ##

### Description ###

With StorageCrypt, encrypt your files before saving them on the cloud.

  * Open source : [https://github.com/petrus-dev/storagecrypt](https://github.com/petrus-dev/storagecrypt) and [https://github.com/petrus-dev/filepickerlib](https://github.com/petrus-dev/filepickerlib)
  * Java 7 required
  * Strong encryption : AES 256
  * Encryption keys are stored in a password protected keystore
  * Only you can decrypt your files, unless you share your keys
  * Encrypted files are stored locally, with optional synchronization on Google Drive, Dropbox, Box, HubiC or OneDrive
  * You can use different keys to encrypt different files
  * Import/export your keys to be able to decrypt your files on multiple devices

### Version ###

* 0.12.0

### Changelog ###

* 0.12.0
  * End of beta tests : official release of the app for everyone. Not yet 1.0 but getting close.
  * First key creation or import dialog after keystore creation, to let the user either create a new key or import one.

* 0.11.2
  * HubiC : new method for quota refresh

* 0.11.1 :
  * Bug fix : folders were not successfully recreated on a push after deleting the remote folders.

* 0.11.0 :
  * Prepared for release

* 0.10.0 :
  * Some bugs fixes
  * Code refactoring for stability
  * Better encryption, incompatible with previous versions