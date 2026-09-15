"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import * as api from "@/lib/api";
import { getToken } from "@/lib/session";
import type { EventInput } from "@/lib/api";
import type { ActionResult } from "@/lib/action-result";

/** result.published: true means the event went live immediately (data.id is its id);
 * false means it has no direct create rights and was routed into the review queue
 * instead (see api.createEvent) - the caller should point the user at "Meine Vorschläge"
 * (Verwaltung) rather than at an event page that doesn't exist yet. */
export async function createEventAction(
  input: EventInput
): Promise<ActionResult<{ published: true; id: number } | { published: false }>> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    const result = await api.createEvent(input, token);
    revalidatePath("/konzerte");
    revalidatePath("/");
    if (result.published && result.event) {
      return { ok: true, data: { published: true, id: result.event.id } };
    }
    revalidatePath("/verwaltung");
    return { ok: true, data: { published: false } };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function updateEventAction(id: number, input: EventInput): Promise<ActionResult<{ id: number }>> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateEvent(id, input, token);
    revalidatePath(`/konzerte/${id}`);
    revalidatePath("/konzerte");
    return { ok: true, data: { id } };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function cancelEventAction(id: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateEventStatus(id, "CANCELLED", token);
    revalidatePath(`/konzerte/${id}`);
    revalidatePath("/konzerte");
    revalidatePath("/");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

/** Undoes an accidental cancellation - back to PUBLISHED, the status every event is
 * created with (see EventService.create). */
export async function reactivateEventAction(id: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateEventStatus(id, "PUBLISHED", token);
    revalidatePath(`/konzerte/${id}`);
    revalidatePath("/konzerte");
    revalidatePath("/");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function deleteEventAction(id: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.deleteEvent(id, token);
    revalidatePath("/konzerte");
    revalidatePath("/");
    revalidatePath("/admin/events");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function toggleSaveEventAction(eventId: number, save: boolean): Promise<ActionResult> {
  const token = await getToken();
  if (!token) redirect("/login");

  try {
    if (save) {
      await api.saveEvent(eventId, token);
    } else {
      await api.unsaveEvent(eventId, token);
    }
    revalidatePath("/");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}
