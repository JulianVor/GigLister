"use client";

import Link from "next/link";
import { useState } from "react";
import { logoutAction } from "@/actions/auth";

// Same pattern as EntityPicker: an absolute URL the browser can reach directly,
// distinct from the server-only API_BASE_URL used for Docker-internal calls.
const PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** Bundles the account-level links (Verwaltung, Einstellungen, App, Admin, Abmelden) behind
 * one dropdown instead of six separate items competing for header space - same
 * click-to-toggle popover LocationPicker already uses, for a consistent header. */
export function UserMenu({ username, isAdmin }: { username: string; isAdmin: boolean }) {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="font-meta text-sm tracking-wide hover:text-accent"
      >
        {username} <span aria-hidden>▾</span>
      </button>

      {open && (
        // Same z-index floor as LocationPicker's own panel - Leaflet's panes/controls
        // (see ConcertMap) reach z-index 1000 without a stacking context of their own.
        <div className="absolute right-0 z-[1010] mt-2 min-w-[11rem] border border-line bg-surface py-1 shadow-lg">
          <Link
            href="/verwaltung"
            onClick={() => setOpen(false)}
            className="block px-4 py-2 font-meta text-sm hover:bg-bg hover:text-accent"
          >
            Verwaltung
          </Link>
          <Link
            href="/einstellungen"
            onClick={() => setOpen(false)}
            className="block px-4 py-2 font-meta text-sm hover:bg-bg hover:text-accent"
          >
            Einstellungen
          </Link>
          <a
            href={`${PUBLIC_API_URL}/api/app/download`}
            onClick={() => setOpen(false)}
            className="block px-4 py-2 font-meta text-sm hover:bg-bg hover:text-accent"
          >
            App herunterladen
          </a>
          {isAdmin && (
            <Link
              href="/admin"
              onClick={() => setOpen(false)}
              className="block px-4 py-2 font-meta text-sm hover:bg-bg hover:text-accent"
            >
              Admin
            </Link>
          )}
          <form action={logoutAction}>
            <button
              type="submit"
              className="block w-full px-4 py-2 text-left font-meta text-sm text-muted hover:bg-bg hover:text-accent"
            >
              Abmelden
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
