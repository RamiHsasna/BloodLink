# Audit Logs High-Level Improvement Plan

Status: tightened on 2026-04-28 after module review.

This is the high-level contract only. The file-by-file execution map lives in the companion ultra-review plan. Do not add implementation file lists here.

## Goal

Improve only the BloodLink Audit Logs module by:

- wiping the legacy translation feature
- replacing weak or generic AI additions with a real AI-powered anomaly detection feature
- sending SMS alerts for high-risk audit events
- tightening the existing CRUD, dashboard charts, and visible review surfaces
- preserving the module's current role and hospital-scoping rules

## Scope

In scope:

- Back Office Audit Logs overview
- donation log CRUD
- transfer log CRUD
- donor read-only audit trail
- audit dashboard charts and review indicators
- module-owned AI anomaly detection and SMS alerting
- audit-module documentation and verification notes

Out of scope:

- Alerts workspace behavior
- donation events, inventory, transfers, users, auth, or donor eligibility features except where the audit module reads their relations
- global redesign of the BloodLink application
- generic chatbot behavior
- translation as a "smart" feature
- storing secrets in committed files

## Current Reality Summary

The Audit Logs module is already a real integrated module, not an isolated prototype. It contains:

- role-aware Back Office access for admins and hospital staff
- hospital-scoped donation and transfer audit queries for hospital staff
- donor-facing read-only audit visibility
- forms with server-side status transition validation
- dashboard cards, recent activity, and lightweight audit charts
- shared Back Office and Front Office shells

The current working copy also contains uncommitted drift:

- AI summary endpoint and buttons
- audit chatbot endpoint and floating widget
- PDF export route and template
- legacy translation route still present

That drift must be reviewed before implementation. The requested target is anomaly detection, not translation, and not a generic chatbot.

## Skill And Engineering Posture

Use these principles during implementation:

- Karpathy guidelines: surgical changes, no speculative features, explicit assumptions, verifiable checkpoints.
- Symfony controller cleanup: keep controllers as orchestration only; move AI, SMS, PDF, and dashboard aggregation into services where useful.
- Senior frontend review: make anomaly state visible through clear badges, cards, filters, and chart cues without turning the module into a marketing page.
- Code-review posture: prioritize correctness, role scoping, data privacy, external-service failure behavior, and testability.
- Symfony quality checks: verify routes, Twig, container wiring, Doctrine mapping, and focused PHP syntax before handoff.

## Target Feature

The smart feature is:

**AI Audit Anomaly Detection with SMS Alerting**

Behavior:

1. A new donation or transfer audit log is created through the normal module flow.
2. The module builds a trusted audit payload from the saved entity and server/session context, not from client-submitted JSON.
3. A detector evaluates the event using deterministic rules first, then AI structured analysis when configured.
4. The detector returns a structured result: anomalous or normal, severity, score, reasons, explanation, and recommended action.
5. The result is stored as module-owned anomaly metadata.
6. High-risk or critical results trigger an SMS alert to the configured admin phone number.
7. The dashboard, list pages, and detail pages show the anomaly state visibly.

Important constraints:

- AI or SMS failure must not roll back the audit log creation.
- Real SMS sending must be environment-gated and testable without live credentials.
- The module must not invent facts such as IP reputation unless that data is actually captured.
- The detector must not leak hospital-scoped data across staff accounts.

## Implementation Phases

### Phase 1 - Clean The Existing Drift

- Remove the legacy translation route and any translation UI or dependency.
- Decide whether current AI summary/chatbot work is removed or reworked into the anomaly workflow.
- Keep PDF export only if it is corrected and kept inside module access rules.

Verification:

- Translation route is gone.
- No translation UI remains.
- No generic chatbot is visible unless it becomes anomaly-specific and scoped.

### Phase 2 - Backend Tightening

- Keep donation and transfer CRUD behavior intact.
- Extract repeated infrastructure where it materially reduces risk: PDF export, anomaly payload building, anomaly persistence, and SMS delivery.
- Preserve hospital scoping in all read and write flows.
- Keep status snapshots locked after creation.

Verification:

- Existing donation and transfer CRUD still works.
- Staff users cannot read or create audit entries outside their hospital scope.
- Form validation still blocks invalid status transitions.

### Phase 3 - AI Anomaly Detection

- Add a module-owned anomaly assessment model.
- Run detection after successful audit-log creation.
- Use deterministic checks for obvious cases and AI for structured risk interpretation.
- Store the decision and reasons so the feature is explainable during demo.

Verification:

- A normal log receives a low or normal assessment.
- A suspicious log receives a higher severity assessment with reasons.
- AI provider failure is visible but non-blocking.

### Phase 4 - SMS Alerting

- Add Twilio-compatible SMS delivery through Symfony's HTTP client or the existing notifier boundary.
- Send alerts only for configured severity thresholds.
- Avoid committing real credentials.

Verification:

- In mock/dry-run mode, alert payloads are observable without sending SMS.
- With credentials configured, a high-risk event sends one SMS.
- Repeated assessment does not spam duplicate SMS alerts.

### Phase 5 - Visible Review Surface

- Add anomaly KPIs to the audit overview.
- Add anomaly badges and filters to donation and transfer list pages.
- Add an explanation panel to detail pages.
- Keep charts operational and audit-focused.

Verification:

- Anomalies are immediately visible in Back Office.
- The donor Front Office remains read-only and does not expose internal anomaly reasoning unless intentionally summarized.
- Layout remains responsive and does not overlap existing CRUD controls.

## Verification Plan

Run focused checks after implementation:

- PHP syntax for modified PHP files
- Symfony route inspection for removed/added audit routes
- Twig lint for audit templates
- container lint if service wiring changes
- Doctrine mapping/schema validation if a new entity/table is added
- focused PHPUnit tests for anomaly detector, SMS notifier, and access scoping where practical
- manual browser smoke for audit overview, donation list/detail/create, transfer list/detail/create, and donor audit trail

## Acceptance Criteria

The work is done when:

- translation is fully wiped from the Audit Logs module
- the smart feature is anomaly detection, not a generic chat or summary widget
- suspicious audit entries can be detected, stored, explained, and surfaced
- high-risk anomaly alerts can dispatch SMS without blocking CRUD
- audit CRUD, charts, filters, role guards, hospital scoping, PDF/export decisions, and donor read-only view remain coherent
- the module is demonstrable to the client as a serious audit-log management surface
