"use client";

import { useState } from "react";
import { updateLocationAction } from "@/actions/locations";
import { PasteImageUpload } from "@/components/PasteImageUpload";
import type { LocationResponse } from "@/lib/types";

/** Same idea as BandTitleImage - persists via the same full-replace updateLocationAction
 * the edit form uses, carrying every other current field along unchanged. */
export function LocationTitleImage({ location }: { location: LocationResponse }) {
  const [src, setSrc] = useState(location.titleImageUrl);

  async function save(url: string) {
    const result = await updateLocationAction(location.id, {
      name: location.name,
      city: location.city,
      address: location.address ?? undefined,
      postalCode: location.postalCode ?? undefined,
      country: location.country ?? undefined,
      website: location.website ?? undefined,
      logoUrl: location.logoUrl ?? undefined,
      titleImageUrl: url,
      latitude: location.latitude ?? undefined,
      longitude: location.longitude ?? undefined,
    });
    if (result.ok) setSrc(url);
    return result;
  }

  return (
    <PasteImageUpload
      src={src}
      placeholderName={location.name}
      className="mb-6 aspect-video w-full border border-line"
      textClassName="text-6xl sm:text-7xl"
      onUpload={save}
    />
  );
}
