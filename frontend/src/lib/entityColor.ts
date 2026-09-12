/** Muted, editorial-toned fills for entities without a photo - distinct enough from each
 * other and from the site's one accent color that a list of placeholders doesn't read as
 * "all broken images," but restrained enough to still sit inside the black/white/accent
 * palette rather than clashing with it. Shared by EntityPlaceholder (Band/Location detail
 * pages) and EventCard's no-photo fallback, so the same name always gets the same color
 * everywhere it's shown. */
const ENTITY_COLOR_PALETTE = [
  "#8a3324", // terracotta
  "#2f5d5a", // deep teal
  "#5b5230", // olive
  "#3c4a6b", // slate blue
  "#5c3a52", // plum
  "#7a5a1e", // ochre
];

/** Deterministic, not random - the same name always gets the same color, so it doesn't
 * need to be stored anywhere. */
export function entityColor(seed: string): string {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash * 31 + seed.charCodeAt(i)) | 0;
  }
  return ENTITY_COLOR_PALETTE[Math.abs(hash) % ENTITY_COLOR_PALETTE.length];
}
