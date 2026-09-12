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

export async function promoteUserAction(userId: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.promoteUser(userId, token);
    revalidatePath("/admin/users");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function demoteUserAction(userId: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.demoteUser(userId, token);
    revalidatePath("/admin/users");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function geocodeMissingLocationsAction(): Promise<ActionResult<{ attempted: number; resolved: number }>> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    const result = await api.geocodeMissingLocations(token);
    revalidatePath("/admin/locations");
    return { ok: true, data: result };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function updateSubmissionAction(
  id: number,
  payload: Record<string, unknown>,
  imageUrl?: string
): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateSubmission(id, { payload, imageUrl }, token);
    revalidatePath("/admin/submissions");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function approveSubmissionAction(id: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.approveSubmission(id, token);
    revalidatePath("/admin/submissions");
    revalidatePath("/admin");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function rejectSubmissionAction(id: number, reason?: string): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.rejectSubmission(id, reason, token);
    revalidatePath("/admin/submissions");
    revalidatePath("/admin");
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}
