# Politique de Confidentialité — Nomade Restaurant

**Dernière mise à jour : 11 mai 2025**

---

## 1. Présentation

La présente Politique de Confidentialité décrit comment **Nomade253** (ci-après « nous », « notre » ou « l'Éditeur ») collecte, utilise et protège les informations personnelles des utilisateurs de l'application mobile **Nomade Restaurant** (identifiant : `dj.nomade.velox_restaurant`), disponible sur Google Play Store.

Cette application est un outil de gestion professionnel destiné exclusivement aux **restaurants partenaires Nomade253**. Elle n'est pas accessible au grand public.

---

## 2. Responsable du traitement

| | |
|---|---|
| **Entité** | Nomade253 |
| **Pays** | République de Djibouti |
| **Contact** | devchirdon@gmail.com |

---

## 3. Données collectées

### 3.1 Données fournies par l'utilisateur

| Donnée | Finalité | Base légale |
|---|---|---|
| Adresse email | Authentification et identification du compte restaurant | Exécution du contrat |
| Mot de passe | Sécurisation de l'accès (hashé, non accessible par Nomade253) | Exécution du contrat |
| Nom du restaurant | Affichage dans l'application et sur la plateforme Nomade253 | Exécution du contrat |
| Adresse du restaurant | Coordination des livraisons | Exécution du contrat |
| Numéro de téléphone | Contact et coordination opérationnelle | Exécution du contrat |
| Photo de bannière | Personnalisation du profil restaurant sur la plateforme | Consentement |
| Photos des plats | Affichage du menu dans l'application client | Consentement |

### 3.2 Données collectées automatiquement

| Donnée | Finalité | Base légale |
|---|---|---|
| Token FCM (Firebase Cloud Messaging) | Envoi de notifications push (nouvelles commandes) | Intérêt légitime |
| Données de session Firebase Auth | Maintien de la connexion sécurisée | Exécution du contrat |
| Rapports de crash (Firebase Crashlytics) | Amélioration de la stabilité de l'application | Intérêt légitime |
| Données d'utilisation anonymisées (Firebase Analytics) | Analyse de l'utilisation pour améliorer l'expérience | Intérêt légitime |

### 3.3 Données NON collectées

Nous ne collectons pas :
- Données de localisation GPS
- Contacts du téléphone
- Contenu des SMS ou appels
- Données biométriques
- Informations bancaires ou de paiement

---

## 4. Services tiers utilisés

L'application utilise les services Firebase de Google LLC (1600 Amphitheatre Parkway, Mountain View, CA 94043, USA) :

| Service | Finalité | Politique Google |
|---|---|---|
| Firebase Authentication | Gestion des comptes et sessions | [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy) |
| Firebase Firestore | Stockage et synchronisation des données en temps réel | [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy) |
| Firebase Storage | Stockage des photos (plats, bannières) | [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy) |
| Firebase Cloud Messaging | Notifications push | [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy) |
| Firebase Crashlytics | Rapports de crash anonymisés | [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy) |
| Firebase Analytics | Statistiques d'utilisation anonymisées | [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy) |

Les données peuvent être transférées et traitées sur des serveurs situés aux États-Unis. Google LLC est soumis au mécanisme EU-U.S. Data Privacy Framework.

---

## 5. Partage des données

Nous ne vendons, ne louons et ne partageons pas vos données personnelles avec des tiers à des fins commerciales.

Les données peuvent être partagées uniquement avec :
- **Firebase / Google** : en tant que sous-traitant technique (voir section 4)
- **Livreurs partenaires Nomade253** : uniquement les informations nécessaires à la livraison (nom du restaurant, adresse de collecte) lors de la validation d'une commande
- **Autorités compétentes** : si requis par la loi en République de Djibouti

---

## 6. Conservation des données

| Type de données | Durée de conservation |
|---|---|
| Données de compte (email, profil restaurant) | Jusqu'à la suppression du compte partenaire |
| Historique des commandes | 2 ans à compter de la date de commande |
| Photos (plats, bannières) | Jusqu'à suppression manuelle par l'utilisateur |
| Données analytiques anonymisées | 14 mois (paramètre Google Analytics) |
| Rapports de crash | 90 jours (paramètre Crashlytics) |
| Token FCM | Mis à jour automatiquement, supprimé à la déconnexion |

---

## 7. Sécurité des données

Nous mettons en œuvre les mesures de sécurité suivantes :
- Authentification sécurisée via Firebase Auth (mots de passe hashés, jamais accessibles en clair)
- Communication chiffrée en transit (HTTPS/TLS)
- Accès aux données Firestore restreint aux utilisateurs authentifiés via des règles de sécurité
- Accès à l'application conditionné à un compte partenaire actif (`isActive == true`)
- Certificat d'intégrité de l'application via Google Play Integrity API

---

## 8. Droits des utilisateurs

Conformément aux lois applicables, vous disposez des droits suivants :

- **Droit d'accès** : obtenir une copie de vos données personnelles
- **Droit de rectification** : corriger des données inexactes via l'écran Profil de l'application
- **Droit à l'effacement** : demander la suppression de votre compte et de vos données
- **Droit à la portabilité** : recevoir vos données dans un format structuré
- **Droit d'opposition** : vous opposer au traitement basé sur l'intérêt légitime

Pour exercer ces droits, contactez-nous à : **devchirdon@gmail.com**

Nous nous engageons à répondre dans un délai de **30 jours**.

---

## 9. Notifications push

L'application utilise les notifications push (FCM) pour vous alerter en temps réel des nouvelles commandes. Ces notifications sont essentielles au fonctionnement du service.

Vous pouvez désactiver les notifications à tout moment depuis **Paramètres Android → Applications → Nomade Restaurant → Notifications**. Cela n'affectera pas votre accès à l'application, mais vous ne recevrez plus d'alertes en temps réel.

---

## 10. Mineurs

Cette application est destinée à un usage professionnel exclusivement. Elle n'est pas destinée aux personnes de moins de 18 ans. Nous ne collectons pas sciemment de données relatives à des mineurs.

---

## 11. Modifications de la politique

Nous pouvons mettre à jour cette Politique de Confidentialité. En cas de modification substantielle, nous vous en informerons via une notification dans l'application ou par email.

La date de « Dernière mise à jour » en haut de ce document indique la version en vigueur.

---

## 12. Contact

Pour toute question relative à cette politique ou à vos données personnelles :

**Nomade253**
Email : devchirdon@gmail.com

---

## 13. Publication web

Cette politique doit être publiée à une URL publique et permanente avant la soumission sur Google Play Store.

**URL recommandée :** `https://nomade253.com/privacy-policy`

Options d'hébergement gratuites :
- **GitHub Pages** : créer un repo public avec ce fichier en `.html`
- **Notion** : créer une page publique et copier l'URL
- **Google Sites** : créer une page et la rendre publique

---

*© 2025 Nomade253 — Tous droits réservés*
