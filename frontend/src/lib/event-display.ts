import type { BandSummary, EventSeriesSummary, EventSummary } from "./types";

/** "HOME + Band B + Band C", or the event's own title when it has one (festivals etc.). */
export function eventLineupLabel(event: { title: string | null; bands: BandSummary[] }): string {
  if (event.title) return event.title;
  if (event.bands.length === 0) return "Konzert";
  return event.bands.map((b) => b.name).join(" + ");
}

/** Same as eventLineupLabel, but prefixed with "<Festival> - " when the event belongs to one -
 * for listing contexts (EventCard) outside the series' own page, where that context would
 * otherwise be invisible. Never used on the series' own page itself (see EventCard's
 * hideSeriesPrefix) - there the series name is already the page, so plain eventLineupLabel
 * is enough and repeating it in every title would just be noise. */
export function eventListLabel(event: {
  title: string | null;
  bands: BandSummary[];
  eventSeries: EventSeriesSummary | null;
}): string {
  const label = eventLineupLabel(event);
  return event.eventSeries ? `${event.eventSeries.name} - ${label}` : label;
}

/** Narrows a saved festival concert down to just the acts actually gemerkt within it (see
 * UserService.saveAct), so anywhere it's later labeled (eventLineupLabel/eventListLabel)
 * reads "<Festival> - Act 1 + Act 2", not the concert's whole line-up. A no-op (the full
 * line-up stays) when nothing was saved act-by-act for this event - it was merkt as a
 * whole, so its full bill is exactly what should show. */
export function narrowToSavedActs(event: EventSummary, savedActBandIds: Set<number> | undefined): EventSummary {
  if (!savedActBandIds || savedActBandIds.size === 0) return event;
  return { ...event, bands: event.bands.filter((b) => savedActBandIds.has(b.id)) };
}
