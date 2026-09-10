"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { ApiError, login as apiLogin, register as apiRegister } from "@/lib/api";
import { TOKEN_COOKIE } from "@/lib/session";

export type AuthFormState = { error?: string } | undefined;

async function setSessionCookie(token: string) {
  const store = await cookies();
  store.set(TOKEN_COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60 * 24, // matches the backend's 24h token expiry
  });
}

export async function loginAction(_prevState: AuthFormState, formData: FormData): Promise<AuthFormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  if (!email || !password) {
    return { error: "Bitte E-Mail und Passwort angeben." };
  }

  try {
    const res = await apiLogin({ email, password });
    await setSessionCookie(res.token);
  } catch (err) {
    if (err instanceof ApiError) {
      return { error: err.status === 401 ? "E-Mail oder Passwort ist falsch." : err.message };
    }
    throw err;
  }

  redirect("/mein-giglister");
}

export async function registerAction(_prevState: AuthFormState, formData: FormData): Promise<AuthFormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  const displayName = String(formData.get("displayName") ?? "").trim();

  if (!email || !password || !displayName) {
    return { error: "Bitte alle Felder ausfüllen." };
  }
  if (password.length < 8) {
    return { error: "Das Passwort muss mindestens 8 Zeichen haben." };
  }

  try {
    const res = await apiRegister({ email, password, displayName });
    await setSessionCookie(res.token);
  } catch (err) {
    if (err instanceof ApiError) {
      return { error: err.status === 409 ? "Für diese E-Mail existiert bereits ein Konto." : err.message };
    }
    throw err;
  }

  redirect("/mein-giglister");
}

export async function logoutAction() {
  const store = await cookies();
  store.delete(TOKEN_COOKIE);
  redirect("/");
}
