"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { approveSubmissionAction, rejectSubmissionAction, updateSubmissionAction } from "@/actions/admin";
import { SUBMISSION_TYPE_LABELS } from "@/lib/status-labels";
import type { SubmissionResponse } from "@/lib/types";

export function SubmissionsList({ submissions }: { submissions: SubmissionResponse[] }) {
  if (submissions.length === 0) {
    return <p className="font-meta text-sm text-muted">Keine Vorschläge in dieser Ansicht.</p>;
  }

  return (
    <ul className="divide-y divide-line border-y border-line">
      {submissions.map((submission) => (
        <SubmissionItem key={submission.id} submission={submission} />
      ))}
    </ul>
  );
}

function SubmissionItem({ submission }: { submission: SubmissionResponse }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState(false);
  const [editing, setEditing] = useState(false);
  const [reason, setReason] = useState("");

  function approve() {
    setError(null);
    startTransition(async () => {
      const result = await approveSubmissionAction(submission.id);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      router.refresh();
    });
  }

  function reject() {
    setError(null);
    startTransition(async () => {
      const result = await rejectSubmissionAction(submission.id, reason || undefined);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      setRejecting(false);
      router.refresh();
    });
  }

  const isDecided = submission.status !== "PENDING";

  return (
    <li className="py-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex items-start gap-4">
          {submission.imageUrl && !editing && (
            // External URL, previewed directly from the browser - never fetched by our
            // server until an admin approves this submission.
            // eslint-disable-next-line @next/next/no-img-element
            <img src={submission.imageUrl} alt="" className="h-20 w-20 flex-none border border-line object-cover" />
          )}
          <div>
            <div className="flex items-center gap-2">
              <span className="border border-line px-2 py-0.5 font-meta text-xs uppercase tracking-wide text-muted">
                {SUBMISSION_TYPE_LABELS[submission.type]}
              </span>
              <span className="font-meta text-xs text-muted">
                {new Date(submission.submittedAt).toLocaleString("de-DE")}
              </span>
            </div>

            {editing ? (
              <PayloadEditor
                submission={submission}
                pending={pending}
                onCancel={() => setEditing(false)}
                onSave={(payload, imageUrl) => {
                  setError(null);
                  startTransition(async () => {
                    const result = await updateSubmissionAction(submission.id, payload, imageUrl);
                    if (!result.ok) {
                      setError(result.error);
                      return;
                    }
                    setEditing(false);
                    router.refresh();
                  });
                }}
              />
            ) : (
              <PayloadPreview payload={submission.payload} />
            )}

            {submission.status === "REJECTED" && submission.rejectionReason && (
              <p className="mt-2 font-meta text-sm text-muted">Grund: „{submission.rejectionReason}“</p>
            )}
            {submission.status === "APPROVED" && submission.resultEntityId && (
              <p className="mt-2 font-meta text-sm text-muted">Angelegt als ID {submission.resultEntityId}</p>
            )}
          </div>
        </div>

        {!isDecided && !rejecting && !editing && (
          <div className="flex flex-none gap-2">
            <button
              type="button"
              disabled={pending}
              onClick={approve}
              className="border border-accent px-3 py-1.5 font-meta text-sm text-accent hover:bg-accent hover:text-accent-fg disabled:opacity-60"
            >
              Freigeben
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => setEditing(true)}
              className="border border-line px-3 py-1.5 font-meta text-sm hover:border-fg disabled:opacity-60"
            >
              Bearbeiten
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => setRejecting(true)}
              className="border border-line px-3 py-1.5 font-meta text-sm hover:border-fg disabled:opacity-60"
            >
              Ablehnen
            </button>
          </div>
        )}
      </div>

      {rejecting && (
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <input
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Grund (optional)"
            className="input max-w-xs"
          />
          <button
            type="button"
            disabled={pending}
            onClick={reject}
            className="border border-accent px-3 py-1.5 font-meta text-sm text-accent hover:bg-accent hover:text-accent-fg disabled:opacity-60"
          >
            Ablehnung bestätigen
          </button>
          <button
            type="button"
            disabled={pending}
            onClick={() => setRejecting(false)}
            className="font-meta text-sm text-muted hover:text-fg"
          >
            Abbrechen
          </button>
        </div>
      )}

      {error && <p className="mt-2 font-meta text-sm text-accent">{error}</p>}
    </li>
  );
}

/** Generic renderer for the submission's payload - its shape depends on `type`
 * (matches BandCreateRequest/LocationCreateRequest/EventCreateRequest), so this
 * just lists whatever fields are there rather than a bespoke view per type. */
