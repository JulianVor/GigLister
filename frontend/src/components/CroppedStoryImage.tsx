const FALLBACK_BG = "#111111";

/** Renders a BandStory's image exactly as the band positioned/scaled/rotated it in
 * BandStoryComposer - same numbers (% of a 9:16 frame, centered), same
 * left/top-plus-translate(-50%,-50%)-plus-rotate technique, just without the drag/zoom/rotate
 * interactivity. The image itself is never actually cropped server-side; this is what
 * reproduces that view on display, here and in BandStoryComposer's own live preview. The
 * image is never forced to cover the frame, so whatever it doesn't cover shows
 * imgBackgroundColor (an average sampled from the photo itself) instead of a flat gap. */
export function CroppedStoryImage({
  src,
  widthPct,
  heightPct,
  centerXPct,
  centerYPct,
  rotationDeg,
  backgroundColor,
}: {
  src: string;
  widthPct: number | null;
  heightPct: number | null;
  centerXPct: number | null;
  centerYPct: number | null;
  rotationDeg: number | null;
  backgroundColor: string | null;
}) {
  if (widthPct == null || heightPct == null || centerXPct == null || centerYPct == null) {
    // No crop data (shouldn't normally happen going forward) - show the whole image, letterboxed.
    // eslint-disable-next-line @next/next/no-img-element
    return <img src={src} alt="" className="h-full w-full object-contain" />;
  }

  return (
    <div className="relative h-full w-full overflow-hidden" style={{ backgroundColor: backgroundColor ?? FALLBACK_BG }}>
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
    </div>
  );
}
