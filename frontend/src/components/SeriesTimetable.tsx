import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { fullDateLabel, formatTime, isSameDate } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";
import { entityColor } from "@/lib/entityColor";
import { EventCard } from "./EventCard";

/** A Reihe's events grouped by day - a day with only one concert still gets the normal
 * photo-card treatment, but once several overlap (the whole point of a multi-stage
 * festival/night like "SüdKultur MusicNight"), stacked full-size cards are hard to scan
 * chronologically. Those days instead get a compact running-order table: time, line-up,
 * location, sorted by start time - the events are already date/time-sorted server side
 * (EventSeriesService), so grouping consecutive same-date ones is enough, no re-sort. */
export function SeriesTimetable({ events }: { events: EventSummary[] }) {
  const days = groupByDay(events);
  return (
    <div>
      {days.map(({ date, events: dayEvents }) => (
        <div key={date}>
          <h2 className="mb-1 mt-8 font-meta text-sm uppercase tracking-wide text-muted first:mt-0">
            {fullDateLabel(date)}
          </h2>
          {dayEvents.length > 1 ? <Timetable events={dayEvents} /> : <EventCard event={dayEvents[0]} />}
        </div>
      ))}
    </div>
  );
}

function groupByDay(events: EventSummary[]): { date: string; events: EventSummary[] }[] {
  const groups: { date: string; events: EventSummary[] }[] = [];
  for (const event of events) {
    const last = groups[groups.length - 1];
    if (last && isSameDate(last.date, event.date)) {
      last.events.push(event);
    } else {
      groups.push({ date: event.date, events: [event] });
    }
  }
  return groups;
}

/** One row per concert, ordered by start time - a color bar keyed off the same
 * deterministic entityColor used elsewhere stands in for a photo so rows still read as
 * distinct acts at a glance, without the size a full EventCard would need. */
function Timetable({ events }: { events: EventSummary[] }) {
  const sorted = [...events].sort((a, b) => (a.startTime ?? "").localeCompare(b.startTime ?? ""));
  return (
    <div className="divide-y divide-line border-y border-line">
      {sorted.map((event) => (
        <Link key={event.id} href={`/konzerte/${event.id}`} className="flex items-center gap-3 py-3 hover:bg-surface">
          <span
            aria-hidden
            className="h-8 w-1.5 flex-none"
            style={{ backgroundColor: entityColor(event.bands[0]?.name ?? event.location.name) }}
          />
          <span className="w-12 flex-none font-meta text-sm tabular-nums text-muted">
            {formatTime(event.startTime) ?? "–"}
          </span>
          <span className="min-w-0 flex-1">
            <span className="block truncate font-display text-lg leading-tight">{eventLineupLabel(event)}</span>
            <span className="block truncate font-meta text-xs text-muted">{event.location.name}</span>
          </span>
        </Link>
      ))}
    </div>
  );
}
