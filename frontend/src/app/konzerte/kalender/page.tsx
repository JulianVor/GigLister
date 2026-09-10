import { endOfMonth, startOfMonth } from "date-fns";
import { getCalendar, getEvents } from "@/lib/api";
import { getLocationPrefs } from "@/lib/location-prefs";
import { toDateOnly } from "@/lib/format";
import { Calendar } from "@/components/Calendar";
import { EventListByDay } from "@/components/EventListByDay";
import { EmptyState } from "@/components/EmptyState";

export default async function KalenderPage({
  searchParams,
}: {
  searchParams: Promise<{ year?: string; month?: string }>;
}) {
  const params = await searchParams;
  const now = new Date();
  const year = params.year ? Number(params.year) : now.getFullYear();
  const month = params.month ? Number(params.month) : now.getMonth() + 1;

  const prefs = await getLocationPrefs();
  const geo = {
    city: prefs.city ?? undefined,
    lat: prefs.lat ?? undefined,
    lon: prefs.lon ?? undefined,
    radiusKm: prefs.radiusKm ?? undefined,
  };

  const monthDate = new Date(year, month - 1, 1);
  const monthStart = toDateOnly(startOfMonth(monthDate));
  const monthEnd = toDateOnly(endOfMonth(monthDate));

  const [counts, monthEvents] = await Promise.all([
    getCalendar({ year, month, ...geo }),
    getEvents({ ...geo, from: monthStart, to: monthEnd, size: 300 }),
  ]);

  return (
    <div>
      <h1 className="font-display text-3xl">Kalender</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        {prefs.city ? `${prefs.city} · ${prefs.radiusKm ?? 25} km` : prefs.lat ? `Aktueller Standort · ${prefs.radiusKm ?? 25} km` : "Alle Konzerte"}
      </p>

      <div className="mt-6">
        <Calendar year={year} month={month} counts={counts} />
      </div>

      <div className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Konzerte in diesem Monat</h2>
        <div className="mt-2">
          {monthEvents.content.length === 0 ? (
            <EmptyState>In diesem Monat sind noch keine Konzerte gelistet.</EmptyState>
          ) : (
            <EventListByDay events={monthEvents.content} anchors />
          )}
        </div>
      </div>
    </div>
  );
}
