import "server-only";
import { cookies } from "next/headers";
import { CITY_COOKIE, RADIUS_COOKIE } from "./location-cookies";

export interface LocationPrefs {
  city: string | null;
  radiusKm: number | null;
}

export async function getLocationPrefs(): Promise<LocationPrefs> {
  const store = await cookies();
  const city = store.get(CITY_COOKIE)?.value ?? null;
  const radiusRaw = store.get(RADIUS_COOKIE)?.value;
  const radiusKm = radiusRaw ? Number(radiusRaw) : null;
  return { city, radiusKm: radiusKm !== null && Number.isFinite(radiusKm) ? radiusKm : null };
}
