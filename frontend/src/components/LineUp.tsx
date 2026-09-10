import Link from "next/link";
import type { BandSummary } from "@/lib/types";
import { StatusBadge } from "./StatusBadge";

export function LineUp({ bands }: { bands: BandSummary[] }) {
  return (
    <div className="divide-y divide-line border-y border-line">
      {bands.map((band) => {
        const content = (
          <div className="flex items-center gap-4 py-3">
            {band.logoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={band.logoUrl} alt="" className="h-10 w-10 flex-none border border-line object-cover" />
            ) : (
              <div className="h-10 w-10 flex-none border border-line" />
            )}
            <div>
              <div className="font-display text-base">{band.name}</div>
              {band.city && <div className="font-meta text-sm text-muted">{band.city}</div>}
            </div>
            <StatusBadge status={band.status} />
          </div>
        );

        return band.linkable ? (
          <Link key={band.id} href={`/bands/${band.id}`} className="block hover:text-accent">
            {content}
          </Link>
        ) : (
          <div key={band.id}>{content}</div>
        );
      })}
    </div>
  );
}
