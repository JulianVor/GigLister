import Link from "next/link";
import type { BandImageDisplay, EventSummary } from "@/lib/types";
import { dayAndMonth, dayNumber, formatTime, monthShort, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";

/** An image-forward post-style card (photo up top, a floating date pill, details below) -
 * self-spaced (`mb-4`) so every list of these just stacks without callers adding gaps. */
export function EventCard({ event }: { event: EventSummary }) {
  const time = formatTime(event.startTime);
  const heroImage = event.titleImageUrl;
  const bandImages = event.bands
    .map((b) => (event.bandImageDisplay === "PHOTO" ? b.titleImageUrl : b.logoUrl))
    .filter((url): url is string => !!url)
    .slice(0, 4);
  const locationImage = event.location.titleImageUrl;
  const collageSourceCount = bandImages.length + (locationImage ? 1 : 0);
  // A lone image only gets the plain full-bleed treatment when it's genuinely photo-like
  // (the location, or a single band PHOTO) - a lone LOGO still goes through BandCollage
  // below, which shows it as one clean, uncropped tile instead of stretched edge to edge.
  const showSingleImagePlain = collageSourceCount === 1 && (locationImage !== null || event.bandImageDisplay === "PHOTO");

  return (
    <Link href={`/konzerte/${event.id}`} className="group mb-4 block border border-line hover:border-fg">
      <div className="relative h-44 w-full overflow-hidden border-b border-line bg-surface sm:h-60">
        {heroImage ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={heroImage}
            alt=""
            className="h-full w-full object-cover transition-transform duration-300 ease-out group-hover:scale-[1.04]"
          />
        ) : showSingleImagePlain ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={locationImage ?? bandImages[0]}
            alt=""
            className="h-full w-full object-cover transition-transform duration-300 ease-out group-hover:scale-[1.04]"
          />
        ) : collageSourceCount >= 1 ? (
          <BandCollage bandImages={bandImages} locationImage={locationImage} mode={event.bandImageDisplay} />
        ) : (
          <div className="flex h-full w-full flex-col items-center justify-center gap-1">
            <div className="font-display text-6xl leading-none sm:text-7xl">{dayNumber(event.date)}</div>
            <div className="font-meta text-sm uppercase tracking-wide text-muted">{monthShort(event.date)}</div>
          </div>
        )}
        <div className="absolute left-3 top-3 border border-line bg-bg/90 px-2 py-1 font-meta text-xs uppercase tracking-wide">
          {weekdayShort(event.date)} {dayAndMonth(event.date)}
          {time ? ` · ${time}` : ""}
        </div>
      </div>

      <div className="p-4">
        <div className="truncate font-display text-xl group-hover:text-accent sm:text-2xl">
          {eventLineupLabel(event)}
        </div>
        <div className="mt-1 font-meta text-sm text-muted">
          {event.location.name} · {event.location.city}
        </div>
      </div>
    </Link>
  );
}

/** Each tile's fixed height, tuned so a row (or a stacked pair, for 4 bands) fits inside
 * the card's own h-44/sm:h-60 with room to spare - shrinks as more bands need to fit.
 * Percentage heights don't work here since the tiles sit in nested flex rows/columns
 * with no definite height of their own to be a percentage of. Same heights for both
 * PHOTO and LOGO tiles, so a lineup keeps the same overall footprint either way. */
const TILE_HEIGHT: Record<1 | 2 | 3 | 4, string> = {
  1: "h-36 sm:h-52",
  2: "h-32 sm:h-44",
  3: "h-28 sm:h-36",
  4: "h-20 sm:h-28",
};

