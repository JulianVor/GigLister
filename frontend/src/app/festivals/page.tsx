import Link from "next/link";
import { getEventSeriesList } from "@/lib/api";
import { EmptyState } from "@/components/EmptyState";
import { EntityPlaceholder } from "@/components/EntityPlaceholder";

export default async function FestivalsPage() {
  const festivals = await getEventSeriesList();

  return (
    <div>
      <h1 className="font-display text-3xl">Festivals</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        Festivals und Nächte, die mehrere Konzerte bündeln - z. B. &bdquo;SüdKultur MusicNight&ldquo;.
      </p>

      <div className="mt-6">
        {festivals.length === 0 ? (
          <EmptyState>Noch keine Festivals gelistet.</EmptyState>
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            {festivals.map((festival) => (
              <Link
                key={festival.id}
                href={`/festivals/${festival.id}`}
                className="group block border border-line hover:border-fg"
              >
                <div className="aspect-video w-full overflow-hidden border-b border-line bg-surface">
                  {festival.titleImageUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={festival.titleImageUrl} alt="" className="h-full w-full object-cover" />
                  ) : (
                    <EntityPlaceholder name={festival.name} className="h-full w-full" textClassName="text-4xl" />
                  )}
                </div>
                <div className="p-3 font-display text-lg group-hover:text-accent">{festival.name}</div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
