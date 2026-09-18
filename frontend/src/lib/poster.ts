import type { EventResponse } from "./types";

export const POSTER_WIDTH = 1000;
export const POSTER_HEIGHT = 1414;
export const patterns = ["Verlauf", "Strahlen", "Streifen", "Punkte", "Körnung", "Blitze", "Spritzer", "Risse", "Schachbrett", "Wirbel"] as const;
export type Pattern = typeof patterns[number];
export interface PosterLayer {
  id: string; kind: "title" | "band" | "footer"; label: string;
  x: number; y: number; width: number; height: number; scale: number; rotation: number;
  color: string; box: boolean; logoMode: "original" | "white" | "black";
  logoFrame: boolean; logoFrameColor: string; logoFrameWidth: number;
  text: string; genre?: string; bandId?: number; logoUrl?: string | null;
}
export interface PosterDraft {
  version: 1; eventId: number;
  background: { image: string | null; color1: string; color2: string; pattern: Pattern; seed: number; scale: number; x: number; y: number; rotation: number; dim: number };
  footerOpacity: number; cornerRadius: number; ticketLabel: string | null; layers: PosterLayer[];
}
export function posterDate(date: string, time: string | null): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  const label = match ? `${match[3]}.${match[2]}.${match[1]}` : date;
  return label + (time ? ` · ${time.slice(0, 5)} UHR` : "");
}
// Bottom margin (poster-space px) reserved below the footer box for the ticket-provider line
// when one exists - the footer simply keeps its original full-bleed position otherwise.
export const TICKET_LABEL_GAP = 70;
/** Just the bare domain ("tix4gigs.com"), never the scheme/www/path - this is printed on the
 * poster itself, not clicked, so anything more than the domain is just noise (or, worse, a
 * full URL someone would need to type out by hand off a printed poster). */
