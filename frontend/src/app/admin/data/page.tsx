import { ImportDataForm } from "@/components/admin/ImportDataForm";

export default function AdminDataPage() {
  return (
    <div>
      <h1 className="font-display text-3xl">Daten</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        Alle Daten der Datenbank exportieren oder importieren - für Backups oder um Daten zwischen Umgebungen zu
        übertragen.
      </p>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Export</h2>
        <p className="mt-1 font-meta text-xs text-muted">
          Lädt eine JSON-Datei mit jeder Zeile jeder Tabelle herunter - inklusive Passwort-Hashes und anderer
          sensibler Daten, also sicher aufbewahren.
        </p>
        <a
          href="/admin/data/export"
          className="mt-2 inline-block whitespace-nowrap bg-fg px-4 py-2 font-meta text-sm text-bg hover:bg-accent hover:text-accent-fg"
        >
          Alle Daten exportieren
        </a>
      </section>

      <section className="mt-10">
        <h2 className="font-meta text-sm uppercase tracking-wide text-muted">Import</h2>
        <ImportDataForm />
      </section>
    </div>
  );
}
