import Link from "next/link";
import { getAdminLocations } from "@/lib/api";
import { getToken } from "@/lib/session";
import { StatusFilter } from "@/components/admin/StatusFilter";
import { AdminSearchForm } from "@/components/admin/AdminSearchForm";
import { ENTITY_STATUS_HINTS, ENTITY_STATUS_LABELS } from "@/lib/status-labels";
import type { EntityStatus } from "@/lib/types";

const STATUSES: EntityStatus[] = ["STUB", "DRAFT", "PUBLISHED", "ARCHIVED"];

export default async function AdminLocationsPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: EntityStatus; q?: string }>;
}) {
  const { status, q } = await searchParams;
  const token = (await getToken())!;
  const page = await getAdminLocations({ status, q, size: 100 }, token);

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl">Orte</h1>
          <p className="mt-1 font-meta text-sm text-muted">
            Alle Locations, unabhängig vom Status — auch Stubs und Entwürfe, die noch Pflege brauchen.
          </p>
        </div>
        <Link
          href="/orte/neu"
          className="whitespace-nowrap bg-fg px-4 py-2 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg"
        >
          + Neuer Ort
        </Link>
      </div>

      <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
        <StatusFilter basePath="/admin/locations" statuses={STATUSES} active={status} query={q} labels={ENTITY_STATUS_LABELS} />
        <AdminSearchForm action="/admin/locations" query={q} placeholder="Suche nach Name …" hidden={{ status }} />
      </div>

      <p className="mt-4 font-meta text-xs text-muted">{page.totalElements} Orte</p>

      <div className="mt-2 divide-y divide-line border-y border-line">
        {page.content.length === 0 ? (
          <p className="py-6 font-meta text-sm text-muted">Keine Orte gefunden.</p>
        ) : (
          page.content.map((location) => (
            <div key={location.id} className="flex items-center justify-between gap-4 py-3">
              <div>
                <Link href={`/orte/${location.id}`} className="font-display text-lg hover:text-accent">
                  {location.name}
                </Link>
                {location.city && <div className="font-meta text-sm text-muted">{location.city}</div>}
              </div>
              <div className="flex items-center gap-3">
                <span
                  title={ENTITY_STATUS_HINTS[location.status]}
                  className="border border-line px-2 py-0.5 font-meta text-xs uppercase tracking-wide text-muted"
                >
                  {ENTITY_STATUS_LABELS[location.status]}
                </span>
                <Link href={`/orte/${location.id}/bearbeiten`} className="font-meta text-sm text-accent hover:underline">
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
