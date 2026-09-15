import Link from "next/link";
import { notFound } from "next/navigation";
import { ApiError, getEventSeries } from "@/lib/api";
import { getSession } from "@/lib/session";
import { canManageEntity } from "@/lib/permissions";
import { SeriesTimetable } from "@/components/SeriesTimetable";
import { EmptyState } from "@/components/EmptyState";
import { EntityPlaceholder } from "@/components/EntityPlaceholder";

export default async function EventSeriesDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const seriesId = Number(id);
  const session = await getSession();

  const series = await getEventSeries(seriesId).catch((err) => {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  });

  const canEdit = canManageEntity(session, "EVENT_SERIES", series.id);

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
          <Link href={`/reihen/${series.id}/bearbeiten`} className="text-accent hover:underline">
            Bearbeiten
          </Link>
        )}
      </div>

      {series.description && <p className="mt-6 whitespace-pre-wrap leading-relaxed">{series.description}</p>}

      <h2 className="mt-10 font-meta text-sm uppercase tracking-wide text-muted">Konzerte dieser Reihe</h2>
      <div className="mt-2">
        {series.events.length === 0 ? (
          <EmptyState>Noch keine Konzerte dieser Reihe zugeordnet.</EmptyState>
        ) : (
          <SeriesTimetable events={series.events} />
        )}
      </div>
    </div>
  );
}
