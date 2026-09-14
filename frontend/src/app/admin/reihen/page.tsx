import Link from "next/link";
import { getAdminEventSeries } from "@/lib/api";
import { getToken } from "@/lib/session";
import { AdminSearchForm } from "@/components/admin/AdminSearchForm";

export default async function AdminEventSeriesPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const { q } = await searchParams;
  const token = (await getToken())!;
  const page = await getAdminEventSeries({ q, size: 100 }, token);

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl">Reihen</h1>
          <p className="mt-1 font-meta text-sm text-muted">
            Festivals/Nächte, die mehrere Konzerte bündeln - z. B. &bdquo;SüdKultur MusicNight&ldquo;.
          </p>
        </div>
        <Link
          href="/reihen/neu"
          className="whitespace-nowrap bg-fg px-4 py-2 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg"
        >
          + Neue Reihe
        </Link>
      </div>

      <div className="mt-6">
        <AdminSearchForm action="/admin/reihen" query={q} placeholder="Suche nach Name …" />
      </div>

      <p className="mt-4 font-meta text-xs text-muted">{page.totalElements} Reihen</p>

      <div className="mt-2 divide-y divide-line border-y border-line">
        {page.content.length === 0 ? (
          <p className="py-6 font-meta text-sm text-muted">Keine Reihen gefunden.</p>
        ) : (
          page.content.map((series) => (
            <div key={series.id} className="flex items-center justify-between gap-4 py-3">
              <Link href={`/reihen/${series.id}`} className="font-display text-lg hover:text-accent">
                {series.name}
              </Link>
              <div className="flex items-center gap-3">
                <span className="font-meta text-sm text-muted">
                  {series.eventCount} {series.eventCount === 1 ? "Konzert" : "Konzerte"}
                </span>
                <Link href={`/reihen/${series.id}/bearbeiten`} className="font-meta text-sm text-accent hover:underline">
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
