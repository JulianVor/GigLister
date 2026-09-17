"use client";

import { useRef, useState, useSyncExternalStore, useTransition } from "react";
import { uploadImageAction } from "@/actions/uploads";
import { createBandStoryAction } from "@/actions/bands";
import { MAX_UPLOAD_SIZE_BYTES, MAX_UPLOAD_SIZE_MB } from "@/lib/upload";

const TEXT_MAX_LENGTH = 280;
// Same frame the viewer actually shows the story in (see BandStoryViewer) - the crop only
// reproduces faithfully if both sides agree on what shape "the frame" is.
const FRAME_ASPECT = 9 / 16;
// scale=1 always means "the whole photo is visible" (fit) - there's no forced minimum crop
// anymore, per the band's own request: zooming out all the way should show the entire image,
// with whatever's left of the frame filled by imgBackgroundColor rather than always cropping.
const MIN_SCALE = 1;
const MAX_SCALE = 5;
const FALLBACK_BG = "#111111";

interface Transform {
  widthPct: number;
  heightPct: number;
  centerXPct: number;
  centerYPct: number;
  rotationDeg: number;
}

interface Point {
  x: number;
  y: number;
}

/** The size (as % of the frame) at which an image of this aspect ratio is entirely visible
 * within the frame (letterboxed on one axis unless the ratios match exactly) - the scale=1
 * baseline everything else scales up from. */
function fitSize(imgAspect: number): { widthPct: number; heightPct: number } {
  if (imgAspect > FRAME_ASPECT) {
    return { widthPct: 100, heightPct: (FRAME_ASPECT / imgAspect) * 100 };
  }
  return { widthPct: (imgAspect / FRAME_ASPECT) * 100, heightPct: 100 };
}

function sizeAtScale(imgAspect: number, scale: number) {
  const fit = fitSize(imgAspect);
  return { widthPct: fit.widthPct * scale, heightPct: fit.heightPct * scale };
}

function scaleOf(transform: Transform, imgAspect: number): number {
  return transform.widthPct / fitSize(imgAspect).widthPct;
}

// Loose safety bound only - keeps a wild drag/pinch from losing the image entirely off-frame
// or scaling it away to nothing/absurdly large. Not "always cover the frame": gaps are fine
// now, imgBackgroundColor fills them.
function clampCenter(pct: number): number {
  return Math.min(150, Math.max(-50, pct));
}

function clampScale(scale: number): number {
  return Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale));
}

const TOUCH_PRIMARY_QUERY = "(hover: none) and (pointer: coarse)";

function subscribeToTouchPrimary(callback: () => void) {
  const mq = window.matchMedia(TOUCH_PRIMARY_QUERY);
  mq.addEventListener("change", callback);
  return () => mq.removeEventListener("change", callback);
}

function getIsTouchPrimary() {
  return window.matchMedia(TOUCH_PRIMARY_QUERY).matches;
}

function getIsTouchPrimaryServerSnapshot() {
  return false;
}

function distance(a: Point, b: Point): number {
  return Math.hypot(b.x - a.x, b.y - a.y);
}

function angleDeg(a: Point, b: Point): number {
  return (Math.atan2(b.y - a.y, b.x - a.x) * 180) / Math.PI;
}

/** Average color sampled from the photo (a 1x1 canvas draw does a cheap area-average via the
 * browser's own downscaling) - used to fill whatever the image doesn't cover once it's no
 * longer forced to cover the whole frame. Needs crossOrigin on the loader (not on the live
 * preview <img>, which never reads pixels) since the upload is served from a different origin
 * than the frontend in dev - falls back to null (caller uses FALLBACK_BG) if that's blocked
 * for any reason, e.g. a stricter CORS setup elsewhere. */
function sampleAverageColor(url: string): Promise<string | null> {
  return new Promise((resolve) => {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => {
      try {
        const canvas = document.createElement("canvas");
        canvas.width = 1;
        canvas.height = 1;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          resolve(null);
          return;
        }
        ctx.drawImage(img, 0, 0, 1, 1);
        const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
        resolve(`#${[r, g, b].map((c) => c.toString(16).padStart(2, "0")).join("")}`);
      } catch {
        resolve(null);
      }
    };
    img.onerror = () => resolve(null);
    img.src = url;
  });
}

