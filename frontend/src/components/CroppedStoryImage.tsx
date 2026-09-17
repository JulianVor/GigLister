/** Renders a BandStory's image exactly as the band positioned/zoomed it in
 * BandStoryComposer - same four numbers (% of a 9:16 frame), same absolute-positioned-image-
 * in-an-overflow-hidden-box technique, just without the drag/zoom interactivity. The image
 * itself is never actually cropped server-side; this is what reproduces that crop on
 * display, here and in BandStoryComposer's own live preview. */
export function CroppedStoryImage({
  src,
  widthPct,
  heightPct,
  offsetLeftPct,
  offsetTopPct,
}: {
  src: string;
  widthPct: number | null;
  heightPct: number | null;
  offsetLeftPct: number | null;
  offsetTopPct: number | null;
}) {
  if (widthPct == null || heightPct == null || offsetLeftPct == null || offsetTopPct == null) {
    // No crop data (shouldn't normally happen going forward) - show the whole image, letterboxed.
    // eslint-disable-next-line @next/next/no-img-element
    return <img src={src} alt="" className="h-full w-full object-contain" />;
  }

  return (
    <div className="relative h-full w-full overflow-hidden">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={src}
        alt=""
        draggable={false}
        className="absolute max-w-none"
        style={{ width: `${widthPct}%`, height: `${heightPct}%`, left: `${offsetLeftPct}%`, top: `${offsetTopPct}%` }}
      />
    </div>
  );
}
