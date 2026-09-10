import type { EventSummary } from "@/lib/types";
import { fullDateLabel, isSameDate } from "@/lib/format";
import { EventCard } from "./EventCard";

/** Renders events chronologically, inserting a day heading whenever the date changes. */
export function EventListByDay({ events }: { events: EventSummary[] }) {
  return (
    <div>
      {events.map((event, i) => {
        const previous = events[i - 1];
        const isNewDay = !previous || !isSameDate(previous.date, event.date);
        return (
          <div key={event.id}>
            {isNewDay && (
              <h2 className="mt-8 mb-1 font-meta text-sm uppercase tracking-wide text-muted first:mt-0">
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
