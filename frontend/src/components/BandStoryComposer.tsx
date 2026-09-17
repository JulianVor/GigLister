"use client";

import { useRef, useState, useTransition } from "react";
import { uploadImageAction } from "@/actions/uploads";
import { createBandStoryAction } from "@/actions/bands";
import { MAX_UPLOAD_SIZE_BYTES, MAX_UPLOAD_SIZE_MB } from "@/lib/upload";

const TEXT_MAX_LENGTH = 280;

/** "Status posten" - the entry point a band's own manager uses to publish a new 24h status
 * (see BandStoryAvatarButton/BandStoryViewer for how everyone else then sees it). Two steps
 * in one small panel: upload a photo, then optionally caption it before actually posting -
 * unlike PasteImageUpload elsewhere, the image alone isn't persisted anywhere until "Posten"
 * (there's nothing sensible to save it onto before the story itself exists). */
export function BandStoryComposer({ bandId }: { bandId: number }) {
  const [open, setOpen] = useState(false);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [text, setText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [pending, startTransition] = useTransition();
  const inputRef = useRef<HTMLInputElement>(null);

  function reset() {
    setOpen(false);
    setImageUrl(null);
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
      setImageUrl(result.data.url);
    });
  }

  function submit() {
    if (!imageUrl) return;
    setError(null);
    startTransition(async () => {
      const result = await createBandStoryAction(bandId, { imageUrl, text: text.trim() || undefined });
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

        {!imageUrl ? (
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
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={imageUrl} alt="" className="mt-3 aspect-[9/16] w-full border border-line object-cover" />
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
            disabled={!imageUrl || pending}
            className="bg-accent px-5 py-2 font-meta text-sm text-accent-fg disabled:opacity-50"
          >
            {pending ? "Wird gepostet …" : "Posten"}
          </button>
        </div>
      </div>
    </div>
  );
}
