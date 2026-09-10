"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { CITY_COOKIE, LAT_COOKIE, LON_COOKIE, RADIUS_COOKIE } from "@/lib/location-cookies";

const RADII = [10, 25, 50];
const COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

export function LocationPicker({
  city,
  radiusKm,
  usingDeviceLocation,
}: {
  city: string | null;
  radiusKm: number | null;
  usingDeviceLocation: boolean;
}) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [cityInput, setCityInput] = useState(city ?? "");
  const [radiusInput, setRadiusInput] = useState(radiusKm ?? 25);
  const [locating, setLocating] = useState(false);
  const [locateError, setLocateError] = useState<string | null>(null);

  function setCookie(name: string, value: string) {
    document.cookie = `${name}=${encodeURIComponent(value)};path=/;max-age=${COOKIE_MAX_AGE}`;
  }

  function clearCookie(name: string) {
    document.cookie = `${name}=;path=/;max-age=0`;
  }

  function applyCity(nextCity: string, nextRadius: number) {
    setCookie(CITY_COOKIE, nextCity.trim());
    setCookie(RADIUS_COOKIE, String(nextRadius));
    // A typed city name replaces any earlier device coordinates, so the two never conflict.
    clearCookie(LAT_COOKIE);
    clearCookie(LON_COOKIE);
    setOpen(false);
    router.refresh();
  }

  function useDeviceLocation() {
    if (!navigator.geolocation) {
      setLocateError("Dieses Gerät unterstützt keine Standortermittlung.");
      return;
    }
    setLocating(true);
    setLocateError(null);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCookie(LAT_COOKIE, String(position.coords.latitude));
        setCookie(LON_COOKIE, String(position.coords.longitude));
        setCookie(RADIUS_COOKIE, String(radiusInput));
        // No geocoding (see README) - a device position has no city name, so
        // an old typed city must be cleared or it'd keep filtering results out.
        clearCookie(CITY_COOKIE);
        setLocating(false);
        setOpen(false);
        router.refresh();
      },
      () => {
        setLocating(false);
        setLocateError("Standort konnte nicht ermittelt werden.");
      }
    );
  }

  const label = usingDeviceLocation
    ? `Aktueller Standort · ${radiusKm ?? 25} km`
    : city
      ? `${city} · ${radiusKm ?? 25} km`
      : "Standort wählen";

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="font-meta text-sm tracking-wide text-muted hover:text-fg transition-colors"
      >
        {label} <span aria-hidden>▾</span>
      </button>

      {open && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            applyCity(cityInput, radiusInput);
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
          <button
            type="button"
            onClick={useDeviceLocation}
            disabled={locating}
            className="w-full border border-line py-2 text-sm font-meta hover:border-fg disabled:opacity-60"
          >
            {locating ? "Standort wird ermittelt …" : "Standort verwenden"}
          </button>
          {locateError && <p className="font-meta text-xs text-accent">{locateError}</p>}
        </form>
      )}
    </div>
  );
}
