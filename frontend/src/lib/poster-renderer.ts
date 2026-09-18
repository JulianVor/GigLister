import { POSTER_WIDTH as W, POSTER_HEIGHT as H, TICKET_LABEL_GAP, backgroundGradientLuminance, luminanceTextColor, type PosterDraft, type PosterLayer } from "./poster";

export type PosterImages = Map<string, HTMLImageElement>;
export interface PosterFonts { display: string; meta: string }
/** Deterministic PRNG from a single seed - patterns redraw identically across re-renders and
 * exports, and a fresh one only appears via "Neue Farbstimmung" picking a new seed. */
function seededRandom(seed: number) {
  let s = seed || 1;
  return () => (s = (s * 1664525 + 1013904223) >>> 0) / 4294967296;
}
/** Rectangle path, rounded when radius > 0 - shared by the box fill, the logo frame stroke and
 * the selection outline, so "Eckenradius" affects all of a poster's chrome consistently. */
function roundedRectPath(ctx: CanvasRenderingContext2D, x: number, y: number, width: number, height: number, radius: number) {
  ctx.beginPath();
  const r = Math.min(radius, width / 2, height / 2);
  if (r > 0) ctx.roundRect(x, y, width, height, r); else ctx.rect(x, y, width, height);
}
/** Average luminance of a background image, downscaled - same "read a handful of pixels, not
 * the whole photo" approach as logo-transparency.ts, just averaged instead of alpha-checked.
 * Falls back to the gradient colors (which drawPoster paints regardless, visible or not) if
 * the image can't be read for any reason. */
function imageLuminance(image: HTMLImageElement): number | null {
  try {
    const size = 32;
    const canvas = document.createElement("canvas"); canvas.width = size; canvas.height = size;
    const ctx = canvas.getContext("2d", { willReadFrequently: true });
    if (!ctx) return null;
    ctx.drawImage(image, 0, 0, size, size);
    const pixels = ctx.getImageData(0, 0, size, size).data;
    let sum = 0, n = 0;
    for (let i = 0; i < pixels.length; i += 4) { sum += (.2126 * pixels[i] + .7152 * pixels[i + 1] + .0722 * pixels[i + 2]) / 255; n++; }
    return n ? sum / n : null;
  } catch { return null; }
}
/** Black or white, whichever actually reads against this specific background - a real photo's
 * average brightness when there is one (drawn exactly as the poster shows it, including the
 * always-on dim overlay), otherwise the gradient's own two colors. Just a starting point: the
 * band can still repaint any layer's own Textfarbe afterwards. */
