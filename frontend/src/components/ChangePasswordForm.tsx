"use client";

import { useState } from "react";
import { changePasswordAction } from "@/actions/me";

/** Changing a known password while logged in - no email round-trip like "Passwort
 * vergessen" needs, since typing the current password already proves it's really the
 * account owner (see AuthService.changePassword on the backend). */
export function ChangePasswordForm() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setMessage(null);

    if (newPassword.length < 8) {
      setError("Das neue Passwort muss mindestens 8 Zeichen haben.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Die neuen Passwörter stimmen nicht überein.");
      return;
    }

    setSaving(true);
    const result = await changePasswordAction({ currentPassword, newPassword });
    setSaving(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setMessage("Passwort geändert.");
  }

  return (
    <form onSubmit={onSubmit} className="mt-2 max-w-sm space-y-3 border border-line bg-surface p-4">
      <div>
        <label className="font-meta text-xs uppercase tracking-wide text-muted">Aktuelles Passwort</label>
        <input
          type="password"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
        />
      </div>
      <div>
        <label className="font-meta text-xs uppercase tracking-wide text-muted">Neues Passwort</label>
        <input
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
        />
      </div>
      <div>
        <label className="font-meta text-xs uppercase tracking-wide text-muted">Neues Passwort bestätigen</label>
        <input
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
        />
      </div>
      <button
        type="submit"
        disabled={saving}
        className="w-full bg-fg py-2 text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {saving ? "Wird geändert …" : "Passwort ändern"}
      </button>
      {error && <p className="font-meta text-xs text-accent">{error}</p>}
      {message && <p className="font-meta text-xs text-muted">{message}</p>}
    </form>
  );
}
