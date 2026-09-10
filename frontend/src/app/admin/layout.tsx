import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session || !session.platformAdmin) {
    redirect("/");
  }

  return (
    <div>
      <nav className="mb-8 flex gap-5 border-b border-line pb-4 font-meta text-sm tracking-wide">
        <Link href="/admin" className="hover:text-accent">
          Übersicht
        </Link>
        <Link href="/admin/claims" className="hover:text-accent">
          Claims
        </Link>
        <Link href="/admin/duplicates" className="hover:text-accent">
          Duplikate
        </Link>
      </nav>
      {children}
    </div>
  );
}
