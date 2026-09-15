"use server";

import { revalidatePath } from "next/cache";
import * as api from "@/lib/api";
import { getToken } from "@/lib/session";
import type { EventSeriesInput } from "@/lib/api";
import type { ActionResult } from "@/lib/action-result";

export async function createEventSeriesAction(input: EventSeriesInput): Promise<ActionResult<{ id: number }>> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    const series = await api.createEventSeries(input, token);
    return { ok: true, data: { id: series.id } };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function updateEventSeriesAction(id: number, input: EventSeriesInput): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateEventSeries(id, input, token);
    revalidatePath(`/festivals/${id}`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}
