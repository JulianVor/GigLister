import type { EntityStatus } from "@/lib/types";

const LABELS: Record<EntityStatus, string> = {
  STUB: "Stub",
  DRAFT: "Entwurf",
  PUBLISHED: "Veröffentlicht",
  ARCHIVED: "Archiviert",
};

export function StatusBadge({ status }: { status: EntityStatus }) {
  if (status === "PUBLISHED") return null;
  return (
    <span className="inline-block border border-line px-2 py-0.5 font-meta text-xs uppercase tracking-wide text-muted">
      {LABELS[status]}
    </span>
  );
}
