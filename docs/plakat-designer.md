# Plakat-Designer für die Webseite

Stand: 18. September 2026. Einstieg für Konzert-Bearbeiter: **Konzert → Plakat gestalten**, URL `/konzerte/{id}/plakat`. Die gleichen objektbezogenen Rechte wie beim Bearbeiten des Konzerts gelten; der Entwurf verändert weder Konzert noch Bandprofile.

## Ablauf

1. Eigenes JPG/PNG/WebP auswählen oder einen Hintergrund aus einem Farbverlauf und einem von zwölf generativen Mustern erzeugen: Verlauf, Strahlen, Streifen, Punkte, Körnung, Blitze, Spritzer, Risse, Schachbrett, Wirbel, Höhenlinien oder Marmor. Der Farbverlauf ist nicht auf zwei Farben beschränkt: über „+ Farbe hinzufügen“ lassen sich bis zu acht Farbstops mit eigener Position (0–100 %) anlegen und wieder entfernen. Fünf Regler wirken auf alle zwölf Muster: Musterfarbe, Musterstärke (Deckkraft, 5–90 %), Musterdichte (Abstand/Anzahl der Formen, 0,3×–3×), Strichstärke (Linien-/Punktdicke, 0,3×–3×) und Chaos (Unregelmäßigkeit, 0 = geometrisch sauber, 1 = Standard, bis 5 = extrem wild). Dazu ein Mischmodus (Normal, Multiplizieren, Negativ multiplizieren, Ineinanderkopieren, Differenz, Abwedeln, Ausschluss), der bestimmt, wie das Muster mit dem Farbverlauf/Bild darunter verrechnet wird. Die Vorschau zeigt bereits die Konzertdaten. Neue Farbstimmungen lassen sich per Knopfdruck erzeugen.
2. „Weiter zum Designer“ öffnet die Ebenenbearbeitung. Titel, Band-Blöcke und Infokasten werden automatisch gesetzt. Bei kleinen ungeraden Line-ups steht die erste Band größer über den anderen. Fehlt der Veranstaltungstitel, entfällt die Überschrift. Jede Band zeigt ihr Logo oder ersatzweise ihren Namen und darunter nur ihr erstes Genre.
3. Maus, Touch und Pfeiltasten verschieben die ausgewählte Ebene; Zwei-Finger-Gesten und Regler ändern Größe/Drehung. Textfarbe, dunkle Kästen und die Deckkraft des unteren Infokastens sind einstellbar. Logos können original, weiß oder schwarz dargestellt werden. Rückgängig, Ebenen-Reset und automatische Neuanordnung sind verfügbar.
4. PNG herunterladen: 2480 × 3508 Pixel, A4-Seitenverhältnis. Beim Drucken auf A4 einpassen. Die Ausgabe verwendet dieselbe Zeichenroutine und dieselben Schriften wie die Vorschau, ohne Auswahlrahmen.

Datum, Uhrzeit und Location werden automatisch übernommen. Eine nicht eingetragene Uhrzeit wird nicht erfunden. Namen und Genres werden auf ihre vorgesehenen Flächen eingepasst. Der standardmäßig zu 65 Prozent deckende schwarze Kasten lässt den Hintergrund durchscheinen.

## Entwürfe und Bilder

Hintergrundbilder werden lokal im Browser eingelesen, nicht auf den Server hochgeladen. Die bestehende Upload-Größenkonfiguration gilt auch hier. „Entwurf speichern/laden“ verwendet den lokalen Browserspeicher pro Konzert. Bei Speicherknappheit erscheint ein Hinweis; direktes Herunterladen bleibt möglich. Beim Laden stammen Titel, Logos, Genres und Termin aus dem aktuellen Konzert. Geänderte Line-ups werden erkannt; ein inkompatibler Entwurf wird nicht stillschweigend verwendet.

