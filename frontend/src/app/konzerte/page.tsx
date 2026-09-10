import Link from "next/link";
import { getEvents } from "@/lib/api";
import { getLocationPrefs } from "@/lib/location-prefs";
import { resolveDateRange } from "@/lib/date-range";
import { DateNav } from "@/components/DateNav";
import { EventListByDay } from "@/components/EventListByDay";
import { EmptyState } from "@/components/EmptyState";

const PAGE_SIZE = 20;

export default async function KonzerteePage({
  searchParams,
}: {
  searchParams: Promise<{ range?: string; from?: string; to?: string; page?: string }>;
}) {
  const params = await searchParams;
  const prefs = await getLocationPrefs();
  const { from, to, active } = resolveDateRange(params);
  const page = params.page ? Math.max(0, Number(params.page) - 1) : 0;

  const result = await getEvents({
    city: prefs.city ?? undefined,
    radiusKm: prefs.radiusKm ?? undefined,
    from,
    to,
    page,
    size: PAGE_SIZE,
  });

  function pageHref(targetPage: number): string {
    const q = new URLSearchParams();
    if (params.range) q.set("range", params.range);
    if (params.from) q.set("from", params.from);
    if (params.to) q.set("to", params.to);
    q.set("page", String(targetPage));
    return `/konzerte?${q.toString()}`;
  }

  return (
    <div>
      <h1 className="font-display text-3xl">Konzerte {prefs.city ? `in ${prefs.city}` : ""}</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        {prefs.city ? `${prefs.city} · ${prefs.radiusKm ?? 25} km` : "Standort oben rechts wählen für eine Umkreissuche"}
      </p>

      <div className="mt-6">
        <DateNav active={active} />
      </div>

      {result.content.length === 0 ? (
        <EmptyState>Für diesen Zeitraum sind keine Konzerte gelistet.</EmptyState>
      ) : (
        <EventListByDay events={result.content} />
      )}

      <div className="mt-8 flex justify-between font-meta text-sm">
        {page > 0 ? (
          <Link href={pageHref(page)} className="text-accent hover:underline">
            ← Zurück
          </Link>
        ) : (
          <span />
        )}
        {page + 1 < result.totalPages && (
          <Link href={pageHref(page + 2)} className="text-accent hover:underline">
            Weiter →
          </Link>
        )}
      </div>
    </div>
  );
}
