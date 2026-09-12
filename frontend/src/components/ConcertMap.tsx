"use client";

import "leaflet/dist/leaflet.css";
import L from "leaflet";
import { useEffect, useRef } from "react";
import { formatTime, fullDateLabel } from "@/lib/format";

export interface MapLocation {
  id: number;
  name: string;
  city: string;
  latitude: number;
  longitude: number;
  events: { id: number; date: string; startTime: string | null }[];
}

/** The concert finder for /orte - a Leaflet + OpenStreetMap view centered on whatever
 * "Standort wählen" resolved (typed-and-geocoded city, device position, or a saved
 * profile default), with the same radius circle that already governs every other list's
 * filtering. Plain divIcon markers (an accent dot for "you", a numbered dot per venue)
 * instead of Leaflet's default pin images, which sidesteps the well-known bundler-asset
 * path issue with those and lets the markers match the app's own flat, editorial style
 * instead of looking like a generic map widget dropped on the page.
 *
 * Torn down and rebuilt on every prop change (center/radius/locations) rather than
 * incrementally diffed - simplest to get right, and cheap at this scale (a few dozen
 * markers at most). */
export function ConcertMap({
  center,
  radiusKm,
  locations,
}: {
  center: { lat: number; lon: number } | null;
  radiusKm: number;
  locations: MapLocation[];
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const locationsKey = locations.map((l) => `${l.id}:${l.events.length}`).join(",");

  useEffect(() => {
    if (!containerRef.current) return;

    const map = L.map(containerRef.current, { scrollWheelZoom: false });
    // A circle's/marker's own getBounds() projects through the map's current view, which
    // doesn't exist yet on a freshly created map - setting some view up front (refined
    // below once the real content is known) is what makes those calls safe to make at all.
    map.setView(center ? [center.lat, center.lon] : [51.16, 10.45], center ? 12 : 5);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 18,
    }).addTo(map);

    const bounds = L.latLngBounds([]);

    if (center) {
      const youIcon = L.divIcon({
        className: "",
        html: `<div style="width:16px;height:16px;border-radius:9999px;background:var(--accent);border:3px solid var(--bg);box-shadow:0 0 0 1px var(--accent)"></div>`,
        iconSize: [16, 16],
        iconAnchor: [8, 8],
      });
      L.marker([center.lat, center.lon], { icon: youIcon, zIndexOffset: 1000 })
        .addTo(map)
        .bindTooltip("Dein Standort");

      const circle = L.circle([center.lat, center.lon], {
        radius: radiusKm * 1000,
        color: "var(--accent)",
        weight: 1,
        fillOpacity: 0.05,
      }).addTo(map);
      bounds.extend(circle.getBounds());
    }

    for (const loc of locations) {
      const count = loc.events.length;
      const venueIcon = L.divIcon({
        className: "",
        html: `<div style="min-width:22px;height:22px;padding:0 4px;border-radius:9999px;background:var(--fg);color:var(--bg);display:flex;align-items:center;justify-content:center;font:600 11px var(--font-meta,sans-serif);border:2px solid var(--bg)">${count}</div>`,
        iconSize: [22, 22],
        iconAnchor: [11, 11],
      });
      const marker = L.marker([loc.latitude, loc.longitude], { icon: venueIcon }).addTo(map);
      bounds.extend([loc.latitude, loc.longitude]);

      const list = loc.events
        .slice(0, 5)
        .map((e) => {
          const time = formatTime(e.startTime);
          return `<a href="/konzerte/${e.id}" style="display:block;margin-top:4px;color:var(--accent);text-decoration:underline">${fullDateLabel(e.date)}${time ? " · " + time : ""}</a>`;
        })
        .join("");
      // Coordinates rather than an address string - shortest, most precise destination a
      // maps app can be handed, and it works the same regardless of what (if anything)
      // the venue's own address field contains.
      const directionsUrl = `https://www.google.com/maps/dir/?api=1&destination=${loc.latitude},${loc.longitude}`;
      const popupHtml = `<div style="font-family:inherit"><a href="/orte/${loc.id}" style="color:inherit;text-decoration:none"><strong>${escapeHtml(loc.name)}</strong></a><br/><span style="color:var(--muted)">${escapeHtml(loc.city)}</span>${list}<a href="${directionsUrl}" target="_blank" rel="noopener noreferrer" style="display:block;margin-top:8px;color:var(--accent);text-decoration:underline">Route planen →</a></div>`;
      marker.bindPopup(popupHtml);
    }

    // Refines the placeholder view above now that the real content (the radius circle,
    // any venue markers) is known - left as-is when there's nothing to fit to (e.g. a
    // center with no matching venues at all).
    if (bounds.isValid()) {
      map.fitBounds(bounds, { padding: [24, 24], maxZoom: 14 });
    }

    return () => {
      map.remove();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [center?.lat, center?.lon, radiusKm, locationsKey]);

  return <div ref={containerRef} className="h-[420px] w-full border border-line sm:h-[520px]" />;
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);
}
