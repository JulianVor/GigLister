import Link from "next/link";

/** Row of status-filter pills for an admin list page, preserving the current search query. */
export function StatusFilter({
  basePath,
  statuses,
  active,
  query,
  labels,
}: {
  basePath: string;
  statuses: string[];
  active?: string;
  query?: string;
  /** Display label per status value; falls back to the raw value when omitted. */
  labels?: Record<string, string>;
}) {
  function href(status?: string) {
    const params = new URLSearchParams();
    if (status) params.set("status", status);
    if (query) params.set("q", query);
    const qs = params.toString();
    return qs ? `${basePath}?${qs}` : basePath;
  }

  return (
    <nav className="flex flex-wrap gap-2 font-meta text-sm">
      <Link
        href={href(undefined)}
        className={`border px-3 py-1.5 ${!active ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"}`}
      >
        Alle
      </Link>
      {statuses.map((status) => (
        <Link
          key={status}
          href={href(status)}
          className={`border px-3 py-1.5 ${active === status ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"}`}
        >
          {labels?.[status] ?? status}
        </Link>
      ))}
    </nav>
  );
}
