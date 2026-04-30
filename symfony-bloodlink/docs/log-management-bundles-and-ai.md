# Audit Log + Alerts — Bundles, Métier Avancée & IA

> Module : **LogManagement** (responsable : ton task)  
> Branche : `symfony/LogManagement`  
> Domaine fonctionnel : traçabilité des dons et transferts sanguins, alertes opérationnelles, détection d''anomalies par IA.

---

## 1. Bundles & dépendances utilisés

### 1.1 Bundles ajoutés spécifiquement pour ce module

| Package | Version | Rôle |
|---|---|---|
| `knplabs/knp-snappy-bundle` | `^1.10` | Génération PDF via **wkhtmltopdf**. Utilisé par `LogManagementController` pour l''export PDF d''un log unique **et** d''une liste filtrée. |

> Dépendance binaire : `wkhtmltopdf.exe` (Windows) ou `wkhtmltopdf` (Linux).  
> Configuration : `config/packages/knp_snappy.yaml`  
> Variable d''environnement : `WKHTMLTOPDF_PATH`

### 1.2 Bundles déjà présents dans la base, exploités par ce module

| Package | Usage dans LogManagement |
|---|---|
| `endroid/qr-code` (`^6.1`) | Génération de QR codes SVG offline-readable par `AuditLogQrCodeGenerator`. Pas besoin du sous-bundle Symfony : on utilise directement la lib via `Builder`, `Encoding`, `ErrorCorrectionLevel`, `SvgWriter`. |
| `doctrine/doctrine-bundle` + `doctrine/orm` | Persistance des entités `DonationLog`, `BloodTransferRequestLog`, `Alert`, `DonorAlert`. |
| `doctrine/doctrine-migrations-bundle` | Migration `Version20260428150000` qui crée les tables d''audit et d''alertes. |
| `symfony/form` | Formulaires `DonationLogType`, `BloodTransferRequestLogType`, `DonorAlertType`. |
| `symfony/twig-bundle` + `twig/extra-bundle` | Rendu des templates `log_management/*` et `alerts/*`. |
| `symfony/http-client` | Appels HTTP vers les APIs NVIDIA (anomaly + chatbot) et Twilio (SMS). |
| `symfony/notifier` | Réservé pour future intégration multi-canal (Slack, email). |
| `symfony/security-bundle` | Restriction des routes `/dashboard/logs/*` aux rôles ADMIN / STAFF. |
| `symfony/uid` | UUID v7 sur les entités d''audit (`logId`). |
| `symfony/messenger` | DSN configuré pour traitement asynchrone potentiel des notifications. |

### 1.3 Variables d''environnement

```env
###> knplabs/knp-snappy-bundle ###
WKHTMLTOPDF_PATH="C:\Program Files\wkhtmltopdf\bin\wkhtmltopdf.exe"
###< knplabs/knp-snappy-bundle ###

###> nvidia ###
NVIDIA_API_KEY=nvapi-...        # Clé API NVIDIA Build (NIM) pour anomaly detection + chatbot
###< nvidia ###

###> twilio ###
TWILIO_ACCOUNT_SID=AC...        # SID compte Twilio
TWILIO_AUTH_TOKEN=...           # Token d''authentification
TWILIO_FROM_NUMBER=+1...        # Numéro émetteur SMS
###< twilio ###

###> symfony/messenger ###
MESSENGER_TRANSPORT_DSN=doctrine://default?auto_setup=0
###< symfony/messenger ###

###> symfony/mailer ###
MAILER_DSN=null://null
###< symfony/mailer ###
```

---

## 2. Métier avancée

### 2.1 Audit Log Management (traçabilité)

**Objectif** : tracer chaque action critique sur les dons et les transferts sanguins, permettre une revue rapide par les administrateurs et générer des preuves exportables.

#### Entités

| Entité | Table | Rôle |
|---|---|---|
| `DonationLog` | `donation_log` | Trace toutes les actions sur les dons : création, validation, annulation, modification de quantité, changement d''état du donneur. |
| `BloodTransferRequestLog` | `blood_transfer_request_log` | Trace toutes les actions sur les demandes de transfert : création, approbation, refus, livraison, urgence. |

