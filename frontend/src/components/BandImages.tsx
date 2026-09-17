"use client";

import { useState } from "react";
import { updateBandAction } from "@/actions/bands";
import { PasteImageUpload } from "@/components/PasteImageUpload";
import type { BandResponse } from "@/lib/types";

/** Persists a single changed image field via the same full-replace updateBandAction the
 * edit form uses, carrying every other current field along unchanged. */
function saveBandImage(band: BandResponse, field: "titleImageUrl" | "logoUrl" | "profileImageUrl") {
  return (url: string) =>
    updateBandAction(band.id, {
      name: band.name,
      city: band.city ?? undefined,
      country: band.country ?? undefined,
      shortDescription: band.shortDescription ?? undefined,
      website: band.website ?? undefined,
      logoUrl: field === "logoUrl" ? url : (band.logoUrl ?? undefined),
      titleImageUrl: field === "titleImageUrl" ? url : (band.titleImageUrl ?? undefined),
      profileImageUrl: field === "profileImageUrl" ? url : (band.profileImageUrl ?? undefined),
      genres: band.genres,
    });
}

export function BandTitleImage({ band }: { band: BandResponse }) {
  const [src, setSrc] = useState(band.titleImageUrl);
  const save = saveBandImage(band, "titleImageUrl");

  return (
    <PasteImageUpload
      src={src}
      placeholderName={band.name}
      className="mb-6 aspect-video w-full border border-line"
      textClassName="text-6xl sm:text-7xl"
      onUpload={async (url) => {
        const result = await save(url);
        if (result.ok) setSrc(url);
        return result;
      }}
    />
  );
}
