"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { updateProfileAction } from "@/actions/me";
import { geocodeCity } from "@/lib/geocode";
import { CITY_COOKIE, LAT_COOKIE, LON_COOKIE, RADIUS_COOKIE } from "@/lib/location-cookies";

const RADII = [10, 25, 50];
const COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

/** Lets a logged-in user save a default location on their profile - the same city+radius
 * (or device position) picker as the header's "Standort wählen", just persisted server
 * side so it comes back automatically on their next login (see loginAction) instead of
 * needing to be re-picked every session. Also applies it to this browser's own location
 * cookies right away on save, so the change is visible immediately without a fresh login. */
export function ProfileLocationForm({ homeCity, radiusKm }: { homeCity: string | null; radiusKm: number | null }) {
  const router = useRouter();
  const [cityInput, setCityInput] = useState(homeCity ?? "");
  const [radiusInput, setRadiusInput] = useState(radiusKm ?? 25);
  const [saving, setSaving] = useState(false);
  const [locating, setLocating] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function setCookie(name: string, value: string) {
    document.cookie = `${name}=${encodeURIComponent(value)};path=/;max-age=${COOKIE_MAX_AGE}`;
  }
  function clearCookie(name: string) {
    document.cookie = `${name}=;path=/;max-age=0`;
  }

  async function save(city: string, lat?: number, lon?: number) {
    setSaving(true);
    setError(null);
    setMessage(null);
    const result = await updateProfileAction({ homeCity: city, homeLatitude: lat, homeLongitude: lon, radiusKm: radiusInput });
    setSaving(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setCookie(CITY_COOKIE, city);
    setCookie(RADIUS_COOKIE, String(radiusInput));
    if (lat != null && lon != null) {
      setCookie(LAT_COOKIE, String(lat));
      setCookie(LON_COOKIE, String(lon));
    } else {
      clearCookie(LAT_COOKIE);
      clearCookie(LON_COOKIE);
    }
    setMessage("Gespeichert.");
    router.refresh();
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = cityInput.trim();
    setSaving(true);
    const coords = trimmed ? await geocodeCity(trimmed) : null;
    setSaving(false);
    await save(trimmed, coords?.lat, coords?.lon);
  }

  function useDeviceLocation() {
    if (!navigator.geolocation) {
      setError("Dieses Gerät unterstützt keine Standortermittlung.");
      return;
    }
    setLocating(true);
    setError(null);
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        setLocating(false);
        setCityInput("");
        await save("", position.coords.latitude, position.coords.longitude);
      },
      () => {
        setLocating(false);
        setError("Standort konnte nicht ermittelt werden.");
      }
    );
  }

  return (
    <form onSubmit={onSubmit} className="mt-2 max-w-sm space-y-3 border border-line bg-surface p-4">
      <div>
        <label className="font-meta text-xs uppercase tracking-wide text-muted">Stadt</label>
        <input
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
      <button
        type="submit"
        disabled={saving}
        className="w-full bg-fg py-2 text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {saving ? "Wird gespeichert …" : "Speichern"}
      </button>
      <button
        type="button"
        onClick={useDeviceLocation}
        disabled={locating || saving}
        className="w-full border border-line py-2 text-sm font-meta hover:border-fg disabled:opacity-60"
      >
        {locating ? "Standort wird ermittelt …" : "Aktuellen Standort verwenden"}
      </button>
      {error && <p className="font-meta text-xs text-accent">{error}</p>}
      {message && <p className="font-meta text-xs text-muted">{message}</p>}
    </form>
  );
}
