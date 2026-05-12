# BloodLink JavaFX - Clean Run Guide

## Requirements

- Java 21 or newer
- Maven 3.9 or newer
- Network access to the configured PostgreSQL/Supabase database

## Configure

This delivery includes `config.properties` already configured for inspection.

If the owner needs to rotate or replace credentials later, edit:

```properties
BLOODLINK_DB_URL=
BLOODLINK_DB_USER=
BLOODLINK_DB_PASSWORD=
BLOODLINK_IA_PROVIDER=openrouter
BLOODLINK_AUDIT_IA_PROVIDER=openrouter
BLOODLINK_DONATION_IA_PROVIDER=anthropic
BLOODLINK_OPENROUTER_API_KEY=
BLOODLINK_OPENROUTER_MODEL=openrouter/free
BLOODLINK_ANTHROPIC_API_KEY=
```

Environment variables can also be used. Real environment variables override file values.

## Run

From the project folder:

```powershell
mvn clean compile
mvn javafx:run
```

## Quick Inspection

1. Log in with an admin account.
2. Open each visible sidebar page once.
3. Check `Blood Inventory`: inventory cards load and there is no quick actions placeholder section.
4. Check `Blood Types`: list, search, add, edit, delete, refresh.
5. Check `Audit Logs`: donation/transfer logs load, `Analyse IA` uses OpenRouter, and adding a donation log uses a donation dropdown instead of manual UUID entry.
6. Check `Donations` AI prediction: Claude is tried first; if Claude is unavailable, OpenRouter is used as fallback.

## Notes

- `config.properties` is included so the app runs immediately.
- Keep `config.properties.example` as the blank template for future setup or credential rotation.
