"use client";

import { useState, useTransition } from "react";
import { toggleFollowBandAction } from "@/actions/bands";

export function FollowBandButton({ bandId, initiallyFollowing }: { bandId: number; initiallyFollowing: boolean }) {
  const [following, setFollowing] = useState(initiallyFollowing);
  const [pending, startTransition] = useTransition();

  function toggle() {
    const next = !following;
    setFollowing(next);
    startTransition(async () => {
      const result = await toggleFollowBandAction(bandId, next);
      if (!result.ok) setFollowing(!next);
    });
  }

  return (
    <button
      type="button"
      onClick={toggle}
      disabled={pending}
      className={`border px-5 py-2 font-meta text-sm tracking-wide transition-colors ${
        following ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
      }`}
    >
      {following ? "Gefolgt ✓" : "Band folgen"}
    </button>
  );
}
