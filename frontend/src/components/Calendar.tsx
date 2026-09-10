import Link from "next/link";
import { addMonths, eachDayOfInterval, endOfMonth, format, getDay, startOfMonth, subMonths } from "date-fns";
import { de } from "date-fns/locale";
import type { CalendarDayCount } from "@/lib/types";
import { toDateOnly } from "@/lib/format";

const WEEKDAYS = ["Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"];

export function Calendar({ year, month, counts }: { year: number; month: number; counts: CalendarDayCount[] }) {
  const countByDate = new Map(counts.map((c) => [c.date, c.count]));
  const monthStart = startOfMonth(new Date(year, month - 1, 1));
  const monthEnd = endOfMonth(monthStart);
  const days = eachDayOfInterval({ start: monthStart, end: monthEnd });

  // getDay(): 0 = Sunday .. 6 = Saturday; shift so Monday is column 0.
  const leadingBlanks = (getDay(monthStart) + 6) % 7;

  const prev = subMonths(monthStart, 1);
  const next = addMonths(monthStart, 1);

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <Link
          href={`/konzerte/kalender?year=${prev.getFullYear()}&month=${prev.getMonth() + 1}`}
          className="font-meta text-sm hover:text-accent"
        >
          ← Vorheriger Monat
        </Link>
        <div className="font-display text-xl">{format(monthStart, "MMMM yyyy", { locale: de })}</div>
        <Link
          href={`/konzerte/kalender?year=${next.getFullYear()}&month=${next.getMonth() + 1}`}
          className="font-meta text-sm hover:text-accent"
        >
          Nächster Monat →
        </Link>
      </div>

      <div className="grid grid-cols-7 gap-px border border-line bg-line font-meta text-sm">
        {WEEKDAYS.map((w) => (
          <div key={w} className="bg-surface px-2 py-1 text-center text-xs uppercase tracking-wide text-muted">
            {w}
          </div>
        ))}

        {Array.from({ length: leadingBlanks }).map((_, i) => (
          <div key={`blank-${i}`} className="bg-bg" />
        ))}

        {days.map((day) => {
          const iso = toDateOnly(day);
          const count = countByDate.get(iso) ?? 0;
          if (count === 0) {
            return (
              <div key={iso} className="flex h-16 flex-col items-center justify-center bg-surface text-muted">
                <span>{format(day, "d")}</span>
              </div>
            );
          }
          return (
            <a
              key={iso}
              href={`#d-${iso}`}
              className="flex h-16 flex-col items-center justify-center bg-surface text-fg hover:bg-bg"
            >
              <span>{format(day, "d")}</span>
              <span className="font-display text-xs text-accent">{count}</span>
            </a>
          );
        })}
      </div>
    </div>
  );
}
