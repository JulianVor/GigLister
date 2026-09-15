import Link from "next/link";
import type { GenreFilterOption } from "@/lib/types";

/** A single collapsible "Genre" field (native <details>, no JS needed) instead of every
 * genre always shown as its own chip - expands to the individual options on click. Several
 * selected genres broaden the search (OR) rather than narrow it further - a concert can't
 * realistically be expected to match all of them at once, so EventService.filterByGenres
 * includes it if it matches any one of the selected genres. Only ever rendered with
 * options that already have at least one matching event (see getGenreFilters), so unlike
 * DateNav there's no separate "clear" option - deselecting the last active genre already
 * gets you back to the unfiltered list. */
export function GenreFilter({
  options,
  active,
  basePath = "/konzerte",
  carryParams = {},
}: {
  options: GenreFilterOption[];
  active: string[];
  basePath?: string;
  carryParams?: Record<string, string | undefined>;
}) {
  if (options.length === 0) {
    return null;
  }

  function href(selection: string[]) {
    const q = new URLSearchParams();
    for (const [key, value] of Object.entries(carryParams)) {
      if (value) q.set(key, value);
    }
    if (selection.length > 0) q.set("genre", selection.join(","));
    const qs = q.toString();
    return qs ? `${basePath}?${qs}` : basePath;
  }

  return (
    <details open={active.length > 0} className="group inline-block border border-line">
      <summary className="flex cursor-pointer select-none items-center gap-2 px-3 py-1.5 font-meta text-sm marker:content-none [&::-webkit-details-marker]:hidden">
        Genre
        {active.length > 0 && <span className="text-accent">({active.length})</span>}
        <span className="text-muted transition-transform group-open:-rotate-180">▾</span>
      </summary>
      <div className="flex max-w-md flex-wrap gap-2 border-t border-line p-3">
        {options.map((option) => {
          const isActive = active.includes(option.genre);
          const nextSelection = isActive ? active.filter((g) => g !== option.genre) : [...active, option.genre];
          return (
            <Link
              key={option.genre}
              href={href(nextSelection)}
              className={`border px-3 py-1.5 font-meta text-sm ${
                isActive ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
              }`}
            >
              {option.genre} <span className={isActive ? "text-accent-fg/70" : "text-muted"}>({option.eventCount})</span>
            </Link>
          );
        })}
      </div>
    </details>
  );
}
