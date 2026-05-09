# BloodLink Symfony

This repository is the merged Symfony copy of BloodLink used for the PI-DEV first validation. It is not just an API boilerplate anymore: it now includes a working Back Office, a donor-facing Front Office, authentication, shared Twig layouts, and the integrated `Audit Logs` workspace.

## Current Scope

The delivered Symfony app currently includes:

- authentication with role-based redirection
- shared Back Office and Front Office layouts
- users, donor eligibility, blood inventory, transfers, alerts, and donor content pages
- the integrated `Audit Logs` module for donation and transfer traceability
- a donor-facing `My Audit Trail` page
- reverse-engineered Doctrine entities aligned with the PostgreSQL / Supabase database

## Main Workspaces

### Back Office

- `/dashboard/users`
- `/dashboard/donor-eligibility`
- `/dashboard/inventory`
- `/dashboard/transfers`
- `/dashboard/alerts`
- `/dashboard/logs`

### Donor Front Office

- `/dashboard/donor`
- `/dashboard/donor/audit-trail`
- `/dashboard/donor/alerts`
- `/dashboard/donor/articles`
- `/dashboard/donor/tips`

## Quick Start

### 1. Create `.env`

The delivery zip intentionally does not include `.env`.

```bash
copy .env.example .env
```

Then update `DATABASE_URL` inside `.env`.
Use the Supabase/PostgreSQL format with:

- `serverVersion=15`
- `charset=utf8`
- `sslmode=require`

### 2. Install dependencies

```bash
composer install
```

### 3. Verify the setup

```bash
php bin/console doctrine:query:sql "SELECT 1"
php bin/console doctrine:mapping:info
php bin/console lint:twig templates
```

### 4. Run the app

```bash
php -S 127.0.0.1:8000 -t public
```

Then open:

- `http://127.0.0.1:8000/auth`

For the full delivery runbook, read:

- [Symfony Setup](docs/symfony-setup.md)
- [Supabase Integration](docs/supabase-integration.md)
- [Demo Accounts](docs/demo-accounts.md)

## Documentation Map

All project-facing documentation lives under [`docs/`](docs/).

- [Documentation Index](docs/README.md)
- [Symfony Setup](docs/symfony-setup.md)
- [Supabase Integration](docs/supabase-integration.md)
- [Development Guide](docs/development-guide.md)
- [API Documentation](docs/api-documentation.md)
- [Demo Accounts](docs/demo-accounts.md)
- [Audit Logs Module Spec](docs/logmanagement-module-spec.md)
- [Audit Logs Integration Note](docs/audit-logs-integration.md)

## Reverse-Engineering Proof

This Symfony copy keeps visible proof that the entity layer follows a database-first workflow:

- `reverse-engineering.php`
- `php bin/console app:reverse-engineer:entities --help`

The generated entity layer was then repaired only where needed for Symfony correctness and module delivery.

## Verification Commands

```bash
php bin/console doctrine:mapping:info
php bin/console lint:twig templates
php bin/console app:reverse-engineer:entities --help
```

## Notes

- `Audit Logs` and `Alerts` are separate workspaces.
- `DONOR` users stay in Front Office only.
- `HOSPITAL_STAFF` is scoped to the staff hospital.
- If a local helper script is ever needed from this workspace, use `py`.
