"use client";

import Link from "next/link";
import { Fragment, useState } from "react";
import type { EventSummary, LocationSummary, TimetableStyle } from "@/lib/types";
import { fullDateLabel, formatTime, isSameDate } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";
import { entityColor } from "@/lib/entityColor";
import { EventCard } from "./EventCard";

/** A Festival's events grouped by day - a day with only one concert still gets the normal
 * photo-card treatment, but once several overlap (the whole point of a multi-location
 * night like "SüdKultur MusicNight") - or a single event's own line-up has bands going on
 * at different times - stacked full-size cards are hard to scan chronologically, and don't
 * show which of them actually run at the same time, when you can only be at one. Those
 * days instead get a compact running-order table that groups same-start-time slots
 * together (LIST, `Timetable`) or lays locations out as columns with time as rows (GRID,
 * `GridTimetable`) - the festival's own creator picks which fits their event (few
 * locations read fine as a list; many are easier to compare as a grid), see
 * EventSeries.timetableStyle. The events are already date/time-sorted server side
 * (EventSeriesService), so grouping consecutive same-date ones here is enough, no re-sort
 * needed at this level. */
export function SeriesTimetable({ events, style = "LIST" }: { events: EventSummary[]; style?: TimetableStyle }) {
  const days = groupByDay(events);
  return (
    <div>
      {days.map(({ date, events: dayEvents }) => (
        <div key={date}>
          <h2 className="mb-1 mt-8 font-meta text-sm uppercase tracking-wide text-muted first:mt-0">
            {fullDateLabel(date)}
          </h2>
          {dayEvents.length > 1 || dayEvents.some(hasAnyBandTime) ? (
            style === "GRID" ? (
              <GridTimetable events={dayEvents} />
            ) : (
              <Timetable events={dayEvents} />
            )
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
  event: EventSummary;
}

/** Turns one event into one or more rows: if none of its bands have their own start time,
 * the whole line-up stays a single row at the event's own time (unchanged from before this
 * band-level granularity existed). If some do, each of those bands gets its own row at its
 * own time, and any remaining (un-timed) bands are grouped into one more row at the
 * event's own time - e.g. a headliner going on later than the rest of the bill. Every row
 * keeps a reference to its own event, so its Location (not the band) can drive color and
 * grid placement - one location keeps one color across the whole night, whoever's playing. */
function explodeEvent(event: EventSummary): SlotRow[] {
  const timedBands = event.bands.filter((b) => b.startTime);
  if (timedBands.length === 0) {
    return [{ key: `e${event.id}`, time: event.startTime, label: eventLineupLabel(event), event }];
  }
  const rows: SlotRow[] = timedBands.map((b) => ({
    key: `e${event.id}-b${b.id}`,
    time: b.startTime,
    label: b.name,
    event,
  }));
  const remaining = event.bands.filter((b) => !b.startTime);
  if (remaining.length > 0) {
    rows.push({
      key: `e${event.id}-rest`,
      time: event.startTime,
      label: remaining.map((b) => b.name).join(" + "),
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
 * of hiding it. Each row's stripe and tag are colored by its own Location - with several
 * locations in play, that's the axis a visitor actually needs to track at a glance, not
 * which band happens to be on. A location filter above the table narrows it down when
 * there are enough locations that "just scan the whole list" stops working. */
function Timetable({ events }: { events: EventSummary[] }) {
  const rows = events.flatMap(explodeEvent);
  const locations = uniqueLocations(rows);
  const [activeLocationId, setActiveLocationId] = useState<number | null>(null);

  const filteredRows = activeLocationId == null ? rows : rows.filter((r) => r.event.location.id === activeLocationId);
  const slots = groupByTime(filteredRows);

  return (
    <div>
      {locations.length > 1 && (
        <div className="mb-3 border border-line p-3">
          <p className="mb-2 font-meta text-xs uppercase tracking-wide text-muted">Locations filtern</p>
          <div className="flex flex-wrap gap-1.5">
            <button
              type="button"
              onClick={() => setActiveLocationId(null)}
              className={`font-meta text-xs font-semibold ${
                activeLocationId == null ? "bg-fg text-bg" : "border border-line"
              } px-2.5 py-1`}
            >
              Alle
            </button>
            {locations.map((loc) => (
              <button
                key={loc.id}
                type="button"
                onClick={() => setActiveLocationId(activeLocationId === loc.id ? null : loc.id)}
                className={`flex items-center gap-1.5 font-meta text-xs font-semibold ${
                  activeLocationId === loc.id ? "bg-fg text-bg" : "border border-line"
                } px-2.5 py-1`}
              >
                <span aria-hidden className="h-2 w-2 flex-none" style={{ backgroundColor: entityColor(loc.name) }} />
                {loc.name}
              </button>
            ))}
          </div>
        </div>
      )}
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
                  <span
                    aria-hidden
                    className="h-8 w-1.5 flex-none"
                    style={{ backgroundColor: entityColor(row.event.location.name) }}
                  />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate font-display text-lg leading-tight">{row.label}</span>
                    <span
                      className="mt-0.5 inline-block px-1.5 py-0.5 font-meta text-xs font-semibold text-accent-fg"
                      style={{ backgroundColor: entityColor(row.event.location.name) }}
                    >
                      {row.event.location.name}
                    </span>
                  </span>
                </Link>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/** The grid alternative to Timetable: locations as columns, time as rows, so every
 * simultaneous act at a many-location night is comparable at a glance without scrolling
 * through a long flat list - the tradeoff is that with few locations it reads sparse, which
 * is exactly why this is the festival creator's own choice (EventSeries.timetableStyle),
 * not a fixed one. Columns can outgrow the viewport, so the whole grid sits in its own
 * horizontally scrollable box with the time column pinned in place while it scrolls. */
function GridTimetable({ events }: { events: EventSummary[] }) {
  const rows = events.flatMap(explodeEvent);
  const locations = uniqueLocations(rows);
  const times = uniqueTimes(rows);

  return (
    <div className="overflow-x-auto border border-line">
      <div
        className="grid w-max"
        style={{ gridTemplateColumns: `4.5rem repeat(${locations.length}, minmax(8.5rem, 1fr))` }}
      >
        <div className="sticky left-0 top-0 z-20 border-b border-r border-line bg-fg" />
        {locations.map((loc) => (
          <div
            key={loc.id}
            className="sticky top-0 z-10 flex flex-col items-center gap-1 border-b border-r border-line bg-fg px-1 py-2 text-center"
          >
            <span aria-hidden className="h-2 w-2 flex-none" style={{ backgroundColor: entityColor(loc.name) }} />
            <span className="font-meta text-[11px] uppercase leading-tight tracking-wide text-bg">{loc.name}</span>
          </div>
        ))}

        {times.map((time) => (
          <Fragment key={time ?? "–"}>
            <div className="sticky left-0 z-10 border-b border-r border-line bg-surface px-2 pt-2 text-right font-meta text-sm tabular-nums text-muted">
              {formatTime(time) ?? "–"}
            </div>
            {locations.map((loc) => {
              const cellRows = rows.filter((r) => r.time === time && r.event.location.id === loc.id);
              return (
                <div key={loc.id} className="space-y-1 border-b border-r border-line p-1">
                  {cellRows.map((row) => (
                    <Link
                      key={row.key}
                      href={`/konzerte/${row.event.id}`}
                      className="block px-2 py-1.5 text-accent-fg hover:opacity-90"
                      style={{ backgroundColor: entityColor(loc.name) }}
                    >
                      <span className="block truncate font-display text-[13px] leading-tight">{row.label}</span>
                    </Link>
                  ))}
                </div>
              );
            })}
          </Fragment>
        ))}
      </div>
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

/** Distinct locations across a set of rows, in first-seen (i.e. chronological) order -
 * shared by the list filter's chip row and the grid's column headers. */
function uniqueLocations(rows: SlotRow[]): LocationSummary[] {
  const seen = new Map<number, LocationSummary>();
  for (const row of rows) {
    if (!seen.has(row.event.location.id)) seen.set(row.event.location.id, row.event.location);
  }
  return [...seen.values()];
}

/** Distinct start times across a set of rows, time-ascending - the grid's row axis. */
function uniqueTimes(rows: SlotRow[]): (string | null)[] {
  const sorted = [...rows].sort((a, b) => (a.time ?? "").localeCompare(b.time ?? ""));
  const times: (string | null)[] = [];
  for (const row of sorted) {
    if (!times.includes(row.time)) times.push(row.time);
  }
  return times;
}
