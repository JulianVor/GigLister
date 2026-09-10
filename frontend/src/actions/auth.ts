"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import {
  ApiError,
  forgotPassword as apiForgotPassword,
  login as apiLogin,
  register as apiRegister,
  resetPassword as apiResetPassword,
} from "@/lib/api";
import { TOKEN_COOKIE } from "@/lib/session";

export type AuthFormState = { error?: string } | undefined;
export type RegisterFormState = { error?: string; success?: boolean; email?: string } | undefined;
export type ForgotPasswordFormState = { error?: string; message?: string } | undefined;
export type ResetPasswordFormState = { error?: string } | undefined;

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

export async function registerAction(_prevState: RegisterFormState, formData: FormData): Promise<RegisterFormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  const username = String(formData.get("username") ?? "").trim();

  if (!email || !password || !username) {
    return { error: "Bitte alle Felder ausfüllen." };
  }
  if (password.length < 8) {
    return { error: "Das Passwort muss mindestens 8 Zeichen haben." };
  }

  try {
    await apiRegister({ email, password, username });
  } catch (err) {
    if (err instanceof ApiError) {
      return { error: err.message };
    }
    throw err;
  }

  return { success: true, email };
}

export async function forgotPasswordAction(
  _prevState: ForgotPasswordFormState,
  formData: FormData
): Promise<ForgotPasswordFormState> {
  const email = String(formData.get("email") ?? "").trim();
  if (!email) {
    return { error: "Bitte eine E-Mail-Adresse angeben." };
  }

  try {
    const res = await apiForgotPassword(email);
    return { message: res.message };
  } catch (err) {
    if (err instanceof ApiError) {
      return { error: err.message };
    }
    throw err;
  }
}

export async function resetPasswordAction(
  _prevState: ResetPasswordFormState,
  formData: FormData
): Promise<ResetPasswordFormState> {
  const token = String(formData.get("token") ?? "");
  const newPassword = String(formData.get("newPassword") ?? "");
  const confirmPassword = String(formData.get("confirmPassword") ?? "");

  if (!token) {
    return { error: "Ungültiger Link zum Zurücksetzen des Passworts." };
  }
  if (newPassword.length < 8) {
    return { error: "Das Passwort muss mindestens 8 Zeichen haben." };
  }
  if (newPassword !== confirmPassword) {
    return { error: "Die Passwörter stimmen nicht überein." };
  }

  try {
    const res = await apiResetPassword(token, newPassword);
    await setSessionCookie(res.token);
  } catch (err) {
    if (err instanceof ApiError) {
      return { error: err.message };
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