function PayloadPreview({ payload }: { payload: Record<string, unknown> }) {
  const entries = Object.entries(payload).filter(([, v]) => v !== null && v !== undefined && v !== "");
  return (
    <dl className="mt-1 space-y-0.5">
      {entries.map(([key, value]) => (
        <div key={key} className="flex gap-2 font-meta text-sm">
          <dt className="text-muted">{key}:</dt>
          <dd>{formatPayloadValue(key, value)}</dd>
        </div>
      ))}
    </dl>
  );
}

/** EVENT's `location`/`bands` are EntityRefs (either {id} for an existing
 * Band/Location or {name, city, ...} for a new one) - shown as plain text
 * instead of a raw JSON dump. Plain string arrays (e.g. `genres`) are
 * comma-joined; anything else nested falls back to pretty-printed JSON. */
function formatPayloadValue(key: string, value: unknown): React.ReactNode {
  if (key === "location") return formatEntityRef(value);
  if (key === "bands" && Array.isArray(value)) return value.map(formatEntityRef).join("; ");
  if (Array.isArray(value) && value.every((v) => typeof v !== "object" || v === null)) {
    return value.join(", ");
  }
  if (typeof value === "object" && value !== null) {
    return <pre className="whitespace-pre-wrap font-sans">{JSON.stringify(value, null, 2)}</pre>;
  }
  return String(value);
}

function formatEntityRef(ref: unknown): string {
  if (typeof ref !== "object" || ref === null) return String(ref);
  const { id, name, city } = ref as { id?: number; name?: string; city?: string };
  if (id != null) return `bestehend (ID ${id})`;
  return ["neu:", name, city].filter(Boolean).join(" ");
}

/** Editable counterpart to PayloadPreview - a plain text input per primitive field, a JSON
 * textarea for nested objects/arrays (only EVENT's location/bands need that). Lets an admin
 * fix a wrong address or a typo before approving, instead of rejecting and waiting on a
 * resubmission. */
function PayloadEditor({
  submission,
  pending,
  onSave,
  onCancel,
}: {
  submission: SubmissionResponse;
  pending: boolean;
  onSave: (payload: Record<string, unknown>, imageUrl?: string) => void;
  onCancel: () => void;
}) {
  const [fields, setFields] = useState<Record<string, string>>(() => {
    const initial: Record<string, string> = {};
    for (const [key, value] of Object.entries(submission.payload)) {
      initial[key] = typeof value === "object" && value !== null ? JSON.stringify(value, null, 2) : String(value ?? "");
    }
    return initial;
  });
  const [imageUrl, setImageUrl] = useState(submission.imageUrl ?? "");
  const [parseError, setParseError] = useState<string | null>(null);

  function save() {
    setParseError(null);
    const payload: Record<string, unknown> = {};
    for (const [key, original] of Object.entries(submission.payload)) {
      const raw = fields[key] ?? "";
      if (typeof original === "object" && original !== null) {
        if (!raw.trim()) continue;
        try {
          payload[key] = JSON.parse(raw);
        } catch {
          setParseError(`„${key}“ ist kein gültiges JSON.`);
          return;
        }
      } else if (raw !== "") {
        payload[key] = raw;
      }
    }
    onSave(payload, imageUrl || undefined);
  }

  return (
    <div className="mt-2 max-w-lg space-y-2">
      {Object.entries(submission.payload).map(([key, value]) => {
        const isNested = typeof value === "object" && value !== null;
        return (
          <label key={key} className="block">
            <span className="font-meta text-xs uppercase tracking-wide text-muted">{key}</span>
            {isNested ? (
              <textarea
                value={fields[key] ?? ""}
                onChange={(e) => setFields({ ...fields, [key]: e.target.value })}
                rows={4}
                className="input mt-0.5 font-mono text-xs"
              />
            ) : (
              <input
                value={fields[key] ?? ""}
                onChange={(e) => setFields({ ...fields, [key]: e.target.value })}
                className="input mt-0.5"
              />
            )}
          </label>
        );
      })}
      <label className="block">
        <span className="font-meta text-xs uppercase tracking-wide text-muted">Bild-URL</span>
        <input value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} className="input mt-0.5" />
      </label>

      {parseError && <p className="font-meta text-sm text-accent">{parseError}</p>}

      <div className="flex gap-2">
        <button
          type="button"
          disabled={pending}
          onClick={save}
          className="border border-accent px-3 py-1.5 font-meta text-sm text-accent hover:bg-accent hover:text-accent-fg disabled:opacity-60"
        >
          Speichern
        </button>
        <button
          type="button"
          disabled={pending}
          onClick={onCancel}
          className="font-meta text-sm text-muted hover:text-fg"
        >
          Abbrechen
        </button>
      </div>
    </div>
  );
}
