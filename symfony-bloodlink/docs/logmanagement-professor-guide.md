# Audit Logs Module - Professor Technical Guide

This document describes the BloodLink Audit Logs module as a standalone Symfony module: what it contains, how it works, which external APIs it uses, and how to test the AI and smart SMS feature correctly.

## 1. Module Purpose

The Audit Logs module is the traceability and governance workspace for BloodLink. It tracks important status transitions for:

- blood donation records
- inter-hospital blood transfer requests

It is separate from the Alerts workspace. Alerts handle donor communication lifecycle. Audit Logs handle operational traceability, anomaly review, and audit evidence.

## 2. Main Features

### Back Office Audit Dashboard

Route:

```text
GET /dashboard/logs
```

The dashboard shows:

- total donation audit logs
- total transfer audit logs
- total audit entries
- recent audit changes
- AI anomaly count
- high-risk anomaly count
- recent donation/transfer audit timeline
- live donation-vs-transfer activity chart
- anomaly severity chart
- shortcuts to review unreviewed donation/transfer anomalies

Hospital staff only see audit data for their hospital. Admins see all audit data.

### Donation Audit CRUD

Routes:

```text
GET      /dashboard/logs/donations
GET|POST /dashboard/logs/donations/new
GET      /dashboard/logs/donations/{logId}
GET|POST /dashboard/logs/donations/{logId}/edit
POST     /dashboard/logs/donations/{logId}/delete
GET      /dashboard/logs/donations/{logId}/export-pdf
POST     /dashboard/logs/donations/{logId}/review-anomaly
```

Features:

- create donation audit entries
- edit allowed audit fields
- delete with CSRF protection and modal confirmation
- filter by keyword, date range, action, new status, and anomaly state
- paginate results
- export a PDF report
- show linked donation, donor, hospital, actor, status transition, notes, AI anomaly result, SMS status, and human review state

On create, the previous status is captured from the selected donation. On edit, snapshot fields stay locked.

### Transfer Audit CRUD

Routes:

```text
GET      /dashboard/logs/transfers
GET|POST /dashboard/logs/transfers/new
GET      /dashboard/logs/transfers/{logId}
GET|POST /dashboard/logs/transfers/{logId}/edit
POST     /dashboard/logs/transfers/{logId}/delete
GET      /dashboard/logs/transfers/{logId}/export-pdf
POST     /dashboard/logs/transfers/{logId}/review-anomaly
```

Features:

- create transfer audit entries
- edit allowed audit fields
- delete with CSRF protection and modal confirmation
- filter by keyword, date range, action, new status, and anomaly state
- paginate results
- export a PDF report
- show linked transfer request, requesting/approving hospitals, actor, status transition, notes, AI anomaly result, SMS status, and human review state

Hospital staff can see transfer logs only when their hospital is the requesting or approving hospital.

### Donor Front Office Audit Trail

Route:

```text
GET /dashboard/donor/audit-trail
```

This is a read-only donor-facing page. It shows the donor their own donation history, donation audit records, and transfer traceability context.

### AI Audit Assistant

Route:

```text
POST /dashboard/logs/chatbot
```

The floating assistant is visible in the Back Office Audit Logs shell. It is the AI integration feature for the module.

Behavior:

- uses NVIDIA AI through `AuditAiClient`
- answers questions about recent audit logs only
- understands donation logs, transfer logs, anomaly severity, anomaly score, review state, and SMS status
- behaves conversationally for greetings and short replies
- is scoped by role:
  - Admin context: recent audit logs across all hospitals
  - Hospital staff context: recent audit logs for the staff hospital only
  - Donor: blocked from Back Office assistant

The assistant does not query unrelated application modules.

### Smart Feature: AI Anomaly Detection With SMS Alerting

This is the smart feature by itself.

When a donation or transfer audit log is created or edited:

1. Symfony saves the audit log entity.
2. The module builds a trusted server-side payload from the saved entity.
3. `AuditAnomalyDetector` runs deterministic audit rules.
4. `AuditAiClient` optionally asks NVIDIA AI for structured anomaly analysis when `NVIDIA_API_KEY` is configured.
5. The anomaly result is stored on the log row.
6. If severity is high or critical, `AuditSmsAlertNotifier` sends an SMS through Twilio.
7. Staff can mark the anomaly as reviewed from the detail page.

Stored anomaly fields include:

- detected or normal
- severity
- score
- reasons
- explanation
- recommended action
- analysis provider
- reviewed or pending
- reviewed by / reviewed at
- SMS status, recipient, error, and sent timestamp

Translation is not part of this module anymore. The old translation feature was removed.

## 3. External APIs Used By Audit Logs

### NVIDIA AI API

Used by:

- `AuditAiClient`
- AI anomaly enrichment
- Audit Logs assistant chatbot

Environment variable:

```env
NVIDIA_API_KEY=...
```

Endpoint:

```text
https://integrate.api.nvidia.com/v1/chat/completions
```

