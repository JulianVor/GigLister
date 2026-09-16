"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";

/** Shown once right after a login that lands on a profile with no preferredGenres set yet
 * (see loginAction's `?genrePrompt=1` redirect) - a nudge towards the one explicit signal
 * behind "Das könnte dich interessieren" (see GenrePreferenceForm), not a blocking gate
 * like RequirePasswordChange, so skipping it is just as valid as answering it. */
export function GenrePromptDialog() {
  const pathname = usePathname();
  const [open, setOpen] = useState(true);

  useEffect(() => {
    // Drops the one-time query param from the address bar without a Next.js navigation -
    // a router.replace would re-render this page's Server Component against the now-gone
    // param and unmount this dialog (losing the `open` state) the instant it lands,
    // closing it out from under whoever's still reading it.
    window.history.replaceState(null, "", pathname);
  }, [pathname]);

  useEffect(() => {
    if (!open) return;
    function handleKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [open]);

  if (!open) return null;

  return (
    <div
      role="presentation"
      onClick={() => setOpen(false)}
      // Above LocationPicker/UserMenu's own z-[1010] floor (itself set to clear Leaflet's
      // panes/controls, which reach z-index 1000 with no stacking context of their own) -
      // this sits over the whole header, not just the map, so it needs to win that too.
      className="fixed inset-0 z-[1100] flex items-center justify-center bg-fg/40 p-4"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="genre-prompt-title"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-sm border border-line bg-surface p-6 shadow-lg"
      >
        <h2 id="genre-prompt-title" className="font-display text-xl">
          Lieblingsgenres festlegen?
        </h2>
        <p className="mt-2 font-meta text-sm text-muted">
          Wähl aus, welche Musikrichtungen dich interessieren - wir zeigen dir dann passendere Vorschläge unter
          &quot;Das könnte dich interessieren&quot;.
        </p>
        <div className="mt-5 flex gap-3">
          <Link
            href="/einstellungen#genres"
            onClick={() => setOpen(false)}
            className="flex-1 bg-fg py-2 text-center text-sm text-bg hover:bg-accent hover:text-accent-fg"
          >
            Genres festlegen
          </Link>
          <button
            type="button"
            onClick={() => setOpen(false)}
            className="flex-1 border border-line py-2 text-sm font-meta hover:border-fg"
          >
            Überspringen
          </button>
        </div>
      </div>
    </div>
  );
}
