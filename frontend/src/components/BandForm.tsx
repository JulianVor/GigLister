"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { updateBandAction, updateBandStatusAction } from "@/actions/bands";
import type { BandResponse, EntityStatus } from "@/lib/types";

const STATUS_OPTIONS: { value: EntityStatus; label: string }[] = [
  { value: "STUB", label: "Stub" },
  { value: "DRAFT", label: "Entwurf" },
  { value: "PUBLISHED", label: "Veröffentlicht" },
  { value: "ARCHIVED", label: "Archiviert" },
];

export function BandForm({ band }: { band: BandResponse }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState(band.name);
  const [city, setCity] = useState(band.city ?? "");
  const [region, setRegion] = useState(band.region ?? "");
  const [country, setCountry] = useState(band.country ?? "");
  const [shortDescription, setShortDescription] = useState(band.shortDescription ?? "");
  const [website, setWebsite] = useState(band.website ?? "");
  const [logoUrl, setLogoUrl] = useState(band.logoUrl ?? "");
  const [titleImageUrl, setTitleImageUrl] = useState(band.titleImageUrl ?? "");
  const [genres, setGenres] = useState(band.genres.join(", "));
  const [status, setStatus] = useState<EntityStatus>(band.status);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    startTransition(async () => {
      const result = await updateBandAction(band.id, {
        name,
        city: city || undefined,
        region: region || undefined,
        country: country || undefined,
        shortDescription: shortDescription || undefined,
        website: website || undefined,
        logoUrl: logoUrl || undefined,
        titleImageUrl: titleImageUrl || undefined,
        genres: genres
          .split(",")
          .map((g) => g.trim())
          .filter(Boolean),
      });
      if (!result.ok) {
        setError(result.error);
        return;
      }
      if (status !== band.status) {
        await updateBandStatusAction(band.id, status);
      }
      router.push(`/bands/${band.id}`);
      router.refresh();
    });
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-xl space-y-4">
      <Field label="Name">
        <input value={name} onChange={(e) => setName(e.target.value)} required className="input" />
      </Field>
      <Field label="Ort">
        <input value={city} onChange={(e) => setCity(e.target.value)} className="input" />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Region">
          <input value={region} onChange={(e) => setRegion(e.target.value)} className="input" />
        </Field>
        <Field label="Land">
          <input value={country} onChange={(e) => setCountry(e.target.value)} className="input" />
        </Field>
      </div>
      <Field label="Kurzbeschreibung">
        <textarea value={shortDescription} onChange={(e) => setShortDescription(e.target.value)} rows={3} className="input" />
      </Field>
      <Field label="Genres (kommagetrennt)">
        <input value={genres} onChange={(e) => setGenres(e.target.value)} className="input" />
      </Field>
      <Field label="Website">
        <input type="url" value={website} onChange={(e) => setWebsite(e.target.value)} className="input" />
      </Field>
      <Field label="Logo-URL">
        <input type="url" value={logoUrl} onChange={(e) => setLogoUrl(e.target.value)} className="input" />
      </Field>
      <Field label="Titelbild-URL">
        <input type="url" value={titleImageUrl} onChange={(e) => setTitleImageUrl(e.target.value)} className="input" />
      </Field>
      <Field label="Status">
        <select value={status} onChange={(e) => setStatus(e.target.value as EntityStatus)} className="input">
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </Field>

      {error && <p className="font-meta text-sm text-accent">{error}</p>}

      <button
        type="submit"
        disabled={pending}
        className="bg-fg px-6 py-2.5 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {pending ? "Wird gespeichert …" : "Speichern"}
      </button>
    </form>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="font-meta text-sm text-muted">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}
