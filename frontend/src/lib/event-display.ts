import type { BandSummary, EventSeriesSummary } from "./types";

/** "HOME + Band B + Band C", or the event's own title when it has one (festivals etc.). */
export function eventLineupLabel(event: { title: string | null; bands: BandSummary[] }): string {
  if (event.title) return event.title;
  if (event.bands.length === 0) return "Konzert";
  return event.bands.map((b) => b.name).join(" + ");
}

/** Same as eventLineupLabel, but prefixed with "<Reihe> - " when the event belongs to one -
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
