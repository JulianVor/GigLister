import { notFound, redirect } from "next/navigation";
import { ApiError, getBand, getBandPermissions } from "@/lib/api";
import { getSession, getToken } from "@/lib/session";
import { canManageEntity } from "@/lib/permissions";
import { BandForm } from "@/components/BandForm";
import { PermissionsPanel } from "@/components/PermissionsPanel";

export default async function EditBandPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const bandId = Number(id);
  const session = await getSession();
  if (!session) redirect("/login");

  const band = await getBand(bandId, await getToken()).catch((err) => {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  });

  if (!canManageEntity(session, "BAND", band.id)) {
    redirect(`/bands/${band.id}`);
  }

  const isManager = session.managedEntities.some(
    (m) => m.entityType === "BAND" && m.entityId === band.id && m.permission === "MANAGE"
  );
  const permissions = isManager || session.platformAdmin ? await getBandPermissions(band.id, (await getToken())!) : [];

  return (
    <div>
      <h1 className="font-display text-3xl">{band.name} bearbeiten</h1>
      <div className="mt-6 space-y-10">
        <BandForm band={band} />
        {(isManager || session.platformAdmin) && (
          <PermissionsPanel entityType="BAND" entityId={band.id} permissions={permissions} />
        )}
      </div>
    </div>
  );
}
