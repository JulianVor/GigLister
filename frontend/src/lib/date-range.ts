import { addDays, addYears } from "date-fns";
import { toDateOnly, todayISO, upcomingWeekendRange } from "./format";
import type { DateRangeKey } from "@/components/DateNav";

/** Resolves the DateNav range keys (and explicit from/to overrides) to a concrete [from, to] pair. */
export function resolveDateRange(params: {
  range?: string;
  from?: string;
  to?: string;
}): { from: string; to?: string; active?: DateRangeKey } {
  if (params.from) {
    return { from: params.from, to: params.to ?? params.from };
  }

  const today = todayISO();

  switch (params.range as DateRangeKey | undefined) {
    case "today":
      return { from: today, to: today, active: "today" };
    case "tomorrow": {
      const tomorrow = toDateOnly(addDays(new Date(), 1));
      return { from: tomorrow, to: tomorrow, active: "tomorrow" };
    }
    case "weekend": {
      const [from, to] = upcomingWeekendRange();
      return { from, to, active: "weekend" };
    }
    case "week": {
      const to = toDateOnly(addDays(new Date(), 6));
      return { from: today, to, active: "week" };
    }
    default:
      // no filter: open-ended upcoming list, capped so the backend query stays bounded
      return { from: today, to: toDateOnly(addYears(new Date(), 1)) };
  }
}
