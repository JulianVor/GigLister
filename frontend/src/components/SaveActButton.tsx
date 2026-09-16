"use client";

import { useState, useTransition } from "react";
import { toggleSaveActAction } from "@/actions/events";

/** Merken for a single band within a festival concert - the small, row-level sibling of
 * SaveEventButton. Saving an act also saves the whole concert server-side (UserService.
 * saveAct), so this never needs to touch the event's own saved state itself. */
export function SaveActButton({
  eventId,
  bandId,
  initiallySaved,
}: {
  eventId: number;
  bandId: number;
  initiallySaved: boolean;
}) {
  const [saved, setSaved] = useState(initiallySaved);
  const [pending, startTransition] = useTransition();

  function toggle(e: React.MouseEvent) {
    // This button sits next to (not inside) the row's own Link to the band's page - stop
    // the click from also bubbling into anything listening above it, for the same reason
    // that link wouldn't want a genre chip's click reaching it either.
    e.preventDefault();
    e.stopPropagation();
    const next = !saved;
    setSaved(next);
    startTransition(async () => {
      const result = await toggleSaveActAction(eventId, bandId, next);
      if (!result.ok) setSaved(!next);
    });
  }

  return (
    <button
      type="button"
      onClick={toggle}
      disabled={pending}
      className={`flex-none border px-2.5 py-1 font-meta text-xs tracking-wide transition-colors ${
        saved ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
      }`}
    >
      {saved ? "Gemerkt ✓" : "Merken"}
    </button>
  );
}
