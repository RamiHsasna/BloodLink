# Audit Logs and Alerts Module — Specification for the Merged Symfony Copy

This document describes the delivered `Audit Logs` workspace inside the merged BloodLink Symfony project, plus the separate `Alerts` workspace and the donor-facing `My Audit Trail` page. It shows how the final structure aligns with the professor's first-validation expectations.

---

## 1. What the professor asked for

The first validation required:

1. a reusable Front Office template
2. a reusable Back Office template
3. entity generation / CRUD with at least one relation
4. server-side validation
5. linked pages and a coherent scenario
6. evidence of workshop alignment, especially reverse engineering

This merged Symfony copy answers that with:

- a shared Back Office shell in `templates/dashboard/layouts/back_office.html.twig`
- a shared Donor Front Office shell in `templates/dashboard/layouts/front_office.html.twig`
- a dedicated auth shell in `templates/auth/index.html.twig`
- CRUD pages for the three audit entities
- relation-based entities and server-side validation rules
- linked BO and FO routes
- reverse-engineering proof via `reverse-engineering.php` and `app:reverse-engineer:entities`

---

## 2. Scope of the delivered module

The Audit Logs workspace is named **Audit Logs** and covers two audit entity families:

| Entity | Purpose |
|--------|---------|
| `DonationLog` | audit trail for donation status transitions |
| `BloodTransferRequestLog` | audit trail for transfer-request status transitions |
| `DonorAlert` | donor communication lifecycle tracking |

The separate Alerts workspace owns `DonorAlert` CRUD and the donor-facing alert timeline. The Audit Logs workspace also exposes a donor-facing `My Audit Trail` page so the donor can review their own audit history without mixing that flow into alert management.

---

## 3. Reverse-engineering alignment

This project is database-first, not schema-invented in Symfony.

Proof kept in the repo:

- `reverse-engineering.php`
- `src/Command/ReverseEngineerEntitiesCommand.php`
- `php bin/console app:reverse-engineer:entities --help`

Repair work added on top of that generated base:

- repository namespace fixes from `App\Entity\Repository\...` to `App\Repository\...`
- missing `BloodTransferRequestLogRepository`
- `DonorAlert` converted from raw FK fields to real Doctrine relations

That means the entity layer follows the reverse-engineering workshop structure, then receives the minimum repair required to become a usable Symfony model layer.

---

## 4. Roles and access model

| Role | Access |
|------|--------|
| `ADMIN` | full Back Office access, sees all audit records and all alerts |
| `HOSPITAL_STAFF` | Back Office access, but audit and alert queries are filtered to the staff member's hospital |
| `DONOR` | Front Office only, read-only access to personal alert pages and My Audit Trail |

Session payload used by the integrated shells:

- `id`
- `email`
- `first_name`
- `last_name`
- `user_type`
- `hospital_id`
- `hospital_name`

The hospital fields are populated at sign-in by joining `users` to `hospital_staff` and `hospital`.

---

## 5. Entity rules

### `DonationLog`

- linked to `Donation`
- linked to `User`
- stores `action`, `previous_status`, `new_status`, `notes`, `created_at`
- `previous_status` and `new_status` must differ
- on create, `previous_status` is captured from the selected donation
- on edit, snapshot fields stay locked

### `BloodTransferRequestLog`

- linked to `BloodTransferRequest`
- linked to `User`
- stores `action`, `previous_status`, `new_status`, `notes`, `created_at`
- `previous_status` and `new_status` must differ
- on create, `previous_status` is captured from the selected transfer request
- on edit, snapshot fields stay locked

### `DonorAlert`

- linked to `Alert`
- linked to `Donor`
- stores notification / read / donor-response lifecycle fields
- rules enforced server-side:
  - `notification_sent_at` required when `is_notified = true`
  - `notification_sent_at` must be empty when `is_notified != true`
  - `read_at` required when `is_read = true`
  - `read_at` must be empty when `is_read != true`
  - a donor response other than `NO_RESPONSE` is only valid once the alert is marked read

`DonorAlert` is owned by the separate Alerts workspace, not the Audit Logs workspace.

---

## 6. Audit Logs pages delivered

### Back Office pages

