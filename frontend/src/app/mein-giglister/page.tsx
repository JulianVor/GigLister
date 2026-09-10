import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { EventCard } from "@/components/EventCard";
import { EmptyState } from "@/components/EmptyState";

export default async function MeinGigListerPage() {
  const session = await getSession();
  if (!session) redirect("/login");

  return (
    <div>
      <h1 className="font-display text-3xl">Mein GigLister</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        {session.displayName} · {session.email}
      </p>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Gemerkt</h2>
        <div className="mt-2">
          {session.savedEvents.length === 0 ? (
            <EmptyState>Noch keine Konzerte gemerkt.</EmptyState>
          ) : (
            session.savedEvents.map((e) => <EventCard key={e.id} event={e} />)
          )}
        </div>
      </section>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Gefolgte Bands</h2>
        <div className="mt-2 divide-y divide-line border-y border-line">
          {session.followedBands.length === 0 ? (
            <EmptyState>Noch keinen Bands gefolgt.</EmptyState>
          ) : (
            session.followedBands.map((band) => (
              <Link key={band.id} href={`/bands/${band.id}`} className="flex items-center justify-between gap-4 py-3 hover:text-accent">
                <span className="font-display text-lg">{band.name}</span>
                <span className="font-meta text-sm text-muted">
                  {band.nextEventDate ? `Nächstes Konzert ${band.nextEventDate}` : "Kein kommendes Konzert"}
                </span>
              </Link>
            ))
          )}
        </div>
      </section>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Von mir verwaltet</h2>
        <div className="mt-2 divide-y divide-line border-y border-line">
          {session.managedEntities.length === 0 ? (
            <EmptyState>Du verwaltest noch keine Bands oder Orte.</EmptyState>
          ) : (
            session.managedEntities.map((entity) => (
              <Link
                key={`${entity.entityType}-${entity.entityId}`}
                href={entity.entityType === "BAND" ? `/bands/${entity.entityId}` : `/orte/${entity.entityId}`}
                className="flex items-center justify-between gap-4 py-3 hover:text-accent"
              >
                <span className="font-display text-lg">{entity.name}</span>
                <span className="font-meta text-sm text-muted">
                  {entity.entityType === "BAND" ? "Band" : "Location"} · {entity.permission}
                </span>
              </Link>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
