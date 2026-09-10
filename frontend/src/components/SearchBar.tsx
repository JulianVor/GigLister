export function SearchBar({ initialQuery }: { initialQuery?: string }) {
  return (
    <form action="/suche" className="flex gap-2">
      <input
        type="search"
        name="q"
        defaultValue={initialQuery}
        placeholder="Konzerte, Bands, Orte …"
        className="w-full border border-line bg-bg px-3 py-2 text-sm outline-none focus:border-accent"
      />
      <button type="submit" className="border border-line px-4 py-2 font-meta text-sm hover:border-fg">
        Suchen
      </button>
    </form>
  );
}