/** Drag-to-position, pinch-to-zoom, two-finger-to-rotate editor for fitting a photo into the
 * 9:16 story frame before posting - same gesture a phone's own camera roll / Instagram gives
 * you, because a band's source image (a flyer, a landscape photo, whatever) rarely already has
 * the story's own aspect ratio. A mouse/trackpad can't pinch, so the sliders next to this stay
 * the only way to zoom/rotate there - but on an actual touchscreen the gesture alone is enough,
 * which is why BandStoryComposer hides them on touch-primary devices. Purely a positioning
 * tool: the source image is never modified, only a few numbers describing where it sits get
 * saved (see CroppedStoryImage, which reproduces this exact same view wherever the story is
 * then shown). */
function StoryCropEditor({
  imageUrl,
  imgAspect,
  transform,
  onTransformChange,
  bgColor,
}: {
  imageUrl: string;
  imgAspect: number;
  transform: Transform;
  onTransformChange: (transform: Transform) => void;
  bgColor: string | null;
}) {
  const frameRef = useRef<HTMLDivElement>(null);
  const pointersRef = useRef<Map<number, Point>>(new Map());
  const gestureRef = useRef({
    transform,
    singleStart: { x: 0, y: 0 },
    // Two-pointer-only reference values.
    distance: 0,
    angle: 0,
    midpoint: { x: 0, y: 0 },
  });

  function startGesture() {
    const points = [...pointersRef.current.values()];
    gestureRef.current.transform = transform;
    if (points.length === 1) {
      gestureRef.current.singleStart = points[0];
    } else if (points.length === 2) {
      gestureRef.current.distance = distance(points[0], points[1]);
      gestureRef.current.angle = angleDeg(points[0], points[1]);
      gestureRef.current.midpoint = { x: (points[0].x + points[1].x) / 2, y: (points[0].y + points[1].y) / 2 };
    }
  }

  function onPointerDown(e: React.PointerEvent) {
    // Guarded: a failed capture (e.g. an already-released pointer in some edge case) would
    // otherwise abort this handler before the pointer is even tracked below, silently
    // breaking the whole gesture instead of just losing capture-while-dragging-outside-frame.
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {
      // ignore
    }
    pointersRef.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    startGesture();
  }

  function onPointerMove(e: React.PointerEvent) {
    if (!pointersRef.current.has(e.pointerId) || !frameRef.current) return;
    pointersRef.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    const points = [...pointersRef.current.values()];
    const rect = frameRef.current.getBoundingClientRect();

    if (points.length === 2) {
      const newDistance = distance(points[0], points[1]);
      const newAngle = angleDeg(points[0], points[1]);
      const newMidpoint = { x: (points[0].x + points[1].x) / 2, y: (points[0].y + points[1].y) / 2 };
      const start = gestureRef.current;
      const scaleRatio = start.distance > 0 ? newDistance / start.distance : 1;
      const newScale = clampScale(scaleOf(start.transform, imgAspect) * scaleRatio);
      const size = sizeAtScale(imgAspect, newScale);
      const dxPct = ((newMidpoint.x - start.midpoint.x) / rect.width) * 100;
      const dyPct = ((newMidpoint.y - start.midpoint.y) / rect.height) * 100;
      onTransformChange({
        widthPct: size.widthPct,
        heightPct: size.heightPct,
        rotationDeg: start.transform.rotationDeg + (newAngle - start.angle),
        centerXPct: clampCenter(start.transform.centerXPct + dxPct),
        centerYPct: clampCenter(start.transform.centerYPct + dyPct),
      });
    } else if (points.length === 1) {
      const start = gestureRef.current;
      const dxPct = ((points[0].x - start.singleStart.x) / rect.width) * 100;
      const dyPct = ((points[0].y - start.singleStart.y) / rect.height) * 100;
      onTransformChange({
        ...start.transform,
        centerXPct: clampCenter(start.transform.centerXPct + dxPct),
        centerYPct: clampCenter(start.transform.centerYPct + dyPct),
      });
    }
  }

  function onPointerUpOrCancel(e: React.PointerEvent) {
    pointersRef.current.delete(e.pointerId);
    startGesture();
  }

  return (
    <div
      ref={frameRef}
      className="relative mt-3 aspect-[9/16] w-full touch-none overflow-hidden border border-line"
      style={{ backgroundColor: bgColor ?? FALLBACK_BG }}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUpOrCancel}
      onPointerCancel={onPointerUpOrCancel}
      onPointerLeave={onPointerUpOrCancel}
    >
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={imageUrl}
        alt=""
        draggable={false}
        className="absolute max-w-none cursor-move select-none"
        style={{
          left: `${transform.centerXPct}%`,
          top: `${transform.centerYPct}%`,
          width: `${transform.widthPct}%`,
          height: `${transform.heightPct}%`,
          transform: `translate(-50%, -50%) rotate(${transform.rotationDeg}deg)`,
        }}
      />
    </div>
  );
}

