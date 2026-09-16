"use client";

import { useRouter } from "next/navigation";
import { FESTIVAL_EVENTS_FILTER_COOKIE, type FestivalEventsFilterValue } from "@/lib/festival-cookies";

const COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

const OPTIONS: { value: FestivalEventsFilterValue; label: string }[] = [
  { value: "ALL", label: "Alle Konzerte" },
  { value: "SAVED", label: "Gemerkte Konzerte" },
];

/** A festival can have far more concerts than the ones you actually merken - this lets you
 * narrow the page down to just those, same idea as the homepage's own saved-events list but
 * scoped to one festival. The choice is a cookie (see festival-cookies.ts), not component
 * state, so it survives a reload/revisit exactly like the site's location filter does -
 * `router.refresh()` re-renders the (server) festival page against the new cookie value. */
export function FestivalEventsFilter({ activeFilter }: { activeFilter: FestivalEventsFilterValue }) {
  const router = useRouter();

  function setCookie(name: string, value: string) {
    // Writing document.cookie here only ever runs from the click handler below, never
    // during render - the react-compiler rule can't tell that apart from a render-time
    // mutation of an outside variable, so it flags this DOM write regardless.
    // eslint-disable-next-line react-hooks/immutability
    document.cookie = `${name}=${value};path=/;max-age=${COOKIE_MAX_AGE}`;
  }

  function select(next: FestivalEventsFilterValue) {
    if (next === activeFilter) return;
    setCookie(FESTIVAL_EVENTS_FILTER_COOKIE, next);
    router.refresh();
  }

  return (
    <div className="flex gap-2">
      {OPTIONS.map((option) => (
        <button
          key={option.value}
          type="button"
          onClick={() => select(option.value)}
          aria-pressed={activeFilter === option.value}
          className={`border px-3 py-1.5 font-meta text-sm ${
            activeFilter === option.value ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
          }`}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
