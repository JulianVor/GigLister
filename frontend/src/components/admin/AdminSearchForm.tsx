export function AdminSearchForm({
  action,
  query,
  placeholder,
  hidden,
}: {
  action: string;
  query?: string;
  placeholder: string;
  /** Extra params (e.g. the current status filter) to carry along on submit. */
  hidden?: Record<string, string | undefined>;
}) {
  return (
    <form action={action} className="flex max-w-sm gap-2">
      {hidden &&
        Object.entries(hidden).map(
          ([name, value]) => value && <input key={name} type="hidden" name={name} value={value} />
        )}
      <input
        type="search"
        name="q"
        defaultValue={query}
        placeholder={placeholder}
        className="w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
      />
      <button type="submit" className="border border-line px-4 py-2 font-meta text-sm hover:border-fg">
        Suchen
      </button>
    </form>
  );
}