/** The oval shape and its soft edge both come from the same radial mask - fading
 * everything outside the ellipse to transparent, rather than a hard `border-radius`
 * clip, so each photo blends into the location image behind it instead of sitting on
 * top of it as a sharply-cut sticker. The ellipse's radii are given explicitly (50% 50%,
 * i.e. exactly the image's own half-width/half-height) rather than left to the default
 * "farthest-corner" sizing - on a non-square box, farthest-corner reaches full
 * transparency at the CORNERS, which is short of full transparency yet at the flat top/
 * bottom/left/right edges, leaving a faint but visible hard line right at the image's
 * actual boundary. Explicit 50% 50% radii fade out exactly at every edge, corners
 * included, so there's no boundary left for a hard edge to show up on.
 *
 * Pushing the solid ("black") stop out close to the edge keeps the fade itself small
 * and, as a side effect, reads as far less oval: the solid area now fills almost the
 * whole box along the flat sides (top/bottom/left/right), so only the corners visibly
 * round off instead of the whole tile reading as a blob. */
function BandPhotoOval({ url, heightClass }: { url: string; heightClass: string }) {
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt=""
      className={`object-cover ${heightClass}`}
      style={{
        aspectRatio: "3 / 2",
        maskImage: "radial-gradient(ellipse 50% 50% at center, black 0%, black 78%, transparent 98%)",
        WebkitMaskImage: "radial-gradient(ellipse 50% 50% at center, black 0%, black 78%, transparent 98%)",
      }}
    />
  );
}

/** Unlike a band photo, a logo isn't meant to be cropped or faded at all - clipping any
 * part of it (round or otherwise) can cut off letters or the mark itself, and a soft
 * edge just looks like a rendering glitch on a flat graphic. Plain square tile, full
 * logo shown via `object-contain` (never cropped), with its own opaque background since
 * most logos are transparent PNGs that would otherwise let the dimmed location image
 * show through their negative space. */
function BandLogoTile({ url, heightClass }: { url: string; heightClass: string }) {
  return (
    <div className={`flex items-center justify-center border border-line bg-bg p-1.5 ${heightClass}`} style={{ aspectRatio: "1 / 1" }}>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={url} alt="" className="h-full w-full object-contain" />
    </div>
  );
}

/** Band images as a clean grid of tiles over the (dimmed) location image - 1-3 bands sit
 * in a single centered row, 4 split into two columns of a stacked pair each, so it never
 * collapses into one crowded row. The location fills whatever the tiles don't cover, so
 * there's never an empty gap; without a location image, PHOTO mode blurs the first band
 * photo into a backdrop instead so the same guarantee holds with band photos alone -
 * LOGO mode uses a plain surface fill instead, since blurring a small flat graphic into
 * a full-bleed backdrop looks like a rendering error, not a design choice. */
function BandCollage({
  bandImages,
  locationImage,
  mode,
}: {
  bandImages: string[];
  locationImage: string | null;
  mode: BandImageDisplay;
}) {
  // For 4 bands, each column stacks a pair; otherwise every band is its own single-item
  // column, which lines them all up side by side in one row instead of stacking them.
  const columns = bandImages.length === 4 ? [bandImages.slice(0, 2), bandImages.slice(2, 4)] : bandImages.map((url) => [url]);
  const heightClass = TILE_HEIGHT[bandImages.length as 1 | 2 | 3 | 4] ?? TILE_HEIGHT[4];
  const Tile = mode === "PHOTO" ? BandPhotoOval : BandLogoTile;

  return (
    <div className="relative h-full w-full overflow-hidden">
      {locationImage ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={locationImage}
          alt=""
          className="absolute inset-0 h-full w-full object-cover brightness-75 saturate-75"
        />
      ) : mode === "PHOTO" ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={bandImages[0]}
          alt=""
          className="absolute inset-0 h-full w-full scale-110 object-cover opacity-70 blur-md"
        />
      ) : (
        <div className="absolute inset-0 bg-surface" />
      )}
      <div className="absolute inset-0 flex items-center justify-center gap-3 sm:gap-4">
        {columns.map((col, ci) => (
          <div key={ci} className="flex flex-col items-center gap-1.5 sm:gap-2">
            {col.map((url, i) => (
              <Tile key={url + i} url={url} heightClass={heightClass} />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
