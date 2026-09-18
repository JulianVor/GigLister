import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { ApiError, getEvent } from "@/lib/api";
import { getSession, getToken } from "@/lib/session";
import { canEditEvent } from "@/lib/permissions";
import { PosterDesigner } from "@/components/PosterDesigner";

export default async function PosterPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!/^\d+$/.test(id) || Number(id) < 1) notFound();
  const [session, token] = await Promise.all([getSession(), getToken()]);
  if (!session) redirect("/login");
  const event = await getEvent(Number(id), token).catch(error => {
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  });
  if (!canEditEvent(session, event)) redirect(`/konzerte/${event.id}`);
  return <div>
    <Link href={`/konzerte/${event.id}`} className="font-meta text-sm text-muted hover:text-fg">← Zurück zum Konzert</Link>
    <PosterDesigner event={event} />
  </div>;
}
