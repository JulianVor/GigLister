"use client";

import { useState, useTransition } from "react";
import { claimBandAction } from "@/actions/bands";
import { claimLocationAction } from "@/actions/locations";
import type { EntityType } from "@/lib/types";

export function ClaimButton({ entityType, entityId }: { entityType: EntityType; entityId: number }) {
  const [state, setState] = useState<"idle" | "sent" | "error">("idle");
  const [pending, startTransition] = useTransition();

  function claim() {
    startTransition(async () => {
      const result =
        entityType === "BAND" ? await claimBandAction(entityId) : await claimLocationAction(entityId);
      setState(result.ok ? "sent" : "error");
    });
  }

  if (state === "sent") {
    return (
      <p className="font-meta text-sm text-muted">
        Anfrage gesendet — ein Admin prüft das in Kürze.
      </p>
    );
  }

  return (
    <div className="space-y-2 border border-line p-4">
      <p className="font-meta text-sm">
        {entityType === "BAND" ? "Gehörst du zu dieser Band?" : "Verwaltest du diesen Veranstaltungsort?"}
      </p>
      <button
        type="button"
        onClick={claim}
        disabled={pending}
        className="border border-accent px-4 py-2 font-meta text-sm text-accent hover:bg-accent hover:text-accent-fg"
      >
        {entityType === "BAND" ? "Band beanspruchen" : "Location beanspruchen"}
      </button>
      {state === "error" && <p className="font-meta text-xs text-accent">Das hat nicht geklappt, bitte erneut versuchen.</p>}
    </div>
  );
}
