import Link from "next/link";
import { getEvents } from "@/lib/api";
import { getLocationPrefs } from "@/lib/location-prefs";
import { fullDateLabel, todayISO } from "@/lib/format";
import { DateNav } from "@/components/DateNav";
import { EventCard } from "@/components/EventCard";
import { EmptyState } from "@/components/EmptyState";

export default async function HomePage() {
  const prefs = await getLocationPrefs();
  const today = todayISO();

  const page = await getEvents({
    city: prefs.city ?? undefined,
    lat: prefs.lat ?? undefined,
    lon: prefs.lon ?? undefined,
    radiusKm: prefs.radiusKm ?? undefined,
    from: today,
    to: today,
    size: 20,
  });

  return (
    <div>
      <h1 className="font-display text-4xl leading-tight sm:text-5xl">
        Konzerte {prefs.city ? `in ${prefs.city}` : "in deiner Nähe"}
      </h1>

      <div className="mt-6">
        <DateNav active="today" />
      </div>

      <h2 className="mt-8 mb-1 font-meta text-sm uppercase tracking-wide text-muted">
        Heute · {fullDateLabel(today)}
      </h2>

      {page.content.length === 0 ? (
        <EmptyState>
          Heute ist nichts gelistet.{" "}
          <Link href="/konzerte?range=weekend" className="text-accent hover:underline">
            Konzerte am Wochenende ansehen →
          </Link>
        </EmptyState>
      ) : (
        <div>
          {page.content.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
      )}

      <div className="mt-8">
        <Link href="/konzerte" className="font-meta text-sm text-accent hover:underline">
          Alle kommenden Konzerte ansehen →
        </Link>
      </div>
    </div>
  );
}
