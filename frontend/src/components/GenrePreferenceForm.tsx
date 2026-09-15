"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { updateProfileAction } from "@/actions/me";

/** Feeds the "Das könnte dich interessieren" section on Entdecken (see DiscoverService)
 * alongside genres implicitly derived from followed bands - this is the explicit signal a
 * user can set even before following/merkt-ing anything. */
export function GenrePreferenceForm({ allGenres, initialSelected }: { allGenres: string[]; initialSelected: string[] }) {
  const router = useRouter();
  const [selected, setSelected] = useState<Set<string>>(new Set(initialSelected));
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function toggle(genre: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(genre)) {
        next.delete(genre);
      } else {
        next.add(genre);
      }
      return next;
    });
    setMessage(null);
  }

  async function save() {
    setSaving(true);
    setError(null);
    const result = await updateProfileAction({ preferredGenres: [...selected] });
    setSaving(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setMessage("Gespeichert.");
    router.refresh();
  }

  return (
    <div className="mt-2 max-w-xl space-y-3 border border-line bg-surface p-4">
      <p className="font-meta text-xs text-muted">Für Vorschläge unter &quot;Entdecken&quot; - wähl aus, was dich interessiert.</p>
      <div className="flex flex-wrap gap-2">
        {allGenres.map((genre) => (
          <button
            key={genre}
            type="button"
            onClick={() => toggle(genre)}
            aria-pressed={selected.has(genre)}
            className={`border px-3 py-1.5 font-meta text-sm ${
              selected.has(genre) ? "border-accent bg-accent text-accent-fg" : "border-line hover:border-fg"
            }`}
          >
            {genre}
          </button>
        ))}
      </div>
      <button
        type="button"
        onClick={save}
        disabled={saving}
        className="bg-fg px-6 py-2 text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {saving ? "Wird gespeichert …" : "Speichern"}
      </button>
      {error && <p className="font-meta text-xs text-accent">{error}</p>}
      {message && <p className="font-meta text-xs text-muted">{message}</p>}
    </div>
  );
}
