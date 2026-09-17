// Same orange the site's own accent color starts from, sweeping through the rest of the
// spectrum and back - reads unmistakably "bunt" against the otherwise black/white/accent
// editorial palette (see entityColor.ts), the one deliberate exception to it.
const STORY_RING_GRADIENT =
  "conic-gradient(from 180deg, #e8481c, #c2277a, #7a3aa8, #2f6fae, #2f9d6c, #d9a91c, #e8481c)";

/** Square-avatar wrapper: a colorful ring when the band has an active status (see
 * BandStoryAvatarButton), a plain border otherwise - used identically on the homepage's
 * FollowedBandsRow tiles and the band's own profile-picture in bands/[id]. The ring is drawn
 * OUTSIDE the given box (two layers behind it, extending past its edges) rather than inset
 * via padding - insetting shrank whatever's inside it, which badly squeezed
 * BandProfileImage's hover-to-upload text in the already-small 64px avatar box it wraps on
 * the band's own page. Growing outward instead keeps the wrapped content exactly the size it
 * would've been without the ring. */
export function StoryRing({
  active,
  size,
  children,
}: {
  active: boolean;
  size: string;
  children: React.ReactNode;
}) {
  if (!active) {
    return <div className={`overflow-hidden border border-line ${size}`}>{children}</div>;
  }

  return (
    <div className={`relative shrink-0 ${size}`}>
      <div className="absolute -inset-[6px]" style={{ background: STORY_RING_GRADIENT }} />
      <div className="absolute -inset-[3px] bg-bg" />
      <div className="absolute inset-0 overflow-hidden">{children}</div>
    </div>
  );
}