Model:

```text
meta/llama-4-maverick-17b-128e-instruct
```

Safety note:

- anomaly payload avoids donor names and actor emails
- assistant context is hospital-scoped for hospital staff
- no API key is committed in project files

### Twilio SMS API

Used by:

- `AuditSmsAlertNotifier`
- high/critical audit anomaly SMS alerting

Environment variables:

```env
TWILIO_ACCOUNT_SID=...
TWILIO_AUTH_TOKEN=...
TWILIO_FROM_NUMBER=...
```

Twilio endpoint:

```text
https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json
```

Recipient resolution:

The SMS uses the phone number of the linked hospital:

- donation log -> donation -> hospital -> phone
- transfer log -> requesting/approving hospital -> phone

## 4. Symfony Bundles And Packages Used By This Module

The Audit Logs module relies on these Symfony/Composer packages:

| Package | Used For |
|---------|----------|
| `doctrine/orm` | audit entities and relations |
| `doctrine/doctrine-bundle` | repository integration and entity manager |
| `doctrine/doctrine-migrations-bundle` | anomaly metadata migration |
| `symfony/form` | donation/transfer audit forms |
| `symfony/validator` | server-side action/status validation |
| `symfony/twig-bundle` and `twig/twig` | Back Office and Front Office templates |
| `symfony/asset` | module CSS and JS assets |
| `symfony/http-client` | NVIDIA and Twilio HTTP calls |
| `dompdf/dompdf` | audit PDF export |
| `phpunit/phpunit` | focused module tests |

## 5. Module File Structure

Only files owned by, or directly relevant to, the Audit Logs module are listed.

```text
docs/
  audit-logs-integration.md
  logmanagement-demo-handoff.md
  logmanagement-module-spec.md
  logmanagement-professor-guide.md
  plans/
    2026-04-28-audit-anomaly-detection.md
    2026-04-28-audit-log-management-ultra-review.md

src/
  Controller/
    LogManagementController.php
    AuditChatbotController.php
  Entity/
    DonationLog.php
    BloodTransferRequestLog.php
    Donation.php
    BloodTransferRequest.php
    User.php
    Hospital.php
    HospitalStaff.php
  Form/
    DonationLogType.php
    BloodTransferRequestLogType.php
  Repository/
    DonationLogRepository.php
    BloodTransferRequestLogRepository.php
    DonationRepository.php
    BloodTransferRequestRepository.php
  Service/
    AuditAiClient.php
    AuditAnomalyDetector.php
    AuditAnomalyResult.php
    AuditSmsAlertNotifier.php

templates/
  log_management/
    dashboard/
      index.html.twig
    donation_logs/
      form.html.twig
      index.html.twig
      show.html.twig
    transfer_logs/
      form.html.twig
      index.html.twig
      show.html.twig
    front/
      index.html.twig
    layouts/
      back_office.html.twig
      front_office.html.twig
    partials/
      _flash.html.twig
      _pagination.html.twig
    pdf_report.html.twig

public/
  scripts/
    log-management-confirm.js
  styles/
    log-management-overrides.css
    log-management-tailadmin.css

migrations/
  Version20260428150000.php

tests/
  Service/
    AuditAnomalyDetectorTest.php

config/
  services.yaml

project root:
  composer.json
  composer.lock
  .env.example
  phpunit.dist.xml
```

## 6. How The Module Works Internally

### Request And Access Flow

1. User signs in through the shared auth page.
2. Session stores:
   - user id
   - email
   - first name
   - last name
   - user type
   - hospital id
   - hospital name
3. `LogManagementController` checks the session before every Back Office audit route.
4. Hospital staff must have a hospital id.
5. Repositories receive the scoped hospital id and apply it to queries.

### Donation Log Create Flow

1. Staff opens `/dashboard/logs/donations/new`.
2. Form lists donations visible to that user.
3. Staff chooses donation, action, new status, and notes.
4. Controller copies previous status from the selected donation.
5. Symfony validation checks required fields and valid status transitions.
6. Doctrine saves the log.
7. `AuditAnomalyDetector` analyzes the saved log.
8. AI result and SMS status are persisted.
9. User is redirected to the detail page.

### Transfer Log Create Flow

1. Staff opens `/dashboard/logs/transfers/new`.
2. Form lists transfer requests visible to that user.
3. Staff chooses request, action, new status, and notes.
4. Controller copies previous status from the selected transfer request.
5. Symfony validation checks required fields and valid status transitions.
6. Doctrine saves the log.
7. `AuditAnomalyDetector` analyzes the saved log.
8. AI result and SMS status are persisted.
9. User is redirected to the detail page.

### AI Anomaly Flow

`AuditAnomalyDetector` applies rules first:

- status moved backward
- sensitive status change without notes
- screening failure
- rejected donation
- transfer delivered from an unexpected previous status
- transfer dispatched from an unexpected previous status
- many audit entries by same actor in a short time
- operational keywords like urgent, override, emergency, correction, backdate

