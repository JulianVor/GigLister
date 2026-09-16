import Link from "next/link";
import { cookies } from "next/headers";
import { notFound } from "next/navigation";
import { ApiError, getEventSeries } from "@/lib/api";
import { getSession } from "@/lib/session";
import { canManageEntity } from "@/lib/permissions";
import { FESTIVAL_EVENTS_FILTER_COOKIE } from "@/lib/festival-cookies";
import { SeriesTimetable } from "@/components/SeriesTimetable";
import { FestivalEventsFilter } from "@/components/FestivalEventsFilter";
import { EmptyState } from "@/components/EmptyState";
import { EntityPlaceholder } from "@/components/EntityPlaceholder";

export default async function EventSeriesDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const seriesId = Number(id);
  const [session, cookieStore] = await Promise.all([getSession(), cookies()]);

  const series = await getEventSeries(seriesId).catch((err) => {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  });

  const canEdit = canManageEntity(session, "EVENT_SERIES", series.id);

  // Only a logged-in visitor has anything saved to filter down to - a guest's leftover
  // cookie from a previous session (or one they poked at directly) never gets to hide
  // every concert on the page out from under them.
  const filter = session && cookieStore.get(FESTIVAL_EVENTS_FILTER_COOKIE)?.value === "SAVED" ? "SAVED" : "ALL";
  const savedIds = new Set(session?.savedEvents.map((e) => e.id) ?? []);
  // Per event, the specific acts merkt within it (if any) - an event reached that path
  // (see UserService.saveAct) rather than a plain whole-event save.
  const savedActBandIdsByEvent = new Map<number, Set<number>>();
  for (const act of session?.savedActs ?? []) {
    if (!savedActBandIdsByEvent.has(act.eventId)) savedActBandIdsByEvent.set(act.eventId, new Set());
    savedActBandIdsByEvent.get(act.eventId)!.add(act.bandId);
  }
  const visibleEvents =
    filter === "SAVED"
      ? series.events
          .filter((e) => savedIds.has(e.id))
          .map((e) => {
            const savedActBandIds = savedActBandIdsByEvent.get(e.id);
            // No individual acts saved for this event - it was merkt as a whole, so its
            // full line-up still shows, same as the ALL view would for it.
            if (!savedActBandIds || savedActBandIds.size === 0) return e;
            return { ...e, bands: e.bands.filter((b) => savedActBandIds.has(b.id)) };
          })
      : series.events;

  return (
    <div className="max-w-2xl">
      {series.titleImageUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={series.titleImageUrl} alt="" className="mb-6 aspect-video w-full border border-line object-cover" />
      ) : (
        <EntityPlaceholder
          name={series.name}
          className="mb-6 aspect-video w-full border border-line"
          textClassName="text-6xl sm:text-7xl"
        />
      )}

      <h1 className="font-display text-4xl">{series.name}</h1>

      <div className="mt-4 flex flex-wrap gap-4 font-meta text-sm">
        {series.ticketUrl && (
          <a href={series.ticketUrl} target="_blank" rel="noreferrer noopener" className="text-accent hover:underline">
            Tickets
          </a>
        )}
        {canEdit && (
          <Link href={`/festivals/${series.id}/bearbeiten`} className="text-accent hover:underline">
            Bearbeiten
          </Link>
        )}
      </div>

      {series.description && <p className="mt-6 whitespace-pre-wrap leading-relaxed">{series.description}</p>}

      <div className="mt-10 flex items-baseline justify-between">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Konzerte dieses Festivals</h2>
        {session && series.events.length > 0 && <FestivalEventsFilter activeFilter={filter} />}
      </div>
      <div className="mt-2">
        {visibleEvents.length === 0 ? (
          <EmptyState>
            {filter === "SAVED"
              ? "Noch keine Konzerte dieses Festivals gemerkt."
              : "Noch keine Konzerte diesem Festival zugeordnet."}
          </EmptyState>
        ) : (
          <SeriesTimetable events={visibleEvents} style={series.timetableStyle} />
        )}
      </div>
    </div>
  );
}
