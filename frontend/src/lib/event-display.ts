import type { BandSummary } from "./types";

/** "HOME + Band B + Band C", or the event's own title when it has one (festivals etc.). */
export function eventLineupLabel(event: { title: string | null; bands: BandSummary[] }): string {
  if (event.title) return event.title;
  if (event.bands.length === 0) return "Konzert";
  return event.bands.map((b) => b.name).join(" + ");
}
