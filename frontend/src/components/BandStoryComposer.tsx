"use client";

import { useRef, useState, useSyncExternalStore, useTransition } from "react";
import { uploadImageAction } from "@/actions/uploads";
import { createBandStoryAction } from "@/actions/bands";
import { MAX_UPLOAD_SIZE_BYTES, MAX_UPLOAD_SIZE_MB } from "@/lib/upload";
import { createTextLayer, serializeTextLayers, TEXT_LAYER_BASE_FONT_CQW, type TextLayer } from "@/lib/storyTextLayers";

const TEXT_MAX_LENGTH = 200;
// Same frame the viewer actually shows the story in (see BandStoryViewer) - the crop only
// reproduces faithfully if both sides agree on what shape "the frame" is.
const FRAME_ASPECT = 9 / 16;
// scale=1 always means "the whole photo is visible" (fit) - there's no forced minimum crop
// anymore, per the band's own request: zooming out all the way should show the entire image,
// with whatever's left of the frame filled by imgBackgroundColor rather than always cropping.
const MIN_PHOTO_SCALE = 1;
const MAX_PHOTO_SCALE = 5;
const MIN_TEXT_SCALE = 0.4;
const MAX_TEXT_SCALE = 4;
const FALLBACK_BG = "#111111";

interface PhotoTransform {
  widthPct: number;
  heightPct: number;
  centerXPct: number;
  centerYPct: number;
  rotationDeg: number;
}

/** Center/scale/rotation, the shape every draggable thing in the editor reduces to - the
 * photo (scale derived from its width vs. the "whole photo visible" baseline) and every text
 * layer (scale stored directly) alike, so the drag/pinch/rotate gesture and the Zoom/Drehen
 * sliders only ever need to know about this one shape, never which kind of object it came
 * from (see StoryCropEditor/useLayerGesture). */
