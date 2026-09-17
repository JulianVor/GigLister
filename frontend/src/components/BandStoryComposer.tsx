"use client";

import { useRef, useState, useTransition } from "react";
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

// Loose safety bound only - keeps a wild drag from losing the image entirely off-frame.
// Not "always cover the frame" anymore: gaps are fine now, imgBackgroundColor fills them.
function clampCenter(pct: number): number {
  return Math.min(150, Math.max(-50, pct));
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

/** Drag-to-position, slider-to-zoom-and-rotate editor for fitting a photo into the 9:16 story
 * frame before posting - same "move and scale" step Instagram/WhatsApp give you, because a
 * band's source image (a flyer, a landscape photo, whatever) rarely already has the story's
 * own aspect ratio. Purely a positioning tool: the source image is never modified, only a few
 * numbers describing where it sits get saved (see CroppedStoryImage, which reproduces this
 * exact same view everywhere the story is then shown). */
function StoryCropEditor({
  imageUrl,
  transform,
  onTransformChange,
  bgColor,
}: {
  imageUrl: string;
  transform: Transform;
  onTransformChange: (transform: Transform) => void;
  bgColor: string | null;
}) {
  const frameRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef({ active: false, startClientX: 0, startClientY: 0, startCenterX: 0, startCenterY: 0 });

  function onPointerDown(e: React.PointerEvent) {
    e.currentTarget.setPointerCapture(e.pointerId);
    dragRef.current = {
      active: true,
      startClientX: e.clientX,
      startClientY: e.clientY,
      startCenterX: transform.centerXPct,
      startCenterY: transform.centerYPct,
    };
  }

  function onPointerMove(e: React.PointerEvent) {
    if (!dragRef.current.active || !frameRef.current) return;
    const rect = frameRef.current.getBoundingClientRect();
    const dxPct = ((e.clientX - dragRef.current.startClientX) / rect.width) * 100;
    const dyPct = ((e.clientY - dragRef.current.startClientY) / rect.height) * 100;
    onTransformChange({
      ...transform,
      centerXPct: clampCenter(dragRef.current.startCenterX + dxPct),
      centerYPct: clampCenter(dragRef.current.startCenterY + dyPct),
    });
  }

  function onPointerUp() {
    dragRef.current.active = false;
  }

  return (
    <div
      ref={frameRef}
      className="relative mt-3 aspect-[9/16] w-full touch-none overflow-hidden border border-line"
      style={{ backgroundColor: bgColor ?? FALLBACK_BG }}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerLeave={onPointerUp}
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
  const [scale, setScale] = useState(MIN_SCALE);
  const [transform, setTransform] = useState<Transform | null>(null);
  const [bgColor, setBgColor] = useState<string | null>(null);
  const [text, setText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [pending, startTransition] = useTransition();
  const inputRef = useRef<HTMLInputElement>(null);

  function reset() {
    setOpen(false);
    setImageUrl(null);
    setImgAspect(null);
    setScale(MIN_SCALE);
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
        setScale(MIN_SCALE);
        setTransform({ widthPct: size.widthPct, heightPct: size.heightPct, centerXPct: 50, centerYPct: 50, rotationDeg: 0 });
        setImageUrl(url);
        setUploading(false);
        setBgColor(await sampleAverageColor(url));
      };
      probe.src = url;
    });
  }

  function handleScaleChange(newScale: number) {
    setScale(newScale);
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

        {!imageUrl || !transform ? (
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
            <StoryCropEditor imageUrl={imageUrl} transform={transform} onTransformChange={setTransform} bgColor={bgColor} />
            <p className="mt-1 font-meta text-xs text-muted">Ziehen zum Verschieben</p>

            <label className="mt-2 flex items-center gap-2" htmlFor="story-zoom">
              <span className="w-14 flex-none font-meta text-xs uppercase tracking-wide text-muted">Zoom</span>
              <input
                id="story-zoom"
                type="range"
                min={MIN_SCALE}
                max={MAX_SCALE}
                step={0.01}
                value={scale}
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
