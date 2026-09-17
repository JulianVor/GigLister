"use client";

import { useRef, useState, useTransition } from "react";
import { uploadImageAction } from "@/actions/uploads";
import { createBandStoryAction } from "@/actions/bands";
import { MAX_UPLOAD_SIZE_BYTES, MAX_UPLOAD_SIZE_MB } from "@/lib/upload";

const TEXT_MAX_LENGTH = 280;
// Same frame the viewer actually shows the story in (see BandStoryViewer) - the crop only
// reproduces faithfully if both sides agree on what shape "the frame" is.
const FRAME_ASPECT = 9 / 16;
const MAX_ZOOM = 3;

interface Crop {
  widthPct: number;
  heightPct: number;
  leftPct: number;
  topPct: number;
}

/** The size (as % of the frame) at which an image of this aspect ratio exactly covers the
 * frame with no gaps - the zoom=1 baseline everything else scales up from. */
function coverSize(imgAspect: number): { widthPct: number; heightPct: number } {
  if (imgAspect > FRAME_ASPECT) {
    return { widthPct: (imgAspect / FRAME_ASPECT) * 100, heightPct: 100 };
  }
  return { widthPct: 100, heightPct: (FRAME_ASPECT / imgAspect) * 100 };
}

function clampOffset(offset: number, sizePct: number): number {
  return Math.min(0, Math.max(100 - sizePct, offset));
}

function centeredCrop(imgAspect: number, zoom: number): Crop {
  const base = coverSize(imgAspect);
  const widthPct = base.widthPct * zoom;
  const heightPct = base.heightPct * zoom;
  return {
    widthPct,
    heightPct,
    leftPct: clampOffset((100 - widthPct) / 2, widthPct),
    topPct: clampOffset((100 - heightPct) / 2, heightPct),
  };
}

/** Drag-to-position, slider-to-zoom editor for fitting a photo into the 9:16 story frame
 * before posting - same "move and scale" step Instagram/WhatsApp give you, because a band's
 * source image (a flyer, a landscape photo, whatever) rarely already has the story's own
 * aspect ratio. Purely a positioning tool: the source image is never modified, only the four
 * numbers describing where it sits get saved (see CroppedStoryImage, which reproduces this
 * exact same view everywhere the story is then shown). */
function StoryCropEditor({ imageUrl, crop, onCropChange }: { imageUrl: string; crop: Crop; onCropChange: (crop: Crop) => void }) {
  const frameRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef({ active: false, startClientX: 0, startClientY: 0, startLeft: 0, startTop: 0 });

  function onPointerDown(e: React.PointerEvent) {
    e.currentTarget.setPointerCapture(e.pointerId);
    dragRef.current = { active: true, startClientX: e.clientX, startClientY: e.clientY, startLeft: crop.leftPct, startTop: crop.topPct };
  }

  function onPointerMove(e: React.PointerEvent) {
    if (!dragRef.current.active || !frameRef.current) return;
    const rect = frameRef.current.getBoundingClientRect();
    const dxPct = ((e.clientX - dragRef.current.startClientX) / rect.width) * 100;
    const dyPct = ((e.clientY - dragRef.current.startClientY) / rect.height) * 100;
    onCropChange({
      ...crop,
      leftPct: clampOffset(dragRef.current.startLeft + dxPct, crop.widthPct),
      topPct: clampOffset(dragRef.current.startTop + dyPct, crop.heightPct),
    });
  }

  function onPointerUp() {
    dragRef.current.active = false;
  }

  return (
    <div
      ref={frameRef}
      className="relative mt-3 aspect-[9/16] w-full touch-none overflow-hidden border border-line bg-black"
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
        style={{ width: `${crop.widthPct}%`, height: `${crop.heightPct}%`, left: `${crop.leftPct}%`, top: `${crop.topPct}%` }}
      />
    </div>
  );
}

/** "Status posten" - the entry point a band's own manager uses to publish a new 24h status
 * (see BandStoryAvatarButton/BandStoryViewer for how everyone else then sees it). Upload a
 * photo, position/zoom it to fit the frame, optionally caption it, then post - unlike
 * PasteImageUpload elsewhere, the image alone isn't persisted onto anything until "Posten"
 * (there's nothing sensible to save it onto before the story itself exists). */
export function BandStoryComposer({ bandId }: { bandId: number }) {
  const [open, setOpen] = useState(false);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [imgAspect, setImgAspect] = useState<number | null>(null);
  const [zoom, setZoom] = useState(1);
  const [crop, setCrop] = useState<Crop | null>(null);
  const [text, setText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [pending, startTransition] = useTransition();
  const inputRef = useRef<HTMLInputElement>(null);

  function reset() {
    setOpen(false);
    setImageUrl(null);
    setImgAspect(null);
    setZoom(1);
    setCrop(null);
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
      setUploading(false);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      const url = result.data.url;
      const probe = new Image();
      probe.onload = () => {
        const aspect = probe.naturalWidth / probe.naturalHeight;
        setImgAspect(aspect);
        setZoom(1);
        setCrop(centeredCrop(aspect, 1));
        setImageUrl(url);
      };
      probe.src = url;
    });
  }

  function handleZoomChange(newZoom: number) {
    setZoom(newZoom);
    if (imgAspect == null || !crop) return;
    const base = coverSize(imgAspect);
    const widthPct = base.widthPct * newZoom;
    const heightPct = base.heightPct * newZoom;
    // Keep whatever's currently centered in the frame centered after rescaling, instead of
    // always re-centering on the image's own middle.
    const centerFracX = (50 - crop.leftPct) / crop.widthPct;
    const centerFracY = (50 - crop.topPct) / crop.heightPct;
    setCrop({
      widthPct,
      heightPct,
      leftPct: clampOffset(50 - centerFracX * widthPct, widthPct),
      topPct: clampOffset(50 - centerFracY * heightPct, heightPct),
    });
  }

  function submit() {
    if (!imageUrl || !crop) return;
    setError(null);
    startTransition(async () => {
      const result = await createBandStoryAction(bandId, {
        imageUrl,
        text: text.trim() || undefined,
        imgWidthPct: crop.widthPct,
        imgHeightPct: crop.heightPct,
        imgOffsetLeftPct: crop.leftPct,
        imgOffsetTopPct: crop.topPct,
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

        {!imageUrl || !crop ? (
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
            <StoryCropEditor imageUrl={imageUrl} crop={crop} onCropChange={setCrop} />
            <p className="mt-1 font-meta text-xs text-muted">Ziehen zum Verschieben</p>
            <input
              type="range"
              min={1}
              max={MAX_ZOOM}
              step={0.01}
              value={zoom}
              onChange={(e) => handleZoomChange(Number(e.target.value))}
              aria-label="Zoom"
              className="mt-2 w-full"
            />

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
            disabled={!imageUrl || !crop || pending}
            className="bg-accent px-5 py-2 font-meta text-sm text-accent-fg disabled:opacity-50"
          >
            {pending ? "Wird gepostet …" : "Posten"}
          </button>
        </div>
      </div>
    </div>
  );
}
