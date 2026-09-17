/** A freely positioned/scaled/rotated text overlay on a band's story photo (see
 * BandStoryComposer's "+ Text") - as many as a band wants, each independently draggable and
 * resizable. Entirely opaque to the backend (see BandStory.textLayersJson): built, parsed and
 * rendered only here and in CroppedStoryImage, which is what makes both the composer's live
 * preview and the actual story viewer show the exact same thing. */
export interface TextLayer {
  id: string;
  text: string;
  centerXPct: number;
  centerYPct: number;
  scale: number;
  rotationDeg: number;
  /** Hue (0-360) for the text color slider; null means "no color chosen yet" -> renders white. */
  colorHue: number | null;
}

// 1 scale unit = this many cqw (% of the frame's own width, via CSS container query units) -
// keeps text sized consistently relative to the frame regardless of how large that frame
// actually renders (the composer's modal vs. the full-screen viewer are very different pixel
// sizes, but always the same 9:16 shape).
export const TEXT_LAYER_BASE_FONT_CQW = 7;

export function createTextLayer(): TextLayer {
  return {
    id: typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `text-${Date.now()}-${Math.random()}`,
    text: "",
    centerXPct: 50,
    centerYPct: 50,
    scale: 1,
    rotationDeg: 0,
    colorHue: null,
  };
}

/** The actual CSS color for a text layer's chosen hue - null (untouched slider) stays white,
 * matching the color the text already had before this slider existed. */
export function textLayerColor(colorHue: number | null | undefined): string {
  return colorHue == null ? "#ffffff" : `hsl(${colorHue}, 85%, 60%)`;
}

/** Drops any layer nobody actually typed into (an added-then-abandoned empty one) - never
 * worth persisting or rendering. Returns undefined (not "[]") when nothing's left, so the
 * story's textLayersJson stays genuinely absent rather than an empty-but-present array. */
export function serializeTextLayers(layers: TextLayer[]): string | undefined {
  const withText = layers.filter((l) => l.text.trim().length > 0);
  return withText.length > 0 ? JSON.stringify(withText) : undefined;
}

export function parseTextLayers(json: string | null | undefined): TextLayer[] {
  if (!json) return [];
  try {
    const parsed: unknown = JSON.parse(json);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (l): l is TextLayer =>
        !!l &&
        typeof l === "object" &&
        typeof (l as TextLayer).id === "string" &&
        typeof (l as TextLayer).text === "string" &&
        typeof (l as TextLayer).centerXPct === "number" &&
        typeof (l as TextLayer).centerYPct === "number" &&
        typeof (l as TextLayer).scale === "number" &&
        typeof (l as TextLayer).rotationDeg === "number" &&
        ((l as TextLayer).colorHue === null ||
          (l as TextLayer).colorHue === undefined ||
          typeof (l as TextLayer).colorHue === "number")
    ).map((l) => ({ ...l, colorHue: typeof l.colorHue === "number" ? l.colorHue : null }));
  } catch {
    return [];
  }
}
