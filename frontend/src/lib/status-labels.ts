import type { BandImageDisplay, EntityStatus, EventStatus, SubmissionStatus, SubmissionType } from "./types";

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

export const SUBMISSION_STATUS_LABELS: Record<SubmissionStatus, string> = {
  PENDING: "Offen",
  APPROVED: "Angenommen",
  REJECTED: "Abgelehnt",
};

export const SUBMISSION_TYPE_LABELS: Record<SubmissionType, string> = {
  BAND: "Band",
  LOCATION: "Ort",
  EVENT: "Konzert",
};

export const BAND_IMAGE_DISPLAY_LABELS: Record<BandImageDisplay, string> = {
  LOGO: "Logo",
  PHOTO: "Bandfoto",
};
