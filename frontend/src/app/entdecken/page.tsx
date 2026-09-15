import { redirect } from "next/navigation";

/** Entdecken's content moved to the homepage (see app/page.tsx's own comment) - this
 * route stays only so an old bookmark or link still lands somewhere instead of 404ing. */
export default function EntdeckenPage() {
  redirect("/");
}
