import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { dayAndMonth, dayNumber, formatTime, monthShort, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";

/** An image-forward post-style card (photo up top, a floating date pill, details below) -
 * self-spaced (`mb-4`) so every list of these just stacks without callers adding gaps. */
export function EventCard({ event }: { event: EventSummary }) {
  const time = formatTime(event.startTime);

  return (
    <Link href={`/konzerte/${event.id}`} className="group mb-4 block border border-line hover:border-fg">
      <div className="relative h-44 w-full overflow-hidden border-b border-line bg-surface sm:h-60">
        {event.titleImageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={event.titleImageUrl}
            alt=""
            className="h-full w-full object-cover transition-transform duration-300 ease-out group-hover:scale-[1.04]"
          />
        ) : (
          <div className="flex h-full w-full flex-col items-center justify-center gap-1">
            <div className="font-display text-6xl leading-none sm:text-7xl">{dayNumber(event.date)}</div>
            <div className="font-meta text-sm uppercase tracking-wide text-muted">{monthShort(event.date)}</div>
          </div>
        )}
        <div className="absolute left-3 top-3 border border-line bg-bg/90 px-2 py-1 font-meta text-xs uppercase tracking-wide">
          {weekdayShort(event.date)} {dayAndMonth(event.date)}
          {time ? ` · ${time}` : ""}
        </div>
      </div>

      <div className="p-4">
        <div className="truncate font-display text-xl group-hover:text-accent sm:text-2xl">
          {eventLineupLabel(event)}
        </div>
        <div className="mt-1 font-meta text-sm text-muted">
          {event.location.name} · {event.location.city}
        </div>
      </div>
    </Link>
  );
}
