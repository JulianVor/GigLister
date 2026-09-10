import Link from "next/link";
import { ResetPasswordForm } from "@/components/ResetPasswordForm";

export default async function PasswortZuruecksetzenPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;

  if (!token) {
    return (
      <div className="max-w-sm">
        <h1 className="font-display text-3xl">Ungültiger Link</h1>
        <p className="mt-2 font-meta text-sm text-muted">Diesem Link fehlt der nötige Code.</p>
        <p className="mt-6 font-meta text-sm text-muted">
          <Link href="/passwort-vergessen" className="text-accent hover:underline">
            Neuen Link anfordern
          </Link>
        </p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="font-display text-3xl">Neues Passwort vergeben</h1>
      <div className="mt-6">
        <ResetPasswordForm token={token} />
      </div>
    </div>
  );
}
