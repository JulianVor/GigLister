import Link from "next/link";
import type { BandSummary, EventSummary } from "@/lib/types";
import { fullDateLabel, formatTime, isSameDate } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";
import { entityColor } from "@/lib/entityColor";
import { EventCard } from "./EventCard";

/** A Festival's events grouped by day - a day with only one concert still gets the normal
 * photo-card treatment, but once several overlap (the whole point of a multi-location
 * night like "SüdKultur MusicNight") - or a single event's own line-up has bands going on
 * at different times - stacked full-size cards are hard to scan chronologically, and don't
 * show which of them actually run at the same time, when you can only be at one. Those
 * days instead get a compact running-order table (see Timetable) that groups same-start-
 * time slots together. The events are already date/time-sorted server side
 * (EventSeriesService), so grouping consecutive same-date ones here is enough, no re-sort
 * needed at this level. */
export function SeriesTimetable({ events }: { events: EventSummary[] }) {
  const days = groupByDay(events);
  return (
    <div>
      {days.map(({ date, events: dayEvents }) => (
        <div key={date}>
          <h2 className="mb-1 mt-8 font-meta text-sm uppercase tracking-wide text-muted first:mt-0">
            {fullDateLabel(date)}
          </h2>
          {dayEvents.length > 1 || dayEvents.some(hasAnyBandTime) ? (
            <Timetable events={dayEvents} />
          ) : (
            <EventCard event={dayEvents[0]} hideSeriesPrefix />
          )}
        </div>
      ))}
    </div>
  );
}

function hasAnyBandTime(event: EventSummary): boolean {
  return event.bands.some((b) => b.startTime);
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

interface SlotRow {
  key: string;
  time: string | null;
  label: string;
  colorKey: string;
  event: EventSummary;
}

/** Turns one event into one or more rows: if none of its bands have their own start time,
 * the whole line-up stays a single row at the event's own time (unchanged from before this
 * band-level granularity existed). If some do, each of those bands gets its own row at its
 * own time, and any remaining (un-timed) bands are grouped into one more row at the
 * event's own time - e.g. a headliner going on later than the rest of the bill. */
function explodeEvent(event: EventSummary): SlotRow[] {
  const timedBands = event.bands.filter((b) => b.startTime);
  if (timedBands.length === 0) {
    return [
      {
        key: `e${event.id}`,
        time: event.startTime,
        label: eventLineupLabel(event),
        colorKey: event.bands[0]?.name ?? event.location.name,
        event,
      },
    ];
  }
  const rows: SlotRow[] = timedBands.map((b) => ({
    key: `e${event.id}-b${b.id}`,
    time: b.startTime,
    label: b.name,
    colorKey: b.name,
    event,
  }));
  const remaining = event.bands.filter((b) => !b.startTime);
  if (remaining.length > 0) {
    rows.push({
      key: `e${event.id}-rest`,
      time: event.startTime,
      label: remaining.map((b: BandSummary) => b.name).join(" + "),
      colorKey: remaining[0]?.name ?? event.location.name,
      event,
    });
  }
  return rows;
}

/** Grouped by exact start time, not just one row per slot - at a multi-location night
 * several bands (or whole events) can easily start at the same time in different venues,
 * and you can only be at one of them. A flat time-sorted list would put those right after
 * each other looking sequential; grouping them under one shared time slot (with a
 * "zeitgleich" hint once there's more than one) makes the actual choice visible instead
 * of hiding it. */
function Timetable({ events }: { events: EventSummary[] }) {
  const rows = events.flatMap(explodeEvent);
  const slots = groupByTime(rows);
  return (
    <div className="divide-y divide-line border-y border-line">
      {slots.map(({ time, rows: slotRows }) => (
        <div key={time ?? "–"} className="flex gap-3 py-3">
          <span className="w-12 flex-none pt-1 font-meta text-sm tabular-nums text-muted">{formatTime(time) ?? "–"}</span>
          <div className={`min-w-0 flex-1 space-y-2 ${slotRows.length > 1 ? "border-l-2 border-accent/30 pl-3" : ""}`}>
            {slotRows.length > 1 && (
              <p className="font-meta text-xs uppercase tracking-wide text-accent">Zeitgleich</p>
            )}
            {slotRows.map((row) => (
              <Link key={row.key} href={`/konzerte/${row.event.id}`} className="flex items-center gap-3 hover:text-accent">
                <span aria-hidden className="h-8 w-1.5 flex-none" style={{ backgroundColor: entityColor(row.colorKey) }} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-display text-lg leading-tight">{row.label}</span>
                  <span className="block truncate font-meta text-xs text-muted">{row.event.location.name}</span>
                </span>
              </Link>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function groupByTime(rows: SlotRow[]): { time: string | null; rows: SlotRow[] }[] {
  const sorted = [...rows].sort((a, b) => (a.time ?? "").localeCompare(b.time ?? ""));
  const groups: { time: string | null; rows: SlotRow[] }[] = [];
  for (const row of sorted) {
    const last = groups[groups.length - 1];
    if (last && last.time === row.time) {
      last.rows.push(row);
    } else {
      groups.push({ time: row.time, rows: [row] });
    }
  }
  return groups;
}
