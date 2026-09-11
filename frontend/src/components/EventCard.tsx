import Link from "next/link";
import type { EventSummary } from "@/lib/types";
import { dayAndMonth, dayNumber, formatTime, monthShort, weekdayShort } from "@/lib/format";
import { eventLineupLabel } from "@/lib/event-display";

/** Falls back to a live CSS collage of whatever band/location title images exist when the
 * event has none of its own - no image generation or storage needed, and it works with
 * however many (or few) of those images happen to be set. Capped at 4 tiles. */
function collageImages(event: EventSummary): string[] {
  const candidates = [...event.bands.map((b) => b.titleImageUrl), event.location.titleImageUrl];
  return candidates.filter((url): url is string => !!url).slice(0, 4);
}

/** An image-forward post-style card (photo up top, a floating date pill, details below) -
 * self-spaced (`mb-4`) so every list of these just stacks without callers adding gaps. */
export function EventCard({ event }: { event: EventSummary }) {
  const time = formatTime(event.startTime);
  const heroImage = event.titleImageUrl;
  const collage = heroImage ? [] : collageImages(event);

  return (
    <Link href={`/konzerte/${event.id}`} className="group mb-4 block border border-line hover:border-fg">
      <div className="relative h-44 w-full overflow-hidden border-b border-line bg-surface sm:h-60">
        {heroImage || collage.length === 1 ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={heroImage ?? collage[0]}
            alt=""
            className="h-full w-full object-cover transition-transform duration-300 ease-out group-hover:scale-[1.04]"
          />
        ) : collage.length >= 2 ? (
          <ImageCollage images={collage} />
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

/** 2 images side by side; 3 is one big tile + two stacked; 4 is an even grid. Hairline gaps
 * (via a `bg-line` parent showing through a 1px gap) keep it in the site's bordered style. */
function ImageCollage({ images }: { images: string[] }) {
  return (
    <div
      className={`grid h-full w-full gap-px bg-line ${
        images.length === 3 ? "grid-cols-2 grid-rows-2" : images.length === 4 ? "grid-cols-2 grid-rows-2" : "grid-cols-2"
      }`}
    >
      {images.map((url, i) => (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          key={url + i}
          src={url}
          alt=""
          className={`h-full w-full object-cover ${images.length === 3 && i === 0 ? "row-span-2" : ""}`}
        />
      ))}
    </div>
  );
}
