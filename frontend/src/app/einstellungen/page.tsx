import { redirect } from "next/navigation";
import { getGenres } from "@/lib/api";
import { getSession } from "@/lib/session";
import { ProfileLocationForm } from "@/components/ProfileLocationForm";
import { GenrePreferenceForm } from "@/components/GenrePreferenceForm";
import { ChangePasswordForm } from "@/components/ChangePasswordForm";

export default async function EinstellungenPage() {
  const session = await getSession();
  if (!session) redirect("/login");

  const allGenres = await getGenres();

  return (
    <div>
      <h1 className="font-display text-3xl">Einstellungen</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        {session.username} · {session.email}
      </p>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Standort</h2>
        <p className="mt-1 font-meta text-xs text-muted">
          Wird bei der nächsten Anmeldung automatisch als &quot;Standort wählen&quot; übernommen.
        </p>
        <ProfileLocationForm homeCity={session.homeCity} radiusKm={session.radiusKm} />
      </section>

      <section id="genres" className="mt-10 scroll-mt-4">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Genres</h2>
        <GenrePreferenceForm allGenres={allGenres} initialSelected={session.preferredGenres} />
      </section>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Passwort</h2>
        <ChangePasswordForm />
      </section>
    </div>
  );
}
