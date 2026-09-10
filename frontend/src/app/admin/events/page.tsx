import Link from "next/link";
import { getAdminEvents } from "@/lib/api";
import { getToken } from "@/lib/session";
import { StatusFilter } from "@/components/admin/StatusFilter";
import { AdminSearchForm } from "@/components/admin/AdminSearchForm";
import { dayAndMonth, weekdayShort } from "@/lib/format";
import type { EventStatus } from "@/lib/types";

const STATUSES: EventStatus[] = ["DRAFT", "PUBLISHED", "CANCELLED"];

export default async function AdminEventsPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: EventStatus; q?: string }>;
}) {
  const { status, q } = await searchParams;
  const token = (await getToken())!;
  const page = await getAdminEvents({ status, q, size: 100 }, token);

  return (
    <div>
      <h1 className="font-display text-3xl">Konzerte</h1>
      <p className="mt-1 font-meta text-sm text-muted">Alle Konzerte, unabhängig vom Status.</p>

      <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
        <StatusFilter basePath="/admin/events" statuses={STATUSES} active={status} query={q} />
        <AdminSearchForm
          action="/admin/events"
          query={q}
          placeholder="Suche nach Titel oder Band …"
          hidden={{ status }}
        />
      </div>

      <p className="mt-4 font-meta text-xs text-muted">{page.totalElements} Konzerte</p>

      <div className="mt-2 divide-y divide-line border-y border-line">
        {page.content.length === 0 ? (
          <p className="py-6 font-meta text-sm text-muted">Keine Konzerte gefunden.</p>
        ) : (
          page.content.map((event) => (
            <div key={event.id} className="flex items-center justify-between gap-4 py-3">
              <div>
                <div className="font-meta text-xs text-muted">
                  {weekdayShort(event.date)} {dayAndMonth(event.date)}
                </div>
                <Link href={`/konzerte/${event.id}`} className="font-display text-lg hover:text-accent">
                  {event.title || event.bandNames.join(" + ") || "Konzert"}
                </Link>
                <div className="font-meta text-sm text-muted">{event.locationName}</div>
              </div>
              <div className="flex items-center gap-3">
                <span className="border border-line px-2 py-0.5 font-meta text-xs uppercase tracking-wide text-muted">
                  {event.status}
                </span>
                <Link href={`/konzerte/${event.id}/bearbeiten`} className="font-meta text-sm text-accent hover:underline">
                  Bearbeiten
                </Link>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
