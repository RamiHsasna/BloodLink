# BloodLink — JavaFX Desktop App

Standalone Maven + JavaFX desktop client. This folder is **independent** from `symfony-bloodlink/` and can be opened, built, and run on its own.

## Layout

| Path | What it is |
|------|------------|
| `pom.xml` | Maven build descriptor (Java 21, JavaFX 21.0.2, PostgreSQL JDBC, iText, ZXing) |
| `src/WorkshopJDBC/Main.java` | JavaFX main class (entry point, declared in `pom.xml` `<mainClass>`) |
| `src/tn/edu/esprit/...` | Application packages (entities, services, controllers, FXML, etc.) |
| `lib/` | Extra jars bundled outside Maven (`core-3.5.2.jar`, `javase-3.5.2.jar`) |
| `.deps/` | Reserved folder for additional vendored dependencies |
| `config.properties` | DB connection + AI provider keys (already filled in) |
| `config.properties.example` | Safe template for teammates |
| `.env.example` | Alternative env-var format for the same keys |
| `BloodLink.iml`, `.idea/` | IntelliJ IDEA project descriptors |

## Prerequisites

- **JDK 21** (Oracle, Temurin, or Zulu — anything supporting Java 21)
- **Maven 3.9+** (only if you run from the command line; IntelliJ ships with its own)
- A PostgreSQL connection (the project ships configured to talk to the same Supabase instance as the Symfony web app)

## Run from IntelliJ (recommended)

1. **File → Open** and pick the `javafx-bloodlink` folder.
2. Wait for IntelliJ to import the Maven project (it will download dependencies on first open).
3. Open the Maven tool window → `bloodlink-javafx → Plugins → javafx → javafx:run` → double-click.
4. The desktop window opens. Sign in with the same demo accounts as the web app:
   - Admin: `admin@admin.com` / `admin123654`
   - Hospital staff: `yassine@staff.com` / `7894561230`
   - Donor: `user1@gmail.com` / `123456789`

If you prefer the green "Run" arrow, right-click `WorkshopJDBC.Main` → **Run 'Main.main()'**.

## Run from the command line

```powershell
cd C:\Users\dell\Desktop\BloodLink-master\javafx-bloodlink
mvn clean compile
mvn javafx:run
```

The first `mvn clean compile` downloads JavaFX, the PostgreSQL driver, iText, and ZXing into your local Maven cache. Subsequent runs are fast.

## Configuration

Database and AI provider credentials live in `config.properties`. To regenerate the file from the template:

```powershell
Copy-Item config.properties.example config.properties
notepad config.properties
```

Fill in:

- `BLOODLINK_DB_URL` — JDBC URL, e.g. `jdbc:postgresql://aws-1-eu-central-1.pooler.supabase.com:6543/postgres?sslmode=require`
- `BLOODLINK_DB_USER` / `BLOODLINK_DB_PASSWORD` — Supabase pooler credentials
- `BLOODLINK_*_API_KEY` — optional AI provider keys (OpenRouter / Gemini / Groq / Anthropic). Leave blank to disable AI features.

## Build a distributable jar

```powershell
mvn clean package
```

The shaded jar lands in `target/`. Run with:

```powershell
java --module-path "<path-to-javafx-sdk-21.0.2>\lib" --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing -jar target\bloodlink-javafx-1.0.0.jar
```

## Relationship to the Symfony web app

This client and the Symfony app under `../symfony-bloodlink/` share **the same PostgreSQL database** but are otherwise completely independent — different tech stacks, different processes. You can run either one on its own without touching the other.
