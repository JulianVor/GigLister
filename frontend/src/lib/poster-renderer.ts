import { POSTER_WIDTH as W, POSTER_HEIGHT as H, TICKET_LABEL_GAP, backgroundGradientLuminance, luminanceTextColor, type PosterDraft, type PosterLayer } from "./poster";

export type PosterImages = Map<string, HTMLImageElement>;
export interface PosterFonts { display: string; meta: string }
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
    ctx.globalAlpha = .17; ctx.fillStyle = "#ffffff"; ctx.strokeStyle = "#ffffff";
    if (bg.pattern === "Strahlen") for (let i = 0; i < 18; i++) {
      const angle = i * Math.PI / 9 + bg.seed / 100;
      ctx.beginPath(); ctx.moveTo(W * .5, H * .32); ctx.lineTo(W * .5 + Math.cos(angle) * 2200, H * .32 + Math.sin(angle) * 2200); ctx.lineTo(W * .5 + Math.cos(angle + .13) * 2200, H * .32 + Math.sin(angle + .13) * 2200); ctx.fill();
    }
    if (bg.pattern === "Streifen") { ctx.rotate(-.5); for (let x = -1500; x < 2500; x += 90) ctx.fillRect(x, -1500, 24, 4000); }
    if (bg.pattern === "Punkte") for (let y = 0; y < H; y += 52) for (let x = 0; x < W; x += 52) { ctx.beginPath(); ctx.arc(x + (y % 104 ? 26 : 0), y, 4, 0, Math.PI * 2); ctx.fill(); }
    if (bg.pattern === "Körnung") {
      let seed = bg.seed || 1;
      for (let i = 0; i < 13000; i++) { seed = (seed * 1664525 + 1013904223) >>> 0; const x = seed / 4294967296 * W; seed = (seed * 1664525 + 1013904223) >>> 0; ctx.fillRect(x, seed / 4294967296 * H, 2, 2); }
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
