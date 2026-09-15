import Link from "next/link";
import { getLocationPrefs } from "@/lib/location-prefs";
import { getSession } from "@/lib/session";
import { LocationPicker } from "./LocationPicker";
import { UserMenu } from "./UserMenu";

export async function Header() {
  const [session, prefs] = await Promise.all([getSession(), getLocationPrefs()]);

  return (
    <header className="border-b border-line">
      <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-4">
        <Link href="/" className="font-display text-xl tracking-tight">
          GIGLISTER
        </Link>

        {/* Entdecken is deliberately not here anymore - its content is the homepage's own
            feed now (see app/page.tsx), not a separate destination to navigate to. */}
        <nav className="flex items-center gap-5 font-meta text-sm tracking-wide">
          <Link href="/konzerte" className="hover:text-accent">
            Konzerte
          </Link>
          <Link href="/orte" className="hover:text-accent">
            Orte
          </Link>
          <Link href="/festivals" className="hover:text-accent">
            Festivals
          </Link>
          <Link href="/suche" className="hover:text-accent" aria-label="Suche">
            Suche
          </Link>
        </nav>

        <div className="flex items-center gap-4">
          <LocationPicker city={prefs.city} radiusKm={prefs.radiusKm} usingDeviceLocation={prefs.lat !== null} />
          {session ? (
            <div className="flex items-center gap-4 font-meta text-sm">
              <Link href="/konzerte/neu" className="hover:text-accent">
                + Konzert
              </Link>
              <UserMenu username={session.username} isAdmin={session.platformAdmin} />
            </div>
          ) : (
            <Link href="/login" className="font-meta text-sm hover:text-accent">
              Anmelden
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
