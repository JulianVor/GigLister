"use client";

import { useState, useTransition } from "react";
import { toggleSaveEventAction } from "@/actions/events";

export function SaveEventButton({ eventId, initiallySaved }: { eventId: number; initiallySaved: boolean }) {
  const [saved, setSaved] = useState(initiallySaved);
  const [pending, startTransition] = useTransition();

  function toggle() {
    const next = !saved;
    setSaved(next);
    startTransition(async () => {
      const result = await toggleSaveEventAction(eventId, next);
      if (!result.ok) setSaved(!next);
    });
  }

  return (
    <button
      type="button"
      onClick={toggle}
      disabled={pending}
      className={`border px-5 py-2 font-meta text-sm tracking-wide transition-colors ${
        saved ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
      }`}
    >
      {saved ? "Gemerkt ✓" : "Merken"}
    </button>
  );
}
