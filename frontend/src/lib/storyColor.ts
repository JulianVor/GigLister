/** The white-to-black rainbow gradient a story's color slider picks from (see
 * BandStoryComposer) - shared by text layers and band tags, since both are just colored labels
 * on the photo and both need the slider's track and the actual rendered color to agree on
 * exactly the same gradient. White at 0 (matches how a label already looked before this slider
 * existed - an untouched slider stays there) through the hue wheel to black at 100. */
const GRADIENT_ANCHORS: readonly [number, number, number][] = [
  [255, 255, 255], // 0   white
  [255, 0, 0], // red
  [255, 255, 0], // yellow
  [0, 255, 0], // green
  [0, 255, 255], // cyan
  [0, 0, 255], // blue
  [255, 0, 255], // magenta
  [0, 0, 0], // 100 black
];

// The slider's own track background - evenly-spaced CSS color stops, matching the same
// evenly-spaced RGB interpolation colorFromSliderPos does below.
export const COLOR_SLIDER_GRADIENT_CSS = "linear-gradient(to right, white, red, yellow, green, cyan, blue, magenta, black)";

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

/** pos: 0 (white) .. 100 (black) on the gradient above, or null/undefined for "the slider was
 * never touched" - defaults to white either way, so an object looks exactly the same as it did
 * before this slider existed until someone actually drags it. */
export function colorFromSliderPos(pos: number | null | undefined): string {
  if (pos == null) return "#ffffff";
  const segments = GRADIENT_ANCHORS.length - 1;
  const scaled = (clamp(pos, 0, 100) / 100) * segments;
  const i = Math.min(segments - 1, Math.floor(scaled));
  const frac = scaled - i;
  const [r1, g1, b1] = GRADIENT_ANCHORS[i];
  const [r2, g2, b2] = GRADIENT_ANCHORS[i + 1];
  const rgb = [r1 + (r2 - r1) * frac, g1 + (g2 - g1) * frac, b1 + (b2 - b1) * frac].map((c) => Math.round(c));
  return `#${rgb.map((c) => c.toString(16).padStart(2, "0")).join("")}`;
}
