import { getCalendar } from "@/lib/api";
import { Calendar } from "@/components/Calendar";

export default async function KalenderPage({
  searchParams,
}: {
  searchParams: Promise<{ year?: string; month?: string }>;
}) {
  const params = await searchParams;
  const now = new Date();
  const year = params.year ? Number(params.year) : now.getFullYear();
  const month = params.month ? Number(params.month) : now.getMonth() + 1;

  const counts = await getCalendar(year, month);

  return (
    <div>
      <h1 className="font-display text-3xl">Kalender</h1>
      <div className="mt-6">
        <Calendar year={year} month={month} counts={counts} />
      </div>
    </div>
  );
}
