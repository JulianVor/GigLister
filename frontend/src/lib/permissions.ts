import type { EntityType, EventResponse, MeResponse } from "./types";

export function canManageEntity(session: MeResponse | null, entityType: EntityType, entityId: number): boolean {
  if (!session) return false;
  if (session.platformAdmin) return true;
  return session.managedEntities.some((m) => m.entityType === entityType && m.entityId === entityId);
}

/** Creator, platform admin, or anyone managing the location or one of the bands in the line-up. */
export function canEditEvent(session: MeResponse | null, event: EventResponse): boolean {
  if (!session) return false;
  if (session.platformAdmin || session.id === event.createdBy) return true;
  if (canManageEntity(session, "LOCATION", event.location.id)) return true;
  return event.bands.some((b) => canManageEntity(session, "BAND", b.id));
}