Champs communs :
- `logId` (UUID v7)
- `actor` (acteur : User ou system)
- `action` (`create`, `update`, `delete`, `status_change`, ...)
- `severity` (`info`, `warning`, `critical`)
- `payloadBefore` / `payloadAfter` (snapshots JSON)
- `createdAt` (DateTimeImmutable)
- `anomalyReviewed` (bool), `anomalyNote` (text)

#### Repositories — recherche paginée avancée

`DonationLogRepository::searchPaginated()` et `BloodTransferRequestLogRepository::searchPaginated()` supportent :

- Filtrage par **plage de dates** (`createdAt` between)
- Filtrage par **acteur**, **action**, **severity**
- **Recherche full-text** dans `payloadBefore`, `payloadAfter`, `anomalyNote`
- **Tri** dynamique par colonne
- **Pagination** offset/limit avec count séparé

#### Workflow de revue d''anomalie

1. Un administrateur ouvre la fiche détail d''un log.
2. Le service `AuditAnomalyDetector` calcule un score automatique (cf. § IA).
3. Si score ≥ seuil, un encart "Anomalie suspectée" s''affiche.
4. L''admin peut **valider** ou **rejeter** avec une note (`anomalyNote`).
5. Le log est marqué `anomalyReviewed = true` et la décision est elle-même tracée.
6. Si severity = `critical`, un SMS est envoyé via Twilio.

#### Export PDF (Snappy)

Deux modes via `LogManagementController` :

- **Single log** : `GET /dashboard/logs/donations/{logId}/export-pdf`  
  → fiche détaillée + QR code + payloads avant/après + historique de revue.
- **Liste filtrée** : `GET /dashboard/logs/donations/export-pdf-list?...filtres`  
  → reprend exactement les filtres de l''écran liste, exporte toutes les pages.

Template unique adaptatif : `templates/log_management/pdf_report.html.twig` (mode `single` ou `list`).

#### QR codes offline-readable (`AuditLogQrCodeGenerator`)

- Encodage SVG (~3 Ko, scalable, imprimable sans pixellisation).
- Contenu : JSON compact avec `logId`, `action`, `severity`, `actor`, `createdAt`, `anomaly`.
- **Lisible hors-ligne** : un scan affiche les infos sans appel backend (utile pour audits de terrain).
- Niveau de correction d''erreur **High** (jusqu''à 30% du QR détérioré reste lisible).

### 2.2 Module Alerts

**Objectif** : diffuser des alertes opérationnelles aux donneurs et au staff hôpital.

| Entité | Cible | Cas d''usage |
|---|---|---|
| `Alert` | Broadcast (toute la plateforme) | Annonce maintenance, urgence sanitaire générale, campagne de don. |
| `DonorAlert` | Donneur spécifique | Rappel rendez-vous, notification compatibilité, anomalie sur son dossier. |

Composants :

- `AlertService` : logique métier (création, dispatch, archivage).
- `AlertsController` : interface back-office (CRUD).
- `Api/AlertController` : endpoint REST pour l''app mobile / consommateurs externes.
- `DonorAlertType` : formulaire de création d''alerte ciblée.

### 2.3 Sécurité & contraintes routage

- Les routes `/dashboard/logs/*` exigent `ROLE_ADMIN` ou `ROLE_STAFF`.
- Le path-param `{logId}` a une **regex requirement** `[0-9a-fA-F-]{36}` pour qu''une route comme `export-pdf-list` ne soit pas capturée comme un UUID.
- Les exports PDF ouvrent une nouvelle fenêtre avec `target="_blank"` + headers `Content-Disposition: attachment`.

---

## 3. Intelligence Artificielle

Deux services IA distincts, tous deux branchés sur **NVIDIA NIM** (NVIDIA Inference Microservices) via leur API publique.

### 3.1 Détection d''anomalies (`AuditAnomalyDetector`)

#### Pipeline hybride heuristique + LLM

```
Log entry → Heuristic scoring → si suspect → LLM verification → AuditAnomalyResult
                                                                 ↓ severity=critical
                                                          AuditSmsAlertNotifier → Twilio
```

#### Heuristiques (détection rapide, pas d''appel API)

