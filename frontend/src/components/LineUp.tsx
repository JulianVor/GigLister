import Link from "next/link";
import type { BandSummary, MeResponse } from "@/lib/types";
import { canManageEntity } from "@/lib/permissions";
import { formatTime, sortableMinutes } from "@/lib/format";
import { EntityPlaceholder } from "./EntityPlaceholder";
import { StatusBadge } from "./StatusBadge";
import { SaveActButton } from "./SaveActButton";

/** Bands with their own start time come first, chronologically - the same "timed ones
 * sorted, then the rest" split SeriesTimetable's explodeEvent already uses for a Festival's
 * running order. Bands without one keep their original (line-up/position) order and stay
 * grouped at the end, since there's nothing to sort them by. Sorted against the event's own
 * overall startTime (see sortableMinutes) so a band going on after midnight - "21:00, 22:00,
 * then 00:00" - lands last, not first just because "00:00" reads earlier as a bare time. */
function sortByStartTime(bands: BandSummary[], eventStartTime: string | null): BandSummary[] {
  const timed = bands
    .filter((b) => b.startTime)
    .sort((a, b) => sortableMinutes(a.startTime, eventStartTime) - sortableMinutes(b.startTime, eventStartTime));
  const untimed = bands.filter((b) => !b.startTime);
  return [...timed, ...untimed];
}

export function LineUp({
  bands,
  loggedIn,
  session,
  eventId,
  eventStartTime = null,
  partOfFestival = false,
}: {
  bands: BandSummary[];
  loggedIn: boolean;
  session: MeResponse | null;
  /** Needed to save/unsave an individual act - omit on contexts with no per-band saving
   * (a band's own profile page reuses LineUp-shaped markup nowhere, so this is always
   * given in practice, but stays optional rather than forcing every caller to pass a
   * meaningless id). */
  eventId?: number;
  /** The event's own overall start time - the reference sortByStartTime needs to place an
   * after-midnight band correctly. */
  eventStartTime?: string | null;
  /** Only a festival concert has individual acts worth picking out from the rest of the
   * bill - see UserService.saveAct. */
  partOfFestival?: boolean;
}) {
  const savedActBandIds = new Set(
    (eventId != null ? session?.savedActs.filter((a) => a.eventId === eventId) : [])?.map((a) => a.bandId) ?? []
  );

  return (
    <div className="divide-y divide-line border-y border-line">
      {sortByStartTime(bands, eventStartTime).map((band) => {
        const rowContent = (
          <>
            {band.logoUrl ? (
              // A logo isn't meant to be cropped - object-cover in a bordered box was
              // cutting into the artwork and boxing it in a border that fights with the
              // logo's own shape. object-contain shows it whole, no box around it.
              // eslint-disable-next-line @next/next/no-img-element
              <img src={band.logoUrl} alt="" className="h-12 w-12 flex-none object-contain" />
            ) : (
              <EntityPlaceholder name={band.name} className="h-12 w-12 flex-none" textClassName="text-xl" />
            )}
            <div className="min-w-0">
              <div className="flex items-baseline gap-2">
                <span className="font-display text-base">{band.name}</span>
                {/* Only shown when this band goes on at a different time than the event's
                    own overall startTime - the common case needs no per-band repeat. */}
                {band.startTime && (
                  <span className="font-meta text-xs tabular-nums text-muted">{formatTime(band.startTime)}</span>
                )}
              </div>
              {/* City is rarely useful here - this site is for local gigs, so almost every
                  band already plays in or near the city the event itself is in. Genres
                  tell a visitor something they don't already know. */}
              {band.genres.length > 0 && <div className="font-meta text-sm text-muted">{band.genres.join(", ")}</div>}
            </div>
            {/* Unvollständig/Entwurf is only meaningful to an admin or this band's own manager - a
                random visitor doesn't need to see internal workflow state. */}
            {canManageEntity(session, "BAND", band.id) && <StatusBadge status={band.status} />}
          </>
        );

        // PUBLISHED bands are linkable for everyone; a STUB/DRAFT band has no
        // real public profile yet, but a logged-in user can still reach it -
        // otherwise nobody could ever discover and claim it.
        const linkable = band.linkable || loggedIn;

        return (
          <div key={band.id} className="flex items-center gap-4 py-3">
            {linkable ? (
              <Link href={`/bands/${band.id}`} className="flex min-w-0 flex-1 items-center gap-4 hover:text-accent">
                {rowContent}
              </Link>
            ) : (
              <div className="flex min-w-0 flex-1 items-center gap-4">{rowContent}</div>
            )}
            {/* A save-act button here would sit INSIDE the Link above if it were part of
                rowContent - nested interactive elements, and a click would both toggle the
                save and navigate. Kept as this row's own sibling instead, not nested in
                either branch above. */}
            {loggedIn && partOfFestival && eventId != null && band.startTime && (
              <SaveActButton eventId={eventId} bandId={band.id} initiallySaved={savedActBandIds.has(band.id)} />
            )}
          </div>
        );
      })}
    </div>
  );
}
