"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { createEventSeriesAction, updateEventSeriesAction } from "@/actions/eventSeries";
import { ImageUploadField } from "@/components/ImageUploadField";
import type { EventSeriesResponse } from "@/lib/types";

export function EventSeriesForm({ series }: { series?: EventSeriesResponse }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState(series?.name ?? "");
  const [description, setDescription] = useState(series?.description ?? "");
  const [titleImageUrl, setTitleImageUrl] = useState(series?.titleImageUrl ?? "");
  const [ticketUrl, setTicketUrl] = useState(series?.ticketUrl ?? "");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const input = {
      name,
      description: description || undefined,
      titleImageUrl: titleImageUrl || undefined,
      ticketUrl: ticketUrl || undefined,
    };

    startTransition(async () => {
      if (series) {
        const result = await updateEventSeriesAction(series.id, input);
        if (!result.ok) {
          setError(result.error);
          return;
        }
        router.push(`/reihen/${series.id}`);
        router.refresh();
        return;
      }

      const result = await createEventSeriesAction(input);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      router.push(`/reihen/${result.data.id}`);
      router.refresh();
    });
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-xl space-y-4">
      <Field label="Name">
        <input value={name} onChange={(e) => setName(e.target.value)} required className="input" />
      </Field>
      <Field label="Beschreibung">
        <textarea rows={4} value={description} onChange={(e) => setDescription(e.target.value)} className="input" />
      </Field>
      <Field label="Ticketlink (für die ganze Reihe, falls es ein gemeinsames Ticket gibt)">
        <input type="url" value={ticketUrl} onChange={(e) => setTicketUrl(e.target.value)} className="input" />
      </Field>
      <Field label="Titelbild">
        <ImageUploadField value={titleImageUrl} onChange={setTitleImageUrl} aspect="video" />
      </Field>

      {error && <p className="font-meta text-sm text-accent">{error}</p>}

      <button
        type="submit"
        disabled={pending}
        className="bg-fg px-6 py-2.5 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {pending ? "Wird gespeichert …" : series ? "Speichern" : "Reihe anlegen"}
      </button>
    </form>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="font-meta text-sm text-muted">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}
