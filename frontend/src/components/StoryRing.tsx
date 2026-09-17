// Same orange the site's own accent color starts from, sweeping through the rest of the
// spectrum and back - reads unmistakably "bunt" against the otherwise black/white/accent
// editorial palette (see entityColor.ts), the one deliberate exception to it.
const STORY_RING_GRADIENT =
  "conic-gradient(from 180deg, #e8481c, #c2277a, #7a3aa8, #2f6fae, #2f9d6c, #d9a91c, #e8481c)";

/** Square-avatar wrapper: a colorful ring when the band has an active status (see
 * BandStoryAvatarButton), a plain border otherwise - used identically on the homepage's
 * FollowedBandsRow tiles and the band's own profile-picture in bands/[id]. The two-layer
 * padding trick (gradient background, then a bg-bg gap, then the avatar) is the same
 * construction Instagram's own story ring uses. */
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
    <div className={`shrink-0 p-[2px] ${size}`} style={{ background: STORY_RING_GRADIENT }}>
      <div className="h-full w-full bg-bg p-[2px]">
        <div className="h-full w-full overflow-hidden">{children}</div>
      </div>
    </div>
  );
}
