import Link from "next/link";
import { EntityPlaceholder } from "./EntityPlaceholder";
import { BandStoryAvatarButton } from "./BandStoryAvatarButton";

interface FollowedBand {
  id: number;
  name: string;
  logoUrl: string | null;
  profileImageUrl: string | null;
  hasActiveStory: boolean;
}

const TILE_SIZE = "h-20 w-20 sm:h-24 sm:w-24";

/** A horizontally scrollable row of square band tiles - same logo-or-color-initial
 * treatment LineUp gives a band within a line-up, just sized up into its own tile with
 * the name below instead of a small avatar next to it. The only place a visitor can
 * actually see which bands they follow at a glance; before this, that list only existed
 * buried further down the homepage as plain text rows.
 *
 * The avatar and the name are two separate clickable elements (not one tile-wide Link)
 * because a band with an active status needs its avatar to open the story viewer instead
 * of navigating away - see BandStoryAvatarButton. The name below always still links to the
 * profile either way, so a story-having band is never harder to actually visit. */
export function FollowedBandsRow({ bands }: { bands: FollowedBand[] }) {
  // Bands currently posting a status are the whole point of checking this row - put them
  // first. A stable sort (Array.prototype.sort has been spec-guaranteed stable since ES2019)
  // keeps everyone else in whatever order the caller already had them in.
  const sorted = [...bands].sort((a, b) => Number(b.hasActiveStory) - Number(a.hasActiveStory));

  return (
    // p-1.5 (not just pb-2): overflow-x-auto also clips vertically (setting only one axis to a
    // non-visible overflow makes the browser compute the other as auto too - a CSS overflow
    // quirk), and StoryRing's colorful ring extends 6px past each tile on every side, so it
    // needs breathing room on top/left/right too, not just underneath.
    <div className="flex gap-4 overflow-x-auto p-1.5">
      {sorted.map((band) => (
        <div key={band.id} className="w-20 flex-none text-center sm:w-24">
          {band.hasActiveStory ? (
            <BandStoryAvatarButton
              bandId={band.id}
              bandName={band.name}
              profileImageUrl={band.profileImageUrl}
              logoUrl={band.logoUrl}
              size={TILE_SIZE}
              textClassName="text-3xl"
            />
          ) : (
            <Link href={`/bands/${band.id}`} className="block hover:text-accent">
              {band.profileImageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={band.profileImageUrl} alt="" className={`border border-line object-cover ${TILE_SIZE}`} />
              ) : band.logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={band.logoUrl}
                  alt=""
                  className={`border border-line bg-surface object-contain p-2 ${TILE_SIZE}`}
                />
              ) : (
                <EntityPlaceholder name={band.name} className={TILE_SIZE} textClassName="text-3xl" />
              )}
            </Link>
          )}
          <Link href={`/bands/${band.id}`} className="mt-1.5 block truncate font-meta text-xs hover:text-accent">
            {band.name}
          </Link>
        </div>
      ))}
    </div>
  );
}
