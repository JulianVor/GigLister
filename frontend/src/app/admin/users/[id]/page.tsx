import Link from "next/link";
import { notFound } from "next/navigation";
import { ApiError, getAdminUserDetail } from "@/lib/api";
import { NextConcertsList } from "@/components/NextConcertsList";
import { FollowedBandsRow } from "@/components/FollowedBandsRow";
import { EmptyState } from "@/components/EmptyState";
import { getToken } from "@/lib/session";

/**
 * "Details ansehen" - an admin's read-only view of one account: which Bands it folgt,
 * which Konzerte es gemerkt hat, plus what it manages. Same MeResponse shape and mostly
 * the same components (NextConcertsList/FollowedBandsRow) the account sees about itself
 * under Mein GigLister - see AdminService.userDetail.
 */
export default async function AdminUserDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const token = (await getToken())!;

  const user = await getAdminUserDetail(Number(id), token).catch((err) => {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  });

  const myLocations = user.managedEntities.filter((e) => e.entityType === "LOCATION");
  const myBands = user.managedEntities.filter((e) => e.entityType === "BAND");
  const myFestivals = user.managedEntities.filter((e) => e.entityType === "EVENT_SERIES");

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl">{user.username}</h1>
          <p className="mt-1 font-meta text-sm text-muted">{user.email}</p>
        </div>
        <div className="flex items-center gap-2">
          {user.mustChangePassword && (
            <span className="border border-line px-2 py-0.5 font-meta text-xs uppercase tracking-wide text-muted">
              Wartet auf Passwortänderung
            </span>
          )}
          <span className="border border-line px-2 py-0.5 font-meta text-xs uppercase tracking-wide text-muted">
            {user.platformAdmin ? "Admin" : "Nutzer"}
          </span>
        </div>
      </div>

      {(user.homeCity || user.preferredGenres.length > 0) && (
        <p className="mt-4 font-meta text-sm text-muted">
          {user.homeCity && `${user.homeCity} · ${user.radiusKm ?? 25} km`}
          {user.homeCity && user.preferredGenres.length > 0 && " · "}
          {user.preferredGenres.length > 0 && `Genres: ${user.preferredGenres.join(", ")}`}
        </p>
      )}

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Gefolgte Bands</h2>
        <div className="mt-2">
          {user.followedBands.length === 0 ? (
            <EmptyState>Folgt noch keinen Bands.</EmptyState>
          ) : (
            <FollowedBandsRow bands={user.followedBands} />
          )}
        </div>
      </section>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Gemerkte Konzerte</h2>
        <div className="mt-2">
          {user.savedEvents.length === 0 ? (
            <EmptyState>Noch keine Konzerte gemerkt.</EmptyState>
          ) : (
            <NextConcertsList events={user.savedEvents} />
          )}
        </div>
      </section>

      {(myBands.length > 0 || myLocations.length > 0 || myFestivals.length > 0) && (
        <section className="mt-10">
          <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Verwaltet</h2>
          <div className="mt-2 divide-y divide-line border-y border-line">
            {myBands.map((entity) => (
              <Link
                key={`band-${entity.entityId}`}
                href={`/bands/${entity.entityId}`}
                className="flex items-center justify-between gap-4 py-3 hover:text-accent"
              >
                <span className="font-display text-lg">{entity.name}</span>
                <span className="font-meta text-sm text-muted">Band · {entity.permission}</span>
              </Link>
            ))}
            {myLocations.map((entity) => (
              <Link
                key={`location-${entity.entityId}`}
                href={`/orte/${entity.entityId}`}
                className="flex items-center justify-between gap-4 py-3 hover:text-accent"
              >
                <span className="font-display text-lg">{entity.name}</span>
                <span className="font-meta text-sm text-muted">Ort · {entity.permission}</span>
              </Link>
            ))}
            {myFestivals.map((entity) => (
              <Link
                key={`festival-${entity.entityId}`}
                href={`/festivals/${entity.entityId}`}
                className="flex items-center justify-between gap-4 py-3 hover:text-accent"
              >
                <span className="font-display text-lg">{entity.name}</span>
                <span className="font-meta text-sm text-muted">Festival · {entity.permission}</span>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
