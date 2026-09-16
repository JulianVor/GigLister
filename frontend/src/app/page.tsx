import Link from "next/link";
import { discover, getEvents } from "@/lib/api";
import { getLocationPrefs } from "@/lib/location-prefs";
import { getSession, getToken } from "@/lib/session";
import { todayISO } from "@/lib/format";
import { buildMapLocations } from "@/lib/map";
import { ConcertMapClient } from "@/components/ConcertMapClient";
import { EventCard } from "@/components/EventCard";
import { LocationTeaser } from "@/components/LocationTeaser";
import { EmptyState } from "@/components/EmptyState";
import { StatusBadge } from "@/components/StatusBadge";
import { NextConcertsList } from "@/components/NextConcertsList";
import { FollowedBandsRow } from "@/components/FollowedBandsRow";
import type { EventSummary } from "@/lib/types";

/**
 * The feed every visitor lands on - merges what used to be split across three places:
 * the plain "heute in deiner Nähe" this page only used to show, Entdecken's curated
 * sections (now folded in here instead of behind a separate nav item), and the
 * logged-in-only "Gemerkt"/"Gefolgte Bands" that used to be buried in Mein GigLister
 * (now "Verwaltung", left with just the creator tools - see that page).
 */
export default async function HomePage() {
  const [prefs, session, token] = await Promise.all([getLocationPrefs(), getSession(), getToken()]);
  const today = todayISO();
  const hasLocation = prefs.lat != null && prefs.lon != null;

  const [data, todayPage] = await Promise.all([
    discover(
      {
        city: prefs.city ?? undefined,
        lat: prefs.lat ?? undefined,
        lon: prefs.lon ?? undefined,
        radiusKm: prefs.radiusKm ?? undefined,
      },
      token
    ),
    hasLocation
      ? getEvents({ lat: prefs.lat!, lon: prefs.lon!, radiusKm: prefs.radiusKm ?? 25, from: today, to: today, size: 200 })
      : Promise.resolve(null),
  ]);

  // Only ever the ones still ahead of you - a past saved concert has nothing left to
  // remind you of, so it's dropped here rather than cluttering the one list meant to
  // answer "what's coming up for me". savedEvents is already date-ascending
  // (UserService.toMeResponse), so filtering keeps that order.
  const upcomingSaved = dedupeFestivals(session?.savedEvents.filter((e) => e.date >= today) ?? []);

  return (
    <div>
      <h1 className="font-display text-4xl leading-tight sm:text-5xl">
        {session ? `Hallo ${session.username}` : "Konzerte in deiner Nähe"}
      </h1>

      <section className="mt-8">
        {/* Mobile (1 column) reads top-to-bottom in `order`: next concerts, then followed
            bands, then the map - both text lists before the heavier map, matching how
            quickly a visitor actually wants to see "what's coming up for me" here. Desktop
            (md:grid-cols-2) keeps concerts and map side by side with followed bands below,
            spanning both columns - `order` reshuffles CSS Grid's auto-placement, so the same
            three blocks just need different `order`/`md:order` per breakpoint, no separate
            markup. */}
        <div className="grid gap-6 md:grid-cols-2">
          <div className="order-1">
            <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Deine nächsten Konzerte</h2>
            <div className="mt-2">
              {!session ? (
                <EmptyState>
                  <Link href="/login" className="text-accent hover:underline">
                    Melde dich an
                  </Link>
                  , um Konzerte zu merken und hier wiederzufinden.
                </EmptyState>
              ) : upcomingSaved.length === 0 ? (
                <EmptyState>
                  Noch keine Konzerte gemerkt.{" "}
                  <Link href="/konzerte" className="text-accent hover:underline">
                    Konzerte entdecken →
                  </Link>
                </EmptyState>
              ) : (
                <NextConcertsList events={upcomingSaved} />
              )}
            </div>
          </div>

          {session && (
            <div className="order-2 md:order-3 md:col-span-2">
              <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Gefolgte Bands</h2>
              <div className="mt-2">
                {session.followedBands.length === 0 ? (
                  <EmptyState>Noch keinen Bands gefolgt.</EmptyState>
                ) : (
                  <FollowedBandsRow bands={session.followedBands} />
                )}
              </div>
            </div>
          )}

          <div className="order-3 md:order-2">
            <div className="flex items-baseline justify-between">
              <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Heute in deiner Nähe</h2>
              <Link href="/orte" className="font-meta text-sm text-accent hover:underline">
                Karte & weitere Tage →
              </Link>
            </div>
            <div className="mt-2">
              {hasLocation && todayPage ? (
                <>
                  <ConcertMapClient
                    center={{ lat: prefs.lat!, lon: prefs.lon! }}
                    radiusKm={prefs.radiusKm ?? 25}
                    locations={buildMapLocations(todayPage.content)}
                  />
                  {todayPage.content.length === 0 && (
                    <p className="mt-2 font-meta text-sm text-muted">Heute ist nichts in deiner Nähe gelistet.</p>
                  )}
                </>
              ) : data.todayNearby.length === 0 ? (
                <EmptyState>
                  Heute ist nichts gelistet.{" "}
                  <Link href="/konzerte?range=weekend" className="text-accent hover:underline">
                    Konzerte am Wochenende ansehen →
                  </Link>
                </EmptyState>
              ) : (
                data.todayNearby.map((e) => <EventCard key={e.id} event={e} />)
              )}
            </div>
            {!hasLocation && (
              <p className="mt-2 font-meta text-xs text-muted">
                Wähle oben rechts einen Standort, um das auf einer Karte zu sehen.
              </p>
            )}
          </div>
        </div>
      </section>

      {data.recommendedForYou.length > 0 && (
        <Section title={session ? "Das könnte dich interessieren" : "Beliebt in deiner Nähe"}>
          {data.recommendedForYou.map((e) => (
            <EventCard key={e.id} event={e} />
          ))}
        </Section>
      )}

      <Section title="Dieses Wochenende">
        {data.thisWeekend.length === 0 ? (
          <EmptyState>Am Wochenende noch nichts gelistet.</EmptyState>
        ) : (
          data.thisWeekend.map((e) => <EventCard key={e.id} event={e} />)
        )}
      </Section>

      <Section title="Neue Konzerte">
        {data.newEvents.length === 0 ? (
          <EmptyState>Noch nichts Neues.</EmptyState>
        ) : (
          data.newEvents.map((e) => <EventCard key={e.id} event={e} />)
        )}
      </Section>

      <Section title="Orte mit kommenden Shows">
        {data.locationsWithUpcomingShows.length === 0 ? (
          <EmptyState>Noch keine Orte mit Konzerten.</EmptyState>
        ) : (
          data.locationsWithUpcomingShows.map((l) => <LocationTeaser key={l.id} location={l} />)
        )}
      </Section>

      <Section title="Bands, die demnächst spielen">
        {data.bandsPlayingSoon.length === 0 ? (
          <EmptyState>Noch keine Bands gelistet.</EmptyState>
        ) : (
          <div className="divide-y divide-line border-y border-line">
            {data.bandsPlayingSoon.map((band) => (
              <Link key={band.id} href={`/bands/${band.id}`} className="flex items-center justify-between gap-4 py-3 hover:text-accent">
                <div>
                  <div className="font-display text-lg">{band.name}</div>
                  {band.city && <div className="font-meta text-sm text-muted">{band.city}</div>}
                </div>
                <StatusBadge status={band.status} />
              </Link>
            ))}
          </div>
        )}
      </Section>

      <div className="mt-8">
        <Link href="/konzerte" className="font-meta text-sm text-accent hover:underline">
          Alle kommenden Konzerte ansehen →
        </Link>
      </div>
    </div>
  );
}

/** One entry per festival, not one per saved concert within it - NextConcertsList already
 * shows a festival-linked row as just the festival's name and date (see its own comment),
 * so three saved concerts from the same festival would otherwise repeat that same name
 * three times. Events are already date-ascending, so the first (i.e. soonest) one for a
 * given festival is the one kept; any later ones from it are dropped, not moved elsewhere -
 * its own Gemerkte Konzerte page is where the rest of them actually show. A non-festival
 * event is never deduped against anything. */
function dedupeFestivals(events: EventSummary[]): EventSummary[] {
  const seenFestivalIds = new Set<number>();
  return events.filter((e) => {
    if (e.eventSeries == null) return true;
    if (seenFestivalIds.has(e.eventSeries.id)) return false;
    seenFestivalIds.add(e.eventSeries.id);
    return true;
  });
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-10">
      <h2 className="font-meta text-sm uppercase tracking-wide text-muted">{title}</h2>
      <div className="mt-2">{children}</div>
    </section>
  );
}
