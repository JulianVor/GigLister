import { getEvents, getLocations } from "@/lib/api";
import { getLocationPrefs } from "@/lib/location-prefs";
import { resolveDateRange } from "@/lib/date-range";
import { DateNav } from "@/components/DateNav";
import { ConcertMapClient } from "@/components/ConcertMapClient";
import type { MapLocation } from "@/components/ConcertMap";
import { EventListByDay } from "@/components/EventListByDay";
import { LocationTeaser } from "@/components/LocationTeaser";
import { EmptyState } from "@/components/EmptyState";

export default async function OrtePage({
  searchParams,
}: {
  searchParams: Promise<{ range?: string; from?: string; to?: string }>;
}) {
  const params = await searchParams;
  const prefs = await getLocationPrefs();
  const { from, to, active } = resolveDateRange(params);

  if (prefs.lat != null && prefs.lon != null) {
    const center = { lat: prefs.lat, lon: prefs.lon };
    const radiusKm = prefs.radiusKm ?? 25;
    // A pure vicinity search, unlike the other list pages that also AND a typed city's
    // text against Location.city - a map showing "concerts around here" would otherwise
    // hide a venue one town over just because its own city field doesn't literally match
    // what was typed (see LocationPicker's own geocode-on-submit for how a typed city
    // ends up with real coordinates here in the first place).
    const page = await getEvents({ ...center, radiusKm, from, to, size: 200 });
    const events = page.content;

    const byLocation = new Map<number, MapLocation>();
    for (const event of events) {
      const loc = event.location;
      if (loc.latitude == null || loc.longitude == null) continue;
      let entry = byLocation.get(loc.id);
      if (!entry) {
        entry = { id: loc.id, name: loc.name, city: loc.city, latitude: loc.latitude, longitude: loc.longitude, events: [] };
        byLocation.set(loc.id, entry);
      }
      entry.events.push({ id: event.id, date: event.date, startTime: event.startTime });
    }

    return (
      <div>
        <h1 className="font-display text-3xl">Konzerte in deiner Nähe</h1>
        <p className="mt-1 font-meta text-sm text-muted">
          {prefs.city ? `${prefs.city} · ` : ""}
          {radiusKm} km Umkreis
        </p>

        <div className="mt-6">
          <DateNav active={active} basePath="/orte" />
        </div>

        <div className="mt-6">
          <ConcertMapClient center={center} radiusKm={radiusKm} locations={Array.from(byLocation.values())} />
        </div>

        <div className="mt-8">
          {events.length === 0 ? (
            <EmptyState>Für diesen Zeitraum sind keine Konzerte in der Nähe gelistet.</EmptyState>
          ) : (
            <EventListByDay events={events} />
          )}
        </div>
      </div>
    );
  }

  // No coordinates yet, so there's nothing to center a map on - fall back to the plain,
  // text-filtered location list this page always had, with a prompt toward the map.
  const page = await getLocations({ city: prefs.city ?? undefined, size: 50 });

  return (
    <div>
      <h1 className="font-display text-3xl">Orte</h1>
      <p className="mt-1 font-meta text-sm text-muted">Wo gibt es Konzerte?</p>
      <p className="mt-4 border border-line bg-surface p-4 font-meta text-sm text-muted">
        Wähle oben rechts einen Standort, um Konzerte in deiner Nähe auf einer Karte zu sehen.
      </p>

      <div className="mt-6">
        {page.content.length === 0 ? (
          <EmptyState>Noch keine Orte gelistet.</EmptyState>
        ) : (
          page.content.map((location) => <LocationTeaser key={location.id} location={location} />)
        )}
      </div>
    </div>
  );
}
