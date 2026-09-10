"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import * as api from "@/lib/api";
import { getToken } from "@/lib/session";
import type { BandInput } from "@/lib/api";
import type { EntityStatus, PermissionLevel } from "@/lib/types";
import type { ActionResult } from "@/lib/action-result";

export async function createBandAction(input: BandInput): Promise<ActionResult<{ id: number }>> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    const band = await api.createBand(input, token);
    revalidatePath("/bands");
    return { ok: true, data: { id: band.id } };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function updateBandAction(id: number, input: BandInput): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateBand(id, input, token);
    revalidatePath(`/bands/${id}`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function updateBandStatusAction(id: number, status: EntityStatus): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateBandStatus(id, status, token);
    revalidatePath(`/bands/${id}`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function toggleFollowBandAction(bandId: number, follow: boolean): Promise<ActionResult> {
  const token = await getToken();
  if (!token) redirect("/login");

  try {
    if (follow) {
      await api.followBand(bandId, token);
    } else {
      await api.unfollowBand(bandId, token);
    }
    revalidatePath("/mein-giglister");
    revalidatePath(`/bands/${bandId}`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function claimBandAction(bandId: number, message?: string): Promise<ActionResult> {
  const token = await getToken();
  if (!token) redirect("/login");

  try {
    await api.claimBand(bandId, message, token);
    revalidatePath(`/bands/${bandId}`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function grantBandPermissionAction(
  bandId: number,
  userId: number,
  permission: PermissionLevel
): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.grantBandPermission(bandId, userId, permission, token);
    revalidatePath(`/bands/${bandId}/bearbeiten`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function revokeBandPermissionAction(bandId: number, userId: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.revokeBandPermission(bandId, userId, token);
    revalidatePath(`/bands/${bandId}/bearbeiten`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}
