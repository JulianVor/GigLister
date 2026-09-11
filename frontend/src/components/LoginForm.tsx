"use client";

import { useActionState } from "react";
import { loginAction } from "@/actions/auth";

export function LoginForm() {
  const [state, formAction, pending] = useActionState(loginAction, undefined);

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
          autoComplete="username"
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
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
      </div>

      {state?.error && <p className="font-meta text-sm text-accent">{state.error}</p>}

      <button
        type="submit"
        disabled={pending}
        className="w-full bg-fg py-2.5 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {pending ? "Einloggen …" : "Einloggen"}
      </button>
    </form>
  );
}
