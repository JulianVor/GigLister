import type { EventSummary } from "@/lib/types";
import { fullDateLabel, isSameDate } from "@/lib/format";
import { EventCard } from "./EventCard";

/** Renders events chronologically, inserting a day heading whenever the date changes.
 * With `anchors`, each day heading gets an id (`d-YYYY-MM-DD`) so other UI - the month
 * calendar grid - can link straight to that day within the same page. */
export function EventListByDay({ events, anchors }: { events: EventSummary[]; anchors?: boolean }) {
  return (
    <div>
      {events.map((event, i) => {
        const previous = events[i - 1];
        const isNewDay = !previous || !isSameDate(previous.date, event.date);
        return (
          <div key={event.id}>
            {isNewDay && (
              <h2
                id={anchors ? `d-${event.date}` : undefined}
                className="mt-8 mb-1 scroll-mt-4 font-meta text-sm uppercase tracking-wide text-muted first:mt-0"
              >
                {fullDateLabel(event.date)}
              </h2>
            )}
            <EventCard event={event} />
          </div>
        );
      })}
    </div>
  );
}
