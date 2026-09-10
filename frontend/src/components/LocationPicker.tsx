"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { CITY_COOKIE, RADIUS_COOKIE } from "@/lib/location-cookies";

const RADII = [10, 25, 50];

export function LocationPicker({ city, radiusKm }: { city: string | null; radiusKm: number | null }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [cityInput, setCityInput] = useState(city ?? "");
  const [radiusInput, setRadiusInput] = useState(radiusKm ?? 25);

  function apply(nextCity: string, nextRadius: number) {
    const trimmed = nextCity.trim();
    document.cookie = `${CITY_COOKIE}=${encodeURIComponent(trimmed)};path=/;max-age=${60 * 60 * 24 * 365}`;
    document.cookie = `${RADIUS_COOKIE}=${nextRadius};path=/;max-age=${60 * 60 * 24 * 365}`;
    setOpen(false);
    router.refresh();
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="font-meta text-sm tracking-wide text-muted hover:text-fg transition-colors"
      >
        {city ? `${city} · ${radiusKm ?? 25} km` : "Standort wählen"} <span aria-hidden>▾</span>
      </button>

      {open && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            apply(cityInput, radiusInput);
          }}
          className="absolute right-0 z-20 mt-2 w-64 space-y-3 border border-line bg-surface p-4 shadow-lg"
        >
          <div>
            <label className="font-meta text-xs uppercase tracking-wide text-muted">Konzerte rund um</label>
            <input
              autoFocus
              value={cityInput}
              onChange={(e) => setCityInput(e.target.value)}
              placeholder="Hamburg"
              className="mt-1 w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
            />
          </div>
          <div>
            <span className="font-meta text-xs uppercase tracking-wide text-muted">Umkreis</span>
            <div className="mt-1 flex gap-2">
              {RADII.map((r) => (
                <button
                  type="button"
                  key={r}
                  onClick={() => setRadiusInput(r)}
                  className={`flex-1 border px-2 py-1 text-sm font-meta ${
                    radiusInput === r ? "border-accent bg-accent text-accent-fg" : "border-line"
                  }`}
                >
                  {r} km
                </button>
              ))}
            </div>
          </div>
          <button type="submit" className="w-full bg-fg py-2 text-sm text-bg hover:bg-accent hover:text-accent-fg">
            Übernehmen
          </button>
        </form>
      )}
    </div>
  );
}
