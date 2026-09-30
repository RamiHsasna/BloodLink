# Branch Hygiene Policy

## Canonical branch
- Canonical integration branch is `master`.
- All production-bound changes flow into `master` through pull requests.

## Merge freeze for synchronization windows
- During branch-alignment windows, do not merge feature branches until divergence audit is complete.
- The freeze starts when announced in project channels and ends after the audit summary is shared.

## Branch naming conventions
- Feature work: `feature/<scope>-<short-description>`
- Bug fixes: `fix/<scope>-<short-description>`
- Hotfixes: `hotfix/<scope>-<short-description>`
- Archival copies: `archive/<original-branch>-YYYYMMDD`

## Branch lifetime and sync cadence
- Target max active lifetime for feature/fix branches: 14 days.
- Branches older than 30 days require revalidation with owners.
- Re-sync active branches with `master` at least every 3 days.

## Branch classification rules
- `keep and sync`: active branch with pending approved work.
- `keep but archive`: historically relevant branch no longer receiving commits.
- `delete`: merged or obsolete branch with no retention requirement.

## Synchronization rules
- Behind-only branch: merge `master` into branch before next PR update.
- Ahead-only branch: open PR to `master`, require review and checks.
- Diverged branch: merge/rebase onto latest `master`, resolve conflicts, rerun checks.

## Required checks before merge
- Security Guardrails workflow must pass.
- Branch Hygiene Audit must not be ignored for diverged branch updates.
- Any repository-defined test/build checks must pass.

## Stale branch maintenance
- Weekly branch hygiene audit workflow produces ahead/behind report.
- Owners should archive or delete stale branches in the same sprint.

## Secret safety requirements
- `.env` and other sensitive files must never be tracked.
- Use `.env.example` as the only committed template.
- Local hooks (`pre-commit` and `pre-push`) must be enabled via `git config core.hooksPath .githooks`.
