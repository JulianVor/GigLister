import { NextRequest, NextResponse } from "next/server";
import { importAllData } from "@/lib/api";
import { getSession, getToken } from "@/lib/session";

/**
 * Forwards an import request's raw body to the backend - a route handler, not a Server
 * Action, for the same reason as the export one: Server Actions cap their body size (see
 * next.config.ts) at a limit meant for image uploads, and this payload is the whole
 * database. Reads the body as plain text and passes it straight through rather than
 * JSON.parse/stringify-ing it, so nothing here needs to understand its shape.
 */
export async function POST(request: NextRequest) {
  const [session, token] = await Promise.all([getSession(), getToken()]);
  if (!session || !token) {
    return NextResponse.json({ message: "Bitte zuerst einloggen." }, { status: 401 });
  }
  if (!session.platformAdmin) {
    return NextResponse.json({ message: "Nur für Admins." }, { status: 403 });
  }

  const body = await request.text();
  const res = await importAllData(body, token);
  const text = await res.text();
  return new NextResponse(text, {
    status: res.status,
    headers: { "Content-Type": res.headers.get("Content-Type") ?? "application/json" },
  });
}
