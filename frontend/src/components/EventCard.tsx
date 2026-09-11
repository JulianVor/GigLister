import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { dayAndMonth, dayNumber, formatTime, monthShort, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";

interface BandPhoto {
  name: string;
  url: string;
}

/** An image-forward post-style card (photo up top, a floating date pill, details below) -
 * self-spaced (`mb-4`) so every list of these just stacks without callers adding gaps. */
export function EventCard({ event }: { event: EventSummary }) {
  const time = formatTime(event.startTime);
  const heroImage = event.titleImageUrl;
  const locationImage = event.location.titleImageUrl;
  const bandPhotos: BandPhoto[] = event.bands
    .map((b) => ({ name: b.name, url: event.bandImageDisplay === "PHOTO" ? b.titleImageUrl : b.logoUrl }))
    .filter((entry): entry is BandPhoto => !!entry.url)
    .slice(0, 4);

  let content;
  if (heroImage) {
    content = <PlainCover src={heroImage} />;
  } else if (bandPhotos.length === 0) {
    content = locationImage ? (
      <PlainCover src={locationImage} />
    ) : (
      <div className="flex h-full w-full flex-col items-center justify-center gap-1">
        <div className="font-display text-6xl leading-none sm:text-7xl">{dayNumber(event.date)}</div>
        <div className="font-meta text-sm uppercase tracking-wide text-muted">{monthShort(event.date)}</div>
      </div>
    );
  } else if (event.bandImageDisplay === "PHOTO") {
    content =
      bandPhotos.length === 1 ? (
        <SingleBandCollage band={bandPhotos[0]} locationImage={locationImage} />
      ) : (
        <DiagonalPhotoCollage bands={bandPhotos} />
      );
  } else {
    content = <LogoCollage logos={bandPhotos.map((b) => b.url)} locationImage={locationImage} />;
  }

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

function segmentClipPath(seg: Segment): string {
  const points: [number, number][] = [
    [cutX(seg.left, seg.top), seg.top],
    [cutX(seg.right, seg.top), seg.top],
    [cutX(seg.right, seg.bottom), seg.bottom],
    [cutX(seg.left, seg.bottom), seg.bottom],
  ];
  return `polygon(${points.map(([x, y]) => `${x * 100}% ${y * 100}%`).join(", ")})`;
}

/** 2 or 4 bands split the card into equal same-height diagonal strips. 3 gives the lead
 * band a wider strip (matching how it's usually billed) with the other two stacked in
 * the remainder, rather than three equally-narrow slivers. */
function layoutSegments(bands: BandPhoto[]): Segment[] {
  if (bands.length === 3) {
    const mainWidth = 0.58;
    return [
      { ...bands[0], left: 0, right: mainWidth, top: 0, bottom: 1 },
      { ...bands[1], left: mainWidth, right: 1, top: 0, bottom: 0.5 },
      { ...bands[2], left: mainWidth, right: 1, top: 0.5, bottom: 1 },
    ];
  }
  const n = bands.length;
  return bands.map((band, i) => ({ ...band, left: i / n, right: (i + 1) / n, top: 0, bottom: 1 }));
}

/** Full-bleed diagonal-cut lineup, each photo cropped to its own strip and labeled with
 * the band's name - no location image shown once there's more than one band, since the
 * strips already fill the whole card between them. */
function DiagonalPhotoCollage({ bands }: { bands: BandPhoto[] }) {
  const segments = layoutSegments(bands);

  return (
    <div className="relative h-full w-full overflow-hidden bg-surface">
      {segments.map((seg, i) => (
        <div key={i} className="absolute inset-0" style={{ clipPath: segmentClipPath(seg) }}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={seg.url} alt="" className="h-full w-full object-cover" />
        </div>
      ))}
      {segments.map((seg, i) => (
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

/** The one-band case keeps the location visible - the band photo covers the left ~60%
 * and fades via a plain transparent gradient (no diagonal cut, this is the only place
 * with a soft edge) into the location photo on the right. Without a location image the
 * band photo just fills the whole card, same as it always would. */
function SingleBandCollage({ band, locationImage }: { band: BandPhoto; locationImage: string | null }) {
  return (
    <div className="relative h-full w-full overflow-hidden bg-surface">
      {locationImage && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={locationImage} alt="" className="absolute inset-0 h-full w-full object-cover" />
      )}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={band.url}
        alt=""
        className="absolute inset-0 h-full w-full object-cover"
        style={
          locationImage
            ? {
                maskImage: "linear-gradient(to right, black 0%, black 58%, transparent 78%)",
                WebkitMaskImage: "linear-gradient(to right, black 0%, black 58%, transparent 78%)",
              }
            : undefined
        }
      />
      <div
        className="pointer-events-none absolute bottom-0 left-0 flex items-end pb-2 pl-2 sm:pb-3 sm:pl-3"
        style={{ width: locationImage ? "55%" : "100%" }}
      >
        <span className="truncate font-display text-base font-bold uppercase tracking-wide text-white [text-shadow:0_2px_6px_rgba(0,0,0,0.85)] sm:text-xl">
          {band.name}
        </span>
      </div>
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
