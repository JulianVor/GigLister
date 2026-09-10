"use client";

import { useTransition } from "react";
import { useRouter } from "next/navigation";
import { approveClaimAction, rejectClaimAction } from "@/actions/admin";
import type { ClaimResponse } from "@/lib/types";

export function ClaimsList({ claims }: { claims: ClaimResponse[] }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  function decide(id: number, approve: boolean) {
    startTransition(async () => {
      await (approve ? approveClaimAction(id) : rejectClaimAction(id));
      router.refresh();
    });
  }

  if (claims.length === 0) {
    return <p className="font-meta text-sm text-muted">Keine offenen Claims.</p>;
  }

  return (
    <ul className="divide-y divide-line border-y border-line">
      {claims.map((claim) => (
        <li key={claim.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
          <div>
            <div className="font-display text-lg">{claim.entityName}</div>
            <div className="font-meta text-sm text-muted">
              {claim.entityType === "BAND" ? "Band" : "Location"} · angefragt von {claim.requestedByEmail}
            </div>
            {claim.message && <div className="mt-1 font-meta text-sm">„{claim.message}“</div>}
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={pending}
              onClick={() => decide(claim.id, true)}
              className="border border-accent px-3 py-1.5 font-meta text-sm text-accent hover:bg-accent hover:text-accent-fg"
            >
              Genehmigen
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => decide(claim.id, false)}
              className="border border-line px-3 py-1.5 font-meta text-sm hover:border-fg"
            >
              Ablehnen
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
