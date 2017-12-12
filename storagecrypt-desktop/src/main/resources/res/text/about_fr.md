# StorageCrypt #
## À propos ##

### Description ###

StorageCrypt vous permet de chiffrer vos fichiers avant de les sauvegarder dans le cloud.

  * Open source : [https://github.com/petrus-dev/storagecrypt](https://github.com/petrus-dev/storagecrypt) et [https://github.com/petrus-dev/filepickerlib](https://github.com/petrus-dev/filepickerlib)
  * Java 8 requis
  * Chiffrement fort : AES 256
  * Les clés de chiffrement sont stockées dans un keystore protégé par mot de passe
  * Vous seul pouvez déchiffrer vos fichiers, à moins de partager vos clés
  * Les fichiers chiffrés sont stockés localement, avec possibilité de synchronisation sur Google Drive, Dropbox, Box, HubiC ou OneDrive
  * Vous pouvez utiliser différentes clés pour chiffrer différents fichiers
  * Importez/exportez vos clés pour pouvoir déchiffrer vos fichiers sur plusieurs appareils

### Version ###

* 0.27.0

### Changelog ###

* 0.27.0
  * Correction de l'ordonancement de la synchronisation du compte et des documents
  * Mise à jour des bibliothèques

* 0.26.0
  * Mise à jour des bibliothèques à la dernière version.
  * Lorsque le mot de passe de déverrouillage du magazin de clé est erroné, affichage d'un message d'erreur et réaffichage de la demande de mot de passe.

* 0.25.0
  * Lorsque le déverrouillage de la base de données échoue, ajout d'une option pour la réinitialiser.
  * Box.com : amélioration de la synchronisation du compte.

* 0.24.0
  * Lorsqu'il est impossible de renouveler le token pour un compte, proposer à l'utiliser de se réauthentifier.

* 0.23.0
  * Déplacement des fichiers dans un service en arrière-plan, et affichage d'un dialogue de progression
  * Correction de bugs mineurs

* 0.22.0
  * Correction de bug : lors de l'import de clés depuis un autre keystore, si un mauvais mot de passe était saisi, une liste vide était présentée et non un message d'erreur.
  * Correction de bug : une erreur se produisait lors de l'ajout d'un document comportant une apostrophe dans son nom.
  * Les documents peuvent maintenant être déplacés vers un autre dossier ou même sur un autre compte.
  * Correction d'un bug dans les dialogues qui empéchait le contenu de grandir lors du redimentionnement de la fenêtre.

* 0.21.3
  * Mise à jour des bibliothèques à la dernière version
  * Ajout d'un bouton pour fermer les fenêtres de résultat

* 0.21.2
  * Meilleure implémentation des opérations cryptographiques.
  * Correction de bug : lors du download, l'icone affichée dans le bouton de synchro était celle de la suppression.
  * Amélioration du processus de synchronisation des modifications.

* 0.21.1
  * Correction de bug : les dossiers distants n'étaient pas correctement créés

* 0.21.0
  * Lors du chiffrement, il est maintenant possible de choisir quels fichiers existants écraser.

* 0.20.0
  * Amélioration de la stabilité
  * Fermeture de l'application : arrêt propre des tâches en cours, et suppression des fichiers temporaires.
  * Synchronisation des documents : suppression des fichiers locaux lorsque les fichiers distants sont supprimés 

* 0.19.0
  * Correction de bug : certains changements distants n'étaient pas correctement pris en compte
  * Affichage de la description des actions lorsque la souris passe au dessus des icones de la barre d'outils
  * Correction de bug : le dialogue présentant la liste des résultats n'était pas affiché s'il était fermé puis ouvert à nouveau
  * Meilleure présentation de la liste des résultats

* 0.18.2
  * Correction de bugs
  * Amélioration de la stabilité

* 0.18.1
  * Correction de bug : La synchronisation était parfois relancée après une annulation
  * Dialogues de progression plus clairs
  * Correction de bug : les résultats affichés après une annulation étaient faux
  * Correction de bug : plantages lors de la perte de connexion avec les stockages distants

* 0.18.0
  * Correction de bug : l'annulation lors du choix de fichiers ou d'un dossier n'était pas correctement détecté
  * Sélection multiple de documents

* 0.17.4
  * Amélioration du style des images dans la page d'aide.

* 0.17.3
  * Nouvelles icones material design

* 0.17.2
  * Mise à jour de toutes les bibliothèques à la dernière version

* 0.17.1
  * Corrections de la page d'aide pour se conformer aux modifications de l'interface.
  * Boutons de synchronisation : utilisation de vrais boutons

* 0.17.0
  * Nouvelle manière d'afficher le dossier courant et de naviguer parmi les dossiers parents.

* 0.16.0
  * Icones plus nettes.
  * Amélioration de l'interface graphique.

* 0.15.0
  * Correction dans le compte des erreurs de synchronisation des documents.

* 0.14.0
  * Simplifications dans la manière dont le progression de la synchronisation des documents est affichée.
  * Correction de bug : duplication de fichier lors de la synchronisation de compte en même temps qu'un upload (la v0.13.0 ne réglait pas complètement le problème).

* 0.13.0
  * Correction de bug : duplication de fichier lors de la synchronisation de compte en même temps qu'un upload.
  * Niveau de log de production (auparavant : DEBUG).
  * Lors de l'ajout d'un compte et que l'application n'arrive pas à joindre le serveur, propose à l'utilisateur de vérifier ses paramètres de proxy.
  * Paramètres par défaut du proxy : "Utiliser les proxies du système".
  * Correction du mauvais message d'erreur lors de l'ajout d'un compte.

* 0.12.0
  * Fin des tests de la beta : publication officielle de l'application pour tout le monde. Pas encore en 1.0 mais on s'en approche.
  * Ajout d'un dialogue pour créer une première clé ou en importer une après la création d'un keystore pour laisser le choix à l'utilisateur de créer une clé ou d'en importer une.

* 0.11.2
  * HubiC : nouvelle méthode pour mettre à jour le quota

* 0.11.1 :
  * Correction de bug : les dossiers n'étaient pas bien recréés lors d'un envoi après avoir supprimé les dossiers distants.

* 0.11.0 :
  * Préparation pour la diffusion

* 0.10.0 :
  * Correction de divers bugs
  * Restructuration du code pour une meilleure stabilité
  * Amélioration du chiffrement, incompatible avec les versions précédentes