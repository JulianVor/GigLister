import type { EventSummary } from "./types";
import type { MapLocation } from "@/components/ConcertMap";

/** Groups a flat event list by venue for ConcertMap - shared between /orte (the full
 * multi-day map) and the homepage's "Heute in deiner Nähe" (today only). Events whose
 * location never resolved coordinates (see LocationService's geocoding) are silently
 * dropped rather than shown at some meaningless default point. */
export function buildMapLocations(events: EventSummary[]): MapLocation[] {
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
  return Array.from(byLocation.values());
}
