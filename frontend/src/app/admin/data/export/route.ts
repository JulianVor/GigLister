import { NextResponse } from "next/server";
import { exportAllData } from "@/lib/api";
import { getSession, getToken } from "@/lib/session";

/**
 * Streams the backend's export straight through as a file download - a plain <a href>
 * link can't attach the Authorization header a normal fetch would, so this route handler
 * reads the token server-side (same as a Server Action would) and proxies the request,
 * keeping the backend's own Content-Disposition header so the browser saves it as a file.
 * A GET, not a Server Action - Server Actions have their own body-size cap (see
 * next.config.ts), and this response only grows as the database does.
 */
export async function GET() {
  const [session, token] = await Promise.all([getSession(), getToken()]);
  if (!session || !token) {
    return NextResponse.json({ message: "Bitte zuerst einloggen." }, { status: 401 });
  }
  if (!session.platformAdmin) {
    return NextResponse.json({ message: "Nur für Admins." }, { status: 403 });
  }

  const res = await exportAllData(token);
  if (!res.ok || !res.body) {
    return NextResponse.json({ message: "Export fehlgeschlagen." }, { status: res.status || 502 });
  }

  const contentDisposition = res.headers.get("Content-Disposition") ?? 'attachment; filename="giglister-export.json"';
  return new NextResponse(res.body, {
    headers: {
      "Content-Type": "application/json",
      "Content-Disposition": contentDisposition,
    },
  });
}
