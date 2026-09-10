import type { EntityStatus, EventStatus } from "./types";

/** User-facing German labels — chosen to say what the status actually means,
 * not just repeat the technical enum name (e.g. "Unvollständig" instead of "Stub"). */
export const ENTITY_STATUS_LABELS: Record<EntityStatus, string> = {
  STUB: "Unvollständig",
  DRAFT: "Entwurf",
  PUBLISHED: "Veröffentlicht",
  ARCHIVED: "Archiviert",
};

export const ENTITY_STATUS_HINTS: Record<EntityStatus, string> = {
  STUB: "Nur Basisdaten vorhanden, noch keine öffentliche Seite",
  DRAFT: "Mehr Infos vorhanden, aber noch nicht veröffentlicht",
  PUBLISHED: "Öffentlich sichtbar",
  ARCHIVED: "Nicht mehr aktiv, vergangene Konzerte bleiben erhalten",
};

export const EVENT_STATUS_LABELS: Record<EventStatus, string> = {
  DRAFT: "Entwurf",
  PUBLISHED: "Veröffentlicht",
  CANCELLED: "Abgesagt",
};
