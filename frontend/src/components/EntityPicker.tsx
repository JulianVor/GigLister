"use client";

import { useEffect, useState, useTransition } from "react";
import type { DuplicateCandidate, EntityType } from "@/lib/types";

const PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function fetchDuplicates(entityType: EntityType, name: string, city: string): Promise<DuplicateCandidate[]> {
  if (!name.trim()) return [];
  const path = entityType === "BAND" ? "/api/bands/duplicates" : "/api/locations/duplicates";
  const params = new URLSearchParams({ name });
  if (city) params.set("city", city);
  const res = await fetch(`${PUBLIC_API_URL}${path}?${params.toString()}`);
  if (!res.ok) return [];
  return res.json();
}

export interface EntityPickerValue {
  id?: number;
  name: string;
  city: string;
  address?: string;
}

/**
 * Picks an existing Band/Location by name, or falls back to creating a new
 * STUB — surfacing "Meintest du?" duplicate suggestions first (concept §31-33).
 */
export function EntityPicker({
  entityType,
  label,
  value,
  onChange,
  showAddress,
  cityRequired,
}: {
  entityType: EntityType;
  label: string;
  value: EntityPickerValue;
  onChange: (v: EntityPickerValue) => void;
  showAddress?: boolean;
  cityRequired?: boolean;
}) {
  const [candidates, setCandidates] = useState<DuplicateCandidate[]>([]);
  const [, startTransition] = useTransition();

  useEffect(() => {
    if (value.id || !value.name.trim()) {
      return;
    }
    const handle = setTimeout(() => {
      startTransition(async () => {
        const results = await fetchDuplicates(entityType, value.name, value.city);
        setCandidates(results);
      });
    }, 300);
    return () => clearTimeout(handle);
  }, [value.name, value.city, value.id, entityType]);

  const visibleCandidates = value.id || !value.name.trim() ? [] : candidates;

  return (
    <div className="space-y-2 border border-line p-3">
      <label className="font-meta text-xs uppercase tracking-wide text-muted">{label}</label>
      <input
        value={value.name}
        onChange={(e) => onChange({ ...value, id: undefined, name: e.target.value })}
        placeholder="Name"
        required
        className="w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
      />

      {visibleCandidates.length > 0 && (
        <div className="border border-line bg-surface p-2">
          <p className="font-meta text-xs uppercase tracking-wide text-muted">Meintest du?</p>
          <ul className="mt-1 space-y-1">
            {visibleCandidates.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  onClick={() => {
                    onChange({ id: c.id, name: c.name, city: c.city ?? "" });
                    setCandidates([]);
                  }}
                  className="text-left font-meta text-sm text-accent hover:underline"
                >
                  {c.name}
                  {c.city ? ` · ${c.city}` : ""}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {!value.id && (
        <div className="grid grid-cols-2 gap-2">
          <input
            value={value.city}
            onChange={(e) => onChange({ ...value, city: e.target.value })}
            placeholder="Stadt"
            required={cityRequired}
            className="border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
          />
          {showAddress && (
            <input
              value={value.address ?? ""}
              onChange={(e) => onChange({ ...value, address: e.target.value })}
              placeholder="Adresse (optional)"
              className="border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
            />
          )}
        </div>
      )}

      {value.id && (
        <p className="font-meta text-xs text-muted">
          Bestehend ausgewählt: <strong className="text-fg">{value.name}</strong>{" "}
          <button
            type="button"
            onClick={() => onChange({ id: undefined, name: "", city: "" })}
            className="text-accent hover:underline"
          >
            Ändern
          </button>
        </p>
      )}
    </div>
  );
}

export function toEntityRef(v: EntityPickerValue) {
  return v.id ? { id: v.id } : { name: v.name, city: v.city || undefined, address: v.address || undefined };
}
