"use client";

import { useState } from "react";
import { geocodeMissingLocationsAction } from "@/actions/admin";

/** Triggers the one-off backfill for locations that don't have coordinates yet (created
 * before geocoding existed, or whose address didn't resolve at the time) - see
 * LocationService.backfillMissingCoordinates. Can take a while since it paces its own
 * requests to Nominatim, so this shows a loading state rather than looking stuck. */
export function GeocodeMissingButton() {
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<string | null>(null);

  async function run() {
    setRunning(true);
    setResult(null);
    const res = await geocodeMissingLocationsAction();
    setRunning(false);
    if (!res.ok) {
      setResult(res.error);
      return;
    }
    setResult(
      res.data.attempted === 0
        ? "Alle Orte haben bereits Koordinaten."
        : `${res.data.resolved} von ${res.data.attempted} Orten haben jetzt Koordinaten.`
    );
  }

  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        onClick={run}
        disabled={running}
        className="whitespace-nowrap border border-line px-4 py-2 font-meta text-sm hover:border-fg disabled:opacity-60"
      >
        {running ? "Wird ermittelt …" : "Fehlende Koordinaten ermitteln"}
      </button>
      {result && <span className="font-meta text-xs text-muted">{result}</span>}
    </div>
  );
}
