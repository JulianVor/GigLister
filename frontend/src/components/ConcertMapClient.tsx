"use client";

import dynamic from "next/dynamic";
import type { MapLocation } from "./ConcertMap";

/** Leaflet touches `window` as soon as it's imported, which crashes during the server
 * render every client component still gets for its initial HTML - `ssr: false` skips
 * that entirely, so the map only ever loads and runs in the browser. `next/dynamic` with
 * `ssr: false` isn't allowed directly inside a Server Component's render tree, hence this
 * separate client wrapper around it instead of just importing ConcertMap straight from
 * the (server) Orte page. */
const ConcertMap = dynamic(() => import("./ConcertMap").then((m) => m.ConcertMap), {
  ssr: false,
  loading: () => <div className="h-[420px] w-full animate-pulse border border-line bg-surface sm:h-[520px]" />,
});

export function ConcertMapClient(props: { center: { lat: number; lon: number } | null; radiusKm: number; locations: MapLocation[] }) {
  return <ConcertMap {...props} />;
}
