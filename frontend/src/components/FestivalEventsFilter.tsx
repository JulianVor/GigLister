"use client";

import { useEffect, useTransition } from "react";
import { usePathname, useRouter } from "next/navigation";
import { FESTIVAL_EVENTS_FILTER_COOKIE, type FestivalEventsFilterValue } from "@/lib/festival-cookies";

const COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

const OPTIONS: { value: FestivalEventsFilterValue; label: string }[] = [
  { value: "ALL", label: "Alle Konzerte" },
  { value: "SAVED", label: "Gemerkte Konzerte" },
];

function setCookie(name: string, value: string) {
  document.cookie = `${name}=${value};path=/;max-age=${COOKIE_MAX_AGE}`;
}

/** A festival can have far more concerts than the ones you actually merken - this lets you
 * narrow the page down to just those, same idea as the homepage's own saved-events list but
 * scoped to one festival. The choice is a cookie (see festival-cookies.ts), not component
 * state, so it survives a reload/revisit exactly like the site's location filter does -
 * `router.refresh()` re-renders the (server) festival page against the new cookie value. */
export function FestivalEventsFilter({
  activeFilter,
  clearFilterParam = false,
}: {
  activeFilter: FestivalEventsFilterValue;
  /** True when this render's activeFilter came from the one-time `?filter=saved` link the
   * homepage's saved-concerts list points at (see NextConcertsList), not from the cookie.
   * That query param has to be stripped from the URL right after landing - router.refresh()
   * (what every toggle click below does) re-requests the SAME URL, so as long as
   * `?filter=saved` stayed in it, every future click would keep landing back on SAVED
   * (including a deliberate click on "Alle Konzerte") - the cookie takes over instead once
   * this runs once, and normal toggling starts working. */
  clearFilterParam?: boolean;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const [pending, startTransition] = useTransition();

  useEffect(() => {
    if (!clearFilterParam) return;
    setCookie(FESTIVAL_EVENTS_FILTER_COOKIE, "SAVED");
    router.replace(`${pathname}#konzerte`, { scroll: false });
    // Only ever meant to run once, right after landing on the query-param link - re-running
    // on every activeFilter change would fight a deliberate click straight back to SAVED.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function select(next: FestivalEventsFilterValue) {
    // Ignoring a click while the previous one is still in flight isn't just a nicety - two
    // overlapping router.refresh() calls can resolve out of order, and whichever one lands
    // last wins regardless of which was clicked last, silently reverting a real click back
    // to the other tab. One in flight at a time avoids that race entirely.
    if (next === activeFilter || pending) return;
    setCookie(FESTIVAL_EVENTS_FILTER_COOKIE, next);
    startTransition(() => {
      router.refresh();
    });
  }

  return (
    <div className="flex gap-2">
      {OPTIONS.map((option) => (
        <button
          key={option.value}
          type="button"
          onClick={() => select(option.value)}
          disabled={pending}
          aria-pressed={activeFilter === option.value}
          className={`border px-3 py-1.5 font-meta text-sm disabled:opacity-60 ${
            activeFilter === option.value ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
          }`}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