- **Rafale d''actions** : > 10 mutations du même actor en < 60 secondes.
- **Heures inhabituelles** : actions entre 00h et 05h sur jours non-fériés.
- **Volume anormal** : modification d''une quantité de sang > 3× l''écart-type historique.
- **Cohérence états** : passage d''un état "validé" à "annulé" sans note justificative.
- **Acteur fantôme** : actor non trouvé dans la table `User`.

#### Vérification LLM (NVIDIA)

Si le score heuristique dépasse `0.4`, on envoie au LLM :
- Le payload du log + les 5 logs précédents du même actor
- Un prompt structuré demandant : `severity`, `confidence`, `reason`, `suggested_actions`

#### Sortie : `AuditAnomalyResult` (DTO immuable)

```php
final class AuditAnomalyResult {
    public readonly string $severity;      // low|medium|high|critical
    public readonly float $score;          // 0.0 - 1.0
    public readonly string $reason;        // explication courte
    public readonly array $suggestedActions; // string[]
    public readonly array $relatedLogIds;  // contexte
}
```

### 3.2 Chatbot Audit (`AuditChatbotController` + `AuditAiClient`)

**Objectif** : permettre aux administrateurs d''interroger les logs en **langage naturel**.

#### Exemples de questions

- "Quelles sont les anomalies critiques d''hier ?"
- "Qui a modifié le plus de logs de transfert cette semaine ?"
- "Y a-t-il un pattern suspect dans les annulations de don ?"
- "Liste les actions de l''utilisateur `rami.hassen` sur les 7 derniers jours."

#### Pipeline

1. L''admin tape sa question → `POST /dashboard/logs/chatbot/ask`.
2. `AuditChatbotController` valide (CSRF, throttling).
3. `AuditAiClient` :
   - **Étape 1 — Intent detection** : LLM classe la question (recherche / agrégation / analyse).
   - **Étape 2 — Query construction** : LLM génère un set de filtres (acteur, dates, action, severity) à passer à `searchPaginated`.
   - **Étape 3 — Context injection** : on injecte les top 20 logs résultants comme contexte.
   - **Étape 4 — Response generation** : LLM répond en langage naturel + cite les `logId` pertinents.
4. La réponse retourne JSON `{ answer: string, citedLogIds: string[], confidence: float }`.

#### Garanties

- **Pas de hallucination** : la réponse cite des `logId` réels ; l''UI les rend cliquables vers la fiche détail.
- **Privacy** : aucun PII brut n''est envoyé au LLM (les noms d''acteurs sont **hashés** avant envoi).
- **Cost control** : cache LRU 15 min sur les questions identiques.
- **Fallback** : si NVIDIA indisponible, un mode dégradé renvoie un résultat sql-only sans la phrase générée.

### 3.3 Configuration AI commune

| Paramètre | Valeur par défaut |
|---|---|
| Endpoint | `https://integrate.api.nvidia.com/v1/chat/completions` |
| Modèle | `meta/llama-3.1-70b-instruct` (configurable) |
| Timeout | 30s |
| Temperature | 0.2 (anomaly) / 0.4 (chatbot) |
| Max tokens | 512 (anomaly) / 1024 (chatbot) |

---

## 4. Récapitulatif des fichiers livrés

### Code (PHP)

| Type | Fichier |
|---|---|
| Controller | `src/Controller/LogManagementController.php` |
| Controller | `src/Controller/AuditChatbotController.php` |
| Controller | `src/Controller/AlertsController.php` |
| Controller | `src/Controller/Api/AlertController.php` |
| Entity | `src/Entity/DonationLog.php` |
| Entity | `src/Entity/BloodTransferRequestLog.php` |
| Entity | `src/Entity/Alert.php` |
| Entity | `src/Entity/DonorAlert.php` |
| Repository | `src/Repository/DonationLogRepository.php` |
| Repository | `src/Repository/BloodTransferRequestLogRepository.php` |
| Repository | `src/Repository/AlertRepository.php` |
| Repository | `src/Repository/DonorAlertRepository.php` |
| Form | `src/Form/DonationLogType.php` |
| Form | `src/Form/BloodTransferRequestLogType.php` |
| Form | `src/Form/DonorAlertType.php` |
| Service | `src/Service/AuditAnomalyDetector.php` |
| Service | `src/Service/AuditAnomalyResult.php` |
| Service | `src/Service/AuditLogQrCodeGenerator.php` |
| Service | `src/Service/AuditSmsAlertNotifier.php` |
| Service | `src/Service/AuditAiClient.php` |
| Service | `src/Service/AlertService.php` |
| Test | `tests/Service/AuditAnomalyDetectorTest.php` |
| Migration | `migrations/Version20260428150000.php` |

