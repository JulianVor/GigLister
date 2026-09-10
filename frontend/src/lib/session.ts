import "server-only";
import { cookies } from "next/headers";
import { ApiError, getMe } from "./api";
import type { MeResponse } from "./types";

export const TOKEN_COOKIE = "giglister_token";

export async function getToken(): Promise<string | undefined> {
  const store = await cookies();
  return store.get(TOKEN_COOKIE)?.value;
}

/** The current user, or null if not logged in / the session is no longer valid. */
export async function getSession(): Promise<MeResponse | null> {
  const token = await getToken();
  if (!token) {
    return null;
  }
  try {
    return await getMe(token);
  } catch (err) {
    if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
      return null;
    }
    throw err;
  }
}
