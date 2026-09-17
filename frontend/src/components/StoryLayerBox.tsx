/** A solid, square-cornered backdrop for a story's text layer or band tag, for when the photo
 * behind it makes the label hard to read (see BandStoryComposer's "Kasten" toggle). Not one
 * plain rectangle - three overlapping, differently-sized and slightly rotated ones, so their
 * combined silhouette is an irregular polygon rather than a simple box, while every edge stays
 * perfectly straight (no rounded corners, matching the site's square look everywhere else).
 *
 * Sized entirely in em, off the content's own font-size - the same unit the text/tag's scale
 * slider already drives - so it grows and shrinks with the content with no JS measurement.
 * The caller's wrapping element must itself be `position: absolute` (or otherwise positioned)
 * so these size relative to it; render this as the FIRST child, before the actual content, so
 * the content simply paints on top in DOM order - each rect is `position: absolute` itself, so
 * being a flex/block sibling here never disturbs the content's own layout. */
export function StoryLayerBox() {
  const rectClassName = "pointer-events-none absolute bg-black/85";
  return (
    <>
      <span aria-hidden className={rectClassName} style={{ top: "-0.35em", left: "-0.6em", right: "-0.6em", bottom: "-0.35em", transform: "rotate(-2deg)" }} />
      <span aria-hidden className={rectClassName} style={{ top: "-0.5em", left: "-0.35em", right: "-0.75em", bottom: "-0.2em", transform: "rotate(1.5deg)" }} />
      <span aria-hidden className={rectClassName} style={{ top: "-0.2em", left: "-0.75em", right: "-0.35em", bottom: "-0.5em", transform: "rotate(1deg)" }} />
    </>
  );
}
