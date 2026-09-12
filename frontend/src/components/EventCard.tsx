import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { dayAndMonth, formatTime, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";
import { entityColor } from "@/lib/entityColor";

interface BandPhoto {
  name: string;
  url: string | null;
  genres: string[];
  /** For a band with no photo (`url` null, which in LOGO mode is every band - see
   * eventPhotoContent) - its logo (if it has one) is shown as large as possible over
   * whatever fills the rest of its segment (a color wash or dimmed location photo)
   * instead of leaving that segment bare. */
  logoUrl?: string | null;
}

/** The event/band/location photo treatment shared by the card and the event detail page's
 * own banner - null when there's no photo anywhere to show, so each caller decides what
 * (if anything) replaces it: the card falls back to ColorCollage, the detail page just
 * shows no banner at all rather than repeating the Line-up's colors a second time.
 * `showLabels` (default on, for the card) draws each band's name over its photo - the
 * detail page turns it off since the Line-up right below already lists every name.
 * `requireBandPhoto` (off by default, on for the detail page) skips the "no band has a
 * photo, but the location does" fallback entirely (returning null instead) - the card
 * wants that colored-over-location treatment so it's never just a blank location photo
 * with no indication of who's playing, but on the detail page a location photo alone
 * isn't a real event photo and the Line-up right below already shows each band's color,
 * so showing it there too would just be a location photo wearing a costume. */
export function eventPhotoContent(
  event: EventSummary,
  { showLabels = true, requireBandPhoto = false }: { showLabels?: boolean; requireBandPhoto?: boolean } = {}
): React.ReactNode {
  const heroImage = event.titleImageUrl;
  const locationImage = event.location.titleImageUrl;

  if (heroImage) {
    return <PlainCover src={heroImage} />;
  }

  const isPhotoMode = event.bandImageDisplay === "PHOTO";
  // Every one of (up to 4) bands gets a slot regardless of whether it has an image - a
  // band without one still shows its own color there (see
  // DiagonalPhotoCollage/SingleBandCollage's handling of a null url) instead of just
  // dropping out of the lineup and leaving the others to stretch into the space it would
  // have had. LOGO mode never uses a band's photo, not even as a fallback - every band's
  // `url` is null there, so every one of them always takes that same colored-segment path,
  // just with its logo (if it has one) layered over the color/location fill instead of a
  // photo ever appearing. Sorted so every band with an image comes before every one
  // without - a stable sort, so within each of those two groups the original line-up
  // order is kept. Grouping them is what lets a partial location photo (see
  // DiagonalPhotoCollage) sit behind exactly the photo-less bands as one contiguous block
  // instead of needing to peek out from between unrelated photos wherever a gap happens
  // to fall in the original order.
  const allBands: BandPhoto[] = event.bands
    .slice(0, 4)
    .map((b) => ({
      name: b.name,
      url: isPhotoMode ? b.titleImageUrl : null,
      genres: b.genres,
      logoUrl: b.logoUrl,
    }))
    .sort((a, b) => (a.url ? 0 : 1) - (b.url ? 0 : 1));
  const anyImage = allBands.some((b) => !!b.url);

  if (!anyImage) {
    if (!locationImage || requireBandPhoto) return null;
    // No band has an image of its own, but the location does - rather than just showing
    // that alone with no indication of who's playing, lay each band's own color over a
    // dimmed copy of it in the exact same layout a real photo lineup would use (the fade
    // for one band, the diagonal strips for more), so this degrades the same way instead
    // of looking like a completely different, band-less card.
    return allBands.length === 1 ? (
      <SingleBandCollage band={allBands[0]} locationImage={locationImage} showLabel={showLabels} colorMode />
    ) : (
      <DiagonalPhotoCollage bands={allBands} showLabels={showLabels} colorMode locationImage={locationImage} />
    );
  }

  // Only reachable in PHOTO mode - LOGO mode's bands all have a null url above, so they
  // always take the colored-fallback branch instead.
  return allBands.length === 1 ? (
    <SingleBandCollage band={allBands[0]} locationImage={locationImage} showLabel={showLabels} />
  ) : (
    <DiagonalPhotoCollage bands={allBands} showLabels={showLabels} locationImage={locationImage} />
  );
}

/** An image-forward post-style card (photo up top, a floating date pill, details below) -
 * self-spaced (`mb-4`) so every list of these just stacks without callers adding gaps. */
export function EventCard({ event }: { event: EventSummary }) {
  const time = formatTime(event.startTime);
  const content = eventPhotoContent(event) ?? (
    <ColorCollage
      locationName={event.location.name}
      bands={event.bands.slice(0, 4).map((b) => ({ name: b.name, genres: b.genres, logoUrl: b.logoUrl }))}
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

interface Segment {
  left: number;
  right: number;
  top: number;
  bottom: number;
}

type SegmentBox = Segment;

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
 * by side - 2, 3, or 4, all the same layout, just narrower per strip as more bands join.
 * Generic so both the photo collage and the fully-colored one (ColorCollage) share this
 * layout and the seam-drawing code built on it. */
function layoutSegments<T>(items: T[]): (T & Segment)[] {
  const n = items.length;
  return items.map((item, i) => ({ ...item, left: i / n, right: (i + 1) / n, top: 0, bottom: 1 }));
}

/** Half-thickness of the white seam line drawn over each internal boundary, as a fraction
 * of the card's width - kept small so it reads as a thin divider, not a border. */
const SEAM_HALF_WIDTH = 0.0015;

/** The diagonal seam between two segments is otherwise just wherever their clip-paths
 * happen to meet - no pixels of its own. This draws a thin white sliver centered exactly
 * on that shared line (same cutX as the segments themselves, so it tracks it perfectly at
 * every height) as its own layer on top, in a box just wide enough to hold it. */
function SeamLine({ boundary, top, bottom, color = "#fff" }: { boundary: number; top: number; bottom: number; color?: string }) {
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
      className="pointer-events-none absolute"
      style={{
        left: `${boxLeft * 100}%`,
        width: `${w * 100}%`,
        top: `${top * 100}%`,
        height: `${h * 100}%`,
        backgroundColor: color,
        clipPath: `polygon(${points.map(([x, y]) => `${x * 100}% ${y * 100}%`).join(", ")})`,
      }}
    />
  );
}

/** Full-bleed diagonal-cut lineup, each photo cropped to its own strip and (by default)
 * labeled with the band's name - no location image shown once there's more than one band,
 * since the strips already fill the whole card between them. `showLabels` is off on the
 * event detail page, where the Line-up right below already lists every name.
 *
 * `colorMode` is for when none of the bands have a photo but the location does: each
 * strip becomes a translucent wash of the band's own color (see entityColor) instead of
 * an image, over a single dimmed copy of the location photo filling the whole card behind
 * them all - same geometry either way, just what fills each strip.
 *
 * Without colorMode, `bands` can still be a mix (some with a photo, some without, per
 * `eventPhotoContent`'s sort grouping every photo-less one at the end): that contiguous
 * group gets the same dimmed-location-behind-a-translucent-wash treatment as colorMode,
 * just sized to only that group's own share of the card - not the ones that already have
 * a real photo, which stay exactly as if colorMode/no-location never applied to them. */
function DiagonalPhotoCollage({
  bands,
  showLabels = true,
  colorMode = false,
  locationImage,
}: {
  bands: BandPhoto[];
  showLabels?: boolean;
  colorMode?: boolean;
  locationImage?: string | null;
}) {
  const segments = layoutSegments(bands);
  const firstPhotolessIndex = colorMode ? -1 : segments.findIndex((s) => !s.url);
  const partialLocation = !colorMode && firstPhotolessIndex !== -1 && !!locationImage;

  return (
    <div className="relative h-full w-full overflow-hidden bg-surface">
      {colorMode && locationImage && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={locationImage} alt="" className="absolute inset-0 h-full w-full object-cover brightness-50 saturate-75" />
      )}
      {partialLocation && (
        // Sized to just the photo-less group's own share of the card (from its left edge,
        // itself a plain segment boundary that - like an outer edge - never needs the
        // rightward CUT_SKEW widening segmentBox gives an internal right edge, to the
        // card's own right edge) - the same "don't stretch a photo across space it's
        // never actually visible in" fix segmentBox/SingleBandCollage already apply.
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={locationImage}
          alt=""
          className="absolute inset-y-0 right-0 h-full object-cover brightness-50 saturate-75"
          style={{ width: `${(1 - firstPhotolessIndex / segments.length) * 100}%` }}
        />
      )}
      {segments.map((seg, i) => {
        const box = segmentBox(seg);
        const translucent = colorMode || partialLocation;
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
            {seg.url ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={seg.url} alt="" className="h-full w-full object-cover" />
            ) : (
              <div
                className="h-full w-full"
                style={{ backgroundColor: entityColor(seg.name), opacity: translucent ? 0.7 : 1 }}
              />
            )}
          </div>
        );
      })}
      {segments.slice(0, -1).map((seg, i) => (
        <SeamLine key={`seam-${i}`} boundary={seg.right} top={seg.top} bottom={seg.bottom} />
      ))}
      {segments.map(
        (seg, i) =>
          !seg.url &&
          seg.logoUrl && (
            <div
              key={`logo-${i}`}
              className="absolute"
              style={{
                left: `${seg.left * 100}%`,
                width: `${(seg.right - seg.left) * 100}%`,
                top: `${seg.top * 100}%`,
                height: `${(seg.bottom - seg.top) * 100}%`,
              }}
            >
              <BandLogoOverlay logoUrl={seg.logoUrl} />
            </div>
          )
      )}
      {showLabels &&
        segments.map((seg, i) => (
          <div
            key={`label-${i}`}
            className="pointer-events-none absolute flex flex-col justify-end overflow-hidden pb-2 pl-2 sm:pb-3 sm:pl-3"
            style={{
              left: `${seg.left * 100}%`,
              width: `${(seg.right - seg.left) * 100}%`,
              top: `${seg.top * 100}%`,
              height: `${(seg.bottom - seg.top) * 100}%`,
            }}
          >
            <span className="break-words font-display text-sm font-bold uppercase leading-tight tracking-wide text-white [text-shadow:0_2px_6px_rgba(0,0,0,0.85)] sm:text-lg">
              {seg.name}
            </span>
            {seg.genres[0] && (
              <span className="truncate font-meta text-xs uppercase tracking-wide text-white/80 [text-shadow:0_1px_4px_rgba(0,0,0,0.85)]">
                {seg.genres[0]}
              </span>
            )}
          </div>
        ))}
    </div>
  );
}

