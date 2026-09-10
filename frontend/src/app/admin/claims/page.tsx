import { getAdminClaims } from "@/lib/api";
import { getToken } from "@/lib/session";
import { ClaimsList } from "@/components/admin/ClaimsList";

export default async function AdminClaimsPage() {
  const token = (await getToken())!;
  const claims = await getAdminClaims(token);

  return (
    <div>
      <h1 className="font-display text-3xl">Offene Claims</h1>
      <div className="mt-6">
        <ClaimsList claims={claims} />
      </div>
    </div>
  );
}
