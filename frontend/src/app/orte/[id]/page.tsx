import Link from "next/link";
import { notFound } from "next/navigation";
import { ApiError, getLocation } from "@/lib/api";
import { getSession, getToken } from "@/lib/session";
import { canManageEntity } from "@/lib/permissions";
import { EventCard } from "@/components/EventCard";
import { EmptyState } from "@/components/EmptyState";
import { StatusBadge } from "@/components/StatusBadge";
import { ClaimButton } from "@/components/ClaimButton";

export default async function LocationDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const locationId = Number(id);
  const [session, token] = await Promise.all([getSession(), getToken()]);

  const location = await getLocation(locationId, token).catch(async (err) => {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  });

  const canEdit = canManageEntity(session, "LOCATION", location.id);
  const pastYears = Object.keys(location.pastEventsByYear)
    .map(Number)
    .sort((a, b) => b - a);

  return (
    <div className="max-w-2xl">
      {location.titleImageUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={location.titleImageUrl} alt="" className="mb-6 aspect-video w-full border border-line object-cover" />
      )}

      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-4xl">{location.name}</h1>
          <p className="mt-1 font-meta text-lg text-muted">
            {location.city}
            {location.address ? ` · ${location.address}` : ""}
          </p>
        </div>
        {/* Unvollständig/Entwurf is only meaningful to an admin or this location's own manager. */}
        {canEdit && <StatusBadge status={location.status} />}
      </div>

      <div className="mt-4 flex flex-wrap gap-4 font-meta text-sm">
        {location.website && (
          <a href={location.website} target="_blank" rel="noreferrer noopener" className="text-accent hover:underline">
            Website
          </a>
        )}
        {location.address && (
          <a
            href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${location.name} ${location.address} ${location.city}`)}`}
            target="_blank"
            rel="noreferrer noopener"
            className="text-accent hover:underline"
          >
            Route
          </a>
        )}
        {canEdit && (
          <Link href={`/orte/${location.id}/bearbeiten`} className="text-accent hover:underline">
            Bearbeiten
          </Link>
        )}
      </div>

      <h2 className="mt-10 font-meta text-sm uppercase tracking-wide text-muted">Kommende Konzerte</h2>
      <div className="mt-2">
        {location.upcomingEvents.length === 0 ? (
          <EmptyState>Noch keine kommenden Konzerte gelistet.</EmptyState>
        ) : (
          location.upcomingEvents.map((e) => <EventCard key={e.id} event={e} />)
        )}
      </div>

      {pastYears.length > 0 && (
        <div className="mt-10">
          <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Vergangene Konzerte</h2>
          {pastYears.map((year) => (
            <details key={year} className="mt-2 border-b border-line py-2">
              <summary className="cursor-pointer font-meta text-sm">{year}</summary>
              <div className="mt-2">
                {location.pastEventsByYear[String(year)].map((e) => (
                  <EventCard key={e.id} event={e} />
                ))}
              </div>
            </details>
          ))}
        </div>
      )}

      {location.unclaimed && session && (
        <div className="mt-10">
          <ClaimButton entityType="LOCATION" entityId={location.id} />
        </div>
      )}
    </div>
  );
}