/** A band with no photo but a logo gets it shown here, as large as the segment/box
 * comfortably allows, instead of leaving just the flat color/gradient behind it bare.
 * Positioned with generous insets so it never runs into a diagonal cut or a label. */
function BandLogoOverlay({ logoUrl }: { logoUrl?: string | null }) {
  if (!logoUrl) return null;
  return (
    // The padding lives on this wrapper, not the <img> itself - an absolutely positioned
    // <img> with 'auto' width/height sizes itself to its own intrinsic pixel size even
    // when inset on all four sides, ignoring the box that would imply; giving the <img>
    // an explicit h-full/w-full here is what actually makes it fill (and object-contain
    // scale within) the padded box instead of rendering at its native size.
    <div className="pointer-events-none absolute inset-0 p-4 sm:p-6">
      {/* A logo isn't meant to be cropped - object-contain shows it whole, floating over
          whatever fills the segment instead of replacing it, same treatment LineUp
          already gives a logo elsewhere. */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={logoUrl} alt="" className="h-full w-full object-contain" />
    </div>
  );
}

interface BandColorInfo {
  name: string;
  genres: string[];
  logoUrl: string | null;
}

/** Same diagonal-strip geometry as DiagonalPhotoCollage, for when neither the location nor
 * any band has a photo at all: each strip is a gradient from its own band's color (see
 * entityColor) into the location's, and the seams between them - plus a frame around the
 * whole card - use the location's color instead of white/border-line, so the location
 * still visibly ties the card together even without a photo of its own. */
function ColorCollage({ locationName, bands }: { locationName: string; bands: BandColorInfo[] }) {
  const locationColor = entityColor(locationName);
  const segments = layoutSegments(bands);

  return (
    <div className="relative h-full w-full overflow-hidden" style={{ boxShadow: `inset 0 0 0 3px ${locationColor}` }}>
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
              background: `linear-gradient(160deg, ${entityColor(seg.name)}, ${locationColor})`,
            }}
          />
        );
      })}
      {segments.slice(0, -1).map((seg, i) => (
        <SeamLine key={`seam-${i}`} boundary={seg.right} top={seg.top} bottom={seg.bottom} color={locationColor} />
      ))}
      {segments.map(
        (seg, i) =>
          seg.logoUrl && (
            <div
              key={`logo-${i}`}
              className="absolute"
              style={{
                left: `${seg.left * 100}%`,
                width: `${(seg.right - seg.left) * 100}%`,
                top: `${seg.top * 100}%`,
                height: `${(seg.bottom - seg.top) * 100}%`,
              }}
            >
              <BandLogoOverlay logoUrl={seg.logoUrl} />
            </div>
          )
      )}
      {segments.map((seg, i) => (
        <div
          key={`label-${i}`}
          className="pointer-events-none absolute flex flex-col justify-end overflow-hidden pb-2 pl-2 sm:pb-3 sm:pl-3"
          style={{
            left: `${seg.left * 100}%`,
            width: `${(seg.right - seg.left) * 100}%`,
            top: `${seg.top * 100}%`,
            height: `${(seg.bottom - seg.top) * 100}%`,
          }}
        >
          <span className="break-words font-display text-sm font-bold uppercase leading-tight tracking-wide text-white [text-shadow:0_2px_6px_rgba(0,0,0,0.85)] sm:text-lg">
            {seg.name}
          </span>
          {seg.genres[0] && (
            <span className="truncate font-meta text-xs uppercase tracking-wide text-white/80 [text-shadow:0_1px_4px_rgba(0,0,0,0.85)]">
              {seg.genres[0]}
            </span>
          )}
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
  colorMode = false,
}: {
  band: BandPhoto;
  locationImage: string | null;
  showLabel?: boolean;
  colorMode?: boolean;
}) {
  const boxWidth = locationImage ? BAND_FADE_END : 1;
  const localFadeStart = (BAND_FADE_START / BAND_FADE_END) * 100;
  const fadeMask = locationImage
    ? {
        maskImage: `linear-gradient(to right, black 0%, black ${localFadeStart}%, transparent 100%)`,
        WebkitMaskImage: `linear-gradient(to right, black 0%, black ${localFadeStart}%, transparent 100%)`,
      }
    : {};

  return (
    <div className="relative h-full w-full overflow-hidden bg-surface">
      {locationImage && (
        // Left of BAND_FADE_START the band photo is fully opaque, so the location photo
        // is never visible there - rendering it at the full card width anyway just makes
        // object-cover crop it harder to fill space nothing shows, same problem the band
        // photo itself had. Sizing its box to just the region it can ever appear in fixes
        // it the same way. colorMode darkens it - the band side no longer being a photo
        // itself, the location photo is the only actual picture on the card.
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={locationImage}
          alt=""
          className={`absolute inset-y-0 right-0 h-full object-cover ${colorMode ? "brightness-50 saturate-75" : ""}`}
          style={{ width: `${(1 - BAND_FADE_START) * 100}%` }}
        />
      )}
      {colorMode ? (
        <div
          className="absolute inset-y-0 left-0 h-full"
          style={{ width: `${boxWidth * 100}%`, backgroundColor: entityColor(band.name), ...fadeMask }}
        />
      ) : (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={band.url ?? ""}
          alt=""
          className="absolute inset-y-0 left-0 h-full object-cover"
          style={{ width: `${boxWidth * 100}%`, ...fadeMask }}
        />
      )}
      {colorMode && (
        // Not masked like the color fill behind it - the logo itself shouldn't fade out,
        // only the flat color it's floating over.
        <div className="absolute inset-y-0 left-0 h-full" style={{ width: `${boxWidth * 100}%` }}>
          <BandLogoOverlay logoUrl={band.logoUrl} />
        </div>
      )}
      {showLabel && (
        <div
          className="pointer-events-none absolute bottom-0 left-0 flex flex-col pb-2 pl-2 sm:pb-3 sm:pl-3"
          style={{ width: locationImage ? "55%" : "100%" }}
        >
          <span className="break-words font-display text-base font-bold uppercase leading-tight tracking-wide text-white [text-shadow:0_2px_6px_rgba(0,0,0,0.85)] sm:text-xl">
            {band.name}
          </span>
          {band.genres[0] && (
            <span className="truncate font-meta text-xs uppercase tracking-wide text-white/80 [text-shadow:0_1px_4px_rgba(0,0,0,0.85)]">
              {band.genres[0]}
            </span>
          )}
        </div>
      )}
    </div>
  );
}

