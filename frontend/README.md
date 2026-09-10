# GigLister – Frontend (V1)

Next.js (App Router, React, TypeScript) frontend for the GigLister concept — a
local concert guide. Talks to the Spring Boot backend in `../` over its REST
API (`../README.md`).

## Stack & architecture

- **Next.js 16 (App Router) + TypeScript + Tailwind CSS v4.**
- Public pages (home, Konzerte, Eventdetail, Orte, Bandseite, Entdecken,
  Suche) are Server Components that fetch straight from the backend
  server-side — good for SEO and simplicity, no client-side loading states
  needed for the read-only golden path.
- Auth is a JWT issued by the backend, stored in an **httpOnly cookie** set
  by a Server Action (`src/actions/auth.ts`). It's never exposed to
  client-side JS. All authenticated mutations (create/edit event, follow,
  save, claim, admin actions, …) go through Server Actions in `src/actions/`,
  which read the cookie server-side and call the backend with a Bearer token.
- The one exception: the "Meintest du?" duplicate-suggestion lookup while
  typing a new band/location name (`EntityPicker`) calls the backend directly
  from the browser, since that endpoint is public and needs to feel instant.
- `src/lib/api.ts` is a typed client for every backend endpoint used here;
  `src/lib/types.ts` mirrors the backend DTOs.
- Design tokens (colors, the three-tier type system: condensed meta font /
  bold display font / body sans) live in `src/app/globals.css`.

## Running locally

The backend must be running first (see `../README.md`; default `:8080`).

```bash
cp .env.local.example .env.local   # points at the local backend
npm install
npm run dev
```

Open http://localhost:3000.

## Route map

Mirrors the concept's information architecture directly:

```
/                          Startseite (heute, sofort Events)
/konzerte                  Konzertübersicht (Datumsnav + Filter)
/konzerte/kalender         Monatsansicht
/konzerte/neu               Konzert anlegen
/konzerte/[id]              Eventdetail
/konzerte/[id]/bearbeiten   Konzert bearbeiten
/orte, /orte/[id]           Orte-Liste, Locationseite
/orte/[id]/bearbeiten
/bands/[id]                 Bandseite (nicht in der Hauptnav, nur verlinkt)
/bands/[id]/bearbeiten
/entdecken                  Kuratierte Sektionen (kein Algorithmus)
/suche                      Gruppierte Suche über Konzerte/Bands/Orte
/mein-giglister              Gemerkt / Gefolgt / Verwaltet
/login, /registrieren
/admin, /admin/bands, /admin/locations, /admin/events     Nur für platformAdmin
/admin/claims, /admin/duplicates, /admin/users             (alle Status, filterbar, nicht nur PUBLISHED)
```

## Known V1 gaps

- No image upload — logo/titlebild fields are plain URL inputs.
- No geocoding — umkreissuche needs lat/lng, which nobody supplies yet
  (falls back to city-only matching).
- Granting a permission on a Band/Location edit page takes a raw numeric
  user ID, because the backend has no user search/invite-by-email endpoint
  (deliberately — no public person search per the concept). A real invite
  flow needs a small backend addition first.
- Genre filtering on `/konzerte` isn't wired up — the backend's event list
  endpoint doesn't take a genre parameter yet.
