import Link from "next/link";
import { EntityPlaceholder } from "@/components/EntityPlaceholder";
import { TEXT_LAYER_BASE_FONT_CQW, textLayerColor, type TextLayer } from "@/lib/storyTextLayers";
import { BAND_TAG_BASE_FONT_CQW, bandTagColor, type BandTagLayer } from "@/lib/storyBandTags";

const FALLBACK_BG = "#111111";

/** Renders a BandStory's image exactly as the band positioned/scaled/rotated it in
 * BandStoryComposer, plus any text layers it added - same numbers (% of a 9:16 frame,
 * centered), same left/top-plus-translate(-50%,-50%)-plus-rotate technique for both the photo
 * and each text layer, just without the drag/zoom/rotate interactivity. The image itself is
 * never actually cropped server-side; this is what reproduces that view on display, here and
 * in BandStoryComposer's own live preview. The image is never forced to cover the frame, so
 * whatever it doesn't cover shows imgBackgroundColor (an average sampled from the photo
 * itself) instead of a flat gap. */
export function CroppedStoryImage({
  src,
  widthPct,
  heightPct,
  centerXPct,
  centerYPct,
  rotationDeg,
  backgroundColor,
  textLayers = [],
  bandTags = [],
}: {
  src: string;
  widthPct: number | null;
  heightPct: number | null;
  centerXPct: number | null;
  centerYPct: number | null;
  rotationDeg: number | null;
  backgroundColor: string | null;
  textLayers?: TextLayer[];
  bandTags?: BandTagLayer[];
}) {
  if (widthPct == null || heightPct == null || centerXPct == null || centerYPct == null) {
    // No crop data (shouldn't normally happen going forward) - show the whole image, letterboxed.
    // eslint-disable-next-line @next/next/no-img-element
    return <img src={src} alt="" className="h-full w-full object-contain" />;
  }

  return (
    <div
      className="relative h-full w-full overflow-hidden"
      style={{ backgroundColor: backgroundColor ?? FALLBACK_BG, containerType: "inline-size" }}
    >
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={src}
        alt=""
        draggable={false}
        className="absolute max-w-none"
        style={{
          left: `${centerXPct}%`,
          top: `${centerYPct}%`,
          width: `${widthPct}%`,
          height: `${heightPct}%`,
          transform: `translate(-50%, -50%) rotate(${rotationDeg ?? 0}deg)`,
        }}
      />
      {textLayers.map((layer) => (
        <p
          key={layer.id}
          className="absolute max-w-[90%] whitespace-pre-wrap break-words text-center font-display font-bold leading-tight [text-shadow:0_1px_6px_rgba(0,0,0,0.6)]"
          style={{
            left: `${layer.centerXPct}%`,
            top: `${layer.centerYPct}%`,
            fontSize: `${layer.scale * TEXT_LAYER_BASE_FONT_CQW}cqw`,
            color: textLayerColor(layer.colorPos),
            transform: `translate(-50%, -50%) rotate(${layer.rotationDeg}deg)`,
          }}
        >
          {layer.text}
        </p>
      ))}
      {bandTags.map((tag) => (
        // z-10: sits above BandStoryViewer's invisible full-frame prev/next click zones
        // (those have no z-index of their own, so without this a tag would render underneath
        // them in DOM order and never actually be clickable).
        <Link
          key={tag.id}
          href={`/bands/${tag.bandId}`}
          className="absolute z-10 flex max-w-[85%] flex-col items-center gap-[0.2em] whitespace-nowrap"
          style={{
            left: `${tag.centerXPct}%`,
            top: `${tag.centerYPct}%`,
            fontSize: `${tag.scale * BAND_TAG_BASE_FONT_CQW}cqw`,
            transform: `translate(-50%, -50%) rotate(${tag.rotationDeg}deg)`,
          }}
        >
          <span className="block h-[1.8em] w-[1.8em] flex-none overflow-hidden border border-white/80 bg-surface">
            {tag.profileImageUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={tag.profileImageUrl} alt="" className="h-full w-full object-cover" />
            ) : tag.logoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={tag.logoUrl} alt="" className="h-full w-full object-contain p-[0.15em]" />
            ) : (
              <EntityPlaceholder name={tag.bandName} className="h-full w-full" textClassName="text-[0.9em]" />
            )}
          </span>
          <span
            className="truncate font-display text-[0.85em] font-bold leading-tight [text-shadow:0_1px_6px_rgba(0,0,0,0.6)]"
            style={{ color: bandTagColor(tag.colorPos) }}
          >
            {tag.bandName}
          </span>
        </Link>
      ))}
    </div>
  );
}
