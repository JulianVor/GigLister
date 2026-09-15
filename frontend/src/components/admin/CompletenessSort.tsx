import Link from "next/link";

/** Toggles an admin list's sort order through: default -> completeness_asc (least complete
 * first - the most actionable order for an admin) -> completeness_desc -> default. Preserves
 * the current status/search filter, same as StatusFilter. */
export function CompletenessSort({
  basePath,
  sort,
  status,
  query,
}: {
  basePath: string;
  sort?: string;
  status?: string;
  query?: string;
}) {
  const next = sort === "completeness_asc" ? "completeness_desc" : sort === "completeness_desc" ? undefined : "completeness_asc";

  function href(targetSort?: string) {
    const params = new URLSearchParams();
    if (status) params.set("status", status);
    if (query) params.set("q", query);
    if (targetSort) params.set("sort", targetSort);
    const qs = params.toString();
    return qs ? `${basePath}?${qs}` : basePath;
  }

  const arrow = sort === "completeness_asc" ? "▲" : sort === "completeness_desc" ? "▼" : "";

  return (
    <Link
      href={href(next)}
      className={`border px-3 py-1.5 font-meta text-sm ${
        sort ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
      }`}
    >
      Nach Vollständigkeit {arrow}
    </Link>
  );
}
