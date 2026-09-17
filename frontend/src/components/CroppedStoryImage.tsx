import { TEXT_LAYER_BASE_FONT_CQW, textLayerColor, type TextLayer } from "@/lib/storyTextLayers";

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
}: {
  src: string;
  widthPct: number | null;
  heightPct: number | null;
  centerXPct: number | null;
  centerYPct: number | null;
  rotationDeg: number | null;
  backgroundColor: string | null;
  textLayers?: TextLayer[];
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
            color: textLayerColor(layer.colorHue),
            transform: `translate(-50%, -50%) rotate(${layer.rotationDeg}deg)`,
          }}
        >
          {layer.text}
        </p>
      ))}
    </div>
  );
}
