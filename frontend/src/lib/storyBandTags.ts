import { colorFromSliderPos } from "@/lib/storyColor";

/** A freely positioned/scaled/rotated tag for another band on a story's photo (see
 * BandStoryComposer's "+ Band") - a square picture plus the band's name, clickable through to
 * that band's profile wherever the story is shown (see CroppedStoryImage). Same
 * opaque-to-the-backend, frontend-owned pattern as TextLayer (see storyTextLayers.ts):
 * BandStory.bandTagsJson is just a string as far as the backend is concerned. The tagged
 * band's name/picture are snapshotted at tagging time rather than looked up live, same as the
 * rest of a story is an immutable-once-posted snapshot. */
export interface BandTagLayer {
  id: string;
  bandId: number;
  bandName: string;
  profileImageUrl: string | null;
  logoUrl: string | null;
  centerXPct: number;
  centerYPct: number;
  scale: number;
  rotationDeg: number;
  /** Position (0-100) on the color slider's white-to-black gradient (see storyColor.ts) for
   * the displayed band name - the picture itself is never recolored, only the name is a label
   * like a text layer's. Null means "no color chosen yet" -> renders white. */
  colorPos: number | null;
}

// Same unit as TEXT_LAYER_BASE_FONT_CQW (% of the frame's own width) - the chip's square
// picture and name both size off this as em-relative children, so one number scales the whole
// chip together, same semantics as the Zoom slider already has for text.
export const BAND_TAG_BASE_FONT_CQW = 5;

export function createBandTagLayer(band: {
  id: number;
  name: string;
  profileImageUrl: string | null;
  logoUrl: string | null;
}): BandTagLayer {
  return {
    id: typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `tag-${Date.now()}-${Math.random()}`,
    bandId: band.id,
    bandName: band.name,
    profileImageUrl: band.profileImageUrl,
    logoUrl: band.logoUrl,
    centerXPct: 50,
    centerYPct: 50,
    scale: 1,
    rotationDeg: 0,
    colorPos: null,
  };
}

/** The actual CSS color for a band tag's chosen slider position - see colorFromSliderPos. */
export function bandTagColor(colorPos: number | null | undefined): string {
  return colorFromSliderPos(colorPos);
}

export function serializeBandTags(tags: BandTagLayer[]): string | undefined {
  return tags.length > 0 ? JSON.stringify(tags) : undefined;
}

export function parseBandTags(json: string | null | undefined): BandTagLayer[] {
  if (!json) return [];
  try {
    const parsed: unknown = JSON.parse(json);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (t): t is BandTagLayer =>
        !!t &&
        typeof t === "object" &&
        typeof (t as BandTagLayer).id === "string" &&
        typeof (t as BandTagLayer).bandId === "number" &&
        typeof (t as BandTagLayer).bandName === "string" &&
        typeof (t as BandTagLayer).centerXPct === "number" &&
        typeof (t as BandTagLayer).centerYPct === "number" &&
        typeof (t as BandTagLayer).scale === "number" &&
        typeof (t as BandTagLayer).rotationDeg === "number" &&
        ((t as BandTagLayer).colorPos === null ||
          (t as BandTagLayer).colorPos === undefined ||
          typeof (t as BandTagLayer).colorPos === "number")
    ).map((t) => ({ ...t, colorPos: typeof t.colorPos === "number" ? t.colorPos : null }));
  } catch {
    return [];
  }
}
