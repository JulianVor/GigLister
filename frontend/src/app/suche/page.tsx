import Link from "next/link";
import { search } from "@/lib/api";
import { SearchBar } from "@/components/SearchBar";
import { EventCard } from "@/components/EventCard";
import { StatusBadge } from "@/components/StatusBadge";

export default async function SuchePage({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const { q } = await searchParams;
  const results = q ? await search(q) : null;

  return (
    <div>
      <h1 className="font-display text-3xl">Suche</h1>
      <div className="mt-6 max-w-md">
        <SearchBar initialQuery={q} />
      </div>

      {results && (
        <div className="mt-10 space-y-10">
          {results.events.length === 0 && results.bands.length === 0 && results.locations.length === 0 && (
            <p className="font-meta text-sm text-muted">Keine Ergebnisse für „{q}“.</p>
          )}

          {results.events.length > 0 && (
            <section>
              <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Konzerte</h2>
              <div className="mt-2">
                {results.events.map((e) => (
                  <EventCard key={e.id} event={e} />
                ))}
              </div>
            </section>
          )}

          {results.bands.length > 0 && (
            <section>
              <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Bands</h2>
              <div className="mt-2 divide-y divide-line border-y border-line">
                {results.bands.map((band) => (
                  <Link
                    key={band.id}
                    href={`/bands/${band.id}`}
                    className="flex items-center justify-between gap-4 py-3 hover:text-accent"
                  >
                    <div>
                      <div className="font-display text-lg">{band.name}</div>
                      {band.city && <div className="font-meta text-sm text-muted">{band.city}</div>}
                    </div>
                    <StatusBadge status={band.status} />
                  </Link>
                ))}
              </div>
            </section>
          )}

          {results.locations.length > 0 && (
            <section>
              <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Orte</h2>
              <div className="mt-2 divide-y divide-line border-y border-line">
                {results.locations.map((location) => (
                  <Link
                    key={location.id}
                    href={`/orte/${location.id}`}
                    className="flex items-center justify-between gap-4 py-3 hover:text-accent"
                  >
                    <div>
                      <div className="font-display text-lg">{location.name}</div>
                      <div className="font-meta text-sm text-muted">{location.city}</div>
                    </div>
                  </Link>
                ))}
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  );
}
