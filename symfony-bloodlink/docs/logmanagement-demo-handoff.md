# Audit Logs and Alerts — Demo Handoff Guide

This guide is for the final merged Symfony copy only. It explains how to run the app locally, which accounts to use, and which routes to show during the professor demo.

---

## 1. What is included in this copy

| Area | Included |
|------|----------|
| Shared Back Office shell | Yes |
| Shared Donor Front Office shell | Yes |
| Dedicated auth shell | Yes |
| Audit Logs overview | Yes |
| Donation logs CRUD | Yes |
| Transfer logs CRUD | Yes |
| Alerts workspace | Yes |
| Donor alert timeline + detail | Yes |
| Donor My Audit Trail | Yes |
| Existing migration pages kept and normalized | Users · Donor Eligibility · Blood Inventory · Transfers · Donor Home · Articles · Tips |
| Reverse-engineering proof | `reverse-engineering.php` and `php bin/console app:reverse-engineer:entities --help` |

---

## 2. Prerequisites

| Requirement | Version |
|-------------|---------|
| PHP | 8.1+ |
| Composer | 2.x |
| PostgreSQL connection | Provided through `DATABASE_URL` |

This copy already reads its DB connection from the Symfony environment. If the DB URL is moved out of `.env`, provide it again through `.env.local` or a machine environment variable before running the app.

---

## 3. Exact local run steps

Open a terminal in the project root:

```bash
cd symfony-bloodlink
```

Install dependencies:

```bash
composer install
```

Verify Doctrine mapping:

```bash
php bin/console doctrine:mapping:info
```

Verify Twig syntax:

```bash
php bin/console lint:twig templates
```

Optional DB smoke check:

```bash
php bin/console doctrine:query:sql "SELECT 1"
```

Start the local server:

```bash
php -S 127.0.0.1:8000 -t public
```

Open:

```text
http://127.0.0.1:8000/auth
```

---

## 4. Demo accounts

See [demo-accounts.md](demo-accounts.md).

Quick reference:

| Role | Email | Password | Main landing page |
|------|-------|----------|-------------------|
| Admin | `admin@admin.com` | `admin123654` | `/dashboard/users` |
| Hospital Staff | `yassine@staff.com` | `7894561230` | `/dashboard/users` |
| Donor | `user1@gmail.com` | `123456789` | `/dashboard/donor` |

---

## 5. Suggested walkthrough

1. Open `/auth` and show the dedicated sign-in page.
2. Sign in as admin.
3. Open `/dashboard/users` and show the shared Back Office shell.
4. Open `/dashboard/logs` and show:
   - summary cards
   - recent donation and transfer audit activity
5. Open `/dashboard/logs/donations` and demonstrate:
   - keyword search
   - start/end date filtering
   - action filter
   - status filter
   - pagination
6. Open one donation log detail page, then its edit page.
7. Explain that on existing audit records the snapshot fields stay locked and only the allowed fields remain editable.
8. Open `/dashboard/logs/transfers` and show the same CRUD/filter flow for transfer audits.
9. Open `/dashboard/alerts` and show the separate alerts workspace.
10. Sign out and sign in as hospital staff.
11. Open `/dashboard/logs` again and point out that the audit module is hospital-scoped for staff. The sidebar session card also shows the live hospital name from the dataset.
12. Open `/dashboard/alerts` again and show that alerts are also limited to the staff hospital.
13. Sign out and sign in as donor.
14. Open `/dashboard/donor`, then `/dashboard/donor/audit-trail`, then `/dashboard/donor/alerts`, then one alert detail page.
15. Explain that the current seeded donor demo account is strongest on alerts; the audit-trail page is part of the FO delivery and role separation, but its data volume depends on whether the selected donor already has donation-linked history in the database.
16. Show that donor routes are read-only and stay inside the donor FO shell.

---

## 6. Route map to know during the demo

### Authentication

| Route | Purpose |
|-------|---------|
| `/auth` | sign-in / sign-up page |
| `/auth/sign-in` | process sign-in |
| `/auth/sign-up` | create donor account |
| `/auth/logout` | sign out |

### Back Office

| Route | Purpose |
|-------|---------|
| `/dashboard/users` | existing user management page |
| `/dashboard/donor-eligibility` | donor eligibility workspace |
| `/dashboard/inventory` | blood inventory page |
| `/dashboard/transfers` | operational transfers page |
| `/dashboard/logs` | audit overview |
| `/dashboard/logs/donations` | donation audit list |
| `/dashboard/logs/donations/new` | create donation audit |
| `/dashboard/logs/donations/{logId}` | donation audit detail |
| `/dashboard/logs/donations/{logId}/edit` | donation audit edit |
| `/dashboard/logs/transfers` | transfer audit list |
| `/dashboard/logs/transfers/new` | create transfer audit |
| `/dashboard/logs/transfers/{logId}` | transfer audit detail |
| `/dashboard/logs/transfers/{logId}/edit` | transfer audit edit |
| `/dashboard/alerts` | alerts BO list |
| `/dashboard/alerts/new` | create alert |
| `/dashboard/alerts/{donorAlertId}` | alert BO detail |
| `/dashboard/alerts/{donorAlertId}/edit` | alert BO edit |

### Donor Front Office

| Route | Purpose |
|-------|---------|
| `/dashboard/donor` | donor home |
| `/dashboard/donor/audit-trail` | personal donor audit trail |
| `/dashboard/donor/alerts` | donor alert timeline |
| `/dashboard/donor/alerts/{donorAlertId}` | donor alert detail |
| `/dashboard/donor/articles` | donor articles |
| `/dashboard/donor/tips` | donor health tips |

---

## 7. Verification commands

Run these before handing over the folder:

```bash
php bin/console doctrine:mapping:info
php bin/console lint:twig templates
php bin/console app:reverse-engineer:entities --help
php bin/console doctrine:query:sql "SELECT 1"
```

---

## 8. Important notes

- The audit module is fully integrated into the migration copy. There is no dependency on the older reference folder.
- Audit Logs and Alerts are separate workspaces.
- Admin sees all audit data and all alerts.
- Hospital staff sees only hospital-scoped audit data and hospital-scoped alerts.
- Donor pages are read-only and include My Audit Trail plus personal alerts.
- Custom branded 403/404/500 templates are present in `templates/error/`, but when Symfony runs in debug mode the framework exception screen takes priority over those templates. That is normal for `APP_ENV=dev`.
