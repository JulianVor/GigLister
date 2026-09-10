import Link from "next/link";

export default async function EmailBestaetigenFehlerPage({
  searchParams,
}: {
  searchParams: Promise<{ message?: string }>;
}) {
  const { message } = await searchParams;

  return (
    <div className="max-w-sm">
      <h1 className="font-display text-3xl">Bestätigung fehlgeschlagen</h1>
      <p className="mt-2 font-meta text-sm text-muted">
        {message ?? "Diesem Bestätigungslink fehlt der nötige Code."}
      </p>
      <p className="mt-6 font-meta text-sm text-muted">
        <Link href="/registrieren" className="text-accent hover:underline">
          Neu registrieren
        </Link>
      </p>
    </div>
  );
}
