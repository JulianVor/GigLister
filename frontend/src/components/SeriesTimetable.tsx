import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { fullDateLabel, formatTime, isSameDate } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";
import { entityColor } from "@/lib/entityColor";
import { EventCard } from "./EventCard";

/** A Reihe's events grouped by day - a day with only one concert still gets the normal
 * photo-card treatment, but once several overlap (the whole point of a multi-location
 * night like "SüdKultur MusicNight"), stacked full-size cards are hard to scan
 * chronologically - and don't show which of them actually run at the same time, when
 * you can only be at one. Those days instead get a compact running-order table (see
 * Timetable) that groups same-start-time concerts together. The events are already
 * date/time-sorted server side (EventSeriesService), so grouping consecutive
 * same-date ones here is enough, no re-sort needed at this level. */
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

/** Grouped by exact start time, not just one row per concert - at a multi-location night
 * several bands can easily start at the same time in different venues, and you can only
 * be at one of them. A flat time-sorted list would put those right after each other
 * looking sequential; grouping them under one shared time slot (with a "zeitgleich" hint
 * once there's more than one) makes the actual choice visible instead of hiding it. */
function Timetable({ events }: { events: EventSummary[] }) {
  const slots = groupByTime(events);
  return (
    <div className="divide-y divide-line border-y border-line">
      {slots.map(({ time, events: slotEvents }) => (
        <div key={time} className="flex gap-3 py-3">
          <span className="w-12 flex-none pt-1 font-meta text-sm tabular-nums text-muted">{formatTime(time) ?? "–"}</span>
          <div className={`min-w-0 flex-1 space-y-2 ${slotEvents.length > 1 ? "border-l-2 border-accent/30 pl-3" : ""}`}>
            {slotEvents.length > 1 && (
              <p className="font-meta text-xs uppercase tracking-wide text-accent">Zeitgleich</p>
            )}
            {slotEvents.map((event) => (
              <Link key={event.id} href={`/konzerte/${event.id}`} className="flex items-center gap-3 hover:text-accent">
                <span
                  aria-hidden
                  className="h-8 w-1.5 flex-none"
                  style={{ backgroundColor: entityColor(event.bands[0]?.name ?? event.location.name) }}
                />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-display text-lg leading-tight">{eventLineupLabel(event)}</span>
                  <span className="block truncate font-meta text-xs text-muted">{event.location.name}</span>
                </span>
              </Link>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function groupByTime(events: EventSummary[]): { time: string | null; events: EventSummary[] }[] {
  const sorted = [...events].sort((a, b) => (a.startTime ?? "").localeCompare(b.startTime ?? ""));
  const groups: { time: string | null; events: EventSummary[] }[] = [];
  for (const event of sorted) {
    const last = groups[groups.length - 1];
    if (last && last.time === event.startTime) {
      last.events.push(event);
    } else {
      groups.push({ time: event.startTime, events: [event] });
    }
  }
  return groups;
}
