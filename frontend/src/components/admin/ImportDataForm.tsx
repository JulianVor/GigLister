"use client";

import { useRef, useState, useTransition } from "react";
import { useRouter } from "next/navigation";

/** Has to match DataTransferService.CONFIRMATION_PHRASE exactly - the backend re-checks
 * this itself too (never trusts this form alone), but matching it here means a typo shows
 * up immediately as a disabled button instead of a 400 after picking the file. */
const CONFIRMATION_PHRASE = "ALLE DATEN ERSETZEN";

/** "Alle Daten importieren" - replaces every row in every table with what's in the chosen
 * file (see DataTransferService.importAll), so this is deliberately harder to trigger by
 * accident than every other admin action: the button only unlocks once the exact
 * confirmation phrase is typed, matching the same phrase the backend itself requires. */
export function ImportDataForm() {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [confirmText, setConfirmText] = useState("");
  const [pending, startTransition] = useTransition();
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = confirmText === CONFIRMATION_PHRASE;

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const file = fileInputRef.current?.files?.[0];
    if (!file || !canSubmit) return;

    setError(null);
    setResult(null);
    startTransition(async () => {
      let data: unknown;
      try {
        data = JSON.parse(await file.text());
      } catch {
        setError("Die Datei ist kein gültiges JSON.");
        return;
      }

      const res = await fetch("/admin/data/import", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ confirm: confirmText, data }),
      });
      const body = await res.json().catch(() => null);
      if (!res.ok) {
        setError(body?.message ?? `Import fehlgeschlagen (${res.status}).`);
        return;
      }
      setResult(`${body.rowsImported} Zeilen über ${body.tablesReplaced} Tabellen importiert.`);
      setConfirmText("");
      if (fileInputRef.current) fileInputRef.current.value = "";
      router.refresh();
    });
  }

  return (
    <form onSubmit={onSubmit} className="mt-2 max-w-lg space-y-3 border border-accent bg-surface p-4">
      <p className="font-meta text-sm text-accent">
        Ersetzt ALLE Daten in der Datenbank durch den Inhalt der Datei - nichts wird zusammengeführt, alles
        Bestehende geht unwiderruflich verloren.
      </p>
      <div>
        <label className="font-meta text-xs uppercase tracking-wide text-muted">Export-Datei</label>
        <input
          ref={fileInputRef}
          type="file"
          accept="application/json"
          className="mt-1 w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
        />
      </div>
      <div>
        <label className="font-meta text-xs uppercase tracking-wide text-muted">
          Zum Bestätigen &bdquo;{CONFIRMATION_PHRASE}&ldquo; eintippen
        </label>
        <input
          value={confirmText}
          onChange={(e) => setConfirmText(e.target.value)}
          placeholder={CONFIRMATION_PHRASE}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
        />
      </div>
      <button
        type="submit"
        disabled={!canSubmit || pending}
        className="w-full bg-accent py-2 text-sm text-accent-fg hover:opacity-90 disabled:opacity-40"
      >
        {pending ? "Wird importiert …" : "Alle Daten ersetzen"}
      </button>
      {error && <p className="font-meta text-xs text-accent">{error}</p>}
      {result && <p className="font-meta text-xs text-muted">{result}</p>}
    </form>
  );
}
