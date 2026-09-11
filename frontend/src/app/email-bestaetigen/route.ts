import { NextRequest, NextResponse } from "next/server";
import { ApiError, verifyEmail } from "@/lib/api";
import { TOKEN_COOKIE } from "@/lib/session";

/**
 * Base URL for the redirects below - deliberately NOT built from the
 * request itself (`request.url`/`request.nextUrl.origin`). In a Docker
 * deployment (`output: "standalone"`), Next.js stitches that from its own
 * internal view of itself (container hostname + the container-internal
 * port it listens on), completely ignoring the actual public host/port a
 * browser reached it through - behind a reverse proxy on a real domain
 * that's neither the domain nor the port anyone can actually reach.
 * GIGLISTER_FRONTEND_URL is the same "what's my own public origin" value
 * the backend already needs for the links inside its emails - set it to
 * this app's real public URL in production; falls back to the request's
 * own origin for plain `next dev`, where this doesn't apply.
 */
const FRONTEND_URL = process.env.GIGLISTER_FRONTEND_URL;

/**
 * The link from the verification email. A plain GET so it works straight out
 * of any mail client; a route handler (not a page) because confirming sets
 * the session cookie, which Next.js only allows outside of rendering.
 */
export async function GET(request: NextRequest) {
  const base = FRONTEND_URL || request.nextUrl.origin;
  const token = request.nextUrl.searchParams.get("token");
  if (!token) {
    return NextResponse.redirect(new URL("/email-bestaetigen/fehler", base));
  }

  try {
    const res = await verifyEmail(token);
    const response = NextResponse.redirect(new URL("/mein-giglister", base));
    response.cookies.set(TOKEN_COOKIE, res.token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 60 * 24, // matches the backend's 24h token expiry
    });
    return response;
  } catch (err) {
    const message = err instanceof ApiError ? err.message : "Unbekannter Fehler";
    return NextResponse.redirect(new URL(`/email-bestaetigen/fehler?message=${encodeURIComponent(message)}`, base));
  }
}
