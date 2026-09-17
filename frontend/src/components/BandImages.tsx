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

export function BandLogo({ band }: { band: BandResponse }) {
  const [src, setSrc] = useState(band.logoUrl);
  const save = saveBandImage(band, "logoUrl");

  return (
    <PasteImageUpload
      src={src}
      placeholderName={band.name}
      className="h-16 w-16 flex-none border border-line"
      textClassName="text-2xl"
      onUpload={async (url) => {
        const result = await save(url);
        if (result.ok) setSrc(url);
        return result;
      }}
    />
  );
}

/** The band's dedicated profile picture (see StoryRing/BandStoryAvatarButton for where
 * everyone else sees it) - logoUrl is only its fallback, shown here too until the band sets
 * one of these explicitly, so a manager always sees exactly what a visitor would. */
export function BandProfileImage({ band }: { band: BandResponse }) {
  const [src, setSrc] = useState(band.profileImageUrl ?? band.logoUrl);
  const save = saveBandImage(band, "profileImageUrl");

  return (
    <PasteImageUpload
      src={src}
      placeholderName={band.name}
      className="h-16 w-16 flex-none border border-line"
      textClassName="text-2xl"
      onUpload={async (url) => {
        const result = await save(url);
        if (result.ok) setSrc(url);
        return result;
      }}
    />
  );
}
