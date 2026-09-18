import { ApiError, getEvent } from "@/lib/api";
import { canEditEvent } from "@/lib/permissions";
import { getSession, getToken } from "@/lib/session";

/** Same-origin logo bytes keep the downloadable canvas untainted. The caller supplies
 * only a band id; the destination is always our configured backend's upload directory. */
export async function GET(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const bandId = Number(new URL(request.url).searchParams.get("band"));
  if (!/^\d+$/.test(id) || Number(id) < 1 || !Number.isSafeInteger(bandId) || bandId < 1) return new Response(null, { status: 400 });
  const [session, token] = await Promise.all([getSession(), getToken()]);
  if (!session) return new Response(null, { status: 401 });
  try {
    const event = await getEvent(Number(id), token);
    if (!canEditEvent(session, event)) return new Response(null, { status: 403 });
    const logo = event.bands.find(band => band.id === bandId)?.logoUrl;
    if (!logo) return new Response(null, { status: 404 });
    const path = new URL(logo, "http://placeholder").pathname;
    if (!/^\/uploads\/[a-zA-Z0-9_-]+\.(png|jpe?g|webp|gif)$/i.test(path)) return new Response(null, { status: 404 });
    const response = await fetch(new URL(path, process.env.API_BASE_URL ?? "http://localhost:8080"), { redirect: "error", signal: AbortSignal.timeout(10000), cache: "no-store" });
    const type = response.headers.get("content-type")?.split(";")[0];
    if (!response.ok || !type || !["image/png", "image/jpeg", "image/webp", "image/gif"].includes(type)) return new Response(null, { status: 502 });
    const maxBytes = 10 * 1024 * 1024;
    if (Number(response.headers.get("content-length")) > maxBytes) { await response.body?.cancel(); return new Response(null, { status: 413 }); }
    const reader = response.body?.getReader();
    if (!reader) return new Response(null, { status: 502 });
    const chunks: Uint8Array[] = []; let length = 0;
    try {
      while (true) { const { done, value } = await reader.read(); if (done) break; length += value.length; if (length > maxBytes) { await reader.cancel(); return new Response(null, { status: 413 }); } chunks.push(value); }
    } finally { reader.releaseLock(); }
    const bytes = new Uint8Array(length); let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
    return new Response(bytes, { headers: { "Content-Type": type, "Cache-Control": "private, no-store", "X-Content-Type-Options": "nosniff" } });
  } catch (error) {
    return new Response(null, { status: error instanceof ApiError && error.status === 404 ? 404 : 502 });
  }
}
