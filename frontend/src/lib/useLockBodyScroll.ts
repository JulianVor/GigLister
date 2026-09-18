"use client";

import { useEffect } from "react";

/** Locks page scrolling while a full-screen overlay (BandStoryViewer/BandStoryComposer) is
 * open - mobile browsers hide/show their address bar in response to scrolling, which resizes
 * the viewport mid-interaction and visibly resizes the story frame along with it. Not
 * scrolling in the first place is the most direct fix for that, on top of sizing the frame
 * off svh/dvh (see both components) so it stays stable even if a scroll sneaks through (e.g.
 * the OS's own rubber-band overscroll). */
export function useLockBodyScroll(active: boolean) {
  useEffect(() => {
    if (!active) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, [active]);
}
