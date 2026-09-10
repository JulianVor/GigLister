import { NextRequest, NextResponse } from "next/server";
import { ApiError, verifyEmail } from "@/lib/api";
import { TOKEN_COOKIE } from "@/lib/session";

/**
 * The link from the verification email. A plain GET so it works straight out
 * of any mail client; a route handler (not a page) because confirming sets
 * the session cookie, which Next.js only allows outside of rendering.
 */
export async function GET(request: NextRequest) {
  const token = request.nextUrl.searchParams.get("token");
  if (!token) {
    return NextResponse.redirect(new URL("/email-bestaetigen/fehler", request.url));
  }

  try {
    const res = await verifyEmail(token);
    const response = NextResponse.redirect(new URL("/mein-giglister", request.url));
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
    return NextResponse.redirect(new URL(`/email-bestaetigen/fehler?message=${encodeURIComponent(message)}`, request.url));
  }
}
