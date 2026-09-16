"use client";

import { useRouter } from "next/navigation";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { CITY_COOKIE, LAT_COOKIE, LON_COOKIE, RADIUS_COOKIE } from "@/lib/location-cookies";
import { geocodeCity } from "@/lib/geocode";

const RADII = [10, 25, 50];
const COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

/** The header's right-hand controls wrap onto their own row on narrow screens, and land
 * flush against the LEFT edge there (a lone flex item on a `justify-between` line has
 * nothing to space itself against) - nowhere near this button's desktop position on the
 * right. A plain `absolute right-0` dropdown anchored to the button's own tightly-fitted
 * wrapper therefore comes out far enough left of the button to run off the left edge of
 * the viewport. Since the button can land anywhere from flush-left to flush-right
 * depending on viewport width and login state (more/fewer sibling controls), a fixed
 * Tailwind breakpoint can't reliably tell "wrapped" from "not" - so this measures the
 * button's actual position after opening and clamps the panel to stay fully on-screen. */
const PANEL_WIDTH = 256;
const VIEWPORT_MARGIN = 16;

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
  const anchorRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [panelStyle, setPanelStyle] = useState<{ top: number; left: number; width: number } | null>(null);
  const [cityInput, setCityInput] = useState(city ?? "");
  const [radiusInput, setRadiusInput] = useState(radiusKm ?? 25);
  const [locating, setLocating] = useState(false);
  const [locateError, setLocateError] = useState<string | null>(null);
  const [geocoding, setGeocoding] = useState(false);

  useLayoutEffect(() => {
    if (!open || !anchorRef.current) return;
    const rect = anchorRef.current.getBoundingClientRect();
    const width = Math.min(PANEL_WIDTH, window.innerWidth - VIEWPORT_MARGIN * 2);
    const maxLeft = window.innerWidth - width - VIEWPORT_MARGIN;
    const left = Math.min(Math.max(rect.right - width, VIEWPORT_MARGIN), Math.max(maxLeft, VIEWPORT_MARGIN));
    setPanelStyle({ top: rect.bottom + 8, left, width });
  }, [open]);

  // The panel is `position: fixed`, computed once against the button's position at the
  // moment it opens - it doesn't track the page afterwards. Left open, a scroll leaves it
  // stranded at that same screen spot while the button (and everything else) moves away
  // underneath it, floating disconnected wherever the page happened to scroll to. Closing
  // on any scroll, and on a click outside the button/panel (both live under anchorRef, so
  // one `contains` check covers both), avoids that instead of trying to keep it glued to
  // the button.
  useEffect(() => {
    if (!open) return;
    function handlePointerDown(e: MouseEvent) {
      if (anchorRef.current && !anchorRef.current.contains(e.target as Node)) setOpen(false);
    }
    function handleScroll() {
      setOpen(false);
    }
    document.addEventListener("mousedown", handlePointerDown);
    window.addEventListener("scroll", handleScroll, { capture: true, passive: true });
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      window.removeEventListener("scroll", handleScroll, { capture: true });
    };
  }, [open]);

  function setCookie(name: string, value: string) {
    document.cookie = `${name}=${encodeURIComponent(value)};path=/;max-age=${COOKIE_MAX_AGE}`;
  }

  function clearCookie(name: string) {
    document.cookie = `${name}=;path=/;max-age=0`;
  }

  async function applyCity(nextCity: string, nextRadius: number) {
    const trimmed = nextCity.trim();
    setCookie(CITY_COOKIE, trimmed);
    setCookie(RADIUS_COOKIE, String(nextRadius));
    setGeocoding(true);
    // Resolves the typed name to real coordinates (e.g. for the Orte map) - best-effort,
    // so a failed/offline lookup still leaves the plain text filter working exactly like
    // it always did, just without a real radius/map center until it succeeds.
    const coords = await geocodeCity(trimmed);
    if (coords) {
      setCookie(LAT_COOKIE, String(coords.lat));
      setCookie(LON_COOKIE, String(coords.lon));
    } else {
      clearCookie(LAT_COOKIE);
      clearCookie(LON_COOKIE);
    }
    setGeocoding(false);
    setOpen(false);
    router.refresh();
  }

  function resetLocation() {
    clearCookie(CITY_COOKIE);
    clearCookie(LAT_COOKIE);
    clearCookie(LON_COOKIE);
    clearCookie(RADIUS_COOKIE);
    setCityInput("");
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
    <div ref={anchorRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="font-meta text-sm tracking-wide text-muted hover:text-fg transition-colors"
      >
        {label} <span aria-hidden>▾</span>
      </button>

      {open && panelStyle && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void applyCity(cityInput, radiusInput);
          }}
          style={{ top: panelStyle.top, left: panelStyle.left, width: panelStyle.width }}
          // Leaflet's own panes/controls (see ConcertMap) go up to z-index 1000 without
          // creating their own stacking context, so anything meant to float above a map
          // anywhere on the page - not just Tailwind's z-50 default max - has to clear that.
          className="fixed z-[1010] space-y-3 border border-line bg-surface p-4 shadow-lg"
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
          <button
            type="submit"
            disabled={geocoding}
            className="w-full bg-fg py-2 text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
          >
            {geocoding ? "Wird gesucht …" : "Übernehmen"}
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
          {(city || usingDeviceLocation) && (
            <button
              type="button"
              onClick={resetLocation}
              className="w-full font-meta text-sm text-muted hover:text-accent"
            >
              Standort zurücksetzen
            </button>
          )}
        </form>
      )}
    </div>
  );
}
