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
