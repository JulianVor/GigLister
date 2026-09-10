import Link from "next/link";
import type { LocationListItem } from "@/lib/types";

export function LocationTeaser({ location }: { location: LocationListItem }) {
  return (
    <Link
      href={`/orte/${location.id}`}
      className="flex items-baseline justify-between gap-4 border-b border-line py-4 hover:text-accent"
    >
      <div>
        <div className="font-display text-lg">{location.name}</div>
        <div className="font-meta text-sm text-muted">{location.city}</div>
      </div>
      <div className="font-meta text-sm text-muted whitespace-nowrap">
        {location.upcomingEventCount} kommende{location.upcomingEventCount === 1 ? "s" : ""} Konzert
        {location.upcomingEventCount === 1 ? "" : "e"}
      </div>
    </Link>
  );
}
