# Audit Logs Module Ultra Review And Execution Plan

Status: planning only. No implementation files have been edited for this plan.

Primary target: BloodLink Audit Logs module only.

Companion plan: `docs/plans/2026-04-28-audit-anomaly-detection.md`

## Inputs Reviewed

- Existing Antigravity plan artifact for Audit Logs improvements and AI anomaly detection.
- Current BloodLink plan at `docs/plans/2026-04-28-audit-anomaly-detection.md`.
- BloodLink module docs: audit integration note, module spec, demo handoff, developer checklist.
- `mds` guidance: BloodLink is not ORBIT product; PIDEV AI grading expects a visible, relevant, valuable UI feature, preferably structured and traceable.
- Local git history for the paid BloodLink work:
  - `cf4fb92`: Java/reference BloodLink project import.
  - `26efe91`: moved from Java reference toward Symfony paid task.
  - `786df3c`: phase 1 + 2 of Symfony audit module, with controller, entities, forms, repositories, templates.
  - `8365124`: last edits before first validation, shared shells, delete modal, validation repairs.
  - `39f4b6e`: audit logs module delivery and handoff docs.
  - `363d3ce`: merged/integrated BloodLink delivery into the current `other_project_refs` location.

## Current Module From Scratch

The Audit Logs module is a traceability workspace for two audit families:

- donation status transitions
- inter-hospital transfer request status transitions

It also exposes a donor-facing read-only "My Audit Trail" page. Alerts are documented as a separate workspace and should not be folded back into Audit Logs.

### Users And Visibility

- `ADMIN`: full Back Office visibility for audit records.
- `HOSPITAL_STAFF`: Back Office visibility scoped to the staff hospital.
- `DONOR`: Front Office read-only audit trail only.

The module depends on the session payload created at sign-in: id, email, first name, last name, user type, hospital id, and hospital name.

### Current Route Surface

Back Office:

- `GET /dashboard/logs`
- `GET /dashboard/logs/donations`
- `GET|POST /dashboard/logs/donations/new`
- `GET /dashboard/logs/donations/{logId}`
- `GET|POST /dashboard/logs/donations/{logId}/edit`
- `POST /dashboard/logs/donations/{logId}/delete`
- `GET /dashboard/logs/donations/{logId}/export-pdf`
- `GET /dashboard/logs/transfers`
- `GET|POST /dashboard/logs/transfers/new`
- `GET /dashboard/logs/transfers/{logId}`
- `GET|POST /dashboard/logs/transfers/{logId}/edit`
- `POST /dashboard/logs/transfers/{logId}/delete`
- `GET /dashboard/logs/transfers/{logId}/export-pdf`
- `POST /dashboard/logs/translate`
- `POST /dashboard/logs/summarize`
- `POST /dashboard/logs/chatbot`

Front Office:

- `GET /dashboard/donor/audit-trail`

The translation, summarizer, and chatbot routes are the current concern. Translation must be wiped. The summarizer/chatbot are not the requested smart feature and must either be removed or rebuilt as anomaly-specific, scoped review behavior.

## Current Module Structure

Only module-owned or module-touched layers are listed.

```text
docs/
  audit-logs-integration.md
  logmanagement-demo-handoff.md
  logmanagement-module-spec.md
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
    OpenRouterClient.php

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
  dashboard/layouts/
    back_office.html.twig
    front_office.html.twig
  shared/
    _confirm_modal.html.twig
    _flash.html.twig
    _pagination.html.twig

public/
  scripts/
    log-management-confirm.js
    shared-shell.js
  styles/
    log-management-overrides.css
    log-management-tailadmin.css
    normalized-ui.css

config/
  services.yaml
  packages/
    messenger.yaml
    notifier.yaml

project root:
  composer.json
  composer.lock
  .env.example
  phpunit.dist.xml
```

## Layer Review

### Controller Layer

`LogManagementController` currently owns too much:

- CRUD orchestration for donation logs
- CRUD orchestration for transfer logs
- dashboard summary/chart building
- recent activity composition
- access checks
- UUID validation
- PDF export construction
- translation endpoint
- AI summary endpoint
- donor audit trail composition

Planning decision:

- Keep the controller as the route owner.
- Move AI anomaly detection, SMS delivery, PDF generation, and assessment payload building out of the controller.
- Do not split every CRUD action into new controllers unless implementation becomes harder to read. Surgical service extraction is enough.

