"use client";

import { useTransition } from "react";
import { useRouter } from "next/navigation";
import { mergeEntitiesAction } from "@/actions/admin";
import type { DuplicatePair } from "@/lib/types";

export function DuplicatesList({ pairs }: { pairs: DuplicatePair[] }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  function merge(pair: DuplicatePair, keep: "first" | "second") {
    const [sourceId, targetId] =
      keep === "first" ? [pair.secondId, pair.firstId] : [pair.firstId, pair.secondId];
    startTransition(async () => {
      await mergeEntitiesAction(pair.entityType, sourceId, targetId);
      router.refresh();
    });
  }

  if (pairs.length === 0) {
    return <p className="font-meta text-sm text-muted">Keine wahrscheinlichen Duplikate gefunden.</p>;
  }

  return (
    <ul className="divide-y divide-line border-y border-line">
      {pairs.map((pair, i) => (
        <li key={i} className="py-4">
          <div className="font-meta text-xs uppercase tracking-wide text-muted">
            {pair.entityType === "BAND" ? "Band" : "Location"} · {Math.round(pair.similarity * 100)}% Übereinstimmung
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-3">
            <div className="font-display text-lg">
              {pair.firstName} <span className="font-meta text-sm text-muted">#{pair.firstId}</span>
            </div>
            <span className="font-meta text-sm text-muted">vs.</span>
            <div className="font-display text-lg">
              {pair.secondName} <span className="font-meta text-sm text-muted">#{pair.secondId}</span>
            </div>
          </div>
          <div className="mt-2 flex gap-2">
            <button
              type="button"
              disabled={pending}
              onClick={() => merge(pair, "first")}
              className="border border-line px-3 py-1.5 font-meta text-xs hover:border-fg"
            >
              #{pair.firstId} behalten, #{pair.secondId} zusammenführen
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => merge(pair, "second")}
              className="border border-line px-3 py-1.5 font-meta text-xs hover:border-fg"
            >
              #{pair.secondId} behalten, #{pair.firstId} zusammenführen
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
