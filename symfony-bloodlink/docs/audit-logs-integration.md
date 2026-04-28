# Audit Logs Integration Note

This document explains how the `Audit Logs` module was integrated into the final merged Symfony copy and how it fits into the rest of the application.

## Integration Target

The final delivered Symfony work lives in one project:

- `symfony-bloodlink`

The Audit Logs module was integrated into that merged project rather than kept as a separate isolated prototype.

## Goal

The integration work had four objectives:

1. keep the migration branch as the single final Symfony deliverable
2. transplant the clean Audit Logs implementation into that project
3. normalize the UI so the module feels native to the rest of the app
4. preserve role logic and hospital scoping

## What Was Integrated

### Back Office Audit Workspace

The integrated Audit Logs workspace includes:

- Audit overview dashboard
- donation log CRUD
- transfer log CRUD
- filters, pagination, timeline, and overview cards
- audit-specific charts on the overview page
- AI anomaly detection with severity, score, reasons, recommendation, and review state
- high-risk SMS alert attempts for critical audit signals

Routes:

- `/dashboard/logs`
- `/dashboard/logs/donations`
- `/dashboard/logs/donations/new`
- `/dashboard/logs/donations/{logId}`
- `/dashboard/logs/donations/{logId}/review-anomaly`
- `/dashboard/logs/donations/{logId}/edit`
- `/dashboard/logs/donations/{logId}/delete`
- `/dashboard/logs/donations/{logId}/export-pdf`
- `/dashboard/logs/transfers`
- `/dashboard/logs/transfers/new`
- `/dashboard/logs/transfers/{logId}`
- `/dashboard/logs/transfers/{logId}/review-anomaly`
- `/dashboard/logs/transfers/{logId}/edit`
- `/dashboard/logs/transfers/{logId}/delete`
- `/dashboard/logs/transfers/{logId}/export-pdf`

### Donor Front Office Audit View

The audit integration also includes a donor-facing read-only page:

- `/dashboard/donor/audit-trail`

This page was added so the module has a real FO presence instead of being Back Office only. It shows:

- donation history
- linked donation audit records
- transfer traceability context

## What Stayed Separate

`Alerts` was intentionally left as its own workspace.

That means:

- alert CRUD remains under `/dashboard/alerts`
- donor alert pages remain under `/dashboard/donor/alerts`
- the Audit Logs overview does not own alert lifecycle charts anymore

This separation mirrors the intended module boundaries more cleanly.

## Data-Layer Integration

The integration kept the reverse-engineered entity layer and repaired it where needed:

- repository namespace fixes
- missing `BloodTransferRequestLogRepository`
- relation-based `DonorAlert`

The main audit entities used by the module are:

- `DonationLog`
- `BloodTransferRequestLog`

Both log entities now carry module-owned anomaly metadata and SMS delivery metadata. This keeps AI review traceability with the audit record itself.

The module continues to rely on the shared PostgreSQL / Supabase dataset rather than mock data.

## Session and Role Integration

The module uses the same auth session contract as the rest of the merged app:

- `id`
- `email`
- `first_name`
- `last_name`
- `user_type`
- `hospital_id`
- `hospital_name`

Role behavior:

- `ADMIN`: sees all audit data
- `HOSPITAL_STAFF`: sees only records tied to the staff hospital
- `DONOR`: Front Office only, read-only audit trail access

## UI Integration

The module was normalized into the shared template system instead of keeping its own disconnected shell.

It now uses:

- shared Back Office shell
- shared Front Office shell
- shared flash / pagination / confirm-modal partials
- module-specific audit styles layered on top of the normalized UI

This makes the module read like part of the same application rather than a pasted demo.

## Form and Validation Behavior

The module keeps the intended audit-trail rules:

- snapshot fields are captured from the selected parent record
- on edit, snapshot fields stay locked
- action and notes remain editable where allowed
- status-transition validation stays server-side

## AI and SMS Integration

Translation and generic chatbot behavior are not part of the Audit Logs smart feature.

The implemented smart feature is AI-assisted anomaly review:

- deterministic audit rules always run
- NVIDIA-backed AI enrichment runs only when `NVIDIA_API_KEY` is configured
- the AI payload avoids donor names and actor emails; local Symfony screens keep full attribution
- high/critical anomaly results attempt SMS delivery through Twilio credentials when available
- missing AI or SMS configuration degrades to stored review metadata instead of blocking CRUD
- staff can mark anomaly results reviewed from the donation/transfer detail page

This applies to both donation logs and transfer logs.

## Demo Integration

The merged copy also includes a usable donor demo path:

- one donor donation history entry
- linked donation-log records
- matching transfer-context data

This ensures `My Audit Trail` shows real data for demonstration instead of an empty state.

## Verification

Use these commands after pulling the project:

```bash
php bin/console doctrine:mapping:info
php bin/console lint:twig templates
php bin/console app:reverse-engineer:entities --help
php bin/console doctrine:query:sql "SELECT 1"
```

## Practical Summary

In short, the module is no longer a standalone clean copy living beside the app. It is now integrated into the merged Symfony project with:

- shared shells
- preserved role guardrails
- audit-specific CRUD
- donor-facing FO coverage
- live database-backed demo data
