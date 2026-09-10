"use server";

import * as api from "@/lib/api";

export async function checkUsernameAvailable(username: string): Promise<boolean> {
  const res = await api.usernameAvailable(username);
  return res.available;
}
