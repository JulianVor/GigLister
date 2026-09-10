"use server";

import { revalidatePath } from "next/cache";
import * as api from "@/lib/api";
import { getToken } from "@/lib/session";
import type { EntityType } from "@/lib/types";
import type { ActionResult } from "@/lib/action-result";

export async function approveClaimAction(claimId: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.approveClaim(claimId, token);
    revalidatePath("/admin/claims");
    revalidatePath("/admin");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function rejectClaimAction(claimId: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.rejectClaim(claimId, token);
    revalidatePath("/admin/claims");
    revalidatePath("/admin");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function mergeEntitiesAction(
  entityType: EntityType,
  sourceEntityId: number,
  targetEntityId: number
): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.mergeEntities({ entityType, sourceEntityId, targetEntityId }, token);
    revalidatePath("/admin/duplicates");
    revalidatePath("/admin");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}
