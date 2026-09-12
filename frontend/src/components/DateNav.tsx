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
 * a duplicate per basePath. */
export function DateNav({ active, basePath = "/konzerte" }: { active?: DateRangeKey; basePath?: string }) {
  return (
    <nav className="flex flex-wrap items-center gap-2 font-meta text-sm tracking-wide">
      {ITEMS.map((item) => (
        <Link
          key={item.key}
          href={`${basePath}?range=${item.key}`}
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
