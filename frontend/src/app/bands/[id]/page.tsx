import Link from "next/link";
import { notFound } from "next/navigation";
import { ApiError, getBand } from "@/lib/api";
import { getSession, getToken } from "@/lib/session";
import { canManageEntity } from "@/lib/permissions";
import { EventCard } from "@/components/EventCard";
import { EmptyState } from "@/components/EmptyState";
import { StatusBadge } from "@/components/StatusBadge";
import { FollowBandButton } from "@/components/FollowBandButton";
import { ClaimButton } from "@/components/ClaimButton";

export default async function BandDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const bandId = Number(id);
  const [session, token] = await Promise.all([getSession(), getToken()]);

  const band = await getBand(bandId, token).catch((err) => {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  });

  const canEdit = canManageEntity(session, "BAND", band.id);
  const following = session?.followedBands.some((b) => b.id === band.id) ?? false;

  return (
    <div className="max-w-2xl">
      {band.titleImageUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={band.titleImageUrl} alt="" className="mb-6 aspect-video w-full border border-line object-cover" />
      )}

      <div className="flex items-start gap-4">
        {band.logoUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={band.logoUrl} alt="" className="h-16 w-16 flex-none border border-line object-cover" />
        )}
        <div className="flex-1">
          <h1 className="font-display text-4xl">{band.name}</h1>
          <p className="mt-1 font-meta text-lg text-muted">
            {[band.city, band.region, band.country].filter(Boolean).join(" · ")}
          </p>
        </div>
        {/* Unvollständig/Entwurf is only meaningful to an admin or this band's own manager. */}
        {canEdit && <StatusBadge status={band.status} />}
      </div>

      {band.genres.length > 0 && (
        <div className="mt-3 flex gap-2 font-meta text-xs uppercase tracking-wide text-muted">
          {band.genres.map((g) => (
            <span key={g} className="border border-line px-2 py-0.5">
              {g}
            </span>
          ))}
        </div>
      )}

      {band.shortDescription && <p className="mt-4 leading-relaxed">{band.shortDescription}</p>}

      <div className="mt-6 flex flex-wrap gap-3">
        {session ? (
          <FollowBandButton bandId={band.id} initiallyFollowing={following} />
        ) : (
          <Link href="/login" className="border border-line px-5 py-2 font-meta text-sm hover:border-fg">
            Band folgen
          </Link>
        )}
        {band.website && (
          <a
            href={band.website}
            target="_blank"
            rel="noreferrer noopener"
            className="border border-line px-5 py-2 font-meta text-sm hover:border-fg"
          >
            Website
          </a>
        )}
        {canEdit && (
          <Link href={`/bands/${band.id}/bearbeiten`} className="border border-line px-5 py-2 font-meta text-sm hover:border-fg">
            Bearbeiten
          </Link>
        )}
      </div>

      <h2 className="mt-10 font-meta text-sm uppercase tracking-wide text-muted">Nächste Konzerte</h2>
      <div className="mt-2">
        {band.upcomingEvents.length === 0 ? (
          <EmptyState>Noch keine kommenden Konzerte gelistet.</EmptyState>
        ) : (
          band.upcomingEvents.map((e) => <EventCard key={e.id} event={e} />)
        )}
      </div>

      {band.unclaimed && session && (
        <div className="mt-10">
          <ClaimButton entityType="BAND" entityId={band.id} />
        </div>
      )}
    </div>
  );
}
