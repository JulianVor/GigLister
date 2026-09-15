"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { cancelEventAction, reactivateEventAction } from "@/actions/events";
import type { EventStatus } from "@/lib/types";

/** Sets an event's status to CANCELLED (or back to PUBLISHED, to undo an accidental
 * cancellation) - only ever rendered for someone canEditEvent already lets edit the event,
 * same permission the backend itself checks. A cancelled event drops out of every
 * "upcoming" listing/recommendation on its own, since those only ever query PUBLISHED
 * events (see EventService.listUpcoming/DiscoverService) - no extra filtering needed here. */
export function CancelEventButton({ eventId, status }: { eventId: number; status: EventStatus }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const isCancelled = status === "CANCELLED";

  function run() {
    if (!isCancelled && !window.confirm("Konzert wirklich absagen? Es verschwindet dann aus den Konzert-Übersichten.")) {
      return;
    }
    setError(null);
    startTransition(async () => {
      const result = isCancelled ? await reactivateEventAction(eventId) : await cancelEventAction(eventId);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      router.refresh();
    });
  }

  return (
    <>
      <button
        type="button"
        onClick={run}
        disabled={pending}
        className="border border-line px-5 py-2 font-meta text-sm hover:border-fg disabled:opacity-60"
      >
        {pending ? "Wird gespeichert …" : isCancelled ? "Wieder aktivieren" : "Absagen"}
      </button>
      {error && <p className="mt-2 w-full font-meta text-sm text-accent">{error}</p>}
    </>
  );
}
