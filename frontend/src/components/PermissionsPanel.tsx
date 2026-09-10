"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { grantBandPermissionAction, revokeBandPermissionAction } from "@/actions/bands";
import { grantLocationPermissionAction, revokeLocationPermissionAction } from "@/actions/locations";
import type { EntityType, PermissionLevel, PermissionResponse } from "@/lib/types";

export function PermissionsPanel({
  entityType,
  entityId,
  permissions,
}: {
  entityType: EntityType;
  entityId: number;
  permissions: PermissionResponse[];
}) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const [userId, setUserId] = useState("");
  const [level, setLevel] = useState<PermissionLevel>("EDIT");

  function grant(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const id = Number(userId);
    if (!id) {
      setError("Bitte eine gültige Nutzer-ID angeben.");
      return;
    }
    startTransition(async () => {
      const result =
        entityType === "BAND"
          ? await grantBandPermissionAction(entityId, id, level)
          : await grantLocationPermissionAction(entityId, id, level);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      setUserId("");
      router.refresh();
    });
  }

  function revoke(targetUserId: number) {
    startTransition(async () => {
      const result =
        entityType === "BAND"
          ? await revokeBandPermissionAction(entityId, targetUserId)
          : await revokeLocationPermissionAction(entityId, targetUserId);
      if (!result.ok) setError(result.error);
      else router.refresh();
    });
  }

  return (
    <div className="border border-line p-4">
      <h3 className="font-meta text-sm uppercase tracking-wide text-muted">Berechtigungen</h3>

      <ul className="mt-3 divide-y divide-line">
        {permissions.map((p) => (
          <li key={p.userId} className="flex items-center justify-between py-2">
            <div>
              <div className="font-meta text-sm">{p.username}</div>
              <div className="font-meta text-xs text-muted">{p.email}</div>
            </div>
            <div className="flex items-center gap-3">
              <span className="font-meta text-xs uppercase text-muted">{p.permission}</span>
              <button type="button" onClick={() => revoke(p.userId)} disabled={pending} className="font-meta text-xs text-accent hover:underline">
                Entfernen
              </button>
            </div>
          </li>
        ))}
      </ul>

      <form onSubmit={grant} className="mt-4 flex flex-wrap items-end gap-2">
        <div>
          <label className="font-meta text-xs text-muted">Nutzer-ID</label>
          <input
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            className="mt-1 block w-28 border border-line bg-bg px-2 py-1.5 text-sm outline-none focus:border-accent"
          />
        </div>
        <div>
          <label className="font-meta text-xs text-muted">Recht</label>
          <select
            value={level}
            onChange={(e) => setLevel(e.target.value as PermissionLevel)}
            className="mt-1 block border border-line bg-bg px-2 py-1.5 text-sm outline-none focus:border-accent"
          >
            <option value="EDIT">EDIT</option>
            <option value="MANAGE">MANAGE</option>
          </select>
        </div>
        <button type="submit" disabled={pending} className="border border-line px-3 py-1.5 font-meta text-sm hover:border-fg">
          Nutzer hinzufügen
        </button>
      </form>

      {error && <p className="mt-2 font-meta text-xs text-accent">{error}</p>}
    </div>
  );
}
