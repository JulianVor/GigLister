import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { BandForm } from "@/components/BandForm";

export default async function NewBandPage() {
  const session = await getSession();
  if (!session) redirect("/login");

  return (
    <div>
      <h1 className="font-display text-3xl">Neue Band</h1>
      <div className="mt-6">
        <BandForm />
      </div>
    </div>
  );
}