export function ticketProviderLabel(url: string | null | undefined): string | null {
  if (!url?.trim()) return null;
  try {
    const parsed = new URL(/^[a-z][a-z\d+.-]*:\/\//i.test(url) ? url : `https://${url}`);
    return parsed.hostname.replace(/^www\./i, "") || null;
  } catch { return null; }
}
function relativeLuminance(hex: string): number {
  const n = parseInt(hex.slice(1), 16);
  const channels = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map(c => {
    const s = c / 255;
    return s <= .03928 ? s / 12.92 : ((s + .055) / 1.055) ** 2.4;
  });
  return .2126 * channels[0] + .7152 * channels[1] + .0722 * channels[2];
}
/** Black or white, whichever reads better against a given luminance once the background's own
 * dim overlay (always painted, see drawPoster) is factored in. Exported separately from
 * suggestTextColor (poster-renderer.ts) because it has no DOM dependency - a background image's
 * actual luminance needs canvas pixel access, which only exists there; this is the pure
 * gradient-only fallback, used directly whenever there's no image to sample. */
export function luminanceTextColor(luminance: number, dim: number): string {
  return luminance * (1 - dim) > .4 ? "#000000" : "#ffffff";
}
export function backgroundGradientLuminance(background: Pick<PosterDraft["background"], "color1" | "color2">): number {
  return (relativeLuminance(background.color1) + relativeLuminance(background.color2)) / 2;
}
export function initialPoster(event: EventResponse): PosterDraft {
  const title = event.title?.trim();
  const layers: PosterLayer[] = [];
  const base = { scale: 1, rotation: 0, color: "#ffffff", box: false, logoMode: "original" as const, logoFrame: false, logoFrameColor: "#ffffff", logoFrameWidth: 6 };
  if (title) layers.push({ ...base, id: "title", kind: "title", label: "Veranstaltungstitel", text: title, x: 500, y: 145, width: 880, height: 170 });
  const top = title ? 295 : 100;
  const bottom = 1130;
  const count = event.bands.length;
  const headliner = count % 2 === 1 && count <= 7;
  const columns = count > 8 ? 3 : 2;
  const rows = headliner ? 1 + Math.ceil((count - 1) / 2) : Math.ceil(count / columns);
  const rowHeight = (bottom - top) / Math.max(1, rows);
  event.bands.forEach((band, index) => {
    const hero = headliner && index === 0;
    const adjusted = headliner ? index - 1 : index;
    const row = hero ? 0 : Math.floor(adjusted / columns) + (headliner ? 1 : 0);
    const rowCount = hero ? 1 : Math.min(columns, count - (headliner ? 1 : 0) - Math.floor(adjusted / columns) * columns);
    const column = hero ? 0 : adjusted % columns;
    layers.push({ ...base, id: `band-${band.id}`, kind: "band", label: band.name, text: band.name,
      bandId: band.id, logoUrl: band.logoUrl, genre: band.genres[0] ?? "",
      x: (column + .5) * (900 / rowCount) + 50, y: top + (row + .5) * rowHeight,
      width: hero ? 790 : 820 / rowCount, height: Math.min(hero ? 300 : 260, rowHeight * .82) });
  });
  const ticketLabel = ticketProviderLabel(event.ticketUrl);
  const footerY = ticketLabel ? 1294 - TICKET_LABEL_GAP : 1294;
  layers.push({ ...base, id: "footer", kind: "footer", label: "Ort & Termin", text: event.location.name,
    genre: posterDate(event.date, event.startTime), x: 500, y: footerY, width: 1000, height: 240 });
  return { version: 1, eventId: event.id, background: { image: null, color1: "#171f2c", color2: "#c84b24", pattern: "Körnung", seed: 42, scale: 1, x: 500, y: 707, rotation: 0, dim: .12 }, footerOpacity: .65, cornerRadius: 0, ticketLabel, layers };
}
export function hitLayer(layer: PosterLayer, x: number, y: number): boolean {
  const angle = -layer.rotation * Math.PI / 180;
  const dx = x - layer.x, dy = y - layer.y;
  return Math.abs(dx * Math.cos(angle) - dy * Math.sin(angle)) <= layer.width * layer.scale / 2 &&
    Math.abs(dx * Math.sin(angle) + dy * Math.cos(angle)) <= layer.height * layer.scale / 2;
}
export function restorePoster(raw: string, event: EventResponse): PosterDraft {
  const d = JSON.parse(raw) as PosterDraft;
  const validColor = (s: unknown) => typeof s === "string" && /^#[\da-f]{6}$/i.test(s);
  const finite = (n: unknown, low: number, high: number) => typeof n === "number" && Number.isFinite(n) && n >= low && n <= high;
  if (d?.version !== 1 || d.eventId !== event.id || !Array.isArray(d.layers) || d.layers.length > 200 || !d.background) throw Error("Dieser Entwurf passt nicht zum Konzert.");
  const b = d.background;
  if (!validColor(b.color1) || !validColor(b.color2) || !patterns.includes(b.pattern) || !finite(b.seed, 0, 1e9) || !finite(b.scale, .4, 5) || !finite(b.rotation, -360, 360) || !finite(b.x, -1000, 2000) || !finite(b.y, -1414, 2828) || !finite(b.dim, 0, .85) || !finite(d.footerOpacity, 0, 1)) throw Error("Der gespeicherte Hintergrund ist ungültig.");
  if (d.cornerRadius !== undefined && !finite(d.cornerRadius, 0, 80)) throw Error("Der gespeicherte Eckenradius ist ungültig.");
  d.cornerRadius ??= 0;
  // Event-owned, like logoUrl below - never trust a saved ticket label, always re-derive it.
  d.ticketLabel = ticketProviderLabel(event.ticketUrl);
  if (b.image !== null && (typeof b.image !== "string" || !/^data:image\/(png|jpeg|webp);base64,/.test(b.image) || b.image.length > 8_000_000)) throw Error("Das gespeicherte Bild ist ungültig.");
  const current = initialPoster(event);
  const ids = new Set<string>();
  d.layers = d.layers.map(layer => {
    const original = current.layers.find(item => item.id === layer.id);
    if (!original || ids.has(layer.id) || !finite(layer.x, -1000, 2000) || !finite(layer.y, -1414, 2828) || !finite(layer.scale, .4, 3) || !finite(layer.rotation, -180, 180) || !validColor(layer.color) || typeof layer.box !== "boolean" || !["original", "white", "black"].includes(layer.logoMode)) throw Error("Die gespeicherten Ebenen sind ungültig oder das Line-up hat sich geändert.");
    if ((layer.logoFrame !== undefined && typeof layer.logoFrame !== "boolean") || (layer.logoFrameColor !== undefined && !validColor(layer.logoFrameColor)) || (layer.logoFrameWidth !== undefined && !finite(layer.logoFrameWidth, 2, 24))) throw Error("Der gespeicherte Logo-Rahmen ist ungültig.");
    ids.add(layer.id);
    // Event-owned data and asset URLs always come from the current event, never local storage.
    return { ...original, x: layer.x, y: layer.y, scale: layer.scale, rotation: layer.rotation, color: layer.color, box: layer.box, logoMode: layer.logoMode, logoFrame: layer.logoFrame ?? false, logoFrameColor: layer.logoFrameColor ?? "#ffffff", logoFrameWidth: layer.logoFrameWidth ?? 6 };
  });
  if (ids.size !== current.layers.length) throw Error("Das Line-up hat sich geändert. Bitte die automatische Anordnung verwenden.");
  return d;
}