### Entity Layer

Current audit entities:

- `DonationLog`
- `BloodTransferRequestLog`

Strengths:

- Doctrine relations exist.
- server-side choice constraints exist.
- status transition callback prevents same previous/new status.
- snapshot fields are locked by form options after creation.

Gaps:

- no module-owned place exists to store AI anomaly result, severity, reasons, SMS state, or assessment timestamp.
- existing audit logs cannot show anomaly state without either a schema addition or a separate anomaly table.

Planning decision:

- Add a separate anomaly assessment record instead of overloading notes or mutating the two audit entities with many nullable AI fields.

### Repository Layer

Current repositories handle:

- keyword/date/action/status filters
- pagination
- hospital-scoped aggregate counts
- daily chart counts
- recent detailed entries
- donor-specific detail/count queries

Strengths:

- hospital-scoped search exists for both donation and transfer logs.
- dashboard chart data is aggregated in repositories instead of templates.

Gaps:

- anomaly counts and filters do not exist.
- chatbot currently fetches latest logs without hospital scoping.
- dashboard aggregation logic is partly in the controller.

Planning decision:

- Add anomaly-aware query helpers.
- Keep charts server-rendered unless Chart.js is already required for another BloodLink surface; current CSS-based charting is acceptable.

### Form And Validation Layer

Current forms:

- lock actor/snapshot fields after creation
- restrict actor choices to Back Office user types
- restrict donation/transfer choices by hospital for staff
- use entity constraints for status/action choices

Gaps:

- invalid date ranges on filters are silently ignored.
- creation flashes do not report anomaly assessment result.
- no tests currently prove staff cannot create logs against out-of-scope relations.

Planning decision:

- Keep forms mostly intact.
- Add only targeted validation/UI feedback needed for anomaly behavior and scoping confidence.

### Template And Frontend Layer

Current Back Office templates provide:

- overview dashboard cards
- donation/transfer tables
- filters and pagination
- detail pages
- create/edit forms
- shared confirmation modal
- module CSS overrides

Current Front Office template provides:

- donor audit summary cards
- donation history
- linked audit activity
- transfer context

Current drift:

- inline AI summary CSS/JS duplicated in both detail templates.
- floating chatbot CSS/JS is embedded directly inside the module layout.
- inline styles and emoji-like labels reduce maintainability and polish.

Planning decision:

- Move anomaly-specific UI behavior into module CSS/JS assets.
- Show anomaly state in dashboard/list/detail views.
- Keep donor audit trail read-only and avoid exposing internal anomaly reasoning unless explicitly summarized.

### AI And External Services Layer

Current drift:

- `OpenRouterClient` is actually configured for NVIDIA NIM.
- `summarizeLog` trusts client-submitted JSON instead of loading the log from the database.
- `AuditChatbotController` sends latest donation/transfer logs to AI without hospital scoping.
- translation still exists through a raw cURL call to MyMemory.

Planning decision:

- Remove translation completely.
- Retire or replace generic summarizer/chatbot behavior.
- Build one real AI feature: anomaly detection on saved audit entries, using trusted server-built payloads.
- Use deterministic risk rules first, AI second.
- Parse AI as structured JSON and fail safely.

### SMS Layer

Current stack already has Symfony HttpClient and Notifier/Messenger configuration, but no dedicated Twilio implementation is present.

Planning decision:

- Use a small Twilio SMS service backed by Symfony HttpClient.
- Configure credentials only through env variables.
- Provide dry-run/no-credentials behavior for local verification.
- Do not commit real phone numbers, SIDs, tokens, or API keys.

### Tests And Verification Layer

Current `tests/` contains only bootstrap. There is no real module test suite yet.

Planning decision:

- Add focused tests only where risk justifies it:
  - anomaly detector parsing/fallback behavior
  - Twilio notifier dry-run and request construction
  - access/scoping behavior for AI/anomaly routes if functional tests are practical
- Always run syntax, route, Twig, container, and Doctrine checks after implementation.

## Major Review Findings To Fix During Implementation

1. Translation is still present and must be removed.
   - It is not the requested smart feature.
   - It uses raw cURL and a third-party translation endpoint directly from the controller.
   - The translation dependency should be removed if unused after cleanup.

