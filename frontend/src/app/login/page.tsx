import Link from "next/link";
import { LoginForm } from "@/components/LoginForm";

export default function LoginPage() {
  return (
    <div>
      <h1 className="font-display text-3xl">Anmelden</h1>
      <div className="mt-6">
        <LoginForm />
      </div>
      <p className="mt-6 font-meta text-sm text-muted">
        Noch kein Konto?{" "}
        <Link href="/registrieren" className="text-accent hover:underline">
          Registrieren
        </Link>
      </p>
    </div>
  );
}