Then it calls NVIDIA AI if configured. The final result merges deterministic rules and AI response.

### SMS Flow

If severity is high or critical:

1. `AuditSmsAlertNotifier` resolves the linked hospital phone number.
2. It sends an SMS through Twilio REST API.
3. The log stores `sent`, `failed`, `skipped`, or `not_required`.

## 7. Professor Demo Accounts

Use these accounts from `docs/demo-accounts.md`.

```text
Admin:
email: admin@admin.com
password: admin123654

Hospital staff for Hôpital Charles Nicollee:
email: yassine@staff.com
password: 7894561230
```

The best demo account for the SMS scenario is:

```text
yassine@staff.com
```

Reason:

- it proves hospital staff scoping
- it is linked to Hôpital Charles Nicollee
- the hospital phone fallback can be tested cleanly

## 8. How To Test The AI Assistant

Prerequisite:

```env
NVIDIA_API_KEY=...
```

Steps:

1. Sign in as `yassine@staff.com`.
2. Open:

```text
http://127.0.0.1:8000/dashboard/logs
```

3. Click the floating message icon in the bottom-right corner.
4. Ask:

```text
hey
```

Expected:

- assistant greets back
- assistant offers audit-log review options

5. Ask:

```text
what high risk audit logs do I have?
```

Expected:

- assistant answers only from recent hospital-scoped audit context
- assistant mentions anomaly severity, score, status transition, review state, or SMS state when available

6. Ask:

```text
why was the latest donation log marked high risk?
```

Expected:

- assistant explains the high-risk reasons using the saved audit context

## 9. How To Test The Smart Feature And Real SMS

This is the real professor-facing smart feature test.

### Environment

In local `.env`, set:

```env
NVIDIA_API_KEY=...
TWILIO_ACCOUNT_SID=...
TWILIO_AUTH_TOKEN=...
TWILIO_FROM_NUMBER=...
```

Do not commit `.env` and do not include it in the final zip.

### Database Preparation

For the test, temporarily put the professor's own phone number in the selected hospital's `phone` field in Supabase.

Recommended Supabase update target:

```text
table: hospital
record: Hôpital Charles Nicollee
field: phone
value: professor phone number in E.164 format, for example +216XXXXXXXX
```

After the test, restore the original hospital phone number.

Original value before the test:

```text
Hôpital Charles Nicollee
phone = +216 71 999 888
```

You can restore it with:

```bash
php bin/console doctrine:query:sql "UPDATE hospital SET phone = '+216 71 999 888' WHERE hospital_id = '550e8400-e29b-41d4-a716-446655440001'"
```

### Real Test Steps

1. Sign in as:

```text
email: yassine@staff.com
password: 7894561230
```

2. Open:

```text
http://127.0.0.1:8000/dashboard/logs/donations/new
```

3. Select a donation from Hôpital Charles Nicollee.

Known examples:

```text
7e06f685...
359db5e5...
bcabc7bf...
```

4. Fill the form:

```text
Action: Screening Failed
New Status: Rejected
Notes: urgent manual override for screening failure
```

5. Save.

Expected:

- detail page opens
- AI anomaly review appears
- severity is high
- score is visible
- reasons explain the risk
- SMS status becomes `sent`
- the phone number stored on the hospital receives the Twilio SMS

Recommended screenshots:

1. audit log detail page showing:
   - high severity
   - score
   - reasons
   - provider `rules+ai`
   - SMS `sent`
2. phone screen showing received SMS
3. chatbot answer explaining the high-risk log

### If SMS Does Not Send

Check the detail page:

- `SMS: skipped` means Twilio credentials or recipient were missing.
- `SMS: failed` means Twilio rejected the request or the phone number.
- `SMS: not_required` means the anomaly was not high/critical.

For Tunisian numbers, use E.164 format:

```text
+216XXXXXXXX
```

## 10. Required Verification Commands

Run before demo or zip packaging:

```bash
php bin/console doctrine:migrations:migrate
php bin/console doctrine:schema:validate --skip-sync
php bin/console lint:twig templates/log_management
php bin/console lint:container
vendor/bin/phpunit tests/Service/AuditAnomalyDetectorTest.php
```

Also confirm routes:

```bash
php bin/console debug:router | grep dashboard_logs
```

On Windows PowerShell:

```powershell
php bin\console debug:router | Select-String dashboard_logs
```

## 11. Packaging Notes

Before zipping the folder:

- remove local secrets from `.env`
- do not include live Twilio or NVIDIA keys in the zip
- restore Hôpital Charles Nicollee phone to its original value if it was changed for testing
- keep `.env.example` because it documents required env keys without secrets
- keep this guide in `docs/`

The module can be demonstrated without Twilio credentials, but the SMS part will show `skipped`. For a live SMS screenshot, Twilio credentials and a real hospital phone number are required.
