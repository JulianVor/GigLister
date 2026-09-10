import { notFound, redirect } from "next/navigation";
import { ApiError, getLocation, getLocationPermissions } from "@/lib/api";
import { getSession, getToken } from "@/lib/session";
import { canManageEntity } from "@/lib/permissions";
import { LocationForm } from "@/components/LocationForm";
import { PermissionsPanel } from "@/components/PermissionsPanel";

export default async function EditLocationPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const locationId = Number(id);
  const session = await getSession();
  if (!session) redirect("/login");

  const location = await getLocation(locationId, await getToken()).catch((err) => {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  });

  if (!canManageEntity(session, "LOCATION", location.id)) {
    redirect(`/orte/${location.id}`);
  }

  const isManager = session.managedEntities.some(
    (m) => m.entityType === "LOCATION" && m.entityId === location.id && m.permission === "MANAGE"
  );
  const permissions = isManager || session.platformAdmin ? await getLocationPermissions(location.id, (await getToken())!) : [];

  return (
    <div>
      <h1 className="font-display text-3xl">{location.name} bearbeiten</h1>
      <div className="mt-6 space-y-10">
        <LocationForm location={location} />
        {(isManager || session.platformAdmin) && (
          <PermissionsPanel entityType="LOCATION" entityId={location.id} permissions={permissions} />
        )}
      </div>
    </div>
  );
}
