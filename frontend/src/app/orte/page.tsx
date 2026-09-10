import { getLocations } from "@/lib/api";
import { getLocationPrefs } from "@/lib/location-prefs";
import { LocationTeaser } from "@/components/LocationTeaser";
import { EmptyState } from "@/components/EmptyState";

export default async function OrtePage() {
  const prefs = await getLocationPrefs();
  const page = await getLocations({ city: prefs.city ?? undefined, size: 50 });

  return (
    <div>
      <h1 className="font-display text-3xl">Orte {prefs.city ? `in ${prefs.city}` : ""}</h1>
      <p className="mt-1 font-meta text-sm text-muted">Wo gibt es Konzerte?</p>

      <div className="mt-6">
        {page.content.length === 0 ? (
          <EmptyState>Noch keine Orte gelistet.</EmptyState>
        ) : (
          page.content.map((location) => <LocationTeaser key={location.id} location={location} />)
        )}
      </div>
    </div>
  );
}
