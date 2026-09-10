"use server";

import { revalidatePath } from "next/cache";
import * as api from "@/lib/api";
import { getToken } from "@/lib/session";
import type { LocationInput } from "@/lib/api";
import type { EntityStatus, PermissionLevel } from "@/lib/types";
import type { ActionResult } from "@/lib/action-result";

export async function createLocationAction(input: LocationInput): Promise<ActionResult<{ id: number }>> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    const location = await api.createLocation(input, token);
    revalidatePath("/orte");
    return { ok: true, data: { id: location.id } };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function updateLocationAction(id: number, input: LocationInput): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateLocation(id, input, token);
    revalidatePath(`/orte/${id}`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function updateLocationStatusAction(id: number, status: EntityStatus): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.updateLocationStatus(id, status, token);
    revalidatePath(`/orte/${id}`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function claimLocationAction(locationId: number, message?: string): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.claimLocation(locationId, message, token);
    revalidatePath(`/orte/${locationId}`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function grantLocationPermissionAction(
  locationId: number,
  userId: number,
  permission: PermissionLevel
): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.grantLocationPermission(locationId, userId, permission, token);
    revalidatePath(`/orte/${locationId}/bearbeiten`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}

export async function revokeLocationPermissionAction(locationId: number, userId: number): Promise<ActionResult> {
  const token = await getToken();
  if (!token) return { ok: false, error: "Bitte zuerst einloggen." };

  try {
    await api.revokeLocationPermission(locationId, userId, token);
    revalidatePath(`/orte/${locationId}/bearbeiten`);
    return { ok: true, data: undefined };
  } catch (err) {
    if (err instanceof api.ApiError) return { ok: false, error: err.message };
    throw err;
  }
}
