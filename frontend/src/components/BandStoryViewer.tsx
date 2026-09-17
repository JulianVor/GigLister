"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { deleteBandStoryAction } from "@/actions/bands";
import { CroppedStoryImage } from "@/components/CroppedStoryImage";
import { EntityPlaceholder } from "@/components/EntityPlaceholder";
import { parseTextLayers } from "@/lib/storyTextLayers";
import type { BandStory } from "@/lib/types";

const STORY_DURATION_MS = 6000;
const PROGRESS_TICK_MS = 50;

/** One segment of the top progress-bar row, and the timer that drives it. Only ticks while
 * `active` (the current story) - `isPast` segments just render full without ever running
 * their own timer, so there's nothing to reset when `index` moves on: each segment's own
 * `progress` state starts at 0 on mount and is only ever touched while it's the active one. */
function StoryProgressSegment({
  active,
  isPast,
  pausedRef,
  onAdvance,
}: {
  active: boolean;
  isPast: boolean;
  pausedRef: React.RefObject<boolean>;
  onAdvance: () => void;
}) {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    if (!active) return;
    const interval = setInterval(() => {
      if (pausedRef.current) return;
      setProgress((p) => {
        const next = p + (PROGRESS_TICK_MS / STORY_DURATION_MS) * 100;
        if (next < 100) return next;
        onAdvance();
        return 100;
      });
    }, PROGRESS_TICK_MS);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active]);

  return (
    <div className="h-1 flex-1 overflow-hidden bg-white/30">
      <div className="h-full bg-white" style={{ width: `${isPast ? 100 : active ? progress : 0}%` }} />
    </div>
  );
}

/** Full-screen tap-through viewer for a band's active stories (see BandStoryAvatarButton,
 * which is the only thing that ever opens this) - same progress-bar-segments-plus-auto-advance
 * shape as Instagram/WhatsApp Status. Full-bleed on mobile; a centered phone-shaped card with
 * a dark backdrop on anything wide enough (sm:) to have room around it. */
export function BandStoryViewer({
  bandId,
  bandName,
  profileImageUrl,
  logoUrl,
  stories: initialStories,
  canManage = false,
  onClose,
}: {
  bandId: number;
  bandName: string;
  profileImageUrl?: string | null;
  logoUrl?: string | null;
  stories: BandStory[];
  /** Whether the current visitor manages this band - shows a "Löschen" button per story. */
  canManage?: boolean;
  onClose: () => void;
}) {
  // Its own copy, not the prop directly - deleting a story removes it from here so the
  // viewer can keep showing the rest without waiting on a refetch from the parent.
  const [stories, setStories] = useState(initialStories);
  const [index, setIndex] = useState(0);
  const [deleting, setDeleting] = useState(false);
  const pausedRef = useRef(false);

  const goNext = useCallback(() => {
    setIndex((i) => (i < stories.length - 1 ? i + 1 : i));
    if (index >= stories.length - 1) onClose();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [index, stories.length]);

  function goPrev() {
    setIndex((i) => Math.max(0, i - 1));
  }

  async function handleDelete() {
    const story = stories[index];
    if (!story || deleting) return;
    if (!window.confirm("Diesen Status wirklich löschen?")) return;
    setDeleting(true);
    const result = await deleteBandStoryAction(bandId, story.id);
    setDeleting(false);
    if (!result.ok) return;
    const remaining = stories.filter((s) => s.id !== story.id);
    if (remaining.length === 0) {
      onClose();
      return;
    }
    setStories(remaining);
    setIndex((i) => Math.min(i, remaining.length - 1));
  }

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
      if (e.key === "ArrowRight") goNext();
      if (e.key === "ArrowLeft") goPrev();
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [goNext]);

  function pause() {
    pausedRef.current = true;
  }

  function resume() {
    pausedRef.current = false;
  }

  const story = stories[index];
  if (!story) return null;

  return (
    <div
      className="fixed inset-0 z-[2000] flex items-center justify-center bg-black/90"
      role="dialog"
      aria-modal="true"
      aria-label={`Status von ${bandName}`}
    >
      {/* True 9:16, not just "whatever box happens to be available" - the crop a band chose in
          BandStoryComposer is expressed as % of that exact ratio, so reproducing it accurately
          (rather than stretching into an arbitrarily-shaped box) needs the same ratio here. */}
      <div className="relative aspect-[9/16] h-full max-h-[900px] max-w-full overflow-hidden bg-black sm:h-[85vh]">
        <div className="absolute inset-x-0 top-0 z-10 flex gap-1 p-2">
          {stories.map((s, i) => (
            <StoryProgressSegment
              key={s.id}
              active={i === index}
              isPast={i < index}
              pausedRef={pausedRef}
              onAdvance={goNext}
            />
          ))}
        </div>

        <div className="absolute inset-x-0 top-4 z-10 flex items-center justify-between px-3">
          {/* onClose on top of the navigation itself: if the visitor is already on this exact
              band page (e.g. viewing their own story from their own profile), Next treats the
              click as a no-op navigation and never unmounts this dialog - closing it explicitly
              is what actually reveals the profile underneath either way. */}
          <Link href={`/bands/${bandId}`} onClick={onClose} className="flex min-w-0 items-center gap-2">
            <span className="h-7 w-7 flex-none overflow-hidden rounded-full border border-white/40">
              {profileImageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={profileImageUrl} alt="" className="h-full w-full object-cover" />
              ) : logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={logoUrl} alt="" className="h-full w-full bg-surface object-contain p-0.5" />
              ) : (
                <EntityPlaceholder name={bandName} className="h-full w-full" textClassName="text-xs" />
              )}
            </span>
            <span className="truncate font-meta text-sm font-medium text-white drop-shadow">{bandName}</span>
          </Link>
          <div className="flex items-center gap-3">
            {canManage && (
              <button
                type="button"
                onClick={handleDelete}
                disabled={deleting}
                className="font-meta text-xs text-white/90 underline hover:text-white disabled:opacity-60"
              >
                {deleting ? "Wird gelöscht …" : "Löschen"}
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              aria-label="Schließen"
              className="px-2 py-1 font-meta text-lg leading-none text-white/90 hover:text-white"
            >
              ✕
            </button>
          </div>
        </div>

        <CroppedStoryImage
          src={story.imageUrl}
          widthPct={story.imgWidthPct}
          heightPct={story.imgHeightPct}
          centerXPct={story.imgCenterXPct}
          centerYPct={story.imgCenterYPct}
          rotationDeg={story.imgRotationDeg}
          backgroundColor={story.imgBackgroundColor}
          textLayers={parseTextLayers(story.textLayersJson)}
        />

        <button
          type="button"
          aria-label="Vorherige"
          className="absolute inset-y-0 left-0 w-1/3 cursor-default"
          onClick={goPrev}
          onMouseDown={pause}
          onMouseUp={resume}
          onTouchStart={pause}
          onTouchEnd={resume}
        />
        <button
          type="button"
          aria-label="Nächste"
          className="absolute inset-y-0 right-0 w-2/3 cursor-default"
          onClick={goNext}
          onMouseDown={pause}
          onMouseUp={resume}
          onTouchStart={pause}
          onTouchEnd={resume}
        />
      </div>
    </div>
  );
}
