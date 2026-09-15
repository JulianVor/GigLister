import Link from "next/link";
import { getEvents, getGenreFilters } from "@/lib/api";
import { getLocationPrefs } from "@/lib/location-prefs";
import { resolveDateRange } from "@/lib/date-range";
import { DateNav } from "@/components/DateNav";
import { GenreFilter } from "@/components/GenreFilter";
import { EventListByDay } from "@/components/EventListByDay";
import { EmptyState } from "@/components/EmptyState";

const PAGE_SIZE = 20;

export default async function KonzerteePage({
  searchParams,
}: {
  searchParams: Promise<{ range?: string; from?: string; to?: string; genre?: string; page?: string }>;
}) {
  const params = await searchParams;
  const prefs = await getLocationPrefs();
  const { from, to, active } = resolveDateRange(params);
  const page = params.page ? Math.max(0, Number(params.page) - 1) : 0;
  const selectedGenres = params.genre ? params.genre.split(",").filter(Boolean) : [];
  const locationFilter = {
    city: prefs.city ?? undefined,
    lat: prefs.lat ?? undefined,
    lon: prefs.lon ?? undefined,
    radiusKm: prefs.radiusKm ?? undefined,
  };

  const [result, genreOptions] = await Promise.all([
    getEvents({ ...locationFilter, from, to, genre: params.genre, page, size: PAGE_SIZE }),
    getGenreFilters({ ...locationFilter, from, to }),
  ]);

  function pageHref(targetPage: number): string {
    const q = new URLSearchParams();
    if (params.range) q.set("range", params.range);
    if (params.from) q.set("from", params.from);
    if (params.to) q.set("to", params.to);
    if (params.genre) q.set("genre", params.genre);
    q.set("page", String(targetPage));
    return `/konzerte?${q.toString()}`;
  }

  return (
    <div>
      <h1 className="font-display text-3xl">Konzerte {prefs.city ? `in ${prefs.city}` : prefs.lat ? "in deiner Nähe" : ""}</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        {prefs.city
          ? `${prefs.city} · ${prefs.radiusKm ?? 25} km`
          : prefs.lat
            ? `Aktueller Standort · ${prefs.radiusKm ?? 25} km`
            : "Standort oben rechts wählen für eine Umkreissuche"}
      </p>

      <div className="mt-6">
        <DateNav active={active} genre={params.genre} />
      </div>

      <div className="mt-3">
        <GenreFilter
          options={genreOptions}
          active={selectedGenres}
          carryParams={{ range: params.range, from: params.from, to: params.to }}
        />
      </div>

      {result.content.length === 0 ? (
        <EmptyState>
          {selectedGenres.length > 0
            ? `Für diesen Zeitraum sind keine Konzerte in den Genres ${selectedGenres.join(", ")} gelistet.`
            : "Für diesen Zeitraum sind keine Konzerte gelistet."}
        </EmptyState>
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
