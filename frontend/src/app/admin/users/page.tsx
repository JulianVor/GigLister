import { getAdminUsers } from "@/lib/api";
import { getSession, getToken } from "@/lib/session";
import { UsersList } from "@/components/admin/UsersList";

export default async function AdminUsersPage({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const { q } = await searchParams;
  const [token, session] = await Promise.all([getToken(), getSession()]);
  const users = await getAdminUsers(q, token!);

  return (
    <div>
      <h1 className="font-display text-3xl">Nutzer</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        Admin-Rechte sind nur ein Flag auf einem ganz normalen Konto — kein eigener Account-Typ.
      </p>

      <form action="/admin/users" className="mt-6 flex max-w-sm gap-2">
        <input
          type="search"
          name="q"
          defaultValue={q}
          placeholder="Suche nach Name oder E-Mail …"
          className="w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
        />
        <button type="submit" className="border border-line px-4 py-2 font-meta text-sm hover:border-fg">
          Suchen
        </button>
      </form>

      <div className="mt-6">
        <UsersList users={users} currentUserId={session!.id} />
      </div>
    </div>
  );
}
