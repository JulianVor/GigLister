import Link from "next/link";
import { ForgotPasswordForm } from "@/components/ForgotPasswordForm";

export default function PasswortVergessenPage() {
  return (
    <div>
      <h1 className="font-display text-3xl">Passwort vergessen</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        Gib deine E-Mail-Adresse ein, wir schicken dir einen Link zum Zurücksetzen.
      </p>
      <div className="mt-6">
        <ForgotPasswordForm />
      </div>
      <p className="mt-6 font-meta text-sm text-muted">
        <Link href="/login" className="text-accent hover:underline">
          Zurück zum Login
        </Link>
      </p>
    </div>
  );
}
