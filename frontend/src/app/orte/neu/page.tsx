import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { LocationForm } from "@/components/LocationForm";

export default async function NewLocationPage() {
  const session = await getSession();
  if (!session) redirect("/login");

  return (
    <div>
      <h1 className="font-display text-3xl">Neuer Ort</h1>
      <div className="mt-6">
        <LocationForm />
      </div>
    </div>
  );
}
