import Link from "next/link";
import { notFound } from "next/navigation";
import { ApiError, getEvent, getLocation } from "@/lib/api";
import { getSession, getToken } from "@/lib/session";
import { canEditEvent } from "@/lib/permissions";
import { dayAndMonth, fullDateLabel, formatTime, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";
import { LineUp } from "@/components/LineUp";
import { SaveEventButton } from "@/components/SaveEventButton";
import { StatusBadge } from "@/components/StatusBadge";
import { EventCard } from "@/components/EventCard";

export default async function EventDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const eventId = Number(id);

  const [event, session, token] = await Promise.all([
    getEvent(eventId).catch((err) => {
      if (err instanceof ApiError && err.status === 404) notFound();
      throw err;
    }),
    getSession(),
    getToken(),
  ]);

  const location = await getLocation(event.location.id, token).catch(() => null);
  const otherAtLocation = location
    ? location.upcomingEvents.filter((e) => e.id !== event.id).slice(0, 4)
    : [];

  const saved = session?.savedEvents.some((e) => e.id === event.id) ?? false;
  const canEdit = canEditEvent(session, event);

  return (
    <div className="max-w-2xl">
      {event.status === "CANCELLED" && (
        <p className="mb-4 border border-accent px-3 py-2 font-meta text-sm text-accent">Dieses Konzert wurde abgesagt.</p>
      )}

      <div className="font-meta text-lg tracking-wide text-muted">
        {weekdayShort(event.date)}
        <br />
        <span className="font-display text-3xl text-fg">{dayAndMonth(event.date)}</span>
        {formatTime(event.startTime) && <span className="ml-2">{formatTime(event.startTime)}</span>}
      </div>
      <p className="font-meta text-sm text-muted">{fullDateLabel(event.date)}</p>

      <h1 className="mt-4 font-display text-4xl leading-tight">{eventLineupLabel(event)}</h1>

      <p className="mt-2 font-meta text-lg">
        {event.location.linkable || session ? (
          <Link href={`/orte/${event.location.id}`} className="hover:text-accent">
            {event.location.name}
          </Link>
        ) : (
          event.location.name
        )}
        , {event.location.city}
      </p>

      <div className="mt-6 flex flex-wrap gap-3">
        {event.ticketUrl && (
          <a
            href={event.ticketUrl}
            target="_blank"
            rel="noreferrer noopener"
            className="border border-fg bg-fg px-5 py-2 font-meta text-sm text-bg hover:bg-accent hover:border-accent hover:text-accent-fg"
          >
            Tickets
          </a>
        )}
        {session ? (
          <SaveEventButton eventId={event.id} initiallySaved={saved} />
        ) : (
          <Link href="/login" className="border border-line px-5 py-2 font-meta text-sm hover:border-fg">
            Merken
          </Link>
        )}
        {canEdit && (
          <Link
            href={`/konzerte/${event.id}/bearbeiten`}
            className="border border-line px-5 py-2 font-meta text-sm hover:border-fg"
          >
            Bearbeiten
          </Link>
        )}
      </div>

      {event.description && <p className="mt-8 whitespace-pre-wrap leading-relaxed">{event.description}</p>}

      <h2 className="mt-10 font-meta text-sm uppercase tracking-wide text-muted">Line-up</h2>
      <div className="mt-2">
        <LineUp bands={event.bands} loggedIn={!!session} />
      </div>

      <h2 className="mt-10 font-meta text-sm uppercase tracking-wide text-muted">Ort</h2>
      <div className="mt-2">
        {event.location.linkable || session ? (
          <Link href={`/orte/${event.location.id}`} className="font-display text-xl hover:text-accent">
            {event.location.name}
          </Link>
        ) : (
          <p className="font-display text-xl">{event.location.name}</p>
        )}
        {location?.address && <p className="font-meta text-sm text-muted">{location.address}</p>}
        <p className="font-meta text-sm text-muted">{event.location.city}</p>
        <StatusBadge status={event.location.status} />
      </div>

      {otherAtLocation.length > 0 && (
        <div className="mt-12">
          <h2 className="font-meta text-sm uppercase tracking-wide text-muted">
            Weitere Konzerte {location?.name ? `im ${location.name}` : "dort"}
          </h2>
          <div className="mt-2">
            {otherAtLocation.map((e) => (
              <EventCard key={e.id} event={e} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
