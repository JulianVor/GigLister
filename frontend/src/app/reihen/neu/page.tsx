import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { EventSeriesForm } from "@/components/EventSeriesForm";

export default async function NewEventSeriesPage() {
  const session = await getSession();
  if (!session) redirect("/login");

  return (
    <div>
      <h1 className="font-display text-3xl">Neue Reihe</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        Für ein Festival oder eine Nacht, die mehrere einzelne Konzerte an verschiedenen Orten bündelt.
      </p>
      <div className="mt-6">
        <EventSeriesForm />
      </div>
    </div>
  );
}
