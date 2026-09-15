"use client";

import Link from "next/link";
import { useState } from "react";
import { logoutAction } from "@/actions/auth";

/** Bundles the account-level links (Verwaltung, Einstellungen, Admin, Abmelden) behind
 * one dropdown instead of five separate items competing for header space - same
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
        <div className="absolute right-0 z-20 mt-2 min-w-[11rem] border border-line bg-surface py-1 shadow-lg">
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
