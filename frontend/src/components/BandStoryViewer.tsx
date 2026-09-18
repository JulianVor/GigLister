"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { deleteBandStoryAction } from "@/actions/bands";
import { CroppedStoryImage } from "@/components/CroppedStoryImage";
import { EntityPlaceholder } from "@/components/EntityPlaceholder";
import { parseTextLayers } from "@/lib/storyTextLayers";
import { parseBandTags } from "@/lib/storyBandTags";
import { useLockBodyScroll } from "@/lib/useLockBodyScroll";
import type { BandStory } from "@/lib/types";

const STORY_DURATION_MS = 6000;
const PROGRESS_TICK_MS = 50;
// How far down (px) a swipe needs to travel before it closes the viewer, rather than snapping
// back - the classic Instagram/WhatsApp Status gesture.
const SWIPE_CLOSE_THRESHOLD_PX = 110;
// Below this, a touch is still a tap (for the prev/next buttons) rather than a drag.
const SWIPE_START_THRESHOLD_PX = 8;

/** "5 Min." for the first hour, then "3 Std." - matches the same "how old is this status"
 * glance Instagram/WhatsApp Status give, and the band's explicit ask for a minutes/hours
 * switchover at the one-hour mark. Recomputed on every render (there's no ticking timer for
 * this alone) - accurate enough since the viewer already re-renders on every story advance,
 * and nobody needs second-level precision on "how old is this status". */
function formatStoryAge(createdAt: string): string {
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(createdAt).getTime()) / 60000));
  if (minutes < 60) return `${minutes} Min.`;
  return `${Math.floor(minutes / 60)} Std.`;
}

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
  useLockBodyScroll(true);

  // Swipe-down-to-close (the classic Instagram/WhatsApp Status gesture): tracked as a ref (not
  // state) while it's just a candidate drag, since most pointer-downs are actually taps on the
  // prev/next buttons and never need a re-render. `dragOffsetY` (state) only starts changing
  // once a move is unambiguously a downward drag, which is also what the frame's live
  // follow-the-finger transform below is driven by.
  const swipeRef = useRef({ active: false, startX: 0, startY: 0, dragging: false });
  const [dragOffsetY, setDragOffsetY] = useState(0);
  // Set for exactly one click after a genuine drag, so the prev/next buttons' own onClick
  // (which the browser still fires right after a drag's pointerup, on whichever button the
  // finger happened to be over) doesn't also turn that release into a spurious page change.
  const suppressNextClickRef = useRef(false);

  function onFramePointerDown(e: React.PointerEvent) {
    swipeRef.current = { active: true, startX: e.clientX, startY: e.clientY, dragging: false };
    pause();
  }

  function onFramePointerMove(e: React.PointerEvent) {
    const swipe = swipeRef.current;
    if (!swipe.active) return;
    const dx = e.clientX - swipe.startX;
    const dy = e.clientY - swipe.startY;
    if (!swipe.dragging) {
      if (Math.abs(dy) < SWIPE_START_THRESHOLD_PX && Math.abs(dx) < SWIPE_START_THRESHOLD_PX) return;
      // Only claims the gesture once it's clearly more vertical-downward than sideways -
      // anything else (a horizontal or upward move) stays a normal tap/prev/next.
      if (dy <= 0 || dy < Math.abs(dx)) return;
      swipe.dragging = true;
    }
    setDragOffsetY(Math.max(0, dy));
  }

  function onFramePointerUp() {
    const swipe = swipeRef.current;
    const wasDragging = swipe.dragging;
    swipe.active = false;
    swipe.dragging = false;
    if (wasDragging) {
      suppressNextClickRef.current = true;
      if (dragOffsetY > SWIPE_CLOSE_THRESHOLD_PX) {
        onClose();
        return;
      }
      setDragOffsetY(0);
    }
    resume();
  }

  function onFrameClickCapture(e: React.MouseEvent) {
    if (!suppressNextClickRef.current) return;
    suppressNextClickRef.current = false;
    e.stopPropagation();
  }

  // A cancelled/interrupted gesture (finger left the frame, browser took over) always just
  // snaps back - never closes, unlike a clean release past the threshold.
  function onFrameGestureAbort() {
    swipeRef.current.active = false;
    swipeRef.current.dragging = false;
    setDragOffsetY(0);
    resume();
  }

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
      className="fixed inset-x-0 top-0 z-[2000] flex h-[100dvh] select-none items-center justify-center [-webkit-touch-callout:none]"
      style={{ backgroundColor: story.imgBackgroundColor ?? "#000" }}
      role="dialog"
      aria-modal="true"
      aria-label={`Status von ${bandName}`}
    >
      {/* True 9:16, not just "whatever box happens to be available" - the crop a band chose in
          BandStoryComposer is expressed as % of that exact ratio, so reproducing it accurately
          (rather than stretching into an arbitrarily-shaped box) needs the same ratio here.
          Sized off svh (not h-full/vh) so a mobile browser's address bar hiding/showing mid-
          scroll - which resizes the *actual* viewport - never resizes this frame along with
          it: svh locks to the viewport's smallest state (bar visible), so any extra height a
          hidden bar frees up just shows as more of the backdrop above/below instead of
          stretching the frame - centered here by the parent's items-center, matching the
          image's own dominant color set as that backdrop above. */}
      <div
        className="relative aspect-[9/16] h-[100svh] max-h-[900px] max-w-full touch-none overflow-hidden bg-black sm:h-[85svh]"
        style={{
          transform: dragOffsetY > 0 ? `translateY(${dragOffsetY}px)` : undefined,
          opacity: dragOffsetY > 0 ? Math.max(0.5, 1 - dragOffsetY / 500) : 1,
          transition: dragOffsetY === 0 ? "transform 0.2s ease-out, opacity 0.2s ease-out" : undefined,
        }}
        onPointerDown={onFramePointerDown}
        onPointerMove={onFramePointerMove}
        onPointerUp={onFramePointerUp}
        onPointerCancel={onFrameGestureAbort}
        onPointerLeave={onFrameGestureAbort}
        onClickCapture={onFrameClickCapture}
      >
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
            <span className="h-7 w-7 flex-none overflow-hidden border border-white/40">
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
            <span className="flex-none font-meta text-xs text-white/70 drop-shadow">· {formatStoryAge(story.createdAt)}</span>
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
          bandTags={parseBandTags(story.bandTagsJson)}
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
