"use client";

import { useActionState } from "react";
import { resetPasswordAction } from "@/actions/auth";

export function ResetPasswordForm({ token }: { token: string }) {
  const [state, formAction, pending] = useActionState(resetPasswordAction, undefined);

  return (
    <form action={formAction} className="max-w-sm space-y-4">
      <input type="hidden" name="token" value={token} />
      <div>
        <label className="font-meta text-sm text-muted" htmlFor="newPassword">
          Neues Passwort
        </label>
        <input
          id="newPassword"
          name="newPassword"
          type="password"
          required
          minLength={8}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
        <p className="mt-1 font-meta text-xs text-muted">Mindestens 8 Zeichen.</p>
      </div>
      <div>
        <label className="font-meta text-sm text-muted" htmlFor="confirmPassword">
          Neues Passwort bestätigen
        </label>
        <input
          id="confirmPassword"
          name="confirmPassword"
          type="password"
          required
          minLength={8}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
      </div>

      {state?.error && <p className="font-meta text-sm text-accent">{state.error}</p>}

      <button
        type="submit"
        disabled={pending}
        className="w-full bg-fg py-2.5 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {pending ? "Wird gespeichert …" : "Passwort speichern"}
      </button>
    </form>
  );
}
