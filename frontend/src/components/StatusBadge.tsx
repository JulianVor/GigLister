import type { EntityStatus } from "@/lib/types";
import { ENTITY_STATUS_HINTS, ENTITY_STATUS_LABELS } from "@/lib/status-labels";

export function StatusBadge({ status }: { status: EntityStatus }) {
  if (status === "PUBLISHED") return null;
  return (
    <span
      title={ENTITY_STATUS_HINTS[status]}
      className="inline-block border border-line px-2 py-0.5 font-meta text-xs uppercase tracking-wide text-muted"
    >
      {ENTITY_STATUS_LABELS[status]}
    </span>
  );
}
