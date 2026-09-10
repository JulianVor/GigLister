import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession, getToken } from "@/lib/session";
import { getMyBands, getMyEvents } from "@/lib/api";
import { EventCard } from "@/components/EventCard";
import { EmptyState } from "@/components/EmptyState";
import { StatusBadge } from "@/components/StatusBadge";
import type { BandResponse, EventSummary } from "@/lib/types";

export default async function MeinGigListerPage() {
  const session = await getSession();
  if (!session) redirect("/login");

  const token = await getToken();
  const [myBands, myEvents]: [BandResponse[], EventSummary[]] = token
    ? await Promise.all([getMyBands(token), getMyEvents(token)])
    : [[], []];

  const myLocations = session.managedEntities.filter((e) => e.entityType === "LOCATION");

  return (
    <div>
      <h1 className="font-display text-3xl">Mein GigLister</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        {session.username} · {session.email}
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
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Meine Bands</h2>
        <div className="mt-2 divide-y divide-line border-y border-line">
          {myBands.length === 0 ? (
            <EmptyState>Du verwaltest noch keine Bands.</EmptyState>
          ) : (
            myBands.map((band) => {
              const permission = session.managedEntities.find(
                (e) => e.entityType === "BAND" && e.entityId === band.id
              )?.permission;
              return (
                <Link
                  key={band.id}
                  href={`/bands/${band.id}`}
                  className="flex items-center justify-between gap-4 py-3 hover:text-accent"
                >
                  <span className="flex items-center gap-2">
                    <span className="font-display text-lg">{band.name}</span>
                    <StatusBadge status={band.status} />
                  </span>
                  <span className="font-meta text-sm text-muted">
                    {band.city ?? "—"}
                    {permission ? ` · ${permission}` : ""}
                  </span>
                </Link>
              );
            })
          )}
        </div>
      </section>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Meine Veranstaltungen</h2>
        <p className="mt-1 font-meta text-xs text-muted">Kommende Konzerte über alle deine Bands hinweg.</p>
        <div className="mt-2">
          {myEvents.length === 0 ? (
            <EmptyState>Keine kommenden Konzerte deiner Bands.</EmptyState>
          ) : (
            myEvents.map((e) => <EventCard key={e.id} event={e} />)
          )}
        </div>
      </section>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Meine Orte</h2>
        <div className="mt-2 divide-y divide-line border-y border-line">
          {myLocations.length === 0 ? (
            <EmptyState>Du verwaltest noch keine Orte.</EmptyState>
          ) : (
            myLocations.map((entity) => (
              <Link
                key={entity.entityId}
                href={`/orte/${entity.entityId}`}
                className="flex items-center justify-between gap-4 py-3 hover:text-accent"
              >
                <span className="font-display text-lg">{entity.name}</span>
                <span className="font-meta text-sm text-muted">Location · {entity.permission}</span>
              </Link>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
