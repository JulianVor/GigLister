import { getAdminDuplicates } from "@/lib/api";
import { getToken } from "@/lib/session";
import { DuplicatesList } from "@/components/admin/DuplicatesList";

export default async function AdminDuplicatesPage() {
  const token = (await getToken())!;
  const pairs = await getAdminDuplicates(token);

  return (
    <div>
      <h1 className="font-display text-3xl">Mögliche Duplikate</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        Normalisierter Namensabgleich, optional mit gleicher Stadt. Beim Zusammenführen bleibt die behaltene Entität
        bestehen, alle Konzerte/Berechtigungen werden umgehängt.
      </p>
      <div className="mt-6">
        <DuplicatesList pairs={pairs} />
      </div>
    </div>
  );
}
