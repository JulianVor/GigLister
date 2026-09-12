import "server-only";
import { cookies } from "next/headers";
import { CITY_COOKIE, LAT_COOKIE, LON_COOKIE, RADIUS_COOKIE } from "./location-cookies";

export interface LocationPrefs {
  city: string | null;
  radiusKm: number | null;
  lat: number | null;
  lon: number | null;
}

function readNumber(raw: string | undefined): number | null {
  if (!raw) return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}

export async function getLocationPrefs(): Promise<LocationPrefs> {
  const store = await cookies();
  const city = store.get(CITY_COOKIE)?.value ?? null;
  const radiusKm = readNumber(store.get(RADIUS_COOKIE)?.value);
  const lat = readNumber(store.get(LAT_COOKIE)?.value);
  const lon = readNumber(store.get(LON_COOKIE)?.value);
  return { city, radiusKm, lat, lon };
}

const COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

/** Applies a user's saved profile location as their site-wide "Standort wählen" the
 * moment they log in, exactly as if they'd just picked it themselves - same cookies
 * LocationPicker itself writes, just from the server side (a Server Action, here) since
 * that's the only place these cookies can be set for a page that hasn't loaded yet.
 * A no-op when the profile has no home location saved, leaving whatever the visitor
 * already had (a guest pick, or nothing) untouched. */
export async function applyHomeLocationCookies(home: {
  homeCity: string | null;
  homeLatitude: number | null;
  homeLongitude: number | null;
  radiusKm: number | null;
}) {
  if (!home.homeCity && home.homeLatitude == null) {
    return;
  }
  const store = await cookies();
  const set = (name: string, value: string) => store.set(name, value, { path: "/", maxAge: COOKIE_MAX_AGE });

  if (home.homeCity) {
    set(CITY_COOKIE, home.homeCity);
  } else {
    store.delete(CITY_COOKIE);
  }
  if (home.homeLatitude != null && home.homeLongitude != null) {
    set(LAT_COOKIE, String(home.homeLatitude));
    set(LON_COOKIE, String(home.homeLongitude));
  } else {
    store.delete(LAT_COOKIE);
    store.delete(LON_COOKIE);
  }
  if (home.radiusKm != null) {
    set(RADIUS_COOKIE, String(home.radiusKm));
  }
}
