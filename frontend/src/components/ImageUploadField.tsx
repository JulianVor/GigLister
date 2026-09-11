"use client";

import { useRef, useState, useTransition } from "react";
import { uploadImageAction } from "@/actions/uploads";

// Same GIGLISTER_UPLOAD_MAX_SIZE_MB the backend enforces (see .env.example) - baked into
// the browser bundle at build time via NEXT_PUBLIC_UPLOAD_MAX_SIZE_MB (docker-compose.yml/
// Dockerfile). Checked here, before the file is ever sent, because a file this large
// blows past the Server Action's own body-size limit first - that crashes with a raw
// Next.js error page instead of the backend's friendly "Datei ist zu groß" message, so
// it has to be caught client-side.
const MAX_FILE_SIZE_MB = Number(process.env.NEXT_PUBLIC_UPLOAD_MAX_SIZE_MB) || 5;
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

/** Images are always uploaded, never entered as an external URL by hand. */
export function ImageUploadField({
  value,
  onChange,
  aspect = "square",
}: {
  value: string;
  onChange: (url: string) => void;
  aspect?: "square" | "video";
}) {
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError(null);

    if (file.size > MAX_FILE_SIZE_BYTES) {
      setError(`Die Datei ist zu groß (maximal ${MAX_FILE_SIZE_MB} MB)`);
      if (inputRef.current) inputRef.current.value = "";
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    startTransition(async () => {
      const result = await uploadImageAction(formData);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      onChange(result.data.url);
    });
  }

  function handleRemove() {
    onChange("");
    setError(null);
    if (inputRef.current) inputRef.current.value = "";
  }

  return (
    <div className="flex items-start gap-3">
      {value && (
        <img
          src={value}
          alt=""
          className={`flex-none border border-line object-cover ${aspect === "square" ? "h-16 w-16" : "h-16 w-28"}`}
        />
      )}
      <div>
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          onChange={handleFileChange}
          disabled={pending}
          className="block font-meta text-sm disabled:opacity-60"
        />
        {pending && <p className="mt-1 font-meta text-xs text-muted">Wird hochgeladen …</p>}
        {error && <p className="mt-1 font-meta text-xs text-accent">{error}</p>}
        {value && !pending && (
          <button
            type="button"
            onClick={handleRemove}
            className="mt-1 font-meta text-xs text-muted underline hover:text-accent"
          >
            Entfernen
          </button>
        )}
      </div>
    </div>
  );
}
