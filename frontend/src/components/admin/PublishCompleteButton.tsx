"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { publishCompleteBandsAction, publishCompleteLocationsAction } from "@/actions/admin";
import type { ActionResult } from "@/lib/action-result";

type EntityKind = "band" | "location";

const ACTIONS: Record<EntityKind, () => Promise<ActionResult<{ checked: number; published: number }>>> = {
  band: publishCompleteBandsAction,
  location: publishCompleteLocationsAction,
};

/** Bulk-publishes every STUB/DRAFT band or location that already clears the same
 * completeness bar a GPT-skill submission's enrichment uses (see
 * BandService/LocationService.publishAllComplete) - the one thing a normal edit never does
 * on its own, since update() doesn't re-check completeness the way applyEnrichment does.
 * Shown on the Entwurf/Unvollständig views since those are exactly what it sweeps;
 * refreshes the list since a published row drops out of either filter. */
export function PublishCompleteButton({ entityType }: { entityType: EntityKind }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function run() {
    setError(null);
    setResult(null);
    startTransition(async () => {
      const res = await ACTIONS[entityType]();
      if (!res.ok) {
        setError(res.error);
        return;
      }
      setResult(
        res.data.published === 0
          ? "Nichts war bereits vollständig genug."
          : `${res.data.published} von ${res.data.checked} veröffentlicht.`
      );
      router.refresh();
    });
  }

  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        onClick={run}
        disabled={pending}
        className="whitespace-nowrap border border-line px-4 py-2 font-meta text-sm hover:border-fg disabled:opacity-60"
      >
        {pending ? "Wird geprüft …" : "Alle auf Vollständigkeit setzen"}
      </button>
      {error && <span className="font-meta text-xs text-accent">{error}</span>}
      {result && <span className="font-meta text-xs text-muted">{result}</span>}
    </div>
  );
}