/** "Status posten" - the entry point a band's own manager uses to publish a new 24h status
 * (see BandStoryAvatarButton/BandStoryViewer for how everyone else then sees it). Upload a
 * photo, position/zoom/rotate it to fit the frame, optionally caption it, then post - unlike
 * PasteImageUpload elsewhere, the image alone isn't persisted onto anything until "Posten"
 * (there's nothing sensible to save it onto before the story itself exists). */
export function BandStoryComposer({ bandId }: { bandId: number }) {
  const [open, setOpen] = useState(false);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [imgAspect, setImgAspect] = useState<number | null>(null);
  const [transform, setTransform] = useState<Transform | null>(null);
  const [bgColor, setBgColor] = useState<string | null>(null);
  const [text, setText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [pending, startTransition] = useTransition();
  const inputRef = useRef<HTMLInputElement>(null);

  // No hover + a coarse pointer = a touchscreen is the primary input (phones/tablets), as
  // opposed to a mouse/trackpad that can't pinch or rotate - those keep the sliders below,
  // since there's no gesture for them to use instead. Server snapshot is "false" (sliders
  // shown) since matchMedia isn't available during SSR - corrects itself on the client's
  // first paint, same as any other viewport-dependent UI.
  const isTouchPrimary = useSyncExternalStore(subscribeToTouchPrimary, getIsTouchPrimary, getIsTouchPrimaryServerSnapshot);

  function reset() {
    setOpen(false);
    setImageUrl(null);
    setImgAspect(null);
    setTransform(null);
    setBgColor(null);
    setText("");
    setError(null);
  }

  function pickFile(file: File) {
    setError(null);
    if (!file.type.startsWith("image/")) {
      setError("Das ist keine Bilddatei.");
      return;
    }
    if (file.size > MAX_UPLOAD_SIZE_BYTES) {
      setError(`Die Datei ist zu groß (maximal ${MAX_UPLOAD_SIZE_MB} MB)`);
      return;
    }
    const formData = new FormData();
    formData.append("file", file);
    setUploading(true);
    startTransition(async () => {
      const result = await uploadImageAction(formData);
      if (!result.ok) {
        setUploading(false);
        setError(result.error);
        return;
      }
      const url = result.data.url;
      const probe = new Image();
      probe.onload = async () => {
        const aspect = probe.naturalWidth / probe.naturalHeight;
        const size = sizeAtScale(aspect, MIN_SCALE);
        setImgAspect(aspect);
        setTransform({ widthPct: size.widthPct, heightPct: size.heightPct, centerXPct: 50, centerYPct: 50, rotationDeg: 0 });
        setImageUrl(url);
        setUploading(false);
        setBgColor(await sampleAverageColor(url));
      };
      probe.src = url;
    });
  }

  function handleScaleChange(newScale: number) {
    if (imgAspect == null || !transform) return;
    const size = sizeAtScale(imgAspect, newScale);
    setTransform({ ...transform, widthPct: size.widthPct, heightPct: size.heightPct });
  }

  function handleRotationChange(deg: number) {
    if (!transform) return;
    setTransform({ ...transform, rotationDeg: deg });
  }

  function submit() {
    if (!imageUrl || !transform) return;
    setError(null);
    startTransition(async () => {
      const result = await createBandStoryAction(bandId, {
        imageUrl,
        text: text.trim() || undefined,
        imgWidthPct: transform.widthPct,
        imgHeightPct: transform.heightPct,
        imgCenterXPct: transform.centerXPct,
        imgCenterYPct: transform.centerYPct,
        imgRotationDeg: transform.rotationDeg,
        imgBackgroundColor: bgColor ?? FALLBACK_BG,
      });
      if (!result.ok) {
        setError(result.error ?? "Posten fehlgeschlagen.");
        return;
      }
      reset();
    });
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="border border-line px-5 py-2 font-meta text-sm hover:border-fg"
      >
        Status posten
      </button>
    );
  }

  return (
    <div className="fixed inset-0 z-[2000] flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-sm border border-line bg-surface p-4">
        <div className="flex items-center justify-between">
          <h3 className="font-meta text-sm uppercase tracking-wide text-muted">Status posten</h3>
          <button type="button" onClick={reset} aria-label="Schließen" className="font-meta text-lg leading-none hover:text-accent">
            ✕
          </button>
        </div>

        {!imageUrl || !transform || imgAspect == null ? (
          <div
            role="button"
            tabIndex={0}
            onClick={() => inputRef.current?.click()}
            onPaste={(e) => {
              const item = Array.from(e.clipboardData.items).find((i) => i.type.startsWith("image/"));
              const file = item?.getAsFile();
              if (file) pickFile(file);
            }}
            className="mt-3 flex aspect-[9/16] cursor-pointer flex-col items-center justify-center gap-1 border border-dashed border-line text-center outline-none focus:border-accent"
          >
            <span className="font-meta text-sm text-muted">{uploading ? "Wird hochgeladen …" : "Foto wählen oder einfügen (Strg+V)"}</span>
          </div>
        ) : (
          <>
            <StoryCropEditor
              imageUrl={imageUrl}
              imgAspect={imgAspect}
              transform={transform}
              onTransformChange={setTransform}
              bgColor={bgColor}
            />
            <p className="mt-1 font-meta text-xs text-muted">
              {isTouchPrimary ? "Ziehen zum Verschieben · zwei Finger zum Zoomen und Drehen" : "Ziehen zum Verschieben"}
            </p>

            {!isTouchPrimary && (
              <>
                <label className="mt-2 flex items-center gap-2" htmlFor="story-zoom">
                  <span className="w-14 flex-none font-meta text-xs uppercase tracking-wide text-muted">Zoom</span>
                  <input
                    id="story-zoom"
                    type="range"
                    min={MIN_SCALE}
                    max={MAX_SCALE}
                    step={0.01}
                    value={scaleOf(transform, imgAspect)}
                    onChange={(e) => handleScaleChange(Number(e.target.value))}
                    className="w-full"
                  />
                </label>
                <label className="mt-2 flex items-center gap-2" htmlFor="story-rotation">
                  <span className="w-14 flex-none font-meta text-xs uppercase tracking-wide text-muted">Drehen</span>
                  <input
                    id="story-rotation"
                    type="range"
                    min={-180}
                    max={180}
                    step={1}
                    value={transform.rotationDeg}
                    onChange={(e) => handleRotationChange(Number(e.target.value))}
                    className="w-full"
                  />
                </label>
              </>
            )}

            <label className="mt-3 block font-meta text-xs uppercase tracking-wide text-muted" htmlFor="story-text">
              Text (optional)
            </label>
            <textarea
              id="story-text"
              value={text}
              onChange={(e) => setText(e.target.value.slice(0, TEXT_MAX_LENGTH))}
              maxLength={TEXT_MAX_LENGTH}
              rows={2}
              className="mt-1 w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
            />
            <div className="mt-1 text-right font-meta text-xs text-muted">
              {text.length}/{TEXT_MAX_LENGTH}
            </div>
          </>
        )}

        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) pickFile(file);
            if (inputRef.current) inputRef.current.value = "";
          }}
          className="hidden"
        />

        {error && <p className="mt-2 font-meta text-xs text-accent">{error}</p>}

        <div className="mt-4 flex justify-end gap-3">
          <button type="button" onClick={reset} className="font-meta text-sm text-muted hover:text-accent">
            Abbrechen
          </button>
          <button
            type="button"
            onClick={submit}
            disabled={!imageUrl || !transform || pending}
            className="bg-accent px-5 py-2 font-meta text-sm text-accent-fg disabled:opacity-50"
          >
            {pending ? "Wird gepostet …" : "Posten"}
          </button>
        </div>
      </div>
    </div>
  );
}
