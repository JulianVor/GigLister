import Link from "next/link";
import { discover } from "@/lib/api";
import { getLocationPrefs } from "@/lib/location-prefs";
import { EventCard } from "@/components/EventCard";
import { LocationTeaser } from "@/components/LocationTeaser";
import { EmptyState } from "@/components/EmptyState";
import { StatusBadge } from "@/components/StatusBadge";

export default async function EntdeckenPage() {
  const prefs = await getLocationPrefs();
  const data = await discover({
    city: prefs.city ?? undefined,
    lat: prefs.lat ?? undefined,
    lon: prefs.lon ?? undefined,
    radiusKm: prefs.radiusKm ?? undefined,
  });

  return (
    <div>
      <h1 className="font-display text-3xl">Entdecken</h1>
      <p className="mt-1 font-meta text-sm text-muted">Keine Algorithmen — nur, was gerade da ist.</p>

      <Section title="Heute in deiner Nähe">
        {data.todayNearby.length === 0 ? (
          <EmptyState>Heute nichts gelistet.</EmptyState>
        ) : (
          data.todayNearby.map((e) => <EventCard key={e.id} event={e} />)
        )}
      </Section>

      <Section title="Dieses Wochenende">
        {data.thisWeekend.length === 0 ? (
          <EmptyState>Am Wochenende noch nichts gelistet.</EmptyState>
        ) : (
          data.thisWeekend.map((e) => <EventCard key={e.id} event={e} />)
        )}
      </Section>

      <Section title="Neue Konzerte">
        {data.newEvents.length === 0 ? (
          <EmptyState>Noch nichts Neues.</EmptyState>
        ) : (
          data.newEvents.map((e) => <EventCard key={e.id} event={e} />)
        )}
      </Section>

      <Section title="Orte mit kommenden Shows">
        {data.locationsWithUpcomingShows.length === 0 ? (
          <EmptyState>Noch keine Orte mit Konzerten.</EmptyState>
        ) : (
          data.locationsWithUpcomingShows.map((l) => <LocationTeaser key={l.id} location={l} />)
        )}
      </Section>

      <Section title="Bands, die demnächst spielen">
        {data.bandsPlayingSoon.length === 0 ? (
          <EmptyState>Noch keine Bands gelistet.</EmptyState>
        ) : (
          <div className="divide-y divide-line border-y border-line">
            {data.bandsPlayingSoon.map((band) => (
              <Link key={band.id} href={`/bands/${band.id}`} className="flex items-center justify-between gap-4 py-3 hover:text-accent">
                <div>
                  <div className="font-display text-lg">{band.name}</div>
                  {band.city && <div className="font-meta text-sm text-muted">{band.city}</div>}
                </div>
                <StatusBadge status={band.status} />
              </Link>
            ))}
          </div>
        )}
      </Section>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-10">
      <h2 className="font-meta text-sm uppercase tracking-wide text-muted">{title}</h2>
      <div className="mt-2">{children}</div>
    </section>
  );
}
