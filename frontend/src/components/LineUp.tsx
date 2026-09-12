import Link from "next/link";
import type { BandSummary, MeResponse } from "@/lib/types";
import { canManageEntity } from "@/lib/permissions";
import { EntityPlaceholder } from "./EntityPlaceholder";
import { StatusBadge } from "./StatusBadge";

export function LineUp({ bands, loggedIn, session }: { bands: BandSummary[]; loggedIn: boolean; session: MeResponse | null }) {
  return (
    <div className="divide-y divide-line border-y border-line">
      {bands.map((band) => {
        const content = (
          <div className="flex items-center gap-4 py-3">
            {band.logoUrl ? (
              // A logo isn't meant to be cropped - object-cover in a bordered box was
              // cutting into the artwork and boxing it in a border that fights with the
              // logo's own shape. object-contain shows it whole, no box around it.
              // eslint-disable-next-line @next/next/no-img-element
              <img src={band.logoUrl} alt="" className="h-12 w-12 flex-none object-contain" />
            ) : (
              <EntityPlaceholder name={band.name} className="h-12 w-12 flex-none" textClassName="text-xl" />
            )}
            <div>
              <div className="font-display text-base">{band.name}</div>
              {/* City is rarely useful here - this site is for local gigs, so almost every
                  band already plays in or near the city the event itself is in. Genres
                  tell a visitor something they don't already know. */}
              {band.genres.length > 0 && <div className="font-meta text-sm text-muted">{band.genres.join(", ")}</div>}
            </div>
            {/* Unvollständig/Entwurf is only meaningful to an admin or this band's own manager - a
                random visitor doesn't need to see internal workflow state. */}
            {canManageEntity(session, "BAND", band.id) && <StatusBadge status={band.status} />}
          </div>
        );

        // PUBLISHED bands are linkable for everyone; a STUB/DRAFT band has no
        // real public profile yet, but a logged-in user can still reach it -
        // otherwise nobody could ever discover and claim it.
        return band.linkable || loggedIn ? (
          <Link key={band.id} href={`/bands/${band.id}`} className="block hover:text-accent">
            {content}
          </Link>
        ) : (
          <div key={band.id}>{content}</div>
        );
      })}
    </div>
  );
}
