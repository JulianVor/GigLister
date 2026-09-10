"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { updateLocationAction, updateLocationStatusAction } from "@/actions/locations";
import { ENTITY_STATUS_LABELS } from "@/lib/status-labels";
import type { EntityStatus, LocationResponse } from "@/lib/types";

const STATUS_OPTIONS: { value: EntityStatus; label: string }[] = (
  Object.keys(ENTITY_STATUS_LABELS) as EntityStatus[]
).map((value) => ({ value, label: ENTITY_STATUS_LABELS[value] }));

export function LocationForm({ location }: { location: LocationResponse }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState(location.name);
  const [city, setCity] = useState(location.city);
  const [address, setAddress] = useState(location.address ?? "");
  const [postalCode, setPostalCode] = useState(location.postalCode ?? "");
  const [country, setCountry] = useState(location.country ?? "");
  const [website, setWebsite] = useState(location.website ?? "");
  const [logoUrl, setLogoUrl] = useState(location.logoUrl ?? "");
  const [titleImageUrl, setTitleImageUrl] = useState(location.titleImageUrl ?? "");
  const [status, setStatus] = useState<EntityStatus>(location.status);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    startTransition(async () => {
      const result = await updateLocationAction(location.id, {
        name,
        city,
        address: address || undefined,
        postalCode: postalCode || undefined,
        country: country || undefined,
        website: website || undefined,
        logoUrl: logoUrl || undefined,
        titleImageUrl: titleImageUrl || undefined,
        latitude: location.latitude ?? undefined,
        longitude: location.longitude ?? undefined,
      });
      if (!result.ok) {
        setError(result.error);
        return;
      }
      if (status !== location.status) {
        await updateLocationStatusAction(location.id, status);
      }
      router.push(`/orte/${location.id}`);
      router.refresh();
    });
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-xl space-y-4">
      <Field label="Name">
        <input value={name} onChange={(e) => setName(e.target.value)} required className="input" />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Adresse">
          <input value={address} onChange={(e) => setAddress(e.target.value)} className="input" />
        </Field>
        <Field label="PLZ">
          <input value={postalCode} onChange={(e) => setPostalCode(e.target.value)} className="input" />
        </Field>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Ort">
          <input value={city} onChange={(e) => setCity(e.target.value)} required className="input" />
        </Field>
        <Field label="Land">
          <input value={country} onChange={(e) => setCountry(e.target.value)} className="input" />
        </Field>
      </div>
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