interface NormalizedTransform {
  centerXPct: number;
  centerYPct: number;
  scale: number;
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

function scaleOfPhoto(transform: PhotoTransform, imgAspect: number): number {
  return transform.widthPct / fitSize(imgAspect).widthPct;
}

// Loose safety bound only - keeps a wild drag/pinch from losing an object entirely off-frame.
// Not "always cover the frame": gaps are fine now, imgBackgroundColor fills them for the photo,
// and a text layer never needs to "cover" anything to begin with.
function clampCenter(pct: number): number {
  return Math.min(150, Math.max(-50, pct));
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

function distance(a: Point, b: Point): number {
  return Math.hypot(b.x - a.x, b.y - a.y);
}

function angleDeg(a: Point, b: Point): number {
  return (Math.atan2(b.y - a.y, b.x - a.x) * 180) / Math.PI;
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

/** Drag/pinch/rotate for one object (the photo, or one text layer) against a shared frame -
 * factored out so the photo and every text layer drive the exact same gesture math, just
 * pointed at a different NormalizedTransform getter/setter. One finger pans; two fingers
 * pinch-zoom and rotate at once, anchored to the pinch's own midpoint. Also reports a plain
 * tap (pointer released with barely any movement, never a second finger) via `onTap` - how a
 * text layer knows "select and start editing" versus "I just got dragged". */
function useLayerGesture({
  frameRef,
  getTransform,
  setTransform,
  minScale,
  maxScale,
  onTap,
}: {
  frameRef: React.RefObject<HTMLDivElement | null>;
  getTransform: () => NormalizedTransform;
  setTransform: (t: NormalizedTransform) => void;
  minScale: number;
  maxScale: number;
  onTap?: () => void;
}) {
  const pointersRef = useRef<Map<number, Point>>(new Map());
  const gestureRef = useRef({
    transform: getTransform(),
    singleStart: { x: 0, y: 0 },
    movement: 0,
    distance: 0,
    angle: 0,
    midpoint: { x: 0, y: 0 },
  });

  function startGesture() {
    const points = [...pointersRef.current.values()];
    gestureRef.current.transform = getTransform();
    if (points.length === 1) {
      gestureRef.current.singleStart = points[0];
      gestureRef.current.movement = 0;
    } else if (points.length === 2) {
      gestureRef.current.distance = distance(points[0], points[1]);
      gestureRef.current.angle = angleDeg(points[0], points[1]);
      gestureRef.current.midpoint = { x: (points[0].x + points[1].x) / 2, y: (points[0].y + points[1].y) / 2 };
    }
  }

  function onPointerDown(e: React.PointerEvent) {
    // Stops the frame's own background gesture (which would otherwise re-select "photo") from
    // also firing for the same touch when this is a text layer.
    e.stopPropagation();
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {
      // Guarded: an invalid/already-released pointer id shouldn't abort tracking it below.
    }
    pointersRef.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    startGesture();
  }

  function onPointerMove(e: React.PointerEvent) {
    if (!pointersRef.current.has(e.pointerId) || !frameRef.current) return;
    pointersRef.current.set(e.pointerId, { x: e.clientX, y: e.clientY });
    const points = [...pointersRef.current.values()];
    const rect = frameRef.current.getBoundingClientRect();
    const start = gestureRef.current;

    if (points.length === 2) {
      const newDistance = distance(points[0], points[1]);
      const newAngle = angleDeg(points[0], points[1]);
      const newMidpoint = { x: (points[0].x + points[1].x) / 2, y: (points[0].y + points[1].y) / 2 };
      const scaleRatio = start.distance > 0 ? newDistance / start.distance : 1;
      const dxPct = ((newMidpoint.x - start.midpoint.x) / rect.width) * 100;
      const dyPct = ((newMidpoint.y - start.midpoint.y) / rect.height) * 100;
      setTransform({
        scale: clamp(start.transform.scale * scaleRatio, minScale, maxScale),
        rotationDeg: start.transform.rotationDeg + (newAngle - start.angle),
        centerXPct: clampCenter(start.transform.centerXPct + dxPct),
        centerYPct: clampCenter(start.transform.centerYPct + dyPct),
      });
    } else if (points.length === 1) {
      const dxPct = ((points[0].x - start.singleStart.x) / rect.width) * 100;
      const dyPct = ((points[0].y - start.singleStart.y) / rect.height) * 100;
      gestureRef.current.movement = Math.max(gestureRef.current.movement, distance(points[0], start.singleStart));
      setTransform({
        ...start.transform,
        centerXPct: clampCenter(start.transform.centerXPct + dxPct),
        centerYPct: clampCenter(start.transform.centerYPct + dyPct),
      });
    }
  }

  function onPointerUpOrCancel(e: React.PointerEvent) {
    e.stopPropagation();
    const wasTap = pointersRef.current.size === 1 && gestureRef.current.movement < 6;
    pointersRef.current.delete(e.pointerId);
    startGesture();
    if (wasTap) onTap?.();
  }

  return {
    onPointerDown,
    onPointerMove,
    onPointerUp: onPointerUpOrCancel,
    onPointerCancel: onPointerUpOrCancel,
    onPointerLeave: onPointerUpOrCancel,
  };
}

/** One text layer inside the crop editor - a draggable/pinchable/rotatable label, tap to
 * (re)select and start typing, drag to reposition (which selects it too, but doesn't open the
 * keyboard). Only ever renders its own <input> while `editing`; otherwise it's the same static
 * text CroppedStoryImage would show, just with a selection outline when it's the active target
 * of the Zoom/Drehen sliders. */
function TextLayerOverlay({
  frameRef,
  layer,
  selected,
  editing,
  onSelect,
  onChange,
  onCommitText,
  onStartEditing,
  onStopEditing,
}: {
  frameRef: React.RefObject<HTMLDivElement | null>;
  layer: TextLayer;
  selected: boolean;
  editing: boolean;
  onSelect: () => void;
  onChange: (t: NormalizedTransform) => void;
  onCommitText: (text: string) => void;
  onStartEditing: () => void;
  onStopEditing: () => void;
}) {
  const gesture = useLayerGesture({
    frameRef,
    getTransform: () => ({ centerXPct: layer.centerXPct, centerYPct: layer.centerYPct, scale: layer.scale, rotationDeg: layer.rotationDeg }),
    setTransform: onChange,
    minScale: MIN_TEXT_SCALE,
    maxScale: MAX_TEXT_SCALE,
    onTap: () => {
      onSelect();
      onStartEditing();
    },
  });

  const style: React.CSSProperties = {
    left: `${layer.centerXPct}%`,
    top: `${layer.centerYPct}%`,
    fontSize: `${layer.scale * TEXT_LAYER_BASE_FONT_CQW}cqw`,
    transform: `translate(-50%, -50%) rotate(${layer.rotationDeg}deg)`,
  };

  if (editing) {
    return (
      <input
        autoFocus
        value={layer.text}
        onChange={(e) => onCommitText(e.target.value.slice(0, TEXT_MAX_LENGTH))}
        onBlur={onStopEditing}
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            e.currentTarget.blur();
          }
        }}
        onPointerDown={(e) => e.stopPropagation()}
        placeholder="Text"
        className="absolute border-b border-white/70 bg-transparent text-center font-display font-bold leading-tight text-white outline-none placeholder:text-white/50"
        style={{ ...style, width: "min(85%, 12em)" }}
      />
    );
  }

