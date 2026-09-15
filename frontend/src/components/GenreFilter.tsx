import Link from "next/link";
import type { GenreFilterOption } from "@/lib/types";

/** Quick genre filter chips, styled like DateNav's range buttons. Only ever rendered with
 * options that already have at least one matching event (see getGenreFilters) - so unlike
 * DateNav there's no "clear" chip, an empty list just means the section doesn't render at
 * all (nothing to filter by). Clicking the active genre again deselects it. `carryParams`
 * keeps the current date-range filter intact while switching genres (and vice versa via
 * DateNav's own `genre` prop) instead of the two filters clobbering each other. */
export function GenreFilter({
  options,
  active,
  basePath = "/konzerte",
  carryParams = {},
}: {
  options: GenreFilterOption[];
  active?: string;
  basePath?: string;
  carryParams?: Record<string, string | undefined>;
}) {
  if (options.length === 0) {
    return null;
  }

  function href(genre: string | null) {
    const q = new URLSearchParams();
    for (const [key, value] of Object.entries(carryParams)) {
      if (value) q.set(key, value);
    }
    if (genre) q.set("genre", genre);
    const qs = q.toString();
    return qs ? `${basePath}?${qs}` : basePath;
  }

  return (
    <nav className="flex flex-wrap items-center gap-2 font-meta text-sm tracking-wide">
      {options.map((option) => {
        const isActive = active === option.genre;
        return (
          <Link
            key={option.genre}
            href={href(isActive ? null : option.genre)}
            className={`border px-3 py-1.5 ${
              isActive ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
            }`}
          >
            {option.genre} <span className={isActive ? "text-accent-fg/70" : "text-muted"}>({option.eventCount})</span>
          </Link>
        );
      })}
    </nav>
  );
}
