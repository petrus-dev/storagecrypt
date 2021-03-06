# StorageCrypt #
## About ##

### Description ###

With StorageCrypt, encrypt your files before saving them on the cloud.

  * Open source : [https://github.com/petrus-dev/storagecrypt](https://github.com/petrus-dev/storagecrypt) and [https://github.com/petrus-dev/filepickerlib](https://github.com/petrus-dev/filepickerlib)
  * Java 8 required
  * Strong encryption : AES 256
  * Encryption keys are stored in a password protected keystore
  * Only you can decrypt your files, unless you share your keys
  * Encrypted files are stored locally, with optional synchronization on Google Drive, Dropbox, Box, HubiC or OneDrive
  * You can use different keys to encrypt different files
  * Import/export your keys to be able to decrypt your files on multiple devices

### Version ###

* 0.27.0

### Changelog ###

* 0.27.0
  * Fixed the sync order between account and documents
  * Updated library versions

* 0.26.0
  * Updated libraries to latest version
  * When keystore unlock password is wrong, show an error message and ask the password again.

* 0.25.0
  * When the database unlock fails, add an option to reset the database.
  * Box.com : enhanced account synchronization.

* 0.24.0
  * When impossible to renew the token for an account, ask the user to reauthenticate.

* 0.23.0
  * Move files in a background task, and show a progress dialog
  * Minor bug fixes

* 0.22.0
  * Bug fix : when trying to import keys from another keystore, and typing the wrong password, an empty keys selection list was opened, instead of an error message.
  * Bug fix : an error occured when importing documents with a single quote in the name.
  * Documents can now be moved to another folder or even another storage account.
  * Fixed a bug on dialogs which prevented the contents to grow when resizing the window.

* 0.21.3
  * Updated libraries to latest version
  * Added a close button in the results windows

* 0.21.2
  * Cleaner crypto implementation.
  * Bug fix : when downloading, the documents sync button displayed was the deletion one.
  * Enhanced changes sync process.

* 0.21.1
  * Bug fix : remote folders were note correctly created

* 0.21.0
  * When encrypting documents, choose which ones to overwrite

* 0.20.0
  * Stability fixes
  * Application shutdown : graceful tasks shutdown and temporary files cleanup.
  * Documents synchronization : delete remove files locally when remote files are deleted 

* 0.19.0
  * Bug fix : some remote changes were not correctly handled
  * Display actions description when the mouse is over the tool bar icons
  * Bug fix : results list dialog was not displayed if closed then reopened
  * Better results list display

* 0.18.2
  * Bug fixes
  * Improved stability

* 0.18.1
  * Bug Fix : Synchronization was sometimes launched again after canceling
  * Clearer progress dialogs
  * Bug Fix : results report when canceling a task were wrong
  * Bug fix : crash when losing contact with remote storages

* 0.18.0
  * Bug Fix : correctly detect canceling when selecting files or folders
  * Multiple documents selection

* 0.17.4
  * Enhanced style for the images in the help page.

* 0.17.3
  * New material design icons

* 0.17.2
  * Updated all libraries to latest version

* 0.17.1
  * Corrections on the help page to reflect the GUI changes.
  * Synchronization buttons : use real buttons.

* 0.17.0
  * New way to display the current folder and navigate through parents

* 0.16.0
  * Cleaner icons.
  * Enhanced GUI.

* 0.15.0
  * Fixed documents synchronization failures count.

* 0.14.0
  * Simplifications in the way the documents synchronization progress is displayed.
  * Bug fix : duplicate file when syncing account when uploading file (v0.13.0 did not fix the whole problem).

* 0.13.0
  * Bug fix : duplicate file when syncing account when uploading file.
  * Bug fix : when list elements change while context menu is opened, the wrong item was opened.
  * Production log level (was DEBUG).
  * When adding an account and app fails to connect the server, ask the user to check his proxy settings.
  * Default proxy settings set to "Use system proxies".
  * Fixed bad error message when failing to add an account.

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