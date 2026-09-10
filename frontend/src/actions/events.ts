"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import * as api from "@/lib/api";
import { getToken } from "@/lib/session";
import type { EventInput } from "@/lib/api";
import type { ActionResult } from "@/lib/action-result";

export async function createEventAction(input: EventInput): Promise<ActionResult<{ id: number }>> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    const event = await api.createEvent(input, token);
    revalidatePath("/konzerte");
    revalidatePath("/entdecken");
    revalidatePath("/");
    return { ok: true, data: { id: event.id } };
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
    revalidatePath("/mein-giglister");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}
