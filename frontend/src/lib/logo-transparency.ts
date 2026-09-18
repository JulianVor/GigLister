/** Checks actual alpha values, not the extension: a PNG can still be fully opaque.
 * Native-resolution tiles avoid allocating a second full-size copy of large logos. */
export type LogoTransparency = "transparent" | "opaque" | "unknown";
export function detectLogoTransparency(image: HTMLImageElement): LogoTransparency {
  try {
    if (!image.naturalWidth || !image.naturalHeight) return "unknown";
    const canvas = document.createElement("canvas"); canvas.width = 512; canvas.height = 512;
    const ctx = canvas.getContext("2d", { willReadFrequently: true });
    if (!ctx) return "unknown";
    for (let y = 0; y < image.naturalHeight; y += 512) for (let x = 0; x < image.naturalWidth; x += 512) {
      const w = Math.min(512, image.naturalWidth - x), h = Math.min(512, image.naturalHeight - y);
      ctx.clearRect(0, 0, 512, 512); ctx.drawImage(image, x, y, w, h, 0, 0, w, h);
      const pixels = ctx.getImageData(0, 0, w, h).data;
      for (let i = 3; i < pixels.length; i += 4) if (pixels[i] < 255) return "transparent";
    }
    return "opaque";
  } catch { return "unknown"; }
}
