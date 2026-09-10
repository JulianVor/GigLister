# GigLister – Backend (V1)

Spring Boot backend implementing the V1 concept: a local concert guide built
around three connected entities — **Event**, **Location**, **Band** — plus a
single account type (**User**) with per-entity permissions.

## Stack

- Java 21, Spring Boot 3.3 (Web, Data JPA, Security, Validation)
- Postgres (H2 only for the test suite, in-memory)
- JWT auth (stateless, `jjwt`)
- Maven

## Running

Start a local Postgres (or point at any existing one — see env vars below):

```
docker compose up -d
```

Then:

```
mvn spring-boot:run
```

The API listens on `:8080`. Schema is created/updated automatically by
Hibernate (`ddl-auto: update`) — fine for V1, but the first thing to swap
for a real migration tool (Flyway) once the schema needs to evolve under
real data.

Connection defaults match `docker-compose.yml` (`localhost:5432/giglister`,
user/password `giglister`); override with `GIGLISTER_DB_HOST`,
`GIGLISTER_DB_PORT`, `GIGLISTER_DB_NAME`, `GIGLISTER_DB_USER`,
`GIGLISTER_DB_PASSWORD` to point at a different instance (a managed Postgres
in production, for instance).

On first `register`, set
`GIGLISTER_ADMIN_EMAIL` to that email beforehand to bootstrap the first
`PLATFORM_ADMIN` account (there's no separate admin account type — it's just a
flag on a normal user, as in the concept). From there, that admin can promote
or demote any other user via `POST /api/admin/users/{id}/promote|demote`
(surfaced in the frontend under `/admin/users`) — a user can never remove
their own admin rights, so the platform can't end up without one.

Registration only needs a unique username, an email (used for password
recovery) and a password; the account stays unverified and login is blocked
until the confirmation link is followed. `POST /api/auth/forgot-password`
sends a reset link the same way (and always answers identically whether or
not the email is registered, so it can't be used to enumerate accounts);
following it via `POST /api/auth/reset-password` sets a new password, logs
the user in, and — since it proves the same inbox ownership as the
verification link — verifies the email too. Mail is sent
via `GIGLISTER_MAIL_HOST`/`GIGLISTER_MAIL_PORT`/`GIGLISTER_MAIL_USER`/
`GIGLISTER_MAIL_PASSWORD`/`GIGLISTER_MAIL_FROM`; with `GIGLISTER_MAIL_HOST`
unset (the local default), the verification/reset link is written to the server
log instead of actually being sent. `GIGLISTER_FRONTEND_URL` controls which
frontend origin that link points at (defaults to `http://localhost:3000`).

## Domain model

- **User** — single account type; `platformAdmin` is the only special flag.
- **Band** / **Location** — `status`: `STUB → DRAFT → PUBLISHED → ARCHIVED`.
  Only `PUBLISHED` entities get a real public profile page (`linkable: true`
  in API responses) — this is the "no empty profile pages" rule from the
  concept.
- **Event** — the core entity; references `Location`/`Band` only by numeric
  ID, never by name, so renames and merges never break a relationship.
- **EntityPermission** — object-specific `EDIT` / `MANAGE` grants on a Band or
  Location. No `BandAccount`/`LocationAccount` types exist.
- **Claim** — a user's request to become `MANAGE` owner of an unclaimed
  entity; decided by a platform admin.
- **EntityMerge** — audit trail for admin-driven duplicate merges; the source
  entity is archived (not deleted) and its old name kept as an alias.
- **SavedEvent** / **BandFollow** — "Merken" and "Band folgen".

## API overview

All endpoints are under `/api`. Public (no auth): `GET` on
`/events`, `/locations`, `/bands`, `/search`, `/discover`. Everything else
requires a Bearer JWT; `/admin/**` additionally requires `platformAdmin`.

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login` |
| Events | `GET /events`, `GET /events/calendar`, `GET/POST/PUT /events/{id}`, `PATCH /events/{id}/status`, `POST\|DELETE /events/{id}/save` |
| Bands | `GET/POST/PUT /bands`, `GET /bands/duplicates`, `PATCH /bands/{id}/status`, `GET/POST/DELETE /bands/{id}/permissions`, `POST /bands/{id}/claim`, `POST\|DELETE /bands/{id}/follow` |
| Locations | same shape as Bands (no `follow`) |
| Discovery | `GET /discover`, `GET /search?q=&type=` |
| Me | `GET/PUT /me` — saved events, followed bands, managed entities |
| Admin | `GET /admin/dashboard`, `GET /admin/duplicates`, `GET /admin/claims`, `POST /admin/claims/{id}/approve\|reject`, `POST /admin/merge`, `GET /admin/users`, `POST /admin/users/{id}/promote\|demote`, `GET /admin/bands\|locations\|events` (every status, not just PUBLISHED — filterable by `status`/`q`) |

**Creating an event** (`POST /events`) accepts either an existing
`location`/`band` id, or just a `name`+`city` to create a `STUB` inline —
this is the "Neue Band/Location während eines Events" flow from section
31/32 of the concept. Duplicate-aware: an exact name+city match reuses the
existing stub instead of creating a new one; `GET /bands/duplicates?name=&city=`
and `GET /locations/duplicates?...` power the "Meintest du?" suggestion UI
before a user creates a brand new entity.

## What's implemented vs. deferred

Implemented: the full V1 scope table from the concept (§51), backend and
frontend — discovery by date/city/radius (including a real device-location
radius search via "Standort verwenden", §13), calendar counts,
event/band/location CRUD, claim workflow, multiple managers per entity,
stub-on-the-fly creation, duplicate detection + admin merge, follow/save,
search, discover sections, admin dashboard, registration with email
verification, and self-service password reset.

Deliberately deferred (matches §45 "was V1 nicht enthält" plus normal
backend-first sequencing): no image upload (media fields are plain URLs),
no reverse geocoding (a typed city stays a plain name match; only
"Standort verwenden" gives a real coordinate radius), no genre filter on
the concert/discover lists (bands carry genres, but nothing filters by them
yet), no distance shown per event, and merged entities don't yet redirect
their old URL to the surviving one (§36) — the alias is stored, just not
acted on.
