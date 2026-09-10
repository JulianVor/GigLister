"use client";

import { useActionState, useEffect, useState, useTransition } from "react";
import { registerAction } from "@/actions/auth";
import { checkUsernameAvailable } from "@/actions/username";

export function RegisterForm() {
  const [state, formAction, pending] = useActionState(registerAction, undefined);
  const [username, setUsername] = useState("");
  const [availability, setAvailability] = useState<{ username: string; available: boolean } | null>(null);
  const [isPendingCheck, startTransition] = useTransition();

  useEffect(() => {
    const trimmed = username.trim();
    if (trimmed.length < 3) {
      return;
    }
    const handle = setTimeout(() => {
      startTransition(async () => {
        const available = await checkUsernameAvailable(trimmed);
        setAvailability({ username: trimmed, available });
      });
    }, 400);
    return () => clearTimeout(handle);
  }, [username]);

  const trimmedUsername = username.trim();
  const hasResultForCurrent = availability?.username === trimmedUsername;
  const showChecking = trimmedUsername.length >= 3 && (isPendingCheck || !hasResultForCurrent);
  const showAvailable = hasResultForCurrent && availability!.available;
  const showTaken = hasResultForCurrent && !availability!.available;

  if (state?.success) {
    return (
      <div className="max-w-sm border border-line p-4">
        <p className="font-display text-lg">Fast geschafft.</p>
        <p className="mt-2 font-meta text-sm text-muted">
          Wir haben eine Bestätigungsmail an <strong className="text-fg">{state.email}</strong> geschickt. Bitte
          klicke auf den Link darin, um dein Konto zu aktivieren.
        </p>
      </div>
    );
  }

  return (
    <form action={formAction} className="max-w-sm space-y-4">
      <div>
        <label className="font-meta text-sm text-muted" htmlFor="username">
          Nutzername
        </label>
        <input
          id="username"
          name="username"
          type="text"
          required
          minLength={3}
          maxLength={30}
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
        {showChecking && <p className="mt-1 font-meta text-xs text-muted">Wird geprüft …</p>}
        {showAvailable && <p className="mt-1 font-meta text-xs text-accent">Verfügbar.</p>}
        {showTaken && <p className="mt-1 font-meta text-xs text-accent">Dieser Nutzername ist schon vergeben.</p>}
      </div>
      <div>
        <label className="font-meta text-sm text-muted" htmlFor="email">
          E-Mail
        </label>
        <input
          id="email"
          name="email"
          type="email"
          required
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
      </div>
      <div>
        <label className="font-meta text-sm text-muted" htmlFor="password">
          Passwort
        </label>
        <input
          id="password"
          name="password"
          type="password"
          required
          minLength={8}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
        <p className="mt-1 font-meta text-xs text-muted">Mindestens 8 Zeichen.</p>
      </div>

      {state?.error && <p className="font-meta text-sm text-accent">{state.error}</p>}

      <button
        type="submit"
        disabled={pending || showTaken}
        className="w-full bg-fg py-2.5 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {pending ? "Konto wird erstellt …" : "Konto erstellen"}
      </button>
    </form>
  );
}
