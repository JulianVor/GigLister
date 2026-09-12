import { entityColor } from "@/lib/entityColor";

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

  return (
    <div className={`flex items-center justify-center ${className ?? ""}`} style={{ backgroundColor: entityColor(name) }}>
      <span className={`font-display font-bold leading-none text-white/90 ${textClassName}`}>{letter}</span>
    </div>
  );
}
