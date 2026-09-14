import { notFound, redirect } from "next/navigation";
import { ApiError, getEventSeries } from "@/lib/api";
import { getSession } from "@/lib/session";
import { canManageEntity } from "@/lib/permissions";
import { EventSeriesForm } from "@/components/EventSeriesForm";

export default async function EditEventSeriesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const seriesId = Number(id);
  const session = await getSession();
  if (!session) redirect("/login");

  const series = await getEventSeries(seriesId).catch((err) => {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  });

  if (!canManageEntity(session, "EVENT_SERIES", series.id)) {
    redirect(`/reihen/${series.id}`);
  }

  return (
    <div>
      <h1 className="font-display text-3xl">Reihe bearbeiten</h1>
      <div className="mt-6">
        <EventSeriesForm series={series} />
      </div>
    </div>
  );
}
