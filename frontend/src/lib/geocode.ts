/** Turns a typed city name into real coordinates, via OpenStreetMap's free Nominatim
 * search - the map (and the radius filter everywhere else) needs an actual center point,
 * which a plain typed string never had before (see the "no reverse geocoding" note this
 * replaces). Runs in the browser rather than through our own backend: Nominatim's usage
 * policy is fine with client-side calls (it CORS-allows them) and identifies the caller
 * from the browser's own Referer, and each visitor's own occasional lookup never comes
 * close to Nominatim's rate limit the way a shared server-side proxy could.
 *
 * Best-effort - returns null on no match, a network error, or an unreachable Nominatim
 * (offline, blocked, down), so callers always have a plain-text fallback to fall back to. */
export async function geocodeCity(query: string): Promise<{ lat: number; lon: number } | null> {
  const trimmed = query.trim();
  if (!trimmed) return null;

  try {
    const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=${encodeURIComponent(trimmed)}`;
    const res = await fetch(url, { headers: { Accept: "application/json" } });
    if (!res.ok) return null;
    const results = (await res.json()) as { lat: string; lon: string }[];
    if (results.length === 0) return null;
    const lat = Number(results[0].lat);
    const lon = Number(results[0].lon);
    if (!Number.isFinite(lat) || !Number.isFinite(lon)) return null;
    return { lat, lon };
  } catch {
    return null;
  }
}