  return (
    <p
      className={`absolute max-w-[85%] cursor-move touch-none select-none whitespace-pre-wrap break-words text-center font-display font-bold leading-tight text-white [text-shadow:0_1px_6px_rgba(0,0,0,0.6)] ${
        selected ? "outline outline-2 outline-dashed outline-offset-4 outline-white/80" : ""
      }`}
      style={style}
      onPointerDown={(e) => {
        onSelect();
        gesture.onPointerDown(e);
      }}
      onPointerMove={gesture.onPointerMove}
      onPointerUp={gesture.onPointerUp}
      onPointerCancel={gesture.onPointerCancel}
      onPointerLeave={gesture.onPointerLeave}
    >
      {layer.text}
    </p>
  );
}

/** Drag-to-position, pinch-to-zoom, two-finger-to-rotate editor for fitting a photo (and any
 * number of text layers on top of it) into the 9:16 story frame before posting - same gesture
 * a phone's own camera roll / Instagram gives you. Whichever object is currently selected
 * (the photo, or one text layer) is what the Zoom/Drehen sliders and a two-finger gesture
 * anywhere in the frame act on - see useLayerGesture/TextLayerOverlay. A mouse/trackpad can't
 * pinch, so the sliders stay the only way to zoom/rotate there; BandStoryComposer hides them
 * on touch-primary devices, where the gesture alone is enough. Purely a positioning tool: the
 * source image is never modified, only a few numbers describing where everything sits get
 * saved (see CroppedStoryImage, which reproduces this exact same view wherever the story is
 * then shown). */
function StoryCropEditor({
  imageUrl,
  imgAspect,
  transform,
  onTransformChange,
  bgColor,
  textLayers,
  selectedLayerId,
  editingLayerId,
  onSelectLayer,
  onTextLayerChange,
  onTextLayerCommitText,
  onStartEditing,
  onStopEditing,
}: {
  imageUrl: string;
  imgAspect: number;
  transform: PhotoTransform;
  onTransformChange: (transform: PhotoTransform) => void;
  bgColor: string | null;
  textLayers: TextLayer[];
  selectedLayerId: string;
  editingLayerId: string | null;
  onSelectLayer: (id: string) => void;
  onTextLayerChange: (id: string, t: NormalizedTransform) => void;
  onTextLayerCommitText: (id: string, text: string) => void;
  onStartEditing: (id: string) => void;
  onStopEditing: () => void;
}) {
  const frameRef = useRef<HTMLDivElement>(null);

  const photoGesture = useLayerGesture({
    frameRef,
    getTransform: () => ({
      centerXPct: transform.centerXPct,
      centerYPct: transform.centerYPct,
      scale: scaleOfPhoto(transform, imgAspect),
      rotationDeg: transform.rotationDeg,
    }),
    setTransform: (n) => {
      const size = sizeAtScale(imgAspect, n.scale);
      onTransformChange({ widthPct: size.widthPct, heightPct: size.heightPct, centerXPct: n.centerXPct, centerYPct: n.centerYPct, rotationDeg: n.rotationDeg });
    },
    minScale: MIN_PHOTO_SCALE,
    maxScale: MAX_PHOTO_SCALE,
  });

  return (
    <div
      ref={frameRef}
      className="relative mt-3 aspect-[9/16] w-full touch-none overflow-hidden border border-line"
      style={{ backgroundColor: bgColor ?? FALLBACK_BG, containerType: "inline-size" }}
      onPointerDown={(e) => {
        onSelectLayer("photo");
        photoGesture.onPointerDown(e);
      }}
      onPointerMove={photoGesture.onPointerMove}
      onPointerUp={photoGesture.onPointerUp}
      onPointerCancel={photoGesture.onPointerCancel}
      onPointerLeave={photoGesture.onPointerLeave}
    >
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={imageUrl}
        alt=""
        draggable={false}
        className="absolute max-w-none"
        style={{
          left: `${transform.centerXPct}%`,
          top: `${transform.centerYPct}%`,
          width: `${transform.widthPct}%`,
          height: `${transform.heightPct}%`,
          transform: `translate(-50%, -50%) rotate(${transform.rotationDeg}deg)`,
        }}
      />
      {textLayers.map((layer) => (
        <TextLayerOverlay
          key={layer.id}
          frameRef={frameRef}
          layer={layer}
          selected={selectedLayerId === layer.id}
          editing={editingLayerId === layer.id}
          onSelect={() => onSelectLayer(layer.id)}
          onChange={(next) => onTextLayerChange(layer.id, next)}
          onCommitText={(text) => onTextLayerCommitText(layer.id, text)}
          onStartEditing={() => onStartEditing(layer.id)}
          onStopEditing={onStopEditing}
        />
      ))}
    </div>
  );
}

