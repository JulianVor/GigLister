"use server";

import { revalidatePath } from "next/cache";
import * as api from "@/lib/api";
import { getToken } from "@/lib/session";
import type { ActionResult } from "@/lib/action-result";

export async function updateProfileAction(input: {
  displayName?: string;
  homeCity?: string;
  homeLatitude?: number;
  homeLongitude?: number;
  radiusKm?: number;
}): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateProfile(input, token);
    revalidatePath("/mein-giglister");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}
