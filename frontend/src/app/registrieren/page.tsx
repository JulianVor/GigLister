import Link from "next/link";
import { RegisterForm } from "@/components/RegisterForm";

export default function RegisterPage() {
  return (
    <div>
      <h1 className="font-display text-3xl">Konto erstellen</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        Ein Konto reicht für alles — Konzerte merken, Bands folgen und Bands/Orte verwalten.
      </p>
      <div className="mt-6">
        <RegisterForm />
      </div>
      <p className="mt-6 font-meta text-sm text-muted">
        Schon ein Konto?{" "}
        <Link href="/login" className="text-accent hover:underline">
          Anmelden
        </Link>
      </p>
    </div>
  );
}