2. The current AI summary route is not a serious audit feature.
   - It summarizes client-submitted data, not trusted database records.
   - It is not the anomaly detection feature requested by the client.
   - If any summary remains, it must load the real log by ID and respect hospital scope.

3. The current chatbot draft is a hospital-scope risk.
   - It fetches recent logs globally.
   - Hospital staff could receive context outside their hospital.
   - It is generic query behavior, not the requested anomaly workflow.

4. PDF export needs hardening before keeping it.
   - Transfer PDF currently references a non-existent transfer request property in the template plan path.
   - PDF generation is duplicated in controller actions.
   - Export should be moved behind a small service and checked for both donation and transfer records.

5. Dashboard/chart data should grow around anomaly insight.
   - Current mix and 7-day activity charts are useful.
   - They should be complemented with anomaly severity counts, alert-sent counts, and recent high-risk entries.

6. External services must be fail-open for CRUD.
   - AI and SMS failures should not block audit log creation.
   - Failure state must be observable through flash, log, or stored assessment error state.

## Proposed Target Module Shape

After implementation, the module should contain:

```text
src/
  Controller/
    LogManagementController.php
  Entity/
    DonationLog.php
    BloodTransferRequestLog.php
    AuditLogAnomaly.php
  Repository/
    DonationLogRepository.php
    BloodTransferRequestLogRepository.php
    AuditLogAnomalyRepository.php
  Service/
    AuditLogContextBuilder.php
    AuditAnomalyDetector.php
    AuditLogAnomalyRecorder.php
    AuditLogPdfExporter.php
    TwilioSmsNotifier.php
    OpenRouterClient.php or renamed provider client
```

The exact class names can still be adjusted during implementation, but the responsibilities should stay separate.

## Execution File List

### Planning And Documentation

- `docs/plans/2026-04-28-audit-anomaly-detection.md`
- `docs/plans/2026-04-28-audit-log-management-ultra-review.md`
- `docs/logmanagement-module-spec.md`
- `docs/logmanagement-demo-handoff.md`
- `docs/audit-logs-integration.md`

### Configuration And Dependencies

- `.env.example`
- `composer.json`
- `composer.lock`
- `config/services.yaml`
- `config/packages/notifier.yaml`
- `config/packages/messenger.yaml`

### Backend Controllers

- `src/Controller/LogManagementController.php`
- `src/Controller/AuditChatbotController.php`

Expected action:

- Keep `LogManagementController` as route owner, but slim it.
- Delete or fully repurpose `AuditChatbotController`; do not keep a generic global-context chatbot.

### Entities And Database

- `src/Entity/DonationLog.php`
- `src/Entity/BloodTransferRequestLog.php`
- `src/Entity/AuditLogAnomaly.php`
- `migrations/Version20260428*.php` or an explicit SQL migration note if this project stays database-first.

Expected action:

- Prefer a new anomaly entity/table.
- Avoid storing anomaly state in free-text notes.

### Repositories

- `src/Repository/DonationLogRepository.php`
- `src/Repository/BloodTransferRequestLogRepository.php`
- `src/Repository/AuditLogAnomalyRepository.php`
- `src/Repository/DonationRepository.php`
- `src/Repository/BloodTransferRequestRepository.php`

Expected action:

- Add anomaly-aware counts/lookups.
- Preserve existing hospital scoping.
- Avoid global recent-log reads for staff AI context.

### Forms

- `src/Form/DonationLogType.php`
- `src/Form/BloodTransferRequestLogType.php`

Expected action:

- Keep current form ownership.
- Add only targeted refinements if implementation discovers scope or validation gaps.

### Services

- `src/Service/OpenRouterClient.php`
- `src/Service/AuditLogContextBuilder.php`
- `src/Service/AuditAnomalyDetector.php`
- `src/Service/AuditLogAnomalyRecorder.php`
- `src/Service/AuditLogPdfExporter.php`
- `src/Service/TwilioSmsNotifier.php`

Expected action:

- AI provider code should return structured, parseable output.
- anomaly detector should be testable with a fake AI client.
- SMS notifier should support dry-run/no-credential local behavior.
- PDF export should leave the controller.

### Back Office Templates

