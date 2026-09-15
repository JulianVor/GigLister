import Link from "next/link";

export type DateRangeKey = "today" | "tomorrow" | "weekend" | "week";

const ITEMS: { key: DateRangeKey; label: string }[] = [
  { key: "today", label: "Heute" },
  { key: "tomorrow", label: "Morgen" },
  { key: "weekend", label: "Wochenende" },
  { key: "week", label: "Diese Woche" },
];

/** `basePath` lets other pages (the Orte map) reuse this same Heute/Morgen/.../Kalender
 * filter bar over their own route instead of always linking back into /konzerte - the
 * full calendar view itself stays a single shared page (linked absolutely) rather than
 * a duplicate per basePath. `genre` carries the active GenreFilter selection along so
 * switching the date range doesn't silently drop it. */
export function DateNav({ active, basePath = "/konzerte", genre }: { active?: DateRangeKey; basePath?: string; genre?: string }) {
  function href(range: string) {
    const q = new URLSearchParams({ range });
    if (genre) q.set("genre", genre);
    return `${basePath}?${q.toString()}`;
  }

  return (
    <nav className="flex flex-wrap items-center gap-2 font-meta text-sm tracking-wide">
      {ITEMS.map((item) => (
        <Link
          key={item.key}
          href={href(item.key)}
          className={`border px-3 py-1.5 ${
            active === item.key ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
          }`}
        >
          {item.label}
        </Link>
      ))}
      <Link href="/konzerte/kalender" className="border border-line px-3 py-1.5 hover:border-fg">
        Kalender
      </Link>
    </nav>
  );
}
