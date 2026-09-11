import Link from "next/link";
import { getAdminDashboard } from "@/lib/api";
import { getToken } from "@/lib/session";

export default async function AdminDashboardPage() {
  const token = (await getToken())!;
  const dashboard = await getAdminDashboard(token);

  const tiles = [
    { label: "Offene Claims", value: dashboard.openClaims, href: "/admin/claims" },
    { label: "Bands unvollständig", value: dashboard.bandsNeedingAttention, href: "/admin/bands" },
    { label: "Orte unvollständig", value: dashboard.locationsNeedingAttention, href: "/admin/locations" },
    { label: "Mögliche Duplikate", value: dashboard.possibleDuplicates, href: "/admin/duplicates" },
  ];

  return (
    <div>
      <h1 className="font-display text-3xl">Plattformverwaltung</h1>
      <div className="mt-6 grid grid-cols-2 gap-px border border-line bg-line sm:grid-cols-4">
        {tiles.map((tile) => (
          <Link key={tile.label} href={tile.href} className="bg-surface p-4 hover:bg-bg">
            <div className="font-display text-3xl">{tile.value}</div>
            <div className="mt-1 font-meta text-xs uppercase tracking-wide text-muted">{tile.label}</div>
          </Link>
        ))}
      </div>
    </div>
  );
}