- `templates/log_management/layouts/back_office.html.twig`
- `templates/log_management/dashboard/index.html.twig`
- `templates/log_management/donation_logs/index.html.twig`
- `templates/log_management/donation_logs/show.html.twig`
- `templates/log_management/donation_logs/form.html.twig`
- `templates/log_management/transfer_logs/index.html.twig`
- `templates/log_management/transfer_logs/show.html.twig`
- `templates/log_management/transfer_logs/form.html.twig`
- `templates/log_management/pdf_report.html.twig`

Expected action:

- Remove translation UI.
- Remove or replace summary/chatbot UI.
- Add anomaly badges, severity panels, alert state, and anomaly dashboard cards.
- Correct PDF template fields.

### Front Office Template

- `templates/log_management/front/index.html.twig`

Expected action:

- Keep donor view read-only.
- Do not leak internal anomaly reasoning by default.

### Frontend Assets

- `public/scripts/log-management-confirm.js`
- `public/scripts/audit-anomaly.js`
- `public/styles/log-management-overrides.css`
- `public/styles/log-management-tailadmin.css`

Expected action:

- Keep confirm modal behavior.
- Move inline AI/anomaly scripts out of Twig.
- Keep visible anomaly controls polished, compact, and responsive.

### Tests

- `tests/bootstrap.php`
- `tests/Service/AuditAnomalyDetectorTest.php`
- `tests/Service/TwilioSmsNotifierTest.php`
- `tests/Controller/LogManagementAccessTest.php` if functional DB setup is practical.

Expected action:

- Add focused tests around new risk points instead of broad fragile coverage.

## Implementation Checkpoints

### Checkpoint 1 - Remove Weak Features

- Delete translation behavior and dependency.
- Remove generic chatbot UI/API or quarantine it until it can be scoped and anomaly-specific.
- Keep only code that serves the anomaly plan.

Validation:

- no translation route
- no translation dependency
- no global chatbot context exposure

### Checkpoint 2 - Add Anomaly Data Model

- Add module-owned anomaly record.
- Link anomaly records to audit log type and log id.
- Store severity, score, reasons, explanation, provider/model, assessment time, and SMS state.

Validation:

- Doctrine mapping passes.
- Schema/migration path is explicit.

### Checkpoint 3 - Add Detector And SMS Services

- Build trusted payload from saved entity and session/server context.
- Run deterministic risk rules first.
- Call AI only when configured.
- Parse structured response.
- Send SMS only for configured severity threshold.

Validation:

- normal, suspicious, provider-failure, and SMS-failure cases are covered.

### Checkpoint 4 - Wire CRUD Lifecycle

- Run anomaly detection after successful donation log creation.
- Run anomaly detection after successful transfer log creation.
- Do not block CRUD if AI/SMS fails.
- Flash or display assessment outcome after redirect.

Validation:

- create donation log still redirects to detail.
- create transfer log still redirects to detail.
- anomaly state appears on detail pages.

### Checkpoint 5 - Tighten Dashboard, Lists, Details

- Add anomaly overview cards.
- Add severity distribution or recent high-risk panel.
- Add anomaly badges on list rows.
- Add detail explanation panel.
- Correct and harden PDF export if retained.

Validation:

- dashboard remains readable with no anomaly data.
- list filters still work.
- detail pages show anomaly result without layout overlap.

### Checkpoint 6 - Final Verification

Run:

```bash
php -l src/Controller/LogManagementController.php
php bin/console debug:router | findstr dashboard_logs
php bin/console lint:twig templates/log_management
php bin/console lint:container
php bin/console doctrine:mapping:info
php bin/console doctrine:schema:validate --skip-sync
php bin/phpunit
```

Manual smoke:

- Back Office audit overview
- donation logs list, create, detail, edit, delete confirmation
- transfer logs list, create, detail, edit, delete confirmation
- PDF export if retained
- anomaly state on dashboard, list, and detail
- donor audit trail remains read-only

## Success Criteria

- The first plan stays high-level and no longer contains file-edit details.
- This second plan owns the file-level implementation map.
- Translation is removed.
- The smart feature is anomaly detection with explainable AI output and SMS alerting.
- The module remains scoped to Audit Logs, with no unrelated BloodLink modules edited except required relation/config support.
- The client can understand the full module: what it contains, how it is structured, where it is visible, and why the AI feature is serious.
