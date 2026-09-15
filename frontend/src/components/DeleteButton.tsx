"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { deleteBandAction } from "@/actions/bands";
import { deleteLocationAction } from "@/actions/locations";
import { deleteEventAction } from "@/actions/events";
import type { ActionResult } from "@/lib/action-result";

type EntityKind = "band" | "location" | "event";

const ACTIONS: Record<EntityKind, (id: number) => Promise<ActionResult>> = {
  band: deleteBandAction,
  location: deleteLocationAction,
  event: deleteEventAction,
};

const CONFIRM_MESSAGES: Record<EntityKind, string> = {
  band: "Band wirklich löschen? Das kann nicht rückgängig gemacht werden.",
  location: "Ort wirklich löschen? Das kann nicht rückgängig gemacht werden.",
  event: "Konzert wirklich löschen? Das kann nicht rückgängig gemacht werden.",
};

/** A Band/Location with any event still on it is refused by the backend (409) - the
 * error message it sends back ("...hat noch Konzerte...") is shown as-is, so there's no
 * need to duplicate that condition here. `redirectTo` is for a detail page that won't
 * exist anymore after deletion; omit it (e.g. in an admin list) to just refresh in place. */
export function DeleteButton({
  entityType,
  entityId,
  redirectTo,
  className,
}: {
  entityType: EntityKind;
  entityId: number;
  redirectTo?: string;
  className?: string;
}) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  function handleClick() {
    if (!window.confirm(CONFIRM_MESSAGES[entityType])) return;
    setError(null);
    startTransition(async () => {
      const result = await ACTIONS[entityType](entityId);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      if (redirectTo) {
        router.push(redirectTo);
      } else {
        router.refresh();
      }
    });
  }

  return (
    <span>
      <button
        type="button"
        onClick={handleClick}
        disabled={pending}
        className={className ?? "border border-line px-5 py-2 font-meta text-sm text-accent hover:border-accent disabled:opacity-60"}
      >
        {pending ? "Wird gelöscht …" : "Löschen"}
      </button>
      {error && <p className="mt-1 font-meta text-xs text-accent">{error}</p>}
    </span>
  );
}
