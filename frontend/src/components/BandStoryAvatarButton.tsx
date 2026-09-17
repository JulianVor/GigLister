"use client";

import { useState } from "react";
import { StoryRing } from "@/components/StoryRing";
import { EntityPlaceholder } from "@/components/EntityPlaceholder";
import { BandStoryViewer } from "@/components/BandStoryViewer";
import type { BandStory } from "@/lib/types";

// Same pattern as EntityPicker/UserMenu: an absolute URL the browser can reach directly.
const PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** The colorful-ringed avatar itself, wherever a visitor can see it (FollowedBandsRow tiles,
 * the band's own profile header) - clicking it opens the story viewer instead of navigating
 * away, which is why callers never wrap this in a Link (see FollowedBandsRow for how the
 * band name stays separately clickable alongside it). Pass `preloadedStories` when the page
 * already fetched them server-side (the band's own page always does); otherwise this fetches
 * them itself, lazily, only once someone actually clicks - the homepage's followed-bands row
 * only needs to know `hasStory` upfront (already on MeResponse), not every story's content. */
export function BandStoryAvatarButton({
  bandId,
  bandName,
  profileImageUrl,
  logoUrl,
  size,
  textClassName,
  preloadedStories,
  canManage = false,
}: {
  bandId: number;
  bandName: string;
  profileImageUrl: string | null;
  logoUrl: string | null;
  size: string;
  textClassName?: string;
  preloadedStories?: BandStory[];
  /** Whether the current visitor manages this band - shows a "Löschen" button inside the
   * viewer for each story when true (see BandStoryViewer). */
  canManage?: boolean;
}) {
  const [stories, setStories] = useState<BandStory[] | null>(preloadedStories ?? null);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleClick() {
    if (stories) {
      setOpen(true);
      return;
    }
    setLoading(true);
    try {
      const res = await fetch(`${PUBLIC_API_URL}/api/bands/${bandId}/stories`);
      if (!res.ok) return;
      const data: BandStory[] = await res.json();
      if (data.length === 0) return;
      setStories(data);
      setOpen(true);
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={handleClick}
        disabled={loading}
        aria-label={`Status von ${bandName} ansehen`}
        className={`block disabled:opacity-60 ${size}`}
      >
        <StoryRing active size={size}>
          {profileImageUrl ? (
            // A real photo - fills the square like the site's other title/banner images do.
            // eslint-disable-next-line @next/next/no-img-element
            <img src={profileImageUrl} alt="" className="h-full w-full object-cover" />
          ) : logoUrl ? (
            // A logo mark, not a photo - contained with breathing room instead of cropped,
            // same treatment FollowedBandsRow/the band page have always given logoUrl.
            // eslint-disable-next-line @next/next/no-img-element
            <img src={logoUrl} alt="" className="h-full w-full bg-surface object-contain p-2" />
          ) : (
            <EntityPlaceholder name={bandName} className="h-full w-full" textClassName={textClassName} />
          )}
        </StoryRing>
      </button>
      {open && stories && stories.length > 0 && (
        <BandStoryViewer
          bandId={bandId}
          bandName={bandName}
          stories={stories}
          canManage={canManage}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}
