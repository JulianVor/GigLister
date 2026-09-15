import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession, getToken } from "@/lib/session";
import { getMyBands, getMyEvents, getMySubmissions } from "@/lib/api";
import { EventCard } from "@/components/EventCard";
import { StatusBadge } from "@/components/StatusBadge";
import { SUBMISSION_STATUS_LABELS } from "@/lib/status-labels";
import type { BandResponse, EventSummary, SubmissionResponse } from "@/lib/types";

/**
 * What's left of the old "Mein GigLister" once its consumer-facing content (Gemerkt,
 * Gefolgte Bands) moved to the homepage feed - just the creator/manager side: bands,
 * locations and events this account actually has EDIT/MANAGE rights on.
 */
export default async function VerwaltungPage() {
  const session = await getSession();
  if (!session) redirect("/login");

  const token = await getToken();
  const [myBands, myEvents, mySubmissions]: [BandResponse[], EventSummary[], SubmissionResponse[]] = token
    ? await Promise.all([getMyBands(token), getMyEvents(token), getMySubmissions(token)])
    : [[], [], []];

  const myLocations = session.managedEntities.filter((e) => e.entityType === "LOCATION");
  const nothingManaged = myBands.length === 0 && myLocations.length === 0;

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl">Verwaltung</h1>
          <p className="mt-1 font-meta text-sm text-muted">
            {session.username} · {session.email}
          </p>
        </div>
        <Link href="/einstellungen" className="font-meta text-sm text-accent hover:underline">
          Einstellungen
        </Link>
      </div>

      {nothingManaged && (
        <p className="mt-6 border border-line bg-surface p-4 font-meta text-sm text-muted">
          Du verwaltest noch keine Band oder Location. Lege eine{" "}
          <Link href="/bands/neu" className="text-accent hover:underline">
            Band
          </Link>{" "}
          oder einen{" "}
          <Link href="/orte/neu" className="text-accent hover:underline">
            Ort
          </Link>{" "}
          an, oder beanspruche eine bestehende über deren Seite.
        </p>
      )}

      {myBands.length > 0 && (
        <section className="mt-10">
          <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Meine Bands</h2>
          <div className="mt-2 divide-y divide-line border-y border-line">
            {myBands.map((band) => {
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
            })}
          </div>
        </section>
      )}

      {myEvents.length > 0 && (
        <section className="mt-10">
          <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Meine Veranstaltungen</h2>
          <p className="mt-1 font-meta text-xs text-muted">Kommende Konzerte über alle deine Bands hinweg.</p>
          <div className="mt-2">
            {myEvents.map((e) => (
              <EventCard key={e.id} event={e} />
            ))}
          </div>
        </section>
      )}

      {mySubmissions.length > 0 && (
        <section className="mt-10">
          <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Meine Vorschläge</h2>
          <p className="mt-1 font-meta text-xs text-muted">
            Konzerte, die du eingereicht hast, weil du keine der ausgewählten Bands oder die Location verwaltest -
            wir prüfen sie, bevor sie veröffentlicht werden.
          </p>
          <div className="mt-2 divide-y divide-line border-y border-line">
            {mySubmissions.map((s) => (
              <SubmissionSummary key={s.id} submission={s} />
            ))}
          </div>
        </section>
      )}

      {myLocations.length > 0 && (
        <section className="mt-10">
          <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Meine Orte</h2>
          <div className="mt-2 divide-y divide-line border-y border-line">
            {myLocations.map((entity) => (
              <Link
                key={entity.entityId}
                href={`/orte/${entity.entityId}`}
                className="flex items-center justify-between gap-4 py-3 hover:text-accent"
              >
                <span className="font-display text-lg">{entity.name}</span>
                <span className="font-meta text-sm text-muted">Location · {entity.permission}</span>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

/** Renders an EVENT submission's payload (the only type a user can propose themselves
 * today - see createEvent) as a plain summary line, with a status badge and, once
 * decided, the reason/link to what came of it. No edit/approve actions here - those are
 * admin-only (see admin/submissions). */
function SubmissionSummary({ submission }: { submission: SubmissionResponse }) {
  const payload = submission.payload as {
    title?: string;
    date?: string;
    location?: { name?: string };
    bands?: { name?: string }[];
  };
  const label = payload.title || payload.bands?.map((b) => b.name).filter(Boolean).join(", ") || "Konzert";

  return (
    <div className="flex flex-wrap items-center justify-between gap-2 py-3">
      <div>
        <div className="font-display text-lg">{label}</div>
        <div className="font-meta text-sm text-muted">
          {[payload.date, payload.location?.name].filter(Boolean).join(" · ")}
        </div>
        {submission.status === "REJECTED" && submission.rejectionReason && (
          <p className="mt-1 font-meta text-xs text-muted">Grund: „{submission.rejectionReason}“</p>
        )}
      </div>
      {submission.status === "APPROVED" && submission.resultEntityId ? (
        <Link href={`/konzerte/${submission.resultEntityId}`} className="font-meta text-sm text-accent hover:underline">
          {SUBMISSION_STATUS_LABELS[submission.status]} →
        </Link>
      ) : (
        <span className="font-meta text-sm text-muted">{SUBMISSION_STATUS_LABELS[submission.status]}</span>
      )}
    </div>
  );
}