| Route | Purpose |
|-------|---------|
| `/dashboard/logs` | audit overview dashboard |
| `/dashboard/logs/donations` | donation log listing + filters + pagination |
| `/dashboard/logs/donations/new` | create donation log |
| `/dashboard/logs/donations/{logId}` | donation log detail |
| `/dashboard/logs/donations/{logId}/edit` | edit donation log |
| `/dashboard/logs/transfers` | transfer log listing + filters + pagination |
| `/dashboard/logs/transfers/new` | create transfer log |
| `/dashboard/logs/transfers/{logId}` | transfer log detail |
| `/dashboard/logs/transfers/{logId}/edit` | edit transfer log |

### Donor Front Office pages

| Route | Purpose |
|-------|---------|
| `/dashboard/donor/audit-trail` | read-only donor audit history |
| `/dashboard/donor/alerts` | read-only donor alert timeline |
| `/dashboard/donor/alerts/{donorAlertId}` | read-only donor alert detail |

### Alerts workspace pages

| Route | Purpose |
|-------|---------|
| `/dashboard/alerts` | alerts BO listing + filters + pagination |
| `/dashboard/alerts/new` | create alert |
| `/dashboard/alerts/{donorAlertId}` | alert BO detail |
| `/dashboard/alerts/{donorAlertId}/edit` | alert BO edit |

---

## 7. Shared template normalization delivered

The final merged copy now exposes one coherent template system:

### Shared shells

- `templates/dashboard/layouts/back_office.html.twig`
- `templates/dashboard/layouts/front_office.html.twig`
- `templates/auth/index.html.twig`

### Shared partials

- `templates/shared/_flash.html.twig`
- `templates/shared/_pagination.html.twig`
- `templates/shared/_confirm_modal.html.twig`

### Shared assets

- `public/styles/normalized-ui.css`
- `public/styles/auth-shell.css`
- `public/scripts/shared-shell.js`
- `public/scripts/auth-shell.js`

### Module assets

- `public/styles/log-management-tailadmin.css`
- `public/styles/log-management-overrides.css`
- `public/scripts/log-management-confirm.js`

The old duplicated page shells were removed from the normalized pages. Existing migration-branch pages now sit inside the shared BO/FO shells instead of each page carrying its own full sidebar and header structure.

---

## 8. Other normalized pages kept in the merged copy

The merged copy also keeps the migration branch pages and aligns them to the shared shells:

| Area | Route family |
|------|--------------|
| User management | `/dashboard/users` |
| Donor eligibility | `/dashboard/donor-eligibility` |
| Blood inventory | `/dashboard/inventory` |
| Operational transfers | `/dashboard/transfers` |
| Donor home | `/dashboard/donor` |
| Donor articles | `/dashboard/donor/articles` |
| Donor tips | `/dashboard/donor/tips` |

These pages keep their existing business behavior unless the merge required shell/session compatibility changes.

---

## 9. Filtering and statistics delivered

The Audit Logs workspace includes:

- keyword search
- start date filter
- end date filter
- action filter
- status filter
- pagination
- recent-activity ordering
- hospital-scoped aggregate cards for staff

The Alerts workspace keeps its own lifecycle filters and CRUD. The Audit Logs overview stays lightweight and does not carry the alerts chart or donor-response summary.

---

## 10. Delete UX and error handling

- Audit delete actions use the integrated in-page confirmation modal, not the browser's raw `confirm()` dialog.
- Branded 403 / 404 / 500 templates exist under `templates/error/`.
- In Symfony debug mode, the framework exception screen overrides those templates. In non-debug mode the branded templates take over.

---

## 11. What to say if asked “what proves the professor requirements were followed?”

Short answer:

1. the app uses shared BO and FO shells rather than isolated pages
2. the audit module provides real CRUD on reverse-engineered Doctrine entities
3. the entities have relations
4. the forms use server-side validation
5. the pages are linked through one coherent navigation system
6. the repo contains an explicit reverse-engineering command and script as workshop proof

---

## 12. Verification commands

```bash
php bin/console doctrine:mapping:info
php bin/console lint:twig templates
php bin/console app:reverse-engineer:entities --help
php bin/console doctrine:query:sql "SELECT 1"
```
