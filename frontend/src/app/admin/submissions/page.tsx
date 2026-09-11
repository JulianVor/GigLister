import { getAdminSubmissions } from "@/lib/api";
import { getToken } from "@/lib/session";
import { StatusFilter } from "@/components/admin/StatusFilter";
import { SubmissionsList } from "@/components/admin/SubmissionsList";
import { SUBMISSION_STATUS_LABELS } from "@/lib/status-labels";
import type { SubmissionStatus } from "@/lib/types";

const STATUSES: SubmissionStatus[] = ["PENDING", "APPROVED", "REJECTED"];

export default async function AdminSubmissionsPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: SubmissionStatus }>;
}) {
  const { status } = await searchParams;
  const token = (await getToken())!;
  const submissions = await getAdminSubmissions(status, token);

  return (
    <div>
      <h1 className="font-display text-3xl">Vorschläge</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        Von der ChatGPT-Anbindung eingereichte Bands, Orte und Konzerte — nichts davon existiert, bis du es
        freigibst.
      </p>

      <div className="mt-6">
        <StatusFilter basePath="/admin/submissions" statuses={STATUSES} active={status} labels={SUBMISSION_STATUS_LABELS} />
      </div>

      <div className="mt-4">
        <SubmissionsList submissions={submissions} />
      </div>
    </div>
  );
}
