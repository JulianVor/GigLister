"use client";

import { useTransition } from "react";
import { useRouter } from "next/navigation";
import { demoteUserAction, promoteUserAction } from "@/actions/admin";
import type { AdminUserResponse } from "@/lib/types";

export function UsersList({ users, currentUserId }: { users: AdminUserResponse[]; currentUserId: number }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  function toggle(user: AdminUserResponse) {
    startTransition(async () => {
      await (user.platformAdmin ? demoteUserAction(user.id) : promoteUserAction(user.id));
      router.refresh();
    });
  }

  if (users.length === 0) {
    return <p className="font-meta text-sm text-muted">Keine Nutzer gefunden.</p>;
  }

  return (
    <ul className="divide-y divide-line border-y border-line">
      {users.map((user) => {
        const isSelf = user.id === currentUserId;
        return (
          <li key={user.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
            <div>
              <div className="font-display text-lg">
                {user.displayName}
                {isSelf && <span className="ml-2 font-meta text-xs text-muted">(du)</span>}
              </div>
              <div className="font-meta text-sm text-muted">{user.email}</div>
            </div>
            <div className="flex items-center gap-3">
              <span className="font-meta text-xs uppercase tracking-wide text-muted">
                {user.platformAdmin ? "Admin" : "Nutzer"}
              </span>
              {user.platformAdmin ? (
                <button
                  type="button"
                  disabled={pending || isSelf}
                  title={isSelf ? "Du kannst dir nicht selbst die Admin-Rechte entziehen" : undefined}
                  onClick={() => toggle(user)}
                  className="border border-line px-3 py-1.5 font-meta text-sm hover:border-fg disabled:opacity-40"
                >
                  Admin-Rechte entziehen
                </button>
              ) : (
                <button
                  type="button"
                  disabled={pending}
                  onClick={() => toggle(user)}
                  className="border border-accent px-3 py-1.5 font-meta text-sm text-accent hover:bg-accent hover:text-accent-fg"
                >
                  Zum Admin ernennen
                </button>
              )}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