Bandlogos werden über einen authentifizierten Same-Origin-Endpunkt geladen, damit der Canvas-Export auch bei separater Backend-Domain funktioniert. Der Endpunkt akzeptiert nur eine Band-ID aus dem Konzert, prüft die Konzertrechte und liest ausschließlich Dateien aus dem Upload-Verzeichnis des konfigurierten Backends. Es gibt keinen frei adressierbaren URL-Proxy. Nicht ladbare Logos erhalten einen sichtbaren Hinweis und Namensersatz.

## Prüfung

- Produktionsbuild (`next build`), TypeScript und ESLint der betroffenen Dateien: erfolgreich.
- Fünf Modelltests: Titel-/Zeit-Fallback, erstes Genre, überschneidungsfreie Standardanordnung für 1–20 Bands, validierte Entwurfswiederherstellung mit aktuellen Konzertdaten sowie Auswahl gedrehter/vergrößerter Ebenen.
- Browserprüfung gegen einen lokalen Fixture-Server: Gast-/Fremdzugriff, Rechte für Logo-Endpunkt, Konzert-Einstieg, alle fünf Muster, Maus- und Tastaturverschiebung, Größenänderung, Rückgängig, Entwurf laden, Bildimport/ungültige Datei, PNG-Download mit geprüften Abmessungen, fehlender Titel/Uhrzeit und mobile Ansicht ohne horizontalen Überlauf.
- Zusätzliche mobile Prüfung mit echten Browser-Touch-Ereignissen: Zwei-Finger-Zoom und Drehung erfolgreich.
- Desktop-, mobile Ansicht und tatsächlicher PNG-Export visuell geprüft. Sämtliche Test-API-Aufrufe waren lokale GET-Anfragen. Keine Live-Veröffentlichung und keine Live-Datenänderungen.

Modelltests mit Node 22.18+ / 24: `node --experimental-strip-types --test tests/poster.test.mjs` im Frontend-Ordner. Browserprüfung nach einem Build: `node tests/poster-browser.mjs`; benötigt Playwright und Chromium. Optional bestimmen `PLAYWRIGHT_PACKAGE` den Paketpfad und `CHROMIUM_PATH` den Browserpfad. Sie startet und beendet lokale Testserver auf 3218/19081. Artefakte und Logs liegen unter `frontend/build/poster-tests/`.

## Ansichten mit Testdaten

- [Desktop-Designer](plakat-designer/desktop.png)
- [Mobile Ansicht ohne Veranstaltungstitel](plakat-designer/mobil.png)

## Transparenzprüfung und Logo-Rahmen

Logos werden beim Laden anhand ihrer tatsächlichen Alpha-Werte in Originalauflösung geprüft (gekacheltes Auslesen begrenzt den zusätzlichen Bildspeicher). Mindestens ein Pixel mit Alpha kleiner als 255 bedeutet transparente bzw. teiltransparente Bildanteile. Die Dateiendung allein ist kein Kriterium: auch vollständig deckende PNGs werden erkannt. Ein Fehler beim Auslesen wird als „nicht ermittelbar“ angezeigt.

Bei einem deckenden Logo kann an der jeweiligen Band-Ebene „Rahmen um dieses Logo“ eingeschaltet werden, mit eigener Farbe und Stärke. Der Rahmen umschließt die tatsächliche Bildfläche, nicht das Genre oder den gesamten Band-Block; er skaliert und dreht sich mit dem Logo. Die Reservierung der Rahmenbreite hält den Rahmen innerhalb der vorgesehenen Bandfläche. Bei nicht ermittelbarer Transparenz bleibt eine manuelle Auswahl möglich. Alte Entwürfe erhalten standardmäßig keinen Rahmen.

Geprüft: sechs Modelltests, Produktionsbuild, ESLint und erweiterter Browserlauf mit transparentem PNG und vollständig deckendem PNG, Rahmenauswahl/-stärke, Speicherung und Wiederladen, PNG-Export und unabhängigen Band-Ebenen. [Ansicht mit Logo-Rahmen](plakat-designer/logo-rahmen.png).
