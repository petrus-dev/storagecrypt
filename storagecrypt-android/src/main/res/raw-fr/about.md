# StorageCrypt #
## À propos ##

### Description ###

StorageCrypt vous permet de chiffrer vos fichiers avant de les sauvegarder dans le cloud.

  * Open source : [https://github.com/petrus-dev/storagecrypt](https://github.com/petrus-dev/storagecrypt) et [https://github.com/petrus-dev/filepickerlib](https://github.com/petrus-dev/filepickerlib)
  * Compatible avec android 4.4+
  * Chiffrement fort : AES 256
  * Les clés de chiffrement sont stockées dans un keystore protégé par mot de passe
  * Vous seul pouvez déchiffrer vos fichiers, à moins de partager vos clés
  * Les fichiers chiffré sont stockés localement, avec possibilité de synchronisation sur Google Drive, Dropbox, Box, HubiC ou OneDrive
  * Vous pouvez utiliser différentes clés pour chiffrer différents fichiers
  * Importez/exportez vos clés pour pouvoir déchiffrer vos fichiers sur plusieurs appareil
  
### Version ###

* 0.13.0

### Changelog ###

* 0.13.0
  * Correction de bug : duplication de fichier lors de la synchronisation de compte en même temps qu'un upload.
  * Correction de bug : lors d'un changement de la liste de documents alors que le menu contextuel est affiché, l'action était lancée sur le mauvais document.
  * Ajout d'un compte : barre de progression avec valeur indéterminée.

* 0.12.0
  * Fin des tests de la beta : publication officielle de l'application pour tout le monde. Pas encore en 1.0 mais on s'en approche.
  * Ajout d'un dialogue pour créer une première clé ou en importer une après la création d'un keystore pour laisser le choix à l'utilisateur de créer une clé ou d'en importer une.

* 0.11.2
  * HubiC : nouvelle méthode pour mettre à jour le quota

* 0.11.1 :
  * Correction de bug : les dossiers n'étaient pas bien recréés lors d'un envoi après avoir supprimé les dossiers distants.

* 0.11.0 :
  * Correction du crash au démarrage des services de chiffrement et déchiffrement
  * Suppression de l'apparition aléatoire de l'icone de synchronisation dans la liste de documents

* 0.10.0 :
  * Correction de divers bugs
  * Restructuration du code pour une meilleure stabilité
  * Amélioration du chiffrement, incompatible avec les versions précédentes