/** Muted, editorial-toned fills for entities without a photo - distinct enough from each
 * other and from the site's one accent color that a list of placeholders doesn't read as
 * "all broken images," but restrained enough to still sit inside the black/white/accent
 * palette rather than clashing with it. */
const PLACEHOLDER_PALETTE = [
  "#8a3324", // terracotta
  "#2f5d5a", // deep teal
  "#5b5230", // olive
  "#3c4a6b", // slate blue
  "#5c3a52", // plum
  "#7a5a1e", // ochre
];

/** Deterministic, not random - the same name always gets the same tile, so a band's logo
 * placeholder and its hero placeholder match, and the same band looks the same everywhere
 * it appears without storing anything. */
function paletteIndex(seed: string): number {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash * 31 + seed.charCodeAt(i)) | 0;
  }
  return Math.abs(hash) % PLACEHOLDER_PALETTE.length;
}

/** Drop-in stand-in for a missing Band/Location/Event photo - a big initial on a
 * per-entity color, in the same oversized-display-type spirit as the event card's
 * day-number fallback. `className` controls the box (size, aspect ratio, border) exactly
 * like the `<img>` it replaces would; `textClassName` sizes the initial for that box. */
export function EntityPlaceholder({
  name,
  className,
  textClassName = "text-5xl",
}: {
  name: string;
  className?: string;
  textClassName?: string;
}) {
  const letter = name.trim().charAt(0).toUpperCase() || "?";
  const color = PLACEHOLDER_PALETTE[paletteIndex(name)];

  return (
    <div className={`flex items-center justify-center ${className ?? ""}`} style={{ backgroundColor: color }}>
      <span className={`font-display font-bold leading-none text-white/90 ${textClassName}`}>{letter}</span>
    </div>
  );
}
