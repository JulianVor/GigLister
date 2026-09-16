import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { dayAndMonth, formatTime, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";
import { entityColor } from "@/lib/entityColor";
import { EmptyState } from "./EmptyState";

/** A compact, photo-free stand-in for EventCard, built for a small scrollable box (the
 * homepage's "Deine nächsten Konzerte") where a dozen-plus concerts need to fit and scan
 * quickly rather than a handful of full-size cards. Same location-color stripe
 * SeriesTimetable uses, for the same reason: a quick visual anchor without loading an
 * image. Fixed height + internal scroll (matching ConcertMap's own height) rather than
 * growing the page, so it can sit right next to the map instead of pushing it down. */
export function NextConcertsList({ events }: { events: EventSummary[] }) {
  if (events.length === 0) {
    return <EmptyState>Keine kommenden Konzerte gefunden.</EmptyState>;
  }
  return (
    <div className="h-[420px] overflow-y-auto border border-line sm:h-[520px]">
      <div className="divide-y divide-line">
        {events.map((event) => {
          const festival = event.eventSeries;
          const time = formatTime(event.startTime);
          // A saved festival act/concert points at the festival's own Gemerkte Konzerte
          // view (not this one concert's page) - which band, which venue, which of
          // possibly several times that night is exactly what that view (narrowed to just
          // what you merkt) answers, so this row only needs to say "you've got something
          // saved in this festival, on this date" and let that page do the rest.
          const href = festival ? `/festivals/${festival.id}?filter=saved#konzerte` : `/konzerte/${event.id}`;
          return (
            <Link key={event.id} href={href} className="flex items-start gap-3 px-3 py-2.5 hover:text-accent">
              <span
                aria-hidden
                className="mt-0.5 h-8 w-1 flex-none"
                style={{ backgroundColor: entityColor(festival ? festival.name : event.location.name) }}
              />
              <span className="w-14 flex-none font-meta text-xs uppercase tracking-wide text-muted">
                <span className="block">
                  {weekdayShort(event.date)} {dayAndMonth(event.date)}
                </span>
                {!festival && time && <span className="block tabular-nums">{time}</span>}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate font-display text-base leading-tight">
                  {festival ? festival.name : eventLineupLabel(event)}
                </span>
                {!festival && (
                  <span className="block truncate font-meta text-xs text-muted">
                    {event.location.name} · {event.location.city}
                  </span>
                )}
              </span>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
