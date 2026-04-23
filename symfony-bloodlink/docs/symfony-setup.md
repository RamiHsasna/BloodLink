# BloodLink Symfony Setup And Run Guide

This is the concrete handoff runbook for the integrated Symfony delivery used in the PI-DEV first validation.

## What This Project Expects

- PHP 8.1 or higher
- Composer
- access to the shared Supabase PostgreSQL database
- internet access for Composer package installation

## 1. Open The Project

Work from the Symfony folder root:

```bash
cd symfony-bloodlink
```

## 2. Create The Local Environment File

The delivery zip does not include `.env` on purpose. That avoids shipping the live database connection.

Create `.env` from `.env.example` first:

```bash
copy .env.example .env
```

Then open `.env` and set `DATABASE_URL` to the shared Supabase PostgreSQL connection string.

For the real demo run, paste the working `DATABASE_URL` received from the project owner or from the secured local handoff copy. Do not store the live credential in markdown or commit it into the delivered package.

Example shape:

```dotenv
DATABASE_URL="postgresql://postgres:<password>@<host>:5432/postgres?serverVersion=15&charset=utf8&sslmode=require"
```

Important:

- keep `postgresql://`
- keep `serverVersion=15`
- keep `charset=utf8`
- keep `sslmode=require`
- point to the same Supabase project used by the Java side

More details are documented in [Supabase Integration](supabase-integration.md).

## 3. Install PHP Dependencies

Run Composer only after `.env` exists, because Symfony auto-scripts boot the kernel during install.

```bash
composer install
```

If `vendor/` already exists from a previous run, this command is still safe to run.

## 4. Verify The Database Before Running

Run these checks in order:

```bash
php bin/console doctrine:query:sql "SELECT 1"
php bin/console doctrine:mapping:info
php bin/console lint:twig templates
```

Expected result:

- the SQL check returns `1`
- Doctrine mapping resolves all entities without repository errors
- Twig lint passes cleanly

## 5. Start The Local Server

Use the PHP built-in server:

```bash
php -S 127.0.0.1:8000 -t public
```

Open:

- `http://127.0.0.1:8000/auth`

Stop the server with `Ctrl+C`.

## 6. Sign In With Demo Accounts

Use the accounts documented in [Demo Accounts](demo-accounts.md).

Recommended demo order:

1. Admin for Back Office navigation and CRUD
2. Hospital staff for hospital-scoped behavior
3. Donor for Front Office pages such as `My Audit Trail`, alerts, and donation events

## 7. Validation Walkthrough

After sign-in, verify these pages:

### Back Office

- `/dashboard/users`
- `/dashboard/donor-eligibility`
- `/dashboard/inventory`
- `/dashboard/transfers`
- `/dashboard/alerts`
- `/dashboard/logs`
- `/dashboard/donation-events`

### Donor Front Office

- `/dashboard/donor`
- `/dashboard/donor/audit-trail`
- `/dashboard/donor/alerts`
- `/dashboard/donor/events`
- `/dashboard/donor/articles`
- `/dashboard/donor/tips`

## 8. Notes About The Current Delivery

- `Audit Logs` and `Alerts` are separate workspaces
- donor users stay in Front Office only
- hospital staff is scoped to the staff hospital
- this project is database-first and reuses the shared PostgreSQL schema already used by the Java application
- if a local helper script is ever needed from this workspace, run it with `py`

## Troubleshooting

### Database connection fails

Check:

- `.env` exists and was created from `.env.example`
- `.env` contains the correct `DATABASE_URL`
- the Supabase password and host are correct
- `sslmode=require` is still present

### Composer install fails during auto-scripts

This usually means Symfony tried to boot without a valid `.env`.

Fix order:

```bash
copy .env.example .env
composer install
```

If you already ran `composer install` once and it failed, create `.env` and run `composer install` again.

### Doctrine mapping fails

Run:

```bash
php bin/console doctrine:mapping:info
```

If an entity fails here, the app should not be demoed before that mapping issue is fixed.

### Port 8000 is already used

Run:

```bash
php -S 127.0.0.1:8001 -t public
```

Then open `http://127.0.0.1:8001/auth`.