export function suggestTextColor(background: PosterDraft["background"], images: PosterImages): string {
  const image = background.image ? images.get(background.image) : null;
  const luminance = (image && imageLuminance(image)) ?? backgroundGradientLuminance(background);
  return luminanceTextColor(luminance, background.dim);
}
export function drawPoster(canvas: HTMLCanvasElement, draft: PosterDraft, images: PosterImages, fonts: PosterFonts, selected?: string) {
  const ctx = canvas.getContext("2d");
  if (!ctx) throw Error("Dein Browser unterstützt den Plakat-Export nicht.");
  ctx.save(); ctx.scale(canvas.width / W, canvas.height / H);
  const bg = draft.background;
  const gradient = ctx.createLinearGradient(0, 0, W, H);
  gradient.addColorStop(0, bg.color1); gradient.addColorStop(1, bg.color2);
  ctx.fillStyle = gradient; ctx.fillRect(0, 0, W, H);
  if (bg.image && images.has(bg.image)) {
    const img = images.get(bg.image)!;
    const fit = Math.max(W / img.naturalWidth, H / img.naturalHeight) * bg.scale;
    ctx.save(); ctx.translate(bg.x, bg.y); ctx.rotate(bg.rotation * Math.PI / 180);
    ctx.drawImage(img, -img.naturalWidth * fit / 2, -img.naturalHeight * fit / 2, img.naturalWidth * fit, img.naturalHeight * fit); ctx.restore();
  } else {
    ctx.save(); ctx.translate(bg.x, bg.y); ctx.rotate(bg.rotation * Math.PI / 180); ctx.scale(bg.scale, bg.scale); ctx.translate(-W / 2, -H / 2);
    ctx.globalAlpha = bg.patternOpacity; ctx.fillStyle = bg.patternColor; ctx.strokeStyle = bg.patternColor;
    // "Musterdichte" scales each pattern's own notion of spacing/count/coil-tightness, so one
    // slider reads as "more/less of it" no matter which generator is active.
    const density = bg.patternDensity;
    if (bg.pattern === "Strahlen") {
      const count = Math.max(6, Math.round(18 * density)), step = Math.PI * 2 / count, width = step * .37;
      for (let i = 0; i < count; i++) {
        const angle = i * step + bg.seed / 100;
        ctx.beginPath(); ctx.moveTo(W * .5, H * .32); ctx.lineTo(W * .5 + Math.cos(angle) * 2200, H * .32 + Math.sin(angle) * 2200); ctx.lineTo(W * .5 + Math.cos(angle + width) * 2200, H * .32 + Math.sin(angle + width) * 2200); ctx.fill();
      }
    }
    if (bg.pattern === "Streifen") { ctx.rotate(-.5); const step = 90 / density; for (let x = -1500; x < 2500; x += step) ctx.fillRect(x, -1500, 24, 4000); }
    if (bg.pattern === "Punkte") {
      const step = 52 / density;
      for (let row = 0, y = 0; y < H; y += step, row++) for (let x = 0; x < W; x += step) { ctx.beginPath(); ctx.arc(x + (row % 2 ? step / 2 : 0), y, 4, 0, Math.PI * 2); ctx.fill(); }
    }
    if (bg.pattern === "Körnung") { const rand = seededRandom(bg.seed); const n = Math.round(13000 * density); for (let i = 0; i < n; i++) ctx.fillRect(rand() * W, rand() * H, 2, 2); }
    if (bg.pattern === "Blitze") {
      const rand = seededRandom(bg.seed); ctx.lineWidth = 14; ctx.lineJoin = "miter";
      const n = Math.max(2, Math.round(8 * density));
      for (let i = 0; i < n; i++) {
        let x = rand() * W * 1.2 - W * .1, y = -60;
        ctx.beginPath(); ctx.moveTo(x, y);
        while (y < H + 60) { x += (rand() - .5) * 240; y += rand() * 140 + 90; ctx.lineTo(x, y); }
        ctx.stroke();
      }
    }
    if (bg.pattern === "Spritzer") {
      const rand = seededRandom(bg.seed); const n = Math.max(4, Math.round(22 * density));
      for (let i = 0; i < n; i++) {
        const cx = rand() * W, cy = rand() * H, r = rand() * 46 + 14;
        ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2); ctx.fill();
        for (let d = Math.floor(rand() * 5) + 2; d > 0; d--) {
          const angle = rand() * Math.PI * 2, dist = r + rand() * 70;
          ctx.beginPath(); ctx.arc(cx + Math.cos(angle) * dist, cy + Math.sin(angle) * dist, rand() * 8 + 2, 0, Math.PI * 2); ctx.fill();
        }
      }
    }
    if (bg.pattern === "Risse") {
      const rand = seededRandom(bg.seed); ctx.lineWidth = 3; const clusters = Math.max(1, Math.round(4 * density));
      for (let c = 0; c < clusters; c++) {
        const cx = rand() * W, cy = rand() * H;
        for (let b = Math.floor(rand() * 5) + 6; b > 0; b--) {
          let angle = rand() * Math.PI * 2, x = cx, y = cy;
          ctx.beginPath(); ctx.moveTo(x, y);
          for (let s = Math.floor(rand() * 4) + 3; s > 0; s--) { angle += (rand() - .5) * 1.1; const len = rand() * 90 + 40; x += Math.cos(angle) * len; y += Math.sin(angle) * len; ctx.lineTo(x, y); }
          ctx.stroke();
        }
      }
    }
    if (bg.pattern === "Schachbrett") {
      ctx.rotate(-.4); const size = 70 / density;
      for (let row = 0, y = -1600; y < 2400; y += size, row++) for (let col = 0, x = -1600; x < 2600; x += size, col++) if ((row + col) % 2 === 0) ctx.fillRect(x, y, size, size);
    }
    if (bg.pattern === "Wirbel") {
      ctx.lineWidth = 16; const spread = 16 / density;
      for (let arm = 0; arm < 3; arm++) {
        ctx.beginPath();
        for (let t = 0; t < 60; t++) {
          const theta = t * .25 + arm * (Math.PI * 2 / 3) + bg.seed / 50, r = t * spread;
          const x = W / 2 + Math.cos(theta) * r, y = H * .42 + Math.sin(theta) * r;
          if (t === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        }
        ctx.stroke();
      }
    }
    // Concentric, noise-wobbled rings around a handful of random "peaks" - reads as a
    // topographic elevation map. Two summed sine harmonics per peak keep each ring irregular
    // instead of a plain circle, without needing an actual 2D noise field.
    if (bg.pattern === "Höhenlinien") {
      const rand = seededRandom(bg.seed); ctx.lineWidth = 2.5;
      const peaks = Math.max(2, Math.round(3 * density)), rings = Math.max(3, Math.round(9 * density));
      for (let p = 0; p < peaks; p++) {
        const cx = rand() * W, cy = rand() * H * .9 + H * .05, step = 60 + rand() * 50;
        const h1 = { f: 2 + Math.floor(rand() * 3), a: rand() * 18 + 6, p: rand() * Math.PI * 2 };
        const h2 = { f: 2 + Math.floor(rand() * 3), a: rand() * 14 + 4, p: rand() * Math.PI * 2 };
        for (let r = 1; r <= rings; r++) {
          const base = r * step;
          ctx.beginPath();
          for (let a = 0; a <= 64; a++) {
            const theta = a / 64 * Math.PI * 2;
            const rr = base + Math.sin(theta * h1.f + h1.p) * h1.a + Math.sin(theta * h2.f + h2.p) * h2.a;
            const x = cx + Math.cos(theta) * rr, y = cy + Math.sin(theta) * rr;
            if (a === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
          }
          ctx.closePath(); ctx.stroke();
        }
      }
    }
    // Flowing, roughly horizontal veins built from two summed sine waves per line (a cheap
    // stand-in for turbulence/domain-warped noise) - reads as marbled/wood-grain streaks.
    if (bg.pattern === "Marmor") {
      const rand = seededRandom(bg.seed); ctx.lineWidth = 2;
      const veins = Math.max(6, Math.round(14 * density));
      const h1 = { f: rand() * 3 + 1.5, a: rand() * 70 + 40, p: rand() * Math.PI * 2 };
      const h2 = { f: rand() * 6 + 3, a: rand() * 30 + 10, p: rand() * Math.PI * 2 };
      for (let v = 0; v < veins; v++) {
        const baseY = (v + .5) / veins * H;
        ctx.beginPath();
        for (let x = -40; x <= W + 40; x += 20) {
          const t = x / W * Math.PI * 2;
          const y = baseY + Math.sin(t * h1.f + h1.p + v * .4) * h1.a + Math.sin(t * h2.f + h2.p + v * .7) * h2.a;
          if (x === -40) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        }
        ctx.stroke();
      }
    }
    ctx.restore();
  }
  ctx.fillStyle = `rgba(0,0,0,${bg.dim})`; ctx.fillRect(0, 0, W, H);
  for (const layer of draft.layers) {
    ctx.save(); ctx.translate(layer.x, layer.y); ctx.rotate(layer.rotation * Math.PI / 180); ctx.scale(layer.scale, layer.scale);
    const { width, height } = layer;
    if (layer.kind === "footer" || layer.box) { ctx.fillStyle = `rgba(0,0,0,${layer.kind === "footer" ? draft.footerOpacity : .7})`; roundedRectPath(ctx, -width / 2, -height / 2, width, height, draft.cornerRadius); ctx.fill(); }
    ctx.fillStyle = layer.color; ctx.textAlign = "center"; ctx.textBaseline = "middle";
    if (layer.kind === "footer") {
      fitText(ctx, layer.genre ?? "", width * .91, height * .38, 78, fonts.display, 0, -height * .22);
      fitText(ctx, layer.text, width * .92, height * .4, 72, fonts.display, 0, height * .21);
    } else if (layer.kind === "title") fitText(ctx, layer.text, width, height, 94, fonts.display, 0, 0);
    else drawBand(ctx, layer, images, fonts, draft.cornerRadius);
    if (selected === layer.id) { ctx.strokeStyle = "#ffffff"; ctx.lineWidth = 2 / layer.scale; ctx.setLineDash([8, 5]); roundedRectPath(ctx, -width / 2, -height / 2, width, height, draft.cornerRadius); ctx.stroke(); }
    ctx.restore();
  }
  if (draft.ticketLabel) {
    const footer = draft.layers.find(item => item.kind === "footer");
    const y = footer ? footer.y + footer.height / 2 + TICKET_LABEL_GAP / 2 : H - TICKET_LABEL_GAP / 2;
    ctx.fillStyle = "rgba(255,255,255,.6)"; ctx.font = `26px ${fonts.meta}`; ctx.textAlign = "center"; ctx.textBaseline = "middle";
    ctx.fillText(`VVK: ${draft.ticketLabel}`, W / 2, y);
  }
  ctx.restore();
}
function fitText(ctx: CanvasRenderingContext2D, text: string, width: number, height: number, maxSize: number, family: string, x: number, y: number) {
  let size = maxSize;
  let lines: string[] = [];
  // Split long words too; this keeps even unusual names inside their own band slot.
  do {
    ctx.font = `${size}px ${family}`;
    lines = [""];
    for (const word of text.trim().split(/\s+/)) {
      const last = lines.length - 1;
      const next = lines[last] ? `${lines[last]} ${word}` : word;
      if (ctx.measureText(next).width <= width) lines[last] = next;
      else if (lines[last]) lines.push(word); else lines[last] = word;
    }
    if (lines.every(line => ctx.measureText(line).width <= width) && lines.length * size * 1.15 <= height) break;
    size -= 1;
  } while (size > 4);
  lines.forEach((line, i) => ctx.fillText(line, x, y + (i - (lines.length - 1) / 2) * size * 1.15, width));
}
function drawBand(ctx: CanvasRenderingContext2D, layer: PosterLayer, images: PosterImages, fonts: PosterFonts, cornerRadius: number) {
  const genreHeight = layer.genre ? Math.min(35, layer.height * .2) : 0;
  const h = layer.height - genreHeight - 16;
  const image = layer.logoUrl ? images.get(layer.logoUrl) : null;
  if (image) {
    const frame = layer.logoFrame ? layer.logoFrameWidth : 0;
    const ratio = Math.min(Math.max(1, layer.width - frame * 2) / image.naturalWidth, Math.max(1, h - frame * 2) / image.naturalHeight);
    const width = image.naturalWidth * ratio, height = image.naturalHeight * ratio;
    if (frame) { ctx.save(); ctx.strokeStyle = layer.logoFrameColor; ctx.lineWidth = frame; roundedRectPath(ctx, -width / 2 - frame / 2, -height / 2 - genreHeight / 2 - frame / 2, width + frame, height + frame, cornerRadius); ctx.stroke(); ctx.restore(); }
    if (layer.logoMode === "original") ctx.drawImage(image, -width / 2, -height / 2 - genreHeight / 2, width, height);
    else {
      const mask = document.createElement("canvas"); mask.width = Math.ceil(width * 3); mask.height = Math.ceil(height * 3);
      const m = mask.getContext("2d")!; m.drawImage(image, 0, 0, mask.width, mask.height); m.globalCompositeOperation = "source-in"; m.fillStyle = layer.logoMode === "white" ? "#ffffff" : "#000000"; m.fillRect(0, 0, mask.width, mask.height);
      ctx.drawImage(mask, -width / 2, -height / 2 - genreHeight / 2, width, height);
    }
  } else fitText(ctx, layer.text, layer.width, h, Math.min(90, h * .6), fonts.display, 0, -genreHeight / 2);
  if (layer.genre) fitText(ctx, layer.genre.toUpperCase(), layer.width, genreHeight, genreHeight * .8, fonts.meta, 0, layer.height / 2 - genreHeight / 2);
}
