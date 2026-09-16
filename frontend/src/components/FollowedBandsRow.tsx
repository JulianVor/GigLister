import Link from "next/link";
import { EntityPlaceholder } from "./EntityPlaceholder";

interface FollowedBand {
  id: number;
  name: string;
  logoUrl: string | null;
}

/** A horizontally scrollable row of square band tiles - same logo-or-color-initial
 * treatment LineUp gives a band within a line-up, just sized up into its own tile with
 * the name below instead of a small avatar next to it. The only place a visitor can
 * actually see which bands they follow at a glance; before this, that list only existed
 * buried further down the homepage as plain text rows. */
export function FollowedBandsRow({ bands }: { bands: FollowedBand[] }) {
  return (
    <div className="flex gap-4 overflow-x-auto pb-2">
      {bands.map((band) => (
        <Link key={band.id} href={`/bands/${band.id}`} className="w-20 flex-none text-center hover:text-accent sm:w-24">
          {band.logoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={band.logoUrl}
              alt=""
              className="h-20 w-20 border border-line bg-surface object-contain p-2 sm:h-24 sm:w-24"
            />
          ) : (
            <EntityPlaceholder name={band.name} className="h-20 w-20 sm:h-24 sm:w-24" textClassName="text-3xl" />
          )}
          <div className="mt-1.5 truncate font-meta text-xs">{band.name}</div>
        </Link>
      ))}
    </div>
  );
}
