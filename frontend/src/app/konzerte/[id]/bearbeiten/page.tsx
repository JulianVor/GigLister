import { notFound, redirect } from "next/navigation";
import { ApiError, getEvent } from "@/lib/api";
import { getSession } from "@/lib/session";
import { canEditEvent } from "@/lib/permissions";
import { EventForm } from "@/components/EventForm";

export default async function EditEventPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const eventId = Number(id);
  const session = await getSession();
  if (!session) redirect("/login");

  const event = await getEvent(eventId).catch((err) => {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  });

  if (!canEditEvent(session, event)) {
    redirect(`/konzerte/${eventId}`);
  }

  return (
    <div>
      <h1 className="font-display text-3xl">Konzert bearbeiten</h1>
      <div className="mt-6">
        <EventForm eventId={eventId} initial={event} />
      </div>
    </div>
  );
}
