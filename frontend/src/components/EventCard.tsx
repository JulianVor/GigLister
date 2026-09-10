import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { dayAndMonth, dayNumber, formatTime, monthShort, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";

export function EventCard({ event }: { event: EventSummary }) {
  const time = formatTime(event.startTime);

  return (
    <Link href={`/konzerte/${event.id}`} className="group flex gap-5 border-b border-line py-6">
      {event.titleImageUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={event.titleImageUrl}
          alt=""
          className="h-28 w-28 flex-none border border-line object-cover sm:h-32 sm:w-32"
        />
      ) : (
        <div className="flex h-28 w-28 flex-none flex-col justify-between border border-line p-3 sm:h-32 sm:w-32">
          <div>
            <div className="font-display text-3xl leading-none">{dayNumber(event.date)}</div>
            <div className="font-meta text-xs text-muted">{monthShort(event.date)}</div>
          </div>
          <div className="font-meta text-xs leading-tight text-muted line-clamp-3">
            {event.bands.map((b) => b.name).join(" + ") || "—"}
          </div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col justify-center">
        <div className="font-meta text-sm tracking-wide text-muted">
          {weekdayShort(event.date)} {dayAndMonth(event.date)}
          {time ? ` · ${time}` : ""}
        </div>
        <div className="mt-1 truncate font-display text-xl group-hover:text-accent sm:text-2xl">
          {eventLineupLabel(event)}
        </div>
        <div className="mt-1 font-meta text-sm text-muted">
          {event.location.name} · {event.location.city}
        </div>
      </div>
    </Link>
  );
}
