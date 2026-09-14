import { redirect } from "next/navigation";
import { getEventSeriesList } from "@/lib/api";
import { getSession } from "@/lib/session";
import { EventForm } from "@/components/EventForm";

export default async function NewEventPage() {
  const session = await getSession();
  if (!session) redirect("/login");

  const eventSeriesOptions = await getEventSeriesList();

  return (
    <div>
      <h1 className="font-display text-3xl">Neues Konzert</h1>
      <div className="mt-6">
        <EventForm eventSeriesOptions={eventSeriesOptions} />
      </div>
    </div>
  );
}
