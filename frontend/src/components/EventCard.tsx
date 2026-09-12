import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { dayAndMonth, formatTime, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";
import { entityColor } from "@/lib/entityColor";

interface BandPhoto {
  name: string;
  url: string;
}

/** The event/band/location photo treatment shared by the card and the event detail page's
 * own banner - null when there's no photo anywhere to show, so each caller decides what
 * (if anything) replaces it: the card falls back to ColorCollage, the detail page just
 * shows no banner at all rather than repeating the Line-up's colors a second time.
 * `showLabels` (default on, for the card) draws each band's name over its photo - the
 * detail page turns it off since the Line-up right below already lists every name. */
export function eventPhotoContent(event: EventSummary, { showLabels = true }: { showLabels?: boolean } = {}): React.ReactNode {
  const heroImage = event.titleImageUrl;
  const locationImage = event.location.titleImageUrl;
  const bandPhotos: BandPhoto[] = event.bands
    .map((b) => ({ name: b.name, url: event.bandImageDisplay === "PHOTO" ? b.titleImageUrl : b.logoUrl }))
    .filter((entry): entry is BandPhoto => !!entry.url)
    .slice(0, 4);

  if (heroImage) {
    return <PlainCover src={heroImage} />;
  }
  if (bandPhotos.length === 0) {
    return locationImage ? <PlainCover src={locationImage} /> : null;
  }
  if (event.bandImageDisplay === "PHOTO") {
    return bandPhotos.length === 1 ? (
      <SingleBandCollage band={bandPhotos[0]} locationImage={locationImage} showLabel={showLabels} />
    ) : (
      <DiagonalPhotoCollage bands={bandPhotos} showLabels={showLabels} />
    );
  }
  return <LogoCollage logos={bandPhotos.map((b) => b.url)} locationImage={locationImage} />;
}

/** An image-forward post-style card (photo up top, a floating date pill, details below) -
 * self-spaced (`mb-4`) so every list of these just stacks without callers adding gaps. */
export function EventCard({ event }: { event: EventSummary }) {
  const time = formatTime(event.startTime);
  const content = eventPhotoContent(event) ?? (
    <ColorCollage
      locationName={event.location.name}
      bands={event.bands.slice(0, 4).map((b) => ({ name: b.name, genres: b.genres }))}
    />
  );

  return (
    <Link href={`/konzerte/${event.id}`} className="group mb-4 block border border-line hover:border-fg">
      <div className="relative h-44 w-full overflow-hidden border-b border-line bg-surface sm:h-60">
        {content}
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

function PlainCover({ src }: { src: string }) {
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={src}
      alt=""
      className="h-full w-full object-cover transition-transform duration-300 ease-out group-hover:scale-[1.04]"
    />
  );
}

/** How far a cut's top point shifts right of its bottom point, as a fraction of the
 * card's width - the diagonal "/" look shared by every seam in the collage. */
const CUT_SKEW = 0.04;

/** A shared photo-collage boundary (e.g. the line between two bands) has to land at the
 * exact same x for every segment that touches it, at every y along it - otherwise
 * neighboring segments leave a gap or overlap where they meet. Computing it as one
 * function of (boundary, y) shared by all callers, instead of each segment picking its
 * own offset, is what guarantees that: an outer edge (0 or 1) never moves, an internal
 * one always slides by the same CUT_SKEW-scaled amount at a given height regardless of
 * which segment is asking. */
function cutX(boundary: number, y: number): number {
  if (boundary <= 0 || boundary >= 1) return boundary;
  return boundary + CUT_SKEW * (1 - y);
}

interface Segment extends BandPhoto {
  left: number;
  right: number;
  top: number;
  bottom: number;
}

interface SegmentBox {
  left: number;
  right: number;
  top: number;
  bottom: number;
}

/** cutX only ever shifts a boundary to the right (by up to CUT_SKEW, at the top edge) -
 * never left - so a segment's photo only needs a wider box than its own nominal strip
 * on the right, and only where that edge is an internal cut (an outer 0/1 edge never
 * moves). Sizing each segment's box to (close to) its own true footprint instead of the
 * full card width is what lets object-cover show most of the photo instead of cropping
 * away everything outside its narrow visible sliver. */
function segmentBox(seg: Segment): SegmentBox {
  const right = seg.right < 1 ? Math.min(1, seg.right + CUT_SKEW) : seg.right;
  return { left: seg.left, right, top: seg.top, bottom: seg.bottom };
}

/** Clip points in the segment's own box-local 0-1 space (not the card's), so the box can
 * be sized to just that segment's footprint while the diagonal cut still lands at the
 * exact same card-global x as segmentBox's neighbor computes for the shared boundary. */
function segmentClipPath(seg: Segment, box: SegmentBox): string {
  const w = box.right - box.left;
  const h = box.bottom - box.top;
  const toLocal = (gx: number, gy: number): [number, number] => [(gx - box.left) / w, (gy - box.top) / h];
  const points: [number, number][] = [
    toLocal(cutX(seg.left, seg.top), seg.top),
    toLocal(cutX(seg.right, seg.top), seg.top),
    toLocal(cutX(seg.right, seg.bottom), seg.bottom),
    toLocal(cutX(seg.left, seg.bottom), seg.bottom),
  ];
  return `polygon(${points.map(([x, y]) => `${x * 100}% ${y * 100}%`).join(", ")})`;
}

/** Every band count splits the card into equal-width, full-height diagonal strips side
 * by side - 2, 3, or 4, all the same layout, just narrower per strip as more bands join. */
function layoutSegments(bands: BandPhoto[]): Segment[] {
  const n = bands.length;
  return bands.map((band, i) => ({ ...band, left: i / n, right: (i + 1) / n, top: 0, bottom: 1 }));
}

/** Half-thickness of the white seam line drawn over each internal boundary, as a fraction
 * of the card's width - kept small so it reads as a thin divider, not a border. */
const SEAM_HALF_WIDTH = 0.0015;

/** The diagonal seam between two segments is otherwise just wherever their clip-paths
 * happen to meet - no pixels of its own. This draws a thin white sliver centered exactly
 * on that shared line (same cutX as the segments themselves, so it tracks it perfectly at
 * every height) as its own layer on top, in a box just wide enough to hold it. */
function SeamLine({ boundary, top, bottom }: { boundary: number; top: number; bottom: number }) {
  const boxLeft = Math.max(0, boundary - SEAM_HALF_WIDTH);
  const boxRight = Math.min(1, boundary + CUT_SKEW + SEAM_HALF_WIDTH);
  const w = boxRight - boxLeft;
  const h = bottom - top;
  const toLocal = (gx: number, gy: number): [number, number] => [(gx - boxLeft) / w, (gy - top) / h];
  const points: [number, number][] = [
    toLocal(cutX(boundary, top) - SEAM_HALF_WIDTH, top),
    toLocal(cutX(boundary, top) + SEAM_HALF_WIDTH, top),
    toLocal(cutX(boundary, bottom) + SEAM_HALF_WIDTH, bottom),
    toLocal(cutX(boundary, bottom) - SEAM_HALF_WIDTH, bottom),
  ];
  return (
    <div
      className="pointer-events-none absolute bg-white"
      style={{
        left: `${boxLeft * 100}%`,
        width: `${w * 100}%`,
        top: `${top * 100}%`,
        height: `${h * 100}%`,
        clipPath: `polygon(${points.map(([x, y]) => `${x * 100}% ${y * 100}%`).join(", ")})`,
      }}
    />
  );
}

/** Full-bleed diagonal-cut lineup, each photo cropped to its own strip and (by default)
 * labeled with the band's name - no location image shown once there's more than one band,
 * since the strips already fill the whole card between them. `showLabels` is off on the
 * event detail page, where the Line-up right below already lists every name. */
function DiagonalPhotoCollage({ bands, showLabels = true }: { bands: BandPhoto[]; showLabels?: boolean }) {
  const segments = layoutSegments(bands);

  return (
    <div className="relative h-full w-full overflow-hidden bg-surface">
      {segments.map((seg, i) => {
        const box = segmentBox(seg);
        return (
          <div
            key={i}
            className="absolute"
            style={{
              left: `${box.left * 100}%`,
              width: `${(box.right - box.left) * 100}%`,
              top: `${box.top * 100}%`,
              height: `${(box.bottom - box.top) * 100}%`,
              clipPath: segmentClipPath(seg, box),
            }}
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={seg.url} alt="" className="h-full w-full object-cover" />
          </div>
        );
      })}
      {segments.slice(0, -1).map((seg, i) => (
        <SeamLine key={`seam-${i}`} boundary={seg.right} top={seg.top} bottom={seg.bottom} />
      ))}
      {showLabels &&
        segments.map((seg, i) => (
          <div
            key={`label-${i}`}
            className="pointer-events-none absolute flex items-end justify-start overflow-hidden pb-2 pl-2 sm:pb-3 sm:pl-3"
            style={{
              left: `${seg.left * 100}%`,
              width: `${(seg.right - seg.left) * 100}%`,
              top: `${seg.top * 100}%`,
              height: `${(seg.bottom - seg.top) * 100}%`,
            }}
          >
            <span className="truncate font-display text-sm font-bold uppercase tracking-wide text-white [text-shadow:0_2px_6px_rgba(0,0,0,0.85)] sm:text-lg">
              {seg.name}
            </span>
          </div>
        ))}
    </div>
  );
}

interface BandColorInfo {
  name: string;
  genres: string[];
}

/** Full-bleed grid of band tiles - each one fills its share of the card (a row for 1-3
 * bands, a 2x2 grid for 4), with the location's own color showing through only as the
 * thin gap between them, and each tile's own gradient fading into that same location
 * color at its edge so the whole thing reads as one connected surface rather than
 * separate chips glued on top. Tiles get the full card to grow into (unlike a fixed-size
 * chip), so even a long band name has room to wrap onto two lines instead of truncating. */
function ColorCollage({ locationName, bands }: { locationName: string; bands: BandColorInfo[] }) {
  const locationColor = entityColor(locationName);
  const columns = bands.length === 4 ? [bands.slice(0, 2), bands.slice(2, 4)] : bands.map((band) => [band]);

  return (
    <div className="flex h-full w-full gap-1 p-1 sm:gap-1.5 sm:p-1.5" style={{ backgroundColor: locationColor }}>
      {columns.map((col, ci) => (
        <div key={ci} className="flex flex-1 flex-col gap-1 sm:gap-1.5">
          {col.map((band, i) => (
            <div
              key={band.name + i}
              className="flex flex-1 flex-col items-center justify-center gap-1 px-2 text-center"
              style={{ background: `linear-gradient(160deg, ${entityColor(band.name)}, ${locationColor})` }}
            >
              <span className="max-w-full break-words font-display text-base font-bold uppercase leading-tight tracking-wide text-white sm:text-xl">
                {band.name}
              </span>
              {band.genres[0] && (
                <span className="max-w-full truncate font-meta text-xs uppercase tracking-wide text-white/75 sm:text-sm">
                  {band.genres[0]}
                </span>
              )}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}

/** Where the band photo is fully opaque, and where it's fully faded away, as a fraction
 * of the CARD width (not the photo's own box, which SingleBandCollage sizes to just
 * BAND_FADE_END and re-expresses these same two points in its own local space). */
const BAND_FADE_START = 0.58;
const BAND_FADE_END = 0.78;

/** The one-band case keeps the location visible - the band photo covers the left ~60%
 * and fades via a plain transparent gradient (no diagonal cut, this is the only place
 * with a soft edge) into the location photo on the right. Without a location image the
 * band photo just fills the whole card, same as it always would.
 *
 * The photo's own box only extends to BAND_FADE_END, not the full card width - past that
 * point it's fully transparent anyway, so giving it more box width would only make
 * object-cover crop the photo harder to fill space nothing shows. The mask percentages
 * are re-expressed relative to that narrower box (BAND_FADE_START/BAND_FADE_END instead
 * of BAND_FADE_START/BAND_FADE_END of the card) so the visible fade still lands at the
 * same spot on the card. */
function SingleBandCollage({
  band,
  locationImage,
  showLabel = true,
}: {
  band: BandPhoto;
  locationImage: string | null;
  showLabel?: boolean;
}) {
  const boxWidth = locationImage ? BAND_FADE_END : 1;
  const localFadeStart = (BAND_FADE_START / BAND_FADE_END) * 100;

  return (
    <div className="relative h-full w-full overflow-hidden bg-surface">
      {locationImage && (
        // Left of BAND_FADE_START the band photo is fully opaque, so the location photo
        // is never visible there - rendering it at the full card width anyway just makes
        // object-cover crop it harder to fill space nothing shows, same problem the band
        // photo itself had. Sizing its box to just the region it can ever appear in fixes
        // it the same way.
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={locationImage}
          alt=""
          className="absolute inset-y-0 right-0 h-full object-cover"
          style={{ width: `${(1 - BAND_FADE_START) * 100}%` }}
        />
      )}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={band.url}
        alt=""
        className="absolute inset-y-0 left-0 h-full object-cover"
        style={{
          width: `${boxWidth * 100}%`,
          ...(locationImage
            ? {
                maskImage: `linear-gradient(to right, black 0%, black ${localFadeStart}%, transparent 100%)`,
                WebkitMaskImage: `linear-gradient(to right, black 0%, black ${localFadeStart}%, transparent 100%)`,
              }
            : {}),
        }}
      />
      {showLabel && (
        <div
          className="pointer-events-none absolute bottom-0 left-0 flex items-end pb-2 pl-2 sm:pb-3 sm:pl-3"
          style={{ width: locationImage ? "55%" : "100%" }}
        >
          <span className="truncate font-display text-base font-bold uppercase tracking-wide text-white [text-shadow:0_2px_6px_rgba(0,0,0,0.85)] sm:text-xl">
            {band.name}
          </span>
        </div>
      )}
    </div>
  );
}

/** Each logo's fixed height, tuned so a row (or a stacked pair, for 4 bands) fits inside
 * the card's own h-44/sm:h-60 with room to spare - shrinks as more bands need to fit.
 * Percentage heights don't work here since the tiles sit in nested flex rows/columns
 * with no definite height of their own to be a percentage of. */
const LOGO_TILE_HEIGHT: Record<1 | 2 | 3 | 4, string> = {
  1: "h-40 sm:h-56",
  2: "h-36 sm:h-48",
  3: "h-28 sm:h-40",
  4: "h-20 sm:h-28",
};

/** A logo isn't meant to be cropped or faded at all - clipping any part of it (round,
 * diagonal, or otherwise) can cut off letters or the mark itself, and a soft edge just
 * looks like a rendering glitch on a flat graphic. So unlike the photo collages above,
 * logos always render as plain, uncropped shapes via `object-contain` - no box, no
 * background, no border: wherever the logo file itself has transparency, the location
 * image behind it (or the surface fill, without one) shows straight through, so it
 * reads as a logo floating over the photo rather than a solid card sitting on it. */
function LogoTile({ url, heightClass }: { url: string; heightClass: string }) {
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={url} alt="" className={`object-contain ${heightClass}`} style={{ aspectRatio: "1 / 1" }} />
  );
}

/** Logos as a clean grid of tiles over the (dimmed) location image - 1-3 bands sit in a
 * single centered row, 4 split into two columns of a stacked pair each, so it never
 * collapses into one crowded row. The location fills whatever the tiles don't cover, so
 * there's never an empty gap; without a location image it's a plain surface fill
 * instead, since blurring a small flat graphic into a full-bleed backdrop (as the photo
 * collages do with a band photo) looks like a rendering error, not a design choice. */
function LogoCollage({ logos, locationImage }: { logos: string[]; locationImage: string | null }) {
  // For 4 bands, each column stacks a pair; otherwise every logo is its own single-item
  // column, which lines them all up side by side in one row instead of stacking them.
  const columns = logos.length === 4 ? [logos.slice(0, 2), logos.slice(2, 4)] : logos.map((url) => [url]);
  const heightClass = LOGO_TILE_HEIGHT[logos.length as 1 | 2 | 3 | 4] ?? LOGO_TILE_HEIGHT[4];

  return (
    <div className="relative h-full w-full overflow-hidden">
      {locationImage ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={locationImage}
          alt=""
          className="absolute inset-0 h-full w-full object-cover brightness-75 saturate-75"
        />
      ) : (
        <div className="absolute inset-0 bg-surface" />
      )}
      <div className="absolute inset-0 flex items-center justify-center gap-3 sm:gap-4">
        {columns.map((col, ci) => (
          <div key={ci} className="flex flex-col items-center gap-1.5 sm:gap-2">
            {col.map((url, i) => (
              <LogoTile key={url + i} url={url} heightClass={heightClass} />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