/** "Status posten" - the entry point a band's own manager uses to publish a new 24h status
 * (see BandStoryAvatarButton/BandStoryViewer for how everyone else then sees it). Upload a
 * photo, position/zoom/rotate it (and any text layers on top) to fit the frame, then post -
 * unlike PasteImageUpload elsewhere, the image alone isn't persisted onto anything until
 * "Posten" (there's nothing sensible to save it onto before the story itself exists). */
export function BandStoryComposer({ bandId }: { bandId: number }) {
  const [open, setOpen] = useState(false);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [imgAspect, setImgAspect] = useState<number | null>(null);
  const [transform, setTransform] = useState<PhotoTransform | null>(null);
  const [bgColor, setBgColor] = useState<string | null>(null);
  const [textLayers, setTextLayers] = useState<TextLayer[]>([]);
  const [selectedLayerId, setSelectedLayerId] = useState("photo");
  const [editingLayerId, setEditingLayerId] = useState<string | null>(null);
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

  const selectedTextLayer = selectedLayerId !== "photo" ? (textLayers.find((l) => l.id === selectedLayerId) ?? null) : null;
  const currentScale =
    selectedLayerId === "photo" ? (imgAspect != null && transform ? scaleOfPhoto(transform, imgAspect) : MIN_PHOTO_SCALE) : (selectedTextLayer?.scale ?? 1);
  const currentRotation = selectedLayerId === "photo" ? (transform?.rotationDeg ?? 0) : (selectedTextLayer?.rotationDeg ?? 0);
  const sliderMin = selectedLayerId === "photo" ? MIN_PHOTO_SCALE : MIN_TEXT_SCALE;
  const sliderMax = selectedLayerId === "photo" ? MAX_PHOTO_SCALE : MAX_TEXT_SCALE;

  function reset() {
    setOpen(false);
    setImageUrl(null);
    setImgAspect(null);
    setTransform(null);
    setBgColor(null);
    setTextLayers([]);
    setSelectedLayerId("photo");
    setEditingLayerId(null);
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
        const size = sizeAtScale(aspect, MIN_PHOTO_SCALE);
        setImgAspect(aspect);
        setTransform({ widthPct: size.widthPct, heightPct: size.heightPct, centerXPct: 50, centerYPct: 50, rotationDeg: 0 });
        setImageUrl(url);
        setUploading(false);
        setBgColor(await sampleAverageColor(url));
      };
      probe.src = url;
    });
  }

  function addTextLayer() {
    const layer = createTextLayer();
    setTextLayers((prev) => [...prev, layer]);
    setSelectedLayerId(layer.id);
    setEditingLayerId(layer.id);
  }

  function updateTextLayer(id: string, next: NormalizedTransform) {
    setTextLayers((prev) => prev.map((l) => (l.id === id ? { ...l, ...next } : l)));
  }

  function commitTextLayerText(id: string, text: string) {
    setTextLayers((prev) => prev.map((l) => (l.id === id ? { ...l, text } : l)));
  }

  function stopEditingLayer() {
    const id = editingLayerId;
    if (!id) return;
    const layer = textLayers.find((l) => l.id === id);
    if (!layer || layer.text.trim().length === 0) {
      setTextLayers((prev) => prev.filter((l) => l.id !== id));
      setSelectedLayerId("photo");
    }
    setEditingLayerId(null);
  }

  function deleteSelectedTextLayer() {
    if (selectedLayerId === "photo") return;
    setTextLayers((prev) => prev.filter((l) => l.id !== selectedLayerId));
    setSelectedLayerId("photo");
    setEditingLayerId(null);
  }

  function handleScaleChange(newScale: number) {
    if (selectedLayerId === "photo") {
      if (imgAspect == null || !transform) return;
      const size = sizeAtScale(imgAspect, newScale);
      setTransform({ ...transform, widthPct: size.widthPct, heightPct: size.heightPct });
    } else if (selectedTextLayer) {
      updateTextLayer(selectedTextLayer.id, { ...selectedTextLayer, scale: newScale });
    }
  }

  function handleRotationChange(deg: number) {
    if (selectedLayerId === "photo") {
      if (!transform) return;
      setTransform({ ...transform, rotationDeg: deg });
    } else if (selectedTextLayer) {
      updateTextLayer(selectedTextLayer.id, { ...selectedTextLayer, rotationDeg: deg });
    }
  }

  function submit() {
    if (!imageUrl || !transform) return;
    setError(null);
    const layersJson = serializeTextLayers(textLayers);
    const plainText = textLayers
      .map((l) => l.text.trim())
      .filter(Boolean)
      .join(" · ")
      .slice(0, 280);
    startTransition(async () => {
      const result = await createBandStoryAction(bandId, {
        imageUrl,
        text: plainText || undefined,
        imgWidthPct: transform.widthPct,
        imgHeightPct: transform.heightPct,
        imgCenterXPct: transform.centerXPct,
        imgCenterYPct: transform.centerYPct,
        imgRotationDeg: transform.rotationDeg,
        imgBackgroundColor: bgColor ?? FALLBACK_BG,
        textLayersJson: layersJson,
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
              textLayers={textLayers}
              selectedLayerId={selectedLayerId}
              editingLayerId={editingLayerId}
              onSelectLayer={setSelectedLayerId}
              onTextLayerChange={updateTextLayer}
              onTextLayerCommitText={commitTextLayerText}
              onStartEditing={setEditingLayerId}
              onStopEditing={stopEditingLayer}
            />

            <div className="mt-1 flex items-center justify-between gap-2">
              <p className="font-meta text-xs text-muted">
                {isTouchPrimary ? "Ziehen zum Verschieben · zwei Finger zum Zoomen und Drehen" : "Ziehen zum Verschieben"}
              </p>
              <div className="flex flex-none gap-3">
                {selectedLayerId !== "photo" && (
                  <button type="button" onClick={deleteSelectedTextLayer} className="font-meta text-xs text-accent hover:underline">
                    Löschen
                  </button>
                )}
                <button type="button" onClick={addTextLayer} className="font-meta text-xs text-accent hover:underline">
                  + Text
                </button>
              </div>
            </div>

            {!isTouchPrimary && (
              <>
                <label className="mt-2 flex items-center gap-2" htmlFor="story-zoom">
                  <span className="w-14 flex-none font-meta text-xs uppercase tracking-wide text-muted">Zoom</span>
                  <input
                    id="story-zoom"
                    type="range"
                    min={sliderMin}
                    max={sliderMax}
                    step={0.01}
                    value={currentScale}
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
                    value={currentRotation}
                    onChange={(e) => handleRotationChange(Number(e.target.value))}
                    className="w-full"
                  />
                </label>
              </>
            )}
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
