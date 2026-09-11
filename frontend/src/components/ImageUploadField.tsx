"use client";

import { useRef, useState, useTransition } from "react";
import { uploadImageAction } from "@/actions/uploads";

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
