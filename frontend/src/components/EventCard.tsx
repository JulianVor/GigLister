import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { dayAndMonth, dayNumber, formatTime, monthShort, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";

/** Corner anchor for up to 4 band images - order matches how bands fill in as the lineup
 * grows: the first two take the bottom corners, the next two the top ones. */
const CORNERS = [
  { position: "bottom-0 left-0", origin: "0% 100%" },
  { position: "bottom-0 right-0", origin: "100% 100%" },
  { position: "top-0 left-0", origin: "0% 0%" },
  { position: "top-0 right-0", origin: "100% 0%" },
] as const;

/** An image-forward post-style card (photo up top, a floating date pill, details below) -
 * self-spaced (`mb-4`) so every list of these just stacks without callers adding gaps. */
export function EventCard({ event }: { event: EventSummary }) {
  const time = formatTime(event.startTime);
  const heroImage = event.titleImageUrl;
  const bandImages = event.bands
    .map((b) => b.titleImageUrl)
    .filter((url): url is string => !!url)
    .slice(0, 4);
  const locationImage = event.location.titleImageUrl;
  const collageSourceCount = bandImages.length + (locationImage ? 1 : 0);

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
        ) : collageSourceCount === 1 ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={locationImage ?? bandImages[0]}
            alt=""
            className="h-full w-full object-cover transition-transform duration-300 ease-out group-hover:scale-[1.04]"
          />
        ) : collageSourceCount >= 2 ? (
          <CornerCollage bandImages={bandImages} locationImage={locationImage} />
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

/** Bands sit anchored in the corners, faded toward the center with a transparent radial
 * mask (not a hard crop) so they blend into the location image underneath rather than
 * tiling as separate boxes. Band photos are the point, so their corner tiles are sized
 * off the card's *width* (not height) - on a landscape card that makes them large enough
 * to dominate, overlapping in the middle for two or more bands, while the location image
 * is dimmed a touch so it reads as ambience behind them rather than competing for
 * attention. The location is still a full-bleed base layer underneath everything, so
 * there's never an empty gap regardless of how much of it the bands cover; without a
 * location image, the first band image itself is blurred into a backdrop instead so the
 * same "no gaps" guarantee holds with band photos alone. Each added band shrinks the
 * corner tiles a bit so more of them can fit without collapsing into a single blob. */
function CornerCollage({ bandImages, locationImage }: { bandImages: string[]; locationImage: string | null }) {
  const sizePercent = Math.max(40, 68 - (bandImages.length - 1) * 9);

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
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={bandImages[0]}
          alt=""
          className="absolute inset-0 h-full w-full scale-110 object-cover opacity-70 blur-md"
        />
      )}
      {bandImages.map((url, i) => {
        const corner = CORNERS[i];
        return (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            key={url + i}
            src={url}
            alt=""
            className={`absolute object-cover ${corner.position}`}
            style={{
              width: `${sizePercent}%`,
              aspectRatio: "1 / 1",
              maskImage: `radial-gradient(circle at ${corner.origin}, black 0%, black 48%, transparent 88%)`,
              WebkitMaskImage: `radial-gradient(circle at ${corner.origin}, black 0%, black 48%, transparent 88%)`,
            }}
          />
        );
      })}
    </div>
  );
}
