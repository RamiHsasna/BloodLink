# Branch Alignment and Secret Purge Runbook

## 1) Canonical branch and merge freeze
- Canonical branch: `master`.
- Keep `master` protected (required reviews and required status checks).
- During branch cleanup windows, pause merges to non-canonical branches.

## 2) Branch audit snapshot (relative to `origin/master`)

| Branch | Behind | Ahead | State | Classification |
|---|---:|---:|---|---|
| `AlertManagement` | 2 | 49 | diverged | keep and sync |
| `DonationManagement` | 2 | 50 | diverged | keep and sync |
| `HospitalManagement` | 2 | 51 | diverged | keep and sync |
| `LogManagement` | 2 | 52 | diverged | keep and sync |
| `UserManagement` | 1 | 0 | behind-only | keep and sync |
| `alertForUser` | 2 | 51 | diverged | keep and sync |
| `javafx-merge` | 1 | 1 | diverged | keep and sync |
| `migration` | 2 | 101 | diverged | keep and sync |
| `symfony/AlertManagement` | 2 | 59 | diverged | keep and sync |
| `symfony/DonationManagement` | 1 | 0 | behind-only | keep and sync |
| `symfony/HospitalManagement` | 1 | 2 | diverged | keep and sync |
| `symfony/LogManagement` | 2 | 96 | diverged | keep and sync |
| `symfony/UserManagement` | 0 | 1 | ahead-only | keep and sync |
| `symfony/migration` | 2 | 105 | diverged | keep and sync |

### Archive/Delete candidates
No branch was auto-deleted in this change. Mark any superseded branch as `archive/<branch>-<date>` and delete the original after approval.

## 3) Synchronization procedure
1. For behind-only branches, merge `master` into branch.
2. For ahead-only branches, open PR from branch into `master`, review, merge.
3. For diverged branches, rebase/merge on top of latest `master`, resolve conflicts, run checks, then push.
4. Delete obsolete branches after merge completion and retention/tagging decision.

## 4) Secret incident response (already started here)
- `symfony-bloodlink/.env` removed from tracked files.
- `symfony-bloodlink/.gitignore` now ignores `.env`.
- `.env.example` remains tracked as the setup template.
- CI and local pre-commit guardrails added to block sensitive filenames and scan for secrets.

## 5) Mandatory remaining actions outside this commit
These require repository admin rights and/or force updates to existing remote refs:
1. Rotate/revoke every secret ever present in the leaked `.env`.
2. Rewrite history across all branches/tags to purge `symfony-bloodlink/.env`, then force-push refs.
3. Ask all collaborators to re-clone or hard reset after history rewrite.
4. Enable branch protection rules requiring up-to-date branch and required checks before merge.

## 6) Suggested history rewrite command set (run by repository admin)
```bash
git clone --mirror <repo-url> bloodlink-mirror.git
cd bloodlink-mirror.git
git filter-repo --path symfony-bloodlink/.env --invert-paths --force
git for-each-ref --format='delete %(refname)' refs/original | git update-ref --stdin
git reflog expire --expire=now --all
git gc --prune=now --aggressive
git push --force --all
git push --force --tags
```

## 7) Verification checklist
- `git ls-files | grep -E '(^|/)\.env$'` returns no matches.
- Secret scanning tools return no active leaks.
- Required CI checks are enforced in branch protection.
- New contributors use `.env.example` only.


## 8) Automation now in repository
- Branch hygiene automation now runs weekly via `.github/workflows/branch-hygiene-audit.yml`.
- Security guardrails run on PR/push and weekly schedule via `.github/workflows/security-guardrails.yml`.
- Local hook-based checks are available in `.githooks/pre-commit` and `.githooks/pre-push` (enable with `git config core.hooksPath .githooks`).
- Team policy is documented in `BRANCH_HYGIENE_POLICY.md`.
