# Supabase Integration

This Symfony delivery is connected to the same PostgreSQL data source used by the BloodLink Java application. In practice, that means the Symfony side is not running on a separate local schema: it reads and writes against the shared Supabase PostgreSQL project.

## How The Integration Was Done

### 1. Shared PostgreSQL Database

- the Java application and the Symfony application both target the same Supabase PostgreSQL database
- the Symfony app connects through `DATABASE_URL` in `.env`
- the connection string must include `serverVersion=15`
- the connection string should keep `charset=utf8`
- SSL is required in the connection string

Expected shape:

```dotenv
DATABASE_URL="postgresql://postgres:<password>@<host>:5432/postgres?serverVersion=15&charset=utf8&sslmode=require"
```

For the live demo, the actual credential should be copied into `.env` locally from a secure handoff source. It should not be embedded in markdown files or redistributed inside the delivery archive.

### 2. Database-First Entity Layer

The Symfony entity layer follows a reverse-engineering workflow instead of inventing a new schema.

Visible proof kept in the project:

- `reverse-engineering.php`
- `php bin/console app:reverse-engineer:entities --help`

That generated layer was then repaired only where Symfony correctness required it.

### 3. Doctrine Repairs Applied On Top Of The Generated Layer

The important fixes made after reverse engineering were:

- repository namespace repair from invalid `App\Entity\Repository\...` references to `App\Repository\...`
- creation of the missing `BloodTransferRequestLogRepository`
- repair of `DonorAlert` so it uses real Doctrine relations instead of raw foreign-key fields as business fields

This keeps the entity layer aligned with the shared Supabase schema while still being usable in Symfony.

### 4. UUID-Based Records

Most business tables use UUID primary keys. Symfony keeps those UUIDs as the real record identifiers instead of replacing them with local numeric IDs.

This is why detail pages can display identifiers such as:

- log UUID
- donation UUID
- actor UUID
- event UUID

They are the actual database identifiers coming from Supabase.

### 5. No Separate Local Migration Bootstrap For The Demo

For this delivery:

- the database schema already exists in Supabase
- Symfony was wired to that existing schema
- the project is demonstrated against real shared data

So the normal demo flow is:

1. configure `DATABASE_URL`
2. verify connectivity
3. run the Symfony server

Not:

1. create a brand-new local schema
2. migrate from zero
3. seed a different demo-only dataset

### 6. Module Integration Against Shared Data

The integrated Symfony copy uses Supabase-backed data for:

- users and auth-related profile context
- hospitals and hospital scoping
- donations and donation logs
- transfer requests and transfer logs
- alerts
- donor-facing traceability pages
- donation events

This is why role guardrails and hospital scoping were implemented in Symfony at the query and controller level instead of by duplicating tables.

## Verification Commands

Run these commands after setting `DATABASE_URL`:

```bash
php bin/console doctrine:query:sql "SELECT 1"
php bin/console doctrine:mapping:info
php bin/console app:reverse-engineer:entities --help
```

If those checks pass, the Supabase integration is correctly wired for the delivered app.
