"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createUserAction } from "@/actions/admin";

/** Invite-style user creation: no password field - one is generated and shown here exactly
 * once (never retrievable again afterward), for the admin to pass along however they reach
 * this person. The new account must replace it before it can do anything else (see
 * RequirePasswordChange). */
export function CreateUserForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<{ username: string; temporaryPassword: string } | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    setSaving(true);
    const result = await createUserAction({ email, username });
    setSaving(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setCreated({ username: result.data.username, temporaryPassword: result.data.temporaryPassword });
    setEmail("");
    setUsername("");
    router.refresh();
  }

  return (
    <div className="border border-line bg-surface p-4">
      <h2 className="font-display text-lg">Nutzer anlegen</h2>
      <p className="mt-1 font-meta text-sm text-muted">
        Legt ein Konto mit einem temporären Passwort an - der neue Nutzer muss es nach dem ersten Login ändern,
        bevor er irgendetwas anderes tun kann.
      </p>

      <form onSubmit={onSubmit} className="mt-4 flex max-w-lg flex-wrap items-end gap-3">
        <label className="flex-1 basis-48">
          <span className="font-meta text-xs uppercase tracking-wide text-muted">E-Mail</span>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
          />
        </label>
        <label className="flex-1 basis-48">
          <span className="font-meta text-xs uppercase tracking-wide text-muted">Nutzername</span>
          <input
            type="text"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="mt-1 w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
          />
        </label>
        <button
          type="submit"
          disabled={saving}
          className="bg-fg px-4 py-2 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
        >
          {saving ? "Wird angelegt …" : "Anlegen"}
        </button>
      </form>

      {error && <p className="mt-2 font-meta text-sm text-accent">{error}</p>}

      {created && (
        <div className="mt-4 border border-accent bg-bg p-3">
          <p className="font-meta text-sm">
            Konto für <strong>{created.username}</strong> angelegt. Temporäres Passwort (wird nur jetzt angezeigt):
          </p>
          <p className="mt-2 select-all border border-line bg-surface px-3 py-2 font-mono text-sm">
            {created.temporaryPassword}
          </p>
          <p className="mt-2 font-meta text-xs text-muted">
            Gib das Passwort dem Nutzer weiter — es lässt sich danach nicht mehr abrufen.
          </p>
        </div>
      )}
    </div>
  );
}
