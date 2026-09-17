"use client";

import { useRef, useState, useTransition } from "react";
import { uploadImageAction } from "@/actions/uploads";
import { EntityPlaceholder } from "@/components/EntityPlaceholder";
import { MAX_UPLOAD_SIZE_BYTES, MAX_UPLOAD_SIZE_MB } from "@/lib/upload";

/** An image box (banner or logo) that lets anyone with edit rights on the entity replace it
 * right from its own page - paste (Strg+V) or click to browse - without going to
 * "Bearbeiten" first. Only handles getting a new file onto the server; persisting the
 * resulting URL onto the entity is the caller's job (`onUpload`, usually a bound call to
 * the same update action the edit form already uses, with just this one field changed) -
 * that keeps this component entity-agnostic (Band vs. Location vs. whatever comes next). */
export function PasteImageUpload({
  src,
  placeholderName,
  className,
  textClassName,
  fit = "cover",
  onUpload,
}: {
  src: string | null;
  placeholderName: string;
  className: string;
  textClassName?: string;
  /** "contain" for a logo mark being shown as a stand-in preview (see BandProfileImage's
   * logoUrl fallback) - cropping a logo via "cover" looks broken, so it gets breathing room
   * on a bg-surface backdrop instead, same treatment logos get everywhere else on the site. */
  fit?: "cover" | "contain";
  onUpload: (url: string) => Promise<{ ok: boolean; error?: string }>;
}) {
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  function upload(file: File) {
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

    startTransition(async () => {
      const uploadResult = await uploadImageAction(formData);
      if (!uploadResult.ok) {
        setError(uploadResult.error);
        return;
      }
      const saveResult = await onUpload(uploadResult.data.url);
      if (!saveResult.ok) {
        setError(saveResult.error ?? "Speichern fehlgeschlagen.");
      }
    });
  }

  function handlePaste(e: React.ClipboardEvent<HTMLDivElement>) {
    const item = Array.from(e.clipboardData.items).find((i) => i.type.startsWith("image/"));
    const file = item?.getAsFile();
    if (!file) return;
    e.preventDefault();
    upload(file);
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) upload(file);
    if (inputRef.current) inputRef.current.value = "";
  }

  return (
    <div className="h-full w-full">
      <div
        tabIndex={0}
        role="button"
        aria-label="Bild ändern - klicken oder einfügen mit Strg+V"
        onPaste={handlePaste}
        className={`group relative block cursor-pointer overflow-hidden outline-none focus:ring-2 focus:ring-accent ${className}`}
      >
        {src ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={src}
            alt=""
            className={fit === "contain" ? "h-full w-full bg-surface object-contain p-2" : "h-full w-full object-cover"}
          />
        ) : (
          <EntityPlaceholder name={placeholderName} className="h-full w-full" textClassName={textClassName} />
        )}
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 bg-bg/80 p-1 text-center opacity-0 transition-opacity group-hover:opacity-100 group-focus:opacity-100">
          <span className="font-meta text-xs leading-tight text-fg">
            {pending ? "Wird hochgeladen …" : "Bild einfügen (Strg+V)"}
          </span>
          {!pending && (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                inputRef.current?.click();
              }}
              className="font-meta text-xs text-accent underline"
            >
              Datei wählen
            </button>
          )}
        </div>
      </div>
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        onChange={handleFileChange}
        className="hidden"
      />
      {error && <p className="mt-1 font-meta text-xs text-accent">{error}</p>}
    </div>
  );
}