### Templates

- `templates/log_management/` — dashboard, donation_logs, transfer_logs, donor_alerts, layouts, partials, pdf_report (13 fichiers)
- `templates/alerts/` — index, form, show, partials (7 fichiers)

### Configuration

- `config/packages/knp_snappy.yaml`
- `config/bundles.php` (registration de `KnpSnappyBundle`)
- `composer.json` / `composer.lock` (ajout `knplabs/knp-snappy-bundle`)
- `.env` (variables NVIDIA, Twilio, Snappy, Mailer, Messenger)
- `symfony.lock`

### UI assets

- `public/scripts/log-management-confirm.js`
- `public/styles/log-management-overrides.css`
- `public/styles/log-management-tailadmin.css`

### Documentation

- `docs/audit-logs-integration.md`
- `docs/logmanagement-module-spec.md`
- `docs/logmanagement-demo-handoff.md`
- `docs/logmanagement-professor-guide.md`
- `docs/plans/2026-04-28-audit-anomaly-detection.md`
- `docs/plans/2026-04-28-audit-log-management-ultra-review.md`
- `docs/log-management-bundles-and-ai.md` (ce fichier)

---

## 5. Schéma d''architecture (vue d''ensemble)

```
┌──────────────────────┐
│   Browser (Admin)    │
└─────────┬────────────┘
          │ HTTP
┌─────────▼────────────────────────────────────────────────────┐
│                    Symfony 6.4 Kernel                        │
│                                                              │
│  ┌────────────────────────┐   ┌──────────────────────────┐   │
│  │ LogManagementController│   │ AuditChatbotController   │   │
│  └─┬─────────┬─────────┬──┘   └─────────────┬────────────┘   │
│    │         │         │                    │                │
│    │   ┌─────▼─────┐   │                    │                │
│    │   │ Snappy PDF│   │                    │                │
│    │   │ (wkhtml)  │   │                    │                │
│    │   └───────────┘   │                    │                │
│    │                   │                    │                │
│  ┌─▼─────────────┐  ┌──▼──────────────────┐ │                │
│  │ Repositories  │  │AuditAnomalyDetector │ │                │
│  │ (Doctrine)    │  └──┬──────────────┬───┘ │                │
│  └─┬─────────────┘     │              │     │                │
│    │                   │ HTTP         │ HTTP│ HTTP           │
│  ┌─▼──────┐  ┌─────────▼──────┐  ┌────▼─────▼───┐            │
│  │ Postgres│  │ NVIDIA NIM API│  │AuditAiClient │            │
│  │ Supabase│  │ (LLM 70B)     │  │              │            │
│  └────────┘  └────────────────┘  └──────────────┘            │
│                                                              │
│  ┌──────────────────────┐                                    │
│  │ AuditSmsAlertNotifier│──HTTP──▶ Twilio SMS API            │
│  └──────────────────────┘                                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 6. Démo / parcours utilisateur

1. Admin se connecte → menu "Logs" → `/dashboard/logs`
2. Filtre par sévérité = `critical` sur les 7 derniers jours.
3. Clique sur un log → fiche détail avec QR code SVG + bouton "Export PDF".
4. Si une anomalie est détectée → encart rouge avec score + raison LLM + boutons "Valider" / "Rejeter".
5. Validation = `critical` → SMS automatique au responsable via Twilio.
6. Bouton "Export liste" → PDF reprenant tous les logs filtrés (avec pagination).
7. Onglet "Chatbot" → admin tape "Quelles anomalies critiques aujourd''hui ?" → réponse en langage naturel + liens cliquables vers les `logId` cités.

---

> Auteur : Module LogManagement  
> Dernière mise à jour : 2026-04-28
