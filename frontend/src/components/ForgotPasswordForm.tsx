"use client";

import { useActionState } from "react";
import { forgotPasswordAction } from "@/actions/auth";

export function ForgotPasswordForm() {
  const [state, formAction, pending] = useActionState(forgotPasswordAction, undefined);

  if (state?.message) {
    return (
      <div className="max-w-sm border border-line p-4">
        <p className="font-meta text-sm text-muted">{state.message}</p>
      </div>
    );
  }

  return (
    <form action={formAction} className="max-w-sm space-y-4">
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

      {state?.error && <p className="font-meta text-sm text-accent">{state.error}</p>}

      <button
        type="submit"
        disabled={pending}
        className="w-full bg-fg py-2.5 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {pending ? "Wird gesendet …" : "Link zum Zurücksetzen senden"}
      </button>
    </form>
  );
}
