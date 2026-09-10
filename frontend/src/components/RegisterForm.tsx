"use client";

import { useActionState } from "react";
import { registerAction } from "@/actions/auth";

export function RegisterForm() {
  const [state, formAction, pending] = useActionState(registerAction, undefined);

  return (
    <form action={formAction} className="max-w-sm space-y-4">
      <div>
        <label className="font-meta text-sm text-muted" htmlFor="displayName">
          Name
        </label>
        <input
          id="displayName"
          name="displayName"
          type="text"
          required
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
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
        disabled={pending}
        className="w-full bg-fg py-2.5 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {pending ? "Konto wird erstellt …" : "Konto erstellen"}
      </button>
    </form>
  );
}
