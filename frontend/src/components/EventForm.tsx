"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { createEventAction, updateEventAction } from "@/actions/events";
import { EntityPicker, toEntityRef, type EntityPickerValue } from "@/components/EntityPicker";
import { ImageUploadField } from "@/components/ImageUploadField";
import type { EventResponse } from "@/lib/types";

function emptyBand(): EntityPickerValue {
  return { name: "", city: "" };
}

export function EventForm({ eventId, initial }: { eventId?: number; initial?: EventResponse }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  const [title, setTitle] = useState(initial?.title ?? "");
  const [date, setDate] = useState(initial?.date ?? "");
  const [startTime, setStartTime] = useState(initial?.startTime?.slice(0, 5) ?? "");
  const [location, setLocation] = useState<EntityPickerValue>(
    initial ? { id: initial.location.id, name: initial.location.name, city: initial.location.city } : { name: "", city: "" }
  );
  const [bands, setBands] = useState<EntityPickerValue[]>(
    initial && initial.bands.length > 0
      ? initial.bands.map((b) => ({ id: b.id, name: b.name, city: b.city ?? "" }))
      : [emptyBand()]
  );
  const [ticketUrl, setTicketUrl] = useState(initial?.ticketUrl ?? "");
  const [titleImageUrl, setTitleImageUrl] = useState(initial?.titleImageUrl ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!location.id && (!location.name.trim() || !location.city.trim())) {
      setError("Bitte eine Location auswählen oder Name + Stadt für eine neue Location angeben.");
      return;
    }
    if (bands.some((b) => !b.id && !b.name.trim())) {
      setError("Bitte für jede Band einen Namen angeben oder eine bestehende auswählen.");
      return;
    }

    const input = {
      title: title.trim() || undefined,
      date,
      startTime: startTime || undefined,
      location: toEntityRef(location),
      bands: bands.map(toEntityRef),
      ticketUrl: ticketUrl.trim() || undefined,
      titleImageUrl: titleImageUrl.trim() || undefined,
      description: description.trim() || undefined,
    };

    startTransition(async () => {
      const result = eventId ? await updateEventAction(eventId, input) : await createEventAction(input);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      router.push(`/konzerte/${result.data.id}`);
    });
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-xl space-y-6">
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="font-meta text-sm text-muted" htmlFor="date">
            Datum
          </label>
          <input
            id="date"
            type="date"
            required
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
          />
        </div>
        <div>
          <label className="font-meta text-sm text-muted" htmlFor="startTime">
            Beginn
          </label>
          <input
            id="startTime"
            type="time"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
            className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
          />
        </div>
      </div>

      <EntityPicker entityType="LOCATION" label="Location" value={location} onChange={setLocation} showAddress cityRequired />

      <div className="space-y-3">
        <span className="font-meta text-sm text-muted">Bands</span>
        {bands.map((band, i) => (
          <div key={i} className="relative">
            <EntityPicker
              entityType="BAND"
              label={i === 0 ? "Band" : "Weitere Band"}
              value={band}
              onChange={(v) => setBands((prev) => prev.map((b, idx) => (idx === i ? v : b)))}
            />
            {bands.length > 1 && (
              <button
                type="button"
                onClick={() => setBands((prev) => prev.filter((_, idx) => idx !== i))}
                className="absolute right-3 top-3 font-meta text-xs text-muted hover:text-accent"
              >
                Entfernen
              </button>
            )}
          </div>
        ))}
        <button
          type="button"
          onClick={() => setBands((prev) => [...prev, emptyBand()])}
          className="font-meta text-sm text-accent hover:underline"
        >
          + weitere Band
        </button>
      </div>

      <div>
        <label className="font-meta text-sm text-muted" htmlFor="title">
          Veranstaltungsname (optional, z. B. bei Festivals)
        </label>
        <input
          id="title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
      </div>

      <div>
        <label className="font-meta text-sm text-muted" htmlFor="ticketUrl">
          Ticketlink
        </label>
        <input
          id="ticketUrl"
          type="url"
          value={ticketUrl}
          onChange={(e) => setTicketUrl(e.target.value)}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
      </div>

      <div>
        <span className="font-meta text-sm text-muted">Bild (optional)</span>
        <div className="mt-1">
          <ImageUploadField value={titleImageUrl} onChange={setTitleImageUrl} aspect="video" />
        </div>
      </div>

      <div>
        <label className="font-meta text-sm text-muted" htmlFor="description">
          Beschreibung
        </label>
        <textarea
          id="description"
          rows={4}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="mt-1 w-full border border-line bg-bg px-3 py-2 outline-none focus:border-accent"
        />
      </div>

      {error && <p className="font-meta text-sm text-accent">{error}</p>}

      <button
        type="submit"
        disabled={pending}
        className="bg-fg px-6 py-2.5 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg disabled:opacity-60"
      >
        {pending ? "Wird gespeichert …" : eventId ? "Speichern" : "Veröffentlichen"}
      </button>
    </form>
  );
}
