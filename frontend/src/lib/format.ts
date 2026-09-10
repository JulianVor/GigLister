import { addDays, format, isSameDay, isToday, isTomorrow, parseISO } from "date-fns";
import { de } from "date-fns/locale";

export function toDateOnly(d: Date): string {
  return format(d, "yyyy-MM-dd");
}

export function todayISO(): string {
  return toDateOnly(new Date());
}

export function parseDate(iso: string): Date {
  return parseISO(iso);
}

/** "FR" */
export function weekdayShort(iso: string): string {
  return format(parseISO(iso), "EE", { locale: de }).toUpperCase();
}

/** "18" */
export function dayNumber(iso: string): string {
  return format(parseISO(iso), "d");
}

/** "SEP" */
export function monthShort(iso: string): string {
  return format(parseISO(iso), "MMM", { locale: de }).toUpperCase();
}

/** "18 SEP" */
export function dayAndMonth(iso: string): string {
  return `${dayNumber(iso)} ${monthShort(iso)}`;
}

/** "Donnerstag, 18. September" for headings */
export function fullDateLabel(iso: string): string {
  return format(parseISO(iso), "EEEE, d. MMMM", { locale: de });
}

/** "20:00" from a HH:mm:ss backend time, or null. */
export function formatTime(time: string | null): string | null {
  if (!time) return null;
  return time.slice(0, 5);
}

export function relativeDayLabel(iso: string): string {
  const date = parseISO(iso);
  if (isToday(date)) return "Heute";
  if (isTomorrow(date)) return "Morgen";
  return format(date, "EEEE", { locale: de });
}

export function isSameDate(a: string, b: string): boolean {
  return isSameDay(parseISO(a), parseISO(b));
}

export function addDaysISO(iso: string, amount: number): string {
  return toDateOnly(addDays(parseISO(iso), amount));
}

/** Next Saturday/Sunday (or today+tomorrow if already weekend), as an [from, to] ISO range. */
export function upcomingWeekendRange(from: Date = new Date()): [string, string] {
  const day = from.getDay(); // 0 Sun ... 6 Sat
  const daysUntilSaturday = (6 - day + 7) % 7;
  const saturday = addDays(from, daysUntilSaturday);
  const sunday = addDays(saturday, 1);
  return [toDateOnly(saturday), toDateOnly(sunday)];
}
