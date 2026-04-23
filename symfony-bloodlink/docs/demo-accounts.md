# Demo Accounts

These accounts work against the live Supabase-backed dataset used by this merged Symfony copy.

---

## 1. Administrator

| Field | Value |
|-------|-------|
| Email | `admin@admin.com` |
| Password | `admin123654` |
| Role | `ADMIN` |
| Main landing page | `/dashboard/users` |

### What to show

- all Audit Logs pages
- the separate Alerts workspace
- all hospitals visible in the audit module
- the lightweight recent activity summary on `/dashboard/logs`
- full CRUD inside the audit module

---

## 2. Hospital Staff

| Field | Value |
|-------|-------|
| Email | `yassine@staff.com` |
| Password | `7894561230` |
| Role | `HOSPITAL_STAFF` |
| Hospital in session | Hôpital Charles Nicollee |
| Main landing page | `/dashboard/users` |

### What to show

- sidebar session card includes the hospital name
- `/dashboard/logs` is scoped to the staff hospital
- `/dashboard/logs/donations` only shows hospital-scoped donation audits
- `/dashboard/logs/transfers` only shows transfer audits where the staff hospital is involved
- `/dashboard/alerts` only shows alerts linked to that hospital
- when creating audit entries, the actor is fixed to the signed-in staff member where applicable

---

## 3. Donor

| Field | Value |
|-------|-------|
| Email | `user1@gmail.com` |
| Password | `123456789` |
| Role | `DONOR` |
| Main landing page | `/dashboard/donor` |

### What to show

- donor front-office dashboard
- donor articles and tips
- `/dashboard/donor/audit-trail` personal audit trail page and role separation
- `/dashboard/donor/alerts` personal alert timeline
- `/dashboard/donor/alerts/{id}` read-only alert detail

### Dataset note

- this donor account has a populated alerts timeline
- the `My Audit Trail` page now includes a seeded donation history entry, related donation-log activity, and a matching transfer-context example
- the donor flow stays read-only: it exposes personal history and traceability, never Back Office CRUD

The donor account does not get Back Office CRUD access.
