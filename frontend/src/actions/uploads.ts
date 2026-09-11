"use server";

import * as api from "@/lib/api";
import { getToken } from "@/lib/session";
import type { ActionResult } from "@/lib/action-result";

export async function uploadImageAction(formData: FormData): Promise<ActionResult<{ url: string }>> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) {
    return { ok: false, error: "Bitte eine Datei auswählen." };
  }

  try {
    const data = await api.uploadImage(file, token);
    return { ok: true, data };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}
